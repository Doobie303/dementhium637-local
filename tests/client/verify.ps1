param(
    [string]$ClientDirectory = "C:\Users\Tcarn\Desktop\DyNamic's 639",
    [string]$DecompiledDirectory = "C:\Users\Tcarn\Desktop\DyNamic's 639 - Decompiled"
)
$ErrorActionPreference = 'Stop'
$clientCheckRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
Set-Location -LiteralPath $clientCheckRoot
$clientCheckBuild = Join-Path $clientCheckRoot 'build/client-instance-packets'
New-Item -ItemType Directory -Force $clientCheckBuild | Out-Null
$clientCheckJar = Join-Path $ClientDirectory 'DyNamic-local.jar'
$clientCheckArchive = Join-Path $DecompiledDirectory 'binary/DyNamic-local.jar'
$clientCheckSource = Join-Path $DecompiledDirectory 'src/Class98_Sub36.java'
$clientCheckHash = (Get-FileHash -LiteralPath $clientCheckJar -Algorithm SHA256).Hash
if ($clientCheckHash -ne (Get-FileHash -LiteralPath $clientCheckArchive -Algorithm SHA256).Hash) {
    throw 'The running-client JAR and decompilation archive differ. Recheck decoder provenance.'
}
$clientCheckText = Get-Content -Raw -LiteralPath $clientCheckSource
$clientCheckStart = $clientCheckText.IndexOf('    static final void method1459(int n2) {')
if ($clientCheckStart -lt 0) { throw 'Cannot locate the client dynamic-map decoder' }
$clientCheckBrace = $clientCheckText.IndexOf('{',$clientCheckStart)
$clientCheckNesting = 1
$clientCheckEnd = $clientCheckBrace + 1
while ($clientCheckEnd -lt $clientCheckText.Length -and $clientCheckNesting -gt 0) {
    if ($clientCheckText[$clientCheckEnd] -eq '{') { $clientCheckNesting++ }
    if ($clientCheckText[$clientCheckEnd] -eq '}') { $clientCheckNesting-- }
    $clientCheckEnd++
}
if ($clientCheckNesting -ne 0) { throw 'Unterminated decoder method' }
$clientCheckMethod = $clientCheckText.Substring($clientCheckStart,$clientCheckEnd-$clientCheckStart)
$clientCheckTemplate = Get-Content -Raw -LiteralPath 'tests/client/ClientMapDecoder.java.template'
$clientCheckGenerated = Join-Path $clientCheckBuild 'ClientMapDecoder.java'
[IO.File]::WriteAllText($clientCheckGenerated,$clientCheckTemplate.Replace('/* CLIENT_METHOD */',$clientCheckMethod),[Text.UTF8Encoding]::new($false))
@{
    ClientJar=$clientCheckJar; JarSHA256=$clientCheckHash
    DecoderSource=$clientCheckSource; DecoderSHA256=(Get-FileHash -LiteralPath $clientCheckSource -Algorithm SHA256).Hash
    GeneratedDecoderSHA256=(Get-FileHash -LiteralPath $clientCheckGenerated -Algorithm SHA256).Hash
    Method='Class98_Sub36.method1459(-1048016408)'; Packet=31
    Boundary='Verbatim source method with stubbed scene/archive effects; unmodified binary packet readers and viewport table'
} | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $clientCheckBuild 'provenance.json')
$clientCheckCp = $clientCheckBuild + ';bin;lib/*;' + $clientCheckJar
$clientCheckSources = @($clientCheckGenerated,'tests/client/ClientInstancePacketRegression.java','src/org/dementhium/model/map/region/DynamicMapPacket.java','src/org/dementhium/net/codec/DefaultGameEncoder.java')
& 'C:/Users/Tcarn/Desktop/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21.0.11.v20260515-1531/jre/bin/javac.exe' --release 8 -encoding UTF-8 -cp $clientCheckCp -sourcepath src -d $clientCheckBuild $clientCheckSources *> (Join-Path $clientCheckBuild 'compile.log')
if ($LASTEXITCODE -ne 0) { Get-Content (Join-Path $clientCheckBuild 'compile.log'); exit 1 }
& 'C:/Program Files/Java/jre1.8.0_503/bin/java.exe' '-Djava.awt.headless=true' -Xmx512m -cp $clientCheckCp ClientInstancePacketRegression $clientCheckBuild *> (Join-Path $clientCheckBuild 'regression.log')
$clientCheckExit = $LASTEXITCODE
Get-Content (Join-Path $clientCheckBuild 'regression.log')
exit $clientCheckExit
