param(
 [string]$Jdk='C:/Users/Tcarn/Desktop/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21.0.11.v20260515-1531/jre',
 [string]$Java='C:/Program Files/Java/jre1.8.0_503/bin/java.exe'
)
$ErrorActionPreference='Stop';Set-StrictMode -Version Latest
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$root=Join-Path $repo ('build/client-tooling-tests/'+[guid]::NewGuid().ToString('N'))
$script:checks=0
$timer=[Diagnostics.Stopwatch]::StartNew()
function Check($pass,$why){$script:checks++;if(!$pass){throw "Assertion: $why"}}
function Reject([scriptblock]$action,$pattern){$msg=$null;try{& $action | Out-Null}catch{$msg=$_.Exception.Message};Check ($null -ne $msg -and $msg -match $pattern) "Expected $pattern; got $msg"}
function WriteFixture($path,$text){New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null;[IO.File]::WriteAllText($path,$text)}
function Run($phase,$name='fixture',[string[]]$checks=@('capability')){& "$root/tools/Invoke-ClientBatch.ps1" -Phase $phase -Name $name -Checks $checks -Reason 'Isolated release integration' -Baseline "$root/baseline.jar" -Jdk $Jdk -Java $Java}
foreach($dir in @('tools','build/gambler-interface/dev-client','bootstrap')){New-Item -ItemType Directory -Force (Join-Path $root $dir) | Out-Null}
Copy-Item -LiteralPath "$repo/tools/Invoke-ClientBatch.ps1" -Destination "$root/tools/Invoke-ClientBatch.ps1"
WriteFixture "$root/client/Main.java" 'public class Main {public static int value(){return 1;}}'
& "$Jdk/bin/javac.exe" --release 8 -d "$root/bootstrap" "$root/client/Main.java" *> "$root/bootstrap.log";if($LASTEXITCODE -ne 0){throw 'Bootstrap compile failed'}
& "$Jdk/bin/jar.exe" cf "$root/baseline.jar" -C "$root/bootstrap" .;if($LASTEXITCODE -ne 0){throw 'Bootstrap jar failed'}
Copy-Item "$root/baseline.jar" "$root/build/gambler-interface/dev-client/old.jar"
$launcher="@echo off`r`njava -Xmx1024m -jar old.jar`r`npause`r`n"
WriteFixture "$root/client/gambler-interface/Run Dev Client.bat" $launcher
WriteFixture "$root/build/gambler-interface/dev-client/Run Dev Client.bat" $launcher
WriteFixture "$root/client/Proof.java" 'public class Proof {public static void main(String[] a){if(Main.value()!=2)throw new AssertionError();System.out.println("PROOF passed");}}'
WriteFixture "$root/client/CapeReferenceProof.java" 'import java.nio.file.*; public class CapeReferenceProof {public static void main(String[] a)throws Exception {for(String p:new String[]{"data/cache/reference.dat","data/item/equipIds.txt","data/custom/infernal-cape/item.dat"})if(!new String(Files.readAllBytes(Paths.get(p)),"UTF-8").equals("reference"))throw new AssertionError(p);System.out.println("CAPE passed");}}'
WriteFixture "$root/client/TextureReferenceProof.java" 'import java.nio.file.*; public class TextureReferenceProof {public static void main(String[] a)throws Exception {for(String p:new String[]{"data/cache/reference.dat","client/infernal-cape/assets/texture318.dat"})if(!new String(Files.readAllBytes(Paths.get(p)),"UTF-8").equals("reference"))throw new AssertionError(p);System.out.println("TEXTURE passed");}}'
$catalog=@{baselineSHA256=(Get-FileHash "$root/baseline.jar").Hash;sources=@('client/Main.java');assets=@();proofs=@(
 @{name='capability';source='client/Proof.java';class='Proof';marker='PROOF passed'},
 @{name='cape';source='client/CapeReferenceProof.java';class='CapeReferenceProof';marker='CAPE passed'},
 @{name='texture';source='client/TextureReferenceProof.java';class='TextureReferenceProof';marker='TEXTURE passed'},
 @{name='explicit-none';source='client/Proof.java';class='Proof';marker='PROOF passed';dependencyGroups=@()},
 @{name='legacy';source='client/Proof.java';class='Proof';marker='PROOF passed'}
)}
$catalog | ConvertTo-Json -Depth 8 | Set-Content "$root/client/developer-client.json"
Reject {Run Build} 'cannot find|does not exist'
Run Prepare | Out-Null
Reject {Run Prepare} 'Batch exists'
WriteFixture "$root/client/Main.java" 'public class Main {public static int value(){return 2;} public static class Nested {}}'
Run Build | Out-Null
Reject {Run Publish} 'cannot find|does not exist'
Run Verify | Out-Null
$capability=Get-Content "$root/build/client-batches/fixture/verified.json" -Raw | ConvertFrom-Json
Check (@($capability.DependencyGroups).Count -eq 0 -and @($capability.Dependencies).Count -eq 0) 'Capability has no external server/cache dependencies'
Check (@($capability.References).Count -eq 1 -and $capability.References[0].Path -eq $Java) 'Capability still hashes the executing Java runtime'
Check (!(Test-Path "$root/data/cache") -and !(Test-Path "$root/bin") -and !(Test-Path "$root/client/infernal-cape/assets/texture318.dat")) 'Capability verified without cache, runtime or cape references'
$built=Get-Content "$root/build/client-batches/fixture/built.json" -Raw | ConvertFrom-Json
$candidateBytes=[IO.File]::ReadAllBytes($built.Candidate)
[IO.File]::WriteAllBytes($built.Candidate,[byte[]]@(0,1))
Reject {Run Publish} 'Candidate hash mismatch'
[IO.File]::WriteAllBytes($built.Candidate,$candidateBytes)
$source=[IO.File]::ReadAllText("$root/client/Main.java");WriteFixture "$root/client/Main.java" ($source+' ')
Reject {Run Publish} 'Changed validated input'
WriteFixture "$root/client/Main.java" $source
# A capability-only receipt remains valid after unrelated server/cache files appear.
WriteFixture "$root/bin/extra.class" 'unrelated'
WriteFixture "$root/data/cache/unrelated.dat" 'unrelated'
$v=Get-Content "$root/build/client-batches/fixture/verified.json" -Raw | ConvertFrom-Json
$log=$v.Logs[0].Path;$logText=[IO.File]::ReadAllText($log);WriteFixture $log ($logText+'extra')
Reject {Run Publish} 'Changed validated input'
WriteFixture $log $logText
Check ([IO.File]::ReadAllText("$root/build/gambler-interface/dev-client/Run Dev Client.bat") -ceq $launcher) 'Failed guards leave launcher intact'
Run Publish | Out-Null
$live="$root/build/gambler-interface/dev-client"
Check (Test-Path "$live/Developer-client-fixture.jar") 'New release published'
Check ((Get-FileHash "$live/Developer-client-fixture.jar").Hash -eq $built.CandidateHash) 'Release matches tested candidate'
Check ((Get-FileHash "$live/old.jar").Hash -eq $catalog.baselineSHA256) 'Old JAR retained'
Check ((Get-FileHash "$live/Run Dev Client.bat").Hash -eq (Get-FileHash "$root/client/gambler-interface/Run Dev Client.bat").Hash) 'Both maintained launchers match'
Check ([IO.File]::ReadAllText("$live/Run Dev Client.bat").Contains('-Xmx1024m -jar Developer-client-fixture.jar')) 'Only JAR target replaced'
Check (@(Get-ChildItem -LiteralPath $live -Filter '*.bat').Count -eq 1) 'One launcher'
Check ([IO.File]::ReadAllText("$root/build/client-batches/fixture/before/publish/live-launcher.bat") -ceq $launcher) 'Launcher backup exact'
Reject {Run Publish} 'already attempted'
Reject {Run Build} 'already attempted'

