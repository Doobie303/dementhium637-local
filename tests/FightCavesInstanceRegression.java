import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.netty.channel.Channel;
import org.dementhium.cache.Cache;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.content.minigames.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.instance.*;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.map.Region;
import org.dementhium.model.misc.*;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.impl.TzTokJad;
import org.dementhium.model.player.*;
import org.dementhium.net.GameSession;
import org.dementhium.tickable.Tick;
import org.dementhium.util.MapXTEA;

/** Real-cache, isolated-cycle tests; never starts a server or writes player saves. */
public class FightCavesInstanceRegression {
    static int checks;
    static final List<Tick> tasks = new ArrayList<Tick>();
    static final List<Player> players = new ArrayList<Player>();
    static final Map<Player, AtomicBoolean> connections = new IdentityHashMap<Player, AtomicBoolean>();
    static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    static Player player(String name) {
        AtomicBoolean connected = new AtomicBoolean(true);
        Channel channel = (Channel) Proxy.newProxyInstance(Channel.class.getClassLoader(), new Class[]{Channel.class}, (proxy,m,args) -> {
            if (m.getName().equals("isConnected") || m.getName().equals("isOpen")) return connected.get();
            if (m.getReturnType() == boolean.class) return false;
            if (m.getReturnType() == int.class) return 0;
            return null;
        });
        Player p = new Player(new GameSession(channel), new PlayerDefinition(name, "unused"));
        p.setLocation(FightCaves.OUTSIDE_OF_CAVE); p.setHasReceivedStarter(true); p.setOnline(true);
        World.getWorld().getPlayers().add(p); players.add(p); connections.put(p, connected);
        return p;
    }
    static void pulse(int count) {
        for (int n=0;n<count;n++) for (Tick task : new ArrayList<Tick>(tasks)) if (!task.run()) tasks.remove(task);
    }
    static int amount(Player p, int id) { return p.getInventory().getContainer().getNumberOf(new Item(id)); }
    static void kill(NPC npc) { npc.setHp(0); npc.sendDead(); }
    static void clearWave(FightCavesSession s) {
        for (int split=0;split<3;split++) for (NPC npc : s.getLiveNpcs())
            if (!npc.isDead() && FightCaves.isBlockingNpc(npc.getId())) kill(npc);
    }
    static FightCavesSession start(InstanceManager m, Player p, int wave) {
        FightCavesSession s = FightCavesSession.start(m,p,wave);
        check(s != null && FightCaves.getSession(p)==s,"entry succeeds for "+p.getUsername());
        check(s.getInstance().contains(p.getLocation()),"entry translates player into copied cave");
        check(s.getLiveNpcs().size()>0,"entry spawns wave");
        return s;
    }
    static void closed(FightCavesSession s, Player p) {
        check(s.getInstance().getState()==GameInstance.State.CLOSED,"session closes");
        check(s.getInstance().getNpcCount()==0 && s.getInstance().getTaskCount()==0,"all NPCs and timers removed");
        check(FightCaves.getSession(p)==null && InstanceAccess.owner(p)==null,"session reference and membership removed");
        check(p.getAttribute("inFightCaves")==null && p.getAttribute("teleblock")==null,"cave flags removed");
    }
    static List<NPC> healers(FightCavesSession s) {
        List<NPC> result=new ArrayList<NPC>();
        for (NPC n:s.getLiveNpcs()) if(n.getId()==FightCaves.YT_HURKOT
                && !n.isDead() && !n.isHidden() && s.getInstance().owns(n)) result.add(n);
        return result;
    }
    static void summon(TzTokJad jad, Player p) throws Exception {
        Method method=jad.getCombatAction().getClass().getDeclaredMethod("spawnHealers", Mob.class);
        method.setAccessible(true); method.invoke(jad.getCombatAction(),p);
    }
    public static void main(String[] args) throws Exception {
        Cache.init(); MapXTEA.loadPackedFile(); NPCDefinition.init(); ItemDefinition.init(); GroundItemManager.load();
        Field areas=World.class.getDeclaredField("areaManager"); areas.setAccessible(true); areas.set(World.getWorld(),new AreaManager());
        InstanceManager manager=new InstanceManager(tasks::add); manager.beginCycle();
        try {
            Player p=player("caveone"), q=player("cavetwo");
            FightCavesSession a=start(manager,p,0), b=start(manager,q,2);
            check(a.getInstance()!=b.getInstance() && !p.getLocation().equals(q.getLocation()),"simultaneous solo copies");
            check(a.getCurrentWave()==1 && b.getCurrentWave()==3,"independent start-wave command state");
            check(FightCavesSession.start(manager,p,62)==null && a.getCurrentWave()==1,"duplicate entry cannot replace run");
            check(InstanceAccess.saveLocation(p).equals(FightCaves.OUTSIDE_OF_CAVE),"active save uses ordinary exit");
            int exits=0;
            for(int x=0;x<64;x++) for(int y=0;y<64;y++)
                for(GameObject object:a.getInstance().location(x,y,0).getObjectsSnapshot()) if(object.getId()==9357) {
                    exits++; check(InstanceAccess.canInteract(p,object) && !InstanceAccess.canInteract(q,object),"copied exit usable only by member");
                }
            check(exits>0,"actual exit object included in template");
            NPC kih=a.getLiveNpcs().get(0), kek=b.getLiveNpcs().get(0);
            check(FightCaves.getCombatTarget(kih)==p && FightCaves.getCombatTarget(kek)==q,"NPC combat targets isolated");
            kih.setLocation(a.getInstance().location(10,18,0));
            kih.getCombatExecutor().reset();
            new org.dementhium.task.impl.NPCTickTask(kih).execute();
            check(kih.getCombatExecutor().getVictim()==p,"real NPC tick reacquires its member beyond ordinary leash");
            check(a.getInstance().contains(kih.getLocation()),"clipped cave pursuit stays in copied map");
            check(p.isMulti() && kih.isMulti(),"multi-combat recognition survives translated coordinates");
            check(!FightCaves.isCaveOpponent(kih,q) && !InstanceAccess.canInteract(kih,kek),"cross-run combat rejected");
            check(a.spawnNpc(FightCaves.TZ_KIH,kek.getLocation(),false)==null,"foreign spawn location rejected");
            FightCaves.onCaveNpcDeath(kih,p);
            check(a.getLiveNpcs().contains(kih),"live NPC callback cannot progress wave");
            int beforeRetaliation=q.getHitPoints();
            Damage meleeHit=kek.updateHit(q,20,CombatType.MELEE);
            DamageManager.DamageHit retaliation=q.getDamageManager().getHits().getLast();
            check(meleeHit.getHit()==20&&q.getHitPoints()==beforeRetaliation-10,"Tz-Kek returns ten LP after a successful melee hit");
            check(retaliation.getAttacker()==kek&&retaliation.getType()==DamageManager.DamageType.DEFLECT&&retaliation.getType().toInteger()==4,"Tz-Kek retaliation uses client hit type 4 for reflected damage");
            kill(kek);
            check(b.getLiveNpcs().size()==2,"Tz-Kek creates two owned split NPCs");
            for(NPC split:b.getLiveNpcs()) check(split.getId()==FightCaves.TZ_KEK_SPLIT && split.getOwningInstance()==b.getInstance(),"split ownership");
            FightCaves.onCaveNpcDeath(kek,q);
            check(b.getLiveNpcs().size()==2,"duplicate death does not duplicate splits");
            check(a.getCurrentWave()==1 && a.getLiveNpcs().get(0)==kih,"other run unchanged by split");
            clearWave(b); pulse(4); check(b.getCurrentWave()==3,"wave delay waits five cycles");
            pulse(1); check(b.getCurrentWave()==4,"independent next wave");
            // Play every wave through the actual NPC death callbacks, including splits.
            Location waveThreeSpawn=null;
            for(int expected=1;expected<=63;expected++) {
                check(a.getCurrentWave()==expected,"full-run wave "+expected);
                for(NPC npc:a.getLiveNpcs()) check(npc.getOwningInstance()==a.getInstance() && a.getInstance().contains(npc.getLocation()),"wave NPC owned and placed");
                if(expected==3) waveThreeSpawn=a.getLiveNpcs().get(0).getLocation();
                if(expected==62) for(NPC npc:a.getLiveNpcs()) if(npc.getId()==FightCaves.KET_ZEK_SPAWN)
                    check(npc.getLocation().equals(waveThreeSpawn),"orange Ket-Zek reuses wave-three anchor");
                if(expected==63) {
                    check(a.getLiveNpcs().get(0).getLocation().equals(waveThreeSpawn),"Jad reuses wave-three anchor");
                    break;
                }
                clearWave(a); pulse(5);
            }
            check(b.getCurrentWave()==4,"63-wave run cannot advance concurrent participant");
            TzTokJad jad=(TzTokJad)a.getLiveNpcs().get(0);
            jad.setHp(1000); summon(jad,p);
            List<NPC> hs=healers(a); check(hs.size()==4,"Jad creates four healers in copied cave");
            for(NPC healer:hs) {
                check(healer.getOwningInstance()==a.getInstance(),"healer ownership");
                // Put within healing range; direct relocation stays within the same owned map.
                healer.setLocation(jad.getLocation());
            }
            pulse(4); check(jad.getHp()==1200,"owned healer timers heal their Jad");
            p.setLocation(jad.getLocation());
            hs.get(0).getCombatExecutor().setVictim(p);
            pulse(4); check(jad.getHp()==1350,"tagged nearby healer stops healing while other three continue");
            hs.get(0).getCombatExecutor().reset();
            pulse(4); check(jad.getHp()==1550,"undistracted healer resumes its owned stream");
            int before=jad.getHp(); a.getInstance().removeNpc(hs.get(0)); pulse(4);
            check(jad.getHp()==before+150,"removed healer cannot continue its timer");
            before=jad.getHp();jad.resetCombatState();pulse(4);
            check(jad.getHp()==before,"Jad generation reset cancels existing healer callbacks");
            jad.setHp(1000);
            CombatAction restarted=jad.getCombatAction().newSession();
            restarted.setInteraction(new Interaction(jad,p));
            check(restarted.commenceSession(),"reset Jad can begin a fresh attack");
            check(healers(a).size()==4,"reset replaces old healer set without duplicates");
            for(NPC healer:healers(a))healer.setLocation(jad.getLocation());
            pulse(4);check(jad.getHp()==1200,"fresh generation healers resume normally");
            kill(jad); check(amount(p,6570)==0,"Jad completion waits for death delay");
            FightCaves.onCaveNpcDeath(jad,p); pulse(3); check(amount(p,6570)==0,"duplicate callback cannot accelerate victory");
            pulse(1); closed(a,p);
            check(amount(p,6570)==1 && amount(p,6529)==16064,"victory grants exact existing rewards once");
            check(p.getLocation().equals(FightCaves.OUTSIDE_OF_CAVE),"winner returns to cave lobby");
            pulse(20); FightCaves.onCaveNpcDeath(jad,p);
            check(amount(p,6570)==1 && amount(p,6529)==16064,"stale Jad and healer work cannot repeat rewards");
            b.quit(); closed(b,q);
            check(amount(q,6529)==40 && amount(q,6570)==0,"quit consolation retains existing formula");
            // A dead player cannot receive victory while Jad's completion is pending.
            Player dead=player("cavedeath"); FightCavesSession death=start(manager,dead,62);
            dead.getInventory().addItem(995,50); kill(death.getLiveNpcs().get(0));
            dead.getSkills().setHitPoints(0); dead.getSkills().sendDead(); pulse(5); closed(death,dead);
            check(!dead.isDead() && dead.getHitPoints()>0 && amount(dead,995)==50,"safe cave death restores HP and keeps items");
            check(amount(dead,6570)==0 && amount(dead,6529)==8064,"death during completion earns consolation only");
            Player logout=player("cavelogout"); FightCavesSession disconnected=start(manager,logout,0);
            kill(disconnected.getLiveNpcs().get(0)); connections.get(logout).set(false); manager.maintain(); closed(disconnected,logout);
            check(logout.getLocation().equals(FightCaves.OUTSIDE_OF_CAVE),"disconnect evacuates before save");
            pulse(10); check(amount(logout,6529)==4,"cancelled next wave cannot award twice");
            Player tele=player("cavetele"); FightCavesSession teleport=start(manager,tele,0);
            tele.teleport(Location.locate(3200,3200,0),false); closed(teleport,tele);
            check(tele.getLocation().equals(Location.locate(3200,3200,0)),"ordinary teleport departure preserves destination");
            FightCavesSession again=start(manager,tele,0);
            FightCaves.onCaveNpcDeath(kih,tele); pulse(10);
            check(again.getCurrentWave()==1 && again.getLiveNpcs().size()==1,"old NPC cannot affect new/reused allocation");
            summon(jad,tele);
            check(again.getLiveNpcs().size()==1,"old Jad cannot spawn healers into a player's new run");
            again.getInstance().leave(tele); closed(again,tele);
            // A spawn failure must stop progression and leave the other instance intact.
            Player failed=player("cavespawnfail"); FightCavesSession spawnFailure=start(manager,failed,0);
            for(int x=0;x<64;x++) for(int y=0;y<64;y++) {
                Location tile=spawnFailure.getInstance().location(x,y,0);
                Region.addClipping(tile.getX(),tile.getY(),0,0x200100);
            }
            clearWave(spawnFailure); pulse(30);
            check(spawnFailure.getCurrentWave()==2 && spawnFailure.getLiveNpcs().isEmpty(),"failed wave stops without skipping onward");
            spawnFailure.quit(); closed(spawnFailure,failed);
            // Failure after admission rolls back attributes, map and membership without rewards.
            InstanceManager broken=new InstanceManager(task->{throw new IllegalStateException("test scheduler failure");});
            broken.beginCycle();
            try {
                Player rollback=player("caverollback");
                check(FightCavesSession.start(broken,rollback,0)==null,"startup scheduler failure reported");
                check(broken.getInstances().isEmpty() && FightCaves.getSession(rollback)==null && InstanceAccess.owner(rollback)==null,"startup rollback releases session");
                check(rollback.getAttribute("inFightCaves")==null && rollback.getAttribute("teleblock")==null,"startup rollback clears flags");
                check(rollback.getLocation().equals(FightCaves.OUTSIDE_OF_CAVE) && amount(rollback,6529)==0,"failed startup returns without rewards");
            } finally {broken.closeAll(); broken.endCycle();}
            // Full inventory rewards land outside the instance and survive its destruction.
            Player full=player("cavefull"); for(int n=0;n<28;n++) full.getInventory().addItem(1205,1);
            FightCavesSession fullRun=start(manager,full,62); kill(fullRun.getLiveNpcs().get(0)); pulse(4); closed(fullRun,full);
            int capeDrops=0,tokkulDrops=0;
            for(GroundItem item:new ArrayList<GroundItem>(GroundItemManager.getGroundItems())) {
                if(item.getLocation().equals(FightCaves.OUTSIDE_OF_CAVE)) {
                    if(item.getItem().getId()==6570) capeDrops+=item.getItem().getAmount();
                    if(item.getItem().getId()==6529) tokkulDrops+=item.getItem().getAmount();
                }
                GroundItemManager.discardGroundItem(item);
            }
            check(capeDrops==1 && tokkulDrops==16064,"full-inventory victory drops survive at ordinary lobby");
            // Two simultaneous Jads own distinct healers and repeating tasks.
            Player ja=player("jadone"), jb=player("jadtwo");
            FightCavesSession jadA=start(manager,ja,62), jadB=start(manager,jb,62);
            TzTokJad na=(TzTokJad)jadA.getLiveNpcs().get(0), nb=(TzTokJad)jadB.getLiveNpcs().get(0);
            na.setHp(1000); nb.setHp(1000); summon(na,ja); summon(nb,jb);
            check(healers(jadA).size()==4 && healers(jadB).size()==4,"both Jads have their own four healers");
            for(NPC n:healers(jadA)) n.setLocation(na.getLocation());
            for(NPC n:healers(jadB)) n.setLocation(nb.getLocation());
            pulse(4); check(na.getHp()==1200 && nb.getHp()==1200,"simultaneous healer effects stay local");
            jadA.quit(); closed(jadA,ja); pulse(4);
            check(na.getHp()==1200 && nb.getHp()==1400,"closing one healer set leaves other run healing");
            jadB.quit(); closed(jadB,jb);
            Player mejPlayer=player("cavemej"); FightCavesSession mejRun=start(manager,mejPlayer,29);
            NPC mej=mejRun.getLiveNpcs().get(0), hurt=mejRun.getLiveNpcs().get(1);
            hurt.setLocation(mej.getLocation()); hurt.setHp(100);
            check(FightCaves.getMejKotHealTarget(mej)==hurt,"MejKot selects injured monster in its own run");
            mejRun.quit(); closed(mejRun,mejPlayer);
            check(FightCaves.getMejKotHealTarget(mej)==null,"removed MejKot cannot find a new run's targets");
            // Exercise the public queued entrance, repeat entrance, admin restart and exit paths.
            Player command=player("cavecommand"), partner=player("cavepartner");
            FightCaves.startCaves(command,2);
            check(FightCaves.getSession(command)==null,"off-cycle entry is queued");
            InstanceManager production=InstanceManager.getSingleton(); production.beginCycle();
            try {
                production.drainRequests();
                FightCavesSession first=FightCaves.getSession(command);
                check(first!=null && first.getCurrentWave()==3,"queued entrance starts requested wave");
                FightCaves.startCaves(partner,0); FightCavesSession other=FightCaves.getSession(partner);
                FightCaves.startCaves(command,0);
                check(FightCaves.getSession(command)==first,"repeat entrance preserves active run");
                FightCaves.restartCaves(command,62);
                check(first.getInstance().getState()==GameInstance.State.CLOSED && FightCaves.getCurrentWave(command)==63,"admin wave restart closes own previous session");
                check(FightCaves.getSession(partner)==other && other.getCurrentWave()==1,"admin restart preserves other player's run");
                FightCaves.endCaves(partner);
                check(FightCaves.getSession(partner)==other && amount(partner,6570)==0,"legacy completion entry cannot bypass Jad death");
                FightCavesSession commandRun=FightCaves.getSession(command);
                check(FightCaves.handleEntranceObject(command,9357),"exit object handled"); closed(commandRun,command);
                FightCaves.quitCaves(partner); closed(other,partner);
            } finally { production.closeAll(); production.endCycle(); }
            // Framework callback is exactly once, after unbinding; callback failure closes the map.
            final int[] callbacks={0};
            GameInstance hook=manager.create(8,8,1,FightCaves.OUTSIDE_OF_CAVE,map->{
                map.copyMap(296,632,0,0,8,8,new int[]{0},new int[]{0});
                map.setDepartureHandler(member->{callbacks[0]++; check(InstanceAccess.owner(member)==null,"departure hook runs after unbind"); throw new IllegalStateException("test callback failure");});
            });
            hook.enter(p,hook.location(42,55,0));
            try {hook.leave(p); throw new AssertionError("callback failure must propagate");} catch(IllegalStateException expected) {}
            check(hook.getState()==GameInstance.State.CLOSED && callbacks[0]==1,"failed callback cannot leak empty allocation");
            hook.close(); check(callbacks[0]==1,"callback cannot repeat on cleanup retry");
            Player shut=player("caveshutdown"); FightCavesSession shutdown=start(manager,shut,62);
            check(manager.shutdown(),"manager shutdown completes cave cleanup"); closed(shutdown,shut); pulse(20);
            check(amount(shut,6570)==0 && amount(shut,6529)==8064,"shutdown abort cannot manufacture victory");
            check(manager.getInstances().isEmpty() && World.getWorld().getNpcs().size()==0,"no remaining sessions or monsters");
        } finally {
            manager.closeAll();
            for(Player p:players) World.getWorld().getPlayers().remove(p);
            manager.endCycle();
        }
        System.out.println("FightCavesInstanceRegression: "+checks+" checks passed");
    }
}
