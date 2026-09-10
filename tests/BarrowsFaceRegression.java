import java.lang.reflect.*;
import java.util.*;
import org.jboss.netty.channel.*;
import org.dementhium.cache.Cache;
import org.dementhium.model.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.player.Player;
import org.dementhium.net.*;
import org.dementhium.net.message.Message;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.content.activity.impl.BarrowsActivity;
import org.dementhium.tickable.impl.PlayerAreaTick;

public class BarrowsFaceRegression {
    static int tick;
    static List<Integer> starts=new ArrayList<Integer>(), clears=new ArrayList<Integer>();
    static void require(boolean b,String text){if(!b)throw new AssertionError(text);}
    static void set(Object o,String name,Object v)throws Exception{
        Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,v);
    }
    public static void main(String[] args)throws Exception{
        Cache.init();NPCDefinition.init();
        set(World.getWorld(),"areaManager",new AreaManager());
        Channel channel=(Channel)Proxy.newProxyInstance(Channel.class.getClassLoader(),new Class[]{Channel.class},
            (proxy,method,a)->{
                if(method.getName().equals("isConnected")||method.getName().equals("isOpen"))return true;
                if(method.getName().equals("write")){
                    if(a[0] instanceof Message){
                        Message m=(Message)a[0];
                        if(m.getOpcode()==5&&m.readLEShortA()==1043)starts.add(tick);
                        if(m.getOpcode()==51&&m.readLEShort()==1043&&m.readByte()==-1)clears.add(tick);
                    }
                    return null;
                }
                if(method.getReturnType()==boolean.class)return false;
                if(method.getReturnType()==int.class)return 0;
                return null;
            });
        Player p=new Player(new GameSession(channel),new PlayerDefinition("face-test","unused"));
        p.setOnline(true);p.setLocation(Location.locate(3556,9716,3));
        p.getSettings().getBarrowsKilled().add(2025);
        p.getSkills().setPrayerPoints(99,false);
        BarrowsActivity activity=new BarrowsActivity(p);activity.initializeActivity();
        PlayerAreaTick area=new PlayerAreaTick(p);
        for(tick=1;tick<=95;tick++)area.execute();
        require(starts.size()==3,"Expected three haunts, got "+starts);
        require(starts.get(1)-starts.get(0)==30&&starts.get(2)-starts.get(1)==30,"18-second interval "+starts);
        require(clears.contains(starts.get(0)+5)&&clears.contains(starts.get(1)+5),"Faces must clear after 3 seconds "+clears);
        require(p.getSkills().getPrayerPoints()==72,"Three nine-point drains");
        int before=starts.size();
        activity.updateOverlay();activity.updateOverlay();
        require(starts.size()==before,"Door/kill refresh must not replay a face");
        require(clears.get(clears.size()-1)==tick,"Overlay refresh clears stale face");
        p.setLocation(Location.locate(3554,3296,0));
        area.execute();
        p.setLocation(Location.locate(3556,9716,3));
        for(int n=0;n<20;n++){tick++;area.execute();}
        require(starts.size()==before,"Fresh underground visit cannot immediately haunt");
        System.out.println("PASS: 30-tick haunt interval, 5-tick face expiry, overlay replay prevention and surface reset");
    }
}