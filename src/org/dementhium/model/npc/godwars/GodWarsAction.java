package org.dementhium.model.npc.godwars;

import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.map.Directions;
import org.dementhium.model.map.path.PrimitivePathFinder;
import org.dementhium.model.map.path.ProjectilePathFinder;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;

/** A selected attack determines both movement reach and the immutable pending hit list. */
public class GodWarsAction extends CombatAction {
    public enum Attack {
        BANDOS_MELEE(CombatType.MELEE,600,7060,-1,-1), BANDOS_RANGE(CombatType.RANGE,350,7063,1200,1218),
        SARA_MELEE(CombatType.MELEE,270,6964,-1,-1), SARA_MAGIC(CombatType.MAGIC,200,6970,-1,1194),
        ZAM_MELEE(CombatType.MELEE,460,6945,-1,-1), ZAM_MAGIC(CombatType.MAGIC,300,6947,1211,-1), ZAM_SPECIAL(CombatType.MELEE,490,6947,-1,1210),
        KREE_MELEE(CombatType.MELEE,260,6977,-1,-1), KREE_RANGE(CombatType.RANGE,715,6976,1197,-1), KREE_MAGIC(CombatType.MAGIC,210,6976,1198,-1),
        FOLLOW_MELEE(CombatType.MELEE,0,-1,-1,-1), FOLLOW_RANGE(CombatType.RANGE,0,-1,-1,-1), FOLLOW_MAGIC(CombatType.MAGIC,0,-1,-1,-1);
        public final CombatType type;public final int cap,animation,projectile,graphic;
        Attack(CombatType type,int cap,int animation,int projectile,int graphic){this.type=type;this.cap=cap;this.animation=animation;this.projectile=projectile;this.graphic=graphic;}
        boolean contact(){return type==CombatType.MELEE||this==SARA_MAGIC||this==BANDOS_RANGE||this==ZAM_MAGIC;}
        boolean area(){return this==BANDOS_RANGE||this==SARA_MAGIC||this==KREE_RANGE||this==KREE_MAGIC;}
    }
    private final GodWarsNPC npc;
    private final Attack selected;
    private GodWarsRoom room;
    private long roomRevision;
    private boolean finished;
    private final List<Hit> hits=new ArrayList<Hit>();
    private static final class Hit {
        final Player victim;final Attack attack;final Damage damage;final NPCCombatContext context;int ticks;boolean done;
        Hit(GodWarsNPC npc,Player p,Attack a,Damage d,int ticks){victim=p;attack=a;damage=d;this.ticks=ticks;context=new NPCCombatContext(npc,p);}
    }
    public GodWarsAction(GodWarsNPC npc){this(npc,null);}
    private GodWarsAction(GodWarsNPC npc,Attack selected){super(true);this.npc=npc;this.selected=selected;}
    @Override public CombatAction newSession(){return new GodWarsAction(npc,npc.takeAttack());}
    public Attack attack(){return selected==null?npc.prepareAttack():selected;}
    @Override public CombatType getCombatType(){Attack a=attack();return a.contact()?CombatType.MELEE:a.type;}
    public static Attack choose(GodWarsNPC n,Mob target){
        switch(n.getId()){
        case 6260:return n.getRandom().nextInt(3)==0?Attack.BANDOS_RANGE:Attack.BANDOS_MELEE;
        case 6247:return n.getRandom().nextBoolean()?Attack.SARA_MELEE:Attack.SARA_MAGIC;
        case 6203:
            if(n.getRandom().nextInt(3)==0)return Attack.ZAM_MAGIC;
            return target!=null&&protectedMelee(target)&&n.getRandom().nextInt(9)==0?Attack.ZAM_SPECIAL:Attack.ZAM_MELEE;
        case 6222:
            if(target!=null&&!n.hasAttacker())return Attack.KREE_MELEE;
            return n.getRandom().nextBoolean()?Attack.KREE_RANGE:Attack.KREE_MAGIC;
        case 6261:case 6248:case 6204:case 6227:return Attack.FOLLOW_MELEE;
        case 6265:case 6252:case 6206:case 6225:return Attack.FOLLOW_RANGE;
        default:return Attack.FOLLOW_MAGIC;
        }
    }
    public static boolean eligible(GodWarsNPC npc,Mob target,Attack attack){
        if(npc.getId()==6222)return (attack==Attack.KREE_MELEE)==(target!=null&&!npc.hasAttacker());
        return attack!=Attack.ZAM_SPECIAL||target!=null&&protectedMelee(target);
    }
    private static boolean protectedMelee(Mob m){return m.isPlayer()&&(m.getPlayer().getPrayer().usingPrayer(0,CombatType.MELEE.getProtectionPrayer())||m.getPlayer().getPrayer().usingPrayer(1,9));}
    public static boolean contact(Mob s,Mob v){
        return CombatMovement.hasMeleeContact(s,v,true);
    }
    private int cap(Attack a){
        int base=baseCap(a);
        // Server rule: ordinary Strength drains scale physical melee caps at launch.
        // The prayer smash is a fixed special; magic and ranged caps are independent.
        if(a.type!=CombatType.MELEE||a==Attack.ZAM_SPECIAL)return base;
        int strength=npc.getCombatStats().base(Skills.STRENGTH);
        return strength<=0?base:(int)Math.max(1,Math.min(base,(long)base*npc.getCombatLevel(Skills.STRENGTH)/strength));
    }
    private int baseCap(Attack a){
        if(a.cap>0)return a.cap;
        switch(npc.getId()){
        case 6261:return 160;case 6263:return 170;case 6265:return 210;
        case 6248:case 6250:case 6252:return 160;
        case 6204:return 150;case 6206:return 210;case 6208:return 170;
        case 6227:return 200;default:return 250;
        }
    }
    private int projectile(Attack a){
        if(npc.isBoss())return a.projectile;
        switch(npc.getId()){case 6223:return 1199;case 6225:return 1190;case 6208:return 1213;default:return npc.getDefinition().getProjectileId();}
    }
    @Override public boolean commenceSession(){
        if(!hits.isEmpty()||finished)return false;
        room=npc.getRoom();if(room==null||!(interaction.getVictim() instanceof Player)||!interaction.isNPCContextCurrent())return false;
        Attack a=attack();Player primary=interaction.getVictim().getPlayer();
        if(!room.accepts(npc,primary)||!clear(npc,primary)||a.contact()&&!contact(npc,primary))return false;
        if(a==Attack.ZAM_SPECIAL&&!protectedMelee(primary))return false;
        List<Player> targets=a.area()?room.targets(npc):Collections.singletonList(primary);
        roomRevision=room.getRevision();
        for(Player p:targets){
            if(!NPCCombatContext.validPair(npc,p)||!clear(npc,p))continue;
            Attack each=a;
            if(npc.getId()==6222&&a.area()&&p!=primary)each=npc.getRandom().nextBoolean()?Attack.KREE_RANGE:Attack.KREE_MAGIC;
            int max=cap(each);
            int raw=roll(each,p,max);
            Damage damage=Damage.getDamage(npc,p,each.type,raw,each==Attack.ZAM_SPECIAL);damage.setMaximum(max);
            if(each==Attack.ZAM_SPECIAL)damage.onImpact(actual -> {p.getSkills().drainPray(Math.floor(p.getSkills().getPrayerPoints()/2.0));p.sendMessage("K'ril Tsutsaroth slams through your protection prayer, leaving you feeling drained.");});
            if((each==Attack.ZAM_MELEE||each==Attack.ZAM_SPECIAL)&&npc.getRandom().nextInt(4)==0)damage.onContact(() -> p.getPoisonManager().poison(npc,160));
            if(each==Attack.KREE_RANGE||each==Attack.KREE_MAGIC)damage.onContact(() -> push(p));
            int projectile=projectile(each),delay=projectile<0?1:Math.max(1,(int)(npc.getLocation().distance(p.getLocation())*0.3));
            Hit pending=new Hit(npc,p,each,damage,delay);hits.add(pending);
            room.schedule(delay,() -> impact(pending));
            if(p==primary)interaction.setDamage(damage);
            if(projectile>=0)ProjectileManager.sendProjectile(Projectile.create(npc,p,projectile,30,32,52,80,3,11));
        }
        if(hits.isEmpty())return false;
        npc.animate(npc.isBoss()?a.animation:npc.getAttackAnimation());
        if(!npc.isBoss()&&npc.getDefinition().getStartGraphics()>=0)npc.graphics(npc.getDefinition().getStartGraphics());
        if(a==Attack.ZAM_SPECIAL)npc.forceText("YARRRRRRR!");
        npc.getCombatExecutor().setTicks(npc.getAttackDelay());return true;
    }
    private int roll(Attack a,Player p,int max){
        if(a==Attack.ZAM_SPECIAL)return 350+npc.getRandom().nextInt(max-349);
        int minimum=a==Attack.BANDOS_RANGE?150:a==Attack.SARA_MAGIC||a==Attack.ZAM_MAGIC?100:0;
        if(a==Attack.BANDOS_RANGE||a==Attack.KREE_RANGE||a==Attack.KREE_MAGIC){
            // Blue tornado: ranged accuracy/defence, but magic protection and damage.
            boolean hit=CombatRolls.roll(npc.getRandom(),RangeFormulae.getAccuracy(npc,1))>CombatRolls.roll(p.getRandom(),RangeFormulae.getDefence(npc,p,1));
            return hit?minimum+npc.getRandom().nextInt(max-minimum+1):-1;
        }
        if(a.type==CombatType.MAGIC){int raw=MagicFormulae.getDamage(npc,p,1.0,max-minimum,1.0);return raw<0?-1:minimum+raw;}
        if(a.type==CombatType.MELEE){
            // Preserve accuracy misses separately from successful zero/protected hits:
            // only the latter may deliver contact effects such as poison.
            boolean hit=CombatRolls.roll(npc.getRandom(),MeleeFormulae.getMeleeAccuracy(npc,1))>CombatRolls.roll(p.getRandom(),MeleeFormulae.getMeleeDefence(npc,p,1));
            return hit?npc.getRandom().nextInt(max+1):-1;
        }
        return RangeFormulae.getDamage(npc,p,1.0,max,1.0);
    }
    public static boolean clear(Mob source,Mob victim){
        int sx=Math.max(source.getLocation().getX(),Math.min(victim.getLocation().getX(),source.getLocation().getX()+source.size()-1));
        int sy=Math.max(source.getLocation().getY(),Math.min(victim.getLocation().getY(),source.getLocation().getY()+source.size()-1));
        Location edge=Location.locate(sx,sy,source.getLocation().getZ());
        int vx=Math.max(victim.getLocation().getX(),Math.min(sx,victim.getLocation().getX()+victim.size()-1));
        int vy=Math.max(victim.getLocation().getY(),Math.min(sy,victim.getLocation().getY()+victim.size()-1));
        return ProjectilePathFinder.clearPath(edge,Location.locate(vx,vy,victim.getLocation().getZ()));
    }
    private void push(Player p){
        if(!CombatStatus.statusAllowed(p)||!room.accepts(npc,p))return;
        int dx=Integer.signum(2*p.getLocation().getX()-(2*npc.getLocation().getX()+npc.size()-1));
        int dy=Integer.signum(2*p.getLocation().getY()-(2*npc.getLocation().getY()+npc.size()-1));
        if(dx==0&&dy==0)return;
        Location to=p.getLocation().transform(dx,dy,0);
        if(room.contains(to)&&PrimitivePathFinder.canMove(p.getLocation(),Directions.directionFor(dx,dy),false)&&org.dementhium.model.instance.InstanceAccess.canWalk(p,to))
            p.forceMovement(null,to.getX(),to.getY(),0,30,-1,1,true);
    }
    @Override public boolean executeSession(){return true;}
    @Override public boolean endSession(){finished=true;return true;}
    private void impact(Hit hit){
        if(hit.done)return;hit.done=true;
        if(room==null||room.getRevision()!=roomRevision||!room.owns(npc)||!hit.context.isCurrent()||!room.accepts(npc,hit.victim))return;
        if(hit.damage.getHit()<0)return;
        int graphic=npc.isBoss()?hit.attack.graphic:npc.getDefinition().getEndGraphics();
        if(graphic>=0)hit.victim.graphics(graphic);
        hit.victim.animate(hit.victim.getDefenceAnimation());
        hit.victim.getDamageManager().damage(npc,hit.damage,hit.attack.type.getDamageType());


        hit.victim.retaliate(npc);
    }
}
