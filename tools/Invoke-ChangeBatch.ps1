[CmdletBinding()]
param(
 [Parameter(Mandatory)][ValidateSet('Prepare','Debug','Verify','Stage')][string]$Phase,
 [Parameter(Mandatory)][string]$Manifest,
 [string[]]$Suite=@(),
 [string]$Javac=$env:CODEX_JAVAC,
 [string]$Java=$env:CODEX_JAVA,
 # Isolated integration fixtures may use a child workspace; never an external tree.
 [string]$WorkspaceRoot=(Split-Path $PSScriptRoot -Parent)
)
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$repo=[IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$root=[IO.Path]::GetFullPath($WorkspaceRoot).TrimEnd('\','/')
if($root -ne $repo -and !$root.StartsWith($repo+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Workspace must be the repository or its child fixture'}
function SafePath([string]$relative){
 if(!$relative -or [IO.Path]::IsPathRooted($relative) -or $relative -match '[\r\n";:]'){throw "Invalid relative path: $relative"}
 $path=[IO.Path]::GetFullPath((Join-Path $root $relative))
 if(!$path.StartsWith($root+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw "Path escapes workspace: $relative"}
 for($p=$path;$p -and $p -ne $repo;$p=Split-Path $p -Parent){
  if((Test-Path -LiteralPath $p) -and ((Get-Item -LiteralPath $p -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)){throw "Reparse point not allowed: $p"}
 }
 return $path
}
function Hash([string]$p){
 try{return (Get-FileHash -LiteralPath $p -Algorithm SHA256 -ErrorAction Stop).Hash}
 catch [System.IO.IOException]{
  # Java may retain a read/write handle for a read-only mapped dependency. Request
  # only read access; keep content comparison and reject changes during this read.
  $before=Get-Item -LiteralPath $p
  $length=$before.Length;$writeTicks=$before.LastWriteTimeUtc.Ticks
  $stream=[IO.File]::Open($p,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::ReadWrite)
  try{$value=(Get-FileHash -InputStream $stream -Algorithm SHA256).Hash}finally{$stream.Dispose()}
  $after=Get-Item -LiteralPath $p
  if($after.Length -ne $length -or $after.LastWriteTimeUtc.Ticks -ne $writeTicks){throw "File changed during hashing: $p"}
  return $value
 }
}
function WriteJson($value,[string]$path){$value | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $path -Encoding UTF8}
function ReadJson([string]$path){Get-Content -LiteralPath $path -Raw | ConvertFrom-Json}
function Snapshot($paths){@($paths | Sort-Object -Unique | ForEach-Object {if(!(Test-Path -LiteralPath $_ -PathType Leaf)){throw "Missing input: $_"};[pscustomobject]@{Path=$_;SHA256=(Hash $_)}})}
function AssertHashes($rows){foreach($r in $rows){if(!(Test-Path -LiteralPath $r.Path -PathType Leaf) -or (Hash $r.Path) -ne $r.SHA256){throw "Changed or missing validated file: $($r.Path)"}}}
function RuntimeFiles {
 @('bin','lib') | ForEach-Object {$dir=SafePath $_;if(Test-Path -LiteralPath $dir){Get-ChildItem -LiteralPath $dir -Recurse -File | ForEach-Object {SafePath ($_.FullName.Substring($root.Length+1))}}}
}
function AssertRuntime($rows){
 $current=@(RuntimeFiles | Sort-Object);$expected=@($rows | ForEach-Object Path | Sort-Object)
 if(($current -join "`n") -cne ($expected -join "`n")){throw 'Runtime dependency file set changed since verification'}
 AssertHashes $rows
}
function ResolveJavaTool([string]$explicit,[string]$name){
 if($explicit){if(!(Test-Path -LiteralPath $explicit -PathType Leaf)){throw "Missing $name executable: $explicit"};return [IO.Path]::GetFullPath($explicit)}
 $command=Get-Command ($name+'.exe') -ErrorAction SilentlyContinue
 if($command){return $command.Source}
 throw "Set -$name or CODEX_$($name.ToUpper()) to the installed executable. See tools/README.md."
}
function CompiledFamily([string]$directory,[string]$source){
 $relative=$source.Substring(4);$dir=Split-Path $relative;$name=[IO.Path]::GetFileNameWithoutExtension($relative)
 $familyDir=Join-Path $directory $dir
 if(!(Test-Path -LiteralPath (Join-Path $familyDir ($name+'.class')) -PathType Leaf)){throw "Missing compiled class family: $source"}
 @(Get-ChildItem -LiteralPath $familyDir -Filter '*.class' -File | Where-Object {$_.BaseName -ceq $name -or $_.BaseName.StartsWith($name+'$',[StringComparison]::Ordinal)})
}
function IsStagedClass([string]$relative){
 $relative=$relative.Replace('\','/')
 foreach($source in $m.stageSources){
  $stem=$source.Substring(4,$source.Length-9)
  if($relative -ceq ($stem+'.class') -or ($relative.StartsWith($stem+'$',[StringComparison]::Ordinal) -and $relative.EndsWith('.class',[StringComparison]::Ordinal))){return $true}
 }
 return $false
}
$manifestPath=SafePath $Manifest
$m=ReadJson $manifestPath
if($m.name -notmatch '^[a-z0-9][a-z0-9-]{0,79}$'){throw 'Batch name must be a short lowercase slug'}
if(!@($m.sources).Count -or !@($m.stageSources).Count -or !@($m.suites).Count){throw 'Declare sources, stageSources and selected suites'}
if([string]::IsNullOrWhiteSpace($m.scope)){throw 'Document the batch scope'}
$sources=@($m.sources | ForEach-Object {if($_ -notmatch '^(src|tests)/.+\.java$'){throw "Invalid Java source: $_"};SafePath $_})
if(@($sources | Sort-Object -Unique).Count -ne $sources.Count){throw 'Duplicate sources'}
foreach($s in $m.stageSources){if($s -notmatch '^src/.+\.java$' -or $m.sources -notcontains $s){throw "Staged source must be explicitly compiled: $s"}}
if(@($m.stageSources | Sort-Object -Unique).Count -ne @($m.stageSources).Count){throw 'Duplicate staged sources'}
# Class-family matching must use the same resolved path as compilation/staging.
$m.stageSources=@($m.stageSources | ForEach-Object {
 $canonical=(SafePath $_).Substring($root.Length+1).Replace('\','/')
 if(!$canonical.StartsWith('src/',[StringComparison]::OrdinalIgnoreCase)){throw 'Staged source resolves outside src'}
 'src/'+$canonical.Substring(4)
})
$suiteNames=@();foreach($definition in $m.suites){
 if($definition.class -notmatch '^[A-Za-z_$][\w$]*(\.[A-Za-z_$][\w$]*)*$' -or [string]::IsNullOrWhiteSpace($definition.reason) -or [string]::IsNullOrWhiteSpace($definition.successPattern)){throw 'Each suite needs class, reason and successPattern'}
 if($suiteNames -contains $definition.class){throw 'Duplicate suite'};$suiteNames+=$definition.class
}
if($Phase -eq 'Debug'){
 if(!$Suite.Count){throw 'Debug requires -Suite with one or more manifest suite names'}
 if(@($Suite | Sort-Object -Unique).Count -ne $Suite.Count){throw 'Duplicate diagnostic suite'}
 foreach($name in $Suite){if($suiteNames -cnotcontains $name){throw "Unknown diagnostic suite: $name"}}
}elseif($Suite.Count){throw '-Suite is allowed only with Debug; Verify always runs the full manifest selection'}
$inputs=@($m.inputs | ForEach-Object {SafePath $_})
$batch=SafePath ('build/batches/'+$m.name)
$state=Join-Path $batch 'prepared.json'
Push-Location $root
try {
 if($Phase -eq 'Prepare'){
  if(Test-Path -LiteralPath $batch){throw 'Batch already exists; use a new name (backups are never overwritten)'}
  New-Item -ItemType Directory -Path $batch | Out-Null
  $before=@();foreach($p in @($sources+$inputs | Sort-Object -Unique)){
   $exists=Test-Path -LiteralPath $p -PathType Leaf;$dest=Join-Path $batch ('before/source/'+$p.Substring($root.Length+1))
   if($exists){New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null;Copy-Item -LiteralPath $p -Destination $dest;if((Hash $p) -ne (Hash $dest)){throw 'Source backup mismatch'}}
   $before+=[pscustomobject]@{Path=$p;Existed=$exists;Backup=$dest;SHA256=$(if($exists){Hash $dest}else{$null})}
  }
  WriteJson ([pscustomobject]@{ManifestSHA256=(Hash $manifestPath);Before=$before}) $state
  Write-Output "Prepared $($m.name). Source backups preserved; runtime unchanged."
  return
 }
 $prepared=ReadJson $state
 if((Hash $manifestPath) -ne $prepared.ManifestSHA256){throw 'Manifest changed after Prepare; use a new batch'}
 foreach($b in $prepared.Before){if($b.Existed -and (!(Test-Path -LiteralPath $b.Backup) -or (Hash $b.Backup) -ne $b.SHA256)){throw 'Source backup altered'}}
 $receipt=Join-Path $batch 'verified.json';$started=Join-Path $batch 'stage-started.json'
 if(Test-Path -LiteralPath $started){throw 'Stage already attempted; inspect staged.json or stage-failed.txt before further work'}
 if($Phase -eq 'Verify' -or $Phase -eq 'Debug'){
  # Diagnostic runs invalidate previous release approval and never write a staging receipt.
  if(Test-Path -LiteralPath $receipt){Remove-Item -LiteralPath $receipt}
  $javacPath=ResolveJavaTool $Javac 'javac';$javaPath=ResolveJavaTool $Java 'java'
  $attempt=Join-Path $batch ($Phase.ToLower()+'-'+[guid]::NewGuid().ToString('N'))
  $classes=Join-Path $attempt 'classes';$empty=Join-Path $attempt 'empty-sourcepath'
  $candidate=Join-Path $attempt 'runtime-candidate';$testClasses=Join-Path $attempt 'test-classes'
  New-Item -ItemType Directory -Path $classes,$empty,$candidate,$testClasses | Out-Null
  $selected=@($m.suites | Where-Object {$Phase -eq 'Verify' -or $Suite -ccontains $_.class})
  $totalWatch=[Diagnostics.Stopwatch]::StartNew()
  $summary=[ordered]@{Mode=$Phase;Status='Failed';SelectedSuites=@($selected | ForEach-Object class);CompileSeconds=0;CandidateSeconds=0;SuiteSeconds=0;TotalSeconds=0;Suites=@();Failure=$null}
  $results=@();$activeLog=$null
  try {
   $baseline=Snapshot (@($sources)+@($inputs)+@($manifestPath,$PSCommandPath,$javacPath,$javaPath))
   $runtime=Snapshot @(RuntimeFiles)
   $argsPath=Join-Path $attempt 'sources.args'
   $productionSources=@($m.sources | Where-Object {$_ -match '^src/'} | ForEach-Object {SafePath $_})
   $testSources=@($m.sources | Where-Object {$_ -match '^tests/'} | ForEach-Object {SafePath $_})
   $productionSources | ForEach-Object {'"'+$_.Replace('\','/')+'"'} | Set-Content -LiteralPath $argsPath -Encoding UTF8
   # Compile declared production sources, but test only the class families that will be staged.
   $activeLog=Join-Path $attempt 'compile.log';$watch=[Diagnostics.Stopwatch]::StartNew()
   & $javacPath --release 8 -encoding windows-1252 -cp 'bin;lib/*' -sourcepath $empty -implicit:none -d $classes ('@'+$argsPath) *> $activeLog
   $watch.Stop();$summary.CompileSeconds=[Math]::Round($watch.Elapsed.TotalSeconds,3)
   if($LASTEXITCODE -ne 0){throw "Compilation failed; see $activeLog"}
   # Preserve unchanged runtime dependencies, excluding every old member of replaced families.
   # Keeping bin on the test classpath would expose obsolete inner classes that Stage removes.
   $watch.Restart()
   try {
   $binPrefix=(SafePath 'bin')+[IO.Path]::DirectorySeparatorChar
   foreach($row in $runtime){
    if(!$row.Path.StartsWith($binPrefix,[StringComparison]::OrdinalIgnoreCase)){continue}
    $relative=$row.Path.Substring($binPrefix.Length)
    if(IsStagedClass $relative){continue}
    $dest=Join-Path $candidate $relative
    New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null
    Copy-Item -LiteralPath $row.Path -Destination $dest
    if((Hash $dest) -ne $row.SHA256){throw 'Candidate dependency copy mismatch'}
   }
   foreach($source in $m.stageSources){
    foreach($file in @(CompiledFamily $classes $source)){
     $dest=Join-Path $candidate $file.FullName.Substring($classes.Length+1)
     New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null
     Copy-Item -LiteralPath $file.FullName -Destination $dest
    }
   }
   }finally{$watch.Stop();$summary.CandidateSeconds=[Math]::Round($watch.Elapsed.TotalSeconds,3)}
   if($testSources.Count){
    $testArgs=Join-Path $attempt 'tests.args'
    $testSources | ForEach-Object {'"'+$_.Replace('\','/')+'"'} | Set-Content -LiteralPath $testArgs -Encoding UTF8
    $watch.Restart()
    & $javacPath --release 8 -encoding windows-1252 -cp ($candidate+';lib/*') -sourcepath $empty -implicit:none -d $testClasses ('@'+$testArgs) *>> $activeLog
    $watch.Stop();$summary.CompileSeconds+=[Math]::Round($watch.Elapsed.TotalSeconds,3)
    if($LASTEXITCODE -ne 0){throw "Compilation failed against staging candidate; see $activeLog"}
   }
   foreach($testClass in @(Get-ChildItem -LiteralPath $testClasses -Recurse -File -Filter '*.class')){
    $relative=$testClass.FullName.Substring($testClasses.Length+1)
    if(Test-Path -LiteralPath (Join-Path $candidate $relative) -PathType Leaf){
     throw "Test class shadows candidate runtime class: $relative. Keep test-only classes out of the runtime."
    }
   }
   foreach($entry in $selected){
    $mainPath=$entry.class.Replace('.','/')+'.class'
    if(!(Test-Path -LiteralPath (Join-Path $testClasses $mainPath) -PathType Leaf) -and !((IsStagedClass $mainPath) -and (Test-Path -LiteralPath (Join-Path $classes $mainPath) -PathType Leaf))){
     throw "Suite was not freshly compiled for this candidate: $($entry.class). Declare its source; bin/lib fallback cannot verify a release."
    }
   }
   foreach($entry in $selected){
    $log=Join-Path $attempt ($entry.class+'.log');$activeLog=$log;$arguments=@($entry.arguments)
    $watch=[Diagnostics.Stopwatch]::StartNew()
    & $javaPath -cp ($testClasses+';'+$candidate+';lib/*') $entry.class @arguments *> $log
    $exitCode=$LASTEXITCODE;$watch.Stop();$seconds=[Math]::Round($watch.Elapsed.TotalSeconds,3)
    $passed=$exitCode -eq 0 -and [IO.File]::ReadAllText($log) -match $entry.successPattern
    $summary.Suites+=@([pscustomobject]@{Class=$entry.class;Coverage=$entry.reason;Seconds=$seconds;Passed=$passed;ExitCode=$exitCode;Log=$log})
    $summary.SuiteSeconds+=$seconds
    if($exitCode -ne 0){throw "Suite failed: $($entry.class); see $log"}
    if(!$passed){throw "Missing success marker: $($entry.class); see $log"}
    $results+=[pscustomobject]@{Class=$entry.class;Reason=$entry.reason;Seconds=$seconds;Log=$log;SHA256=(Hash $log)}
    Write-Output "$($entry.class): passed (${seconds}s) — $($entry.reason)"
   }
   $activeLog=$null
   AssertHashes $baseline;AssertRuntime $runtime
   $outputRoots=@($classes,$candidate,$testClasses)
   $outputs=Snapshot @($outputRoots | ForEach-Object {Get-ChildItem -LiteralPath $_ -Recurse -File | ForEach-Object FullName})
   if(!$outputs.Count){throw 'Compiler produced no classes'}
   if($Phase -eq 'Verify'){
    WriteJson ([pscustomobject]@{Inputs=$baseline;Runtime=$runtime;Outputs=$outputs;OutputRoots=$outputRoots;Classes=$classes;Suites=$results}) $receipt
   }
   $summary.Status='Passed'
  }catch{
   $summary.Failure=$_.Exception.Message
   if($activeLog -and (Test-Path -LiteralPath $activeLog)){
    Write-Output "Failure excerpt (last 12 lines); full log: $activeLog"
    Get-Content -LiteralPath $activeLog -Tail 12 | Write-Output
   }
   throw
  }finally{
   $totalWatch.Stop();$summary.TotalSeconds=[Math]::Round($totalWatch.Elapsed.TotalSeconds,3)
   $summary.CompileSeconds=[Math]::Round($summary.CompileSeconds,3)
   $summary.SuiteSeconds=[Math]::Round($summary.SuiteSeconds,3)
   WriteJson $summary (Join-Path $attempt 'summary.json')
  }
  Write-Output "$Phase $($m.name): $($results.Count) suites passed; tests $($summary.SuiteSeconds)s, compile $($summary.CompileSeconds)s, candidate $($summary.CandidateSeconds)s, total $($summary.TotalSeconds)s. Summary: $attempt/summary.json"
  if($Phase -eq 'Debug'){Write-Output 'Diagnostic only. Run Verify for the complete release selection before Stage.'}
  else{Write-Output 'Runtime unchanged; Stage is a separate action.'}
  return
 }
 $v=ReadJson $receipt
 AssertHashes $v.Inputs;AssertRuntime $v.Runtime;AssertHashes $v.Outputs
 $outputPaths=@($v.OutputRoots | ForEach-Object {Get-ChildItem -LiteralPath $_ -Recurse -File | ForEach-Object FullName} | Sort-Object)
 if(($outputPaths -join "`n") -cne (@($v.Outputs | ForEach-Object Path | Sort-Object) -join "`n")){throw 'Compiled output file set changed'}
 foreach($validatedSuite in $v.Suites){if((Hash $validatedSuite.Log) -ne $validatedSuite.SHA256){throw 'Verification log changed'}}
 if(@($v.Suites).Count -ne $suiteNames.Count){throw 'Incomplete suite receipt'}
 $plan=@()
 foreach($source in $m.stageSources){
  $relative=$source.Substring(4);$dir=Split-Path $relative;$name=[IO.Path]::GetFileNameWithoutExtension($relative)
  $newDir=Join-Path $v.Classes $dir;$liveDir=SafePath ('bin/'+$dir)
  $new=@(CompiledFamily $v.Classes $source)
  $old=@();if(Test-Path -LiteralPath $liveDir){$old=@(Get-ChildItem -LiteralPath $liveDir -Filter '*.class' | Where-Object {$_.BaseName -eq $name -or $_.BaseName.StartsWith($name+'$')})}
  $newNames=@($new | ForEach-Object Name);$oldNames=@($old | ForEach-Object Name)
  foreach($file in @($newNames+$oldNames | Sort-Object -Unique)){
   $dest=SafePath ('bin/'+$dir+'/'+$file);$compiled=Join-Path $newDir $file;$exists=Test-Path -LiteralPath $dest
   $plan+=[pscustomobject]@{Path=$dest;New=$(if($newNames -contains $file){$compiled}else{$null});Existed=$exists;Backup=(Join-Path $batch ('before/runtime/'+$dir+'/'+$file));BeforeHash=$(if($exists){Hash $dest}else{$null});AfterHash=$(if($newNames -contains $file){Hash $compiled}else{$null})}
  }
 }
 # Back up every affected file before any runtime mutation, including obsolete inner classes.
 foreach($p in $plan){if($p.Existed){New-Item -ItemType Directory -Force (Split-Path $p.Backup) | Out-Null;Copy-Item -LiteralPath $p.Path -Destination $p.Backup;if((Hash $p.Backup) -ne $p.BeforeHash){throw 'Runtime backup mismatch'}}}
 AssertRuntime $v.Runtime
 WriteJson $plan $started
 try {
  foreach($p in $plan){
   New-Item -ItemType Directory -Force (Split-Path $p.Path) | Out-Null
   if($p.New){Copy-Item -LiteralPath $p.New -Destination $p.Path;if((Hash $p.Path) -ne $p.AfterHash){throw 'Staged hash mismatch'}}
   elseif(Test-Path -LiteralPath $p.Path){Remove-Item -LiteralPath $p.Path}
  }
  WriteJson $plan (Join-Path $batch 'staged.json')
 } catch {
  $failure=$_
  foreach($p in $plan){if($p.Existed){Copy-Item -LiteralPath $p.Backup -Destination $p.Path;if((Hash $p.Path) -ne $p.BeforeHash){throw "Rollback failed: $($p.Path)"}}elseif(Test-Path -LiteralPath $p.Path){Remove-Item -LiteralPath $p.Path}}
  $failure | Out-String | Set-Content -LiteralPath (Join-Path $batch 'stage-failed.txt')
  throw $failure
 }
 Write-Output "Staged $(@($plan | Where-Object New).Count) classes with verified backups. Server restart and relevant live acceptance required."
} finally {Pop-Location}



