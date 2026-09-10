package org.dementhium.content;
import java.util.ArrayList;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Player;
/** OSRS item bonuses; visual adaptation is supplied by the development client. */
public final class InfernalCape {
 public static final int ID=20430, EQUIP_ID=5156;
 private InfernalCape(){}
 private static final byte[] CACHE_DEFINITION = loadDefinition();
 private static byte[] loadDefinition(){
  try{return java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("data/custom/infernal-cape/item.dat"));}
  catch(java.io.IOException e){throw new IllegalStateException("Missing Infernal Cape definition",e);}
 }
 public static byte[] cacheDefinition(){return CACHE_DEFINITION.clone();}
 public static ItemDefinition definition(){return new ItemDefinition(ID,"Infernal cape","A cape of fire, awarded to those who have defeated the Inferno.",EQUIP_ID,ItemDefinition.forId(6570).getRenderId(),new int[]{4,4,4,1,1,12,12,12,12,12,0,8,0,2,0},false,false,false,new ArrayList<Integer>(),new ArrayList<Integer>(),1.814,48000,32000,80000,80000,0,Equipment.SLOT_CAPE,new int[]{0,0,0},true,false);}
 public static boolean supported(Player p){return p.getConnection().supportsInfernalCape();}
 public static int appearanceId(Player viewer,int itemId,int equipId){return itemId==ID&&!supported(viewer)?ItemDefinition.forId(6570).getEquipId():equipId;}
}
