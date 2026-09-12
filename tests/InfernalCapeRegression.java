import java.nio.*;
import java.nio.file.*;
import java.util.*;
import org.dementhium.cache.Cache;
import org.dementhium.content.*;
import org.dementhium.content.items.CustomItems;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.io.*;
import org.dementhium.model.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.player.*;
import org.dementhium.net.message.MessageBuilder;
public class InfernalCapeRegression extends DuelRegression {
 static int appearance(Player viewer,Player wearer){MessageBuilder b=new MessageBuilder();new PlayerUpdate(viewer).applyAppearanceMask(wearer,b);return b.getBuffer().getUnsignedShort(7)-32768;}
 public static void main(String[] args)throws Exception {
  root=Files.createTempDirectory(Paths.get("build/infernal-cape"),"test-accounts-");
  Cache.init();ItemDefinition.init();NPCDefinition.init();
  field(World.getWorld(),World.class,"areaManager",new AreaManager());field(World.getWorld(),World.class,"playerLoader",new PlayerLoader(root));
  ItemDefinition d=ItemDefinition.forId(CustomItems.INFERNAL_CAPE);
  check(Arrays.equals(d.getBonus(),new int[]{4,4,4,1,1,12,12,12,12,12,0,8,0,2,0}),"OSRS bonus vector");
  check(Arrays.equals(d.getAbsorptionBonus(),new int[3]),"no extra absorption");
  check(d.getWeight()==1.814&&!d.isTradeable()&&!d.isNoted()&&!d.isStackable(),"weight and item flags");
  check(d.getCacheDefinition().canEquip(),"native server definition recognizes wearable");
  Player admin=p();admin.getDefinition().setRights(2);
  Commands.handle(admin,new String[]{"infernalcape"});check(count(admin.getInventory().getContainer(),CustomItems.INFERNAL_CAPE)==0,"old client cannot spawn custom item");
  admin.getConnection().readClientSettings("|infernal-cape=1|gambler-ui=1");
  check(admin.getConnection().supportsInfernalCape()&&admin.getConnection().supportsGamblerInterface(),"capabilities coexist");
  Player ordinary=p();ordinary.getConnection().readClientSettings("|infernal-cape=1|gambler-ui=1");Commands.handle(ordinary,new String[]{"infernalcape"});check(count(ordinary.getInventory().getContainer(),CustomItems.INFERNAL_CAPE)==0,"admin-only grant");
  Commands.handle(admin,new String[]{"infernalcape"});check(count(admin.getInventory().getContainer(),CustomItems.INFERNAL_CAPE)==1,"admin grant");
  admin.getEquipment().equip(admin,0,0,CustomItems.INFERNAL_CAPE,true);check(admin.getEquipment().get(Equipment.SLOT_CAPE).getId()==CustomItems.INFERNAL_CAPE,"equips in cape slot");check(count(admin.getInventory().getContainer(),CustomItems.INFERNAL_CAPE)==0,"equip consumes inventory copy");
  for(int i=0;i<15;i++)check(admin.getBonuses().getBonus(i)==d.getBonus()[i],"equipped bonus "+i); Player old=p();
  for(byte gender=0;gender<2;gender++){admin.getAppearance().setGender(gender);check(appearance(admin,admin)==5156,"capable viewer actual appearance packet");check(appearance(old,admin)==ItemDefinition.forId(6570).getEquipId(),"old viewer actual appearance packet fallback");}
  check(World.getWorld().getPlayerLoader().save(admin),"save custom equipment");Player loaded=reload(admin);check(loaded.getEquipment().get(Equipment.SLOT_CAPE).getId()==CustomItems.INFERNAL_CAPE,"custom equipment persisted");
  old.getInventory().getContainer().set(0,new Item(CustomItems.INFERNAL_CAPE));old.getEquipment().equip(old,0,0,CustomItems.INFERNAL_CAPE,true);check(old.getEquipment().get(Equipment.SLOT_CAPE)==null&&count(old.getInventory().getContainer(),CustomItems.INFERNAL_CAPE)==1,"old client equip rejects without loss");
  for(String marker:new String[]{null,"","|infernal-cape=10|","|infernal-cape=1-extra|"}){admin.getConnection().readClientSettings(marker);check(!admin.getConnection().supportsInfernalCape(),"unknown markers clear capability");}
  System.out.println("PASS: "+checks+" Infernal Cape server checks; isolated storage "+root);
 }
}

