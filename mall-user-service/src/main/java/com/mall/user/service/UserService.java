package com.mall.user.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mall.common.JwtUtil;
import com.mall.common.RedisService;
import com.mall.common.Result;
import com.mall.user.dto.WxLoginRequest;
import com.mall.user.entity.User;
import com.mall.user.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String USER_CACHE_KEY = "user:";
    private static final int CACHE_TIMEOUT = 30;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedisService redisService;

    @Resource
    private RedissonClient redissonClient;

    // BCrypt 密码加密器
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ============ 微信小程序配置（见 application.yaml 的 wechat.miniapp） ============

    @Value("${wechat.miniapp.appid:}")
    private String wxAppid;

    @Value("${wechat.miniapp.secret:}")
    private String wxSecret;

    @Value("${wechat.miniapp.jscode2session-url:https://api.weixin.qq.com/sns/jscode2session}")
    private String wxJscode2SessionUrl;

    // ============ 文件上传目录（见 application.yaml 的 file.upload-dir） ============
    // 本服务是写入方，mall-Project 的 WebConfig 是读取方（/uploads/** 静态映射），
    // 两者必须指向同一个绝对路径。原先各自用 System.getProperty("user.dir")，
    // 而 user.dir 是各进程自己的启动目录，导致写进 A 目录、却去 B 目录找，头像 404。
    @Value("${file.upload-dir}")
    private String fileUploadDir;

    /**
     * 用户注册
     *
     * @param user 用户信息
     * @return 注册结果
     * <p>
     * 实现流程：
     * 1. 参数校验（用户名、密码不能为空，密码长度至少6位）
     * 2. 检查用户名是否已存在
     * 3. 密码加密（BCrypt）
     * 4. 将空字符串转为 null，避免唯一约束冲突
     * 5. 设置默认状态（status=1, isDeleted=0）
     * 6. 插入用户记录
     * 7. 返回成功结果
     */
    @Transactional
    public Result<Object> register(User user) {
        // 1. 校验参数
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return Result.error(400, "用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return Result.error(400, "密码至少6位");
        }
        // 2. 检查用户名是否存在
        User existUser = userMapper.findByUsername(user.getUsername());
        if (existUser != null) {
            return Result.error(400, "用户名已存在");
        }

        // 3. 密码加密（BCrypt）
        String encryptedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encryptedPassword);

        // 4. 手机号可选填：为空转 null；填写则校验格式并查重（避免撞 uk_phone 唯一键）
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            String phone = user.getPhone().trim();
            if (!ReUtil.isMatch("^1[3-9]\\d{9}$", phone)) {
                return Result.error(400, "手机号格式不正确");
            }
            if (userMapper.findByPhone(phone) != null) {
                return Result.error(400, "手机号已被其他用户使用");
            }
            user.setPhone(phone);
        } else {
            user.setPhone(null);
        }
        if (user.getEmail() != null && user.getEmail().trim().isEmpty()) {
            user.setEmail(null);
        }
        if (user.getNickname() != null && user.getNickname().trim().isEmpty()) {
            user.setNickname(null);
        }
        user.setStatus(1);
        user.setIsDeleted(0);

        // 5. 插入数据库

        String username = user.getUsername();
        //        尝试获取锁
        String lockKey = "lock:user:register:" + username;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
