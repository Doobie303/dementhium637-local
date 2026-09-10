import java.nio.*;
import java.nio.file.*;
import java.util.*;
import org.dementhium.cache.Cache;
import org.dementhium.content.DialogueManager;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.content.minigames.gambler.*;
import org.dementhium.io.*;
import org.dementhium.model.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.instance.InstanceManager;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.net.packethandlers.ActionButtonHandler;
import org.dementhium.net.message.*;
import org.dementhium.util.*;

public class GamblerRegression extends DuelRegression {
    static NPC npc;
    static GamblerSession begin(Player p,int id,int amount)throws Exception {
        GamblerSession.open(p,npc);
        check(p.getActivity() instanceof GamblerSession,"Gambler opens through owned activity");
        GamblerSession s=(GamblerSession)p.getActivity();
        if(id==995)GamblerSession.dialogue(p,19101);
        else {GamblerSession.dialogue(p,19102);GamblerSession.dialogue(p,id==560?19105:id==565?19106:19107);}
        InputHandler.handleIntegerInput(p,amount);
        check(s.phase()==GamblerSession.Phase.REVIEW,"amount opens review");return s;
    }
    static long house(int id)throws Exception{return new GamblerJournal(root).balances(GamblerPolicy.INSTANCE.seeds).get(id);}
    static long total(Player p,int id)throws Exception {
        GamblerRecovery.Record r=GamblerRecovery.get(p);
        return house(id)+count(p.getInventory().getContainer(),id)+(r!=null&&r.item==id?r.pending:0);
    }
    static void click(Player p,int inter,int child) {
        Message m=new MessageBuilder(6).writeShort(inter).writeShort(child).writeLEShortA(65535).writeShort(65535).toMessage();
        new ActionButtonHandler().handlePacket(p,m);
    }
    static void odds() {
        int wins=0,ties=0,losses=0;long returns=0;
        for(int a=1;a<=100;a++)for(int b=1;b<=100;b++){
            int payout=GamblerRecovery.payout(100,a,b);check(payout==(a>b?200:a==b?100:0),"payout pair "+a+":"+b);
            returns+=payout;if(a>b)wins++;else if(a==b)ties++;else losses++;
        }
        check(wins==4950&&losses==4950&&ties==100&&returns==1000000,"fair exact payout distribution");
        check(!GamblerPolicy.INSTANCE.valid(995,-1)&&!GamblerPolicy.INSTANCE.valid(995,Integer.MAX_VALUE),"invalid stake limits");
        Item charge=new Item(560,100);charge.setHealth(1);
        check(!GamblerPolicy.INSTANCE.eligible(charge)&&!GamblerPolicy.INSTANCE.eligible(new Item(4151)),"charged and nonwhitelisted items rejected");
    }
    static void routes()throws Exception {
        Player a=p();a.getInventory().getContainer().set(0,new Item(995,100000));
        long initial=total(a,995);GamblerSession s=begin(a,995,10000);
        check(text(a,626).get(46).contains("10,000")&&text(a,626).get(47).contains("20,000"),"review exact stake/total");
        check(!s.confirm(),"review delay protects queued clicks");advance(4);click(a,626,53);
        check(s.phase()==GamblerSession.Phase.ROLLING,"actual button route commits");
        GamblerRecovery.Record r=GamblerRecovery.get(a);check(r!=null&&r.pending==r.returned(),"committed pending entitlement");
        check(total(a,995)==initial,"house inventory pending conserve wealth");
        check(!s.confirm(),"repeated confirm rejected");check(GamblerRecovery.claim(a)==(r.pending==0),"no early animation collection unless no reward");
        advance(3);s.updateSession();advance(3);s.updateSession();
        check(s.phase()==GamblerSession.Phase.RESULT,"owned timer reveals results");
        nameVariableBeforeOpen(a,634,"");
        check(text(a,634).get(15).equals("<col="+(r.playerRoll>r.houseRoll?"00cc00":r.playerRoll==r.houseRoll?"ffcc00":"ff0000")+">You rolled: "+r.playerRoll+" | Gambler rolled: "+r.houseRoll+"</col>"),"opponent roll on reward screen");
        check(total(a,995)==initial,"automatic collection conserved");
        click(a,634,17);check(a.getActivity()==Mob.DEFAULT_ACTIVITY,"result close releases activity");
        check(text(a,634).get(30).equals("Name:")&&text(a,634).get(29).equals("The Defeated:"),"duel labels restored");
        long before=count(a.getInventory().getContainer(),995);click(a,634,17);check(count(a.getInventory().getContainer(),995)==before,"repeat close cannot pay");
        for(int stage:new int[]{9050,9051,9052,9053,9054,9055,9056,6669,6670,6671,6672,6673,6674}) {
            check(!DialogueManager.proceedDialogue(a,stage),"legacy stage retired "+stage);
            check(count(a.getInventory().getContainer(),995)==before,"retired stage cannot mutate money");
        }
    }
    static void isolation()throws Exception {
        Player a=p(),b=p();a.getInventory().getContainer().set(0,new Item(995,100000));b.getInventory().getContainer().set(0,new Item(995,100000));
        GamblerSession first=begin(a,995,10000),second=begin(b,995,10000);advance(4);
        check(first.confirm()&&!second.confirm(),"one visible rolling round per NPC");
        long wealth=total(a,995);a.closeAll(false,true);check(a.getActivity()==Mob.DEFAULT_ACTIVITY,"other activity closes presentation");
        check(GamblerRecovery.get(a)!=null&&total(a,995)==wealth,"closing preserves committed result");
        check(second.confirm(),"NPC reservation released on cancellation");second.endSession();
        Player saved=reload(a);GamblerRecovery.Record r=GamblerRecovery.get(saved);
        check(r!=null&&r.id.equals(GamblerRecovery.get(a).id)&&r.pending==GamblerRecovery.get(a).pending,"outcome survives save reload");
        wealth=total(a,995);GamblerRecovery.claim(a);check(total(a,995)==wealth,"recovery claim preserves wealth");
        Player c=p();c.getInventory().getContainer().set(0,new Item(995,100000));GamblerSession s=begin(c,995,10000);advance(4);
        c.getInventory().getContainer().set(0,new Item(995,99999));check(!s.confirm()&&GamblerRecovery.get(c)==null,"inventory changed since review rejected");s.endSession();
        GamblerSession.open(c,npc);GamblerSession.dialogue(c,19101);c.closeAll(false,true);InputHandler.handleIntegerInput(c,10000);
        check(c.getActivity()==Mob.DEFAULT_ACTIVITY,"stale input cannot reopen cancelled round");
        s=begin(c,995,10000);advance(101);check(!s.confirm(),"expired review rejected");s.updateSession();check(c.getActivity()==Mob.DEFAULT_ACTIVITY,"expired round released");
        s=begin(c,995,10000);c.setLocation(Location.locate(3200,3200,0));advance(4);check(!s.confirm(),"remote confirm rejected");s.endSession();
    }
    static int resultFont(Player p){
        int found=-1;
        for(Message m:messages.get(p))if(m.getOpcode()==16){
            org.jboss.netty.buffer.ChannelBuffer b=m.getBuffer().duplicate();
            String types=BufferUtils.readRS2String(b);
            if(!types.equals("ii"))continue;
            int font=b.readInt(),widget=b.readInt(),script=b.readInt();
            if(script==1191&&widget==((634<<16)|29))found=font;
        }
        return found;
    }
    static void rewards()throws Exception {
        for(int id:new int[]{995,560,565,561}) {
            Player a=p();int stake=id==995?10000:100;a.getInventory().getContainer().set(0,new Item(id,stake*3));
            long before=total(a,id);GamblerSession s=begin(a,id,stake);advance(4);check(s.confirm(),"asset accepted "+id);s.endSession();
            check(total(a,id)==before,"asset conservation "+id);GamblerRecovery.claim(a);check(total(a,id)==before,"asset claim conservation "+id);
        }
        for(int[] rolls:new int[][]{{100,1},{1,100},{42,42}}) {
            Player a=p();GamblerRecovery.Record r=new GamblerRecovery.Record(UUID.randomUUID(),995,10000,rolls[0],rolls[1],GamblerRecovery.payout(10000,rolls[0],rolls[1]));
            GamblerRecovery.set(a,r);GamblerSession.open(a,npc);GamblerSession.dialogue(a,19104);
            check(text(a,634).get(29).contains(rolls[0]>rolls[1]?"You won":rolls[0]==rolls[1]?"Tie - refunded":"You lost"),"distinct result title");
            String color=rolls[0]>rolls[1]?"00cc00":rolls[0]==rolls[1]?"ffcc00":"ff0000";
            check(text(a,634).get(15).equals("<col="+color+">You rolled: "+rolls[0]+" | Gambler rolled: "+rolls[1]+"</col>"),"outcome-colored actual rolls");
            check(resultFont(a)==496,"larger result font");
            a.closeAll(false,true);
            check(resultFont(a)==495,"restore duel result font");
        }
        Player full=p();for(int n=0;n<28;n++)full.getInventory().getContainer().set(n,new Item(4151));
        GamblerRecovery.Record r=new GamblerRecovery.Record(UUID.randomUUID(),995,10000,90,1,20000);GamblerRecovery.set(full,r);
        check(World.getWorld().getPlayerLoader().save(full),"fixture saved");
        check(!GamblerRecovery.claim(full)&&GamblerRecovery.get(full).pending==20000,"full inventory retains payout");
        check(GamblerRecovery.get(reload(full)).pending==20000,"full payout survives reload");
        full.getInventory().getContainer().set(0,null);check(GamblerRecovery.claim(full)&&GamblerRecovery.get(full).pending==0,"free slot claims saved payout");
        long coins=count(full.getInventory().getContainer(),995);GamblerRecovery.claim(full);check(count(full.getInventory().getContainer(),995)==coins,"repeated claim no duplicate");
    }
    static void failures()throws Exception {
        Player a=p();a.getInventory().getContainer().set(0,new Item(995,100000));GamblerSession s=begin(a,995,10000);advance(4);
        Path journal=root.resolve("gamble-commit.bin");Files.createDirectory(journal);
        check(!s.confirm()&&count(a.getInventory().getContainer(),995)==100000&&GamblerRecovery.get(a)==null,"failed commit rolls back stake");
        Files.delete(journal);s.endSession();
        // Exhaustion must reject before even choosing a funded round.
        Map<Integer,Long> stock=new GamblerJournal(root).balances(GamblerPolicy.INSTANCE.seeds);long old=stock.get(995);stock.put(995,0L);
        DuelJournal.atomicWrite(root.resolve("gamble-house.bin"),GamblerJournal.encode(stock));
        s=begin(a,995,10000);advance(4);check(!s.confirm()&&count(a.getInventory().getContainer(),995)==100000,"unfunded house cannot accept");s.endSession();
        stock.put(995,old);DuelJournal.atomicWrite(root.resolve("gamble-house.bin"),GamblerJournal.encode(stock));
        Path isolated=Files.createTempDirectory(root,"journal-");GamblerJournal j=new GamblerJournal(isolated);j.balances(GamblerPolicy.INSTANCE.seeds);
        Files.createDirectory(isolated.resolve("test.bin"));UUID id=UUID.randomUUID();byte[] image={1,2,3,4};
        j.commit("test",image,stock,id,"round="+id);check(Files.exists(isolated.resolve("gamble-commit.bin")),"failed install retains durable commit");
        Files.delete(isolated.resolve("test.bin"));j.recover();j.recover();
        check(Arrays.equals(image,Files.readAllBytes(isolated.resolve("test.bin")))&&!Files.exists(isolated.resolve("gamble-commit.bin")),"recovery idempotently installs account");
        check(Files.readAllLines(isolated.resolve("gamble-rounds").resolve(id+".txt")).size()==1,"one durable receipt");
        Files.delete(isolated.resolve("gamble-house.bin"));boolean refused=false;try{j.balances(GamblerPolicy.INSTANCE.seeds);}catch(java.io.IOException e){refused=true;}
        check(refused,"missing initialized house cannot silently reseed");
    }
    static void extraRecovery()throws Exception {
        Player a=p();a.getInventory().getContainer().set(0,new Item(995,50000));
        GamblerRecovery.Record result=new GamblerRecovery.Record(UUID.randomUUID(),995,10000,100,1,20000);
        GamblerRecovery.set(a,result);check(World.getWorld().getPlayerLoader().save(a),"pending fixture saved");
        Path blocked=root.resolve("gamble-commit.bin");Files.createDirectory(blocked);
        check(!GamblerRecovery.claim(a)&&count(a.getInventory().getContainer(),995)==50000
            &&GamblerRecovery.get(a).pending==20000,"failed claim save rolls back inventory and entitlement");
        Files.delete(blocked);check(GamblerRecovery.claim(a),"claim retries after storage recovery");
        a.setAttribute("saveSessionClosed",true);GamblerRecovery.set(a,result);
        check(!GamblerRecovery.claim(a)&&GamblerRecovery.get(a).pending==20000,"closed player cannot claim");a.removeAttribute("saveSessionClosed");
        Container duelItems=new Container(1,false);duelItems.set(0,new Item(560,20));
        org.dementhium.content.activity.impl.duel.DuelRecovery.setPending(a,duelItems);
        check(World.getWorld().getPlayerLoader().save(a),"both trailers saved");Player restored=reload(a);
        check(GamblerRecovery.get(restored).id.equals(result.id)
            &&org.dementhium.content.activity.impl.duel.DuelRecovery.pending(restored).lookup(560).getAmount()==20,"GAM1 and DUL2 coexist");
        Path store=Files.createTempDirectory(root,"recover-loader-");PlayerLoader loader=new PlayerLoader(store);
        GamblerJournal journal=new GamblerJournal(store);Map<Integer,Long> stock=journal.balances(GamblerPolicy.INSTANCE.seeds);
        byte[] account=new byte[bytes(a).readableBytes()];bytes(a).getBytes(0,account);
        // Force failure AFTER account and house installation, before the durable receipt.
        Files.write(store.resolve("gamble-rounds"),new byte[]{1});
        journal.commit(a.getUsername(),account,stock,result.id,"round="+result.id);
        check(Files.exists(store.resolve("gamble-commit.bin")),"partial install stays recoverable");
        Files.delete(store.resolve("gamble-rounds"));Player candidate=p();
        candidate=new Player(candidate.getConnection(),new PlayerDefinition(a.getUsername(),"unused"));
        check(loader.load(candidate)&&GamblerRecovery.get(candidate).pending==20000
            &&!Files.exists(store.resolve("gamble-commit.bin")),"ordinary PlayerLoader load replays pending journal");
    }
    static void wanderingReplay()throws Exception {
        Player a=p();a.getInventory().getContainer().set(0,new Item(995,100000));
        GamblerSession first=begin(a,995,10000);advance(4);check(first.confirm(),"first round committed");
        advance(6);first.updateSession();click(a,634,17);
        GamblerRecovery.Record old=GamblerRecovery.get(a);
        Location origin=npc.getOriginalLocation();
        // The real spawn sits at y=3266, the south edge of ChallengeRoom.
        for(int dx=-3;dx<=3;dx++)for(int dy=-3;dy<=3;dy++) {
            npc.setLocation(origin.transform(dx,dy,0));a.setLocation(npc.getLocation().transform(0,-1,0));
            check(DialogueManager.handle(a,npc),"actual talk handler accepts interaction");
            check(a.getActivity() instanceof GamblerSession,"reopen beside wandering Gambler "+dx+":"+dy);
            GamblerSession s=(GamblerSession)a.getActivity();s.updateSession();
            check(s.phase()==GamblerSession.Phase.MENU,"session remains valid outside duel-room rectangle");
            check(GamblerRecovery.get(a).id.equals(old.id),"opening menu cannot settle previous round again");s.endSession();
        }
        npc.setLocation(origin.transform(0,-3,0));a.setLocation(npc.getLocation().transform(0,-1,0));
        GamblerSession second=begin(a,995,10000);advance(4);check(second.confirm(),"second wager commits outside challenge boundary");
        check(!GamblerRecovery.get(a).id.equals(old.id),"second wager has independent result");second.endSession();
        npc.setLocation(origin);a.setLocation(origin.transform(0,0,1));GamblerSession.open(a,npc);
        check(a.getActivity()==Mob.DEFAULT_ACTIVITY,"wrong plane rejected");
        a.setLocation(origin.transform(20,0,0));GamblerSession.open(a,npc);check(a.getActivity()==Mob.DEFAULT_ACTIVITY,"remote interaction rejected");
        npc.setLocation(origin.transform(0,-20,0));a.setLocation(npc.getLocation().transform(0,-1,0));GamblerSession.open(a,npc);
        check(a.getActivity()==Mob.DEFAULT_ACTIVITY,"NPC displaced beyond its stand rejected");npc.setLocation(origin);
    }
    static void rulesClose()throws Exception {
        Player a=p();GamblerSession.open(a,npc);GamblerSession.dialogue(a,19103);
        check(a.getActivity() instanceof GamblerSession,"rules session opened");
        check(text(a,214).get(1).contains("Both roll"),"five-line rules rendered");
        Message close=new MessageBuilder(4).writeShort(0).writeLEShort(6).writeLEShort(214).toMessage();
        new org.dementhium.net.packethandlers.DialogueHandler().handlePacket(a,close);
        check(a.getActivity()==Mob.DEFAULT_ACTIVITY,"rules close packet releases activity");
        check(a.getAttribute("nextDialougeStage")==null,"rules close clears dialogue state");
        GamblerSession.open(a,npc);check(a.getActivity() instanceof GamblerSession,"can reopen after rules close");a.closeAll(false,true);
        for(int lines=1;lines<=5;lines++){
            GamblerSession.open(a,npc);String[] text=new String[lines];Arrays.fill(text,"Test rules");
            DialogueManager.sendDisplayBox(a,19109,text);
            new org.dementhium.net.packethandlers.DialogueHandler().handlePacket(a,
                new MessageBuilder(4).writeShort(0).writeLEShort(lines+1).writeLEShort(209+lines).toMessage());
            check(a.getActivity()==Mob.DEFAULT_ACTIVITY,"display box continuation routed for "+lines+" lines");
        }
    }
    static Container previewItems(Player p,int type,boolean split){
        Container found=null;
        for(Message message:messages.get(p))if(message.getOpcode()==113){
            org.jboss.netty.buffer.ChannelBuffer b=message.getBuffer().duplicate();
            int id=b.readUnsignedShort();boolean other=b.readUnsignedByte()!=0;int size=b.readUnsignedShort();
            Container c=new Container(size,false);
            for(int n=0;n<size;n++){
                int item=(b.readUnsignedByte()<<8)|((b.readUnsignedByte()-128)&255);item--;
                int quantity=(-b.readUnsignedByte())&255;
                if(quantity==255){int x=b.readUnsignedByte(),y=b.readUnsignedByte(),z=b.readUnsignedByte(),w=b.readUnsignedByte();quantity=(z<<24)|(w<<16)|(x<<8)|y;}
                if(item>=0)c.set(n,new Item(item,quantity));
            }
            if(id==type&&other==split)found=c;
        }
        return found;
    }
    static int[] previewGridArgs(Player p,int parent){
        int[] found=null;
        for(Message m:messages.get(p))if(m.getOpcode()==16){
            org.jboss.netty.buffer.ChannelBuffer b=m.getBuffer().duplicate();String types=BufferUtils.readRS2String(b);
            java.util.List<Integer> values=new ArrayList<Integer>();
            for(int i=types.length()-1;i>=0;i--)if(types.charAt(i)=='s')BufferUtils.readRS2String(b);else values.add(b.readInt());
            int script=b.readInt();
            if(script==153&&values.size()==6&&values.get(5)==((626<<16)|parent)){
                found=new int[6];for(int n=0;n<6;n++)found[n]=values.get(n);
            }
        }
        return found;
    }
    static void previews()throws Exception{
        for(int id:new int[]{995,560,565,561}){
            Player a=p();int wager=id==995?1000000000:1000;
            a.getInventory().getContainer().set(0,new Item(id,wager));GamblerSession s=begin(a,id,wager);
            Container top=previewItems(a,134,false),bottom=previewItems(a,134,true),icons=previewItems(a,136,false);
            check(top!=null&&top.get(4).getId()==id&&top.get(4).getAmount()==wager,"wager item supplied to cached text and icon panel");
            check(bottom!=null&&bottom.get(4).getId()==id&&bottom.get(4).getAmount()==wager*2,"potential return supplied to cached text panel");
            check(icons!=null&&icons.get(4).getAmount()==wager*2,"potential return icon quantity");
            check(previewGridArgs(a,21)[2]==3&&previewGridArgs(a,22)[2]==3,"both one-time preview grids drawn");
            check(text(a,626).get(23).equals("Your wager")&&!text(a,626).get(34).contains("stake"),"gambling terminology");
            check(count(a.getInventory().getContainer(),id)==wager&&GamblerRecovery.get(a)==null,"preview creates no payout or debit");
            click(a,626,55);
            check(previewGridArgs(a,21)[2]*previewGridArgs(a,21)[3]<0&&previewGridArgs(a,22)[2]*previewGridArgs(a,22)[3]<0,"cancel clears dynamic preview children");
            check(count(a.getInventory().getContainer(),id)==wager,"cancel preserves inventory");
        }
    }
    static void billionWager()throws Exception{
        check(GamblerPolicy.INSTANCE.valid(995,1000000000)&&!GamblerPolicy.INSTANCE.valid(995,1000000001),"one billion GP exact cap");
        check(GamblerRecovery.payout(1000000000,100,1)==2000000000,"two billion total payout stays within item range");
        Map<Integer,Long> funds=new GamblerJournal(root).balances(GamblerPolicy.INSTANCE.seeds);
        funds.put(995,1000000000L);DuelJournal.atomicWrite(root.resolve("gamble-house.bin"),GamblerJournal.encode(funds));
        Player a=p();a.getInventory().getContainer().set(0,new Item(995,1000000000));
        long before=total(a,995);GamblerSession s=begin(a,995,1000000000);advance(4);
        check(s.confirm(),"one billion round commits when funded");advance(6);s.updateSession();
        check(total(a,995)==before,"billion wager and payout conserve house/account wealth");s.endSession();
    }
    public static void main(String[] args)throws Exception {
        root=Files.createTempDirectory(Paths.get("build/gambler"),"test-accounts-");
        Cache.init();MapXTEA.loadPackedFile();NPCDefinition.init();ItemDefinition.init();GroundItemManager.load();
        field(World.getWorld(),World.class,"areaManager",new AreaManager());field(World.getWorld(),World.class,"playerLoader",new PlayerLoader(root));
        InstanceManager.getSingleton().beginCycle();npc=new NPC(2998,Location.locate(3367,3266,0));World.getWorld().getNpcs().add(npc);
        previews();rulesClose();wanderingReplay();odds();routes();isolation();rewards();failures();extraRecovery();billionWager();
        System.out.println("PASS: "+checks+" Gambler checks; isolated storage "+root);
    }
}

