import java.nio.file.*;
import java.util.*;

import org.dementhium.cache.Cache;
import org.dementhium.content.*;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.io.PlayerLoader;
import org.dementhium.model.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.player.*;
import org.dementhium.net.message.MessageBuilder;

public class OsrsEquipmentRegression extends DuelRegression {
 static final int[][] BONUSES={
  {2,2,2,-4,-1,22,22,22,0,0,0,5,0,0,0},
  {0,0,0,-12,12,5,5,5,5,5,0,0,0,0,0},
  {0,0,0,8,0,5,5,5,8,5,0,0,0,0,1},
  {30,29,28,-5,-4,30,29,28,-5,-4,0,8,0,0,0},
  {0,0,0,0,0,9,9,9,9,9,0,0,0,4,0},
  {4,4,4,1,1,12,12,12,12,12,0,8,0,2,0}
 };
 static boolean appearanceContains(Player viewer,Player wearer,int equipId){
  MessageBuilder b=new MessageBuilder();new PlayerUpdate(viewer).applyAppearanceMask(wearer,b);
  for(int i=0;i+1<b.getBuffer().writerIndex();i++)if(b.getBuffer().getUnsignedShort(i)==32768+equipId)return true;
  return false;
 }
 static void max(Player p){for(int skill=0;skill<Skills.SKILL_COUNT;skill++){p.getSkills().setXp(skill,13034431);p.getSkills().setLevel(skill,99);}}
 public static void main(String[] args)throws Exception{
  root=Files.createTempDirectory(Paths.get("build/osrs-equipment"),"test-accounts-");
  Cache.init();ItemDefinition.init();NPCDefinition.init();
  field(World.getWorld(),World.class,"areaManager",new AreaManager());field(World.getWorld(),World.class,"playerLoader",new PlayerLoader(root));
  for(int id=OsrsEquipment.FIRST_ID;id<=OsrsEquipment.LAST_ID;id++){
   ItemDefinition d=ItemDefinition.forId(id);int index=id-OsrsEquipment.FIRST_ID;
   check(Arrays.equals(d.getBonus(),BONUSES[index]),d.getName()+" OSRS bonus vector");
   check(Arrays.equals(d.getAbsorptionBonus(),new int[3]),d.getName()+" has no 2011 absorption");
   check(d.getEquipId()==5157+index,d.getName()+" appends one wearable mapping");
   check(d.getCacheDefinition().canEquip(),d.getName()+" native cache definition is wearable");
  }
  check(ItemDefinition.forId(OsrsEquipment.PRIMORDIAL_BOOTS).getWeight()==1.814,"boot weight");
  check(ItemDefinition.forId(OsrsEquipment.MAX_CAPE).getWeight()==0.453,"max cape weight");
  check(ItemDefinition.forId(OsrsEquipment.ETERNAL_BOOTS).getBonus()[14]==1,"Eternal magic damage");
  check(ItemDefinition.forId(OsrsEquipment.PRIMORDIAL_BOOTS).isTradeable(),"boots tradeable");
  check(!ItemDefinition.forId(OsrsEquipment.AVERNIC_DEFENDER).isTradeable(),"Avernic untradeable");
  check(ItemDefinition.forId(OsrsEquipment.MAX_CAPE).getSkillRequirementId().size()==Skills.SKILL_COUNT,"Max cape requires every server skill");

  Player admin=p();admin.getDefinition().setRights(2);
  Commands.handle(admin,new String[]{"osrsgear"});
  for(int id=OsrsEquipment.FIRST_ID;id<=OsrsEquipment.LAST_ID;id++)check(count(admin.getInventory().getContainer(),id)==0,"legacy client cannot spawn "+id);
  admin.getConnection().readClientSettings("|infernal-cape=1|osrs-equipment=1|gambler-ui=1");max(admin);
  check(admin.getConnection().supportsInfernalCape()&&admin.getConnection().supportsOsrsEquipment()&&admin.getConnection().supportsGamblerInterface(),"capabilities coexist");
  Player ordinary=p();ordinary.getConnection().readClientSettings("|osrs-equipment=1|gambler-ui=1");Commands.handle(ordinary,new String[]{"osrsgear"});
  for(int id=OsrsEquipment.FIRST_ID;id<=OsrsEquipment.LAST_ID;id++)check(count(ordinary.getInventory().getContainer(),id)==0,"admin-only grant "+id);
  Commands.handle(admin,new String[]{"osrsgear"});
  for(int id=OsrsEquipment.FIRST_ID;id<=OsrsEquipment.LAST_ID;id++)check(count(admin.getInventory().getContainer(),id)==1,"admin grant "+id);

  Player old=p();
  for(int id=OsrsEquipment.FIRST_ID;id<=OsrsEquipment.LAST_ID;id++){
   ItemDefinition d=ItemDefinition.forId(id);int slot=d.getEquipmentSlot();
   admin.getEquipment().set(Equipment.SLOT_FEET,null);admin.getEquipment().set(Equipment.SLOT_SHIELD,null);admin.getEquipment().set(Equipment.SLOT_CAPE,null);admin.getInventory().getContainer().reset();admin.getInventory().getContainer().set(0,new Item(id));admin.getEquipment().equip(admin,0,0,id,true);
   check(admin.getEquipment().get(slot)!=null&&admin.getEquipment().get(slot).getId()==id,d.getName()+" equips in correct slot");
   admin.getBonuses().calculate();for(int i=0;i<15;i++)check(admin.getBonuses().getBonus(i)==d.getBonus()[i],d.getName()+" equipped bonus "+i);
   check(appearanceContains(admin,admin,d.getEquipId()),d.getName()+" capable appearance packet");
   check(appearanceContains(old,admin,OsrsEquipment.appearanceId(old,id,d.getEquipId())),d.getName()+" legacy appearance fallback");
  }
  Player low=p();low.getConnection().readClientSettings("|osrs-equipment=1|");low.getInventory().getContainer().set(0,new Item(OsrsEquipment.MAX_CAPE));low.getEquipment().equip(low,0,0,OsrsEquipment.MAX_CAPE,true);
  check(low.getEquipment().get(Equipment.SLOT_CAPE)==null&&count(low.getInventory().getContainer(),OsrsEquipment.MAX_CAPE)==1,"Max cape enforces all-99 requirement without loss");

  admin.getEquipment().set(Equipment.SLOT_FEET,new Item(OsrsEquipment.PRIMORDIAL_BOOTS));admin.getEquipment().set(Equipment.SLOT_SHIELD,new Item(OsrsEquipment.AVERNIC_DEFENDER));admin.getEquipment().set(Equipment.SLOT_CAPE,new Item(OsrsEquipment.INFERNAL_MAX_CAPE));
  check(World.getWorld().getPlayerLoader().save(admin),"save custom equipment");Player loaded=reload(admin);
  check(loaded.getEquipment().getSlot(Equipment.SLOT_FEET)==OsrsEquipment.PRIMORDIAL_BOOTS&&loaded.getEquipment().getSlot(Equipment.SLOT_SHIELD)==OsrsEquipment.AVERNIC_DEFENDER&&loaded.getEquipment().getSlot(Equipment.SLOT_CAPE)==OsrsEquipment.INFERNAL_MAX_CAPE,"custom equipment persists");
  for(String marker:new String[]{null,"","|osrs-equipment=10|","|osrs-equipment=1-extra|"}){admin.getConnection().readClientSettings(marker);check(!admin.getConnection().supportsOsrsEquipment(),"unknown markers clear OSRS capability");}
  System.out.println("PASS: "+checks+" OSRS equipment server checks; isolated storage "+root);
 }
}
