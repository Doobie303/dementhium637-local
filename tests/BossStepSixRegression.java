import java.lang.reflect.Field;
import java.util.Arrays;
import org.dementhium.content.activity.impl.BarrowsActivity;
import org.dementhium.content.activity.impl.barrows.BarrowsCrypt;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.map.Region;
import org.dementhium.model.npc.impl.*;
import org.dementhium.model.player.*;

/** Isolated real-definition combat checks; no running server or account writes. */
public final class BossStepSixRegression {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static void set(Object object,String name,Object value)throws Exception{
        Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);
    }
    static boolean[][] prayers(Player p)throws Exception{
        Field f=p.getPrayer().getClass().getDeclaredField("onPrayers");f.setAccessible(true);return (boolean[][])f.get(p.getPrayer());
    }
    static void clearPrayer(Player p)throws Exception{for(boolean[] book:prayers(p))Arrays.fill(book,false);}
    static CombatAction launch(TzTokJad jad,Player p)throws Exception{
        CombatAction a=jad.getCombatAction().newSession();
        a.setInteraction(new Interaction(jad,p));a.execute();
        check(a.getInteraction().getDamage()!=null,"Jad launches valid attack");
        set(a,"rolledHit",400);
        return a;
    }
    static void finish(CombatAction a){for(int i=0;i<5;i++)a.execute();}
    static void jad()throws Exception{
        TzTokJad jad=new TzTokJad(2745);jad.setLocation(Location.locate(3200,3200,0));
        Player p=CombatFixtures.player(jad);
        Location remote=jad.getLocation().transform(10,0,0), adjacent=jad.getLocation().transform(jad.size(),0,0);
        int ranged=0,magic=0;
        for(int i=0;i<80;i++){
            p.setLocation(remote);p.getSkills().setHitPoints(1000);clearPrayer(p);
            CombatAction a=launch(jad,p);CombatType style=a.getCombatType();
            if(style==CombatType.RANGE)ranged++;else if(style==CombatType.MAGIC)magic++;
            check(style!=CombatType.MELEE,"remote Jad cannot melee");
            p.setLocation(adjacent);
            for(int tick=1;tick<=4;tick++){
                a.execute();check(p.getHitPoints()==1000,"entering melee range cannot shorten tell tick "+tick);
                check(a.getCombatType()==style,"pending style is immutable");
            }
            if(i%2==0)prayers(p)[0][style.getProtectionPrayer()]=true;
            a.execute();check(p.getHitPoints()==(i%2==0?1000:600),"prayer read on impact after tell");
            int hp=p.getHitPoints();a.endSession();check(p.getHitPoints()==hp,"Jad impact cannot replay");
        }
        check(ranged>0&&magic>0,"both remote tells covered");
        clearPrayer(p);p.setLocation(remote);p.getSkills().setHitPoints(1000);
        CombatAction first=launch(jad,p),second=launch(jad,p);
        check(first!=second,"queued Jad sessions are independent");
        set(first,"rolledHit",100);set(second,"rolledHit",200);
        finish(second);finish(first);check(p.getHitPoints()==700,"overlapping rolls cannot overwrite each other");
        for(int mode=0;mode<4;mode++){
            p.setLocation(remote);p.setOnline(true);p.getSkills().setHitPoints(1000);
            CombatAction a=launch(jad,p);
            if(mode==0)jad.resetCombatState();
            if(mode==1)p.setOnline(false);
            if(mode==2)p.setLocation(remote.transform(0,0,1));
            if(mode==3)p.setAttribute("godmode",true);
            finish(a);check(p.getHitPoints()==1000,"cancel/immunity boundary "+mode);
            p.removeAttribute("godmode");
        }
        p.setOnline(true);p.setLocation(remote);p.getSkills().setHitPoints(1000);
        CombatAction miss=launch(jad,p);set(miss,"rolledHit",-1);finish(miss);
        check(p.getHitPoints()==1000,"magic miss remains a miss");
        Region region=Region.forCoords(3200,3200);
        for(int[] row:region.clippingMasks[0])Arrays.fill(row,-1);
        CombatAction blocked=jad.getCombatAction().newSession();blocked.setInteraction(new Interaction(jad,p));
        check(!blocked.commenceSession(),"blocked projectile path prevents launch");
        for(int[] row:region.clippingMasks[0])Arrays.fill(row,0);
        p.setLocation(adjacent);CombatAction melee=launch(jad,p);
        check(melee.getCombatType()==CombatType.MELEE,"adjacent melee selection retained");
        melee.execute();check(p.getHitPoints()==600,"melee resolves next combat tick");
        CombatFixtures.clearPlayers();
    }
    static CombatAction brother(BarrowBrother b,Player p,int roll,int debuff)throws Exception{
        CombatAction a=b.getCombatAction().newSession();a.setInteraction(new Interaction(b,p));
        check(a.commenceSession(),"brother launches "+b.getId());
        set(a,"roll",roll);set(a,"debuff",debuff);set(a,"proc",true);return a;
    }
    static void barrows()throws Exception{
        TzTokJad anchor=new TzTokJad(2745);anchor.setLocation(Location.locate(3200,3200,0));
        Player p=CombatFixtures.player(anchor);
        BarrowsActivity activity=new BarrowsActivity(p);activity.initializeActivity();
        for(BarrowsCrypt crypt:activity.getEntities()){
            BarrowBrother b=(BarrowBrother)crypt.getNPC();
            b.setAttribute("barrowsOwner",p);b.setAttribute("isSpawned",true);b.setLocation(p.getLocation());b.setHp(500);
            for(int mode=0;mode<3;mode++){
                p.getSkills().setHitPoints(1000);p.getSkills().setLevel(Skills.STRENGTH,99);
                p.getSkills().setLevel(Skills.AGILITY,99);p.getWalkingQueue().setRunEnergy(100);
                if(mode==0)p.setAttribute("hitImmunity",World.getTicks()+100);
                if(mode==1)p.setAttribute("godmode",true);
                CombatAction a=brother(b,p,100,-1);a.endSession();
                check(p.getHitPoints()==(mode==2?900:1000),"brother damage eligibility "+b.getId());
                if(mode<2)check(p.getSkills().getLevel(Skills.STRENGTH)==99&&p.getSkills().getLevel(Skills.AGILITY)==99
                    &&p.getWalkingQueue().getRunEnergy()==100,"immunity suppresses all harmful brother effects");
                int hp=p.getHitPoints(),strength=p.getSkills().getLevel(Skills.STRENGTH),agility=p.getSkills().getLevel(Skills.AGILITY),bhp=b.getHp();
                double energy=p.getWalkingQueue().getRunEnergy();a.endSession();
                check(p.getHitPoints()==hp&&b.getHp()==bhp&&p.getSkills().getLevel(Skills.STRENGTH)==strength
                    &&p.getSkills().getLevel(Skills.AGILITY)==agility&&p.getWalkingQueue().getRunEnergy()==energy,"brother cannot replay hit or effects");
                p.removeAttribute("hitImmunity");p.removeAttribute("godmode");
            }
            p.getSkills().setHitPoints(1000);
            CombatAction old=brother(b,p,100,-1);b.resetCombatState();old.endSession();
            check(p.getHitPoints()==1000,"reset cancels old brother impact");
            CombatAction one=brother(b,p,100,-1),two=brother(b,p,200,-1);
            check(one!=two,"independent brother sessions");two.endSession();one.endSession();
            check(p.getHitPoints()==700,"brother overlapping rolls preserved");
            if(b.getId()==2025){
                p.getSkills().setLevel(Skills.ATTACK,99);p.setAttribute("hitImmunity",World.getTicks()+100);
                brother(b,p,100,Skills.ATTACK).endSession();check(p.getSkills().getLevel(Skills.ATTACK)==99,"Ahrim stat spell respects hit immunity");
                p.removeAttribute("hitImmunity");
                brother(b,p,0,Skills.ATTACK).endSession();check(p.getSkills().getLevel(Skills.ATTACK)<99,"successful zero-damage stat spell can drain");
                p.getSkills().setLevel(Skills.ATTACK,99);
                brother(b,p,-1,Skills.ATTACK).endSession();check(p.getSkills().getLevel(Skills.ATTACK)==99,"splash cannot drain");
            }
        }
        CombatFixtures.clearPlayers();
    }
    public static void main(String[] args)throws Exception{
        CombatFixtures.init();jad();barrows();System.out.println("Step six: "+checks+" checks passed");
    }
}
