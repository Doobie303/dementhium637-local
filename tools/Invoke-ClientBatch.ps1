[CmdletBinding()]
param(
 [Parameter(Mandatory)][ValidateSet('Prepare','Build','Verify','Publish')][string]$Phase,
 [Parameter(Mandatory)][ValidatePattern('^[a-z0-9][a-z0-9-]{0,60}$')][string]$Name,
 [string[]]$Checks=@(),[string]$Reason,
 [string]$Baseline="C:/Users/Tcarn/Desktop/DyNamic's 639/DyNamic-local.jar",
 [string]$Jdk='C:/Users/Tcarn/Desktop/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21.0.11.v20260515-1531/jre',
 [string]$Java='C:/Program Files/Java/jre1.8.0_503/bin/java.exe'
)
$ErrorActionPreference='Stop';Set-StrictMode -Version Latest
$root=Split-Path $PSScriptRoot -Parent
function PathInRoot([string]$relative){
 if([IO.Path]::IsPathRooted($relative) -or $relative -match '[\r\n";:]'){throw 'Expected simple workspace-relative path'}
 $p=[IO.Path]::GetFullPath((Join-Path $root $relative))
 if(!$p.StartsWith($root+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Path escapes workspace'}
 for($part=$p;$part -ne $root;$part=Split-Path $part -Parent){if((Test-Path -LiteralPath $part) -and ((Get-Item -LiteralPath $part).Attributes -band [IO.FileAttributes]::ReparsePoint)){throw 'Reparse point not supported'}}
 return $p
}
function Hash($p){(Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash}
function Save($obj,$p){$obj | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $p -Encoding UTF8}
function Read($p){Get-Content -LiteralPath $p -Raw | ConvertFrom-Json}
function Snap($files){@($files | Sort-Object -Unique | ForEach-Object {[pscustomobject]@{Path=$_;SHA256=(Hash $_)}})}
function Assert($rows){foreach($r in $rows){if(!(Test-Path -LiteralPath $r.Path) -or (Hash $r.Path) -ne $r.SHA256){throw "Changed validated input: $($r.Path)"}}}
function ProofDependencies($selected){
 # Built-in defaults preserve the current catalog. Unknown legacy proofs keep the
 # original complete reference scope; new proofs can declare dependencyGroups.
 $defaults=@{interface=@('runtime','cache');capability=@();cape=@('runtime','cache','cape');texture=@('runtime','cache','texture');login=@('runtime','cache')}
 $groups=@($selected | ForEach-Object {
  $declared=$_.PSObject.Properties['dependencyGroups']
  if($null -ne $declared){@($declared.Value)}elseif($defaults.ContainsKey($_.name)){$defaults[$_.name]}else{@('runtime','cache','cape','texture')}
 } | Sort-Object -Unique)
 $directories=@();$files=@()
 foreach($group in $groups){
  switch($group){
   'runtime' {$directories+=@('bin','lib')}
   'cache' {$directories+='data/cache'}
   'cape' {$files+=@('data/item/equipIds.txt','data/custom/infernal-cape/item.dat')}
   'texture' {$files+='client/infernal-cape/assets/texture318.dat'}
   default {throw "Unknown proof dependency group: $group"}
  }
 }
 [pscustomobject]@{Groups=$groups;Directories=@($directories | Sort-Object -Unique | ForEach-Object {PathInRoot $_});Files=@($files | Sort-Object -Unique | ForEach-Object {PathInRoot $_})}
}
function DependencyFiles($directories){@($directories | ForEach-Object {Get-ChildItem -LiteralPath $_ -Recurse -File | ForEach-Object FullName} | Sort-Object -Unique)}
function AssertDependencySet($directories,$rows){
 $current=@(DependencyFiles $directories)
 $recorded=@($rows | Where-Object {$null -ne $_} | ForEach-Object Path | Sort-Object -Unique)
 if(($current -join "`n") -cne ($recorded -join "`n")){throw 'Reference dependency file set changed'}
}
function ZipHashes($p){
 $zip=[IO.Compression.ZipFile]::OpenRead($p);$result=@{}
 try{foreach($e in $zip.Entries){if($e.FullName.EndsWith('/')){continue};if($result.ContainsKey($e.FullName)){throw 'Duplicate JAR entry'};$stream=$e.Open();$sha=[Security.Cryptography.SHA256]::Create();try{$result[$e.FullName]=[BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-','')}finally{$stream.Dispose();$sha.Dispose()}}}finally{$zip.Dispose()}
 return $result
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$catalogPath=PathInRoot 'client/developer-client.json';$catalog=Read $catalogPath
$batch=PathInRoot ('build/client-batches/'+$Name)
$preparedPath=Join-Path $batch 'prepared.json';$builtPath=Join-Path $batch 'built.json';$verifiedPath=Join-Path $batch 'verified.json'
$liveDir=PathInRoot 'build/gambler-interface/dev-client'
$launcher=PathInRoot 'client/gambler-interface/Run Dev Client.bat';$liveLauncher=Join-Path $liveDir 'Run Dev Client.bat'
$sources=@($catalog.sources | ForEach-Object {PathInRoot $_})
$assets=@($catalog.assets | ForEach-Object {if($_.entry -notmatch '^[\w/-]+\.[\w]+$' -or $_.entry.Contains('..')){throw 'Invalid asset entry'};PathInRoot $_.source})
$proofs=@($catalog.proofs | ForEach-Object {PathInRoot $_.source})
$javac=Join-Path $Jdk 'bin/javac.exe';$jarTool=Join-Path $Jdk 'bin/jar.exe'
Push-Location $root
try{
 if($Phase -eq 'Prepare'){
  if(!$Checks.Count -or [string]::IsNullOrWhiteSpace($Reason)){throw 'Prepare requires explicit -Checks and -Reason'}
  foreach($check in $Checks){if($catalog.proofs.name -notcontains $check){throw "Unknown check: $check"}}
  $null=ProofDependencies @($catalog.proofs | Where-Object {$Checks -contains $_.name})
  if(Test-Path -LiteralPath $batch){throw 'Batch exists; select a new name'}
  New-Item -ItemType Directory -Path $batch | Out-Null
  $before=@();foreach($file in @($sources+$assets+$proofs+@($catalogPath,$launcher))){$dest=Join-Path $batch ('before/'+$file.Substring($root.Length+1));New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null;Copy-Item -LiteralPath $file -Destination $dest;$before+=Snap @($dest);if((Hash $dest) -ne (Hash $file)){throw 'Backup mismatch'}}
  Save ([pscustomobject]@{Checks=@($Checks | Sort-Object -Unique);Reason=$Reason;Before=$before}) $preparedPath
  Write-Output "Prepared client batch $Name. No client/cache/launcher change.";return
 }
 $prepared=Read $preparedPath;Assert $prepared.Before
 if(Test-Path -LiteralPath (Join-Path $batch 'publish-started.json')){throw 'Publication already attempted; inspect the release record'}
 if((Hash $Baseline) -ne $catalog.baselineSHA256){throw 'Baseline hash mismatch'}
 if($Phase -eq 'Build'){
  if(Test-Path -LiteralPath $builtPath){Remove-Item -LiteralPath $builtPath};if(Test-Path -LiteralPath $verifiedPath){Remove-Item -LiteralPath $verifiedPath}
  $attempt=Join-Path $batch ('build-'+[guid]::NewGuid().ToString('N'));$classes=Join-Path $attempt 'classes';$empty=Join-Path $attempt 'empty'
  New-Item -ItemType Directory -Path $classes,$empty | Out-Null
  $inputs=Snap @($sources+$assets+$proofs+@($catalogPath,$launcher,$liveLauncher,$Baseline,$PSCommandPath,$javac,$jarTool))
  $argsFile=Join-Path $attempt 'sources.args';$sources | ForEach-Object {'"'+$_.Replace('\','/')+'"'} | Set-Content $argsFile -Encoding UTF8
  & $javac --release 8 -encoding windows-1252 -cp $Baseline -sourcepath $empty -implicit:none -d $classes ('@'+$argsFile) *> (Join-Path $attempt 'compile.log')
  if($LASTEXITCODE -ne 0){throw "Compile failed: $attempt/compile.log"}
  foreach($asset in $catalog.assets){$dest=Join-Path $classes $asset.entry;New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null;Copy-Item -LiteralPath (PathInRoot $asset.source) -Destination $dest}
  $candidate=Join-Path $attempt ('Developer-client-'+$Name+'.jar');Copy-Item -LiteralPath $Baseline -Destination $candidate
  & $jarTool uf $candidate -C $classes . *> (Join-Path $attempt 'package.log');if($LASTEXITCODE -ne 0){throw 'Packaging failed'}
  $original=ZipHashes $Baseline;$packaged=ZipHashes $candidate;$patch=@{}
  foreach($file in Get-ChildItem -LiteralPath $classes -Recurse -File){$patch[$file.FullName.Substring($classes.Length+1).Replace('\','/')]=Hash $file.FullName}
  foreach($entry in $original.Keys){if(!$packaged.ContainsKey($entry)){throw "Lost baseline entry: $entry"};if(!$patch.ContainsKey($entry) -and $packaged[$entry] -ne $original[$entry]){throw "Unexpected baseline modification: $entry"}}
  foreach($entry in $packaged.Keys){if(!$original.ContainsKey($entry) -and !$patch.ContainsKey($entry)){throw "Unlisted JAR entry: $entry"}}
  foreach($entry in $patch.Keys){if(!$packaged.ContainsKey($entry) -or $packaged[$entry] -ne $patch[$entry]){throw "Patch mismatch: $entry"}}
  Assert $inputs
  Save ([pscustomobject]@{Candidate=$candidate;CandidateHash=(Hash $candidate);Inputs=$inputs;PatchEntries=$patch;BaselineEntries=$original.Count}) $builtPath
  Write-Output "Candidate built: $candidate. $($original.Count) baseline entries audited; launcher unchanged.";return
 }
 $built=Read $builtPath;Assert $built.Inputs
 if((Hash $built.Candidate) -ne $built.CandidateHash){throw 'Candidate hash mismatch'}
 if($Phase -eq 'Verify'){
  if(Test-Path -LiteralPath $verifiedPath){Remove-Item -LiteralPath $verifiedPath}
  $attempt=Join-Path $batch ('verify-'+[guid]::NewGuid().ToString('N'));$testClasses=Join-Path $attempt 'tests';$artifacts=Join-Path $attempt 'artifacts';$empty=Join-Path $attempt 'empty'
  New-Item -ItemType Directory -Path $testClasses,$artifacts,$empty | Out-Null
  $selected=@($catalog.proofs | Where-Object {$prepared.Checks -contains $_.name});if($selected.Count -ne @($prepared.Checks).Count){throw 'Selected proof disappeared from catalog'}
  # Hash only the union of the selected proofs' external inputs; Java is always required.
  $scope=ProofDependencies $selected
  $dependencies=@(Snap @(DependencyFiles $scope.Directories))
  $references=@(Snap @($scope.Files+@($Java)))
  $files=@($selected | ForEach-Object {PathInRoot $_.source});$cp=$built.Candidate
  if($scope.Groups -contains 'runtime'){$cp+=';bin;lib/*'}
  & $javac --release 8 -encoding windows-1252 -cp $cp -sourcepath $empty -implicit:none -d $testClasses @files *> (Join-Path $attempt 'compile.log');if($LASTEXITCODE -ne 0){throw "Proof compilation failed: $attempt/compile.log"}
  $logs=@();foreach($proof in $selected){
   $log=Join-Path $attempt ($proof.name+'.log')
   & $Java ('-Dcodex.proofOutput='+$artifacts) -cp ($testClasses+';'+$cp) $proof.class *> $log
   if($LASTEXITCODE -ne 0 -or [IO.File]::ReadAllText($log) -notmatch $proof.marker){throw "Proof failed: $($proof.name); see $log"}
   $logs+=Snap @($log);Get-Content -LiteralPath $log -Tail 1
  }
  Assert $built.Inputs;Assert $dependencies;Assert $references;AssertDependencySet $scope.Directories $dependencies
  if((Hash $built.Candidate) -ne $built.CandidateHash){throw 'Candidate changed during verification'}
  Save ([pscustomobject]@{BuiltHash=(Hash $builtPath);PreparedHash=(Hash $preparedPath);DependencyGroups=$scope.Groups;DependencyDirectories=$scope.Directories;Dependencies=$dependencies;References=$references;Logs=$logs;Checks=$prepared.Checks;Reason=$prepared.Reason}) $verifiedPath
  Write-Output "Verified candidate; outputs: $artifacts. Publish separately after required review.";return
 }
 $verified=Read $verifiedPath
 if((Hash $builtPath) -ne $verified.BuiltHash -or (Hash $preparedPath) -ne $verified.PreparedHash){throw 'Build/selection record changed'}
 Assert $verified.Dependencies;Assert $verified.References;Assert $verified.Logs
 AssertDependencySet $verified.DependencyDirectories $verified.Dependencies
 $jarName=[IO.Path]::GetFileName($built.Candidate);$destination=Join-Path $liveDir $jarName
 if(Test-Path -LiteralPath $destination){throw 'Release JAR already exists; never overwrite a release'}
 $text=[IO.File]::ReadAllText($launcher);$launcherMatches=[regex]::Matches($text,'(?m)(-jar\s+)([^\s"\r\n]+\.jar)')
 if($launcherMatches.Count -ne 1){throw 'Expected exactly one launcher JAR target'}
 if($launcherMatches[0].Groups[2].Value -notmatch '^[A-Za-z0-9][A-Za-z0-9_.-]*\.jar$'){throw 'Launcher must target a local JAR filename'}
 $previousJar=Join-Path $liveDir $launcherMatches[0].Groups[2].Value
 if(!(Test-Path -LiteralPath $previousJar)){throw 'Current launcher JAR missing'}
 $backup=Join-Path $batch 'before/publish';New-Item -ItemType Directory -Force $backup | Out-Null
 Copy-Item -LiteralPath $launcher -Destination (Join-Path $backup 'source-launcher.bat');Copy-Item -LiteralPath $liveLauncher -Destination (Join-Path $backup 'live-launcher.bat')
 $oldHashes=Snap @($launcher,$liveLauncher,$previousJar)
 foreach($pair in @(@($launcher,'source-launcher.bat'),@($liveLauncher,'live-launcher.bat'))){if((Hash $pair[0]) -ne (Hash (Join-Path $backup $pair[1]))){throw 'Launcher backup mismatch'}}
 Save ([pscustomobject]@{Previous=$oldHashes;NewJar=$destination;Backup=$backup}) (Join-Path $batch 'publish-started.json')
 try{
  Copy-Item -LiteralPath $built.Candidate -Destination $destination;if((Hash $destination) -ne $built.CandidateHash){throw 'Release copy mismatch'}
  $updated=$text.Remove($launcherMatches[0].Groups[2].Index,$launcherMatches[0].Groups[2].Length).Insert($launcherMatches[0].Groups[2].Index,$jarName)
  [IO.File]::WriteAllText($launcher,$updated);[IO.File]::WriteAllText($liveLauncher,$updated)
  if((Hash $launcher) -ne (Hash $liveLauncher)){throw 'Launcher mismatch'}
  Save (Snap @($destination,$launcher,$liveLauncher)) (Join-Path $batch 'published.json')
 }catch{
  Copy-Item -LiteralPath (Join-Path $backup 'source-launcher.bat') -Destination $launcher;Copy-Item -LiteralPath (Join-Path $backup 'live-launcher.bat') -Destination $liveLauncher
  Assert $oldHashes
  throw
 }
 Write-Output 'Published through the same Run Dev Client.bat. Reopen client; live acceptance still required.'
}finally{Pop-Location}
