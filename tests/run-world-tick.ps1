param(
    [string]$SourceRoot,
    [string]$OutputRoot,
    [ValidateSet('baseline','fixed')][string]$Expected='fixed',
    [string[]]$Scenarios=@('normal','slow','interruptible','blocked','cpu','failure','shutdown','async','combat-8','combat-32','combat-god32','combat-nex','native'),
    [ValidateRange(1,3)][int]$Repeat=1,
    [string]$Javac='C:/Users/Tcarn/Desktop/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21.0.11.v20260515-1531/jre/bin/javac.exe',
    [string]$Java='C:/Program Files/Java/jre1.8.0_503/bin/java.exe'
)
$ErrorActionPreference='Stop'
$tickRepo=(Resolve-Path "$PSScriptRoot/..").Path
if(!$SourceRoot){$SourceRoot=Join-Path $tickRepo 'src'}
if(!$OutputRoot){$OutputRoot=Join-Path $tickRepo ('build/world-tick-'+[guid]::NewGuid().ToString('N'))}
$SourceRoot=(Resolve-Path -LiteralPath $SourceRoot).Path
$OutputRoot=[IO.Path]::GetFullPath($OutputRoot)
$tickBuild=[IO.Path]::GetFullPath((Join-Path $tickRepo 'build'))+[IO.Path]::DirectorySeparatorChar
if(!$OutputRoot.StartsWith($tickBuild,[StringComparison]::OrdinalIgnoreCase)){throw 'Output must be inside repository build/'}
if(Test-Path -LiteralPath $OutputRoot){throw 'Use a new output directory; existing evidence is preserved'}
# No server startup, account copies, listeners, live bin, or release tooling.
foreach($dir in @('classes','native-classes','lib','fixture/data/cache','fixture/data/objects','fixture/NDE','fixture/settings')) {
    New-Item -ItemType Directory -Force (Join-Path $OutputRoot $dir) | Out-Null
}
Copy-Item -LiteralPath $SourceRoot -Destination (Join-Path $OutputRoot 'source') -Recurse
Copy-Item "$tickRepo/lib/*.jar" -Destination "$OutputRoot/lib"
Copy-Item "$tickRepo/data/cache/main_file_cache.*" -Destination "$OutputRoot/fixture/data/cache"
foreach($dir in @('item','npcs','xml','custom')) {
    Copy-Item -LiteralPath "$tickRepo/data/$dir" -Destination "$OutputRoot/fixture/data/$dir" -Recurse
}
Copy-Item "$tickRepo/data/objects/packedKeys.bin" -Destination "$OutputRoot/fixture/data/objects"
Copy-Item "$tickRepo/data/packetHandlers.ini" -Destination "$OutputRoot/fixture/data"
Copy-Item "$tickRepo/NDE/NPCDefinitions.bin" -Destination "$OutputRoot/fixture/NDE"
Copy-Item "$tickRepo/settings/instances.properties" -Destination "$OutputRoot/fixture/settings"
$tickSource="$OutputRoot/source/org/dementhium/ServerThread.java"
Copy-Item -LiteralPath $tickSource -Destination "$OutputRoot/ServerThread.java"
$tickText=[IO.File]::ReadAllText($tickSource)
$tickFactory='Executors.newScheduledThreadPool(1, new DementhiumThreadFactory("GameLogic", Thread.MAX_PRIORITY))'
$tickMatches=[regex]::Matches($tickText,[regex]::Escape($tickFactory)).Count
if($tickMatches -lt 1 -or $tickMatches -gt 2){throw 'Review changed executor factory before instrumenting'}
# Only substitute the factory. The subclass retains ScheduledThreadPoolExecutor semantics;
# its runnable takes the same reentrant World monitor to measure entry versus worker start.
[IO.File]::WriteAllText($tickSource,$tickText.Replace($tickFactory,'org.dementhium.WorldTickProbe.executor()'),[Text.Encoding]::GetEncoding(1252))
$tickSources=@("$tickRepo/tests/WorldTickSchedulingRegression.java","$tickRepo/tests/support/WorldTickProbe.java")
$tickSources+=@(Get-ChildItem "$OutputRoot/source/org/dementhium/net/packethandlers/*.java" | ForEach-Object FullName)
$tickSources+=@(Get-ChildItem "$OutputRoot/source/org/dementhium/model/npc/impl/*.java" | ForEach-Object FullName)
$tickSources | ForEach-Object {'"'+($_ -replace '\\','/')+'"'} | Set-Content "$OutputRoot/sources.args"
& $Javac --release 8 -encoding windows-1252 -cp "$OutputRoot/lib/*" -sourcepath "$OutputRoot/source;$tickRepo/tests;$tickRepo/tests/support" -d "$OutputRoot/classes" ('@'+"$OutputRoot/sources.args") *> "$OutputRoot/compile.log"
if($LASTEXITCODE -ne 0){Get-Content "$OutputRoot/compile.log" -Tail 30;throw 'Isolated probe compilation failed'}
# The native control loads the exact saved scheduler, without executor or monitor instrumentation.
& $Javac --release 8 -encoding windows-1252 -cp "$OutputRoot/classes;$OutputRoot/lib/*" -sourcepath "$OutputRoot/native-classes" -d "$OutputRoot/native-classes" "$OutputRoot/ServerThread.java" *> "$OutputRoot/native-compile.log"
if($LASTEXITCODE -ne 0){throw 'Native scheduler compilation failed'}
Push-Location "$OutputRoot/fixture"
try {
    for($trial=1;$trial -le $Repeat;$trial++) {
        foreach($scenario in $Scenarios) {
            $label="$scenario-$trial"
            $cp="$OutputRoot/classes;$OutputRoot/lib/*"
            if($scenario -eq 'native'){$cp="$OutputRoot/native-classes;$cp"}
            # The parent bounds each child JVM as well as the Java latch/thread timeouts.
            $arguments=@('-Xms128m','-Xmx512m','-cp',('"'+$cp+'"'),'WorldTickSchedulingRegression',$scenario,$Expected,('"'+"$OutputRoot/$label.csv"+'"'))
            $process=Start-Process -FilePath $Java -ArgumentList $arguments -WorkingDirectory "$OutputRoot/fixture" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$OutputRoot/$label.log" -RedirectStandardError "$OutputRoot/$label.err"
            if(!$process.WaitForExit(45000)){$process.Kill();$process.WaitForExit();throw "Probe exceeded 45 seconds: $label"}
            Get-Content "$OutputRoot/$label.log" -Tail 6
            $result=Get-Content "$OutputRoot/$label.log" -Raw
            $errors=Get-Content "$OutputRoot/$label.err" -Raw
            if($process.ExitCode -ne 0){if($errors){Write-Output $errors};throw "Probe failed: $label"}
            if($result -notmatch '(?m)^PASS '){throw "Probe exited without completing assertions: $label"}
            if($errors -and $scenario -ne 'failure'){Write-Output $errors;throw "Unexpected stderr: $label"}
            if($scenario -eq 'failure' -and $errors -notmatch '^java.lang.IllegalStateException: expected isolated ordinary task failure') {
                throw 'Ordinary task failure was not reported'
            }
        }
    }
} finally {Pop-Location}
Write-Output "Evidence: $OutputRoot"
