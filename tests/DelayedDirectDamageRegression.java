import java.lang.reflect.*;
import java.util.*;
import org.dementhium.content.activity.impl.DefaultActivity;
import org.dementhium.content.misc.Drinking;
import org.dementhium.content.skills.thieving.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.combat.impl.specs.Disrupt;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.*;
import org.dementhium.net.message.*;
import org.dementhium.net.packethandlers.*;
import org.dementhium.tickable.Tick;

/** Real packet/action entries; isolated queue pump follows World's enqueue/run order. */
public class DelayedDirectDamageRegression {
    static int checks;
    static final List<Tick> active = new ArrayList<Tick>();
    static class Target extends NPC {
        Target() { super(1); }
        @Override public void loot(Mob killer) { } // Mock rewards only; real HP/death/timers remain.
    }
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); return f;
    }
    @SuppressWarnings("unchecked") static LinkedList<Tick> queue() throws Exception {
        return (LinkedList<Tick>) field(World.class, "ticksToAdd").get(World.getWorld());
    }
    static void ticks(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            active.addAll(queue()); queue().clear();
            for (Iterator<Tick> it = active.iterator(); it.hasNext();) if (!it.next().run()) it.remove();
        }
    }
    static NPC npc() throws Exception {
        CombatFixtures.clearPlayers(); active.clear(); queue().clear();
        NPC n = new Target(); n.setLocation(Location.locate(3216,3216,0)); n.setOriginalLocation(n.getLocation());
        n.setHp(1000); World.getWorld().getNpcs().add(n); return n;
    }
    static Player player(NPC n) { return CombatFixtures.player(n); }
    static void attack(Player p, Mob victim, int amount) {
        p.setAttribute("superhit", amount);
        if (victim.isNPC()) new NpcOption().handlePacket(p, new MessageBuilder(18).writeShort(victim.getIndex()).writeByte(0).toMessage());
        else {
            p.setActivity(new DefaultActivity() {
                @Override public boolean isCombatActivity(Mob a, Mob b, boolean message) { return true; }
            });
            new PlayerOption().handlePacket(p, new MessageBuilder(70).writeLEShort(victim.getIndex()).toMessage());
        }
    }
    static void transition(Mob m, String state) {
        if (state.equals("reset")) m.getNPC().resetCombatState();
        if (state.equals("teleport")) { Location l=m.getLocation(); m.teleport(l.transform(1,0,0),false); m.teleport(l,false); }
        if (state.equals("logout")) { m.getPlayer().setOnline(false); m.getPlayer().setOnline(true); }
        if (state.equals("death")) { m.getPlayer().getSkills().hit(1000); m.getPlayer().getSkills().setHitPoints(1000); }
        if (state.equals("instance")) m.markInstanceTransition();
    }
    static void superhit() throws Exception {
        for (boolean pvp : new boolean[]{false,true}) for (String state : new String[]{"none","teleport","logout","death","instance","target","reset"}) {
            if (pvp && state.equals("reset")) continue;
            if (!pvp && state.equals("target")) continue;
            NPC n=npc(); Player p=player(n); Mob v=pvp?player(n):n;
            if (pvp) World.getWorld().getPlayers().add(v.getPlayer());
            double xp=p.getSkills().getXp(Skills.MAGIC);
            attack(p,v,120); check(!p.canAnimate(),"Packet launches superhit");
            ticks(2); check(v.getHitPoints()==1000,"No early superhit");
            if (state.equals("reset")) transition(n,"reset");
            else if (state.equals("target")) transition(v,"teleport");
            else transition(p,state);
            ticks(6); check(v.getHitPoints()==1000,"No hit before tick nine");
            ticks(1); boolean hit=state.equals("none");
            check(v.getHitPoints()==(hit?880:1000),"Superhit lifetime "+pvp+" "+state);
            check(p.canAnimate(),"Superhit releases animation ownership "+state);
            if(hit) check(v.getDamageManager().getKiller()==p,"Superhit attacker credit");
            check(p.getSkills().getXp(Skills.MAGIC)==xp,"Custom superhit keeps no XP");
            ticks(2); check(v.getHitPoints()==(hit?880:1000),"Exactly one superhit"); n.destroy();
        }
        NPC n=npc(); Player p=player(n); attack(p,n,120); ticks(2); attack(p,n,60);
        ticks(7); check(n.getHp()==880&&!p.canAnimate(),"Earlier superhit lands without unlocking its successor");
        ticks(2); check(n.getHp()==820&&p.canAnimate(),"Independent successor lands and releases its animation"); n.destroy();
        n=npc(); p=player(n); p.setAttribute("godmode",true); attack(p,n,120); ticks(9);
        check(n.getHp()==250,"God mode retains shared 750 damage override"); n.destroy();
    }
    static void discharge() throws Exception {
        for(String state:new String[]{"none","reset","teleport","logout","death","instance"}) {
            NPC n=npc(); Player p=player(n); Item shield=new Item(11283);shield.setHealth(1);p.getEquipment().set(Equipment.SLOT_SHIELD,shield);
            p.getCombatExecutor().setVictim(n); p.getCombatExecutor().setLastAttacker(n);
            Message packet=new MessageBuilder(13).writeShort(387).writeShort(17).writeLEShortA(Equipment.SLOT_SHIELD).writeShort(11283).toMessage();
            Method m=ActionButtonHandler.class.getDeclaredMethod("handleButtons",Player.class,Message.class,int.class); m.setAccessible(true);
            m.invoke(new ActionButtonHandler(),p,packet,1);
            check(p.getAttribute("dischargeDelay",0)>0,"Operate packet launches discharge");
            check(shield.getHealth()==0,"Discharge consumes one shield charge at launch");
            ticks(2); if(state.equals("reset"))transition(n,state); else transition(p,state);
            ticks(1); check(n.getHp()==1000,"No discharge HP before fourth tick"); ticks(1);
            if(state.equals("none")) {
                check(n.getDamageManager().getHits().size()==1,"One discharge impact");
                check(n.getDamageManager().getHits().getLast().getAttacker()==p,"Discharge retains attacker even on zero roll");
            } else check(n.getHp()==1000&&n.getDamageManager().getHits().isEmpty(),"Cancelled discharge "+state);
            int hp=n.getHp(); ticks(3); check(n.getHp()==hp,"No repeated discharge"); n.destroy();
        }
        NPC n=npc();Player p=player(n);Item shield=new Item(11283);p.getEquipment().set(Equipment.SLOT_SHIELD,shield);
        p.getCombatExecutor().setVictim(n);p.getCombatExecutor().setLastAttacker(n);
        Message packet=new MessageBuilder(13).writeShort(387).writeShort(17).writeLEShortA(Equipment.SLOT_SHIELD).writeShort(11283).toMessage();
        Method m=ActionButtonHandler.class.getDeclaredMethod("handleButtons",Player.class,Message.class,int.class);m.setAccessible(true);
        m.invoke(new ActionButtonHandler(),p,packet,1);check(p.getAttribute("dischargeDelay",0)==0,"Empty shield cannot discharge");
        ticks(5);check(n.getDamageManager().getHits().isEmpty(),"Empty shield schedules no discharge impact");n.destroy();
        n=npc();p=player(n);shield=new Item(11283);shield.setHealth(3);p.getInventory().set(0,shield);
        org.dementhium.event.impl.interfaces.InventoryListener listener=new org.dementhium.event.impl.interfaces.InventoryListener();
        check(listener.interfaceOption(p,149,0,0,11283,0)&&shield.getHealth()==3,"Inspect reports DFS charges without changing them");
        check(listener.interfaceOption(p,149,0,0,11283,15),"Empty DFS inventory option handled");
        check(p.getInventory().get(0).getId()==11284&&p.getInventory().get(0).getHealth()==0,"Empty converts DFS to its uncharged state");n.destroy();
    }
    static void pickpocket() throws Exception {
        for(String state:new String[]{"none","reset","teleport","logout","death","instance"}) {
            NPC n=npc(); Player p=player(n); NPCPickpocketAction action=new NPCPickpocketAction(p,n,PickpocketableNPC.MAN);
            p.getSkills().setLevel(Skills.THIEVING,1); p.getSkills().setLevel(Skills.AGILITY,1);
            check(action.commence(p),"Pickpocket action begins");
            // Choose failure deterministically; finish and scheduler are production methods.
            field(NPCPickpocketAction.class,"succesful").set(action,false);
            check(!action.execute(p)&&action.execute(p),"Pickpocket action duration"); action.finish(p);
            check(p.getHitPoints()==1000,"Retaliation waits one tick");
            if(state.equals("reset"))transition(n,state); else transition(p,state);
            ticks(1); check(p.getHitPoints()==(state.equals("none")?990:1000),"Pickpocket lifetime "+state);
            check(p.getAttribute("stunned",false)==state.equals("none"),"Stun owned by current retaliation "+state); n.destroy();
        }
    }
    static void drink(Player p,int id) {
        p.getInventory().getContainer().set(0,new Item(id));
        check(new Drinking().interfaceOption(p,149,0,0,id,6),"Drink entry "+id);
    }
    static void potions() throws Exception {
        for(String state:new String[]{"none","teleport","logout","death","instance"}) {
            NPC n=npc(); Player p=player(n); drink(p,2450); ticks(2);
            check(p.getHitPoints()==1000,"Brew waits three ticks"); transition(p,state); ticks(1);
            check(state.equals("none")?p.getHitPoints()<1000:p.getHitPoints()==1000,"Brew lifetime "+state); n.destroy();
        }
        NPC n=npc(); Player p=player(n); drink(p,15332); p.fullRestore(); int hp=p.getHitPoints();
        p.processTicks(); p.processTicks(); check(p.getHitPoints()==hp,"Cleared overload cannot continue damaging restored player"); n.destroy();
        n=npc(); p=player(n); drink(p,15332); p.processTicks(); check(p.getHitPoints()==1000,"No early overload pulse");
        for(int i=0;i<9;i++)p.processTicks(); check(p.getHitPoints()==500,"Custom overload keeps five 100-LP pulses"); n.destroy();
    }
    static void disrupt() throws Exception {
        NPC n=npc(); Player p=player(n); p.setPersonalCombatXpRate(100); Disrupt action=new Disrupt();
        Interaction i=new Interaction(p,n); action.commenceSpecialAttack(i); i.getDamage().setHit(120);
        double magic=p.getSkills().getXp(Skills.MAGIC),hp=p.getSkills().getXp(Skills.CONSTITUTION);
        p.setPersonalCombatXpRate(5000); action.endSpecialAttack(i);
        check(Math.abs(p.getSkills().getXp(Skills.MAGIC)-magic-48*Skills.XP_MODIFIER)<0.001,"Disrupt captures launch XP rate");
        check(Math.abs(p.getSkills().getXp(Skills.CONSTITUTION)-hp-15.96*Skills.XP_MODIFIER)<0.001,"Disrupt captures constitution XP rate");
        action.endSpecialAttack(i); check(Math.abs(p.getSkills().getXp(Skills.MAGIC)-magic-48*Skills.XP_MODIFIER)<0.001,"Disrupt XP once"); n.destroy();
        for(String state:new String[]{"none","reset","logout","overkill"}) {
            n=npc(); n.setLocation(Location.locate(2380,5100,0)); n.setOriginalLocation(n.getLocation());
            NPC second=new Target(),third=new Target();
            second.setLocation(n.getLocation().transform(1,0,0)); second.setOriginalLocation(second.getLocation()); second.setHp(1000);
            third.setLocation(n.getLocation().transform(2,0,0)); third.setOriginalLocation(third.getLocation()); third.setHp(1000);
            p=player(n); p.setPersonalCombatXpRate(100); i=new Interaction(p,n); action=new Disrupt(); action.commenceSpecialAttack(i);
            check(i.getTargets()!=null&&i.getTargets().size()==3,"Real multi-target selection");
            for(ExtraTarget target:i.getTargets())target.getDamage().setHit(120);
            if(state.equals("overkill")) for(ExtraTarget target:i.getTargets())target.getVictim().getNPC().setHp(10);
            magic=p.getSkills().getXp(Skills.MAGIC); action.endSpecialAttack(i);
            double primary=p.getSkills().getXp(Skills.MAGIC)-magic;
            check(primary==(state.equals("overkill")?4:48)*Skills.XP_MODIFIER,"Primary actual damage XP");
            NPC next=i.getTargets().get(1).getVictim().getNPC(),last=i.getTargets().get(2).getVictim().getNPC();
            check(next.getHp()==(state.equals("overkill")?10:1000),"Bounce waits one scheduler tick");
            if(state.equals("reset")) { next.resetCombatState(); last.resetCombatState(); }
            if(state.equals("logout")) transition(p,"logout");
            p.setPersonalCombatXpRate(5000); ticks(1);
            boolean cancelled=state.equals("reset")||state.equals("logout");
            check(last.getHp()==(state.equals("overkill")?10:1000),"Third target waits two ticks"); ticks(1);
            check(p.getSkills().getXp(Skills.MAGIC)-magic==primary*(cancelled?1:3),"Owned bounce XP "+state);
            double after=p.getSkills().getXp(Skills.MAGIC); ticks(2); action.endSpecialAttack(i); ticks(2);
            check(p.getSkills().getXp(Skills.MAGIC)==after,"No duplicate bounce XP"); n.destroy(); second.destroy(); third.destroy();
        }
        n=npc(); p=player(n); Player v=player(n);
        field(org.dementhium.model.definition.PlayerDefinition.class,"username").set(v.getPlayerDefinition(),"direct-damage-other");
        new org.dementhium.content.activity.impl.DuelActivity(p,v,false);
        i=new Interaction(p,v); action=new Disrupt(); action.commenceSpecialAttack(i); i.getDamage().setHit(120);
        magic=p.getSkills().getXp(Skills.MAGIC); action.endSpecialAttack(i);
        check(v.getHitPoints()==1000&&!i.getDamage().isResolved(),"Duel consent state rejects direct impact");
        check(p.getSkills().getXp(Skills.MAGIC)==magic,"Rejected impact gives no Magic XP"); n.destroy();
    }
    public static void main(String[] args) throws Exception {
        try { CombatFixtures.init();
        for(String group:args.length==0?new String[]{"superhit","discharge","pickpocket","potions","disrupt"}:args)
            DelayedDirectDamageRegression.class.getDeclaredMethod(group).invoke(null);
        System.out.println("Delayed direct damage checks: "+checks); System.exit(0); } catch(Throwable failure) { failure.printStackTrace(); System.exit(1); }
    }
}
