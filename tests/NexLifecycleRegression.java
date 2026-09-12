import java.lang.reflect.Method;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.combat.*;
import org.dementhium.model.npc.impl.Nex;
import org.dementhium.model.npc.impl.Nex.*;
import org.dementhium.model.player.Player;
import org.dementhium.tickable.Tick;

/** Focused phase/callback boundaries; full progression lives in the existing encounter suite. */
public final class NexLifecycleRegression {
    static final NexAreaEvent event = NexAreaEvent.getNexAreaEvent();
    static int checks;
    static List<String> failures = new ArrayList<String>();
    static void check(boolean ok, String message) { checks++; if (!ok && !failures.contains(message)) failures.add(message); }
    static Object get(Object o, Class<?> c, String key) throws Exception { return BossEncounterCompletionRegression.field(o,c,key); }
    static void set(Object o, Class<?> c, String key, Object value) throws Exception { BossEncounterCompletionRegression.field(o,c,key,value); }
    static Object call(String method, Class<?>[] types, Object... args) throws Exception { Method m=NexAreaEvent.class.getDeclaredMethod(method,types);m.setAccessible(true);return m.invoke(event,args); }
    static int value(Nex n,String key) throws Exception { return (Integer)get(n,Nex.class,key); }
    static Player player;
    static Nex setup(NexPhase phase) throws Exception {
        Nex n=NexBloodPhaseRegression.setup();player=NexBloodPhaseRegression.player;player.getWalkingQueue();
        set(n,Nex.class,"phase",phase);n.setHp(n.getMaximumHitPoints());
        set(event,NexAreaEvent.class,"random",new Random(918));event.setTime(1);
        // Only phase setup is synthetic. Attacks, cooldowns and task ordering are production paths.
        return n;
    }
    static void cycle() throws Exception { BossEncounterCompletionRegression.cycle(); }
    static void minionAggression() throws Exception {
        NexPhase[] phases={NexPhase.SMOKE,NexPhase.SHADOW,NexPhase.BLOOD,NexPhase.ICE};
        for(NexPhase phase:phases) {
            Nex n=setup(phase);
            for(NexPhase spawn:phases)event.spawnMinion(spawn);
            // Isolate minion attacks; vulnerability still opens through the real damage caller.
            BossEncounterCompletionRegression.active.remove(event);
            NPC[] minions=(NPC[])get(event,NexAreaEvent.class,"minions");
            NPC minion=minions[phase.ordinal()-1];
            player.setLocation(minion.getLocation().transform(minion.getLocation().getX()<2924?minion.size():-1,0,0));player.updateRegionArea();
            BossEncounterCompletionRegression.packets.get(player).clear();
            int hp=player.getHitPoints();
            for(int t=0;t<8;t++)cycle();
            check(minion.getCombatExecutor().getVictim()==null,phase+" shielded mage does not acquire nearby player");
            check(projectiles(minion.getDefinition().getProjectileId())==0&&player.getHitPoints()==hp,
                    phase+" shielded mage launches no attacks or damage");
            minion.getCombatExecutor().setVictim(player);
            cycle();
            check(minion.getCombatExecutor().getVictim()==null&&projectiles(minion.getDefinition().getProjectileId())==0,
                    phase+" shield also blocks an explicitly assigned combat target");
            int threshold=n.getMaximumHitPoints()*(5-phase.ordinal())/5;
            n.setHp(threshold+1);BossEncounterCompletionRegression.damage(player,n,1);
            check(minion.getAttribute("nex_vulnerable",false),phase+" actual threshold call activates matching mage");
            for(NPC other:minions)if(other!=minion)
                check(!NPCCombatContext.validPair(other,player),phase+" uncalled mage remains passive");
            for(int t=0;t<12;t++)cycle();
            check(minion.getCombatExecutor().getVictim()==player&&projectiles(minion.getDefinition().getProjectileId())>=2,
                    phase+" called mage naturally acquires player and attacks repeatedly");
        }
    }
    static void specialRecovery() throws Exception {
        for (NexPhase phase : new NexPhase[]{NexPhase.SMOKE,NexPhase.SHADOW,NexPhase.ICE}) {
            Nex n=setup(phase);cycle();int tell=World.getTicks();
            int interval=phase==NexPhase.ICE?6:4;
            for(int elapsed=1;elapsed<=interval;elapsed++) {
                cycle();
                check(value(n,"autoAttacksSinceSpecial")== (elapsed==interval?1:0),
                        phase+" first auto respects the entire special recovery interval at "+elapsed);
            }
            int first=World.getTicks();
            for(int elapsed=1;elapsed<=4;elapsed++) {
                cycle();check(value(n,"autoAttacksSinceSpecial")== (elapsed==4?2:1),
                        phase+" consecutive ordinary attacks stay four ticks apart");
            }
            System.out.println(phase+" special tell="+tell+", first auto="+first);
        }
    }
    static long projectiles(int id) {
        return BossEncounterCompletionRegression.packets.get(player).stream()
                .filter(m->m.getOpcode()==15&&m.getBuffer().getUnsignedShort(9)==id).count();
    }
    static void projectileOwnership() throws Exception {
        for(String mode:new String[]{"current","phase","reset","death","departure","replacement"}) {
            Nex n=setup(NexPhase.SHADOW);set(n,Nex.class,"specialPending",false);
            player.updateRegionArea();
            for(int t=0;t<10&&value(n,"autoAttacksSinceSpecial")==0;t++)cycle();
            check(value(n,"autoAttacksSinceSpecial")==1,"Executor launches projectile for "+mode);
            BossEncounterCompletionRegression.packets.get(player).clear();
            if(!mode.equals("current"))invalidate(n,mode);
            BossEncounterCompletionRegression.active.remove(event);n.getCombatExecutor().setVictim(null);
            cycle();check(projectiles(380)==(mode.equals("current")?1:0),mode+" delayed projectile has encounter/target ownership");
            cycle();check(projectiles(380)==(mode.equals("current")?1:0),mode+" projectile cannot be sent twice");
        }
    }
    static void smokeSelection() throws Exception {
        Nex n=setup(NexPhase.SMOKE);player.updateRegionArea();
        for(NexPhase minionPhase:new NexPhase[]{NexPhase.SMOKE,NexPhase.SHADOW,NexPhase.BLOOD,NexPhase.ICE})event.spawnMinion(minionPhase);
        for(int t=0;t<150;t++)cycle();
        check(!player.getAttribute("godmode",false)&&n.getPhase()==NexPhase.SMOKE&&!n.isProtectingMinion(),"Ordinary encounter stays before Fumus vulnerability without player damage");
        check(projectiles(306)>=3,"Natural smoke rotation repeatedly emits its current ordinary spell projectile");
        check(projectiles(374)==0&&!n.isSiphonMode()&&NexBloodPhaseRegression.reavers().isEmpty(),"Pre-Fumus smoke rotation does not enter blood autos, siphon or reaver summoning");
        System.out.println("Pre-Fumus smoke projectiles="+projectiles(306)+", blood projectiles="+projectiles(374));
    }
    static long shadowImpacts() {
        return BossEncounterCompletionRegression.packets.get(player).stream()
                .filter(m->m.getOpcode()==87&&m.getBuffer().getUnsignedShort(1)==383).count();
    }
    static void shadowDeadline() throws Exception {
        for(String mode:new String[]{"stay","walk","phase","reset","death","replacement"}) {
            Nex n=setup(NexPhase.SHADOW);player.updateRegionArea();cycle();
            List<?> traps=(List<?>)get(event,NexAreaEvent.class,"shadowLocations");
            check(traps.size()==1,"Shadow warning creates one trap for "+mode);
            BossEncounterCompletionRegression.active.remove(event);n.getCombatExecutor().setVictim(null);
            int hp=player.getHitPoints();
            if(!mode.equals("stay")&&!mode.equals("walk"))invalidate(n,mode);
            for(int elapsed=1;elapsed<=3;elapsed++) {
                if(mode.equals("walk")&&elapsed==2){player.getWalkingQueue().reset();player.getWalkingQueue().addToWalkingQueue(player.getViewportX()+1,player.getViewportY());}
                cycle();
                boolean due=elapsed==3&&(mode.equals("stay")||mode.equals("walk"));
                check(shadowImpacts()==(due?1:0),"Shadow graphic appears only with valid tick-three impact: "+mode+"/"+elapsed);
                if(!mode.equals("death"))check(mode.equals("stay")&&due?player.getHitPoints()<hp:player.getHitPoints()==hp,
                        "Shadow hit obeys deadline and actual movement: "+mode+"/"+elapsed);
                if(mode.equals("stay")||mode.equals("walk"))check(traps.isEmpty()==due,"Shadow warning remains until impact");
            }
            check(traps.isEmpty(),mode+" shadow trap cleans up");
            int lost=hp-player.getHitPoints();cycle();cycle();
            if(!mode.equals("death"))check(hp-player.getHitPoints()==lost,"Shadow damage occurs once");
            check(shadowImpacts()==(mode.equals("stay")||mode.equals("walk")?1:0),"Shadow graphic occurs once");
        }
    }
    static void shadowScenery() throws Exception {
        for(String mode:new String[]{"expire","phase","reset"}) {
            Nex n=setup(NexPhase.SHADOW);
            org.dementhium.model.map.GameObject scenery=null;
            for(int x=2934;x<=2940&&scenery==null;x++)for(int y=5213;y<=5219&&scenery==null;y++)
                for(org.dementhium.model.map.GameObject object:Location.locate(x,y,0).getObjectsSnapshot())
                    if(object.getType()>=9&&object.getType()<=21){scenery=object;break;}
            if(scenery==null)throw new AssertionError("Native northeast minion scenery must exist");
            final org.dementhium.model.map.GameObject original=scenery;
            player.setLocation(original.getLocation());player.updateRegionArea();
            cycle();
            check(((List<?>)get(event,NexAreaEvent.class,"shadowLocations")).contains(original.getLocation()),
                    mode+" shadow launches on northeast scenery tile");
            BossEncounterCompletionRegression.active.remove(event);n.getCombatExecutor().setVictim(null);
            BossEncounterCompletionRegression.packets.get(player).clear();
            if(!mode.equals("expire"))invalidate(n,mode);
            for(int t=0;t<3;t++)cycle();
            long restores=BossEncounterCompletionRegression.packets.get(player).stream().filter(m->m.getOpcode()==78
                    &&m.getBuffer().getUnsignedByte(1)+(m.getBuffer().getUnsignedByte(2)<<8)==original.getId()
                    &&m.getBuffer().getUnsignedByte(3)==((original.getType()<<2)|original.getRotation())).count();
            check(restores==1,mode+" cleanup restores original scenery ID, type and rotation exactly once");
            check(original.getLocation().getGameObjectType(original.getType())==original,
                    mode+" server scenery and collision ownership remain unchanged");
        }
    }
    static void sacrificeCancellation() throws Exception {
        // Cancel both before windup and after casting, while the projectile is still queued.
        for(int cancelAt:new int[]{3,4})for(String mode:new String[]{"phase","reset","death","departure","replacement"}) {
            Nex n=setup(NexPhase.BLOOD);set(n,Nex.class,"specialStep",1);player.updateRegionArea();cycle();
            BossEncounterCompletionRegression.active.remove(event);n.getCombatExecutor().setVictim(null);
            for(int t=0;t<cancelAt;t++)cycle();
            invalidate(n,mode);int hp=player.getHitPoints();double prayer=player.getSkills().getPrayerPoints();
            for(int t=cancelAt;t<9;t++)cycle();
            check(projectiles(374)==0,mode+" cancels sacrifice projectile after tick "+cancelAt);
            check(!n.hasTick("blood_sacrifice"),mode+" cancels sacrifice deadline");
            check(player.getSkills().getPrayerPoints()==prayer,mode+" cancels sacrifice prayer drain");
            if(!mode.equals("death"))check(player.getHitPoints()==hp,mode+" cancels sacrifice damage");
        }
    }
    static void thresholdBursts() throws Exception {
        for(NexPhase phase:new NexPhase[]{NexPhase.SMOKE,NexPhase.SHADOW,NexPhase.BLOOD,NexPhase.ICE}) {
            Nex n=setup(phase);set(n,Nex.class,"specialPending",false);
            NPC[] minions=new NPC[4];
            for(int i=0;i<4;i++){minions[i]=new NPC(Nex.FUMUS+i,Location.locate(2912+i,5216,0));minions[i].setAttribute("nex_vulnerable",false);minions[i].setUnrespawnable(true);World.getWorld().getNpcs().add(minions[i]);}
            set(event,NexAreaEvent.class,"minions",minions);
            int threshold=n.getMaximumHitPoints()*(5-phase.ordinal())/5;
            n.setHp(threshold+100);
            final int[] contacts={0};
            Damage crossing=Damage.getDamage(player,n,CombatType.MAGIC,100,true).onContact(()->contacts[0]++);
            n.getDamageManager().damage(player,crossing,org.dementhium.model.misc.DamageManager.DamageType.MAGE);
            check(n.isProtectingMinion(),phase+" threshold opens shield in the hit caller, before another world tick");
            check(contacts[0]==1,phase+" threshold-crossing hit retains its successful contact");
            Damage blocked=Damage.getDamage(player,n,CombatType.MAGIC,100,true).onContact(()->contacts[0]++);
            n.getDamageManager().damage(player,blocked,org.dementhium.model.misc.DamageManager.DamageType.MAGE);
            check(contacts[0]==1,phase+" shielded follow-up has no successful contact");
            for(int hit=0;hit<80;hit++)BossEncounterCompletionRegression.damage(player,n,500);
            check(n.getHitPoints()==threshold&&!n.isDead()&&!n.isDying(),phase+" same-cycle ordinary burst cannot bypass living mage");
            player.setAttribute("godmode",true);BossEncounterCompletionRegression.damage(player,n,1);
            check(n.getHitPoints()==threshold,phase+" God Mode still respects an active minion shield");
            check(Boolean.TRUE.equals(minions[phase.ordinal()-1].getAttribute("nex_vulnerable")),phase+" exposes matching mage only");
            for(int i=0;i<4;i++)if(i!=phase.ordinal()-1)check(!minions[i].getAttribute("nex_vulnerable",false),phase+" other mage stays shielded");
        }
        Nex n=setup(NexPhase.SMOKE);player.setAttribute("godmode",true);
        int hp=n.getHitPoints();
        BossEncounterCompletionRegression.damage(player,n,1);
        check(hp-n.getHitPoints()==750,"Intentional admin 750 damage bypasses the ordinary 500 hit cap");
    }
    static void cadence() throws Exception {
        for(NexPhase phase:new NexPhase[]{NexPhase.SMOKE,NexPhase.SHADOW,NexPhase.ICE}) {
            Nex n=setup(phase);int lastAuto=-1,previousStep=0,previousAutos=0;
            for(int t=0;t<170;t++) {
                cycle();int step=value(n,"specialStep"),autos=value(n,"autoAttacksSinceSpecial");
                if(step!=previousStep) {
                    if(lastAuto>=0)check(World.getTicks()-lastAuto>=4,phase+" special waits four ticks after last auto");
                    previousAutos=0;
                }
                if(autos>previousAutos)lastAuto=World.getTicks();
                previousStep=step;previousAutos=autos;
            }
            check(previousStep>=4 && lastAuto>0,phase+" repeatedly returns from specials to normal attacks");
        }
    }
    static void containment() throws Exception {
        Nex n=setup(NexPhase.ICE);set(n,Nex.class,"specialStep",1);
        cycle();int start=World.getTicks();
        for(int elapsed=1;elapsed<=5;elapsed++) {
            cycle();List<?> ice=(List<?>)get(event,NexAreaEvent.class,"containmentLocations");
            check(elapsed==5 ? !ice.isEmpty() : ice.isEmpty(),"Containment appears exactly five ticks after tell");
            check(value(n,"autoAttacksSinceSpecial")==0,"Containment resolves before next ordinary attack");
        }
        for(int t=0;t<6;t++)cycle();
        check(value(n,"autoAttacksSinceSpecial")>0,"Containment releases normal combat");
        check(((List<?>)get(event,NexAreaEvent.class,"containmentLocations")).isEmpty(),"Containment expires after five further ticks");
    }
    static void invalidate(Nex n,String mode) throws Exception {
        if(mode.equals("phase"))event.changePhase(NexPhase.FINAL);
        else if(mode.equals("reset"))n.resetCombatState();
        else if(mode.equals("death")){n.setHp(500);BossEncounterCompletionRegression.damage(player,n,100000);}
        else if(mode.equals("departure")){player.teleport(Location.locate(3200,3200,0), false);player.teleport(Location.locate(2927,5202,0), false);}
        else {call("resetEncounter",new Class<?>[]{boolean.class},false);}
    }
    static void positivePullAndPhaseImpact() throws Exception {
        Nex n=setup(NexPhase.SMOKE);set(n,Nex.class,"specialPending",false);
        BossEncounterCompletionRegression.active.remove(event);
        int hp=player.getHitPoints();check(event.drag(player),"Positive pull starts");n.getCombatExecutor().setVictim(null);
        cycle();cycle();check(player.getHitPoints()==hp,"Pull damage waits until after landing");
        cycle();int hit=hp-player.getHitPoints();check(hit>=300&&hit<=400,"Owned pull landing still deals its one hit");
        for(int t=0;t<3;t++)cycle();check(hp-player.getHitPoints()==hit,"Pull damage occurs exactly once");
        n=setup(NexPhase.SHADOW);set(n,Nex.class,"specialPending",false);
        for(int t=0;t<10 && value(n,"autoAttacksSinceSpecial")==0;t++)cycle();
        check(value(n,"autoAttacksSinceSpecial")==1,"Normal executor launches shadow ranged attack");
        hp=player.getHitPoints();event.changePhase(NexPhase.FINAL);
        BossEncounterCompletionRegression.active.remove(event);n.getCombatExecutor().setVictim(null);
        for(int t=0;t<3;t++)cycle();check(player.getHitPoints()==hp,"Phase entry cancels launched ordinary projectile");
        n=setup(NexPhase.SHADOW);set(n,Nex.class,"specialStep",1);cycle();hp=player.getHitPoints();
        cycle();check(player.getHitPoints()==hp,"Darkness waits two full ticks before first pulse");
        cycle();check(player.getHitPoints()<hp,"Darkness first pulse lands on tick two");
        event.changePhase(NexPhase.FINAL);hp=player.getHitPoints();
        for(int t=0;t<3;t++)cycle();check(player.getHitPoints()==hp,"Phase entry cancels remaining darkness pulses");
    }
    static void callbacks() throws Exception {
        for(String mode:new String[]{"phase","reset","death","departure","replacement"}) {
            Nex n=setup(NexPhase.SMOKE);set(n,Nex.class,"specialPending",false);
            check(event.drag(player),"Pull starts for "+mode);
            invalidate(n,mode);int hp=player.getHitPoints();
            // Disable arena selection, retaining actual world/player/NPC scheduler order.
            BossEncounterCompletionRegression.active.remove(event);n.getCombatExecutor().setVictim(null);
            for(int t=0;t<4;t++)cycle();
            if (!mode.equals("death")) check(player.getHitPoints()==hp,mode+" cancels pending pull damage");
            check(!player.hasTick("nex_drag"),mode+" cancels pull callback before damage (Wrath may still hit)");
            check(player.getForceWalk()==null,mode+" releases pull movement");

            n=setup(NexPhase.SMOKE);cycle();Tick virus=player.getTick("nex_virus");
            check(virus!=null,"Natural opening casts virus");invalidate(n,mode);
            BossEncounterCompletionRegression.active.remove(event);n.getCombatExecutor().setVictim(null);
            double prayer=player.getSkills().getPrayerPoints();
            cycle();check(player.getSkills().getPrayerPoints()==prayer,mode+" cancels virus drain");
        }
        Nex n=setup(NexPhase.ICE);cycle();
        // Prison target leaves and returns before the six-tick expiry; location alone is insufficient.
        player.teleport(Location.locate(3200,3200,0), false);player.teleport(Location.locate(2927,5202,0), false);
        BossEncounterCompletionRegression.active.remove(event);n.getCombatExecutor().setVictim(null);
        int hp=player.getHitPoints();for(int t=0;t<6;t++)cycle();
        check(player.getHitPoints()==hp,"Out-and-back departure cancels prison damage");
        check(((List<?>)get(event,NexAreaEvent.class,"icePrisonLocations")).isEmpty(),"Departed prison still cleans up shared ice");
    }
    public static void main(String[] args) throws Exception {
        CombatFixtures.init();org.dementhium.model.misc.GroundItemManager.load();
        if(args.length>0&&args[0].equals("minions"))minionAggression();
        else if(args.length>0&&args[0].equals("shadows")){shadowScenery();shadowDeadline();}
        else {minionAggression();specialRecovery();shadowScenery();shadowDeadline();sacrificeCancellation();thresholdBursts();projectileOwnership();smokeSelection();cadence();containment();positivePullAndPhaseImpact();callbacks();}
        call("resetEncounter",new Class<?>[]{boolean.class},false);
        if(!failures.isEmpty())throw new AssertionError(failures.toString());
        System.out.println("Nex lifecycle: "+checks+" checks passed");
    }
}
