@echo off
cd /d "%~dp0"

rem Check port 8095
netstat -ano | findstr ":8095" | findstr "LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo [SKIP] Port 8095 already in use, service may already be running.
    exit /b 0
)

rem Check .env
if not exist ".env" (
    echo [ERROR] .env not found. Run: copy .env.example .env and fill keys.
    exit /b 1
)

echo [START] Starting mall-ai-agent on port 8095 ...
start "" /b python -m uvicorn app.main:app --host 0.0.0.0 --port 8095 > uvicorn.log 2>&1

timeout /t 8 /nobreak >nul

netstat -ano | findstr ":8095" | findstr "LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo [OK] Service is running. Health: http://localhost:8095/health
    echo [OK] API docs: http://localhost:8095/docs
) else (
    echo [FAIL] Service did not start. See uvicorn.log:
    type uvicorn.log 2>nul
)
