# Setup Self-Control Android App Structure
$basePath = "app\src\main\java\com\selfcontrol"

# Create all directories
$directories = @(
    "di",
    "presentation\theme",
    "presentation\navigation",
    "presentation\components",
    "presentation\home",
    "presentation\apps",
    "presentation\requests",
    "presentation\violations",
    "presentation\settings",
    "presentation\blocked",
    "domain\model",
    "domain\repository",
    "domain\usecase\app",
    "domain\usecase\policy",
    "domain\usecase\request",
    "domain\usecase\url",
    "domain\usecase\violation",
    "data\local\dao",
    "data\local\entity",
    "data\local\database",
    "data\local\prefs",
    "data\remote\api",
    "data\remote\dto",
    "data\remote\mapper",
    "data\repository",
    "data\worker",
    "deviceowner",
    "service",
    "receiver",
    "util"
)

Write-Host "Creating directory structure..." -ForegroundColor Green

foreach ($dir in $directories) {
    $fullPath = Join-Path $basePath $dir
    if (!(Test-Path $fullPath)) {
        New-Item -ItemType Directory -Path $fullPath -Force | Out-Null
        Write-Host "  Created: $dir" -ForegroundColor Cyan
    } else {
        Write-Host "  Exists: $dir" -ForegroundColor Yellow
    }
}

# Create buildSrc directory structure
$buildSrcPath = "buildSrc\src\main\kotlin"
if (!(Test-Path $buildSrcPath)) {
    New-Item -ItemType Directory -Path $buildSrcPath -Force | Out-Null
    Write-Host "  Created: buildSrc structure" -ForegroundColor Cyan
}

Write-Host "`nDirectory structure created successfully!" -ForegroundColor Green
