[CmdletBinding()]
param([switch]$Offline)
$ErrorActionPreference='Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    if (-not $env:JAVA_HOME) {
        $javaRoot=Join-Path (Split-Path (Get-Location) -Parent) '.local\java'
        $jdk=Get-ChildItem $javaRoot -Directory -Filter 'jdk-25*' -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($jdk) { $env:JAVA_HOME=$jdk.FullName }
    }
    if (-not $env:GRADLE_USER_HOME) {
        $cache=Join-Path (Split-Path (Get-Location) -Parent) '.local\gradle'
        if (Test-Path $cache) { $env:GRADLE_USER_HOME=$cache }
    }
    $relative='build/mechanical-verification/runs/'+(Get-Date -Format 'yyyyMMdd-HHmmss')
    $root=Join-Path (Get-Location) $relative
    New-Item -ItemType Directory -Path "$root/world" -Force | Out-Null
    Set-Content "$root/world/eula.txt" 'eula=true' -Encoding ascii
    @'
online-mode=false
server-ip=127.0.0.1
server-port=25578
max-tick-time=0
view-distance=2
simulation-distance=2
spawn-protection=0
gamemode=survival
pause-when-empty-seconds=0
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
'@ | Set-Content "$root/world/server.properties" -Encoding ascii
    [string[]]$extra=@()
    if($Offline){$extra=@('--offline')}
    foreach ($phase in @('write','read')) {
        & .\gradlew.bat @extra --max-workers=2 "-PmechanicalRunDir=$relative" --init-script tests/mechanical.init.gradle runServer --console=plain *> "$root/$phase.log"
        if ($LASTEXITCODE -ne 0) { throw "Gradle falhou: $root/$phase.log" }
        if (-not (Test-Path "$root/result.txt") -or -not ((Get-Content "$root/result.txt" -Raw).StartsWith('PASS:'))) { throw "Teste falhou: $root/result.txt; confira $root/$phase.log" }
        if ($phase -eq 'write' -and -not (Test-Path "$root/restart-ready.txt")) { throw 'Fase de gravação não concluiu' }
    }
    if (-not (Test-Path "$root/complete.txt")) { throw 'Verificação completa não foi produzida' }
    Get-Content "$root/restart-ready.txt","$root/complete.txt"
    Write-Output "Logs e mundo descartável: $root"
} finally { Pop-Location }
