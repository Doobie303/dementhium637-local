import java.lang.reflect.Field;
import java.util.*;
import org.dementhium.cache.Cache;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.content.instance.InstanceExamples;
import org.dementhium.content.instance.InstanceExamples.Kind;
import org.dementhium.content.instance.InstanceExamples.QuestStage;
import org.dementhium.model.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.instance.*;
import org.dementhium.model.map.*;
import org.dementhium.model.map.region.*;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.player.*;
import org.dementhium.tickable.Tick;
import org.dementhium.util.MapXTEA;

/** Real cache and simulated players; never launches the server or writes account saves. */
public final class InstanceContentRegression {
    static int checks;
    static boolean failSchedule;
    static final List<Tick> tasks = new ArrayList<Tick>();
    static void check(boolean ok, String text) { checks++; if (!ok) throw new AssertionError(text); }
    static void rejects(Runnable action, String text) {
        try { action.run(); } catch (IllegalArgumentException | IllegalStateException expected) { checks++; return; }
        throw new AssertionError(text);
    }
    static Player player(String name) {
        Player p = FightCavesInstanceRegression.player(name);
        p.getDefinition().setRights(2); return p;
    }
    static void pulse(int cycles) {
        for(int c=0;c<cycles;c++) for(Tick t:new ArrayList<Tick>(tasks)) if(!t.run()) tasks.remove(t);
    }
    static int amount(Player p, int id) { return p.getInventory().getContainer().getNumberOf(new Item(id)); }
    static GameObject object(InstanceExamples s, String anchor) {
        return s.getInstance().location(anchor).getGameObjectType(10);
    }
    static void click(InstanceExamples s, Player p, String anchor) {
        GameObject o = object(s,anchor);
        p.setLocation(o.getLocation().transform(-1,0,0));
        s.interact(p,o,true);
    }
    static void packetClick(Player p, GameObject o) {
        p.processTicks(); p.setAttribute("freezeTime",-1);
        org.dementhium.net.message.Message packet = new org.dementhium.net.message.MessageBuilder(76)
            .writeLEShort(o.getId()).writeByte(0).writeLEShortA(o.getLocation().getX())
            .writeShortA(o.getLocation().getY()).toMessage();
        new org.dementhium.net.packethandlers.ObjectPacketHandler().handlePacket(p,packet);
        for(int n=0;n<15;n++) { p.getWalkingQueue().getNextEntityMovement(); p.processTicks(); }
    }
    static InstanceExamples start(InstanceManager m, Player p, Kind kind) {
        InstanceExamples s = InstanceExamples.start(m,p,kind);
        check(s!=null && InstanceExamples.get(p)==s,"start "+kind);
        check(s.getInstance().getTemplate()!=null && s.getInstance().isMember(p),"template/member published");
        check(s.getInstance().location("entry").equals(p.getLocation()),"named entry");
        // A forged option-one packet is not evidence that the client can offer that action.
        menu(s,"exit","Enter");
        if (kind == Kind.QUEST) { menu(s,"clue","Search"); menu(s,"chest","Open"); }
        else menu(s,"resource","Chop down");
        return s;
    }
    static void menu(InstanceExamples session, String anchor, String expected) {
        GameObject object = object(session,anchor);
        String[] options = object.getDefinition().options;
        check(options != null && options.length > 0 && expected.equals(options[0]),
            anchor + " object " + object.getId() + " must expose first-option " + expected);
    }
    static void closed(InstanceExamples s, Player p) {
        check(s.getInstance().getState()==GameInstance.State.CLOSED,"session closed");
        check(InstanceExamples.get(p)==null && InstanceAccess.owner(p)==null,"content and membership cleared");
        check(s.getInstance().getTaskCount()==0,"no owned timers");
    }
    static void fill(Player p) {
        p.getInventory().getContainer().clear();
        for(int n=0;n<28;n++) p.getInventory().getContainer().set(n,new Item(4151,1));
    }
    static void templates(InstanceManager m) {
        Map<String,InstanceTemplate.Tile> anchors=new LinkedHashMap<String,InstanceTemplate.Tile>();
        anchors.put("entry",new InstanceTemplate.Tile(44,13,0));
        InstanceTemplate template = new InstanceTemplate("regression",360,648,8,8,0,0,
                InstanceTemplate.SourcePolicy.SNAPSHOT_LOADED_WORLD,anchors);
        anchors.clear(); check(template.getAnchors().size()==1,"defensive anchor snapshot");
        try { template.getAnchors().clear(); throw new AssertionError("mutable template"); }
        catch(UnsupportedOperationException expected) { checks++; }
        rejects(()->new InstanceTemplate.Tile(0,0,4),"invalid plane");
        rejects(()->template.getAnchor("missing"),"unknown anchor");
        rejects(()->new InstanceTemplate("bad",360,648,1,1,0,0,
                InstanceTemplate.SourcePolicy.SNAPSHOT_LOADED_WORLD,template.getAnchors()),"anchor bounds");
        rejects(()->new InstanceTemplate("bad",2047,648,8,8,0,0,
                InstanceTemplate.SourcePolicy.SNAPSHOT_LOADED_WORLD,template.getAnchors()),"source bounds");
        rejects(()->new InstanceTemplate("bad",360,648,8,8,0,1,
                InstanceTemplate.SourcePolicy.SNAPSHOT_LOADED_WORLD,template.getAnchors()),"anchor plane mismatch");
        GameInstance a=template.create(m,1,Location.locate(3200,3200,0),i->{});
        GameInstance b=template.create(m,1,Location.locate(3200,3200,0),i->{});
        InstanceTemplate.Tile local=a.toLocal(a.location("entry"));
        check(local.x==44 && local.y==13 && local.plane==0,"coordinate round trip");
        rejects(()->a.toLocal(b.location("entry")),"foreign coordinate rejected");
        check(a.getTemplate().getId().equals("regression"),"template identifier");
        a.close(); b.close();
        rejects(()->a.location("entry"),"closed named location rejected");
        int count=m.getInstances().size();
        rejects(()->template.create(m,1,Location.locate(3200,3200,0),i->{
            i.spawnObject(354,i.location("entry"),10,0);
            i.schedule(2,()->{throw new AssertionError("rolled back callback");});
            throw new IllegalStateException("Injected template initializer failure");
        }),"initializer failure");
        check(m.getInstances().size()==count,"template resource rollback"); pulse(4);
    }
    public static void main(String[] args) throws Exception {
        Cache.init(); MapXTEA.loadPackedFile(); ItemDefinition.init(); NPCDefinition.init(); GroundItemManager.load();
        Field areas=World.class.getDeclaredField("areaManager"); areas.setAccessible(true);
        areas.set(World.getWorld(),new AreaManager());
        InstanceManager manager=new InstanceManager(t->{ if(failSchedule) throw new IllegalStateException("Injected scheduler failure"); tasks.add(t); });
        manager.beginCycle();
        GameObject sourceChange=null;
        try {
            templates(manager);
            Player p=player("proofone"),q=player("prooftwo");
            Location outside=p.getLocation();
            p.getDefinition().setRights(0);
            check(InstanceExamples.start(manager,p,Kind.QUEST)==null,"ordinary player cannot enter proof");
            p.getDefinition().setRights(2);
            InstanceExamples a=start(manager,p,Kind.QUEST), b=start(manager,q,Kind.QUEST);
            check(InstanceExamples.start(manager,p,Kind.SKILL)==a,"repeat entry preserves run");
            check(a.getInstance().getId()!=b.getInstance().getId(),"independent quest maps");
            check(InstanceAccess.saveLocation(p).equals(outside),"save projects ordinary exit");
            a.interact(p,object(a,"clue"),true);
            check(a.getQuestStage()==QuestStage.FIND_CLUE,"distant interaction rejected");
            click(a,p,"chest"); check(a.getQuestStage()==QuestStage.FIND_CLUE,"clue required");
            a.interact(q,object(a,"clue"),true);
            check(a.getQuestStage()==QuestStage.FIND_CLUE,"foreign player rejected");
            click(a,p,"clue");
            check(a.getQuestStage()==QuestStage.OPEN_CHEST && b.getQuestStage()==QuestStage.FIND_CLUE,"independent quest progress");
            GameObject forged=new GameObject(375,object(a,"chest").getLocation(),10,0);
            a.interact(p,forged,true); check(a.getQuestRewards()==0,"forged identity rejected");
            fill(p); click(a,p,"chest");
            check(a.getQuestStage()==QuestStage.OPEN_CHEST && a.getQuestRewards()==0,"full inventory retains claim");
            p.getInventory().getContainer().set(0,new Item(995,5));
            click(a,p,"chest"); click(a,p,"chest");
            check(a.getQuestStage()==QuestStage.COMPLETE && a.getQuestRewards()==1 && amount(p,995)==6,"reward once with full stackable inventory");
            check(b.getQuestRewards()==0 && amount(q,995)==0,"other quest unrewarded");
            GameObject oldClue=object(a,"clue");
            click(a,p,"exit"); closed(a,p); check(p.getLocation().equals(outside),"portal returns to entry origin");
            a.interact(p,oldClue,true); check(a.getQuestRewards()==1,"stale completed callback inert");
            check(b.getInstance().isActive(),"other quest survives close");
            b.getInstance().leave(q); closed(b,q);
            p.getInventory().getContainer().clear();

            // Deliberate loaded-world snapshot: owned resources still start fresh.
            Location source=Location.locate(2928,5202,0);
            check(source.getGameObjectType(10)==null,"test source resource slot is empty");
            sourceChange=ObjectManager.addCustomObject(1342,source.getX(),source.getY(),0,10,0);
            a=start(manager,p,Kind.SKILL); b=start(manager,q,Kind.SKILL);
            check(a.getInstance().getTemplate().getSourcePolicy()==InstanceTemplate.SourcePolicy.SNAPSHOT_LOADED_WORLD,"explicit source policy");
            check(object(a,"resource").getId()==1276 && object(b,"resource").getId()==1276,"fresh nodes override copied depleted source slot");
            int sourceMask=Region.getClippingMask(source.getX(),source.getY(),0);
            double woodXp=p.getSkills().getXp(Skills.WOODCUTTING), woodGain=25*p.getSkills().getXpModifierForSkill(Skills.WOODCUTTING);
            click(a,p,"resource"); click(a,p,"resource");
            check(a.getInstance().getTaskCount()==1,"duplicate harvest click schedules once");
            pulse(2); check(amount(p,1511)==0,"harvest waits for delay");
            pulse(1);
            check(a.getHarvests()==1 && amount(p,1511)==1 && a.isDepleted(),"harvest awards once and depletes");
            check(p.getSkills().getXp(Skills.WOODCUTTING)==woodXp+woodGain,"harvest preserves XP modifier");
            check(object(a,"resource").getId()==1342 && object(b,"resource").getId()==1276,"tree visuals isolated");
            check(source.getGameObjectType(10)==sourceChange && Region.getClippingMask(source.getX(),source.getY(),0)==sourceMask,"source objects and collision unchanged");
            click(a,p,"resource"); pulse(7);
            check(a.getHarvests()==1 && a.isDepleted(),"stump cannot harvest and regrowth delayed");
            pulse(1); check(!a.isDepleted() && object(a,"resource").getId()==1276,"resource regrows");
            click(a,p,"resource"); p.setLocation(a.getInstance().location("entry")); pulse(3);
            check(a.getHarvests()==1 && !a.isDepleted(),"movement cancels reward");
            fill(p); click(a,p,"resource"); pulse(3);
            check(a.getHarvests()==1 && !a.isDepleted(),"full inventory leaves tree intact");
            p.getInventory().getContainer().clear();
            GameObject oldTree=object(a,"resource"); click(a,p,"resource");
            List<Tick> stale=new ArrayList<Tick>(tasks);
            InstanceAccess.depart(p,false); closed(a,p);
            InstanceExamples fresh=start(manager,p,Kind.SKILL);
            for(Tick t:stale) t.run(); pulse(12);
            a.interact(p,oldTree,true);
            check(amount(p,1511)==0 && fresh.getHarvests()==0 && object(fresh,"resource").getId()==1276,"old harvest cannot affect later run");
            click(fresh,p,"resource"); pulse(3);
            InstanceAccess.depart(p,false); closed(fresh,p); pulse(12);
            check(object(b,"resource").getId()==1276 && b.getHarvests()==0,"old regrowth cannot alter other copy");
            InstanceAccess.depart(q,true); closed(b,q);
            ObjectManager.discardCustomObject(sourceChange); sourceChange=null;

            InstanceExamples disconnect=start(manager,p,Kind.SKILL);
            click(disconnect,p,"resource"); int before=amount(p,1511);
            FightCavesInstanceRegression.connections.get(p).set(false); manager.maintain(); pulse(12);
            closed(disconnect,p); check(amount(p,1511)==before,"disconnect cancels harvest");
            FightCavesInstanceRegression.connections.get(p).set(true);
            InstanceExamples death=start(manager,p,Kind.SKILL);
            click(death,p,"resource"); p.getSkills().setHitPoints(0); p.getSkills().sendDead(); pulse(8);
            closed(death,p); check(!p.isDead() && amount(p,1511)==before,"safe death cancels harvest and preserves inventory");
            InstanceExamples failure=start(manager,p,Kind.SKILL);
            failSchedule=true;
            final InstanceExamples failed=failure;
            rejects(()->click(failed,p,"resource"),"scheduler failure surfaced");
            failSchedule=false; closed(failure,p);
            InstanceExamples routed=start(manager,p,Kind.QUEST);
            packetClick(p,object(routed,"clue"));
            check(routed.getQuestStage()==QuestStage.OPEN_CHEST,"real object packet routes crate click");
            packetClick(p,object(routed,"chest"));
            check(routed.getQuestRewards()==1,"real object packet routes reward");
            packetClick(p,object(routed,"exit")); closed(routed,p);
            InstanceExamples routedSkill=start(manager,p,Kind.SKILL);
            int routedBefore=amount(p,1511);
            packetClick(p,object(routedSkill,"resource")); pulse(3);
            check(amount(p,1511)==routedBefore+1 && routedSkill.isDepleted(),"real object packet routes tree harvest");
            routedSkill.getInstance().close(); closed(routedSkill,p);
            InstanceExamples shut=start(manager,p,Kind.QUEST);
            check(manager.shutdown(),"shutdown closes example"); closed(shut,p); pulse(12);
            check(manager.getInstances().isEmpty(),"no retained sessions");
            check(tasks.isEmpty(),"scheduler drains cancelled wrappers");
        } finally {
            failSchedule=false; manager.closeAll();
            if(sourceChange!=null) ObjectManager.discardCustomObject(sourceChange);
            for(Player p:FightCavesInstanceRegression.players) World.getWorld().getPlayers().remove(p);
            manager.endCycle();
        }
        System.out.println("InstanceContentRegression: "+checks+" checks passed");
    }
}






