package org.dementhium.content.home;

import java.util.*;
import org.dementhium.content.DialogueManager;
import org.dementhium.model.*;
import org.dementhium.model.map.*;
import org.dementhium.model.npc.*;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;

/** Piscatoris service overlay. Existing shops, encounter entrances and economies stay authoritative. */
public final class HomeHub {
    private HomeHub() {}
    public enum Service {
        GUIDE(8863,2343,3691,6),
        MELEE(549,2330,3686,6), RANGED(550,2330,3687,6),
        MAGIC(553,2330,3688,6), SUPPLIES(520,2330,3689,6),
        REWARDS(9711,2333,3691,0), SLAYER(1597,2332,3697,4),
        SKILLING(1513,2335,3697,4), GAMBLER(2998,2338,3697,4),
        TOOLS(519,2341,3697,4);
        public final int id,x,y,faceDir;
        Service(int id,int x,int y,int faceDir){this.id=id;this.x=x;this.y=y;this.faceDir=faceDir;}
        public Location location(){return Location.locate(x,y,0);}
    }
    public enum Portal {
        PVM(2465,2332,3684), PVP(2466,2334,3684), ACTIVITIES(2467,2336,3684),
        ALTAR(47120,2335,3682);
        public final int id,x,y;
        Portal(int id,int x,int y){this.id=id;this.x=x;this.y=y;}
        public Location location(){return Location.locate(x,y,0);}
    }
    public static final class Destination {
        public final String name;
        public final Location location;
        public final boolean dangerous;
        Destination(String name,int x,int y,int z,boolean dangerous){this.name=name;this.location=Location.locate(x,y,z);this.dangerous=dangerous;}
    }
    private static Destination safe(String n,int x,int y,int z){return new Destination(n,x,y,z,false);}
    private static final Destination[] PVM = {
        safe("God Wars entrance",2881,5308,2), safe("Nex entrance",2902,5204,0),
        safe("Corporeal Beast entrance",2967,4383,2), safe("Kalphite Queen",3507,9494,0),
        safe("King Black Dragon",2259,4707,0), safe("Tormented demons",2570,5736,0),
        safe("Barrows",3565,3311,0), safe("Fight Caves entrance",2438,5169,0),
        safe("Training dungeon",3152,9575,0), safe("Taverley dungeon",2884,9798,0),
        safe("Rock crabs",2783,10102,0)
    };
    private static final Destination[] PVP = {
        safe("Edgeville - safe staging",3087,3501,0), safe("Duel Arena - friendly / staking",3367,3268,0),
        new Destination("Varrock Wilderness - dangerous",3243,3524,0,true),
        new Destination("Chaos Elemental - dangerous",3245,3917,0,true),
        new Destination("Lava maze - dangerous",3068,3860,0,true),
        safe("Safe PvP - free for all",2815,5513,0),
        new Destination("PvP island - dangerous",3807,2975,0,true)
    };
    private static final Destination[] ACTIVITIES = {
        safe("Fight Caves entrance",2438,5169,0),
        safe("Barrows",3565,3311,0),safe("Castle Wars lobby",2442,3089,0),
        safe("Puro-Puro",2592,4319,0),safe("Daemonheim entrance",3448,3708,0),
        safe("Legacy services / skillcapes",2344,3691,0),safe("Grand Exchange shops",3164,3470,0),
        safe("Warriors Guild",2879,3550,0)
    };
    public static List<Destination> destinations(Portal portal){
        return Collections.unmodifiableList(Arrays.asList(portal==Portal.PVM?PVM:portal==Portal.PVP?PVP:ACTIVITIES));
    }
    public static void spawnServices(){
        for(Service s:Service.values()){
            boolean found=false;
            for(NPC n:World.getWorld().getNpcs())if(n!=null&&n.getId()==s.id&&s.location().equals(n.getOriginalLocation())){found=true;break;}
            if(found)continue;
            NPC n=NPCLoader.getNPC(s.id);n.setLocation(s.location());n.setOriginalLocation(s.location());
            n.setDoesWalk(false);n.setFaceDir(s.faceDir);n.loadEntityVariables();World.getWorld().getNpcs().add(n);
        }
    }
    public static void spawnObjects(){
        for(Portal portal:Portal.values()){
            GameObject current=portal.location().getGameObjectType(10);
            if(current!=null&&current.getId()==portal.id)continue;
            if(current!=null)throw new IllegalStateException("Home object would replace scenery at "+portal.location());
            ObjectManager.addCustomObject(portal.id,portal.x,portal.y,0,10,0,false);
        }
    }
    private static Service service(NPC npc){
        if(npc==null||npc.isDead()||npc.isHidden()||World.getWorld().getNpcs().get(npc.getIndex())!=npc)return null;
        for(Service s:Service.values())if(npc.getId()==s.id&&s.location().equals(npc.getOriginalLocation())
                &&npc.getLocation().equals(s.location()))return s;
        return null;
    }
    public static boolean isGamblerStand(NPC npc){return service(npc)==Service.GAMBLER;}
    private static boolean usable(Player p,Location anchor){
        return p!=null&&p.isOnline()&&!p.isDead()&&p.hasReceivedStarter()&&p.getActivity()==Mob.DEFAULT_ACTIVITY
                &&!p.getAttribute("cantMove",false)
                &&org.dementhium.model.instance.InstanceAccess.owner(p)==null
                &&p.getLocation().withinRange(anchor,4)&&p.getLocation().getZ()==anchor.getZ();
    }
    private static final class Choice {
        final String label;final Runnable action;
        Choice(String label,Runnable action){this.label=label;this.action=action;}
    }
    private static final class Menu {
        final Location anchor;final long revision;final int expires;final Choice[] choices;
        Menu(Player p,Location anchor,Choice[] choices){this.anchor=anchor;this.revision=p.getInstanceRevision();this.expires=World.getTicks()+100;this.choices=choices;}
    }
    private static Choice option(String label,Runnable action){return new Choice(label,action);}
    private static void menu(Player p,Location anchor,String title,Choice... choices){
        if(!usable(p,anchor))return;
        int[] stages=new int[choices.length];String[] labels=new String[choices.length];
        for(int i=0;i<choices.length;i++){stages[i]=19200+i;labels[i]=choices[i].label;}
        DialogueManager.sendOptionDialogue(p,stages,labels);
        ActionSender.sendString(p,title,224+choices.length*2,1);
        p.setAttribute("homeMenu",new Menu(p,anchor,choices));
    }
    public static boolean select(Player p,int slot){
        Menu m=p.getAttribute("homeMenu");p.removeAttribute("homeMenu");
        if(m==null||slot<0||slot>=m.choices.length||World.getTicks()>m.expires
                ||m.revision!=p.getInstanceRevision()||!usable(p,m.anchor))return false;
        p.closeAll(false,true);m.choices[slot].action.run();return true;
    }
    private static void close(Player p){p.closeAll(false,true);}
    public static boolean handleObject(Player p,GameObject object){
        if(object==null)return false;
        for(Portal portal:Portal.values())if(object.getId()==portal.id&&object.getType()==10&&object.getLocation().equals(portal.location())){
            if(object.getLocation().getGameObjectType(10)!=object||!usable(p,portal.location()))return true;
            if(portal==Portal.ALTAR)preparation(p,portal.location());else travel(p,portal.location(),portal,0);
            return true;
        }
        return false;
    }
    private static void travel(Player p,Location anchor,Portal portal,int page){
        List<Destination> list=destinations(portal);List<Choice> choices=new ArrayList<Choice>();
        int start=page*3;
        for(int i=start;i<Math.min(start+3,list.size());i++){
            final Destination d=list.get(i);choices.add(option(d.name,()->chooseDestination(p,anchor,d)));
        }
        if(start+3<list.size())choices.add(option("More destinations",()->travel(p,anchor,portal,page+1)));
        choices.add(option(page>0?"Previous page":"Close",()->{if(page>0)travel(p,anchor,portal,page-1);else close(p);}));
        menu(p,anchor,portal==Portal.PVM?"PvM expeditions":portal==Portal.PVP?"PvP departures":"Activities and skilling",choices.toArray(new Choice[0]));
    }
    private static void chooseDestination(Player p,Location anchor,Destination d){
        if(d.dangerous){menu(p,anchor,"Dangerous PvP - items can be lost",option("Enter: "+d.name,()->depart(p,anchor,d)),option("Cancel",()->close(p)));}
        else depart(p,anchor,d);
    }
    private static void depart(Player p,Location anchor,Destination d){
        if(!usable(p,anchor))return;
        if(p.getAttribute("teleblock",0)>World.getTicks()){p.sendMessage("A teleport block has been cast on you!");return;}
        if(p.getLocation().getWildernessLevel()>20||!p.getActivity().onTeleport(p))return;
        // Resolve immediately after validation; no delayed callback can bypass a later restriction.
        p.getWalkingQueue().reset();p.resetCombat();p.teleport(d.location,false);p.graphics(1577);
        p.sendMessage("Destination: "+d.name+".");
    }
    private static void shop(Player p,int id){
        if(!World.getWorld().getShopManager().openShop(p,id))p.sendMessage("That shop is currently unavailable.");
    }
    private static void preparation(Player p,Location anchor){
        menu(p,anchor,"Combat preparation",
            option("Restore prayer",()->{p.animate(645);p.getSkills().restorePray(99);}),
            option("Change spellbook",()->menu(p,anchor,"Choose spellbook",
                option("Modern",()->p.setSpellBook(192)),option("Ancient",()->p.setSpellBook(193)),
                option("Lunar",()->p.setSpellBook(430)),option("Back",()->preparation(p,anchor)))),
            option("Change prayers / curses",()->menu(p,anchor,"Choose prayer book",
                option("Standard prayers",()->prayerBook(p,false)),option("Ancient curses",()->prayerBook(p,true)),
                option("Back",()->preparation(p,anchor)))),option("Close",()->close(p)));
    }
    private static void prayerBook(Player p,boolean ancient){
        p.getPrayer().closeAllPrayers();p.getPrayer().setAnctientCurses(ancient);p.getPrayer().setAncientBook(ancient);p.getPrayer().switchPrayBook(ancient);
    }
    public static boolean handleNpc(Player p,NPC npc){
        Service s=service(npc);if(s==null)return false;
        // These established dialogues retain their existing requirements, options and session ownership.
        if(s==Service.GAMBLER||s==Service.SLAYER||s==Service.REWARDS||s==Service.TOOLS||s==Service.SKILLING)return false;
        if(!usable(p,s.location()))return true;
        Location at=s.location();
        if(s==Service.GUIDE){
            menu(p,at,"Welcome to Piscatoris",
                option("PvM expeditions",()->travel(p,at,Portal.PVM,0)),option("PvP departures",()->travel(p,at,Portal.PVP,0)),
                option("Activities / legacy services",()->travel(p,at,Portal.ACTIVITIES,0)),
                option("Where is everything?",()->DialogueManager.sendDisplayBox(p,-1,"Bank booths and equipment shops are west of the green.","The south portals lead to PvM, PvP and activities.","The preparation altar is just south of the portals.","Rewards, the Gambler and tools are nearby.","Slayer and skillcapes retain their Piscatoris stands.")));
        }else if(s==Service.MELEE){menu(p,at,"Melee equipment",option("Armour",()->shop(p,549)),option("Swords and equipment",()->shop(p,551)),option("Other weapons",()->shop(p,552)),option("Close",()->close(p)));
        }else if(s==Service.RANGED){menu(p,at,"Ranged equipment",option("Bows, arrows and throwing weapons",()->shop(p,550)),option("Crossbows and bolts",()->shop(p,682)),option("Ranged armour",()->shop(p,683)),option("Close",()->close(p)));
        }else if(s==Service.MAGIC){menu(p,at,"Magic equipment",option("Runes and teleport supplies",()->shop(p,553)),option("Staves",()->shop(p,546)),option("Magic clothing",()->shop(p,461)),option("Close",()->close(p)));
        }else if(s==Service.SUPPLIES){menu(p,at,"Supplies",option("Food, potions and equipment",()->shop(p,520)),option("General store",()->shop(p,521)),option("Tools and crafting supplies",()->shop(p,519)),option("Close",()->close(p)));}
        return true;
    }
}
