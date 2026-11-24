$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$runRoot = 'build/electric-sawmill-verification/' + (Get-Date -Format 'yyyyMMdd-HHmmss')
New-Item -ItemType Directory -Force "$runRoot/world" | Out-Null
'eula=true' | Set-Content "$runRoot/world/eula.txt"
@"
online-mode=false
server-ip=127.0.0.1
server-port=25585
max-tick-time=0
view-distance=2
simulation-distance=2
spawn-protection=0
pause-when-empty-seconds=0
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1}],"biome":"minecraft:plains"}
"@ | Set-Content "$runRoot/world/server.properties"
foreach ($phase in @('write','read','chunk')) {
    & ./gradlew.bat --offline --max-workers=2 "-PelectricSawRunDir=$runRoot" "-PelectricSawPhase=$phase" --init-script tests/electric-sawmill.init.gradle runServer --console=plain *> "$runRoot/$phase.log"
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $runRoot/$phase.log" }
    $result = if ($phase -eq 'write') { "$runRoot/result.txt" } else { "$runRoot/result-$phase.txt" }
    if (!(Test-Path $result) -or !(Select-String -Path $result -Pattern '^PASS:' -Quiet)) { throw "Failed verification: $result" }
    Get-Content $result
}
if (Select-String -Path "$runRoot/classes-*.log" -Pattern '\s(net\.minecraft\.client\.|br\.com\.quantumindustries\.client\.)' -Quiet) { throw 'Client class loaded on dedicated server' }
Write-Output "Evidence: $runRoot"
