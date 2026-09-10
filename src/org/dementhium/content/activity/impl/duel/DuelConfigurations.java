package org.dementhium.content.activity.impl.duel;
import java.util.*;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.areas.Area;
import org.dementhium.model.*;
import org.dementhium.model.map.Region;
import org.dementhium.model.player.*;
import org.dementhium.net.ActionSender;

public class DuelConfigurations {
    public enum Rules {
        RANGE(16),MELEE(32),MAGIC(64),FUN_WEAPONS(4096),FORFEIT(1),DRINKS(128),FOOD(256),PRAYER(512),MOVEMENT(2),OBSTACLES(1024),SPECIAL_ATTACKS(8192),SUMMONING(268435456),
        HAT(16384),CAPE(32768),AMULET(65536),WEAPON(131072),BODIE(262144),SHIELD(524288),LEG(2097152),GLOVE(8388608),BOOT(16777216),RING(67108864),ARROW(134217728);
        final int value;Rules(int value){this.value=value;}
    }
    public static final short[] FUN_WEAPONS={4566,2460,2461,2462,2463,2464,2465,2466,2467,2468,6541};
    public static final byte SUMMONING=5,OBSTACLES=6;
    private final BitSet rules=new BitSet();
    private final DuelActivity owner;
    private static final int[] SLOTS={0,1,2,3,4,5,7,9,10,12,13};
    private static final String[] LABELS={"No Ranged","No Melee","No Magic","Fun Weapons","No Forfeit","No Drinks","No Food","No Prayer","No Movement","Obstacles","No Special Attacks","Enable Summoning","No Helm","No Cape","No Amulet","No Weapon","No Body","No Shield","No Legs","No Gloves","No Boots","No Ring","No Ammo"};
    public DuelConfigurations(){this(null);}
    public DuelConfigurations(DuelActivity owner){this.owner=owner;}
    public BitSet snapshot(){return (BitSet)rules.clone();}
    public boolean getRule(Rules rule){return rules.get(rule.ordinal());}
    public boolean setRule(Rules rule,boolean flag){
        if(owner!=null)throw new IllegalStateException("Live duel rules must use versioned edits");
        rules.set(rule.ordinal(),flag);return true;
    }
    public boolean canSetRule(Player p,Rules rule){
        BitSet next=snapshot();next.flip(rule.ordinal());return valid(next,p);
    }
    private static boolean valid(BitSet next,Player p){
        boolean invalid=(next.get(0)&&next.get(1)&&next.get(2))||(next.get(3)&&(next.get(1)||next.get(15)))||(next.get(4)&&next.get(1));
        if(invalid&&p!=null)p.sendMessage("Those rules would prevent a fair, finishable duel.");return !invalid;
    }
    public boolean swapRule(Player p,Player other,Rules rule){
        if(owner!=null&&(!owner.editable(p)||owner.getOpponent(p)!=other))return false;
        BitSet before=snapshot(),next=snapshot();next.flip(rule.ordinal());
        if(rule==Rules.OBSTACLES&&next.get(9)){next.clear(8);next.clear(11);}
        if((rule==Rules.MOVEMENT||rule==Rules.SUMMONING)&&next.get(rule.ordinal()))next.clear(9);
        if(!valid(next,p))return false;
        rules.clear();rules.or(next);
        StringBuilder change=new StringBuilder();
        for(int n=0;n<LABELS.length;n++)if(before.get(n)!=next.get(n)){
            if(change.length()>0)change.append("; ");change.append(describe(n,next.get(n)));
        }
        BitSet delta=(BitSet)before.clone();delta.xor(next);
        if(owner!=null)owner.changed(p,change.toString(),delta);
        refresh(p);refresh(other);
        return true;
    }
    private static String describe(int n,boolean enabled){
        if(n==3)return enabled?"Fun weapons only":"Normal weapons allowed";
        if(n==9)return "Obstacles "+(enabled?"enabled":"disabled");
        if(n==11)return "Summoning "+(enabled?"enabled":"disabled");
        return LABELS[n].substring(3)+(enabled?" blocked":" allowed");
    }
    public void refreshLabels(Player p){
        for(int n=0;n<12;n++)ActionSender.sendString(p,
            (owner!=null&&owner.highlightRule(p,n)?"<col=ff4040>* ":"<col=bf751d>")+LABELS[n]+"</col>",631,27+2*n);
    }
    public boolean refresh(Player p){int value=0;for(Rules r:Rules.values())if(getRule(r))value|=r.value;ActionSender.sendConfig(p,286,value);return true;}
    public static boolean isFunWeapon(int id){for(short fun:FUN_WEAPONS)if(fun==id)return true;return false;}
    public boolean weaponAllowed(Player p,int id){
        if(getRule(Rules.FUN_WEAPONS)&&!isFunWeapon(id)){p.sendMessage("Fun weapons only: rubber chicken, flowers or mouse toy.");return false;}return true;
    }
    private Container equipmentToRemove(Player p){
        Container removed=new Container(14,false,false,true);
        for(int n=0;n<SLOTS.length;n++)if(getRule(Rules.values()[12+n])){
            Item item=p.getEquipment().get(SLOTS[n]);if(item!=null)removed.set(SLOTS[n],new Item(item));
        }
        Item weapon=p.getEquipment().get(3);
        if(weapon!=null&&getRule(Rules.SHIELD)&&weapon.getDefinition().isTwoHanded())removed.set(3,new Item(weapon));
        return removed;
    }
    public boolean canAccept(Player p){
        if(!valid(rules,p))return false;
        if(p.getFamiliar()!=null&&!getRule(Rules.SUMMONING)){p.sendMessage("You cannot bring familiars into this duel.");return false;}
        if(!weaponAllowed(p,p.getEquipment().getSlot(3)))return false;
        Container inventory=new Container(28,false);DuelRecovery.replace(inventory,p.getInventory().getContainer());
        if(!inventory.tryAddAll(equipmentToRemove(p))){p.sendMessage("Make inventory space for equipment removed by these rules.");return false;}
        return true;
    }
    public boolean canEquip(Player p,int normalizedSlot){
        if(normalizedSlot<0||normalizedSlot>=SLOTS.length)return false;
        if(rules.get(normalizedSlot+12)){p.sendMessage("That equipment slot is disabled for this duel.");return false;}return true;
    }
    public void removeEquipment(Player p){
        Container removed=equipmentToRemove(p);
        if(!p.getInventory().getContainer().tryAddAll(removed))throw new IllegalStateException("Equipment no longer fits");
        for(int n=0;n<removed.getSize();n++)if(removed.get(n)!=null)p.getEquipment().set(n,null);
        p.getEquipment().recalculateHpModifier();p.getEquipment().refresh();p.getInventory().refresh();p.getSkills().refresh();
    }
    private static String itemText(Container c){
        StringBuilder result=new StringBuilder();
        for(Item item:c.toArray())if(item!=null){
            if(result.length()>0)result.append("<br>");
            result.append(item.getDefinition().getName().replace("<","(").replace(">",")")).append(" x ").append(java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(item.getAmount()));
            if(item.getHealth()>0)result.append(" [charge ").append(item.getHealth()).append("]");
        }
        return result.toString();
    }
    public void sendSecondInterface(Player p,Player other){
        ActionSender.closeInventoryInterface(p);ActionSender.sendInterface(p,626);
        ActionSender.sendString(p,"Confirm "+(owner.isStaking()?"stake":"friendly duel")+" with "+other.getDisplayName(),626,20);
        ActionSender.sendString(p,"",626,45);
        Container mine=owner.stakeOf(p).getContainer(),theirs=owner.stakeOf(other).getContainer();
        ActionSender.sendString(p,mine.size()==0?"Absolutely nothing!":"",626,25);
        ActionSender.sendString(p,theirs.size()==0?"Absolutely nothing!":"",626,26);
        ActionSender.sendString(p,itemText(mine),626,46);ActionSender.sendString(p,itemText(theirs),626,47);
        for(int n=28;n<=44;n++)if(n!=32)ActionSender.sendString(p,"",626,n);
        ActionSender.sendString(p,"Stats restored; equipped rules enforced.",626,41);
        List<String> disabled=new ArrayList<String>();
        for(int n=12;n<LABELS.length;n++)if(rules.get(n))disabled.add(LABELS[n].substring(3));
        for(int n=0;n<disabled.size();n+=3)ActionSender.sendString(p,"Remove: "+String.join(", ",disabled.subList(n,Math.min(n+3,disabled.size()))),626,28+n/3);
        List<String> during=new ArrayList<String>();
        for(int n=0;n<12;n++)during.add(LABELS[n]+": "+(rules.get(n)?"ON":"OFF"));
        int[] rows={33,34,35,36,37,38,39,40,42,43,44};
        // Twelve explicit states fit eleven rows by combining the arena toggles.
        during.set(8,during.get(8)+"; "+during.get(9));during.remove(9);
        for(int n=0;n<rows.length;n++)ActionSender.sendString(p,during.get(n),626,rows[n]);
        if(getRule(Rules.SHIELD))p.sendMessage("No Shield also prevents two-handed weapons.");
    }
    public enum TeleportLocations {
        NORMAL_ARENA("NormalArena"),OBSTACLES_ARENA("ObstaclesArena"),SUMMONING_ARENA("SummoningArena"),CHALLENGE_ROOM("ChallengeRoom");
        final String area;TeleportLocations(String area){this.area=area;}
        public Area getArea(){return World.getWorld().getAreaManager().getAreaByName(area);}
    }
    private static boolean open(Location p){int mask=Region.getClippingMask(p.getX(),p.getY(),p.getZ());return (mask&0x12801fe)==0;}
    public Location[] startLocations(){
        TeleportLocations stage=getRule(Rules.SUMMONING)?TeleportLocations.SUMMONING_ARENA:getRule(Rules.OBSTACLES)?TeleportLocations.OBSTACLES_ARENA:TeleportLocations.NORMAL_ARENA;
        Area area=stage.getArea();if(area==null)return null;
        for(int rx=(area.swX-1)>>6;rx<=((area.nwX+1)>>6);rx++)for(int ry=(area.swY-1)>>6;ry<=((area.nwY+1)>>6);ry++){int region=(rx<<8)|ry;if(!org.dementhium.cache.format.LandscapeParser.parseLandscape(region,org.dementhium.util.MapXTEA.getKey(region)))return null;}
        List<Location> tiles=new ArrayList<Location>();
        for(int x=area.swX;x<area.nwX;x++)for(int y=area.swY;y<area.nwY;y++){Location p=Location.locate(x,y,0);if(open(p))tiles.add(p);}
        Collections.shuffle(tiles);
        for(Location a:tiles){
            if(!getRule(Rules.MOVEMENT)){for(Location b:tiles)if(!a.equals(b))return new Location[]{a,b};}
            else for(int[] d:new int[][]{{-1,0},{0,-1},{1,0},{0,1}}){Location b=a.transform(d[0],d[1],0);if(area.contains(b)&&open(b))return new Location[]{a,b};}
        }
        return null;
    }
    public static boolean teleport(Player p,TeleportLocations stage,boolean noMovement){
        if(stage==TeleportLocations.CHALLENGE_ROOM){p.teleport(Location.locate(3366,3266,0),false);return true;}return false;
    }
}
