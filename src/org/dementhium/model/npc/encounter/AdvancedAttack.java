package org.dementhium.model.npc.encounter;

import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.npc.godwars.GodWarsAction;
import org.dementhium.model.npc.impl.*;
import org.dementhium.model.player.*;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.misc.DamageManager.DamageType;

/** Every launched attack owns its kind, damage and impact context. */
public final class AdvancedAttack extends CombatAction {
    public enum Kind {
        TD_MELEE(CombatType.MELEE,189,10922,-1,1886),TD_RANGE(CombatType.RANGE,269,10919,1887,-1),TD_MAGIC(CombatType.MAGIC,269,10918,1884,1883),
        KBD_MELEE(CombatType.MELEE,250,80,-1,-1),FIRE(CombatType.DRAGONFIRE,620,81,393,-1),SHOCK(CombatType.DRAGONFIRE,620,84,396,-1),TOXIC(CombatType.DRAGONFIRE,620,82,394,-1),ICE(CombatType.DRAGONFIRE,620,83,395,-1),
        CHAOS_MAGIC(CombatType.MAGIC,284,314,557,558),CHAOS_RANGE(CombatType.RANGE,284,314,557,558),CHAOS_MELEE(CombatType.MELEE,284,314,557,558),TELEPORT(CombatType.MAGIC,0,314,554,555),DISARM(CombatType.MAGIC,0,314,551,552),
        FROST_MELEE(CombatType.MELEE,214,13155,-1,-1),FROST_MAGIC(CombatType.MAGIC,250,13152,2465,-1),FROST_FIRE(CombatType.DRAGONFIRE,595,13152,2465,-1);
        public final CombatType style;public final int cap,animation,projectile,graphic;
        Kind(CombatType style,int cap,int animation,int projectile,int graphic){this.style=style;this.cap=cap;this.animation=animation;this.projectile=projectile;this.graphic=graphic;}
    }
    private final AdvancedNPC npc;private final Kind selected;private boolean launched;
    public AdvancedAttack(AdvancedNPC npc){this(npc,null);}
    public AdvancedAttack(AdvancedNPC npc,Kind selected){super(true);this.npc=npc;this.selected=selected;}
    public Kind kind(){return selected==null?npc.selectAttack():selected;}
    @Override public CombatAction newSession(){return new AdvancedAttack(npc,npc.takeAdvancedAttack());}
    @Override public CombatType getCombatType(){return kind().style;}
    public static Kind choose(AdvancedNPC n,Mob target){
        if(n instanceof TormentedDemon){CombatType s=((TormentedDemon)n).offence();return s==CombatType.MELEE?Kind.TD_MELEE:s==CombatType.RANGE?Kind.TD_RANGE:Kind.TD_MAGIC;}
        if(n instanceof KingBlackDragon){if(target!=null&&GodWarsAction.contact(n,target)&&n.getRandom().nextInt(10)<3)return Kind.KBD_MELEE;return new Kind[]{Kind.FIRE,Kind.SHOCK,Kind.TOXIC,Kind.ICE}[n.getRandom().nextInt(4)];}
        if(n instanceof FrostDragon){if(target!=null&&GodWarsAction.contact(n,target)&&n.getRandom().nextBoolean())return Kind.FROST_MELEE;return n.getRandom().nextBoolean()?Kind.FROST_FIRE:Kind.FROST_MAGIC;}
        int choice=n.getRandom().nextInt(6);if(choice==0)return Kind.TELEPORT;if(choice==1)return Kind.DISARM;
        int style=n.getRandom().nextInt(10);return style<5?Kind.CHAOS_MAGIC:style<8?Kind.CHAOS_RANGE:Kind.CHAOS_MELEE;
    }
    private int roll(Kind k,Player p){
        int max=k.cap;if(k.style==CombatType.MELEE){int base=npc.getCombatStats().base(Skills.STRENGTH);if(base>0)max=Math.max(1,(int)((long)max*npc.getCombatLevel(Skills.STRENGTH)/base));}
        if(k.style==CombatType.DRAGONFIRE)return npc.getRandom().nextInt(max+1);
        if(k.style==CombatType.MAGIC)return MagicFormulae.getDamage(npc,p,1,max,1);
        boolean hit=k.style==CombatType.MELEE?CombatRolls.hits(npc.getRandom(),MeleeFormulae.getMeleeAccuracy(npc,1),p.getRandom(),MeleeFormulae.getMeleeDefence(npc,p,1)):CombatRolls.hits(npc.getRandom(),RangeFormulae.getAccuracy(npc,1),p.getRandom(),RangeFormulae.getDefence(npc,p,1));
        return hit?npc.getRandom().nextInt(max+1):-1;
    }
    @Override public boolean commenceSession(){
        if(launched||!interaction.isNPCContextCurrent()||!interaction.getVictim().isPlayer())return false;
        Player p=interaction.getVictim().getPlayer();Kind k=kind();
        if(!npc.allows(p)||EncounterNPC.gap(npc,p)>npc.reach()||!GodWarsAction.clear(npc,p))return false;
        if(k.style==CombatType.MELEE&&k!=Kind.CHAOS_MELEE&&!GodWarsAction.contact(npc,p))return false;
        launched=true;npc.getCombatExecutor().setTicks(npc.getAttackDelay());npc.animate(k.animation);
        if(npc instanceof ChaosElemental)npc.graphics(k==Kind.TELEPORT?553:k==Kind.DISARM?550:556);
        if(k.projectile>=0)ProjectileManager.sendProjectile(Projectile.create(npc,p,k.projectile,30,32,52,75,3,11));
        final NPCCombatContext context=new NPCCombatContext(npc,p);int delay=k.projectile<0?1:Math.max(1,(int)(npc.getLocation().distance(p.getLocation())*.3));
        int raw=k.cap==0?0:roll(k,p);final Damage damage=k.style==CombatType.DRAGONFIRE?new Damage(raw):Damage.getDamage(npc,p,k.style,raw);damage.setMaximum(k.cap);interaction.setDamage(damage);
        npc.schedule(delay,()->{
            if(!context.isCurrent()||!npc.allows(p))return;
            if(k==Kind.TELEPORT||k==Kind.DISARM){if(!CombatStatus.statusAllowed(p))return;p.graphics(k.graphic);if(k==Kind.TELEPORT)((ChaosElemental)npc).relocate(p);else ((ChaosElemental)npc).disarm(p);return;}
            if(k.style==CombatType.DRAGONFIRE){
                Damage breath=new Damage(dragonfire(p,k,raw));breath.setMaximum(k.cap);
                if(k==Kind.TOXIC)breath.onContact(()->p.getPoisonManager().poison(npc,88));
                if(k==Kind.ICE)breath.onContact(()->{if(npc.getRandom().nextInt(10)<7)CombatStatus.freeze(p,5);});
                if(k==Kind.SHOCK)breath.onContact(()->{if(npc.getRandom().nextInt(10)<3&&CombatStatus.statusAllowed(p))p.getSkills().decreaseLevelToZero(npc.getRandom().nextInt(3),2);});
                impact(npc,p,k.style,breath,k.graphic);damage.setHit(breath.getHit());
            }else impact(npc,p,k.style,damage,k.graphic);
        });return true;
    }
    /** Explicit per-encounter protection table, evaluated only when the breath lands. */
    public static int dragonfire(Player p,Kind k,int raw){
        if(!CombatStatus.statusAllowed(p))return 0;
        int shield=p.getEquipment().getSlot(Equipment.SLOT_SHIELD);boolean dragonfireShield=CombatUtils.isDragonfireShield(shield);boolean shielded=shield==1540||dragonfireShield||shield==8282||shield==16079||shield==16933;
        if(dragonfireShield)CombatUtils.chargeDragonfireShield(p);
        long now=System.currentTimeMillis();boolean potion=now-p.getAttribute("antiFire",0L)<360000,superPotion=now-p.getAttribute("santiFire",0L)<360000;
        boolean special=k==Kind.ICE||k==Kind.SHOCK||k==Kind.TOXIC;int cap=k.cap;
        if(superPotion||shielded&&potion)cap=special?100:0;else if(shielded||potion)cap=special?200:100;
        int protectedHit=(int)((long)Math.max(0,raw)*cap/k.cap);
        if(EncounterAttack.protects(p,CombatType.MAGIC))protectedHit=(int)(protectedHit*.6);
        return SpiritShield.reduce(p,protectedHit);
    }
    public static void impact(EncounterNPC npc,Player p,CombatType style,Damage damage,int graphic){
        if(damage.getHit()<0||!NPCCombatContext.validPair(npc,p))return;
        if(graphic>=0)p.graphics(graphic);p.animate(p.getDefenceAnimation());p.getDamageManager().damage(npc,damage,style.getDamageType());
        p.retaliate(npc);
    }
    @Override public boolean executeSession(){return true;}
    @Override public boolean endSession(){return true;}
}
