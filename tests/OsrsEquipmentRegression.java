import java.nio.file.*;
import java.util.*;

import org.dementhium.cache.Cache;
import org.dementhium.content.*;
import org.dementhium.content.items.CustomItems;
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
 static int appearanceId(Player viewer,Player wearer,int appearanceSlot){
  MessageBuilder b=new MessageBuilder();new PlayerUpdate(viewer).applyAppearanceMask(wearer,b);
  b.getBuffer().skipBytes(6); // Length byte, then gender/title/skull/prayer/hidden flags.
  for(int slot=0;slot<=appearanceSlot;slot++){
   int high=b.getBuffer().readUnsignedByte();
   int value=high==0?0:(high<<8)|b.getBuffer().readUnsignedByte();
   if(slot==appearanceSlot)return value-32768;
  }
  throw new AssertionError("Invalid appearance slot");
 }
 static void max(Player p){for(int skill=0;skill<Skills.SKILL_COUNT;skill++){p.getSkills().setXp(skill,13034431);p.getSkills().setLevel(skill,99);}}
 static void appearanceFallbacks(){
  int[] ids={20430,20431,20432,20433,20434,20435,20436,6570,11732};
  int[] legacy={1341,2589,489,1437,5053,4977,1341,1341,2589};
  Player wearer=p(),viewer=p();
  for(int flags=0;flags<4;flags++){
   viewer.getConnection().readClientSettings(((flags&1)!=0?"|infernal-cape=1|":"")+((flags&2)!=0?"|osrs-equipment=1|":""));
   for(byte gender=0;gender<2;gender++){
    wearer.getAppearance().setGender(gender);
    for(int index=0;index<ids.length;index++){
     int id=ids[index],slot=ItemDefinition.forId(id).getEquipmentSlot();
     wearer.getEquipment().getContainer().reset();wearer.getEquipment().set(slot,new Item(id));
     int expected=legacy[index];
     if(id==20430&&(flags&1)!=0)expected=5156;
     if(id>=20431&&id<=20436){
      if((flags&2)!=0)expected=5157+id-20431;
      else if(id==20436&&(flags&1)!=0)expected=5156;
     }
     int appearanceSlot=slot==Equipment.SLOT_FEET?10:slot==Equipment.SLOT_SHIELD?5:1;
     check(appearanceId(viewer,wearer,appearanceSlot)==expected,"packet appearance "+id+" capabilities "+flags+" gender "+gender);
    }
   }
  }
 }
 public static void main(String[] args)throws Exception{
  root=Files.createTempDirectory(Paths.get("build/osrs-equipment"),"test-accounts-");
  Cache.init();ItemDefinition.init();NPCDefinition.init();
  field(World.getWorld(),World.class,"areaManager",new AreaManager());field(World.getWorld(),World.class,"playerLoader",new PlayerLoader(root));
  check(ItemDefinition.MAX_SIZE==20437,"catalog retains the server definition capacity");
  check(CustomItems.osrsEquipmentIds().equals(Arrays.asList(20431,20432,20433,20434,20435,20436)),"catalog derives the complete ordered OSRS grant group");
  for(int id : CustomItems.osrsEquipmentIds()){
   ItemDefinition d=ItemDefinition.forId(id);int index=id-CustomItems.PRIMORDIAL_BOOTS;
   check(Arrays.equals(d.getBonus(),BONUSES[index]),d.getName()+" OSRS bonus vector");
   check(Arrays.equals(d.getAbsorptionBonus(),new int[3]),d.getName()+" has no 2011 absorption");
   check(d.getEquipId()==5157+index,d.getName()+" appends one wearable mapping");
   check(d.getCacheDefinition().canEquip(),d.getName()+" native cache definition is wearable");
  }
  check(ItemDefinition.forId(CustomItems.PRIMORDIAL_BOOTS).getWeight()==1.814,"boot weight");
  check(ItemDefinition.forId(CustomItems.MAX_CAPE).getWeight()==0.453,"max cape weight");
  check(ItemDefinition.forId(CustomItems.ETERNAL_BOOTS).getBonus()[14]==1,"Eternal magic damage");
  check(ItemDefinition.forId(CustomItems.PRIMORDIAL_BOOTS).isTradeable(),"boots tradeable");
  check(!ItemDefinition.forId(CustomItems.AVERNIC_DEFENDER).isTradeable(),"Avernic untradeable");
  check(ItemDefinition.forId(CustomItems.MAX_CAPE).getSkillRequirementId().size()==Skills.SKILL_COUNT,"Max cape requires every server skill");

  Player admin=p();admin.getDefinition().setRights(2);
  Commands.handle(admin,new String[]{"osrsgear"});
  for(int id : CustomItems.osrsEquipmentIds())check(count(admin.getInventory().getContainer(),id)==0,"legacy client cannot spawn "+id);
  admin.getConnection().readClientSettings("|infernal-cape=1|osrs-equipment=1|gambler-ui=1");max(admin);
  check(admin.getConnection().supportsInfernalCape()&&admin.getConnection().supportsOsrsEquipment()&&admin.getConnection().supportsGamblerInterface(),"capabilities coexist");
  Player ordinary=p();ordinary.getConnection().readClientSettings("|osrs-equipment=1|gambler-ui=1");Commands.handle(ordinary,new String[]{"osrsgear"});
  for(int id : CustomItems.osrsEquipmentIds())check(count(ordinary.getInventory().getContainer(),id)==0,"admin-only grant "+id);
  Commands.handle(admin,new String[]{"osrsgear"});
  for(int id : CustomItems.osrsEquipmentIds())check(count(admin.getInventory().getContainer(),id)==1,"admin grant "+id);

  Player old=p();max(old);
  for(int id : CustomItems.osrsEquipmentIds()){
   ItemDefinition d=ItemDefinition.forId(id);int slot=d.getEquipmentSlot();
   admin.getEquipment().set(Equipment.SLOT_FEET,null);admin.getEquipment().set(Equipment.SLOT_SHIELD,null);admin.getEquipment().set(Equipment.SLOT_CAPE,null);admin.getInventory().getContainer().reset();admin.getInventory().getContainer().set(0,new Item(id));admin.getEquipment().equip(admin,0,0,id,true);
   check(admin.getEquipment().get(slot)!=null&&admin.getEquipment().get(slot).getId()==id,d.getName()+" equips in correct slot");
   admin.getBonuses().calculate();for(int i=0;i<15;i++)check(admin.getBonuses().getBonus(i)==d.getBonus()[i],d.getName()+" equipped bonus "+i);
   old.getInventory().getContainer().reset();old.getInventory().getContainer().set(0,new Item(id));old.getEquipment().equip(old,0,0,id,true);
   check(old.getEquipment().get(slot)==null&&count(old.getInventory().getContainer(),id)==1,d.getName()+" legacy equip rejection without loss");
  }
  appearanceFallbacks();
  Player low=p();low.getConnection().readClientSettings("|osrs-equipment=1|");low.getInventory().getContainer().set(0,new Item(CustomItems.MAX_CAPE));low.getEquipment().equip(low,0,0,CustomItems.MAX_CAPE,true);
  check(low.getEquipment().get(Equipment.SLOT_CAPE)==null&&count(low.getInventory().getContainer(),CustomItems.MAX_CAPE)==1,"Max cape enforces all-99 requirement without loss");

  admin.getEquipment().set(Equipment.SLOT_FEET,new Item(CustomItems.PRIMORDIAL_BOOTS));admin.getEquipment().set(Equipment.SLOT_SHIELD,new Item(CustomItems.AVERNIC_DEFENDER));admin.getEquipment().set(Equipment.SLOT_CAPE,new Item(CustomItems.INFERNAL_MAX_CAPE));
  check(World.getWorld().getPlayerLoader().save(admin),"save custom equipment");Player loaded=reload(admin);
  check(loaded.getEquipment().getSlot(Equipment.SLOT_FEET)==CustomItems.PRIMORDIAL_BOOTS&&loaded.getEquipment().getSlot(Equipment.SLOT_SHIELD)==CustomItems.AVERNIC_DEFENDER&&loaded.getEquipment().getSlot(Equipment.SLOT_CAPE)==CustomItems.INFERNAL_MAX_CAPE,"custom equipment persists");
  for(String marker:new String[]{null,"","|osrs-equipment=10|","|osrs-equipment=1-extra|"}){admin.getConnection().readClientSettings(marker);check(!admin.getConnection().supportsOsrsEquipment(),"unknown markers clear OSRS capability");}
  System.out.println("PASS: "+checks+" OSRS equipment server checks; isolated storage "+root);
 }
}
