# Mall AI Shopping Guide Agent 启动脚本
# 依赖: pip install -r requirements.txt
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# 检查 .env 是否已配置
if (-not (Test-Path ".env")) {
    Write-Host "[错误] 缺少 .env 文件，请先执行: copy .env.example .env 并填入 DEEPSEEK_API_KEY 与 EMBEDDING_API_KEY" -ForegroundColor Red
    exit 1
}

# 检查 8095 端口是否已被占用
$existing = Get-NetTCPConnection -LocalPort 8095 -State Listen -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "[检测] 8095 端口已有实例在运行 (PID: $($existing.OwningProcess -join ', '))，跳过启动。" -ForegroundColor Yellow
    Write-Host "[提示] 访问 http://localhost:8095/docs 查看接口文档；如需重启请先停止该进程。" -ForegroundColor Yellow
    exit 0
}

# 清理旧日志
Remove-Item "uvicorn.log", "uvicorn.err" -ErrorAction SilentlyContinue

Write-Host "[启动] mall-ai-agent 服务，端口 8095 ..." -ForegroundColor Green
Write-Host "[日志] 输出到 $scriptDir\uvicorn.log 与 uvicorn.err" -ForegroundColor Green

Start-Process -FilePath "python" `
    -ArgumentList '-m uvicorn app.main:app --host 0.0.0.0 --port 8095' `
    -WorkingDirectory $scriptDir `
    -WindowStyle Hidden `
    -RedirectStandardOutput "uvicorn.log" `
    -RedirectStandardError "uvicorn.err"

Start-Sleep -Seconds 8

# 验证服务是否启动成功
$listener = Get-NetTCPConnection -LocalPort 8095 -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    Write-Host "[成功] 服务已启动，PID: $($listener.OwningProcess -join ', ')" -ForegroundColor Green
    Write-Host "[健康检查] http://localhost:8095/health" -ForegroundColor Green
    Write-Host "[接口文档] http://localhost:8095/docs" -ForegroundColor Green
} else {
    Write-Host "[失败] 服务未成功监听 8095 端口，请查看 uvicorn.err 日志：" -ForegroundColor Red
    if (Test-Path "uvicorn.err") { Get-Content "uvicorn.err" -Tail 20 }
}
