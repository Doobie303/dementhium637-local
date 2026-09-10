import java.util.*;
import org.dementhium.content.Commands;
import org.dementhium.content.minigames.gambler.*;
import org.dementhium.model.*;
import org.dementhium.model.player.Player;
import org.dementhium.io.*;

public class GamblerLiveInterfaceRegression extends GamblerRegression {
 static boolean visible(Player p,int child){
  boolean shown=true;
  for(org.dementhium.net.message.Message m:messages.get(p))if(m.getOpcode()==3){
   org.jboss.netty.buffer.ChannelBuffer b=m.getBuffer().duplicate();
   int a=b.readUnsignedByte(),c=b.readUnsignedByte(),d=b.readUnsignedByte(),e=b.readUnsignedByte();
   int widget=(c<<24)|(a<<16)|(e<<8)|d;int hidden=(-b.readUnsignedByte())&255;
   if(widget==((891<<16)|child))shown=hidden==0;
  }
  return shown;
 }
 static void buttons(Player p,boolean actions,boolean collect){
  for(int child:new int[]{24,25,29,30})check(visible(p,child)==actions,"action visibility "+child);
  for(int child:new int[]{26,31})check(visible(p,child)==collect,"collect visibility "+child);
  check(visible(p,27)&&visible(p,32)&&visible(p,33),"close remains visible");
 }
 public static void main(String[] args)throws Exception {
  GamblerRegression.main(args);int baseline=checks;
  Map<Integer,Long> funds=new GamblerJournal(root).balances(GamblerPolicy.INSTANCE.seeds);funds.put(995,1000000000L);DuelJournal.atomicWrite(root.resolve("gamble-house.bin"),GamblerJournal.encode(funds));
  Player a=p();a.getInventory().getContainer().set(0,new Item(995,100000));Commands.handle(a,new String[]{"gamblerui","on"});
  GamblerSession s=begin(a,995,10000);check(text(a,891).get(12).equals("10,000 GP"),"custom review wager");check(text(a,891).get(13).contains("20,000"),"custom return");
  buttons(a,true,false);click(a,891,29);check(s.phase()==GamblerSession.Phase.REVIEW,"custom review delay");
  advance(4);click(a,626,53);check(s.phase()==GamblerSession.Phase.REVIEW,"legacy confirm cannot submit custom review");
  long before=total(a,995);click(a,891,29);check(s.phase()==GamblerSession.Phase.ROLLING,"custom real wager commits");UUID round=GamblerRecovery.get(a).id;buttons(a,false,false);
  click(a,891,29);click(a,891,31);check(GamblerRecovery.get(a).id.equals(round)&&total(a,995)==before,"rolling duplicate controls conserve money");
  advance(3);s.updateSession();check(text(a,891).get(20).equals(String.valueOf(GamblerRecovery.get(a).playerRoll)),"first roll revealed");check(text(a,891).get(21).equals("Rolling..."),"house roll still concealed");
  advance(3);s.updateSession();check(s.phase()==GamblerSession.Phase.RESULT,"custom completed result");buttons(a,true,false);check(text(a,891).get(21).contains(String.valueOf(GamblerRecovery.get(a).houseRoll)),"real house number");
  check(total(a,995)==before,"custom result conserves value");click(a,891,29);check(s.phase()==GamblerSession.Phase.MENU,"new wager returns to selection");
  click(a,891,29);check(s.phase()==GamblerSession.Phase.MENU,"queued old confirm cannot start new wager");s.endSession();
  s=begin(a,995,10000);click(a,891,30);check(s.phase()==GamblerSession.Phase.MENU,"change wager abandons review without debit");s.endSession();
  for(int[] rolls:new int[][]{{90,20},{20,90},{42,42}}){
   Player b=p();b.setAttribute("gamblerCustomUi",true);GamblerRecovery.Record r=new GamblerRecovery.Record(UUID.randomUUID(),565,100,rolls[0],rolls[1],0);GamblerRecovery.set(b,r);
   GamblerSession.open(b,npc);GamblerSession.dialogue(b,19104);
   check(text(b,891).get(10).toLowerCase().contains("blood"),"last result uses recorded rune asset");
   check(text(b,891).get(20).contains(rolls[0]>rolls[1]?"62e68a":rolls[0]<rolls[1]?"ff6868":"efcf7d"),"result color");click(b,891,32);check(b.getActivity()==Mob.DEFAULT_ACTIVITY,"custom close");
  }
  Player full=p();full.setAttribute("gamblerCustomUi",true);for(int n=0;n<28;n++)full.getInventory().getContainer().set(n,new Item(4151));
  GamblerRecovery.set(full,new GamblerRecovery.Record(UUID.randomUUID(),995,10000,90,1,20000));check(World.getWorld().getPlayerLoader().save(full),"pending fixture saved");
  GamblerSession.open(full,npc);GamblerSession.dialogue(full,19104);buttons(full,true,true);click(full,891,31);buttons(full,true,true);check(GamblerRecovery.get(full).pending==20000,"collect with full inventory preserves pending");
  full.getInventory().getContainer().set(0,null);click(full,891,31);check(GamblerRecovery.get(full).pending==0&&count(full.getInventory().getContainer(),995)==20000,"custom collect exactly once");buttons(full,true,false);click(full,891,31);check(count(full.getInventory().getContainer(),995)==20000,"repeated collect cannot dupe");full.closeAll(false,true);
  Commands.handle(a,new String[]{"gamblerui","off"});s=begin(a,995,10000);check(text(a,626).get(53).equals("Roll"),"old-client fallback");advance(4);click(a,891,29);check(s.phase()==GamblerSession.Phase.REVIEW,"custom packet cannot submit legacy review");s.endSession();
  System.out.println("PASS: "+(checks-baseline)+" live custom interface checks in addition to "+baseline+" Gambler checks");
 }
}

