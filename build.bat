@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

echo ==========================================
echo TurtleBot - сборка мода для Minecraft 1.20.1
echo ==========================================
echo.

where java >nul 2>nul
if errorlevel 1 (
  echo ОШИБКА: Java не найдена.
  echo Установи Java 17 (например, Eclipse Temurin 17), затем запусти build.bat снова.
  pause
  exit /b 1
)

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%v
echo Найдена Java: %JAVA_VERSION%
echo.

set "GRADLE_VERSION=8.5"
set "GRADLE_HOME=%USERPROFILE%\.gradle\turtlebot-gradle-%GRADLE_VERSION%"
set "GRADLE_EXE=%GRADLE_HOME%\bin\gradle.bat"
set "GRADLE_ZIP=%TEMP%\gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%GRADLE_EXE%" (
  echo Gradle %GRADLE_VERSION% не найден. Скачиваю официальный дистрибутив...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_ZIP%'"
  if errorlevel 1 (
    echo ОШИБКА: не удалось скачать Gradle. Проверь подключение к интернету.
    pause
    exit /b 1
  )
  echo Распаковываю Gradle...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%GRADLE_ZIP%' -DestinationPath '%USERPROFILE%\.gradle\turtlebot-temp' -Force"
  if errorlevel 1 (
    echo ОШИБКА: не удалось распаковать Gradle.
    pause
    exit /b 1
  )
  if not exist "%USERPROFILE%\.gradle" mkdir "%USERPROFILE%\.gradle"
  if exist "%GRADLE_HOME%" rmdir /s /q "%GRADLE_HOME%"
  move "%USERPROFILE%\.gradle\turtlebot-temp\gradle-%GRADLE_VERSION%" "%GRADLE_HOME%" >nul
  rmdir /s /q "%USERPROFILE%\.gradle\turtlebot-temp" 2>nul
  del "%GRADLE_ZIP%" 2>nul
)

echo Запускаю сборку...
call "%GRADLE_EXE%" --no-daemon build
if errorlevel 1 (
  echo.
  echo СБОРКА ЗАВЕРШИЛАСЬ С ОШИБКОЙ. Скопируй сюда текст ошибки.
  pause
  exit /b 1
)

echo.
echo ==========================================
echo ГОТОВО! Ищи мод в папке build\libs
echo ==========================================
explorer "%~dp0build\libs"
pause
endlocal
