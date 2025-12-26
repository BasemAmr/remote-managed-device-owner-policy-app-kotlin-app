# Create Empty Kotlin Files for Self-Control Android App
$basePath = "app\src\main\java\com\selfcontrol"

# Define all files to create
$files = @{
    # Root
    "" = @("SelfControlApp.kt")
    
    # DI
    "di" = @(
        "AppModule.kt",
        "NetworkModule.kt",
        "DatabaseModule.kt",
        "RepositoryModule.kt",
        "WorkerModule.kt",
        "DeviceOwnerModule.kt"
    )
    
    # Presentation - Theme
    "presentation\theme" = @(
        "Color.kt",
        "Theme.kt",
        "Type.kt"
    )
    
    # Presentation - Navigation
    "presentation\navigation" = @(
        "NavGraph.kt",
        "Screen.kt",
        "NavigationActions.kt"
    )
    
    # Presentation - Components
    "presentation\components" = @(
        "AppCard.kt",
        "LoadingDialog.kt",
        "ErrorScreen.kt",
        "ConfirmDialog.kt",
        "EmptyState.kt",
        "TopAppBar.kt"
    )
    
    # Presentation - Home
    "presentation\home" = @(
        "HomeScreen.kt",
        "HomeViewModel.kt",
        "HomeState.kt",
        "HomeEvent.kt"
    )
    
    # Presentation - Apps
    "presentation\apps" = @(
        "AppsScreen.kt",
        "AppsViewModel.kt",
        "AppsState.kt",
        "AppDetailsScreen.kt",
        "AppBlockToggle.kt",
        "AppSearch.kt"
    )
    
    # Presentation - Requests
    "presentation\requests" = @(
        "RequestsScreen.kt",
        "RequestsViewModel.kt",
        "RequestsState.kt",
        "CreateRequestScreen.kt",
        "RequestCountdownTimer.kt"
    )
    
    # Presentation - Violations
    "presentation\violations" = @(
        "ViolationsScreen.kt",
        "ViolationsViewModel.kt",
        "ViolationsState.kt"
    )
    
    # Presentation - Settings
    "presentation\settings" = @(
        "SettingsScreen.kt",
        "SettingsViewModel.kt",
        "SettingsState.kt"
    )
    
    # Presentation - Blocked
    "presentation\blocked" = @(
        "BlockedScreen.kt",
        "BlockedViewModel.kt"
    )
    
    # Domain - Model
    "domain\model" = @(
        "App.kt",
        "AppPolicy.kt",
        "UrlBlacklist.kt",
        "Request.kt",
        "Violation.kt",
        "DeviceSettings.kt",
        "Result.kt"
    )
    
    # Domain - Repository
    "domain\repository" = @(
        "AppRepository.kt",
        "PolicyRepository.kt",
        "RequestRepository.kt",
        "ViolationRepository.kt",
        "UrlRepository.kt",
        "SettingsRepository.kt"
    )
    
    # Domain - UseCase - App
    "domain\usecase\app" = @(
        "GetInstalledAppsUseCase.kt",
        "GetBlockedAppsUseCase.kt",
        "CheckAppAllowedUseCase.kt",
        "SyncAppPoliciesUseCase.kt"
    )
    
    # Domain - UseCase - Policy
    "domain\usecase\policy" = @(
        "ApplyPolicyUseCase.kt",
        "EnforcePolicyUseCase.kt",
        "GetActivePoliciesUseCase.kt"
    )
    
    # Domain - UseCase - Request
    "domain\usecase\request" = @(
        "CreateAccessRequestUseCase.kt",
        "GetPendingRequestsUseCase.kt",
        "CheckRequestStatusUseCase.kt"
    )
    
    # Domain - UseCase - URL
    "domain\usecase\url" = @(
        "CheckUrlBlockedUseCase.kt",
        "SyncUrlBlacklistUseCase.kt"
    )
    
    # Domain - UseCase - Violation
    "domain\usecase\violation" = @(
        "LogViolationUseCase.kt",
        "GetViolationsUseCase.kt"
    )
    
    # Data - Local - DAO
    "data\local\dao" = @(
        "AppDao.kt",
        "PolicyDao.kt",
        "UrlDao.kt",
        "RequestDao.kt",
        "ViolationDao.kt",
        "SettingsDao.kt"
    )
    
    # Data - Local - Entity
    "data\local\entity" = @(
        "AppEntity.kt",
        "PolicyEntity.kt",
        "UrlEntity.kt",
        "RequestEntity.kt",
        "ViolationEntity.kt",
        "SettingsEntity.kt"
    )
    
    # Data - Local - Database
    "data\local\database" = @(
        "SelfControlDatabase.kt",
        "DatabaseCallback.kt"
    )
    
    # Data - Local - Prefs
    "data\local\prefs" = @(
        "AppPreferences.kt"
    )
    
    # Data - Remote - API
    "data\remote\api" = @(
        "SelfControlApi.kt",
        "AuthInterceptor.kt",
        "NetworkInterceptor.kt"
    )
    
    # Data - Remote - DTO
    "data\remote\dto" = @(
        "DeviceDto.kt",
        "PolicyDto.kt",
        "RequestDto.kt",
        "ViolationDto.kt",
        "ResponseWrapper.kt"
    )
    
    # Data - Remote - Mapper
    "data\remote\mapper" = @(
        "PolicyMapper.kt",
        "RequestMapper.kt",
        "ViolationMapper.kt"
    )
    
    # Data - Repository
    "data\repository" = @(
        "AppRepositoryImpl.kt",
        "PolicyRepositoryImpl.kt",
        "RequestRepositoryImpl.kt",
        "ViolationRepositoryImpl.kt",
        "UrlRepositoryImpl.kt",
        "SettingsRepositoryImpl.kt"
    )
    
    # Data - Worker
    "data\worker" = @(
        "PolicySyncWorker.kt",
        "RequestCheckWorker.kt",
        "ViolationUploadWorker.kt",
        "HeartbeatWorker.kt",
        "UrlBlacklistSyncWorker.kt",
        "CustomWorkerFactory.kt"
    )
    
    # Device Owner
    "deviceowner" = @(
        "DeviceOwnerReceiver.kt",
        "DeviceOwnerManager.kt",
        "AppBlockManager.kt",
        "AccessibilityService.kt",
        "PackageMonitor.kt",
        "UrlFilterService.kt",
        "AccessibilityHelpers.kt"
    )
    
    # Service
    "service" = @(
        "MonitoringService.kt",
        "PolicyEnforcementService.kt",
        "VpnService.kt",
        "NotificationManager.kt"
    )
    
    # Receiver
    "receiver" = @(
        "BootReceiver.kt",
        "PackageChangeReceiver.kt",
        "ScreenStateReceiver.kt",
        "AdminStatusReceiver.kt"
    )
    
    # Util
    "util" = @(
        "Constants.kt",
        "Extensions.kt",
        "Logger.kt",
        "NetworkUtil.kt",
        "DateUtil.kt",
        "CrashReportingTree.kt",
        "SecurityUtil.kt",
        "PermissionUtil.kt"
    )
}

Write-Host "Creating Kotlin files..." -ForegroundColor Green
$totalFiles = 0

foreach ($dir in $files.Keys) {
    $dirPath = if ($dir -eq "") { $basePath } else { Join-Path $basePath $dir }
    
    foreach ($file in $files[$dir]) {
        $filePath = Join-Path $dirPath $file
        
        if (!(Test-Path $filePath)) {
            # Create empty file with package declaration
            $packageName = if ($dir -eq "") {
                "package com.selfcontrol"
            } else {
                $packagePath = $dir.Replace("\", ".")
                "package com.selfcontrol.$packagePath"
            }
            
            $content = "$packageName`n`n// TODO: Implement $file`n"
            Set-Content -Path $filePath -Value $content -Encoding UTF8
            
            $displayPath = if ($dir -eq "") { $file } else { "$dir\$file" }
            Write-Host "  Created: $displayPath" -ForegroundColor Cyan
            $totalFiles++
        } else {
            Write-Host "  Exists: $dir\$file" -ForegroundColor Yellow
        }
    }
}

Write-Host "`n✅ Created $totalFiles Kotlin files successfully!" -ForegroundColor Green
