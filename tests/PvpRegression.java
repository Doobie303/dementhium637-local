import java.lang.reflect.*;
import java.util.*;
import org.dementhium.cache.Cache;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.content.interfaces.ItemsKeptOnDeath;
import org.dementhium.content.misc.*;
import org.dementhium.content.skills.Prayer;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.combat.impl.spells.modern.Teleblock;
import org.dementhium.model.definition.*;
import org.dementhium.model.misc.*;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.player.*;
import org.dementhium.net.*;

public class PvpRegression {
    static int checks;
    static int sequence;
    static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    static void ticks(int n)throws Exception{Field f=World.class.getDeclaredField("ticksPassed");f.setAccessible(true);f.setInt(null,n);}
    static Player player(){
        Player p=InstanceIntegrationRegression.player("pvp"+(++sequence));
        p.setLocation(Location.locate(3100,3550,0));
        p.getSkills().setMaximumLifePoints(1000);p.getSkills().setHitPoints(1000);
        p.targetLikelihood=60;
        World.getWorld().getPlayers().add(p);
        return p;
    }
    static Map<Integer,Long> quantities(Container...containers){
        Map<Integer,Long> result=new TreeMap<Integer,Long>();
        for(Container c:containers)for(Item i:c.toArray())if(i!=null){check(i.getAmount()>0,"positive amount");result.put(i.getId(),result.getOrDefault(i.getId(),0L)+i.getAmount());}
        return result;
    }
    static void bulk(){
        Container source=new Container(3,false);source.set(0,new Item(995,10000));source.set(1,new Item(892,100));source.set(2,new Item(4151));
        Container target=new Container(3,false);target.addAll(source);
        check(quantities(source).equals(quantities(target)),"bulk copies coins, ammunition and equipment");
        target.get(0).setAmount(1);check(source.get(0).getAmount()==10000,"bulk copy does not alias input");
        target=new Container(1,false);target.set(0,new Item(995,10));Map<Integer,Long> before=quantities(target);
        check(!target.tryAddAll(source),"full transfer rejected");check(before.equals(quantities(target)),"failed transfer atomic");
        Container max=new Container(1,false);max.set(0,new Item(995,Integer.MAX_VALUE));
        target=new Container(1,false);target.set(0,new Item(995,10));check(!target.tryAddAll(max),"overflow rejected");check(target.get(0).getAmount()==10,"overflow preserves destination");
        target=new Container(2,false,false,true);target.set(0,new Item(995,10));check(target.tryAddAll(max),"explicit split stack overflow supported");
        check(quantities(target).get(995)==Integer.MAX_VALUE+10L,"split conserved");
        target=new Container(1,false,false,true);target.set(0,new Item(995,10));check(!target.tryAddAll(max)&&target.get(0).getAmount()==10,"failed split cannot mutate existing stack");
        source=new Container(1,false);Item used=new Item(4710);used.setHealth(123);source.set(0,used);
        target=new Container(1,false);target.addAll(source);check(target.get(0).getHealth()==123,"bulk preserves equipment degradation");
    }
    static void deathItems()throws Exception{
        Player p=player();
        for(int i=0;i<28;i++)p.getInventory().getContainer().set(i,new Item(i%2==0?4151:1127));
        for(int i=0;i<14;i++)p.getEquipment().getContainer().set(i,new Item(11732));
        p.getInventory().getContainer().set(0,new Item(995,Integer.MAX_VALUE));
        p.getInventory().getContainer().set(1,new Item(892,Integer.MAX_VALUE));
        p.getEquipment().getContainer().set(13,new Item(892,Integer.MAX_VALUE));
        Map<Integer,Long> before=quantities(p.getInventory().getContainer(),p.getEquipment().getContainer());
        for(int skull=0;skull<2;skull++)for(int protect=0;protect<2;protect++){
            p.getSkullManager().setTicks(skull==1?World.getTicks()+3333:-1);
            CombatBalanceRegression.prayers(p)[0][Prayer.PROTECT_ITEM]=protect==1;
            Container[] result=ItemsKeptOnDeath.getDeathContainers(p);
            check(quantities(result).equals(before),"all 42 physical slots conserved at skull/protect "+skull+"/"+protect);
            check(result[0].size()==(skull==1?0:3)+protect,"correct protected count");
            check(before.equals(quantities(p.getInventory().getContainer(),p.getEquipment().getContainer())),"death preview leaves source unchanged");
        }
        check(ItemsKeptOnDeath.getCarriedWealth(p)==Integer.MAX_VALUE,"wealth saturates without integer overflow");
        p.getInventory().getContainer().clear();p.getEquipment().getContainer().clear();
        Item used=new Item(4710);used.setHealth(123);p.getEquipment().set(0,used);
        for(int skull=0;skull<2;skull++){
            p.getSkullManager().setTicks(skull==1?World.getTicks()+3333:-1);
            CombatBalanceRegression.prayers(p)[0][Prayer.PROTECT_ITEM]=false;
            Container[] result=ItemsKeptOnDeath.getDeathContainers(p);
            Item preserved=(skull==1?result[1]:result[0]).get(0);
            check(preserved.getHealth()==123&&used.getHealth()==123,"death selection preserves equipment charges");
        }
        Random random=new Random(637);
        int[] ids={995,892,4151,1127,11732,1712};
        for(int run=0;run<200;run++){
            p.getInventory().getContainer().clear();p.getEquipment().getContainer().clear();
            for(Container c:new Container[]{p.getInventory().getContainer(),p.getEquipment().getContainer()})for(int i=0;i<c.getSize();i++)if(random.nextBoolean()){
                int id=ids[random.nextInt(ids.length)];c.set(i,new Item(id,ItemDefinition.forId(id).isStackable()?1+random.nextInt(100000):1));
            }
            check(quantities(p.getInventory().getContainer(),p.getEquipment().getContainer()).equals(quantities(ItemsKeptOnDeath.getDeathContainers(p))),"random conservation "+run);
        }
    }
    static void targets(){
        Player a=player(),b=player(),c=player();
        Player accrual=player();accrual.targetLikelihood=5;
        for(int n=0;n<PvpSystem.EP_INTERVAL-1;n++)accrual.getPlayerArea().execute();
        check(accrual.pvpZoneEp==0,"EP waits full custom interval");accrual.getPlayerArea().execute();
        check(accrual.pvpZoneEp==2&&accrual.targetLikelihood==10,"real area tick retains fast EP and likelihood gain");
        accrual.setLocation(Location.locate(3100,3495,0));accrual.getPlayerArea().execute();
        check(accrual.pvpZoneEp==2&&accrual.targetLikelihood==5,"custom safe departure retains EP and resets likelihood");
        check(PvpSystem.pair(a,b)&&a.target==b&&b.target==a,"mutual assignment");
        check(!PvpSystem.pair(c,b),"third player cannot steal assigned target");
        PvpSystem.depart(a);check(a.target==null&&b.target==null&&!a.hasTargetArrow&&!b.hasTargetArrow,"both arrows/references removed");
        check(a.targetLikelihood==5&&b.targetLikelihood==60,"only departing player loses progress");
        a.targetLikelihood=60;b.getSkills().setLevelAndXP(Skills.ATTACK,99,13034431);check(!PvpSystem.canPair(a,b),"combat mismatch excluded");
        b.getSkills().setLevelAndXP(Skills.ATTACK,1,0);b.setLocation(Location.locate(3100,3550,1));check(!PvpSystem.canPair(a,b),"plane mismatch excluded");
        b.setLocation(a.getLocation());b.targetLikelihood=5;check(!PvpSystem.canPair(a,b),"unready candidate excluded");b.targetLikelihood=60;
        check(PvpSystem.pair(a,b),"re-pair eligible opponents");InstanceIntegrationRegression.connections.get(b).set(false);PvpSystem.tick(a);
        check(a.target==null&&b.target==null,"disconnect clears reciprocal assignment");
        a=player();b=player();check(PvpSystem.pair(a,b),"pair before death");PvpSystem.beginDeath(b);
        check(a.target==null&&b.target==null&&a.getUsername().equals(b.getAttribute("pvpDeathTarget")),"death freezes successful target identity before clearing");
        a=player();int lvl=a.getSkills().getCombatLevelWithoutSummoning();int depth=a.getLocation().getWildernessLevel();
        check(Arrays.equals(a.getPlayerArea().getPVPZoneCombatRange(),new int[]{Math.max(3,lvl-depth),Math.min(138,lvl+depth)}),"UI shares wilderness rule");
        for(int n=0;n<100;n++){
            b=player();b.getSkills().setLevelAndXP(Skills.ATTACK,n+1,b.getSkills().getXPForLevel(n+1));
            int other=b.getSkills().getCombatLevelWithoutSummoning();int[] range=PvpSystem.combatRange(a);
            check(PvpSystem.inCombatRange(a,b)==(other>=range[0]&&other<=range[1]),"display matches attack range "+n);
        }
    }
    static void rewards()throws Exception{
        ticks(1000);Player a=player(),b=player();a.pvpZoneEp=100;
        b.getInventory().getContainer().set(0,new Item(995,20000));b.getInventory().getContainer().set(1,new Item(892,50));b.getSkullManager().setTicks(4000);
        int start=GroundItemManager.getGroundItems().size();
        // B does not retaliate. A's own damage history is empty, as in the old crash case.
        b.getDamageManager().addEnemyHit(a,100);PvpSystem.beginDeath(b);
        GraveStoneManager.appendDeath(b,a);
        List<GroundItem> drops=GroundItemManager.getGroundItems().subList(start,GroundItemManager.getGroundItems().size());
        check(drops.size()==3,"two carried stacks plus one 100-EP bonus");
        check(drops.stream().anyMatch(d->d.getItem().getId()==995&&d.getItem().getAmount()==20000),"actual coins dropped");
        check(drops.stream().anyMatch(d->d.getItem().getId()==892&&d.getItem().getAmount()==50),"actual ammunition dropped");
        for(GroundItem d:drops)check(d.getPlayer()==a&&d.getLocation().equals(b.getLocation()),"explicit killer and death location");
        check(a.pvpZoneEp==0&&a.getPkKills()==1&&b.getPkDeaths()==1&&a.getPkPoints()==1,"correct EP debit, stats and points");
        int count=GroundItemManager.getGroundItems().size();GraveStoneManager.appendDeath(b,a);check(count==GroundItemManager.getGroundItems().size()&&a.getPkKills()==1,"repeated death callback settles once");
        PvpSystem.beginDeath(b);b.getInventory().getContainer().set(0,new Item(995,33));a.pvpZoneEp=100;GraveStoneManager.appendDeath(b,a);
        check(GroundItemManager.getGroundItems().size()==count+1&&a.pvpZoneEp==100&&a.getPkPoints()==1,"repeat kill keeps ordinary loot but no extra rewards or EP debit");
        a=player();b=player();a.pvpZoneEp=100;check(PvpSystem.pair(a,b),"target reward fixture");PvpSystem.beginDeath(b);
        start=GroundItemManager.getGroundItems().size();a.handlePkStatistics(b,null);
        check(GroundItemManager.getGroundItems().size()==start+2,"100 EP target gets base plus extra roll");check(a.targetLikelihood==5,"completed target starts fresh wait");
        a=player();b=player();Player last=player();a.pvpZoneEp=100;last.pvpZoneEp=88;PvpSystem.beginDeath(b);a.handlePkStatistics(b,last);
        check(a.getPkPoints()==1&&last.getPkPoints()==2&&last.pvpZoneEp==88,"multi assist points preserve unrelated assist EP");
        check(PvpSystem.points(0,false,true,true)==3,"donor assist rounds 30 percent bonus up");
        check(PvpSystem.points(0,false,true,false)==2,"main donor x2 preserved");
        check(PvpSystem.points(0,true,false,false)==11&&PvpSystem.points(0,true,false,true)==7,"staff bonuses preserved");
        check(PvpSystem.points(200000000,false,false,false)==3,"custom self-risk bonus preserved");
        a=player();b=player();a.addPkPoints(Integer.MAX_VALUE);a.pvpZoneEp=100;PvpSystem.beginDeath(b);start=GroundItemManager.getGroundItems().size();a.handlePkStatistics(b,null);
        check(a.getPkPoints()==Integer.MAX_VALUE&&GroundItemManager.getGroundItems().size()==start+1,"point cap does not disable EP loot");
        for(int ep=-2;ep<=102;ep++)check(PvpSystem.dropChance(ep)==10+90*PvpSystem.clampEp(ep)/100,"EP band continuous "+ep);
        a=player();b=player();check(PvpSystem.rewardEligible(a,b)&&!PvpSystem.rewardEligible(b,a),"repeat guard symmetric");ticks(1500);check(PvpSystem.rewardEligible(b,a),"repeat guard expires at boundary");
    }
    static void creditAndSkulls()throws Exception{
        ticks(2000);Player a=player(),b=player(),v=player();
        v.getDamageManager().addEnemyHit(a,500);ticks(2101);v.getDamageManager().addEnemyHit(b,10);check(v.getDamageManager().getKiller()==b,"old encounter expires");
        v=player();v.getDamageManager().addEnemyHit(a,500);ticks(2202);v.getSkills().setHitPoints(1);v.getDamageManager().miscDamage(1,DamageType.RED_DAMAGE);
        check(v.getAttribute("pvpDeathKiller")==null,"idle old attacker cannot claim later environmental death");
        v=player();v.getSkills().setHitPoints(10);v.getDamageManager().damage(a,new Damage(100),DamageType.MELEE);
        check(v.getAttribute("pvpDeathKiller")==a,"lethal damage credited before synchronous death snapshot");
        check(v.getDamageManager().getEnemyHits().get(a)==10,"lethal credit is HP-capped and not doubled");
        Player owner=player();v=player();
        org.dementhium.content.skills.summoning.Familiar familiar=new org.dementhium.content.skills.summoning.Familiar(owner,6806,100);
        owner.setFamiliar(familiar);v.getDamageManager().addEnemyHit(owner,10);v.getDamageManager().addEnemyHit(familiar,20);v.getDamageManager().addEnemyHit(b,25);
        check(v.getDamageManager().getKiller()==owner,"shared owner/familiar credit lookup cannot null-unbox");
        v=player();owner.pvpZoneEp=100;v.getDamageManager().addEnemyHit(familiar,20);PvpSystem.beginDeath(v);
        int drops=GroundItemManager.getGroundItems().size();GraveStoneManager.appendDeath(v,familiar);
        check(owner.getPkKills()==1&&GroundItemManager.getGroundItems().size()==drops+1,"shared death routing normalizes familiar owner without last-attacker pointer");
        a=player();b=player();Player c=player();a.getSkullManager().appendSkull(b);a.getSkullManager().appendSkull(c);
        b.getSkullManager().appendSkull(a);check(!b.getSkullManager().isSkulled(),"retaliation stays unskulled");
        a.getSkullManager().setTicks(World.getTicks());check(!a.getSkullManager().isSkulled(),"expiry exact boundary");
        check(b.getSkullManager().getAttackers().isEmpty()&&c.getSkullManager().getAttackers().isEmpty(),"expiry clears all cross references");
        a.getSkullManager().getVictims().add(null);a.getSkullManager().removeSkull();check(!a.getSkullManager().isSkulled(),"null-safe skull cleanup");
    }
    static void teleblock()throws Exception{
        ticks(3000);Player a=player(),b=player();Teleblock spell=new Teleblock();Interaction i=new Interaction(a,b);i.setSpell(spell);spell.castSpell(i);
        check(b.getAttribute("teleblock",0)==0,"Teleblock not applied on cast");i.setDamage(new Damage(0));spell.endSpell(i);
        check(b.getAttribute("teleblock",0)==3550&&b.getAttribute("teleblockImmunity",0)==3550,"successful zero hit blocks for preserved duration");
        ticks(3500);spell.endSpell(i);check(b.getAttribute("teleblock",0)==3550,"recast cannot extend active block");
        b=player();i=new Interaction(a,b);i.setSpell(spell);i.setDamage(new Damage(-1));spell.endSpell(i);check(b.getAttribute("teleblock",0)==0,"splash never blocks");
        b=player();b.setAttribute("godmode",true);i=new Interaction(a,b);i.setSpell(spell);i.setDamage(new Damage(0));i.setEndGraphic(org.dementhium.model.mask.Graphic.create(1843));spell.endSpell(i);
        check(b.getAttribute("teleblock",0)==0,"godmode immune");
    }
    @SuppressWarnings("unchecked")
    static void deathCompletion()throws Exception {
        Field field=World.class.getDeclaredField("ticksToAdd");field.setAccessible(true);
        List<org.dementhium.tickable.Tick> pending=(List<org.dementhium.tickable.Tick>)field.get(World.getWorld());pending.clear();
        Player failing=player();final boolean[] fault={false};
        Field random=Mob.class.getDeclaredField("random");random.setAccessible(true);
        random.set(failing,new Random(637){@Override public int nextInt(int bound){
            if(bound==100){fault[0]=true;throw new IllegalStateException("Injected optional reward failure");}return super.nextInt(bound);
        }});
        Player victim=player();victim.getSkills().setHitPoints(10);victim.getSkullManager().setTicks(World.getTicks()+3333);
        CombatBalanceRegression.prayers(victim)[0][Prayer.PROTECT_ITEM]=true;
        victim.getInventory().getContainer().set(0,new Item(995,50));
        victim.getDamageManager().damage(failing,new Damage(50),DamageType.MELEE);
        for(org.dementhium.tickable.Tick task:new ArrayList<org.dementhium.tickable.Tick>(pending))task.run();pending.clear();
        check(victim.getTick("death_tick")!=null,"real death callback scheduled");
        for(int n=0;n<4;n++)victim.processTicks();
        check(fault[0],"optional reward failure was exercised");
        check(!victim.isDead()&&!victim.isInWilderness(),"optional reward failure does not block respawn");
        check(!victim.getSkullManager().isSkulled(),"optional reward failure does not block skull cleanup");
        check(!victim.getPrayer().usingPrayer(0,Prayer.PROTECT_ITEM),"optional reward failure does not block prayer cleanup");
        check(victim.getDamageManager().getEnemyHits().isEmpty(),"optional reward failure does not retain damage credit");
        check(victim.getAttribute("pvpDeathKiller")==null,"death context releases player references");
        int count=GroundItemManager.getGroundItems().size();GraveStoneManager.appendDeath(victim,failing);
        check(count==GroundItemManager.getGroundItems().size(),"completion retains duplicate-death guard");
    }
    public static void main(String[] args)throws Exception{
        Cache.init();org.dementhium.util.MapXTEA.loadPackedFile();ItemDefinition.init();NPCDefinition.init();GroundItemManager.load();
        Field areas=World.class.getDeclaredField("areaManager");areas.setAccessible(true);areas.set(World.getWorld(),new AreaManager());
        bulk();deathItems();targets();rewards();creditAndSkulls();teleblock();deathCompletion();
        System.out.println("PvpRegression: "+checks+" checks passed");
    }
}
