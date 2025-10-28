# PowerShell script để chạy server nhanh
# Chạy: .\run.ps1

Write-Host "🚀 Starting Elder Care AI Matching Service..." -ForegroundColor Green
Write-Host ""

# Check if venv exists
if (Test-Path "venv") {
    Write-Host "📦 Activating virtual environment..." -ForegroundColor Cyan
    & ".\venv\Scripts\Activate.ps1"
} else {
    Write-Host "⚠️ No virtual environment found. Using system Python..." -ForegroundColor Yellow
    Write-Host "💡 Make sure you have installed dependencies: pip install -r requirements.txt" -ForegroundColor Yellow
    Write-Host ""
}

# Run server
Write-Host "🔥 Starting FastAPI server..." -ForegroundColor Cyan
Write-Host "🌐 Server will be available at: http://localhost:8000" -ForegroundColor Green
Write-Host "📚 API Documentation: http://localhost:8000/docs" -ForegroundColor Green
Write-Host ""
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