# Explicitly dependency-free new proofs work without cape references as well.
Run Prepare explicit-none @('explicit-none') | Out-Null
Run Build explicit-none | Out-Null
Run Verify explicit-none | Out-Null
$explicit=Get-Content "$root/build/client-batches/explicit-none/verified.json" -Raw | ConvertFrom-Json
Check (@($explicit.DependencyGroups).Count -eq 0 -and @($explicit.References).Count -eq 1) 'Explicit empty dependency groups stay empty'

# Cape plus texture select a deduplicated union while retaining every relevant guard.
foreach($input in @('bin/reference.dat','lib/reference.dat','data/cache/reference.dat','data/item/equipIds.txt','data/custom/infernal-cape/item.dat','client/infernal-cape/assets/texture318.dat')){WriteFixture (Join-Path $root $input) 'reference'}
Run Prepare references @('cape','texture') | Out-Null
Run Build references | Out-Null
Run Verify references | Out-Null
$references=Get-Content "$root/build/client-batches/references/verified.json" -Raw | ConvertFrom-Json
Check (($references.DependencyGroups -join ',') -eq 'cache,cape,runtime,texture') 'Selected proof dependency groups are unioned'
Check (@($references.Dependencies | Where-Object {$_.Path -eq "$root\data\cache\reference.dat"}).Count -eq 1) 'Shared cache input is recorded once'
Check (@($references.References).Count -eq 4) 'Cape and texture references plus Java are recorded'
foreach($input in @('bin/reference.dat','lib/reference.dat','data/cache/reference.dat','data/item/equipIds.txt','data/custom/infernal-cape/item.dat','client/infernal-cape/assets/texture318.dat')){
 $path=Join-Path $root $input;WriteFixture $path 'changed'
 Reject {Run Publish references} 'Changed validated input'
 WriteFixture $path 'reference'
}
foreach($directory in @('bin','lib','data/cache')){
 $added=Join-Path $root "$directory/added.dat";WriteFixture $added 'added'
 Reject {Run Publish references} 'dependency file set changed'
 Remove-Item -LiteralPath $added
}
$referencePath=Join-Path $root 'data/cache/reference.dat';Remove-Item -LiteralPath $referencePath
Reject {Run Publish references} 'Changed validated input'
WriteFixture $referencePath 'reference'
Check (!(Test-Path "$root/build/client-batches/references/publish-started.json")) 'Reference guard failures do not start publication'
Run Publish references | Out-Null
Check (Test-Path "$root/build/gambler-interface/dev-client/Developer-client-references.jar") 'Restored references permit isolated publication'

# Legacy catalog entries retain their original broad dependency safeguards.
Run Prepare legacy @('legacy') | Out-Null
Run Build legacy | Out-Null
Run Verify legacy | Out-Null
$legacy=Get-Content "$root/build/client-batches/legacy/verified.json" -Raw | ConvertFrom-Json
Check (($legacy.DependencyGroups -join ',') -eq 'cache,cape,runtime,texture' -and @($legacy.References).Count -eq 4) 'Unknown legacy proofs retain complete reference scope'
$catalog.proofs+=@{name='invalid';source='client/Proof.java';class='Proof';marker='PROOF passed';dependencyGroups=@('typo')}
$catalog | ConvertTo-Json -Depth 8 | Set-Content "$root/client/developer-client.json"
Reject {Run Prepare invalid @('invalid')} 'Unknown proof dependency group'
Check (!(Test-Path "$root/build/client-batches/invalid")) 'Invalid dependency scope fails before creating a batch'
$timer.Stop()
$summary="Client tooling: $script:checks checks passed in $([math]::Round($timer.Elapsed.TotalSeconds,3)) seconds. Fixtures: $root"
$summary | Set-Content "$root/result.txt";Write-Output $summary
