import java.util.*;
import org.dementhium.cache.Cache;
import org.dementhium.content.Commands;
import org.dementhium.content.skills.summoning.*;
import org.dementhium.model.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.player.*;
import org.dementhium.net.GameSession;
public class TestGearRegression {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    static int amount(Player p,int id){
        int slot=p.getBank().getContainer().indexOf(new Item(id));
        return slot<0?0:p.getBank().getContainer().get(slot).getAmount();
    }
    public static void main(String[] args)throws Exception{
        Cache.init();ItemDefinition.init();
        Player p=new Player(new GameSession(null),new PlayerDefinition("testgear","unused"));
        p.getBank().commandAdd(1511,77,0);
        Commands.playerCommands(p,new String[]{"testgear"});
        for(SummoningPouch v:SummoningPouch.values())check(amount(p,v.getPouchId())==1000000,"Missing pouch "+v);
        for(SummoningScroll v:SummoningScroll.values())check(amount(p,v.getItemId())==1000000,"Missing scroll "+v);
        for(int id:new int[]{952,2550,4740,12140,12155,12183,12158,12159,12160,12163,15304,564,561})
            check(amount(p,id)==1000000,"Missing utility "+id);
        check(amount(p,1511)==77,"Existing bank items preserved");
        int size=p.getBank().getContainer().size();
        int[] tabs=p.getBank().getTab().clone();
        Commands.playerCommands(p,new String[]{"gearbank"});
        check(p.getBank().getContainer().size()==size,"Repeat must not duplicate bank slots");
        check(Arrays.equals(tabs,p.getBank().getTab()),"Repeat must preserve tab boundaries");
        check(amount(p,12093)==2000000,"Pack yak restocked");
        // A full bank must never shift or overwrite an existing slot to insert new gear.
        Player full=new Player(new GameSession(null),new PlayerDefinition("fullbank","unused"));
        for(int i=0;i<Bank.SIZE;i++)full.getBank().getContainer().set(i,new Item(1511,77));
        int[] fullTabs=full.getBank().getTab().clone();
        Commands.playerCommands(full,new String[]{"testgear"});
        for(int i=0;i<Bank.SIZE;i++)check(full.getBank().getContainer().get(i).getId()==1511
                &&full.getBank().getContainer().get(i).getAmount()==77,"Full bank changed slot "+i);
        check(Arrays.equals(fullTabs,full.getBank().getTab()),"Full bank tab boundaries changed");
        System.out.println("PASS: "+size+" bank slots, "+SummoningPouch.values().length+" pouches, "
            +SummoningScroll.values().length+" scrolls; restocking, aliases and full-bank preservation");
        for(int id:new int[]{12093,12039,12790,12007,12140,15304})
            System.out.println(id+" "+ItemDefinition.forId(id).getName());
    }
}