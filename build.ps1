# TurtleBot build script for Windows 10/11
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
Set-Location -LiteralPath $PSScriptRoot

Write-Host "=========================================="
Write-Host "TurtleBot - Minecraft 1.20.1 build"
Write-Host "=========================================="

try {
    $java = Get-Command java -ErrorAction Stop
    $javaVersionText = (& java -version 2>&1 | Out-String)
    Write-Host $javaVersionText
    if ($javaVersionText -notmatch 'version "17\.') {
        throw "Java 17 is required. Install Java 17 and try again."
    }

    $gradleVersion = "8.5"
    $gradleRoot = Join-Path $env:USERPROFILE ".gradle\turtlebot-gradle-$gradleVersion"
    $gradleExe = Join-Path $gradleRoot "bin\gradle.bat"

    if (-not (Test-Path -LiteralPath $gradleExe)) {
        $zip = Join-Path $env:TEMP "gradle-$gradleVersion-bin.zip"
        $extractRoot = Join-Path $env:TEMP "turtlebot-gradle-extract"
        $url = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
        Write-Host "Downloading Gradle $gradleVersion..."
        Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
        if (Test-Path $extractRoot) { Remove-Item $extractRoot -Recurse -Force }
        Expand-Archive -LiteralPath $zip -DestinationPath $extractRoot -Force
        $source = Join-Path $extractRoot "gradle-$gradleVersion"
        if (Test-Path $gradleRoot) { Remove-Item $gradleRoot -Recurse -Force }
        New-Item -ItemType Directory -Path (Split-Path $gradleRoot) -Force | Out-Null
        Move-Item -LiteralPath $source -Destination $gradleRoot
        Remove-Item $zip -Force
        Remove-Item $extractRoot -Recurse -Force
    }

    Write-Host "Starting mod build..."
    & $gradleExe --no-daemon build
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE"
    }

    Write-Host ""
    Write-Host "SUCCESS. Check the build\libs folder."
    $libs = Join-Path $PSScriptRoot "build\libs"
    if (Test-Path $libs) { Start-Process explorer.exe $libs }
}
catch {
    Write-Host ""
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
