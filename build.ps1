# PowerShell build script for Aerospace Flight Path Optimizer
$ErrorActionPreference = "Stop"

$outDir = Join-Path $PSScriptRoot "out"
if (!(Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir | Out-Null
}

$srcDir = Join-Path $PSScriptRoot "src"
$javaFiles = Get-ChildItem -Path $srcDir -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName

Write-Host "Compiling $($javaFiles.Count) Java source files..." -ForegroundColor Cyan

# Write sources.txt with quoted entries for javac
$sourcesFile = Join-Path $PSScriptRoot "sources.txt"
$javaFiles | ForEach-Object { '"' + $_.Replace('\', '/') + '"' } | Set-Content -Path $sourcesFile -Encoding ascii

try {
    & javac -d $outDir "@$sourcesFile"
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Compilation SUCCESSFUL!" -ForegroundColor Green
    } else {
        Write-Host "Compilation FAILED with code $LASTEXITCODE" -ForegroundColor Red
        exit $LASTEXITCODE
    }
} finally {
    if (Test-Path $sourcesFile) {
        Remove-Item $sourcesFile -Force
    }
}
