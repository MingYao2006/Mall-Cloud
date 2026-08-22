# Sentinel Dashboard 启动与关闭指南

## 一、启动 Sentinel Dashboard

### 1. 基础启动命令（默认端口 8080）

```powershell
java -jar "D:\sentinel-dashboard-1.8.10.jar"
```

### 2. 指定端口启动（推荐）

```powershell
java `-Dserver.port=8081 -jar "D:\sentinel-dashboard-1.8.10.jar"
```

### 3. 完整参数启动

```powershell
java `-Dserver.port=8081 `-Dcsp.sentinel.dashboard.server=localhost:8081 `-Dproject.name=sentinel-dashboard -jar "D:\sentinel-dashboard-1.8.10.jar"
```

**参数说明：**
- `-Dserver.port=8081`：指定控制台端口
- `-Dcsp.sentinel.dashboard.server=localhost:8081`：指定控制台地址
- `-Dproject.name=sentinel-dashboard`：项目名称

### 4. 后台启动（Linux/Mac）

```bash
nohup java -Dserver.port=8081 -jar /path/to/sentinel-dashboard-1.8.10.jar > sentinel.log 2>&1 &
```

### 5. Windows 后台启动（使用 start 命令）

```powershell
start /B java `-Dserver.port=8081 -jar "D:\sentinel-dashboard-1.8.10.jar" > sentinel.log 2>&1
```

## 二、访问 Sentinel Dashboard

启动成功后，打开浏览器访问：

- **地址**：http://localhost:8081
- **账号**：sentinel
- **密码**：sentinel

## 三、关闭 Sentinel Dashboard

### 方法一：使用 Ctrl + C（推荐）

在运行 Sentinel Dashboard 的终端窗口中，按 `Ctrl + C` 组合键，优雅关闭。

### 方法二：查找进程并关闭（Windows PowerShell）

#### 1. 查找 Sentinel Dashboard 进程

```powershell
# 查找占用指定端口的进程
netstat -ano | findstr :8081
```

或者：

```powershell
# 查找 Java 进程
tasklist | findstr java
```

#### 2. 关闭进程

```powershell
# 强制关闭指定进程 ID
taskkill /F /PID <进程ID>
```

**示例：**
```powershell
# 假设进程 ID 为 35680
taskkill /F /PID 35680
```

### 方法三：关闭所有 Java 进程（谨慎使用）

```powershell
# 关闭所有 Java 进程
taskkill /F /IM java.exe
```

**注意：** 此命令会关闭所有 Java 进程，包括其他正在运行的 Java 应用程序。

### 方法四：Linux/Mac 关闭后台进程

#### 1. 查找进程

```bash
ps -ef | grep sentinel-dashboard
```

或者：

```bash
netstat -tulnp | grep 8081
```

#### 2. 关闭进程

```bash
kill -9 <进程ID>
```

## 四、常见问题

### 1. 端口被占用

**错误信息：**
```
Port 8080 was already in use
```

**解决方案：**
- 使用其他端口启动：`-Dserver.port=8081`
- 或者关闭占用端口的进程

**查看端口占用：**
```powershell
netstat -ano | findstr :8080
```

### 2. PowerShell 参数解析错误

**错误信息：**
```
错误: 找不到或无法加载主类 .port=8081
```

**解决方案：**
使用反引号转义：
```powershell
java `-Dserver.port=8081 -jar "D:\sentinel-dashboard-1.8.10.jar"
```

或者使用引号：
```powershell
java "-Dserver.port=8081" -jar "D:\sentinel-dashboard-1.8.10.jar"
```

### 3. 找不到 jar 文件

**错误信息：**
```
Error: Unable to access jarfile sentinel-dashboard-1.8.10.jar
```

**解决方案：**
- 确认 jar 文件路径是否正确
- 使用绝对路径：`"D:\sentinel-dashboard-1.8.10.jar"`

## 五、快速启动脚本

### Windows PowerShell 脚本

创建文件 `start-sentinel.ps1`：

```powershell
# Sentinel Dashboard 启动脚本

$JAR_PATH = "D:\sentinel-dashboard-1.8.10.jar"
$PORT = 8081

Write-Host "正在启动 Sentinel Dashboard..." -ForegroundColor Green
Write-Host "端口: $PORT" -ForegroundColor Yellow
Write-Host "访问地址: http://localhost:$PORT" -ForegroundColor Yellow
Write-Host "账号: sentinel" -ForegroundColor Yellow
Write-Host "密码: sentinel" -ForegroundColor Yellow

java `-Dserver.port=$PORT -jar $JAR_PATH
```

**使用方法：**
```powershell
.\start-sentinel.ps1
```

### Windows 关闭脚本

创建文件 `stop-sentinel.ps1`：

```powershell
# Sentinel Dashboard 关闭脚本

$PORT = 8081

Write-Host "正在查找 Sentinel Dashboard 进程..." -ForegroundColor Yellow

# 查找占用端口的进程
$connections = netstat -ano | findstr ":$PORT"

if ($connections) {
    # 提取进程 ID
    $processId = ($connections -split '\s+')[-1]
    
    Write-Host "找到进程 ID: $processId" -ForegroundColor Yellow
    
    # 关闭进程
    taskkill /F /PID $processId
    
    Write-Host "Sentinel Dashboard 已关闭" -ForegroundColor Green
} else {
    Write-Host "未找到 Sentinel Dashboard 进程" -ForegroundColor Red
}
```

**使用方法：**
```powershell
.\stop-sentinel.ps1
```

## 六、项目配置

### application.yml 配置示例

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8081  # Sentinel Dashboard 地址
        port: 8719                 # 与 Dashboard 通信的端口
      eager: true                  # 应用启动时立即初始化
      datasource:
        flow:                      # 流控规则持久化
          nacos:
            server-addr: localhost:8848
            username: nacos
            password: nacos
            data-id: ${spring.application.name}-flow-rules
            group-id: SENTINEL_GROUP
            data-type: json
            rule-type: flow

# 开启 Feign 对 Sentinel 的支持
feign:
  sentinel:
    enabled: true
```

## 七、日志查看

Sentinel Dashboard 日志默认存储位置：

- **Windows**: `C:\Users\<用户名>\logs\csp\`
- **Linux/Mac**: `/home/<用户名>/logs/csp/`

**查看日志：**
```powershell
Get-Content "C:\Users\$env:USERNAME\logs\csp\sentinel-dashboard.log" -Tail 100
```

## 八、版本信息

- **Sentinel Dashboard 版本**: 1.8.10
- **Spring Boot 版本**: 2.5.12
- **Java 版本要求**: JDK 1.8+

## 九、相关链接

- Sentinel 官方文档：https://sentinelguard.io/zh-cn/
- Sentinel GitHub：https://github.com/alibaba/Sentinel
- 下载地址：https://github.com/alibaba/Sentinel/releases
