@echo off
echo ========================================
echo Universal Database Manager
echo Starting Backend and Frontend
echo ========================================
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven from https://maven.apache.org/
    pause
    exit /b 1
)

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or higher from https://adoptium.net/
    pause
    exit /b 1
)

REM Check if Node.js is installed
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Node.js is not installed or not in PATH
    echo Please install Node.js from https://nodejs.org/
    pause
    exit /b 1
)

REM Check if frontend dependencies are installed
if not exist "frontend\node_modules\" (
    echo Frontend dependencies not found. Installing...
    cd frontend
    call install.bat
    cd ..
    echo.
)

echo Starting backend server in a new window...
start "UDB Manager Backend" cmd /k "cd backend && start-backend.bat"

echo Waiting for backend to start...
timeout /t 10 /nobreak >nul

echo Starting frontend server in a new window...
start "UDB Manager Frontend" cmd /k "cd frontend && start-frontend.bat"

echo.
echo ========================================
echo Both servers are starting!
echo ========================================
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:3000
echo.
echo Press any key to close this window...
echo (Backend and Frontend windows will remain open)
echo ========================================
pause >nul
