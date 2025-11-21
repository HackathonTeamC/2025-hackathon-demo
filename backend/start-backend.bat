@echo off
echo ========================================
echo Starting Universal Database Manager Backend
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

echo Starting Spring Boot application...
echo Backend will be available at: http://localhost:8080
echo API documentation at: http://localhost:8080/h2-console
echo.

mvn spring-boot:run

pause
