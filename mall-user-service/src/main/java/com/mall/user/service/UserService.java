package com.mall.user.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mall.common.JwtUtil;
import com.mall.common.Result;
import com.mall.user.entity.User;
import com.mall.user.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

        // 4. 将空字符串转为 null，避免唯一约束冲突
        if (user.getPhone() != null && user.getPhone().trim().isEmpty()) {
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
     * todo 用户登录
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
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());

        // 6. 返回用户信息（不包含密码）
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("token", token);

        return Result.success(userInfo);
    }

    /**
     * todo 根据ID获取用户信息
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
     * todo 更新用户信息
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
        if (user.getPhone() != null && user.getPhone().trim().isEmpty()) {
            user.setPhone(null);
        }
        if (user.getEmail() != null && user.getEmail().trim().isEmpty()) {
            user.setEmail(null);
        }
        if (user.getNickname() != null && user.getNickname().trim().isEmpty()) {
            user.setNickname(null);
        }

        // 3. 校验手机号唯一性（如果修改了手机号）
        if (user.getPhone() != null && !user.getPhone().equals(existUser.getPhone())) {
            User phoneUser = userMapper.findByPhone(user.getPhone());
            if (phoneUser != null) {
                return Result.error(400, "手机号已被其他用户使用");
            }
        }

        // 4. 校验邮箱唯一性（如果修改了邮箱）
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
        if (user.getPhone() != null) {
            updateWrapper.set(User::getPhone, user.getPhone());
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
            return Result.success("更新成功");
        }
        return Result.error(500, "更新失败");
    }

    /**
     * todo 更新密码
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
     * todo 上传头像
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

        // 6. 保存文件（使用绝对路径）
        try {
            // 获取项目根目录的绝对路径
//            6.1获取项目根目录的绝对路径
            String projectPath = System.getProperty("user.dir");
//            6.2拼接出上传目录的绝对路径
            String uploadDir = projectPath + "/uploads/avatar/";
//            6.3创建目录对象
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // 6.4创建目标文件对象
            java.io.File destFile = new java.io.File(uploadDir + newFilename);
            // 6.5 把上传的文件保存到目标位置
            file.transferTo(destFile);

            // 7. 更新用户头像 URL
            String avatarUrl = "/uploads/avatar/" + newFilename;
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getId, userId).
                    eq(User::getAvatar, avatarUrl);

            int rows = userMapper.update(null, queryWrapper);

            if (rows > 0) {
                return Result.success(avatarUrl);
            }
            return Result.error(400, "头像上传失败");
        } catch (IOException e) {
            return Result.error(500, "上传失败：" + e.getMessage());
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
     * 4. 查询用户是否存在
     * 5. 如果用户不存在，使用分布式锁防止并发注册同一手机号
     *    - 锁key: lock:user:register:phone:{phone}
     *    - 尝试获取锁（等待3秒，锁持有10秒）
     *    - 双重检查用户是否存在
     *    - 创建新用户并保存
     * 6. 生成 JWT token
     * 7. 构建返回数据（用户信息 + token）
     * 8. 返回成功结果
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

        // 查用户，不存在则自动注册
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
        if (user == null) {
            // 使用分布式锁防止并发注册同一手机号
            String lockKey = "lock:user:register:phone:" + phone;
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean tryLock = lock.tryLock(3, 10, TimeUnit.SECONDS);
                if (!tryLock) {
                    return Result.error(500, "系统繁忙，请稍后重试");
                }
                // Double Check
                User existUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone));
                if (existUser != null) {
                    user = existUser;
                } else {
                    user = new User();
                    user.setPhone(phone);
                    // 生成随机用户名：mall_随机8位字母数字
                    String randomUsername = "mall_" + RandomUtil.randomString(8);
                    user.setUsername(randomUsername);
                    // 生成随机昵称：用户+手机号后4位
                    user.setNickname("用户" + phone.substring(phone.length() - 4));
                    user.setPassword(passwordEncoder.encode(RandomUtil.randomString(12)));
                    user.setStatus(1);
                    user.setIsDeleted(0);
                    user.setCreateTime(LocalDateTime.now());
                    user.setUpdateTime(LocalDateTime.now());
                    userMapper.insert(user);
                    log.info("验证码登录自动注册新用户：phone={}, username={}", phone, randomUsername);
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
            if (user.getStatus() != null && user.getStatus() == 0) {
                return Result.error(403, "账号已被禁用");
            }
            if (user.getIsDeleted() != null && user.getIsDeleted() == 1) {
                return Result.error(403, "账号不存在");
            }
        }

        // 删除已使用的验证码
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {

        }

        // 生成 Token + 返回用户信息（与密码登录保持一致）
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("phone", user.getPhone());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("token", token);
        return Result.success(userInfo);
    }

}
