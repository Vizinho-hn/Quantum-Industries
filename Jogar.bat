@echo off
setlocal
cd /d "%~dp0"
if not defined JAVA_HOME (
    for /d %%J in ("%~dp0.local\java\jdk-25*") do set "JAVA_HOME=%%~fJ"
)
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%~dp0.local\gradle"
call gradlew.bat --max-workers=2 runClient --console=plain
if errorlevel 1 (
    echo.
    echo Nao foi possivel iniciar o jogo. Confira o JDK 25 e run\logs\latest.log.
    pause
)
endlocal
