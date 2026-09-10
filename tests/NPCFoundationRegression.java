import java.lang.reflect.*;
import java.util.*;
import org.dementhium.cache.Cache;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.combat.impl.npc.*;
import org.dementhium.model.combat.impl.specs.Warstrike;
import org.dementhium.model.definition.*;
import org.dementhium.model.map.Region;
import org.dementhium.model.npc.*;
import org.dementhium.model.player.*;

public class NPCFoundationRegression {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static NPC npc(){NPC n=new NPC(6260);n.setLocation(Location.locate(3200,3200,0));n.setDoesWalk(false);return n;}
    static void stats()throws Exception {
        NPC n=npc(),copy=npc();NPCCombatStats s=n.getCombatStats();
        for(int skill:new int[]{Skills.ATTACK,Skills.STRENGTH,Skills.DEFENCE,Skills.RANGED,Skills.MAGIC}){
            int base=s.base(skill);check(s.get(skill)==base,"Initial base "+skill);
            int left=s.drain(skill,17),taken=Math.min(17,Math.max(0,base-1));
            check(s.get(skill)==base-taken&&left==17-taken,"Drain arithmetic "+skill);
            check(copy.getCombatLevel(skill)==base,"Independent NPC "+skill);
            check(s.base(skill)==base,"Immutable packed definition "+skill);
        }
        s.reset();int base=s.get(Skills.DEFENCE);s.drain(Skills.DEFENCE,Integer.MAX_VALUE);check(s.get(Skills.DEFENCE)==1,"Floor and overflow safety");
        for(int i=0;i<99;i++)s.tick();check(s.get(Skills.DEFENCE)==1,"Recovery waits 100 ticks");s.tick();check(s.get(Skills.DEFENCE)==2,"Recovery one level");
        for(int i=0;i<base*100;i++)s.tick();check(s.get(Skills.DEFENCE)==base,"Recovery caps at base");
        n.setAttribute("statDrainImmune",true);check(s.drain(Skills.DEFENCE,50)==50&&s.get(Skills.DEFENCE)==base,"Explicit immunity");n.removeAttribute("statDrainImmune");
        try{s.drain(Skills.DEFENCE,-1);throw new AssertionError("Negative accepted");}catch(IllegalArgumentException expected){checks++;}
        s.drain(Skills.DEFENCE,20);n.setDead(true);n.setDead(false);check(s.get(Skills.DEFENCE)==base,"New life resets levels");
        Player p=BossBatchOneRegression.player(n);double before=MeleeFormulae.getMeleeAccuracy(n,1),def=CombatFormula.defenceLevel(n);
        s.drain(Skills.ATTACK,100);s.drain(Skills.DEFENCE,100);check(MeleeFormulae.getMeleeAccuracy(n,1)<before,"Melee reads current attack");check(CombatFormula.defenceLevel(n)<def,"Defence reads current level");
        before=RangeFormulae.getAccuracy(n,1);s.drain(Skills.RANGED,100);check(RangeFormulae.getAccuracy(n,1)<before,"Range reads current level");
        n=BossBatchOneRegression.npc(2882);Method mage=MagicFormulae.class.getDeclaredMethod("getMaximumMagicAccuracy",NPC.class,double.class);mage.setAccessible(true);
        before=(Double)mage.invoke(null,n,1.0);n.getCombatStats().drain(Skills.MAGIC,100);check((Double)mage.invoke(null,n,1.0)<before,"Magic reads current level");
    }
    static void contexts()throws Exception {
        NPC n=npc();Player p=BossBatchOneRegression.player(n);NPCCombatContext old=new NPCCombatContext(n,p);
        Damage hit=Damage.getDamage(n,p,CombatType.MELEE,100);Interaction action=new Interaction(n,p);
        n.setDead(true);n.setDead(false);check(!old.isCurrent()&&!action.isNPCContextCurrent(),"Death and revival invalidate old generation");
        p.getDamageManager().damage(n,hit,CombatType.MELEE.getDamageType());check(p.getHitPoints()==1000,"Old damage rejected after revival");
        hit=Damage.getDamage(p,n,CombatType.MELEE,100);int hp=n.getHitPoints();n.resetCombatState();n.getDamageManager().damage(p,hit,CombatType.MELEE.getDamageType());check(n.getHitPoints()==hp,"Incoming old hit cannot cross reset");
        old=new NPCCombatContext(n,p);p.markInstanceTransition();check(!old.isCurrent(),"Membership transition");
        old=new NPCCombatContext(n,p);p.setOnline(false);check(!old.isCurrent(),"Offline target");p.setOnline(true);
        p.setLocation(Location.locate(3200,3200,1));check(!NPCCombatContext.validPair(n,p),"Cross-plane target");
        p.setLocation(Location.locate(3300,3300,0));check(!NPCCombatContext.validPair(n,p),"Distant target");
        n.getCombatExecutor().setVictim(p);check(n.getCombatExecutor().getVictim()==null,"Invalid target assignment rejected");
        p=BossBatchOneRegression.player(n);n.setOriginalLocation(n.getLocation());n.setLocation(Location.locate(3213,3200,0));
        n.setHp(1000);old=new NPCCombatContext(n,p);n.tick();check(n.isReturningHome()&&!old.isCurrent(),"Leash owns return generation");
        n.getCombatExecutor().setVictim(p);check(n.getCombatExecutor().getVictim()==null,"No reacquisition while returning");
        n.setLocation(n.getOriginalLocation());n.tick();check(!n.isReturningHome()&&n.getHitPoints()==n.getMaxHp(),"Arrival resets health once");
        n.setHp(500);n.tick();check(n.getHitPoints()==500,"Idle home is not repeated healing");
    }
    static void ownership()throws Exception {
        for(CombatAction prototype:new CombatAction[]{new CorporealBeastAction(),new KingBlackDragonAction(),new SaradominAction(),new ZammAction(),new ChaosElementalAction(),new ChromaticDragonAction(),new MetalDragonAction(),new FrostDragonAction(),new TormentedDemonAction()}){
            CombatAction first=prototype.newSession(),second=prototype.newSession();check(first!=prototype&&second!=first,"Independent action "+prototype.getClass().getSimpleName());
            Interaction a=new Interaction(npc(),BossBatchOneRegression.player(npc())),b=new Interaction(npc(),BossBatchOneRegression.player(npc()));first.setInteraction(a);second.setInteraction(b);check(first.getInteraction()==a&&second.getInteraction()==b,"Independent victim ownership");
        }
        TormentedDemonAction td=new TormentedDemonAction();td.setType(CombatType.MAGIC);CombatAction copy=td.newSession();td.setType(CombatType.MELEE);check(copy.getCombatType()==CombatType.MAGIC,"TD selected style captured");
        for(int id:new int[]{6260,6247,6203,50}){
            NPC boss=BossBatchOneRegression.npc(id);boss.setDoesWalk(false);Player tank=BossBatchOneRegression.player(boss);boss.getCombatExecutor().setVictim(tank);
            for(int i=0;i<50;i++){boss.tick();check(boss.getCombatExecutor().getVictim()==tank,"Tank retained "+id);}
        }
    }
    static void warstrike()throws Exception {
        NPC n=npc();Player p=BossBatchOneRegression.player(n);p.getEquipment().set(3,new Item(11696));p.getBonuses().calculate();
        int successes=0;
        for(int seed=0;seed<50;seed++){
            n.getCombatStats().reset();n.setHp(n.getMaxHp());p.getRandom().setSeed(seed);n.getRandom().setSeed(seed+5);
            Interaction it=new Interaction(p,n);new Warstrike().commenceSpecialAttack(it);int before=n.getCombatLevel(Skills.DEFENCE),hp=n.getHitPoints();
            check(n.getCombatLevel(Skills.DEFENCE)==before,"BGS waits for damage");n.getDamageManager().damage(p,it.getDamage(),CombatType.MELEE.getDamageType());int actual=hp-n.getHitPoints();
            check(n.getCombatLevel(Skills.DEFENCE)==before-actual/10,"BGS uses actual LP / 10");if(actual>0)successes++;
            int drained=n.getCombatLevel(Skills.DEFENCE);it.getDamage().finishEffects(p,n,actual);check(n.getCombatLevel(Skills.DEFENCE)==drained,"BGS effect cannot replay");
        }
        check(successes>0,"BGS positive coverage");
    }
    static void timers()throws Exception {
        Field queued=World.class.getDeclaredField("ticksToAdd");queued.setAccessible(true);
        for(Class<?> actionClass:new Class<?>[]{CorporealBeastAction.class,SaradominAction.class,ZammAction.class}){
            NPC n=npc();Player p=BossBatchOneRegression.player(n);
            Class<?> style=Class.forName(actionClass.getName()+"$Style");Object magic=null;
            for(Object e:style.getEnumConstants())if(((Enum<?>)e).name().equals("MAGIC"))magic=e;
            Field task=style.getDeclaredField("task");task.setAccessible(true);
            CombatTask special=(CombatTask)task.get(magic);
            List<org.dementhium.tickable.Tick> pending=(List<org.dementhium.tickable.Tick>)queued.get(World.getWorld());int before=pending.size();
            check(special.execute(new Interaction(n,p)),"Schedule legacy special");check(pending.size()==before+1,"One owned callback");
            org.dementhium.tickable.Tick callback=pending.get(before);n.setDead(true);n.setDead(false);int hp=p.getHitPoints();callback.execute();
            check(p.getHitPoints()==hp&&!callback.isRunning(),"Legacy special rejects revived source "+actionClass.getSimpleName());pending.remove(callback);
        }
        NPC n=npc();final int[] calls={0};Method submit=NPC.class.getDeclaredMethod("submitWorldTick",org.dementhium.tickable.Tick.class);submit.setAccessible(true);
        org.dementhium.tickable.Tick task=new org.dementhium.tickable.Tick(3){public void execute(){calls[0]++;stop();}};
        submit.invoke(n,task);List<org.dementhium.tickable.Tick> pending=(List<org.dementhium.tickable.Tick>)queued.get(World.getWorld());org.dementhium.tickable.Tick wrapper=pending.remove(pending.size()-1);
        wrapper.run();wrapper.run();check(calls[0]==0,"Owned timer retains delay");wrapper.run();check(calls[0]==1&&!wrapper.isRunning(),"Owned timer fires once");
        task=new org.dementhium.tickable.Tick(1){public void execute(){calls[0]++;}};submit.invoke(n,task);wrapper=pending.remove(pending.size()-1);n.resetCombatState();wrapper.run();check(calls[0]==1&&!task.isRunning(),"Old lifecycle callback cannot affect reset life");
    }

    public static void main(String[] args)throws Exception{
        Cache.init();ItemDefinition.init();NPCDefinition.init();Field areas=World.class.getDeclaredField("areaManager");areas.setAccessible(true);areas.set(World.getWorld(),new org.dementhium.content.areas.AreaManager());Method load=NPCLoader.class.getDeclaredMethod("loadCustomizations");load.setAccessible(true);load.invoke(null);
        Region r=Region.forCoords(3200,3200);r.clippingMasks=new int[4][128][128];r.setClipped(true);
        stats();contexts();ownership();warstrike();timers();System.out.println("NPC foundation: "+checks+" checks passed.");
    }
}