//            2.3判断是否获取成功
            if (!tryLock) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
            User existUser2 = userMapper.findByUsername(user.getUsername());
            if (existUser2 != null) {
                return Result.error(400, "用户名已存在");
            }
            int rows = userMapper.insert(user);
            if (rows > 0) {
                return Result.success(null);
            }
            return Result.error(500, "注册失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁被中断", e);
        } finally {
            //            释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 用户登录
     */
    public Result<Map<String, Object>> login(String username, String password) {
        // 1. 校验参数
        if (username == null || username.trim().isEmpty()) {
            return Result.error(400, "用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            return Result.error(400, "密码不能为空且不能小于6位");
        }

        // 2. 查询用户
        User user = userMapper.findByUsername(username);
        if (user == null) {
            log.warn("登录失败：用户不存在, username={}", username);
            return Result.error(400, "用户名或密码错误");
        }

        log.info("用户登录调试 - username: {}, 输入密码: {}, 数据库密码: {}",
                username, password, user.getPassword());

        // 3. 验证密码（使用 BCrypt）
        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
        log.info("密码匹配结果: {}", passwordMatches);

        if (!passwordMatches) {
            log.warn("登录失败：密码错误, username={}", username);
            return Result.error(400, "用户名或密码错误");
        }

        // 4. 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.error(400, "账号已被禁用");
        }

        if (user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            return Result.error(400, "该用户已被删除");
        }

        // 5. 生成 Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), String.valueOf(user.getRole()));

        // 6. 返回用户信息（不包含密码）
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("status", user.getStatus());
        userInfo.put("role", user.getRole() != null ? user.getRole() : 0);
        userInfo.put("token", token);

        return Result.success(userInfo);
    }

    /**
     * 根据ID获取用户信息
     */
    public Result<User> findById(Long id) {
        if (id == null) {
            return Result.error(400, "用户ID不能为空");
        }

        // 构建缓存Key
        String cacheKey = USER_CACHE_KEY + "detail:" + id;
        String nullKey = USER_CACHE_KEY + "null:" + id;

        try {
            // 第1层：检查空值缓存（防止缓存穿透）
            if (redisTemplate.hasKey(nullKey)) {
                return Result.error(404, "用户不存在");
            }

            // 第2层：尝试从缓存获取
            Object cached = redisService.get(cacheKey);
            if (cached != null) {
                return Result.success((User) cached);
            }
        } catch (Exception e) {
            System.err.println("Redis不可用，降级到数据库: " + e.getMessage());
        }

        // 缓存未命中时，使用互斥锁防止缓存击穿
        String lockKey = "lock:user:cache:" + id;
        RLock lock = redissonClient.getLock(lockKey);

        // 最大重试次数
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
                if (!tryLock) {
                    // 获取锁失败，短暂等待后重试
                    Thread.sleep(100 * (i + 1)); // 递增等待时间
                    continue;
                }

                try {
                    // 双重检查：获取锁后再次检查缓存
                    // 检查空值缓存
                    if (redisTemplate.hasKey(nullKey)) {
                        return Result.error(404, "用户不存在");
                    }

                    // 检查缓存
                    Object cached = redisService.get(cacheKey);
                    if (cached != null) {
                        return Result.success((User) cached);
                    }

                    // 第3层：查询数据库
                    User user = userMapper.selectById(id);
                    if (user == null) {
                        // 第4层：缓存空值防止缓存穿透（TTL=5分钟）
                        try {
                            redisService.set(nullKey, "null", 5, TimeUnit.MINUTES);
                        } catch (Exception e) {
                            System.err.println("缓存空值失败: " + e.getMessage());
                        }
                        return Result.error(404, "用户不存在");
                    }

                    // 清除密码信息
                    user.setPassword(null);

                    // 第5层：写入缓存
                    try {
                        redisService.set(cacheKey, user, 30, TimeUnit.MINUTES);
                    } catch (Exception e) {
                        System.err.println("缓存写入失败: " + e.getMessage());
                    }

                    return Result.success(user);
                } finally {
                    // 确保锁被释放
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取锁被中断", e);
            } catch (Exception e) {
                // 其他异常记录日志，继续重试
                System.err.println("缓存查询失败: " + e.getMessage());
                if (i == maxRetries - 1) {
                    // 最后一次重试失败，降级直接查数据库
                    User user = userMapper.selectById(id);
                    if (user == null) {
                        return Result.error(404, "用户不存在");
                    }
                    user.setPassword(null);
                    return Result.success(user);
                }
            }
        }

        // 所有重试都失败，降级直接查数据库
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public Result<Object> updateUserInfo(User user) {
        // 1. 校验用户是否存在
        User existUser = userMapper.selectById(user.getId());
        ;
        if (existUser == null) {
            return Result.error(404, "用户不存在");
        }

        // 2. 空字符串转 null，避免唯一约束冲突
        //    注意：手机号不再允许通过本接口修改，必须走 bindPhone（短信验证码校验），防止抢绑
        if (user.getEmail() != null && user.getEmail().trim().isEmpty()) {
            user.setEmail(null);
        }
        if (user.getNickname() != null && user.getNickname().trim().isEmpty()) {
            user.setNickname(null);
        }

        // 3. 校验邮箱唯一性（如果修改了邮箱）
        if (user.getEmail() != null && !user.getEmail().equals(existUser.getEmail())) {
            User emailUser = userMapper.findByEmail(user.getEmail());
            if (emailUser != null) {
                return Result.error(400, "邮箱已被其他用户使用");
            }
        }
        // 5. 更新数据库
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, user.getId());


        // 只更新非空字段
        if (user.getNickname() != null) {
            updateWrapper.set(User::getNickname, user.getNickname());
        }
        if (user.getEmail() != null) {
            updateWrapper.set(User::getEmail, user.getEmail());
        }
        if (user.getGender() != null) {
            updateWrapper.set(User::getGender, user.getGender());
        }
//第一个参数如果填user会修改所有非null的属性 无法实现动态修改
        int rows = userMapper.update(null, updateWrapper);
        if (rows > 0) {
            // 删除用户详情缓存，避免刷新页面时读到 Redis 中的旧数据
            try {
                redisService.delete(USER_CACHE_KEY + "detail:" + user.getId());
            } catch (Exception e) {
                System.err.println("删除用户缓存失败: " + e.getMessage());
            }
            return Result.success("更新成功");
        }
        return Result.error(500, "更新失败");
    }

    /**
     * 绑定手机号（需短信验证码）
     * <p>
     * 与 updateUserInfo 分离：手机号是登录凭证，必须验证码校验后才能绑定，
     * 防止任意登录用户把他人未绑定的手机号抢绑到自己账号上。
     *
     * @param userId 当前登录用户 ID
     * @param phone 待绑定的手机号
     * @param code 短信验证码
     * @return 绑定结果
     */
    @Transactional
    public Result<Object> bindPhone(Long userId, String phone, String code) {
        // 1. 参数校验
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        if (phone == null || !ReUtil.isMatch("^1[3-9]\\d{9}$", phone)) {
            return Result.error(400, "手机号格式不正确");
        }
        if (code == null || code.trim().isEmpty()) {
            return Result.error(400, "验证码不能为空");
        }

        // 2. 校验验证码
        String redisKey = "sms:code:" + phone;
        Object cached;
        try {
            cached = redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.warn("Redis 不可用: {}", e.getMessage());
            return Result.error(500, "服务繁忙，请稍后重试");
        }
        if (cached == null) {
            return Result.error(400, "验证码已过期或不存在");
        }
        if (!code.equals(cached.toString())) {
            return Result.error(400, "验证码错误");
        }

        // 3. 校验用户是否存在
        User existUser = userMapper.selectById(userId);
        if (existUser == null) {
            return Result.error(404, "用户不存在");
        }

        // 4. 手机号查重（允许自己重复绑定，但手机号已被他人使用则拒绝）
        User phoneUser = userMapper.findByPhone(phone);
        if (phoneUser != null && !phoneUser.getId().equals(userId)) {
            return Result.error(400, "手机号已被其他用户使用");
        }

        // 5. 更新手机号
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                .set(User::getPhone, phone)
                .set(User::getUpdateTime, LocalDateTime.now());

        int rows = userMapper.update(null, updateWrapper);
        if (rows > 0) {
            // 删除已使用的验证码 + 用户详情缓存
            try {
                redisTemplate.delete(redisKey);
            } catch (Exception e) {
                log.warn("删除验证码缓存失败: {}", e.getMessage());
            }
            try {
                redisService.delete(USER_CACHE_KEY + "detail:" + userId);
            } catch (Exception e) {
                log.warn("删除用户缓存失败: {}", e.getMessage());
            }
            return Result.success("绑定成功");
        }
        return Result.error(500, "绑定失败");
    }

    /**
     * 更新密码
     */
    @Transactional
    public Result<Object> updatePassword(Long userId, String oldPassword, String newPassword) {
        // 1. 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 2. 验证旧密码是否正确（使用 BCrypt）
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error(400, "原密码错误");
        }

        // 3. 判断新密码与旧密码是否相同
        if (oldPassword.equals(newPassword)) {
            return Result.error(400, "新密码不能与原密码相同");
        }

        // 4. 加密新密码（使用 BCrypt）
        String encryptedNewPassword = passwordEncoder.encode(newPassword);

        // 5. 更新数据库
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                .set(User::getPassword, encryptedNewPassword);

        int rows = userMapper.update(null, updateWrapper);
        if (rows > 0) {
            return Result.success("密码修改成功");
        }
        return Result.error(500, "密码修改失败");
    }


    /**
     * 上传头像
     */
