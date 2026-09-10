param([string]$Javac='C:/Users/Tcarn/Desktop/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21.0.11.v20260515-1531/jre/bin/javac.exe', [string]$Java='C:/Program Files/Java/jre1.8.0_503/bin/java.exe')
$ErrorActionPreference='Stop'
$root=(Resolve-Path "$PSScriptRoot/../..").Path
Push-Location $root
try {
 $sources=Get-Content build/boss-batch2/changed-sources.txt | Sort-Object -Unique
 $tests=@('NPCFoundationRegression','BossBatchOneRegression','CombatFormulaRegression','SpecialShieldRegression','FightCavesInstanceRegression','InstanceLifecycleRegression')
 $argsFile=@($sources | ForEach-Object {'"'+$_+'"'})
 $argsFile+=@(($tests+@('CombatEnhancementsRegression','CombatBalanceRegression')) | ForEach-Object {'"tests/'+$_+'.java"'})
 $argsFile | Set-Content build/boss-batch2/verify-sources.args
 & $Javac --release 8 -encoding windows-1252 -cp 'bin;lib/*' -sourcepath src -d build/boss-batch2 '@build/boss-batch2/verify-sources.args'
 if($LASTEXITCODE -ne 0){throw 'Compilation failed'}
 foreach($suite in $tests){
  & $Java -cp 'build/boss-batch2;bin;lib/*' $suite *> "build/boss-batch2/$suite.log"
  if($LASTEXITCODE -ne 0){Get-Content "build/boss-batch2/$suite.log" -Tail 12;throw "$suite failed"}
  Get-Content "build/boss-batch2/$suite.log" -Tail 1
 }
} finally {Pop-Location}
