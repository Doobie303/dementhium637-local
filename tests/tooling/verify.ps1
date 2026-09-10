param([Parameter(Mandatory)][string]$Javac,[Parameter(Mandatory)][string]$Java)
$ErrorActionPreference='Stop';Set-StrictMode -Version Latest
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$runner=Join-Path $repo 'tools/Invoke-ChangeBatch.ps1'
$homeDir=Join-Path $repo ('build/tooling-tests/'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $homeDir | Out-Null
$script:checks=0
function Check($ok,$why){$script:checks++;if(!$ok){throw "Assertion: $why"}}
function Reject([scriptblock]$action,[string]$pattern){
 $message=$null;try {& $action | Out-Null}catch {$message=$_.Exception.Message}
 Check ($null -ne $message -and $message -match $pattern) "Expected '$pattern', got '$message'"
}
function WriteFile($path,$text){New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null;[IO.File]::WriteAllText($path,$text)}
function Run($root,$phase){& $runner -Phase $phase -Manifest manifest.json -WorkspaceRoot $root -Javac $Javac -Java $Java}
function Fixture($name,$twoSuites=$false,$mode='normal'){
 $root=Join-Path $homeDir $name
 New-Item -ItemType Directory -Force "$root/src/sample","$root/tests","$root/bin","$root/lib" | Out-Null
 WriteFile "$root/src/sample/Example.java" 'package sample; public class Example { public static int value(){return 1;} public static class Old {} }'
 & $Javac --release 8 -d "$root/bin" "$root/src/sample/Example.java" *> "$root/bootstrap.log"
 if($LASTEXITCODE -ne 0){throw 'Fixture bootstrap failed'}
 WriteFile "$root/bin/sample/ExampleOther.class" 'unrelated-sibling'
 WriteFile "$root/tests/Check.java" 'public class Check { public static void main(String[] a){if(sample.Example.value()!=2)throw new AssertionError("wrong build");System.out.println("FIXTURE passed");} }'
 $manifest=@{name='fixture';scope='Tooling integration fixture';sources=@('src/sample/Example.java','tests/Check.java');stageSources=@('src/sample/Example.java');inputs=@();suites=@(@{class='Check';arguments=@();reason='Exercise actual compiler and staging precedence';successPattern='FIXTURE passed'})}
 if($twoSuites){
  WriteFile "$root/tests/Other.java" 'public class Other {public static void main(String[] a)throws Exception{java.nio.file.Files.write(java.nio.file.Paths.get("other-runs"),new byte[]{1},java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);System.out.println("OTHER passed");}}'
  $manifest.sources+=@('tests/Other.java')
  $manifest.suites+=@(@{class='Other';arguments=@();reason='Prove diagnostics skip unrelated suites';successPattern='OTHER passed'})
 }
 if($mode -eq 'stale-suite'){
  & $Javac --release 8 -cp "$root/bin" -d "$root/bin" "$root/tests/Check.java" *> "$root/stale-suite-bootstrap.log"
  if($LASTEXITCODE -ne 0){throw 'Stale-suite fixture bootstrap failed'}
  $manifest.sources=@('src/sample/Example.java')
 }
 if($mode -eq 'unstaged-dependency'){
  WriteFile "$root/src/sample/Dependency.java" 'package sample; public class Dependency {public static int value(){return 1;}}'
  & $Javac --release 8 -d "$root/bin" "$root/src/sample/Dependency.java" *> "$root/dependency-bootstrap.log"
  if($LASTEXITCODE -ne 0){throw 'Dependency fixture bootstrap failed'}
  $manifest.sources+=@('src/sample/Dependency.java')
 }
 if($mode -eq 'removed-inner' -or $mode -eq 'aliased-source'){
  WriteFile "$root/tests/Check.java" 'public class Check {public static void main(String[] a)throws Exception{try{Class.forName("sample.Example$Old");throw new AssertionError("obsolete inner class visible");}catch(ClassNotFoundException expected){}System.out.println("FIXTURE passed");}}'
 }
 if($mode -eq 'aliased-source'){
  $manifest.sources[0]='src/sample/../sample/Example.java';$manifest.stageSources[0]=$manifest.sources[0]
 }
 if($mode -eq 'shadow-production'){
  WriteFile "$root/tests/shadow/Example.java" 'package sample; public class Example {public static int value(){return 2;}}'
  $manifest.sources+=@('tests/shadow/Example.java')
 }
 $manifest | ConvertTo-Json -Depth 8 | Set-Content "$root/manifest.json"
 Run $root Prepare | Out-Null
 WriteFile "$root/src/sample/Example.java" 'package sample; public class Example { public static int value(){return 2;} public static class New {} }'
 if($mode -eq 'unstaged-dependency'){
  WriteFile "$root/src/sample/Example.java" 'package sample; public class Example {public static int value(){return Dependency.value();}}'
  WriteFile "$root/src/sample/Dependency.java" 'package sample; public class Dependency {public static int value(){return 2;}}'
 }
 return $root
}
# Documentation preservation and routing: no original report pointer may disappear.
$archive=Join-Path $repo 'docs/project/AGENTS_HISTORY_2026-09-09.md'
$old=[IO.File]::ReadAllText($archive)
$new=[IO.File]::ReadAllText((Join-Path $repo 'AGENTS.md'))
$index=[IO.File]::ReadAllText((Join-Path $repo 'docs/project/CONTEXT_INDEX.md'))
foreach($ref in @([regex]::Matches($old,'[A-Za-z0-9_/-]+\.md') | ForEach-Object Value | Sort-Object -Unique)){
 Check ($new.Contains($ref) -or $index.Contains($ref)) "Report indexed: $ref"
 Check (Test-Path -LiteralPath (Join-Path $repo $ref) -PathType Leaf) "Report exists: $ref"
}
Check ($new.Length -lt $old.Length/3) 'Root instructions substantially smaller'
foreach($guard in @('Summoning','Dungeoneering','duel-commit.bin','gamble-commit.bin','20430','tests/TESTING.md','MapAllocation','developer client','godmode')){Check ($new.Contains($guard)) "Global safeguard: $guard"}
$liveBefore=@(Get-ChildItem (Join-Path $repo bin) -Recurse -File | Get-FileHash | Sort-Object Path)
$root=Fixture 'happy'
$beforeHash=(Get-FileHash "$root/bin/sample/Example.class").Hash
Run $root Verify | Out-Null
Check ((Get-FileHash "$root/bin/sample/Example.class").Hash -eq $beforeHash) 'Verify leaves runtime unchanged'
Run $root Stage | Out-Null
Check ((Get-FileHash "$root/bin/sample/Example.class").Hash -ne $beforeHash) 'Stage replaces requested family'
Check (!(Test-Path -LiteralPath "$root/bin/sample/Example`$Old.class")) 'Obsolete inner class removed'
Check (Test-Path -LiteralPath "$root/bin/sample/Example`$New.class") 'New inner class staged'
Check ((Get-Content "$root/bin/sample/ExampleOther.class" -Raw) -eq 'unrelated-sibling') 'Sibling class untouched'
Check (!(Test-Path "$root/bin/Check.class")) 'Test class not staged'
Check ((Get-FileHash "$root/build/batches/fixture/before/runtime/sample/Example.class").Hash -eq $beforeHash) 'Exact runtime backup'
Check ([IO.File]::ReadAllText("$root/build/batches/fixture/before/source/src/sample/Example.java").Contains('return 1;')) 'Pre-edit source backup'
Reject {Run $root Stage} 'already attempted'
Reject {Run $root Prepare} 'already exists'

$root=Fixture 'guards';Run $root Verify | Out-Null
$source=[IO.File]::ReadAllText("$root/src/sample/Example.java")
WriteFile "$root/src/sample/Example.java" ($source+' ')
Reject {Run $root Stage} 'Changed or missing validated'
WriteFile "$root/src/sample/Example.java" $source
WriteFile "$root/bin/injected.class" 'extra'
Reject {Run $root Stage} 'dependency file set changed'
Remove-Item -LiteralPath "$root/bin/injected.class"
$v=Get-Content "$root/build/batches/fixture/verified.json" -Raw | ConvertFrom-Json
$compiled=Join-Path $v.Classes 'sample/Example.class';$bytes=[IO.File]::ReadAllBytes($compiled)
[IO.File]::WriteAllBytes($compiled,[byte[]]@(0,1,2))
Reject {Run $root Stage} 'Changed or missing validated'
[IO.File]::WriteAllBytes($compiled,$bytes)
WriteFile (Join-Path $v.Classes 'sample/Example$Injected.class') 'extra'
Reject {Run $root Stage} 'output file set changed'
Remove-Item -LiteralPath (Join-Path $v.Classes 'sample/Example$Injected.class')
$log=$v.Suites[0].Log;$logText=[IO.File]::ReadAllText($log);WriteFile $log ($logText+'extra')
Reject {Run $root Stage} 'log changed'
WriteFile $log $logText
$candidate=Join-Path (Split-Path $v.Classes -Parent) 'runtime-candidate'
WriteFile "$candidate/sample/Example`$Injected.class" 'extra'
Reject {Run $root Stage} 'output file set changed'
Remove-Item -LiteralPath "$candidate/sample/Example`$Injected.class"
$testClass=Join-Path (Split-Path $v.Classes -Parent) 'test-classes/Check.class'
$testBytes=[IO.File]::ReadAllBytes($testClass);[IO.File]::WriteAllBytes($testClass,[byte[]]@(0,1,2))
Reject {Run $root Stage} 'Changed or missing validated'
[IO.File]::WriteAllBytes($testClass,$testBytes)
# A later unsuccessful Verify must invalidate the earlier pass, even if stdout says passed.
WriteFile "$root/tests/Check.java" 'public class Check {public static void main(String[] a){System.out.println("FIXTURE passed");System.exit(7);}}'
Reject {Run $root Verify} 'Suite failed'
Check (!(Test-Path "$root/build/batches/fixture/verified.json")) 'Failed reverify invalidates previous receipt'
Reject {Run $root Stage} 'does not exist|cannot find'
WriteFile "$root/tests/Check.java" 'public class Check {public static void main(String[] a){System.out.println("wrong marker");}}'
Reject {Run $root Verify} 'Missing success marker'
WriteFile "$root/tests/Check.java" 'not valid java'
Reject {Run $root Verify} 'Compilation failed'
Check (!(Test-Path "$root/build/batches/fixture/stage-started.json")) 'Failed validation never begins staging'
Reject {& $runner -Phase Prepare -Manifest '../escape.json' -WorkspaceRoot $root} 'escapes workspace'
Reject {& $runner -Phase Prepare -Manifest manifest.json -WorkspaceRoot ([IO.Path]::GetTempPath())} 'repository or its child'
$manifest=Get-Content "$root/manifest.json" -Raw | ConvertFrom-Json;$manifest.scope='changed';$manifest | ConvertTo-Json -Depth 8 | Set-Content "$root/manifest.json"
Reject {Run $root Verify} 'Manifest changed'
# Diagnostic selection is not a release gate; always compile fresh declared sources.
$root=Fixture 'diagnostic' $true
Reject {& $runner -Phase Debug -Manifest manifest.json -WorkspaceRoot $root -Javac $Javac -Java $Java} 'requires -Suite'
Reject {& $runner -Phase Debug -Suite Unknown -Manifest manifest.json -WorkspaceRoot $root -Javac $Javac -Java $Java} 'Unknown diagnostic'
Reject {& $runner -Phase Debug -Suite Check,Check -Manifest manifest.json -WorkspaceRoot $root -Javac $Javac -Java $Java} 'Duplicate diagnostic'
Reject {& $runner -Phase Verify -Suite Check -Manifest manifest.json -WorkspaceRoot $root -Javac $Javac -Java $Java} 'only with Debug'
Run $root Verify | Out-Null
Check ((Get-Item "$root/other-runs").Length -eq 1) 'Release verification runs both suites'
$output=@(& $runner -Phase Debug -Suite Check -Manifest manifest.json -WorkspaceRoot $root -Javac $Javac -Java $Java)
Check ((Get-Item "$root/other-runs").Length -eq 1) 'Single-suite debug skips passing unrelated suite'
Check (!(Test-Path "$root/build/batches/fixture/verified.json")) 'Debug invalidates release receipt'
Reject {Run $root Stage} 'does not exist|cannot find'
$summaryFile=Get-ChildItem "$root/build/batches/fixture/debug-*/summary.json" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$summary=Get-Content $summaryFile.FullName -Raw | ConvertFrom-Json
Check ($summary.Mode -eq 'Debug' -and $summary.Status -eq 'Passed' -and $summary.Suites.Count -eq 1) 'Diagnostic summary labels subset'
Check ($summary.CompileSeconds -ge 0 -and $summary.SuiteSeconds -ge 0 -and $summary.TotalSeconds -ge $summary.CompileSeconds) 'Measured durations recorded'
Check ($summary.CandidateSeconds -ge 0 -and $summary.TotalSeconds -ge ($summary.CompileSeconds+$summary.CandidateSeconds)) 'Candidate assembly measured separately from compilation'
Check ($summary.Suites[0].Coverage -match 'actual compiler' -and ($output -join ' ') -match 'Diagnostic only') 'Coverage and diagnostic limit surfaced'
WriteFile "$root/tests/Check.java" 'public class Check {public static void main(String[] a){System.out.println("FIXTURE passed");System.exit(7);}}'
Reject {& $runner -Phase Debug -Suite Check -Manifest manifest.json -WorkspaceRoot $root -Javac $Javac -Java $Java} 'Suite failed'
Check ((Get-Item "$root/other-runs").Length -eq 1) 'Failed diagnostic still skips unrelated suite'
$summaryFile=Get-ChildItem "$root/build/batches/fixture/debug-*/summary.json" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$summary=Get-Content $summaryFile.FullName -Raw | ConvertFrom-Json
Check ($summary.Status -eq 'Failed' -and $summary.Suites[0].ExitCode -eq 7 -and !$summary.Suites[0].Passed) 'Failure summary retains real exit status'
WriteFile "$root/tests/Check.java" 'public class Check {public static void main(String[] a){if(sample.Example.value()!=2)throw new AssertionError();System.out.println("FIXTURE passed");}}'
& $runner -Phase Debug -Suite Check,Other -Manifest manifest.json -WorkspaceRoot $root -Javac $Javac -Java $Java | Out-Null
Check (!(Test-Path "$root/build/batches/fixture/verified.json")) 'Even all-suite diagnostics cannot authorize staging'
Run $root Verify | Out-Null
Check ((Get-Item "$root/other-runs").Length -eq 3) 'Final verification reruns complete selection'
Run $root Stage | Out-Null
Check (Test-Path "$root/build/batches/fixture/staged.json") 'Full fresh verification permits staging'
# Expected: a release suite must come from this compilation, never a leftover bin/lib class.
$root=Fixture 'stale-suite' $false 'stale-suite'
Reject {Run $root Verify} 'not freshly compiled'
Check (!(Test-Path "$root/build/batches/fixture/verified.json")) 'Stale suite cannot authorize Stage'
# Expected: tests see retained runtime dependencies, not compiled-but-unstaged replacements.
$root=Fixture 'unstaged-dependency' $false 'unstaged-dependency'
$beforeHash=(Get-FileHash "$root/bin/sample/Example.class").Hash
Reject {Run $root Verify} 'Suite failed'
Check (!(Test-Path "$root/build/batches/fixture/verified.json")) 'Mixed source/runtime candidate cannot authorize Stage'
Check ((Get-FileHash "$root/bin/sample/Example.class").Hash -eq $beforeHash) 'Rejected candidate leaves runtime unchanged'
# Expected: old inner classes removed by Stage must already be absent during verification.
$root=Fixture 'removed-inner' $false 'removed-inner'
Run $root Verify | Out-Null
Check (Test-Path -LiteralPath "$root/bin/sample/Example`$Old.class") 'Candidate construction leaves old live inner class untouched'
Run $root Stage | Out-Null
Check (!(Test-Path -LiteralPath "$root/bin/sample/Example`$Old.class")) 'Verified candidate matches removal during Stage'
# Expected: separately compiled tests cannot replace production classes during verification.
$root=Fixture 'shadow-production' $false 'shadow-production'
Reject {Run $root Verify} 'Test class shadows candidate'
Check (!(Test-Path "$root/build/batches/fixture/verified.json")) 'Test substitute cannot authorize a production release'
# Expected: equivalent path spellings remove exactly the same obsolete family members.
$root=Fixture 'aliased-source' $false 'aliased-source'
Run $root Verify | Out-Null
Run $root Stage | Out-Null
Check (!(Test-Path -LiteralPath "$root/bin/sample/Example`$Old.class")) 'Normalized source alias preserves candidate/staging identity'
$liveAfter=@(Get-ChildItem (Join-Path $repo bin) -Recurse -File | Get-FileHash | Sort-Object Path)
Check (($liveBefore | ConvertTo-Json -Compress) -ceq ($liveAfter | ConvertTo-Json -Compress)) 'Real server runtime unchanged'
$summary="Tooling: $script:checks checks passed. Fixtures: $homeDir"
$summary | Set-Content (Join-Path $homeDir 'result.txt');Write-Output $summary

