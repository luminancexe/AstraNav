@echo off
setlocal enabledelayedexpansion

echo ==================================================================
echo   COMPILING ASTRANAV — AEROSPACE NAVIGATION & PATHFINDING ENGINE
echo ==================================================================

if not exist out mkdir out

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"

if %ERRORLEVEL% equ 0 (
    echo.
    echo Build completed successfully.
    echo Run 'run.bat' to start the application.
) else (
    echo.
    echo Build failed.
)
