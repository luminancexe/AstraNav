@echo off
setlocal

echo ==================================================================
echo   LAUNCHING ASTRANAV — AEROSPACE NAVIGATION & PATHFINDING ENGINE
echo ==================================================================

if not exist out\com\aerospace\flightpath\Main.class (
    echo [INFO] Binaries not found. Running build.bat first...
    call "%~dp0build.bat"
)

if "%1"=="--cli" (
    java -cp out com.aerospace.flightpath.Main --cli
) else (
    echo [INFO] Starting Web Server on http://localhost:8080 ...
    echo [INFO] Press Ctrl+C to terminate.
    start http://localhost:8080
    java -cp out com.aerospace.flightpath.Main --server
)
