$ErrorActionPreference='Stop'
$root=(Resolve-Path "$PSScriptRoot/../..").Path
$backup="$root/build/batches/boss-step4/before/source"
foreach($id in @(8133,1158,1160)){
 $p="$root/NDE/data/NPCs/NPCDefinition$id.xml";[xml]$xml=Get-Content -LiteralPath "$backup/NDE/data/NPCs/NPCDefinition$id.xml";$d=$xml.NpcDefinition
 if($id -eq 8133){$d.AttackLevel='320';$d.StrengthLevel='320';$d.DefenceLevel='310';$d.MagicLevel='350';$d.AttackSpeed='6';$d.PoisonImmune='false';$bonuses=@(50,50,50,150,0,25,200,100,150,250)}
 else{$d.AttackLevel='300';$d.StrengthLevel='300';$d.DefenceLevel='300';$d.MagicLevel='300';$d.RangeLevel='300';$d.AttackSpeed='4';$bonuses=if($id -eq 1158){@(50,50,50,50,50,50,50,50,550,550)}else{@(50,50,50,50,50,550,550,550,50,50)}}
 for($i=0;$i -lt 10;$i++){$d."Bonus$i"=[string]$bonuses[$i]};$xml.Save($p)
}
# Missing Spinolyp records were cache defaults (100 LP, zero combat levels).
foreach($id in 2891..2896){
 [xml]$xml=Get-Content "$root/NDE/data/NPCs/NPCDefinition1156.xml";$d=$xml.NpcDefinition
 $d.CombatLevel='76';$d.Examine='A sneaky, spiny, subterranean sea-dwelling scamp.';$d.LifePoints='750';$d.Respawn='36'
 $d.AttackLevel='50';$d.StrengthLevel='50';$d.DefenceLevel='50';$d.RangeLevel='75';$d.MagicLevel='50';$d.AttackSpeed='5'
 $d.AttackAnimation='2868';$d.DefenceAnimation='2869';$d.DeathAnimation='2870';$d.UsingMelee='false';$d.UsingRange='true';$d.UsingMagic='true';$d.Aggressive='true';$d.PoisonImmune='false'
 for($i=0;$i -lt 14;$i++){$d."Bonus$i"='0'};$xml.Save("$root/NDE/data/NPCs/NPCDefinition$id.xml")
}
$original=[IO.File]::ReadAllBytes("$backup/NDE/NPCDefinitions.bin");$script:pos=0
function Short {$n=256*[int]$original[$script:pos]+[int]$original[$script:pos+1];$script:pos+=2;if($n -ge 32768){$n-=65536};$n}
$records=@();for($i=0;$script:pos -lt $original.Length;$i++){$start=$script:pos;$id=Short;if($id -ne -1){if($id -ne $i){throw 'Record ID'};$script:pos+=2;while($original[$script:pos++] -ne 0){};$script:pos+=59};$records+=,@($original[$start..($script:pos-1)])}
if($records.Count -ne 13488){throw 'Record count'}
function WriteShort($value){$n=[int]$value;if($n -lt 0){$n+=65536};$stream.WriteByte([byte]($n -shr 8));$stream.WriteByte([byte]($n -band 255))}
foreach($id in @(8133,1158,1160,2891,2892,2893,2894,2895,2896)){
 [xml]$xml=Get-Content "$root/NDE/data/NPCs/NPCDefinition$id.xml";$d=$xml.NpcDefinition;$stream=[IO.MemoryStream]::new();WriteShort $id;WriteShort $d.CombatLevel
 $text=[Text.Encoding]::GetEncoding(1252).GetBytes([string]$d.Examine);$stream.Write($text,0,$text.Length);$stream.WriteByte(0)
 for($i=0;$i -lt 14;$i++){WriteShort $d."Bonus$i"};WriteShort $d.LifePoints;$stream.WriteByte([byte]$d.Respawn)
 foreach($field in @('AttackAnimation','DefenceAnimation','DeathAnimation','StrengthLevel','AttackLevel','DefenceLevel','RangeLevel','MagicLevel')){WriteShort $d.$field}
 $stream.WriteByte([byte]$d.AttackSpeed);foreach($field in @('StartGraphics','ProjectileId','EndGraphics')){WriteShort $d.$field}
 foreach($field in @('UsingMelee','UsingRange','UsingMagic','Aggressive','PoisonImmune')){$stream.WriteByte([byte]([string]$d.$field -eq 'true'))}
 $records[$id]=$stream.ToArray();$stream.Dispose()
}
$out=[IO.MemoryStream]::new();foreach($record in $records){$out.Write([byte[]]$record,0,$record.Length)};[IO.File]::WriteAllBytes("$root/NDE/NPCDefinitions.bin",$out.ToArray());$out.Dispose()
$custom=[IO.File]::ReadAllText("$backup/data/xml/custom_npcs.xml")
$entry=@"
  <npc>
    <handler>org.dementhium.model.npc.impl.KalphiteQueen</handler>
    <id>1158</id>
    <id>1160</id>
  </npc>
  <npc>
    <handler>org.dementhium.model.npc.encounter.EncounterAdd</handler>
    <id>2891</id>
    <id>2892</id>
    <id>2893</id>
    <id>2894</id>
    <id>2895</id>
    <id>2896</id>
  </npc>
"@
[IO.File]::WriteAllText("$root/data/xml/custom_npcs.xml",$custom.Replace('</map>',"$entry`r`n</map>"))
# Remove only exact duplicate stationary Spinolyp positions, keeping each original unique location.
$seen=@{};$lines=foreach($line in [IO.File]::ReadAllLines("$backup/data/npcs/npcspawns.txt")){
 if($line -match '^(289[1-6]) (\d+) (\d+) (\d+) '){$key=$matches[1..4] -join ',';if($seen[$key]){"// Boss step 4: duplicate Spinolyp: $line"}else{$seen[$key]=$true;$line}}else{$line}
}
[IO.File]::WriteAllLines("$root/data/npcs/npcspawns.txt",[string[]]$lines)