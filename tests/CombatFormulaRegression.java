import java.lang.reflect.*;
import java.util.*;
import org.dementhium.cache.Cache;
import org.dementhium.content.skills.Prayer;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.combat.impl.spells.modern.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.*;
import org.dementhium.net.GameSession;

public class CombatFormulaRegression {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static Player player(){Player p=new Player(new GameSession(null),new PlayerDefinition("formula-test","unused"));
        for(int skill:new int[]{Skills.ATTACK,Skills.STRENGTH,Skills.DEFENCE,Skills.RANGED,Skills.MAGIC})p.getSkills().setLevelAndXP(skill,99,13034431);
        p.getSettings().setCombatStyle(WeaponInterface.STYLE_RAPID);p.getSettings().setCombatType(0);p.getBonuses().calculate();return p;}
    static void gear(Player p,int slot,int id){p.getEquipment().set(slot,id<0?null:new Item(id));p.getBonuses().calculate();}
    static double magicAttack(Player p,MagicSpell spell)throws Exception {Method m=MagicFormulae.class.getDeclaredMethod("getMaximumAccuracy",Player.class,MagicSpell.class);m.setAccessible(true);return (Double)m.invoke(null,p,spell);}
    static double magicDefence(Player p,Mob victim)throws Exception {Method m=MagicFormulae.class.getDeclaredMethod("getMaximumDefence",Player.class,Mob.class,MagicSpell.class);m.setAccessible(true);return (Double)m.invoke(null,p,victim,new FireWave());}
    static void voidGear(Player p,int helm,int top,int legs){gear(p,0,helm);gear(p,4,top);gear(p,7,legs);gear(p,9,8842);}
    static double chance(double a,double d){return a>d?1-(d+2)/(2*(a+1)):a/(2*(d+1));}
    public static void main(String[] args)throws Exception {
        Cache.init();ItemDefinition.init();NPCDefinition.init();
        check(CombatFormula.effectiveLevel(99,0.23,3,1)==132,"Piety strength floors 121.77 before stance");
        check(CombatFormula.maximumHit(107,31,1)==163,"Published 99 strength / +31 benchmark: 163 LP");
        check(CombatFormula.maximumHit(110,31,1)==168,"Aggressive +3 strength: 168 LP");
        check(CombatFormula.maximumHit(132,31,1)==200,"Piety aggressive: 200 LP");
        check(CombatFormula.maximumHit(156,31,1)==236,"118 boosted strength / Piety / aggressive: 236 LP");
        check(CombatFormula.maximumHit(132,31,1.25)==251,"Special multiplies untruncated base then floors");
        check(CombatFormula.accuracyRoll(107,31,1)==1588,"Accuracy separate from damage intercept");
        check(CombatFormula.accuracyRoll(107,-64,1)==0&&CombatFormula.accuracyRoll(107,-100,1)==0,"Negative equipment cannot yield negative accuracy");
        check(CombatFormula.maximumHit(107,31,0)==0,"Zero multiplier");
        check(CombatFormula.effectiveLevel(100,.15,0,1)==123,"Decimal precision does not drop an exact level");
        Player p=player(),v=player();
        // A dedicated in-memory item fixture separates formula expectations from unaudited packed bonuses.
        int id=1127;int[] original=ItemDefinition.forId(id).getBonus().clone();
        try {
            Arrays.fill(ItemDefinition.forId(id).getBonus(),0);
            ItemDefinition.forId(id).getBonus()[Bonuses.STRENGTH]=31;
            gear(p,4,id);check(MeleeFormulae.getMeleeDamage(p,1)==163,"Melee integration published benchmark");
            p.getPrayer().modify(Prayer.PIETY,true);p.getSettings().setCombatStyle(WeaponInterface.STYLE_AGGRESSIVE);
            check(MeleeFormulae.getMeleeDamage(p,1)==200,"Piety uses current strength");
            p.getSkills().setLevel(Skills.STRENGTH,118);check(MeleeFormulae.getMeleeDamage(p,1)==236,"Potion applies before prayer");
            check(p.getPrayer().getDefenceModifier()==.25,"Piety restores defence bonus");
            p.getPrayer().modify(Prayer.PIETY,false);check(p.getPrayer().getDefenceModifier()==0,"Piety toggle does not leak defence");
            p.getPrayer().modify(Prayer.CHIVALRY,true);check(p.getPrayer().getDefenceModifier()==.20,"Chivalry defence");p.getPrayer().modify(Prayer.CHIVALRY,false);
            p=player();v=player();
            check(magicAttack(p,new FireStrike())==magicAttack(p,new FireWave()),"Spell damage does not inflate accuracy");
            check(magicDefence(p,v)==1010,"70/30 player magic defence, separate floor stages");
            v.getPrayer().modify(Prayer.PIETY,true);check(magicDefence(p,v)==1080,"Defence prayer affects only 30% contribution");
            v.getPrayer().modify(Prayer.PIETY,false);
            p.getSettings().setCombatStyle(WeaponInterface.STYLE_LONG_RANGE);check(CombatFormula.defenceLevel(p)==110,"Longrange grants +3 defence");
            p.getSettings().setCombatStyle(WeaponInterface.STYLE_CONTROLLED);check(CombatFormula.defenceLevel(p)==108,"Controlled grants +1 defence");
            ItemDefinition.forId(id).getBonus()[Bonuses.MAGIC_ATTACK]=-65;gear(p,4,id);
            check(magicAttack(p,new FireWave())==0,"No artificial minimum spell accuracy");
            for(int i=0;i<30;i++)check(MagicFormulae.getDamage(p,v,new FireWave())==-1,"Guaranteed splash at -65 magic");
            Interaction inter=new Interaction(p,v);inter.setSpell(new FireWave());MagicFormulae.setDamage(inter);
            check(inter.getDamage().getHit()==-1,"Interaction API also preserves splash");
        } finally {System.arraycopy(original,0,ItemDefinition.forId(id).getBonus(),0,15);}
        p=player();v=player();
        voidGear(p,11665,19785,19786);check(p.getEquipment().voidSet(1),"Previously omitted elite robe variant works");
        check(MeleeFormulae.getMeleeDamage(p,1)==122,"Void melee effective level floors before damage");
        check(MeleeFormulae.getMeleeAccuracy(p,1)==1170,"Void melee 10%, not 15%");
        check(MeleeFormulae.getMeleeAccuracy(p,2)==2340,"Void composes multiplicatively with specials");
        gear(p,9,-1);check(!p.getEquipment().voidSet(1),"Incomplete set has no bonus");
        gear(p,5,19711);check(p.getEquipment().voidSet(1),"Deflector replaces a non-helmet piece");
        gear(p,0,-1);check(!p.getEquipment().voidSet(1),"Deflector cannot replace helmet");
        gear(p,0,11663);check(p.getEquipment().voidSet(3),"Mage helmet selects magic set");
        double base=MagicFormulae.getMaximumDamage(v,v,new FireWave());
        check(MagicFormulae.getMaximumDamage(p,v,new FireWave())==base,"Void mage adds no damage");
        check(magicAttack(p,new FireWave())==1390,"Void mage 30% effective level accuracy");
        v.getSkills().setLevel(Skills.MAGIC,106);check(MagicFormulae.getMaximumDamage(v,p,new FireWave())==242,"Seven boosted magic levels grant 21% damage");
        p=player();gear(p,3,861);gear(p,13,892);check(RangeFormulae.getRangeDamage(p,1)==193,"99 rapid magic shortbow/rune arrows max 193");
        p.getSettings().setCombatStyle(WeaponInterface.STYLE_ACCURATE);check(RangeFormulae.getRangeDamage(p,1)==199,"Accurate ranged stance max 199");
        p.getSettings().setCombatStyle(WeaponInterface.STYLE_LONG_RANGE);check(RangeFormulae.getRangeDamage(p,1)==193,"Longrange doesn't add ranged strength");
        // Actual generated attacks with real packed plate, dragonhide and robe bodies.
        p=player();v=player();p.getRandom().setSeed(512);v.getRandom().setSeed(816);
        int[] armour={1127,2503,4091};String[] names={"Rune platebody","Black d'hide body","Mystic robe top"};
        double[][] rates=new double[3][3];
        for(int style=0;style<3;style++){
            gear(p,3,style==0?4151:style==1?861:1381);gear(p,13,style==1?892:-1);
            p.getSettings().setCombatType(1);
            for(int j=0;j<3;j++){
                gear(v,4,armour[j]);
                double attack=style==0?MeleeFormulae.getMeleeAccuracy(p,1):style==1?RangeFormulae.getAccuracy(p,1):magicAttack(p,new FireWave());
                double defence=style==0?MeleeFormulae.getMeleeDefence(p,v,1):style==1?RangeFormulae.getDefence(p,v,1):magicDefence(p,v);
                int max=style==0?MeleeFormulae.getMeleeDamage(p,1):style==1?RangeFormulae.getRangeDamage(p,1):(int)MagicFormulae.getMaximumDamage(p,v,new FireWave());
                int positive=0;long sum=0;int n=20000;
                for(int i=0;i<n;i++){int hit=style==0?MeleeFormulae.getDamage(p,v):style==1?RangeFormulae.getDamage(p,v):MagicFormulae.getDamage(p,v,new FireWave());
                    if(hit>max||hit<(style==2?-1:0))throw new AssertionError("Generated damage outside bounds");if(hit>0){positive++;sum+=hit;}}
                double rate=positive/(double)n;rates[style][j]=rate;
                check(Math.abs(rate-chance(attack,defence)*max/(max+1.0))<.02,"Actual hit frequency matches independent rolls");
                check(Math.abs(sum/(double)positive-(max+1)/2.0)<max*.025,"Successful damage independent of armour");
                System.out.printf(Locale.ROOT,"MATRIX style=%d vs %s: positive hits=%.2f%% max=%d meanPositive=%.2f%n",style,names[j],100*rate,max,sum/(double)positive);
            }
        }
        check(rates[0][0]<rates[0][2],"Plate resists melee better than robes");
        check(rates[1][0]<rates[1][2],"Plate resists ranged better than robes");
        check(rates[2][1]<rates[2][0],"Dragonhide resists magic better than plate");
        NPC npc=new NPC(1);check(MagicFormulae.getMaximumMagicDamage(npc,1)>=0,"NPC distinct magic strength schema retained");
        check(MeleeFormulae.getMeleeAccuracy(npc,1)>0&&RangeFormulae.getAccuracy(npc,1)>0,"NPC roll adapters remain usable");
        System.out.println("PASS: "+checks+" formula checks and 180,000 actual generated attacks");
    }
}