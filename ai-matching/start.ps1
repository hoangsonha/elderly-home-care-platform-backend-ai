# Start Backend Server for Mobile App
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Elder Care Connect - AI Matching Service" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Starting server for Mobile App connection..." -ForegroundColor Yellow
Write-Host ""

# Set environment variables
$env:PYTHONPATH = "D:\CapstoneProject\CapstoneProject\Mobile\capstone-project\backend"
Write-Host "PYTHONPATH set to: $env:PYTHONPATH" -ForegroundColor Green

# Get current IP address
$ipAddress = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object {$_.IPAddress -like "192.168.*"} | Select-Object -First 1).IPAddress
Write-Host "Server IP: $ipAddress" -ForegroundColor Green
Write-Host "Mobile App should use: http://$ipAddress:8000" -ForegroundColor Green
Write-Host ""

# Kill any existing Python processes
Write-Host "Cleaning up existing processes..." -ForegroundColor Yellow
Get-Process python -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Write-Host "Cleaned up existing Python processes" -ForegroundColor Green

Write-Host ""
Write-Host "Starting FastAPI server..." -ForegroundColor Yellow
Write-Host "Host: 0.0.0.0 (accepts external connections)" -ForegroundColor White
Write-Host "Port: 8000" -ForegroundColor White
Write-Host "Mobile URL: http://$ipAddress:8000" -ForegroundColor White
Write-Host ""

# Start the server
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload