import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.netty.channel.Channel;
import org.dementhium.cache.Cache;
import org.dementhium.cache.format.LandscapeParser;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.content.instance.PartyInstanceExample;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.instance.*;
import org.dementhium.model.map.*;
import org.dementhium.model.map.region.*;
import org.dementhium.model.misc.*;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.*;
import org.dementhium.net.GameSession;
import org.dementhium.net.message.Message;
import org.dementhium.tickable.Tick;
import org.dementhium.util.MapXTEA;

/** Real-cache tests with isolated world cycles, simulated connections and no account file IO. */
public final class InstancePartyRegression {
    static int checks, rejectAfter=-1;
    static final Location EXIT=Location.locate(3200,3200,0);
    static final List<Tick> tasks=new ArrayList<Tick>();
    static final List<Player> players=new ArrayList<Player>();
    static final Map<Player,AtomicBoolean> connections=new IdentityHashMap<Player,AtomicBoolean>();
    static final Map<Player,List<Message>> messages=new IdentityHashMap<Player,List<Message>>();
    static final Map<Player,FaultRegion> regions=new IdentityHashMap<Player,FaultRegion>();
    static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    static void rejects(Runnable action,String text){try{action.run();}catch(IllegalStateException|IllegalArgumentException expected){checks++;return;}throw new AssertionError(text);}
    static class FaultRegion extends RegionData {
        boolean rejectAll; Location rejectAt;
        FaultRegion(Player p){super(p);}
        public void teleport(int x,int y,int z){if(rejectAll || (rejectAt!=null && rejectAt.equals(Location.locate(x,y,z))))return;super.teleport(x,y,z);}
    }
    static Player player(String name)throws Exception{
        AtomicBoolean connected=new AtomicBoolean(true);List<Message> sent=new ArrayList<Message>();
        Channel c=(Channel)Proxy.newProxyInstance(Channel.class.getClassLoader(),new Class[]{Channel.class},(proxy,m,args)->{
            if(m.getName().equals("isConnected")||m.getName().equals("isOpen"))return connected.get();
            if(m.getName().equals("write")&&args[0] instanceof Message)sent.add((Message)args[0]);
            if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==int.class)return 0;return null;
        });
        Player p=new Player(new GameSession(c),new PlayerDefinition(name,"unused"));
        p.getDefinition().setRights(2);p.setHasReceivedStarter(true);p.setLocation(EXIT);p.lastLocation=EXIT;p.setOnline(true);
        FaultRegion r=new FaultRegion(p);Field f=Player.class.getDeclaredField("region");f.setAccessible(true);f.set(p,r);
        World.getWorld().getPlayers().add(p);players.add(p);connections.put(p,connected);messages.put(p,sent);regions.put(p,r);return p;
    }
    static void pulse(int cycles){for(int c=0;c<cycles;c++)for(Tick t:new ArrayList<Tick>(tasks))if(!t.run())tasks.remove(t);}
    static void map(GameInstance i){i.copyMap(360,648,0,0,8,8,new int[]{0},new int[]{0});}
    static Location spawn(GameInstance i){return i.location(44,19,0);}
    static GameInstance party(InstanceManager m,Player leader,int capacity,InstanceParty.LeaderDeparture policy,java.util.function.Consumer<Player> departed){
        return m.create(16,16,capacity,EXIT,i->{map(i);i.createParty(leader,policy);i.setDepartureHandler(departed);});
    }
    static void join(GameInstance i,Player p){i.getParty().join(p,spawn(i),EXIT);check(i.isMember(p),"party admission");}
    static void closed(GameInstance i){check(i.getState()==GameInstance.State.CLOSED,"closed");check(i.getTaskCount()==0&&i.getMemberCount()==0,"members/timers gone");}
    static void kill(Player p){p.getSkills().setHitPoints(0);p.getSkills().sendDead();}

    static void parties(InstanceManager m)throws Exception{
        Player p=player("leader"),q=player("memberq"),r=player("memberr"),s=player("members");
        GameInstance i=party(m,p,3,InstanceParty.LeaderDeparture.PROMOTE_OLDEST,x->{});InstanceParty a=i.getParty();
        rejects(()->i.enter(p,spawn(i)),"direct API cannot bypass party authorization");
        rejects(()->a.join(q,spawn(i),EXIT),"leader must enter first");join(i,p);
        rejects(()->a.join(q,spawn(i),EXIT),"uninvited join");
        rejects(()->a.invite(q,r,5),"nonleader invite");
        rejects(()->a.invite(p,q,0),"invalid invitation lifetime");
        rejects(()->a.invite(p,q,10001),"unbounded invitation lifetime");
        a.invite(p,q,2);check(a.isInvited(q)&&i.getTaskCount()==1,"invitation expiry task owned");
        pulse(2);check(!a.isInvited(q)&&i.getTaskCount()==0,"invitation expires");
        rejects(()->a.join(q,spawn(i),EXIT),"expired invitation rejected");
        a.invite(p,q,10);a.revoke(p,q);check(!a.isInvited(q),"revoked invitation");
        a.invite(p,q,2);a.invite(p,q,8);pulse(2);check(a.isInvited(q),"old expiry cannot revoke renewed invite");
        Player duplicate=player("MEMBERQ");rejects(()->a.join(duplicate,spawn(i),EXIT),"invitation does not authorize another login identity");
        connections.get(p).set(false);rejects(()->a.join(q,spawn(i),EXIT),"disconnected leader cannot authorize join before maintenance");connections.get(p).set(true);
        regions.get(q).rejectAll=true;rejects(()->a.join(q,spawn(i),EXIT),"failed entry rolls back");
        check(!i.isMember(q)&&a.isInvited(q)&&i.isActive(),"failed entry retains usable invitation and party");
        regions.get(q).rejectAll=false;join(i,q);check(!a.isInvited(q),"successful join consumes invitation");
        a.invite(p,r,10);a.invite(p,s,10);
        CompletableFuture<Void> first=m.submit(()->{a.join(r,spawn(i),EXIT);return null;});
        CompletableFuture<Void> second=m.submit(()->{a.join(s,spawn(i),EXIT);return null;});m.drainRequests();
        check(!first.isCompletedExceptionally()&&second.isCompletedExceptionally()&&i.getMemberCount()==3,"queued admissions respect capacity");
        check(a.isInvited(s),"capacity rejection preserves invitation");
        rejects(()->a.transferLeadership(q,r),"nonleader cannot transfer");
        rejects(()->a.kick(q,p),"nonleader cannot kick");
        rejects(()->a.kick(p,p),"leader cannot kick self");
        a.kick(p,r);check(!i.isMember(r)&&i.isActive(),"kick evacuates only target");
        a.transferLeadership(p,q);check(a.getLeader()==q&&a.getInvitationCount()==0,"leadership transfer clears old invites");
        rejects(()->a.invite(p,r,10),"old leader loses authority");
        a.invite(q,r,10);join(i,r);a.invite(q,s,10);
        connections.get(q).set(false);m.maintain();
        check(a.getLeader()==p&&i.isMember(p)&&i.isMember(r)&&!i.isMember(q),"disconnect promotes oldest connected member");
        check(a.getInvitationCount()==0,"departure invalidates leader invitations");
        p.teleport(EXIT,false);check(a.getLeader()==r&&i.isActive(),"ordinary-world teleport promotes remaining member");
        a.leave(r);closed(i);check(a.getLeader()==null&&a.getInvitationCount()==0,"last departure clears party");
        rejects(()->a.join(s,spawnDummy(),EXIT),"closed party admission rejected");
        connections.get(q).set(true);

        final Map<Player,Integer> callbacks=new IdentityHashMap<Player,Integer>();
        GameInstance close=party(m,p,3,InstanceParty.LeaderDeparture.CLOSE_SESSION,x->callbacks.put(x,callbacks.getOrDefault(x,0)+1));
        join(close,p);close.getParty().invite(p,q,10);join(close,q);close.getParty().invite(p,s,10);
        close.leave(p);closed(close);
        check(callbacks.get(p)==1&&callbacks.get(q)==1,"recursive party close delivers each content departure once");
        check(q.getLocation().equals(EXIT)&&close.getParty().getInvitationCount()==0,"leader close evacuates others and clears invitations");
        close.close();check(callbacks.get(p)==1&&callbacks.get(q)==1,"repeated close cannot replay callbacks");

        GameInstance retry=party(m,p,2,InstanceParty.LeaderDeparture.PROMOTE_OLDEST,x->{});
        join(retry,p);retry.getParty().invite(p,q,20);join(retry,q);
        regions.get(q).rejectAll=true;
        check(!retry.close()&&retry.getState()==GameInstance.State.CLOSING&&retry.isMember(q),"failed evacuation stays quarantined");
        check(retry.getParty().getLeader()==null,"closing party has no admission authority");
        rejects(()->retry.getParty().invite(q,s,10),"no invitations during close retry");
        regions.get(q).rejectAll=false;check(retry.close(),"cleanup retry succeeds");closed(retry);

        GameInstance fail=party(m,p,2,InstanceParty.LeaderDeparture.PROMOTE_OLDEST,x->{});join(fail,p);
        rejectAfter=0;rejects(()->fail.getParty().invite(p,q,3),"invitation scheduler failure");rejectAfter=-1;
        check(fail.getParty().getInvitationCount()==0&&fail.getTaskCount()==0&&fail.isActive(),"invitation failure rolls back without closing members");fail.close();
    }
    static Location spawnDummy(){return EXIT;}
    @SuppressWarnings("unchecked") static void expansion(InstanceManager m)throws Exception{
        Player p=player("builder"),q=player("viewer");
        GameInstance i=m.create(16,16,2,EXIT,InstancePartyRegression::map);i.enter(p,spawn(i));i.enter(q,spawn(i));
        GameInstance other=m.create(16,16,1,EXIT,InstancePartyRegression::map);
        GameObject original=i.spawnObject(354,i.location(42,19,0),10,0);NPC npc=i.spawnNpc(new NPC(1),i.location(44,13,0));
        GroundItem drop=new GroundItem(p,new Item(995,1),i.location(45,19,0),true,false,GroundItemManager.groundItemIndex++);i.spawnDrop(drop);
        Location resident=p.getLocation();p.updateMap();q.updateMap();messages.get(p).clear();messages.get(q).clear();
        long old=RegionBuilder.sceneRevision(p.getLocation(),p.getViewportDepth());
        rejects(()->i.buildRoom(360,648,0,0,1,1,new int[]{0},new int[]{0}),"active expansion cannot overwrite built empty chunk");
        rejects(()->i.buildRoom(360,648,5,2,1,1,new int[]{0},new int[]{0}),"resident/resource room cannot be replaced");
        rejects(()->i.buildRoom(360,648,15,0,2,1,new int[]{0},new int[]{0}),"footprint overflow");
        rejects(()->i.buildRoom(360,648,-1,0,1,1,new int[]{0},new int[]{0}),"negative offset");
        rejects(()->i.buildRoom(360,648,8,0,1,1,new int[]{0,1},new int[]{0,0}),"duplicate destination plane");
        rejects(()->i.buildRoom(360,648,8,0,1,1,new int[]{4},new int[]{0}),"invalid source plane");
        rejects(()->i.buildRoom(other.location(0,0,0).getX()/8,other.location(0,0,0).getY()/8,8,0,1,1,new int[]{0},new int[]{0}),"dynamic source rejected");
        check(RegionBuilder.sceneRevision(p.getLocation(),p.getViewportDepth())==old,"invalid requests publish nothing");
        Field field=LandscapeParser.class.getDeclaredField("broken");field.setAccessible(true);Map<Integer,Boolean> broken=(Map<Integer,Boolean>)field.get(null);
        int id=(45<<8)|81;Boolean prior=broken.put(id,Boolean.TRUE);
        try{rejects(()->i.buildRoom(360,648,8,0,8,8,new int[]{0},new int[]{0}),"source failure aborts expansion");}
        finally{if(prior==null)broken.remove(id);else broken.put(id,prior);}
        check(!i.contains(i.location(64,0,0))&&RegionBuilder.sceneRevision(p.getLocation(),p.getViewportDepth())==old,"source failure preserves map and scene version");
        i.buildRoom(360,648,8,0,8,8,new int[]{0},new int[]{0});
        check(i.contains(i.location(108,19,0))&&i.canOccupy(i.location(108,19,0),1),"new room walkable");
        check(i.owns(npc)&&drop.getLocation().getX()==i.location(45,19,0).getX()&&original.getLocation().getGameObjectType(10)==original,"original owned content survives expansion");
        check(p.getLocation().equals(resident)&&p.getRegion().isSceneChanged()&&q.getRegion().isSceneChanged(),"stationary viewers detect expansion without teleport");
        new PlayerUpdate(p).sendUpdate();new PlayerUpdate(q).sendUpdate();
        check(messages.get(p).stream().anyMatch(msg->msg.getOpcode()==31)&&messages.get(q).stream().anyMatch(msg->msg.getOpcode()==31),"real player update sends both rebuilds");
        check(!p.getRegion().isSceneChanged()&&!q.getRegion().isSceneChanged(),"sent scene versions recorded");
        check(!other.contains(other.location(108,19,0)),"other allocation unchanged");
        int matches=0;for(int x=0;x<64;x++)for(int y=0;y<64;y++){
            Location dest=i.location(64+x,y,0);if(Region.getClippingMask(dest.getX(),dest.getY(),0)==Region.getClippingMask(2880+x,5184+y,0))matches++;
        }check(matches==4096,"new room collision matches all source tiles");
        old=RegionBuilder.sceneRevision(p.getLocation(),p.getViewportDepth());
        // Plane 1 is free; plane 0 overlaps. Neither may be published by the rejected rectangle.
        rejects(()->i.buildRoom(360,648,8,0,1,1,new int[]{1,0},new int[]{1,0}),"multi-plane overlap rejects whole request");
        check(!i.contains(i.location(64,0,1))&&RegionBuilder.sceneRevision(p.getLocation(),p.getViewportDepth())==old,"multi-plane rejection is atomic");
        rejects(()->i.buildRoom(360,648,5,2,1,1,new int[]{1},new int[]{1}),"resident protection spans planes");
        i.buildRoom(360,648,8,0,1,1,new int[]{1},new int[]{1});check(i.contains(i.location(64,0,1)),"unused plane can expand without replacing existing plane");
        Location released=i.location(108,19,0);i.close();other.close();closed(i);
        GameInstance reuse=m.create(16,16,1,EXIT,InstancePartyRegression::map);
        check(!reuse.contains(reuse.location(108,19,0)),"expanded chunks removed on reuse");
        rejects(()->i.buildRoom(360,648,8,0,1,1,new int[]{0},new int[]{0}),"closed session cannot expand reused coordinates");reuse.close();
    }
    static GameInstance respawning(InstanceManager m,java.util.function.Function<GameInstance,Location> target,java.util.function.Consumer<Player> callback){
        return m.create(16,16,2,EXIT,i->{map(i);i.setRespawnPolicy(p->target.apply(i),callback);});
    }
    static void respawns(InstanceManager m)throws Exception{
        Player p=player("respawnp"),q=player("respawnq");final int[] callbacks={0};
        int initial=m.getInstances().size();
        rejects(()->m.create(8,8,1,EXIT,i->{map(i);i.setDeathPolicy(GameInstance.DeathPolicy.RESPAWN_INSIDE);}),"respawn requires configured location");
        check(m.getInstances().size()==initial,"missing policy rolls creation back");
        GameInstance i=respawning(m,InstancePartyRegression::spawn,x->callbacks[0]++);i.enter(p,i.location(44,13,0));i.enter(q,spawn(i));
        rejects(()->i.setRespawnPolicy(x->spawn(i),x->{}),"active death policy immutable");
        p.getInventory().addItem(995,17);long revision=p.getInstanceRevision();
        Damage before=Damage.getDamage(null,p,CombatType.MELEE,10);kill(p);check(p.isDead()&&i.isMember(p),"death waits in same membership");
        check(p.getInstanceRevision()!=revision&&!before.isInstanceContextCurrent(null,p),"death invalidates previous life damage");
        int taskCount=i.getTaskCount();p.getSkills().sendDead();check(i.getTaskCount()==taskCount,"duplicate death does not schedule again");
        check(InstanceAccess.savedHitPoints(p)>0&&InstanceAccess.saveLocation(p).equals(EXIT),"pending internal death saves safe HP and ordinary exit");
        Damage during=Damage.getDamage(null,p,CombatType.MELEE,10);pulse(4);check(p.isDead()&&callbacks[0]==0,"five-cycle recovery interval");pulse(1);
        check(!p.isDead()&&i.isMember(p)&&i.isMember(q)&&p.getLocation().equals(spawn(i)),"internal respawn retains party and moves to checkpoint");
        check(callbacks[0]==1&&p.getInventory().contains(995,17),"respawn callback once and inventory retained");
        check(!during.isInstanceContextCurrent(null,p),"dead-period damage cannot hit next life");
        check(InstanceAccess.saveLocation(p).equals(EXIT),"live internal respawn still saves ordinary return");
        kill(p);List<Tick> stale=new ArrayList<Tick>(tasks);pulse(5);check(callbacks[0]==2,"second death counts once");
        for(Tick t:stale)t.execute();check(callbacks[0]==2&&i.isMember(p),"completed wrappers cannot replay recovery");
        kill(p);connections.get(p).set(false);m.maintain();pulse(6);
        check(!i.isMember(p)&&i.isMember(q)&&i.isActive()&&callbacks[0]==2&&!p.isDead(),"disconnect during death evacuates without respawn or callback");
        connections.get(p).set(true);i.close();closed(i);
        GameInstance closePending=respawning(m,InstancePartyRegression::spawn,x->callbacks[0]++);closePending.enter(p,spawn(closePending));kill(p);closePending.close();pulse(8);
        check(callbacks[0]==2&&!p.isDead()&&p.getLocation().equals(EXIT),"explicit close resolves pending death safely");
        GameInstance blocked=respawning(m,x->x.location(39,12,0),x->callbacks[0]++);blocked.enter(p,spawn(blocked));blocked.enter(q,spawn(blocked));kill(p);pulse(5);closed(blocked);
        check(callbacks[0]==2&&p.getLocation().equals(EXIT)&&q.getLocation().equals(EXIT),"blocked respawn fails closed and evacuates party");
        GameInstance foreign=respawning(m,x->EXIT,x->callbacks[0]++);foreign.enter(p,spawn(foreign));kill(p);pulse(5);closed(foreign);
        GameInstance failedMove=respawning(m,InstancePartyRegression::spawn,x->callbacks[0]++);failedMove.enter(p,failedMove.location(44,13,0));regions.get(p).rejectAt=spawn(failedMove);kill(p);pulse(5);regions.get(p).rejectAt=null;closed(failedMove);
        check(callbacks[0]==2&&!p.isDead(),"failed respawn teleport restores at exit without callback");
        GameInstance callbackFailure=respawning(m,InstancePartyRegression::spawn,x->{callbacks[0]++;throw new IllegalStateException("Injected respawn callback failure");});callbackFailure.enter(p,spawn(callbackFailure));kill(p);pulse(5);closed(callbackFailure);callbackFailure.close();
        check(callbacks[0]==3,"failing post-respawn callback cannot replay");
        GameInstance schedule=respawning(m,InstancePartyRegression::spawn,x->callbacks[0]++);schedule.enter(p,spawn(schedule));rejectAfter=1;kill(p);rejectAfter=-1;pulse(6);closed(schedule);
        check(!p.isDead()&&callbacks[0]==3,"partial death scheduling failure cancels work and restores safely");
    }
    static void example(InstanceManager m)throws Exception{
        Player p=player("exampleleader"),q=player("exampleguest");
        PartyInstanceExample e=PartyInstanceExample.start(m,p,InstanceParty.LeaderDeparture.PROMOTE_OLDEST);check(e!=null&&PartyInstanceExample.get(p)==e,"admin harness starts");
        rejects(()->e.join(q),"harness rejects uninvited join");e.getParty().invite(p,q,10);e.join(q);
        check(PartyInstanceExample.get(q)==e&&e.getInstance().getMemberCount()==2,"harness joins shared session");
        rejects(()->e.expand(q),"harness only leader expands");e.expand(p);e.expand(p);check(e.getInstance().contains(e.getInstance().location(108,19,0)),"harness expands idempotently");
        p.teleport(e.getInstance().location(108,19,0),false);kill(p);pulse(5);
        check(e.getDeaths(p)==1&&e.getParty().getLeader()==p&&e.getInstance().isMember(p),"internal death retains leader and records count");
        e.getParty().leave(p);check(PartyInstanceExample.get(p)==null&&e.getParty().getLeader()==q,"harness departure clears reference and promotes");
        kill(q);check(m.shutdown(),"shutdown with party death completes");pulse(8);
        check(PartyInstanceExample.get(q)==null&&!q.isDead()&&q.getLocation().equals(EXIT),"shutdown clears harness and restores pending death");closed(e.getInstance());
    }
    public static void main(String[] args)throws Exception{
        Cache.init();MapXTEA.loadPackedFile();ItemDefinition.init();NPCDefinition.init();GroundItemManager.load();
        Field areas=World.class.getDeclaredField("areaManager");areas.setAccessible(true);areas.set(World.getWorld(),new AreaManager());
        InstanceManager m=new InstanceManager(t->{if(rejectAfter==0){rejectAfter=-1;throw new IllegalStateException("Injected scheduling failure");}if(rejectAfter>0)rejectAfter--;tasks.add(t);});m.beginCycle();
        try{parties(m);expansion(m);respawns(m);example(m);pulse(12);check(m.getInstances().isEmpty()&&RegionBuilder.getAllocationCount()==0,"all allocations released");check(tasks.isEmpty(),"no scheduled work retained");}
        finally{rejectAfter=-1;for(FaultRegion r:regions.values()){r.rejectAll=false;r.rejectAt=null;}m.closeAll();for(Player p:players)World.getWorld().getPlayers().remove(p);m.endCycle();}
        System.out.println("InstancePartyRegression: "+checks+" checks passed");
    }
}


