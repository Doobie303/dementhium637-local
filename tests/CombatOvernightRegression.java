import java.lang.reflect.*;
import java.util.*;
import org.dementhium.cache.Cache;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.misc.PoisonManager;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.*;
import org.dementhium.tickable.Tick;

public class CombatOvernightRegression {
    static int checks;
    static void check(boolean value,String text) { checks++; if(!value)throw new AssertionError(text); }
    static Player player() { Player p=CombatEnhancementsRegression.player();p.setOnline(true);return p; }
    static Tick pending(PoisonManager manager)throws Exception {
        Field f=PoisonManager.class.getDeclaredField("pending");f.setAccessible(true);return (Tick)f.get(manager);
    }
    static class Target extends NPC {
        boolean eligible=true;
        Target(int x,int y) {super(1);setLocation(Location.locate(x,y,0));}
        public boolean isMulti(){return true;}
        public boolean isAttackable(Mob attacker){return eligible;}
    }
    static void status()throws Exception {
        Player p=player(),v=player();
        Target first=new Target(3200,3200);List<Mob> crowd=new ArrayList<Mob>();
        for(int i=0;i<20;i++){Target t=new Target(3200,3200);t.eligible=i>=10;crowd.add(t);}
        crowd.add(first);crowd.add(first);
        List<ExtraTarget> chosen=CombatUtils.selectTargets(p,first,1,9,crowd);
        check(chosen.size()==9,"Nine accepted targets despite ten rejected entries");
        check(chosen.get(0).getVictim()==first,"Selected victim first even when last in region order");
        Set<Mob> unique=new HashSet<Mob>();for(ExtraTarget t:chosen)unique.add(t.getVictim());
        check(unique.size()==9,"No duplicate victims");
        check(CombatUtils.selectTargets(p,first,1,0,crowd).isEmpty(),"Zero cap");
        first.setLocation(Location.locate(3200,3200,1));
        check(CombatUtils.selectTargets(p,first,1,9,crowd).size()==1,"Other planes excluded");
        Damage d=Damage.getDamage(p,v,CombatType.MAGIC,0);
        CombatStatus.miasmicOnImpact(d,v,20);
        check(v.getAttribute("miasmicTime",-1)==-1,"Miasmic waits for impact");
        v.getDamageManager().damage(p,d,DamageType.MAGE);
        check(v.getAttribute("miasmicTime",-1)==World.getTicks()+20,"Successful zero rolls apply Miasmic");
        v=player();d=Damage.getDamage(p,v,CombatType.MAGIC,100);CombatStatus.miasmicOnImpact(d,v,20);
        v.setAttribute("godmode",true);v.getDamageManager().damage(p,d,DamageType.MAGE);
        check(v.getAttribute("miasmicTime",-1)==-1,"Impact immunity prevents Miasmic");
        v=player();d=Damage.getDamage(p,v,CombatType.MAGIC,-1);CombatStatus.miasmicOnImpact(d,v,20);
        d.finishEffects(p,v,0);check(v.getAttribute("miasmicTime",-1)==-1,"Splash cannot slow");
        v=player();PoisonManager manager=v.getPoisonManager();
        manager.poison(p,10);check(manager.isPoisoned(),"Ten LP is active poison");Tick old=pending(manager);
        manager.removePoison();manager.poison(p,40);Tick fresh=pending(manager);
        int hp=v.getHitPoints();old.execute();check(v.getHitPoints()==hp&&manager.getCurrentPoisonAmount()==40,"Old poison cannot tick new stream");
        fresh.execute();check(v.getHitPoints()==hp-40,"One new poison hit");
        manager.poison(p,60);check(pending(manager)==fresh&&manager.getCurrentPoisonAmount()==60,"Stronger poison upgrades same stream");
        manager.poison(p,20);check(manager.getCurrentPoisonAmount()==60,"Weaker poison cannot downgrade");
        manager.removePoison();manager.setCanBePoisoned(false);manager.poison(p,60);check(!manager.isPoisoned(),"Antipoison prevents application");
        manager.setCanBePoisoned(true);manager.poison(p,10);pending(manager).execute();check(!manager.isPoisoned(),"Exact-ten final hit cleans up");
        manager.poison(p,60);v.setOnline(false);hp=v.getHitPoints();pending(manager).execute();check(v.getHitPoints()==hp&&!manager.isPoisoned(),"Offline poison stops");
        v=player();CombatEnhancementsRegression.gear(p,3,5698);
        d=Damage.getDamage(p,v,CombatType.MELEE,40);check(!v.getPoisonManager().isPoisoned(),"Weapon poison does not happen at roll");
        CombatEnhancementsRegression.gear(p,3,4151);v.getDamageManager().damage(p,d,DamageType.MELEE);
        check(v.getPoisonManager().isPoisoned(),"Poison uses launch weapon after swap");
    }
    static final int[][] SETS={{4708,4712,4714,4710},{4716,4720,4722,4718},{4724,4728,4730,4726},
        {4732,4736,4738,4734},{4745,4749,4751,4747},{4753,4757,4759,4755}};
    static void set(Player p,int id){int[] slots={0,4,7,3};for(int i=0;i<4;i++)CombatEnhancementsRegression.gear(p,slots[i],SETS[id-1][i]);}
    static long procSeed(){long seed=0;while(new Random(seed).nextInt(4)!=0)seed++;return seed;}
    static void equipment()throws Exception {
        for(int id:new int[]{1,3,4,5}) {
            Player p=player(),v=player();set(p,id);p.getRandom().setSeed(procSeed());
            p.getSkills().setHitPoints(500);v.getSkills().setLevelAndXP(Skills.AGILITY,99,13034431);
            v.getWalkingQueue().setRunEnergy(100);
            CombatType style=id==1?CombatType.MAGIC:id==4?CombatType.RANGE:CombatType.MELEE;
            Damage d=Damage.getDamage(p,v,style,200);
            check(v.getSkills().getLevel(Skills.STRENGTH)==99&&p.getHitPoints()==500,"Set effect waits for hit "+id);
            CombatEnhancementsRegression.gear(p,0,-1);
            v.getDamageManager().damage(p,d,style.getDamageType());
            if(id==1)check(v.getSkills().getLevel(Skills.STRENGTH)==94,"Ahrim drains Strength");
            if(id==3)check(p.getHitPoints()==700,"Guthan heals actual damage from captured set");
            if(id==4)check(v.getSkills().getLevel(Skills.AGILITY)==80,"Karil drains Agility");
            if(id==5)check(v.getWalkingQueue().getRunEnergy()==80,"Torag drains run energy");
            int hp=p.getHitPoints();d.finishEffects(p,v,200);check(p.getHitPoints()==hp,"Set callbacks cannot replay");
        }
        Player p=player(),v=player();set(p,3);p.getSkills().setHitPoints(500);p.getRandom().setSeed(procSeed());
        Damage d=Damage.getDamage(p,v,CombatType.MELEE,200);v.setAttribute("godmode",true);
        v.getDamageManager().damage(p,d,DamageType.MELEE);check(p.getHitPoints()==500,"Immune target gives no Guthan heal");
        CombatEnhancementsRegression.gear(p,0,-1);p.getRandom().setSeed(procSeed());v=player();
        d=Damage.getDamage(p,v,CombatType.MELEE,200);v.getDamageManager().damage(p,d,DamageType.MELEE);
        check(p.getHitPoints()==500,"Incomplete set cannot heal");
        p=player();v=player();set(p,6);CombatBalanceRegression.prayers(v)[0][org.dementhium.content.skills.Prayer.PROTECT_FROM_MELEE]=true;
        check(Damage.getDamage(p,v,CombatType.MELEE,400).getHit()==240,"Ordinary Verac hit respects protection");
        d=Damage.getDamage(p,v,CombatType.MELEE,400,true);check(d.getHit()==400,"Per-hit Verac bypass");
        CombatEnhancementsRegression.gear(v,5,13740);v.getDamageManager().damage(p,d,DamageType.MELEE);
        check(v.getHitPoints()==720,"Bypass retains Divine reduction");
        p=player();CombatEnhancementsRegression.gear(p,3,6528);double base=MeleeFormulae.getMeleeDamage(p,1);
        CombatEnhancementsRegression.gear(p,2,11128);check(EquipmentEffects.obsidianDamage(p)==1.2,"Berserker melee boost");
        check(MeleeFormulae.getMeleeDamage(p,1)>base,"Berserker changes max hit");
        CombatEnhancementsRegression.gear(p,3,6522);check(EquipmentEffects.obsidianDamage(p)==1,"No thrown obsidian boost");
        check(EquipmentEffects.matchesTask("Bloodvelds","Mutated bloodveld"),"Explicit Slayer family");
        check(!EquipmentEffects.matchesTask("Dragon","Dragon impling"),"No substring false task matches");
        check(EquipmentEffects.undead("Mighty banshee"),"Expanded undead family");
    }
    static Object curse(Player p,int id)throws Exception {
        Method m=org.dementhium.content.skills.Prayer.class.getDeclaredMethod("createCurse",int.class);
        m.setAccessible(true);return m.invoke(p.getPrayer(),id);
    }
    static void applyCurse(Object curse,Player victim)throws Exception {
        Method m=curse.getClass().getDeclaredMethod("curse",Mob.class);m.setAccessible(true);m.invoke(curse,victim);
    }
    static void deactivate(Object curse)throws Exception {
        Method m=curse.getClass().getDeclaredMethod("deactivate");m.setAccessible(true);m.invoke(curse);
    }
    static void cursesAndBolts()throws Exception {
        Player p=player(),v=player(),third=player();
        World.getWorld().getPlayers().add(v);World.getWorld().getPlayers().add(third);
        Object sap=curse(p,org.dementhium.content.skills.Prayer.SAP_WARRIOR);
        applyCurse(sap,v);check(Math.abs(v.getPrayer().getAttackModifier()+.10)<1e-8,"Sap starts at ten percent");
        for(int i=0;i<50;i++)applyCurse(sap,v);
        check(Math.abs(v.getPrayer().getAttackModifier()+.20)<1e-8,"Sap capped at twenty percent");
        check(p.getPrayer().getAttackModifier()==0,"Sap never boosts owner");
        deactivate(sap);deactivate(sap);check(v.getPrayer().getAttackModifier()==0,"Sap cleanup exact and idempotent");
        Object leech=curse(p,org.dementhium.content.skills.Prayer.LEECH_ATTACK);
        for(int i=0;i<50;i++){applyCurse(leech,v);applyCurse(leech,third);}
        check(Math.abs(v.getPrayer().getAttackModifier()+.25)<1e-8,"Leech victim capped at twenty-five percent");
        check(Math.abs(p.getPrayer().getAttackModifier()-.05)<1e-8,"Dynamic leech owner boost capped at five percent across targets");
        Object other=curse(player(),org.dementhium.content.skills.Prayer.LEECH_ATTACK);applyCurse(other,v);
        deactivate(leech);check(Math.abs(v.getPrayer().getAttackModifier()+.10)<1e-8,"Cleanup preserves other caster penalty");
        check(p.getPrayer().getAttackModifier()==0&&third.getPrayer().getAttackModifier()==0,"Multi-target leech restores exact owned totals");
        deactivate(other);check(v.getPrayer().getAttackModifier()==0,"All curses restored");
        v.setAttribute("godmode",true);applyCurse(sap,v);check(v.getPrayer().getAttackModifier()==0,"Godmode prevents curses");v.removeAttribute("godmode");
        Object mage=curse(p,org.dementhium.content.skills.Prayer.SAP_MAGE);applyCurse(mage,v);
        check(Math.abs(v.getPrayer().getMagicDefenceModifier()+.10)<1e-8,"Magic curse feeds defence-level contribution");deactivate(mage);
        p=player();v=player();CombatEnhancementsRegression.gear(p,3,9185);CombatEnhancementsRegression.gear(p,13,9244);
        CombatEnhancementsRegression.gear(v,5,1540);check(CombatUtils.dragonstoneProtected(v),"Anti-dragon shield blocks enchantment");
        long seed=0;while(new Random(seed).nextInt(100)<75)seed++;
        p.getRandom().setSeed(seed);v.getRandom().setSeed(99);Damage d=CombatUtils.getRangeDamage(p,v,Ammunition.get(9244));
        p.getRandom().setSeed(seed);p.getRandom().nextInt(100);v.getRandom().setSeed(99);
        int expected=RangeFormulae.getDamage(p,v);
        check(d.getHit()==expected,"Protected dragonstone remains an ordinary bolt, not a dragonfire-reduced arrow");
        CombatEnhancementsRegression.gear(v,5,-1);check(!CombatUtils.dragonstoneProtected(v),"No phantom antifire protection");
        v.setAttribute("antiFire",System.currentTimeMillis());check(CombatUtils.dragonstoneProtected(v),"Antifire blocks enchantment");
    }
    static void specials()throws Exception {
        Player p=player(),v=player();
        p.getWalkingQueue().setRunEnergy(20);v.getWalkingQueue().setRunEnergy(3);
        Damage d=Damage.getDamage(p,v,CombatType.MELEE,100);SpecialEffects.energyDrain(d,p,v);
        check(v.getWalkingQueue().getRunEnergy()==3,"Whip energy waits for impact");
        v.getDamageManager().damage(p,d,DamageType.MELEE);
        check(v.getWalkingQueue().getRunEnergy()==0&&p.getWalkingQueue().getRunEnergy()==23,"Whip transfers only available energy");
        p=player();v=player();p.getSkills().setHitPoints(500);Interaction inter=new Interaction(p,v);
        d=Damage.getDamage(p,v,CombatType.RANGE,100);v.getDamageManager().damage(p,d,DamageType.RANGE);
        Tick healing=SpecialEffects.healing(inter,d,20,2,10);check(p.getHitPoints()==500,"Healing timer does not execute at scheduling");
        healing.execute();healing.execute();check(p.getHitPoints()==540&&!healing.isRunning(),"Exact healing pulses");
        healing=SpecialEffects.healing(inter,d,20,2,10);p.setOnline(false);healing.execute();check(p.getHitPoints()==540&&!healing.isRunning(),"Healing stops at logout");
        p=player();v=player();inter=new Interaction(p,v);d=Damage.getDamage(p,v,CombatType.RANGE,100);
        v.getDamageManager().damage(p,d,DamageType.RANGE);
        Tick one=SpecialEffects.bleed(inter,d,75),two=SpecialEffects.bleed(inter,d,60);
        one.execute();two.execute();one.execute();two.execute();check(v.getHitPoints()==765,"Overlapping bleeds own independent remaining amounts");
        check(!one.isRunning()&&!two.isRunning(),"Bleeds stop after exact totals");
        Tick immune=SpecialEffects.bleed(inter,d,50);v.setAttribute("godmode",true);immune.execute();check(v.getHitPoints()==765&&!immune.isRunning(),"Bleed stops on immunity");
        p=player();v=player();p.getSettings().setCombatStyle(WeaponInterface.STYLE_ACCURATE);
        d=Damage.getDamage(p,v,CombatType.MELEE,100);
        double attack=p.getSkills().getXp(Skills.ATTACK),strength=p.getSkills().getXp(Skills.STRENGTH);
        p.getSettings().setCombatStyle(WeaponInterface.STYLE_AGGRESSIVE);SpecialHits.awardOnImpact(p,d,DamageType.MELEE);
        v.getDamageManager().damage(p,d,DamageType.MELEE);
        check(p.getSkills().getXp(Skills.ATTACK)>attack&&p.getSkills().getXp(Skills.STRENGTH)==strength,"XP uses launch stance after switch");
        double after=p.getSkills().getXp(Skills.ATTACK);SpecialHits.awardOnImpact(p,d,DamageType.MELEE);
        check(p.getSkills().getXp(Skills.ATTACK)==after,"XP cannot be awarded twice");
        p=player();v=player();p.setSpecialAmount(499);inter=new Interaction(p,v);
        new org.dementhium.model.combat.impl.specs.QuickSmash().commenceSpecialAttack(inter);
        check(inter.getDamage()==null&&p.getSpecialAmount()==499,"Maul rejects insufficient energy before roll");
        p.setSpecialAmount(1000);new org.dementhium.model.combat.impl.specs.QuickSmash().commenceSpecialAttack(inter);
        int hp=v.getHitPoints();check(p.getSpecialAmount()==500,"Maul charges once");
        new org.dementhium.model.combat.impl.specs.QuickSmash().commenceSpecialAttack(inter);
        check(p.getSpecialAmount()==500&&v.getHitPoints()==hp,"Maul interaction cannot replay");
        org.dementhium.model.combat.impl.specs.Sever sever=new org.dementhium.model.combat.impl.specs.Sever();
        p=player();v=player();inter=new Interaction(p,v);p.setAttribute("godmode",true);
        sever.commenceSpecialAttack(inter);check(v.getAttribute("protectionDisabledUntil",-1)==-1,"Scimitar restriction waits for impact");
        inter.getDamage().setHit(100);v.getDamageManager().damage(p,inter.getDamage(),DamageType.MELEE);
        check(v.getAttribute("protectionDisabledUntil",-1)==World.getTicks()+8,"Scimitar restriction has expiry");
        v.getPrayer().switchPrayer(org.dementhium.content.skills.Prayer.PROTECT_FROM_MELEE,false);
        check(!v.getPrayer().usingPrayer(0,org.dementhium.content.skills.Prayer.PROTECT_FROM_MELEE),"Scimitar prevents protection reactivation");
        v.setAttribute("protectionDisabledUntil",World.getTicks());v.getPrayer().switchPrayer(org.dementhium.content.skills.Prayer.PROTECT_FROM_MELEE,false);
        check(v.getPrayer().usingPrayer(0,org.dementhium.content.skills.Prayer.PROTECT_FROM_MELEE),"Protection returns at expiry");
        check(RangeWeapon.get(861).getAttackRange(false)==7&&RangeWeapon.get(861).getAttackRange(true)==9,"Shortbow reach plus longrange");
        check(RangeWeapon.get(859).getAttackRange(false)==10&&RangeWeapon.get(859).getAttackRange(true)==10,"Longbow reach cap");
        check(RangeWeapon.get(811).getAttackRange(false)==3&&RangeWeapon.get(811).getAttackRange(true)==5,"Dart reach");
        check(RangeWeapon.get(9185).getAttackRange(false)==7&&RangeWeapon.get(9185).getAttackRange(true)==9,"Crossbow reach");
        check(RangeWeapon.get(8880).getAttackRange(false)==6&&RangeWeapon.get(8880).getAttackRange(true)==8,"Dorgeshuun crossbow reach");
        check(RangeWeapon.get(15241).getAttackRange(false)==9&&RangeWeapon.get(15241).getAttackRange(true)==10,"Hand cannon reach cap");
        p=player();v=player();CombatEnhancementsRegression.gear(p,3,861);p.setLocation(Location.locate(3200,3200,0));v.setLocation(Location.locate(3207,3200,0));
        check(CombatMovement.combatFollow(p,v,CombatType.RANGE),"Shortbow attacks at configured seven-tile boundary");
        v.setLocation(Location.locate(3208,3200,0));check(!CombatMovement.combatFollow(p,v,CombatType.RANGE),"Shortbow follows beyond configured range");
        p=player();v=player();CombatEnhancementsRegression.gear(p,3,861);p.getSettings().setCombatStyle(WeaponInterface.STYLE_LONG_RANGE);p.setLocation(Location.locate(3200,3200,0));v.setLocation(Location.locate(3209,3200,0));
        check(CombatMovement.combatFollow(p,v,CombatType.RANGE),"Longrange shortbow attacks at nine-tile boundary");
        inter=new Interaction(p,v);inter.setSpecialWarmup(3);check(!inter.finishSpecialWarmup()&&!inter.finishSpecialWarmup()&&inter.finishSpecialWarmup()&&!inter.finishSpecialWarmup(),"Cannon projectile released once after owned warmup");
    }
    static void miasmicRequirements() {
        MagicSpell[] spells={new org.dementhium.model.combat.impl.spells.ancient.MiasmicRush(),new org.dementhium.model.combat.impl.spells.ancient.MiasmicBurst(),new org.dementhium.model.combat.impl.spells.ancient.MiasmicBlitz(),new org.dementhium.model.combat.impl.spells.ancient.MiasmicBarrage()};
        for(MagicSpell spell:spells) {
            Player p=player(),v=player();p.getCombatExecutor().setVictim(v);v.getCombatExecutor().setVictim(p);
            check(!spell.castSpell(new Interaction(p,v)),"Miasmic requires staff");
            check(p.getCombatExecutor().getVictim()==null&&v.getCombatExecutor().getVictim()==p,"Invalid caster cannot reset victim: "+spell.getClass().getSimpleName());
        }
    }
    static void variants() {
        for(int set=1;set<=6;set++) {
            Player p=player();int[] slots={0,4,7,3};
            for(int stage=0;stage<4;stage++) {
                for(int piece=0;piece<4;piece++) {
                    int base=SETS[set-1][piece],first=-1;
                    for(int item=4856;item<=4999;item++)if(DegradingHandler.getCombatItemId(item)==base){first=item;break;}
                    check(first>=0,"Usable degradation family exists");
                    CombatEnhancementsRegression.gear(p,slots[piece],first+stage);
                }
                check(p.getEquipment().barrowsSet(set),"Usable wear stage retains complete set "+set+":"+stage);
            }
            CombatEnhancementsRegression.gear(p,0,p.getEquipment().getSlot(0)+1);
            check(!p.getEquipment().barrowsSet(set),"Broken helm disables set "+set);
        }
    }
    public static void main(String[] args)throws Exception {
        Cache.init();ItemDefinition.init();NPCDefinition.init();org.dementhium.model.misc.GroundItemManager.load();
        Field areas=World.class.getDeclaredField("areaManager");areas.setAccessible(true);areas.set(World.getWorld(),new org.dementhium.content.areas.AreaManager());
        org.dementhium.model.map.Region region=org.dementhium.model.map.Region.forCoords(3200,3200);region.clippingMasks=new int[4][128][128];region.setClipped(true);
        if(args.length>0 && args[0].equals("miasmic")){miasmicRequirements();System.out.println("PASS: Miasmic requirement checks");return;}
        status();
        equipment();
        cursesAndBolts();
        specials();
        miasmicRequirements();
        variants();
        System.out.println("PASS: "+checks+" overnight combat checks");
    }
}
