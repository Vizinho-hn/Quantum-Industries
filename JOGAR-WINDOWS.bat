@echo off
setlocal
cd /d "%~dp0"
echo ==========================================
echo  Quantum Industries - Atualizar e Jogar
echo ==========================================
where git >nul 2>nul
if errorlevel 1 (
  echo ERRO: Git nao encontrado. Instale o Git for Windows.
  pause
  exit /b 1
)
git rev-parse --is-inside-work-tree >nul 2>nul
if errorlevel 1 (
  echo ERRO: execute a partir de uma pasta clonada com Git.
  pause
  exit /b 1
)
for /f "delims=" %%B in ('git branch --show-current') do set "CURRENT_BRANCH=%%B"
if /I not "%CURRENT_BRANCH%"=="feature/v0.1.1-conductivity-research" (
  echo ERRO: branch atual: %CURRENT_BRANCH%
  echo Execute: git switch feature/v0.1.1-conductivity-research
  pause
  exit /b 1
)
echo Atualizando somente se possivel avancar sem sobrescrever mudancas...
git pull --ff-only
if errorlevel 1 (
  echo Falha ao atualizar. Confira a conexao e as alteracoes locais.
  pause
  exit /b 1
)
echo Iniciando Minecraft com NeoForge (pode baixar dependencias)...
call "%~dp0gradlew.bat" runClient
if errorlevel 1 (
  echo Erro ao iniciar. Verifique o Java 25 e o log acima.
  pause
  exit /b 1
)
pause
