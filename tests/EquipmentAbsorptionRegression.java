import java.lang.reflect.*;
import java.util.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.dementhium.cache.Cache;
import org.dementhium.content.DialogueManager;
import org.dementhium.model.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.player.*;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.net.*;
import org.dementhium.net.codec.DefaultGameDecoder;
import org.dementhium.net.message.*;
import org.dementhium.net.packethandlers.ActionButtonHandler;
import org.dementhium.net.packethandlers.WalkingHandler;
import org.dementhium.tickable.Tick;

public class EquipmentAbsorptionRegression {
    static int checks;
    static Map<Integer,String> labels = new HashMap<Integer,String>();
    static List<String> opens = new ArrayList<String>();
    static List<Integer> closes = new ArrayList<Integer>();
    static Map<Integer,Integer> varbits = new HashMap<Integer,Integer>();
    static Map<Integer,Integer> configs = new HashMap<Integer,Integer>();
    static Map<Integer,Boolean> visible = new HashMap<Integer,Boolean>();
    static Map<Integer,String> itemStatsText = new HashMap<Integer,String>();
    static List<Integer> scripts = new ArrayList<Integer>();
    static List<String> events = new ArrayList<String>();
    static int intV2(Message m) {
        int a=m.readByte()&255,b=m.readByte()&255,c=m.readByte()&255,d=m.readByte()&255;
        return (b<<24)|(a<<16)|(d<<8)|c;
    }
    static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    static Player player() {
        Channel channel = (Channel) Proxy.newProxyInstance(Channel.class.getClassLoader(), new Class[]{Channel.class},
            (proxy, method, args) -> {
                if (method.getName().equals("isConnected") || method.getName().equals("isOpen")) return true;
                if (method.getName().equals("write") && args[0] instanceof Message) {
                    Message m = (Message) args[0];
                    if (m.getOpcode() == 33) { String value=m.readRS2String(); int component=m.readLEInt(); labels.put(component,value); events.add("text:"+component); }
                    if (m.getOpcode() == 61) closes.add(m.readLEInt());
                    if (m.getOpcode() == 88) { String value=m.readRS2String(); itemStatsText.put(m.readLEShortA(),value); }
                    if (m.getOpcode() == 16 && m.readRS2String().isEmpty()) scripts.add(m.readInt());
                    if (m.getOpcode() == 38) { int value=m.readByteC()&255; varbits.put(m.readShortA(),value); }
                    if (m.getOpcode() == 27) {
                        int a=m.readByte()&255,b=m.readByte()&255,c=m.readByte()&255,d=m.readByte()&255;
                        configs.put(m.readShort(),(c<<24)|(d<<16)|(a<<8)|b);
                    }
                    if (m.getOpcode() == 3) { int component=intV2(m); visible.put(component,m.readByteC()==0); }
                    if (m.getOpcode() == 50) {
                        // Decode V2 bytes explicitly: Message.readInt2 currently masks the high bytes incorrectly.
                        int a=m.readByte()&255,b=m.readByte()&255,c=m.readByte()&255,d=m.readByte()&255;
                        int parent=(b<<24)|(a<<16)|(d<<8)|c;
                        String opened=parent+":"+m.readLEShortA(); opens.add(opened); events.add("open:"+opened);
                    }
                }
                if (method.getReturnType()==boolean.class) return false;
                if (method.getReturnType()==int.class) return 0;
                return null;
            });
        Player p = new Player(new GameSession(channel),new PlayerDefinition("equipment-test","unused"));
        p.setHasReceivedStarter(true);
        p.getSkills().setMaximumLifePoints(1000); p.getSkills().setHitPoints(1000);
        p.setLocation(Location.locate(3200,3200,0));
        return p;
    }
    static void item(int id,int melee,int magic,int ranged) {
        check(Arrays.equals(ItemDefinition.forId(id).getAbsorptionBonus(),new int[]{melee,magic,ranged}),"absorption item "+id);
    }
    static void click(Player p,int ui,int button,int id) throws Exception {
        click(p,ui,button,id,-1,6);
    }
    static void click(Player p,int ui,int button,int id,int slot,int opcode) throws Exception {
        MessageBuilder b=new MessageBuilder(opcode);b.writeShort(ui);b.writeShort(button);b.writeLEShortA(slot);b.writeShort(id);
        // Bypass the handler's catch-all so an invalid button actually fails this regression.
        Method m=ActionButtonHandler.class.getDeclaredMethod("handleButtons",Player.class,Message.class,int.class);
        m.setAccessible(true);m.invoke(new ActionButtonHandler(),p,b.toMessage(),0);
    }
    static Message decodeClientClose() throws Exception {
        ChannelHandlerContext context=(ChannelHandlerContext)Proxy.newProxyInstance(ChannelHandlerContext.class.getClassLoader(),new Class[]{ChannelHandlerContext.class},
            (proxy,method,args)->null);
        ChannelPipeline pipeline=(ChannelPipeline)Proxy.newProxyInstance(ChannelPipeline.class.getClassLoader(),new Class[]{ChannelPipeline.class},
            (proxy,method,args)->method.getName().equals("getContext")?context:null);
        Channel channel=(Channel)Proxy.newProxyInstance(Channel.class.getClassLoader(),new Class[]{Channel.class},
            (proxy,method,args)->method.getName().equals("getPipeline")?pipeline:null);
        DefaultGameDecoder decoder=new DefaultGameDecoder(new GameSession(channel));
        Method decode=DefaultGameDecoder.class.getDeclaredMethod("decode",ChannelHandlerContext.class,Channel.class,ChannelBuffer.class);
        decode.setAccessible(true);
        return (Message)decode.invoke(decoder,context,channel,ChannelBuffers.wrappedBuffer(new byte[]{32}));
    }
    public static void main(String[] args) throws Exception {
        Cache.init();ItemDefinition.init();NPCDefinition.init();
        Message closePacket=decodeClientClose();
        check(closePacket!=null&&closePacket.getOpcode()==32&&closePacket.getLength()==0,"Zero-length client close packet decoded");
        Player closing=player();closing.setAttribute("inBank",true);closing.setAttribute("fromBank",true);closing.setAttribute("bankScreen",2);closes.clear();
        new PacketManager().handlePacket(closing,closePacket);
        check(!closing.getAttribute("inBank",false)&&!closing.getAttribute("fromBank",false),"Client close packet clears bank stats state");
        check(closing.getAttribute("bankScreen")==null,"Client close packet clears bank screen marker");
        check(closes.contains((548<<16)|18)&&closes.contains((548<<16)|197),"Client close packet closes both bank stats panels");
        item(1127,3,0,6);item(1163,1,0,3);
        item(18359,7,0,14);item(18361,0,14,7);item(18363,14,7,0);
        item(17259,0,0,0);item(16711,0,0,0);item(16689,0,0,0);
        item(11724,4,0,9);item(11726,3,0,6);
        item(11718,0,5,2);item(11720,0,10,5);item(11722,0,7,3);
        for(int offset:new int[]{0,2}) {
            item(20135+offset,3,0,6);item(20139+offset,6,0,12);item(20143+offset,4,0,8);
            item(20147+offset,0,6,3);item(20151+offset,0,12,6);item(20155+offset,0,8,4);
            item(20159+offset,6,3,0);item(20163+offset,12,6,0);item(20167+offset,8,4,0);
        }
        Player p=player();p.getEquipment().set(Equipment.SLOT_CHEST,new Item(4895)); // Dharok 25%.
        check(p.getBonuses().getAbsorptionBonus(0)==5,"Worn Barrows retains absorption");
        p.getEquipment().set(Equipment.SLOT_CHEST,new Item(4896)); // Broken.
        check(p.getBonuses().getAbsorptionBonus(0)==0,"Broken Barrows not mapped to working armour");
        p.getEquipment().set(Equipment.SLOT_CHEST,new Item(20142));
        check(p.getBonuses().getAbsorptionBonus(0)==0,"Broken Torva not mapped to working armour");
        for(int mode=0;mode<=3;mode++) {
            p=player();p.getConnection().setDisplayMode(mode);
            p.getEquipment().set(Equipment.SLOT_HAT,new Item(20137));
            p.getEquipment().set(Equipment.SLOT_CHEST,new Item(20141));
            p.getEquipment().set(Equipment.SLOT_LEGS,new Item(20145));
            p.getWalkingQueue().reset();p.getWalkingQueue().setRunToggled(true);
            p.getWalkingQueue().addToWalkingQueue(p.getViewportX()+5,p.getViewportY());
            check(p.getWalkingQueue().writePosition>p.getWalkingQueue().readPosition,"Queued running fixture");
            p.submitTick("following_mob",new Tick(1){public void execute(){throw new AssertionError("Follow must stop");}});
            p.getCombatExecutor().setVictim(player());
            p.setAttribute("godmode",true);p.setAttribute("freezeTime",10000);
            labels.clear();opens.clear();closes.clear();events.clear();
            click(p,387,39,-1);
            check((configs.get(1248)&Integer.MIN_VALUE)==0,"Ordinary stats disables bank return varbit 4894");
            check(Boolean.FALSE.equals(visible.get((667<<16)|48)),"Ordinary stats hides bank return parent");
            check(p.getWalkingQueue().readPosition==p.getWalkingQueue().writePosition,"Stats clears queued movement");
            check(p.getWalkingQueue().isRunToggled(),"Run toggle preserved");
            check(!p.hasTick("following_mob")&&p.getCombatExecutor().getVictim()==null,"Follow and attack stop");
            check(p.getAttribute("godmode",false)&&p.getAttribute("freezeTime",0)==10000,"Custom immunity and movement locks preserved");
            p.getWalkingQueue().getNextEntityMovement();
            check(p.getLocation().getX()==3200&&p.getLocation().getY()==3200,"Opening stats does not move player");
            check("Absorb Melee: +13%".equals(labels.get((667<<16)|41)),"Fresh melee display");
            check("Absorb Magic: +0%".equals(labels.get((667<<16)|42)),"Melee armour has no magic soak");
            check("Absorb Range: +26%".equals(labels.get((667<<16)|43)),"Fresh ranged display");
            int root=mode<2?548:746,main=mode<2?18:9,inv=mode<2?197:84;
            check("9 kg".equals(labels.get((667<<16)|24)),"Weight uses the worn definitions and native integer format");
            check(events.indexOf("open:"+((root<<16)|main)+":667")<events.indexOf("text:"+((667<<16)|24)),"Weight refresh follows equipment on-load defaults");
            check(p.hasTick("equipment_screen_refresh"),"Equipment open schedules a post-clientscript refresh");
            labels.clear();p.processTicks();
            check("9 kg".equals(labels.get((667<<16)|24))&&!p.hasTick("equipment_screen_refresh"),"Post-clientscript refresh restores weight without an equipment action");
            check(opens.contains(((root<<16)|main)+":667"),"Main panel mode "+mode);
            check(opens.contains(((root<<16)|inv)+":670"),"Inventory panel mode "+mode);
            p.closeAll(true,true);
            check(closes.contains((root<<16)|main)&&closes.contains((root<<16)|inv),"Both panels close mode "+mode);
            // Only the last 300 LP of a 500 hit soak; the displayed 13% is used by actual damage.
            p.removeAttribute("godmode");p.getDamageManager().miscDamage(500,DamageType.MELEE);
            check(p.getHitPoints()==539,"Actual Torva damage 461 LP");
            check(p.getDamageManager().getHits().getLast().getPartner().getDamage()==39,"Soak hitsplat agrees");
            opens.clear();closes.clear();p.getBank().openBank();opens.clear();closes.clear();
            // Real clientscript 2318 toggles this locally BEFORE sending the button packet.
            varbits.put(8348,1); click(p,762,117,-1);
            check(varbits.get(8348)==0,"Stats transition resets client-local bank view flag");
            check((configs.get(1248)&Integer.MIN_VALUE)!=0,"Bank stats enables return varbit 4894 for on-load and config scripts");
            check(Boolean.TRUE.equals(visible.get((667<<16)|48)),"Bank return parent is visible, not only its children");
            check(Boolean.TRUE.equals(visible.get(667<<16)),"Equipment root is visible after client scripts");
            itemStatsText.clear();scripts.clear();
            click(p,667,7,20141,Equipment.SLOT_CHEST,73);
            check(itemStatsText.get(321).equals(ItemDefinition.forId(20141).getName()),"Stats names the selected worn Torva item");
            String details=itemStatsText.get(324);
            check(details.contains("Absorb melee: +6%")&&details.contains("Absorb ranged: +12%"),"Stats uses individual Torva absorption, not outfit totals");
            check(details.contains("Stab: "+(ItemDefinition.forId(DegradingHandler.getCombatItemId(20141)).getBonus()[0]>=0?"+":"")+ItemDefinition.forId(DegradingHandler.getCombatItemId(20141)).getBonus()[0]),"Selected attack bonus comes from item definition");
            check(scripts.contains(2782)&&itemStatsText.size()==5,"Supplies all five native popup fields and renders them");
            check(p.getAttribute("fromBank",false),"Item stats preserves bank return state");
            click(p,667,64,-1);
            check(scripts.contains(2947),"Back clears popup strings and both inventory blockers");
            p.getInventory().set(0,new Item(1127));itemStatsText.clear();scripts.clear();
            click(p,670,0,1127,0,73);
            check(itemStatsText.get(321).equals("Rune platebody")&&itemStatsText.get(324).contains("Absorb melee: +3%"),"Inventory equipment stats works");
            p.getInventory().set(0,new Item(385));itemStatsText.clear();scripts.clear();
            click(p,670,0,385,0,73);
            check(itemStatsText.isEmpty()&&!scripts.contains(2782)&&scripts.contains(2947),"Food closes old details without showing equipment stats");
            scripts.clear();click(p,670,0,1127,0,73);click(p,670,0,1127,-1,73);click(p,667,7,1127,Equipment.SLOT_CHEST,73);
            check(scripts.isEmpty(),"Stale and invalid selected items do not open stats");
            p.getInventory().set(0,new Item(4895));click(p,670,0,4895,0,73);
            check(itemStatsText.get(324).contains("Absorb melee: +5%"),"Worn Barrows item details use its effective definition");
            p.getInventory().set(0,new Item(892));click(p,670,0,892,0,73);
            check(itemStatsText.get(324).contains("Ranged strength: +49"),"Ammo displays its own verified strength independent of worn weapon");
            check(!p.getAttribute("inBank",false)&&p.getAttribute("fromBank",false),"Bank stats keeps only its return marker");
            check(closes.contains((root<<16)|main)&&closes.contains((root<<16)|inv),"Bank panels close before stats mode "+mode);
            check(opens.contains(((root<<16)|main)+":667"),"Bank stats opens equipment main panel mode "+mode);
            check(opens.contains(((root<<16)|inv)+":670"),"Bank stats replaces bank inventory with equipment inventory mode "+mode);
            click(p,667,64,-1); // Back has no item attached.
            click(p,667,7,30000); // Invalid/stale item must not throw or unequip.
            check(p.getEquipment().get(Equipment.SLOT_CHEST).getId()==20141,"Invalid equipment click harmless");

            opens.clear();click(p,667,48,-1);
            check(p.getAttribute("inBank",false)&&!p.getAttribute("fromBank",false),"Stats Back reopens a clean bank");
            check(opens.contains(((root<<16)|main)+":762")&&opens.contains(((root<<16)|inv)+":763"),"Stats Back restores both bank panels mode "+mode);

            // Reported lifecycle: leave bank stats by walking, then use a bank again.
            click(p,762,117,-1);varbits.put(8348,1);closes.clear();WalkingHandler.reset(p);
            check(varbits.get(8348)==0,"Minimap cleanup resets client bank visibility flag");
            check(!p.getAttribute("inBank",false)&&!p.getAttribute("fromBank",false),"Walking closes bank stats state");
            check(closes.contains((root<<16)|main)&&closes.contains((root<<16)|inv),"Walking closes bank stats panels mode "+mode);
            opens.clear();p.getBank().openBank();
            check(p.getAttribute("inBank",false)&&!p.getAttribute("fromBank",false),"Bank reopens after leaving stats");
            check(opens.contains(((root<<16)|main)+":762"),"Bank main panel reopens mode "+mode);
            check(opens.contains(((root<<16)|inv)+":763"),"Bank inventory panel reopens mode "+mode);

			// Closing bank-origin stats with its own close button must also end
			// its companion inventory/return state, even when no walk follows.
            click(p,762,117,-1);closes.clear();click(p,667,74,-1);
            check(!p.getAttribute("inBank",false)&&!p.getAttribute("fromBank",false),"Stats close button clears bank state");
            check(closes.contains((root<<16)|main)&&closes.contains((root<<16)|inv),"Stats close button closes both panels mode "+mode);

            // Even if packet ordering leaves the temporary marker behind, a
            // fresh bank request must heal it instead of reopening stats.
            p.setAttribute("fromBank",true);varbits.put(8348,1);opens.clear();DialogueManager.proceedDialogue(p,617);
            check(varbits.get(8348)==0,"Fresh bank access heals client flag even if close notification was lost");
            check(p.getAttribute("inBank",false)&&!p.getAttribute("fromBank",false),"Bank access heals stale stats marker");
            check(opens.contains(((root<<16)|main)+":762"),"Stale marker cannot replace bank with stats mode "+mode);
            check(!opens.contains(((root<<16)|main)+":667"),"Bank access does not reopen stats mode "+mode);
        }
        System.out.println("PASS: "+checks+" equipment absorption, damage and stats-screen checks");
    }
}

