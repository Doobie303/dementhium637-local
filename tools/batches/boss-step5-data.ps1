$ErrorActionPreference='Stop'
$root=(Resolve-Path "$PSScriptRoot/../..").Path
$backup="$root/build/batches/boss-step5/before/source"
$bytes=[IO.File]::ReadAllBytes("$backup/NDE/NPCDefinitions.bin");$pos=0;$found=$false
for($slot=0;$pos -lt $bytes.Length;$slot++){
 $id=256*[int]$bytes[$pos]+[int]$bytes[$pos+1];$pos+=2;if($id -eq 65535){continue};if($id -ne $slot){throw 'Invalid packed ID'}
 $pos+=2;while($bytes[$pos++] -ne 0){}
 if($id -eq 51){$bytes[$pos+50]=9;$bytes[$pos+51]=161;$found=$true};$pos+=59
}
if(!$found -or $slot -ne 13488){throw 'Missing frost profile or bad record count'}
[IO.File]::WriteAllBytes("$root/NDE/NPCDefinitions.bin",$bytes)
[xml]$xml=Get-Content "$backup/NDE/data/NPCs/NPCDefinition51.xml";$xml.NpcDefinition.ProjectileId='2465';$xml.Save("$root/NDE/data/NPCs/NPCDefinition51.xml")
$entry=@"
  <npc>
    <handler>org.dementhium.model.npc.impl.FrostDragon</handler>
    <id>51</id>
  </npc>
"@
$s=[IO.File]::ReadAllText("$backup/data/xml/custom_npcs.xml");[IO.File]::WriteAllText("$root/data/xml/custom_npcs.xml",$s.Replace('</map>',"$entry`r`n</map>"))
