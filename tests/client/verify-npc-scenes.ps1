param([string]$ClientDirectory="C:\Users\Tcarn\Desktop\DyNamic's 639",[string]$DecompiledDirectory="C:\Users\Tcarn\Desktop\DyNamic's 639 - Decompiled")
$ErrorActionPreference='Stop'
$npcRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
Set-Location -LiteralPath $npcRoot
$npcBuild='build/instance-npc-sync'
New-Item -ItemType Directory -Force $npcBuild | Out-Null
$npcJar=Join-Path $ClientDirectory 'DyNamic-local.jar'
if((Get-FileHash -LiteralPath $npcJar).Hash -ne (Get-FileHash -LiteralPath (Join-Path $DecompiledDirectory 'binary/DyNamic-local.jar')).Hash){throw 'Client/archive mismatch'}
$npcTemplate=Get-Content tests/client/ClientNpcSceneDecoder.java.template -Raw
$npcProvenance=@()
foreach($npcMethod in @(@('Class98_Sub10_Sub13','static final void method1043(byte by)','RESET_METHOD'),@('Class98_Sub39','static final void method1468(int n2)','RETAINED_METHOD'),@('Class341','static final void method3810(byte by)','ADD_METHOD'))){
 $npcPath=Join-Path $DecompiledDirectory ('src/'+$npcMethod[0]+'.java')
 $npcSource=Get-Content -LiteralPath $npcPath -Raw
 $npcStart=$npcSource.IndexOf('    '+$npcMethod[1]+' {')
 if($npcStart -lt 0){throw ('Missing method '+$npcMethod[1])}
 $npcBrace=$npcSource.IndexOf('{',$npcStart);$npcLevel=1;$npcEnd=$npcBrace+1
 while($npcEnd -lt $npcSource.Length -and $npcLevel -gt 0){if($npcSource[$npcEnd] -eq '{'){$npcLevel++};if($npcSource[$npcEnd] -eq '}'){$npcLevel--};$npcEnd++}
 if($npcLevel -ne 0){throw 'Unterminated client method'}
 $npcTemplate=$npcTemplate.Replace('/* '+$npcMethod[2]+' */',$npcSource.Substring($npcStart,$npcEnd-$npcStart))
 $npcProvenance+=@{Source=$npcPath;SHA256=(Get-FileHash -LiteralPath $npcPath).Hash;Method=$npcMethod[1]}
}
[IO.File]::WriteAllText((Join-Path $npcRoot "$npcBuild/ClientNpcSceneDecoder.java"),$npcTemplate,[Text.UTF8Encoding]::new($false))
@{Jar=$npcJar;SHA256=(Get-FileHash -LiteralPath $npcJar).Hash;Methods=$npcProvenance;Boundary='Exact reset, retained-list and addition methods; binary bit readers; stubbed definitions/rendering/movement effects'} | ConvertTo-Json -Depth 5 | Set-Content "$npcBuild/provenance.json"
# Compile server-side helpers without the client JAR, which has a default-package PlayerUpdate name collision.
& 'C:/Users/Tcarn/Desktop/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21.0.11.v20260515-1531/jre/bin/javac.exe' --release 8 -encoding windows-1252 -cp "$npcBuild;bin;lib/*" -sourcepath src -d $npcBuild tests/InstancePartyRegression.java tests/InstanceOperationsRegression.java *> "$npcBuild/helper-compile.log"
if($LASTEXITCODE -ne 0){Get-Content "$npcBuild/helper-compile.log";exit 1}
$npcCp="$npcBuild;bin;lib/*;$npcJar"
$npcSources=@("$npcBuild/ClientNpcSceneDecoder.java",'tests/client/ClientNpcSceneRegression.java','src/org/dementhium/model/npc/NpcUpdate.java','src/org/dementhium/net/ActionSender.java')
& 'C:/Users/Tcarn/Desktop/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21.0.11.v20260515-1531/jre/bin/javac.exe' --release 8 -encoding windows-1252 -cp $npcCp -sourcepath src -d $npcBuild $npcSources *> "$npcBuild/compile.log"
if($LASTEXITCODE -ne 0){Get-Content "$npcBuild/compile.log";exit 1}
& 'C:/Program Files/Java/jre1.8.0_503/bin/java.exe' '-Djava.awt.headless=true' -Xmx512m -cp $npcCp ClientNpcSceneRegression *> "$npcBuild/client-regression.log"
$npcExit=$LASTEXITCODE
Get-Content "$npcBuild/client-regression.log" -Tail 12
exit $npcExit


