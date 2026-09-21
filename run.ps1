# PowerShell Run Script
param (
    [switch]$Cli,
    [int]$Port = 8080
)

$outDir = Join-Path $PSScriptRoot "out"
$mainClass = Join-Path $outDir "com\aerospace\flightpath\Main.class"

if (!(Test-Path $mainClass)) {
    Write-Host "[INFO] Binaries not found. Running build.ps1 first..." -ForegroundColor Yellow
    & (Join-Path $PSScriptRoot "build.ps1")
}

if ($Cli) {
    & java -cp $outDir com.aerospace.flightpath.Main --cli
} else {
    Write-Host "==================================================================" -ForegroundColor Cyan
    Write-Host " ✈️  ASTRANAV — AEROSPACE NAVIGATION & PATHFINDING ENGINE" -ForegroundColor Green
    Write-Host " 🌐  Dashboard URL: http://localhost:$Port" -ForegroundColor Cyan
    Write-Host "==================================================================" -ForegroundColor Cyan
    Start-Process "http://localhost:$Port"
    & java -cp $outDir com.aerospace.flightpath.Main --port $Port
}
