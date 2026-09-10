import java.lang.reflect.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.buffer.*;
import org.dementhium.cache.Cache;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.activity.impl.duel.*;
import org.dementhium.content.activity.impl.duel.DuelConfigurations.Rules;
import org.dementhium.event.impl.interfaces.DuelArenaListener;
import org.dementhium.io.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.instance.InstanceManager;
import org.dementhium.model.misc.*;
import org.dementhium.model.player.*;
import org.dementhium.net.*;
import org.dementhium.net.message.Message;
import org.dementhium.tickable.Tick;
import org.dementhium.util.*;

public class DuelRegression {
    static int checks,sequence;
    static Path root;
    static final Map<Player,List<Message>> messages=new IdentityHashMap<Player,List<Message>>();
    static void check(boolean ok,String msg){checks++;if(!ok)throw new AssertionError(msg);}
    static void field(Object o,Class<?> cls,String name,Object value)throws Exception{Field f=cls.getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    static void advance(int n)throws Exception{PvpRegression.ticks(World.getTicks()+n);}
    static Player p() {
        List<Message> sent=new ArrayList<Message>();
        Channel channel=(Channel)Proxy.newProxyInstance(Channel.class.getClassLoader(),new Class[]{Channel.class},(proxy,m,args)->{
            if(m.getName().equals("isConnected")||m.getName().equals("isOpen"))return true;
            if(m.getName().equals("write")&&args[0] instanceof Message)sent.add((Message)args[0]);
            if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==int.class)return 0;return null;
        });
        Player p=new Player(new GameSession(channel),new PlayerDefinition("duel"+(++sequence),"unused"));
        p.setOnline(true);p.setHasReceivedStarter(true);p.setLocation(Location.locate(3366,3266,0));
        p.getSkills().setMaximumLifePoints(990);p.getSkills().setHitPoints(990);
        World.getWorld().getPlayers().add(p);messages.put(p,sent);return p;
    }
    static DuelActivity duel(Player a,Player b){DuelActivity d=new DuelActivity(a,b);d.initializeActivity();return d;}
    static long count(Container c,int id){long n=0;for(Item i:c.toArray())if(i!=null&&i.getId()==id)n+=i.getAmount();return n;}
    static long wealth(Player... ps){long n=0;for(Player p:ps){n+=count(p.getInventory().getContainer(),995)+count(DuelRecovery.savedItems(p),995);}return n;}
    static void ready(DuelActivity d)throws Exception{advance(4);check(d.accept(d.getPlayer()),"first accept");check(d.accept(d.getOtherPlayer()),"second accept");advance(4);d.acceptSecond(d.getPlayer());d.acceptSecond(d.getOtherPlayer());check(d.getCurrentState()==DuelActivity.State.FIGHTING,"final accepts reach fighting");}
    static void pulse()throws Exception{
        Field f=World.class.getDeclaredField("ticksToAdd");f.setAccessible(true);
        @SuppressWarnings("unchecked") List<Tick> queue=(List<Tick>)f.get(World.getWorld());
        List<Tick> ticks=new ArrayList<Tick>(queue);queue.clear();
        for(int i=0;i<12;i++)for(Tick t:ticks)if(t.isRunning())t.run();
    }
    static ChannelBuffer bytes(Player p){ChannelBuffer b=ChannelBuffers.dynamicBuffer();p.save(b);return b;}
    static Player reload(Player original)throws Exception{Player p=p();p.load(ByteBuffer.wrap(Files.readAllBytes(root.resolve(original.getUsername()+".bin"))));return p;}
    static void transfers()throws Exception{
        Player a=p(),b=p();a.getInventory().getContainer().set(0,new Item(995,1000));DuelActivity d=duel(a,b);
        check(d.stakeOf(a).stake(995,0,700),"stake coins");check(wealth(a,b)==1000,"coins conserved");
        Player saved=reload(a);check(count(saved.getInventory().getContainer(),995)==300&&count(DuelRecovery.pending(saved),995)==700,"saved offered coins recover outside escrow");
        check(saved.getLocation().equals(Location.locate(3366,3266,0)),"duel save uses safe return");
        check(!d.stakeOf(a).stake(995,0,-1)&&!d.stakeOf(a).stake(995,28,1),"negative and invalid slot rejected");
        advance(4);d.accept(a);check(d.hasAccepted(a),"accepted current revision");
        check(d.stakeOf(a).remove(995,300),"remove stake");check(!d.hasAccepted(a)&&!d.accept(a),"change clears acceptance and delays next accept");
        check(wealth(a,b)==1000,"removal conserved");
        for(int n=1;n<28;n++)a.getInventory().getContainer().set(n,new Item(4151));
        Item charged=new Item(4710);charged.setHealth(123);a.getInventory().getContainer().set(1,charged);
        check(d.stakeOf(a).stake(4710,1,1),"charged item stake");
        check(d.stakeOf(a).getContainer().lookup(4710).getHealth()==123,"stake preserves charges");
        a.getInventory().getContainer().set(1,new Item(4151));
        check(!d.stakeOf(a).remove(4710,1)&&d.stakeOf(a).getContainer().lookup(4710).getHealth()==123,"full inventory cannot lose removed item");
        check(d.decline(a,false),"decline commits");check(wealth(a,b)==1000,"decline coins conserved");
        check(DuelRecovery.pending(a).lookup(4710).getHealth()==123,"overflow charged item remains durable");
        long before=wealth(a,b);d.decline(a,false);d.endSession();d.forceEnd(a);check(wealth(a,b)==before,"repeat terminal callbacks cannot duplicate");
        a.getInventory().getContainer().set(1,null);check(DuelRecovery.claim(a),"claim overflow");
        check(a.getInventory().getContainer().lookup(4710).getHealth()==123&&DuelRecovery.pending(a).size()==0,"claim preserves metadata and consumes pending");
        saved=reload(a);check(saved.getInventory().getContainer().lookup(4710).getHealth()==123,"charge persisted after claim");
    }
    static void consent()throws Exception{
        Player a=p(),b=p();DuelActivity d=duel(a,b);DuelArenaListener l=new DuelArenaListener();
        advance(4);d.accept(a);
        check(l.interfaceOption(b,631,47,0,0,6),"toggle special restriction");check(!d.hasAccepted(a)&&d.getDuelConfigurations().getRule(Rules.SPECIAL_ATTACKS),"toggle resets first accept");
        advance(4);d.accept(a);l.interfaceOption(b,631,47,0,0,6);check(!d.hasAccepted(a)&&!d.getDuelConfigurations().getRule(Rules.SPECIAL_ATTACKS),"uncheck resets first accept");
        a.getInventory().getContainer().set(0,new Item(995,50));l.interfaceOption(a,628,0,0,995,46);
        d.getDuelConfigurations().swapRule(b,a,Rules.FOOD);DuelArenaListener.input(a,50,true);check(d.stakeOf(a).getContainer().size()==0,"stale X rejects changed offer revision");
        l.interfaceOption(a,628,0,0,995,46);d.decline(a,false);DuelActivity next=duel(a,b);DuelArenaListener.input(a,50,true);check(next.stakeOf(a).getContainer().size()==0,"stale X cannot cross duel identity");
        ready(next);check(!next.stakeOf(a).stake(995,0,1),"cannot alter confirmed stake");
        next.forceEnd(a);
        DuelActivity friendly=new DuelActivity(a,b,false);friendly.initializeActivity();check(!friendly.stakeOf(a).stake(995,0,50),"friendly mode cannot stake");friendly.forceEnd(a);
    }
    static void terminalStates()throws Exception{
        for(int phase=0;phase<4;phase++){
            Player a=p(),b=p();a.getInventory().getContainer().set(0,new Item(995,700));b.getInventory().getContainer().set(0,new Item(995,300));DuelActivity d=duel(a,b);
            d.stakeOf(a).stake(995,0,700);d.stakeOf(b).stake(995,0,300);
            if(phase==1){advance(4);d.accept(a);d.accept(b);}else if(phase>=2)ready(d);
            if(phase==3){check(d.commenceSession(),"commence succeeds");pulse();check(d.isCommenced(),"countdown opens combat");}
            check(d.forceEnd(b),"shutdown/abort settles phase "+phase);check(wealth(a,b)==1000,"abort conserved phase "+phase);
            check(a.getLocation().equals(b.getLocation())&&a.getActivity()==Mob.DEFAULT_ACTIVITY&&b.getActivity()==Mob.DEFAULT_ACTIVITY,"both evacuated phase "+phase);
            pulse();check(!d.isCommenced()&&wealth(a,b)==1000,"cancelled countdown cannot reopen phase "+phase);
        }
        Player a=p(),b=p();a.getInventory().getContainer().set(0,new Item(995,100));b.getInventory().getContainer().set(0,new Item(995,200));DuelActivity d=duel(a,b);
        d.stakeOf(a).stake(995,0,100);d.stakeOf(b).stake(995,0,200);ready(d);d.commenceSession();pulse();
        Damage launched=Damage.getDamage(a,b,CombatType.MELEE,100);
        check(d.depart(a),"disconnect synchronous settlement");check(count(b.getInventory().getContainer(),995)==300&&count(a.getInventory().getContainer(),995)==0,"opponent receives both stakes");
        int hp=b.getHitPoints();b.getDamageManager().damage(a,launched,DamageManager.DamageType.MELEE);check(b.getHitPoints()==hp,"late projectile rejected after duel");
        d.onDeath(a);d.onDeath(b);d.depart(b);check(wealth(a,b)==300,"late death/disconnect cannot repay");
        Player saved=reload(b);check(count(saved.getInventory().getContainer(),995)==300&&DuelRecovery.pending(saved).size()==0,"winner durable before return");
    }
    static void rules()throws Exception{
        Player a=p(),b=p();DuelActivity d=duel(a,b);
        check(d.getDuelConfigurations().swapRule(a,b,Rules.FORFEIT),"no forfeit selectable");ready(d);check(!d.forfeit(a)&&!d.isSettled(),"no forfeit enforced at confirmation");check(d.depart(a),"disconnect still resolves no-forfeit duel");
        a=p();b=p();a.getEquipment().set(3,new Item(11694));d=duel(a,b);
        d.getDuelConfigurations().swapRule(a,b,Rules.SHIELD);ready(d);d.commenceSession();check(a.getEquipment().get(3)==null&&a.getInventory().contains(11694),"existing two-handed weapon removed under no shield");d.forceEnd(a);
        a=p();b=p();d=duel(a,b);d.getDuelConfigurations().swapRule(a,b,Rules.FUN_WEAPONS);
        check(!d.getDuelConfigurations().canAccept(a),"unarmed cannot accept fun weapons");a.getEquipment().set(3,new Item(4566));b.getEquipment().set(3,new Item(2460));
        check(d.getDuelConfigurations().canAccept(a)&&d.getDuelConfigurations().canAccept(b),"supported fun weapons accepted");
        check(!d.getDuelConfigurations().weaponAllowed(a,4151),"ordinary weapon rejected");
        check(!d.getDuelConfigurations().swapRule(a,b,Rules.MELEE),"fun weapons/no-melee contradiction rejected");d.forceEnd(a);
        DuelConfigurations config=new DuelConfigurations();config.setRule(Rules.SHIELD,true);
        a=p();a.getEquipment().set(3,new Item(11694));for(int n=0;n<28;n++)a.getInventory().getContainer().set(n,new Item(4151));
        check(!config.canAccept(a),"full inventory prevents equipment removal");
    }
    static void journal()throws Exception{
        Path dir=Files.createDirectory(root.resolve("journal"));DuelJournal journal=new DuelJournal(dir);
        Files.createDirectory(dir.resolve("second.bin"));byte[] first={1,2,3},second={4,5,6};
        journal.commit("first",first,"second",second);
        check(Files.exists(dir.resolve("duel-commit.bin")),"committed journal survives interrupted account installation");
        check(Arrays.equals(Files.readAllBytes(dir.resolve("first.bin")),first),"first postimage installed before injected second failure");
        Files.delete(dir.resolve("second.bin"));new DuelJournal(dir).recover();
        check(Arrays.equals(Files.readAllBytes(dir.resolve("second.bin")),second)&&!Files.exists(dir.resolve("duel-commit.bin")),"new process replays both images and completes");
        journal.recover();check(Arrays.equals(Files.readAllBytes(dir.resolve("first.bin")),first),"repeated recovery idempotent");
        try{journal.commit("../escape",first,"second",second);throw new AssertionError("path escape");}catch(java.io.IOException expected){checks++;}
        Player a=p(),b=p();a.getInventory().getContainer().set(0,new Item(995,10));DuelActivity d=duel(a,b);d.stakeOf(a).stake(995,0,10);
        Path blocked=root.resolve("blocked");Files.write(blocked,new byte[]{1});field(World.getWorld(),World.class,"playerLoader",new PlayerLoader(blocked));
        check(!d.forceEnd(a)&&!d.isSettled()&&wealth(a,b)==10,"failed commit keeps escrow owned and retryable");
        field(World.getWorld(),World.class,"playerLoader",new PlayerLoader(root));check(d.endSession()&&wealth(a,b)==10,"retry conserves original refund");
    }
    static void admission()throws Exception{
        Player a=p(),b=p(),c=p();DuelActivity d=duel(a,b);check(!DuelChallenge.eligible(c,b),"busy opponent rejected");
        try{new DuelActivity(c,b);throw new AssertionError("overlap admitted");}catch(IllegalArgumentException expected){checks++;}
        d.forceEnd(a);check(DuelChallenge.eligible(a,b),"available lobby players eligible");
        DuelChallenge.select(a,b);a.setAttribute("isStaking",false);DuelChallenge.send(a);DuelChallenge.select(b,a);
        check(b.getActivity() instanceof DuelActivity&&!((DuelActivity)b.getActivity()).isStaking(),"request mode retained");((DuelActivity)b.getActivity()).forceEnd(a);
        DuelChallenge.select(a,b);DuelChallenge.send(a);advance(101);DuelChallenge.select(b,a);check(b.getActivity()==Mob.DEFAULT_ACTIVITY,"expired request not accepted");
        b.setLocation(Location.locate(3200,3200,0));check(!DuelChallenge.eligible(a,b),"departed target rejected");
    }
    static Map<Integer,String> text(Player p,int inter){
        Map<Integer,String> result=new HashMap<Integer,String>();
        for(Message message:messages.get(p))if(message.getOpcode()==33){
            ChannelBuffer buffer=message.getBuffer().duplicate();String value=BufferUtils.readRS2String(buffer);
            int hash=(buffer.readUnsignedByte())|(buffer.readUnsignedByte()<<8)|(buffer.readUnsignedByte()<<16)|(buffer.readUnsignedByte()<<24);
            if((hash>>>16)==inter)result.put(hash&65535,value);
        }return result;
    }
    static void nameVariableBeforeOpen(Player p,int inter,String expected){
        String name=null;boolean opened=false;
        for(Message m:messages.get(p)){
            ChannelBuffer b=m.getBuffer().duplicate();
            if(m.getOpcode()==88){
                String value=BufferUtils.readRS2String(b);
                int id=((b.readUnsignedByte()-128)&255)|(b.readUnsignedByte()<<8);
                if(id==274)name=value;
            }else if(m.getOpcode()==50){
                b.skipBytes(4);
                int id=((b.readUnsignedByte()-128)&255)|(b.readUnsignedByte()<<8);
                if(id==inter){check(expected.equals(name),"script name populated before opening "+inter);opened=true;}
            }
        }
        check(opened,"expected name-bearing interface opened "+inter);
    }
    static void clientNameVariable()throws Exception{
        Player a=p(),b=p();b.setDisplayName("Arena Rival");
        String accountName="Duel"+a.getUsername().substring(4);
        DuelActivity d=duel(a,b);
        nameVariableBeforeOpen(a,631,"Arena Rival");
        nameVariableBeforeOpen(b,631,accountName);
        check("Arena Rival".equals(text(a,631).get(23)),"custom name visible text");
        check(accountName.equals(text(b,631).get(23)),"account fallback visible text");
        ready(d);
        // Clear the earlier name packet: results must initialize their own script input.
        messages.get(a).clear();d.depart(b);
        nameVariableBeforeOpen(a,634,"Arena Rival");
        check("Arena Rival".equals(text(a,634).get(33)),"result opponent name");
        Player c=p();c.setDisplayName("Next Rival");messages.get(a).clear();
        d=duel(a,c);nameVariableBeforeOpen(a,631,"Next Rival");d.forceEnd(a);
    }
    static void uiAndEveryRule()throws Exception{
        DuelArenaListener listener=new DuelArenaListener();
        int[] buttons={28,30,32,34,36,38,40,42,44,46,48,50,54,55,56,58,59,60,61,64,63,62,57};
        for(int rule=0;rule<buttons.length;rule++){
            Player a=p(),b=p();DuelActivity d=duel(a,b);
            check(text(a,631).get(23).equals(b.getDisplayName()),"actual first-screen name packet "+rule);
            int firstOpen=-1,name=-1;
            for(int n=0;n<messages.get(a).size();n++){Message m=messages.get(a).get(n);if(m.getOpcode()==50)firstOpen=n;if(m.getOpcode()==33&&textMessage(m).equals(b.getDisplayName())){name=n;break;}}
            check(firstOpen>=0&&name>firstOpen,"name sent after interface open "+rule);
            advance(4);d.accept(a);
            listener.interfaceOption(b,631,buttons[rule],0,0,6);
            check(d.getDuelConfigurations().getRule(Rules.values()[rule]),"button maps to rule "+rule);
            check(!d.hasAccepted(a)&&text(a,631).get(92).contains("Review"),"real acceptance label cleared "+rule);
            listener.interfaceOption(b,631,buttons[rule],0,0,6);
            check(!d.getDuelConfigurations().getRule(Rules.values()[rule]),"uncheck maps to rule "+rule);
            ready(d);Map<Integer,String> finalText=text(a,626);
            check(finalText.get(20).contains(b.getDisplayName())&&finalText.get(25).equals("Absolutely nothing!"),"final identity/empty stake "+rule);
            check(finalText.get(45).toLowerCase().contains("accepted"),"second acceptance uses correct component "+rule);
            d.forceEnd(a);
        }
        Player a=p(),b=p();DuelActivity d=duel(a,b);
        advance(4);d.accept(a);d.getDuelConfigurations().swapRule(a,b,Rules.MOVEMENT);long rev=d.getRevision();d.getDuelConfigurations().swapRule(b,a,Rules.OBSTACLES);
        check(d.getRevision()==rev+1&&!d.getDuelConfigurations().getRule(Rules.MOVEMENT),"dependent change has one shared revision");
        check(text(a,631).get(43).contains("ff4040")&&text(a,631).get(45).contains("ff4040"),"both changed options highlighted");
        a.getInventory().getContainer().set(0,new Item(995,123456));d.stakeOf(a).stake(995,0,123456);
        advance(4);d.accept(a);d.accept(b);check(text(a,626).get(46).contains("123,456")&&text(b,626).get(47).contains("123,456"),"both final stake lists exact");
        check(text(a,626).get(25).isEmpty()&&text(b,626).get(26).isEmpty(),"nonempty stake placeholder hidden");
        d.acceptSecond(a);check(!d.hasAccepted(a),"new second screen cannot accept queued first-screen click");d.forceEnd(a);
    }
    static String textMessage(Message message){return BufferUtils.readRS2String(message.getBuffer().duplicate());}
    static void actualDeath()throws Exception{
        Player a=p(),b=p();a.getInventory().getContainer().set(0,new Item(995,100));b.getInventory().getContainer().set(0,new Item(995,200));
        DuelActivity d=duel(a,b);d.stakeOf(a).stake(995,0,100);d.stakeOf(b).stake(995,0,200);ready(d);d.commenceSession();pulse();
        b.getSkills().setHitPoints(10);Damage lethal=Damage.getDamage(a,b,CombatType.MELEE,100);
        b.getDamageManager().damage(a,lethal,DamageManager.DamageType.MELEE);
        check(b.getHitPoints()==0&&lethal.getHit()==10,"lethal hit accounts before restoration");
        check(d.getCurrentState()==DuelActivity.State.SETTLING&&!d.isCommenced(),"first lethal impact locks result");
        d.depart(a);long coins=wealth(a,b);pulse();pulse();
        check(coins==300&&wealth(a,b)==300&&count(a.getInventory().getContainer(),995)==300,"winner disconnect during death preserves locked winner");
        check(a.getActivity()==Mob.DEFAULT_ACTIVITY&&b.getActivity()==Mob.DEFAULT_ACTIVITY&&b.getHitPoints()>0,"owned death timer cannot cause ordinary death after return");
        d=duel(a,b);ready(d);d.commenceSession();pulse();b.getSkills().hit(9999);pulse();pulse();check(d.isSettled(),"ordinary lethal duel resolves via owned death task");
    }
    static void boundariesAndRecovery()throws Exception{
        Player a=p(),b=p();a.getInventory().getContainer().set(0,new Item(995,Integer.MAX_VALUE));b.getInventory().getContainer().set(0,new Item(995,Integer.MAX_VALUE));
        DuelActivity d=duel(a,b);check(d.stakeOf(a).stake(995,0,Integer.MAX_VALUE)&&d.stakeOf(b).stake(995,0,Integer.MAX_VALUE),"maximum stakes accepted without overflow");
        ready(d);d.depart(a);check(wealth(a,b)==2L*Integer.MAX_VALUE,"two max stacks conserved across payout");
        check(count(b.getInventory().getContainer(),995)==Integer.MAX_VALUE&&count(DuelRecovery.pending(b),995)==Integer.MAX_VALUE,"overflow max stack retained safely");
        check(count(DuelRecovery.pending(reload(b)),995)==Integer.MAX_VALUE,"overflow survives fresh load");
        a=p();b=p();d=duel(a,b);
        for(int n=0;n<10;n++)a.getInventory().getContainer().set(n,new Item(4151));
        check(!d.stakeOf(a).stake(4151,0,10)&&count(a.getInventory().getContainer(),4151)==10,"ten-slot offer fails atomically");
        check(d.stakeOf(a).stake(4151,0,9)&&d.stakeOf(a).getContainer().size()==9,"nine physical slots supported");
        check(!d.stakeOf(a).stake(4151,9,1)&&count(a.getInventory().getContainer(),4151)==1,"full stake cannot debit inventory");
        d.forceEnd(a);
        for(int n=0;n<150;n++){
            a=p();b=p();a.getInventory().getContainer().set(0,new Item(995,10000));d=duel(a,b);
            Random random=new Random(n);for(int k=0;k<8;k++){
                if(random.nextBoolean())d.stakeOf(a).stake(995,0,random.nextInt(12000));else d.stakeOf(a).remove(995,random.nextInt(12000));
                check(wealth(a,b)==10000,"random transfer sequence conserves coins "+n+"/"+k);
            }
            if(n%2==0){ready(d);d.depart(b);}else d.decline(a,false);
            check(wealth(a,b)==10000,"random terminal conserves coins "+n);d.forceEnd(a);check(wealth(a,b)==10000,"repeat randomized terminal "+n);
        }
    }
    static void saveOrdering()throws Exception{
        Player a=p();a.getInventory().getContainer().set(0,new Item(995,10));PlayerLoader loader=World.getWorld().getPlayerLoader();
        check(loader.save(a),"fixture account saved");
        Player candidate=new Player(a.getConnection(),new PlayerDefinition(a.getUsername(),"unused"));
        check(loader.load(candidate)&&loader.isCurrentLoad(candidate),"loaded fingerprint matches account");
        a.getInventory().getContainer().set(0,new Item(995,20));loader.save(a);
        check(!loader.isCurrentLoad(candidate),"stale pre-login image cannot enter after another save");
        check(!loader.save(candidate),"superseded player object cannot overwrite current session");
        a.setAttribute("saveSessionClosed",true);a.getInventory().getContainer().set(0,new Item(995,30));loader.save(a);
        check(count(reload(a).getInventory().getContainer(),995)==20,"closed session background save is ignored");
        a.removeAttribute("saveSessionClosed");
        java.util.concurrent.CountDownLatch started=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean completed=new java.util.concurrent.atomic.AtomicBoolean();
        Thread saver=new Thread(()->{started.countDown();loader.save(a);completed.set(true);});
        synchronized(World.getWorld()){
            saver.start();started.await();Thread.sleep(40);
            check(!completed.get(),"autosave cannot snapshot inside a world-cycle mutation");
            a.getInventory().getContainer().set(0,new Item(995,40));
        }
        saver.join(5000);check(completed.get()&&count(reload(a).getInventory().getContainer(),995)==40,"autosave sees complete mutation after world lock released");
    }
    static void mapAndNotes()throws Exception{
        Player a=p(),b=p();DuelActivity d=duel(a,b);
        a.getInventory().getContainer().set(0,new Item(4152,123));
        check(d.stakeOf(a).stake(4152,0,123)&&d.stakeOf(a).getContainer().size()==1,"noted stack uses one stake slot");
        check(d.stakeOf(a).remove(4152,23)&&count(a.getInventory().getContainer(),4152)==23,"noted stack removal exact");
        d.getDuelConfigurations().swapRule(a,b,Rules.MOVEMENT);ready(d);check(d.commenceSession(),"no-movement start");
        check(Math.abs(a.getLocation().getX()-b.getLocation().getX())+Math.abs(a.getLocation().getY()-b.getLocation().getY())==1,"no movement starts adjacent");
        pulse();check(!d.walkingUpdate(a),"no movement blocks walking after countdown");d.forceEnd(a);
        a=p();b=p();d=duel(a,b);ready(d);
        org.dementhium.content.areas.Area area=DuelConfigurations.TeleportLocations.NORMAL_ARENA.getArea();
        int[][] masks=new int[area.nwX-area.swX][area.nwY-area.swY];
        for(int x=area.swX;x<area.nwX;x++)for(int y=area.swY;y<area.nwY;y++){
            masks[x-area.swX][y-area.swY]=org.dementhium.model.map.Region.getClippingMask(x,y,0);
            org.dementhium.model.map.Region.addClipping(x,y,0,0x200000);
        }
        check(!d.commenceSession()&&d.isSettled(),"unavailable arena refunds instead of starting");
        for(int x=area.swX;x<area.nwX;x++)for(int y=area.swY;y<area.nwY;y++){
            org.dementhium.model.map.Region.removeClipping(x,y,0,-1);
            org.dementhium.model.map.Region.addClipping(x,y,0,masks[x-area.swX][y-area.swY]);
        }
        for(short id:DuelConfigurations.FUN_WEAPONS){String name=ItemDefinition.forId(id).getName().toLowerCase();check(name.contains("flower")||name.equals("rubber chicken")||name.equals("mouse toy"),"fun whitelist matches packed definition "+id);}
    }
    static long chatCount(Player p){long count=0;for(Message m:messages.get(p))if(m.getOpcode()==53)count++;return count;}
    static void warnings()throws Exception{
        Player a=p(),b=p();DuelActivity d=duel(a,b);long ac=chatCount(a),bc=chatCount(b);
        d.getDuelConfigurations().swapRule(a,b,Rules.FOOD);
        check(chatCount(a)==ac&&chatCount(b)==bc,"ordinary setup has no warning chat spam");
        check(text(a,631).get(92).equals("Accept")&&!text(a,631).get(39).contains("ff4040"),"initial setup is neutral");
        advance(4);d.accept(a);check(text(a,631).get(92).equals("Accepted"),"accepted state outside hover help");
        d.getDuelConfigurations().swapRule(b,a,Rules.FOOD);
        check(text(a,631).get(92).contains("Review")&&text(b,631).get(92).contains("Review"),"post-consent changes visibly require review for both");
        check(!d.accept(a),"original review delay preserved");
        bc=chatCount(b);d.getDuelConfigurations().swapRule(b,a,Rules.PRAYER);
        check(chatCount(b)==bc,"further self edits do not repeat warnings");
        check(text(a,631).get(39).contains("* ")&&text(a,631).get(41).contains("* "),"unreviewed rule highlights accumulate");
        check(text(a,631).get(26).isEmpty(),"bottom line left for hover help");
        advance(4);d.accept(a);
        check(text(a,631).get(92).equals("Accepted")&&!text(a,631).get(39).contains("* ")
            &&text(b,631).get(39).contains("* "),"accept clears only that viewer's review markers");
        a.getInventory().getContainer().set(0,new Item(995,100));d.stakeOf(a).stake(995,0,100);
        check(text(a,631).get(89).contains("* ")&&text(b,631).get(90).contains("* "),"stake change marked for each viewer");
        check(!d.hasAccepted(a)&&text(a,631).get(92).contains("Review"),"stake edit also invalidates acceptance");
        d.forceEnd(a);DuelActivity fresh=duel(a,b);
        check(text(a,631).get(92).equals("Accept")&&!text(a,631).get(39).contains("* ")
            &&!text(a,631).get(89).contains("* "),"new duel resets cached review markers");fresh.forceEnd(a);
    }
    public static void main(String[] args)throws Exception{
        root=Files.createTempDirectory(Paths.get("build/duel-fixes"),"test-accounts-");
        Cache.init();MapXTEA.loadPackedFile();NPCDefinition.init();ItemDefinition.init();GroundItemManager.load();
        field(World.getWorld(),World.class,"areaManager",new AreaManager());field(World.getWorld(),World.class,"playerLoader",new PlayerLoader(root));
        InstanceManager.getSingleton().beginCycle();
        warnings();transfers();consent();terminalStates();rules();journal();admission();clientNameVariable();uiAndEveryRule();actualDeath();boundariesAndRecovery();saveOrdering();mapAndNotes();
        System.out.println("PASS: "+checks+" duel checks; isolated save/journal tests in "+root);
    }
}