//    MultipartFile file：Spring框架提供的接口，用于接收上传的文件
    public Result<String> uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file) {
        // 1. 校验用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 2. 校验文件
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }

        // 3. 校验文件类型
//        `getContentType()` 获取文件的 MIME 类型，比如：
//        - 图片：`image/jpeg`、`image/png`、`image/gif`
//        - 文本：`text/plain`
//        - 视频：`video/mp4`
//        - `startsWith("image/")` 确保只能上传图片，防止有人上传病毒文件

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "只能上传图片文件");
        }

        // 4. 校验文件大小（最大 2MB）
        long maxSize = 2 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return Result.error(400, "图片大小不能超过2MB");
        }

        // 5. 生成文件名
//        | 变量 | 值 | 说明 |
//        |------|-----|------|
//        | `originalFilename` | `我的头像.jpg` | 用户电脑上的原始文件名 |
//        | `extension` | `.jpg` | 提取出的后缀名 |
//        | `System.currentTimeMillis()` | `1715347200000` | 当前时间的毫秒数 |
//        | `newFilename` | `avatar_5_1715347200000.jpg` | 最终的新文件名 |

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String newFilename = "avatar_" + userId + "_" + System.currentTimeMillis() + extension;

        // 6. 保存文件（目录来自配置，与 mall-Project 的 /uploads/** 静态映射共用同一配置项）
        try {
//            6.1 拼接头像子目录（去掉配置里可能带的结尾斜杠，避免出现 //）
            String avatarDir = fileUploadDir.replaceAll("/+$", "") + "/avatar/";
//            6.2创建目录对象
            java.io.File dir = new java.io.File(avatarDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // 6.3创建目标文件对象
            java.io.File destFile = new java.io.File(dir, newFilename);
            // 6.4 把上传的文件保存到目标位置
            file.transferTo(destFile);

            // 6.5 记录旧头像路径，落库成功后再删，避免"新文件没存住、旧头像也没了"
            String oldAvatar = user.getAvatar();

            // 7. 更新用户头像 URL
            String avatarUrl = "/uploads/avatar/" + newFilename;
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(User::getId, userId)
                    .set(User::getAvatar, avatarUrl);

            int rows = userMapper.update(null, updateWrapper);

            if (rows > 0) {
                // 落库成功后再清理旧头像，失败只记日志：换头像不该因为删不掉旧文件而报错
                deleteOldAvatar(oldAvatar, avatarUrl);
                return Result.success(avatarUrl);
            }
            return Result.error(400, "头像上传失败");
        } catch (IOException e) {
            return Result.error(500, "上传失败：" + e.getMessage());
        }
    }

    /**
     * 删除被替换掉的旧头像文件。
     * 只处理本站上传的头像（以 /uploads/avatar/ 开头），外链头像（如微信返回的 URL）不动；
     * 文件名做了路径穿越校验，避免 DB 里的脏值删到目录外的文件。
     */
    private void deleteOldAvatar(String oldAvatar, String newAvatar) {
        if (StrUtil.isBlank(oldAvatar) || oldAvatar.equals(newAvatar)
                || !oldAvatar.startsWith("/uploads/avatar/")) {
            return;
        }
        String oldName = oldAvatar.substring("/uploads/avatar/".length());
        // 只允许纯文件名，禁止 .. 或路径分隔符
        if (StrUtil.isBlank(oldName) || oldName.contains("..")
                || oldName.contains("/") || oldName.contains("\\")) {
            log.warn("旧头像路径不合法，跳过删除: {}", oldAvatar);
            return;
        }
        try {
            java.io.File old = new java.io.File(fileUploadDir.replaceAll("/+$", "") + "/avatar/", oldName);
            if (old.exists() && old.isFile() && old.delete()) {
                log.info("已删除旧头像: {}", oldName);
            }
        } catch (Exception e) {
            log.warn("删除旧头像失败（不影响本次上传）: {}, 错误: {}", oldName, e.getMessage());
        }
    }

    /**
     * 发送验证码
     */
    public Result<String> sendCode(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return Result.error(400, "手机号不能为空");
        }
        if (!ReUtil.isMatch("^1[3-9]\\d{9}$", phone)) {
            return Result.error(400, "手机号格式不正确");
        }
        String code = RandomUtil.randomNumbers(6);
        String redisKey = "sms:code:" + phone;
        try {
            redisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis 不可用，验证码无法存储: {}", e.getMessage());
            return Result.error(500, "服务繁忙，请稍后重试");
        }
        log.info("【验证码】手机号={} 验证码={}", phone, code);
        return Result.success("验证码已发送");
    }

    /**
     * 用户用验证码登录
     *
     * @param phone 手机号
     * @param code 验证码
     * @return 登录结果（包含用户信息和token）
     * <p>
     * 实现流程：
     * 1. 参数校验（手机号、验证码不能为空，手机号格式校验）
     * 2. 从 Redis 获取验证码
     * 3. 验证码校验（是否过期、是否正确）
     * 4. 按手机号查询用户：不存在则提示先用账号密码注册并绑定手机号（不再自动注册）
     * 5. 校验账号状态（禁用 / 删除）
     * 6. 删除已使用的验证码
     * 7. 生成 JWT token 并返回用户信息
     */
    public Result<Map<String, Object>> loginByCode(String phone, String code) {
        if (phone == null || phone.trim().isEmpty()) {
            return Result.error(400, "手机号不能为空");
        }
        if (code == null || code.trim().isEmpty()) {
            return Result.error(400, "验证码不能为空");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error(400, "手机号格式不正确");
        }

        // 从 Redis 获取验证码
        String redisKey = "sms:code:" + phone;
        Object cached;
        try {
            cached = redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.warn("Redis 不可用: {}", e.getMessage());
            return Result.error(500, "服务繁忙，请稍后重试");
        }

        // 先判 Redis 返回值，再判匹配（避免 NPE）
        if (cached == null) {
            return Result.error(400, "验证码已过期或不存在");
        }
        if (!code.equals(cached.toString())) {
            return Result.error(400, "验证码错误");
        }

        // 查用户，不存在则提示先注册并绑定手机号（不再自动注册）
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
        if (user == null) {
            return Result.error(400, "该手机号未绑定账号，请先用账号密码注册并绑定手机号");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.error(403, "账号已被禁用");
        }
        if (user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            return Result.error(403, "账号不存在");
        }

        // 删除已使用的验证码
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {

        }

        // 生成 Token + 返回用户信息（与密码登录保持一致）
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), String.valueOf(user.getRole()));
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("phone", user.getPhone());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("token", token);
        return Result.success(userInfo);
    }

    /**
     * 微信小程序登录
     * <p>
     * 实现流程：
     * 1. 校验 code 与微信配置
     * 2. 调用微信 jscode2session 接口，用 code 换取 openid / unionid
     * 3. 按 openid 查用户：存在则直接登录，不存在则自动注册
     * 4. 签发 JWT，返回结构与账号密码登录保持完全一致
     *
     * @param request 微信登录请求（code 必填，nickname / avatarUrl 可选）
     * @return 登录结果（包含用户信息和 token）
     */
    public Result<Map<String, Object>> wxLogin(WxLoginRequest request) {
        // 1. 参数与配置校验
        if (request == null || StrUtil.isBlank(request.getCode())) {
            return Result.error(400, "微信登录 code 不能为空");
        }
        if (StrUtil.isBlank(wxAppid) || StrUtil.isBlank(wxSecret)
                || wxAppid.startsWith("your-")) {
            log.error("微信小程序 AppID / AppSecret 未配置，请检查 application.yaml");
            return Result.error(500, "微信登录暂未开通，请联系管理员");
        }

        // 2. 用 code 换取 openid
        String openid;
        String unionid;
        try {
            JSONObject session = requestWechatSession(request.getCode().trim());
            openid = session.getStr("openid");
            unionid = session.getStr("unionid");
        } catch (IllegalStateException e) {
            log.warn("微信登录失败：{}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("调用微信 jscode2session 异常", e);
            return Result.error(500, "微信登录服务异常，请稍后重试");
        }

        if (StrUtil.isBlank(openid)) {
            return Result.error(400, "微信登录失败：未获取到 openid");
        }

        // 3. 按 openid 查用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid));

        if (user == null) {
            // 首次登录，自动注册。用分布式锁防止同一 openid 并发建号
            String lockKey = "lock:user:register:openid:" + openid;
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
                if (!tryLock) {
                    return Result.error(500, "系统繁忙，请稍后重试");
                }
                // Double Check：拿到锁后重新查一次
                User existUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, openid));
                if (existUser != null) {
                    user = existUser;
                } else {
                    user = createWxUser(openid, unionid, request);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Result.error(500, "系统异常，请稍后重试");
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } else {
            // 老用户：校验账号状态
            if (user.getStatus() != null && user.getStatus() == 0) {
                return Result.error(403, "账号已被禁用");
            }
            if (user.getIsDeleted() != null && user.getIsDeleted() == 1) {
                return Result.error(403, "账号不存在");
            }
            // 昵称、头像有变化时同步一次
            syncWxProfile(user, request);
        }

        if (user == null || user.getId() == null) {
            return Result.error(500, "微信登录失败，请重试");
        }

        // 4. 生成 Token + 返回用户信息（与密码登录结构一致）
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), String.valueOf(user.getRole()));
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("phone", user.getPhone());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("token", token);
        log.info("微信登录成功：openid={}, userId={}", openid, user.getId());
        return Result.success(userInfo);
    }

    /**
     * 调用微信 jscode2session 接口换取 openid
     *
     * @param code 小程序端 uni.login 获取的临时凭证
     * @return 微信返回的 JSON 对象（含 openid / session_key / unionid）
     * @throws IllegalStateException 微信返回错误码时抛出，消息为可读文案
     */
    private JSONObject requestWechatSession(String code) {
        String url = wxJscode2SessionUrl
                + "?appid=" + wxAppid
                + "&secret=" + wxSecret
                + "&js_code=" + code
                + "&grant_type=authorization_code";

        String body = HttpUtil.get(url, 5000);
        JSONObject json = JSONUtil.parseObj(body);

        Integer errcode = json.getInt("errcode");
        if (errcode != null && errcode != 0) {
            String errmsg = json.getStr("errmsg", "未知错误");
            log.warn("微信 jscode2session 返回错误：errcode={}, errmsg={}", errcode, errmsg);
            throw new IllegalStateException("微信登录失败：" + translateWxError(errcode, errmsg));
        }
        return json;
    }

    /**
     * 创建微信新用户
     * <p>
     * 微信登录无密码，写入随机密码占位，该账号无法通过密码登录；
     * 手机号留空，用户后续可在个人资料中绑定。
     */
    private User createWxUser(String openid, String unionid, WxLoginRequest request) {
        User user = new User();
        user.setOpenid(openid);
        user.setUnionid(unionid);
        // 用户名前缀用 wx_ 与验证码登录的 mall_ 区分，便于排查
        user.setUsername("wx_" + RandomUtil.randomString(8));
        user.setNickname(StrUtil.isNotBlank(request.getNickname())
                ? request.getNickname()
                : "微信用户" + RandomUtil.randomNumbers(4));
        user.setAvatar(StrUtil.isNotBlank(request.getAvatarUrl()) ? request.getAvatarUrl() : null);
        user.setPassword(passwordEncoder.encode(RandomUtil.randomString(12)));
        user.setStatus(1);
        user.setRole(0);
        user.setIsDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        log.info("微信登录自动注册新用户：openid={}, username={}", openid, user.getUsername());
        return user;
    }

    /**
     * 同步微信侧的昵称与头像（仅在确实发生变化时更新）
     */
    private void syncWxProfile(User user, WxLoginRequest request) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, user.getId());
        boolean changed = false;

        if (StrUtil.isNotBlank(request.getNickname())
                && !request.getNickname().equals(user.getNickname())) {
            wrapper.set(User::getNickname, request.getNickname());
            user.setNickname(request.getNickname());
            changed = true;
        }
        if (StrUtil.isNotBlank(request.getAvatarUrl())
                && !request.getAvatarUrl().equals(user.getAvatar())) {
            wrapper.set(User::getAvatar, request.getAvatarUrl());
            user.setAvatar(request.getAvatarUrl());
            changed = true;
        }
        if (changed) {
            wrapper.set(User::getUpdateTime, LocalDateTime.now());
            userMapper.update(null, wrapper);
        }
    }

    /**
     * 微信错误码转换为可读文案
     */
    private String translateWxError(Integer errcode, String errmsg) {
        return switch (errcode) {
            case 40029 -> "code 无效或已过期，请重新登录";
            case 45011 -> "请求过于频繁，请稍后重试";
            case 40226 -> "该用户被微信标记为高风险，无法登录";
            case 40013 -> "AppID 配置错误";
            case 40125 -> "AppSecret 配置错误";
            default -> errmsg;
        };
    }

}
