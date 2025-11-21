@echo off
echo ========================================
echo Building Universal Database Manager Backend
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

echo Building project with Maven...
mvn clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Build successful!
    echo JAR file created in: target/udb-manager-1.0.0.jar
    echo ========================================
) else (
    echo.
    echo ========================================
    echo Build failed!
    echo ========================================
)

pause
