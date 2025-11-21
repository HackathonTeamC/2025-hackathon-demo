@echo off
echo ========================================
echo Starting Universal Database Manager Frontend
echo ========================================
echo.

REM Check if Node.js is installed
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Node.js is not installed or not in PATH
    echo Please install Node.js from https://nodejs.org/
    pause
    exit /b 1
)

REM Check if node_modules exists
if not exist "node_modules\" (
    echo ERROR: Dependencies not installed
    echo Please run install.bat first
    pause
    exit /b 1
)

echo Starting React development server...
echo Frontend will be available at: http://localhost:3000
echo.

npm start

pause
