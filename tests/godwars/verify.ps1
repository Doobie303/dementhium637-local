param([string]$Javac='C:/Users/Tcarn/Desktop/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21.0.11.v20260515-1531/jre/bin/javac.exe',[string]$Java='C:/Program Files/Java/jre1.8.0_503/bin/java.exe')
$ErrorActionPreference='Stop'
Push-Location (Resolve-Path "$PSScriptRoot/../..")
try {
 $tests=@('GodWarsRegression','BossBatchOneRegression','NPCFoundationRegression','FightCavesInstanceRegression','InstanceLifecycleRegression')
 $sources=@(Get-Content build/godwars/changed-sources.txt | Where-Object {$_} | Sort-Object -Unique)
 (@($sources | ForEach-Object {'"'+$_+'"'})+@($tests | ForEach-Object {'"tests/'+$_+'.java"'})) | Set-Content build/godwars/verify-sources.args
 & $Javac --release 8 -encoding windows-1252 -cp 'bin;lib/*' -sourcepath src -d build/godwars '@build/godwars/verify-sources.args'
 if($LASTEXITCODE -ne 0){throw 'Compile failed'}
 foreach($suite in $tests){& $Java -cp 'build/godwars;bin;lib/*' $suite *> "build/godwars/$suite.log";if($LASTEXITCODE -ne 0){Get-Content "build/godwars/$suite.log" -Tail 10;throw "$suite failed"};Get-Content "build/godwars/$suite.log" -Tail 1}
} finally {Pop-Location}
