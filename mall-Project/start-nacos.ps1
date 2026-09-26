# 设置 JAVA_HOME 为实际的 JDK 安装路径
$env:JAVA_HOME = "D:\Java\jdk-23.0.2"
Write-Output "JAVA_HOME=$env:JAVA_HOME"

# 启动前检查 8848 端口，避免重复启动导致 Derby 数据库锁冲突 (XSDB6)
$existing = Get-NetTCPConnection -LocalPort 8848 -State Listen -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "[检测] 8848 端口已有 Nacos 实例在运行 (PID: $($existing.OwningProcess -join ', '))，跳过启动。" -ForegroundColor Yellow
    Write-Host "[提示] 访问 http://127.0.0.1:8848/nacos 即可使用；如需重启请先停止该进程。" -ForegroundColor Yellow
    exit 0
}

& "C:\Users\67161\ai-infra\nacos\standalone\nacos-3.2.2\bin\startup.cmd" -m standalone
