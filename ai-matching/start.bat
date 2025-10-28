@echo off
REM Start Backend Server for Mobile App
REM ====================================

echo ============================================================
echo Elder Care Connect - AI Matching Service
echo ============================================================
echo Starting server for Mobile App connection...
echo.

REM Set environment variables
set PYTHONPATH=D:\CapstoneProject\CapstoneProject\Mobile\capstone-project\backend
echo ✅ PYTHONPATH set to: %PYTHONPATH%

REM Get current IP address (simplified)
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr "IPv4"') do (
    for /f "tokens=1" %%b in ("%%a") do (
        set IP_ADDRESS=%%b
        goto :found_ip
    )
)
:found_ip
echo ✅ Server IP: %IP_ADDRESS%
echo ✅ Mobile App should use: http://%IP_ADDRESS%:8000
echo.

REM Kill any existing Python processes
echo 🔄 Cleaning up existing processes...
taskkill /f /im python.exe >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Cleaned up existing Python processes
) else (
    echo ℹ️  No existing Python processes to clean up
)

echo.
echo 🚀 Starting FastAPI server...
echo    Host: 0.0.0.0 (accepts external connections)
echo    Port: 8000
echo    Mobile URL: http://%IP_ADDRESS%:8000
echo.

REM Start the server
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

REM If error occurs
if %errorlevel% neq 0 (
    echo.
    echo ❌ Error starting server!
    echo Make sure you're in the backend directory and Python is installed.
    echo.
    echo Manual start command:
    echo cd backend
    echo set PYTHONPATH=D:\CapstoneProject\CapstoneProject\Mobile\capstone-project\backend
    echo python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
    echo.
    pause
)
