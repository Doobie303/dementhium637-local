import java.lang.reflect.*;
import java.util.*;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.buffer.*;
import org.dementhium.cache.Cache;
import org.dementhium.model.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.player.*;
import org.dementhium.net.GameSession;
import org.dementhium.net.message.Message;
import org.dementhium.net.packethandlers.InputPacketHandler;

public class ItemTransactionRegression {
 static int checks,sequence;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static Player p(){Channel c=(Channel)Proxy.newProxyInstance(Channel.class.getClassLoader(),new Class[]{Channel.class},(o,m,a)->{if(m.getReturnType()==boolean.class)return true;if(m.getReturnType()==int.class)return 0;return null;});return new Player(new GameSession(c),new PlayerDefinition("itemfixture"+(++sequence),"unused"));}
 static String image(Container c){StringBuilder b=new StringBuilder();for(Item i:c.toArray())b.append(i==null?"empty":i.getId()+":"+i.getHash()).append(';');return b.toString();}
 static long count(Container c,int id){long n=0;for(Item i:c.toArray())if(i!=null&&i.getId()==id)n+=i.getAmount();return n;}
 static Item worn(int id,int health){Item i=new Item(id);i.setHealth(health);return i;}
 static void fill(Container c,int first){for(int i=first;i<c.getSize();i++)c.set(i,new Item(4151));}
 static TradeSession trade(Player a,Player b){a.getDefinition().setRights(2);b.getDefinition().setRights(2);TradeSession t=new TradeSession(a,b);a.setTradeSession(t);b.setTradePartner(a);return t;}
 static void numeric(Player p,int amount){p.setAttribute("inputId",4);p.setAttribute("slotId",0);ChannelBuffer b=ChannelBuffers.dynamicBuffer();b.writeInt(amount);new InputPacketHandler().handlePacket(p,new Message(34,Message.PacketType.STANDARD,b));}
 static void bank(){
  for(int amount:new int[]{Integer.MIN_VALUE,-100,-1,0}){
   Player p=p();p.setAttribute("inBank",true);p.getInventory().set(0,new Item(995,100));String inv=image(p.getInventory().getContainer()),bank=image(p.getBank().getContainer());
   numeric(p,amount);check(inv.equals(image(p.getInventory().getContainer()))&&bank.equals(image(p.getBank().getContainer())),"invalid numeric deposit must conserve items: "+amount);
  }
  Player p=p();p.setAttribute("inBank",true);fill(p.getBank().getContainer(),0);p.getInventory().set(0,worn(4856,42));
  String full=image(p.getBank().getContainer());p.getBank().addItem(0,1,false);
  check(full.equals(image(p.getBank().getContainer()))&&p.getInventory().get(0).getHealth()==42,"full charged bank rejects without overwrite");
  p.getEquipment().set(Equipment.SLOT_HAT,worn(4856,71));p.getEquipment().set(Equipment.SLOT_CHEST,new Item(1127));p.getBank().bankEquip();
  check(full.equals(image(p.getBank().getContainer()))&&p.getEquipment().get(Equipment.SLOT_HAT).getHealth()==71&&p.getEquipment().get(Equipment.SLOT_CHEST)!=null,"bulk equipment deposit rejects full bank without overwriting");
  p.getBank().set(515,null);p.getInventory().set(1,worn(4856,93));p.getBank().addItem(0,1,false);
  check(p.getBank().get(515).getHealth()==42&&p.getInventory().get(0)==null&&p.getInventory().get(1).getHealth()==93,"charged deposit preserves selected and adjacent copy metadata");
  Player w=p();w.setAttribute("inBank",true);w.getBank().set(0,worn(4856,10));w.getBank().set(1,worn(4856,42));w.getBank().removeItem(1,1);
  check(w.getInventory().get(0).getHealth()==42&&w.getBank().get(0).getHealth()==10,"charged withdrawal consumes selected slot only");
  fill(w.getInventory().getContainer(),1);String held=image(w.getBank().getContainer());w.getBank().removeItem(0,1);
  check(held.equals(image(w.getBank().getContainer())),"full inventory charged withdrawal does not debit bank");
  Player ordinary=p();ordinary.setAttribute("inBank",true);ordinary.getInventory().set(0,new Item(995,100));numeric(ordinary,40);
  check(ordinary.getInventory().numberOf(995)==60&&ordinary.getBank().getContainer().getItemCount(995)==40,"positive bank numeric deposit");
 }
 static void containers(){
  Container d=new Container(2,false),s=new Container(2,false);d.set(0,new Item(4151));s.set(0,new Item(1127));s.set(1,new Item(1163));
  String before=image(d);check(!d.hasSpaceFor(s)&&before.equals(image(d)),"collective capacity and no mutation");
  d.clear();check(d.hasSpaceFor(s)&&d.tryAddAll(s),"exact capacity success");
  Container stack=new Container(1,false),add=new Container(2,false);stack.set(0,new Item(995,Integer.MAX_VALUE-10));add.set(0,new Item(995,6));add.set(1,new Item(995,5));
  before=image(stack);check(!stack.hasSpaceFor(add)&&before.equals(image(stack)),"collective stack overflow");
  add.get(1).setAmount(4);check(stack.hasSpaceFor(add)&&stack.tryAddAll(add)&&stack.get(0).getAmount()==Integer.MAX_VALUE,"exact stack cap");
  Container split=new Container(2,true,false,true);split.set(0,new Item(995,Integer.MAX_VALUE-1));add.clear();add.set(0,new Item(995,2));
  check(split.hasSpaceFor(add)&&split.tryAddAll(add)&&count(split,995)==(long)Integer.MAX_VALUE+1,"split-stack capacity retained");
 }
 static void trades(){
  for(int side=0;side<2;side++){
   Player a=p(),b=p(),owner=side==0?a:b;TradeSession t=trade(a,b);owner.getInventory().set(0,new Item(995,100));t.offerItem(owner,0,100);fill(owner.getInventory().getContainer(),0);
   String inv=image(owner.getInventory().getContainer());t.removeItem(owner,0,100);
   check(count(t.getPlayerItemsOffered(owner),995)==100&&inv.equals(image(owner.getInventory().getContainer())),"full inventory removal retains offer, side "+side);
   owner.getInventory().set(0,null);t.removeItem(owner,0,100);
   check(owner.getInventory().numberOf(995)==100&&count(t.getPlayerItemsOffered(owner),995)==0,"retry removal transfers once");
   t.removeItem(owner,0,100);check(owner.getInventory().numberOf(995)==100,"empty-slot repeat cannot duplicate");
   owner.getInventory().getContainer().clear();fill(owner.getInventory().getContainer(),0);fill(t.getPlayerItemsOffered(owner),0);t.getPlayerItemsOffered(owner).set(27,null);
   t.offerItem(owner,0,28);check(count(owner.getInventory().getContainer(),4151)==27&&count(t.getPlayerItemsOffered(owner),4151)==28,"partial nonstack offer debits exactly one");
  }
  int[] denied={4856,4857,4860,4861,4214,4223,4225,4234,20137,20138,13860};
  for(int id:denied)for(int right:new int[]{0,2}){
   Player a=p(),b=p();TradeSession t=trade(a,b);a.getDefinition().setRights(right);b.getDefinition().setRights(right);a.getInventory().set(0,new Item(id));
   t.offerItem(a,0,1);check(a.getInventory().get(0)!=null&&t.getPlayerItemsOffered(a).size()==0,"degraded ID blocked regardless of rights: "+id);
  }
  Player a=p(),b=p();TradeSession t=trade(a,b);a.getInventory().set(0,worn(4716,42));t.offerItem(a,0,1);
  check(a.getInventory().get(0).getHealth()==42&&t.getPlayerItemsOffered(a).size()==0,"stored wear blocked even with pristine ID");
  a.getInventory().set(0,new Item(4716));a.getDefinition().setRights(0);b.getDefinition().setRights(0);t.offerItem(a,0,1);
  check(a.getInventory().get(0)==null&&t.getPlayerItemsOffered(a).get(0).getId()==4716,"pristine tradeable gear allowed");
  a=p();b=p();t=trade(a,b);a.getInventory().set(0,new Item(995,100));t.offerItem(a,0,100);a.getInventory().set(0,new Item(995,100));t.offerItem(a,0,-1);
  check(a.getInventory().numberOf(995)==100&&count(t.getPlayerItemsOffered(a),995)==100,"negative trade unchanged");
  t.acceptPressed(a);t.acceptPressed(b);t.acceptPressed(a);t.acceptPressed(b);
  check(b.getInventory().numberOf(995)==100&&a.getInventory().numberOf(995)==100&&a.getTradeSession()==null&&b.getTradeSession()==null,"two confirmations settle exchange");
  t.acceptPressed(a);t.tradeFailed(a);check(b.getInventory().numberOf(995)==100,"closed trade calls are idempotent");
  a=p();b=p();t=trade(a,b);a.getInventory().set(0,new Item(995,100));t.offerItem(a,0,100);t.tradeFailed(a);t.tradeFailed(b);
  check(a.getInventory().numberOf(995)==100&&a.getTradeSession()==null&&b.getTradeSession()==null,"decline returns offer exactly once");
  a=p();b=p();t=trade(a,b);a.getInventory().set(0,new Item(995,100));t.offerItem(a,0,100);a.getInventory().set(0,new Item(995,Integer.MAX_VALUE-20));t.removeItem(a,0,100);
  check(a.getInventory().numberOf(995)==Integer.MAX_VALUE&&count(t.getPlayerItemsOffered(a),995)==80,"trade withdrawal clamps at exact stack cap");
  a=p();b=p();t=trade(a,b);a.getInventory().set(0,new Item(1127));a.getInventory().set(1,new Item(1163));t.offerItem(a,0,1);t.offerItem(a,1,1);fill(b.getInventory().getContainer(),1);
  t.acceptPressed(a);t.acceptPressed(b);check(t.getState()==TradeSession.TradeState.STATE_ONE&&t.getPlayerItemsOffered(a).size()==2,"first confirmation rejects combined capacity without dropping offers");
  b.getInventory().set(1,null);t.acceptPressed(a);t.acceptPressed(b);check(t.getState()==TradeSession.TradeState.STATE_TWO,"freed capacity allows review");
  b.getInventory().set(1,new Item(4151));t.acceptPressed(a);t.acceptPressed(b);
  check(t.getState()==TradeSession.TradeState.STATE_ONE&&t.getPlayerItemsOffered(a).size()==2&&b.getInventory().numberOf(1127)==0,"capacity lost on second screen returns to offer without partial payment");
 }
 static void shops()throws Exception{
  Method sell=Shop.class.getDeclaredMethod("sellItem",Player.class,int.class,int.class);sell.setAccessible(true);
  int old=ItemDefinition.forId(4151).getStorePrice();ItemDefinition.forId(4151).setStorePrice(100);
  try {
   for(int room:new int[]{0,1,99,100,101,199,200,1000}) {
    Player p=p();p.getInventory().set(0,new Item(995,Integer.MAX_VALUE-room));p.getInventory().set(1,new Item(4151));p.getInventory().set(2,new Item(4151));
    Shop s=new Shop(1,true,new int[]{4151},new int[]{1},true,995);sell.invoke(s,p,4151,2);int quantity=Math.min(2,room/100);
    check(p.getInventory().numberOf(4151)==2-quantity,"shop removes final quantity room="+room);
    check(p.getInventory().numberOf(995)==Integer.MAX_VALUE-room+quantity*100,"shop exact payment room="+room);
    check(s.getShop().getItemCount(4151)==1+quantity,"shop exact stock room="+room);
   }
   Player p=p();fill(p.getInventory().getContainer(),0);Shop s=new Shop(1,true,new int[]{4151},new int[]{1},true,995);sell.invoke(s,p,4151,1);
   check(p.getInventory().numberOf(4151)==27&&p.getInventory().numberOf(995)==100,"sale can use freed item slot for coins");
   p=p();p.getInventory().set(0,new Item(4151));s=new Shop(1,true,new int[]{4151},new int[]{Integer.MAX_VALUE},true,995);sell.invoke(s,p,4151,1);
   check(p.getInventory().numberOf(4151)==1&&p.getInventory().numberOf(995)==0&&s.getShop().getItemCount(4151)==Integer.MAX_VALUE,"stock overflow rejected atomically");
   p=p();p.getInventory().set(0,new Item(4151));s=new Shop(1,true,new int[]{1127},new int[]{1},true,995);fill(s.getShop(),1);String before=image(s.getShop());sell.invoke(s,p,4151,-1);
   check(before.equals(image(s.getShop()))&&p.getInventory().numberOf(4151)==1,"negative shop quantity rejected");
   Random random=new Random(637);
   for(int n=0;n<200;n++){
    int price=1+random.nextInt(1000000000),room=random.nextInt(Integer.MAX_VALUE),owned=1+random.nextInt(27),requested=1+random.nextInt(500);
    ItemDefinition.forId(4151).setStorePrice(price);p=p();p.getInventory().set(0,new Item(995,Integer.MAX_VALUE-room));for(int i=1;i<=owned;i++)p.getInventory().set(i,new Item(4151));
    s=new Shop(1,true,new int[]{4151},new int[]{1},true,995);sell.invoke(s,p,4151,requested);int quantity=Math.min(Math.min(owned,requested),room/price);
    check(p.getInventory().numberOf(4151)==owned-quantity&&count(s.getShop(),4151)==1+quantity,"randomized sale item conservation");
    check(count(p.getInventory().getContainer(),995)==(long)Integer.MAX_VALUE-room+(long)quantity*price,"randomized sale exact payment");
   }
  } finally {ItemDefinition.forId(4151).setStorePrice(old);}
 }
 public static void main(String[] args)throws Exception{Cache.init();ItemDefinition.init();NPCDefinition.init();
  String mode=args.length==0?"all":args[0];
  if(mode.equals("all")||mode.equals("bank"))bank();
  if(mode.equals("all")||mode.equals("containers"))containers();
  if(mode.equals("all")||mode.equals("trades"))trades();
  if(mode.equals("all")||mode.equals("shops"))shops();
  System.out.println("PASS: "+checks+" item transaction checks");}
}
