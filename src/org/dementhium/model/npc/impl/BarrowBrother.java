package org.dementhium.model.npc.impl;

import org.dementhium.content.activity.impl.BarrowsActivity;
import org.dementhium.content.activity.impl.barrows.BarrowsRules;
import org.dementhium.model.Mob;
import org.dementhium.model.combat.*;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;

/** Owner-bound original Barrows brother, with its own attack state. */
public class BarrowBrother extends NPC {
    private final BrotherAction action=new BrotherAction();
    private boolean defilerHit;
    public BarrowBrother(int id){
        super(id);
        setUnrespawnable(true);
        getPoisonManager().setCanBePoisoned(false);
    }
    @Override public boolean isAttackable(Mob attacker) {
        if (attacker != getAttribute("barrowsOwner")) {
            if (attacker != null && attacker.isPlayer()) attacker.getPlayer().sendMessage("This monster is not after you.");
            return false;
        }
        return super.isAttackable(attacker);
    }
    public boolean isDefilerHit(){return defilerHit;}
    @Override public CombatAction getCombatAction(){return action;}
    @Override public int getAttackDelay(){return getId()==2026?7:getId()==2028?4:5;}
    private boolean valid(Player p){
        return !isDead() && p!=null && p.isOnline() && !p.isDead()
                && p==getAttribute("barrowsOwner") && getAttribute("isSpawned",false)
                && p.getActivity() instanceof BarrowsActivity && ((BarrowsActivity)p.getActivity()).owns(this)
                && p.getLocation().getZ()==getLocation().getZ()
                && p.getLocation().distance(getLocation())<=16;
    }
    private final class BrotherAction extends CombatAction {
        private int roll,maximum,debuff=-1,endGraphic=-1;
        private boolean proc;
        private boolean consumed;
        BrotherAction(){super(false);}
        @Override public CombatAction newSession(){return new BrotherAction();}
        public CombatType getCombatType(){
            return getId()==2025?CombatType.MAGIC:getId()==2028?CombatType.RANGE:CombatType.MELEE;
        }
        public boolean commenceSession(){
            Mob victim=interaction.getVictim();
            if(victim==null||!victim.isPlayer()||!valid(victim.getPlayer()))return false;
            consumed=false;
            getCombatExecutor().setTicks(getAttackDelay());
            proc=getRandom().nextInt(4)==0;debuff=-1;
            maximum=getId()==2026?BarrowsRules.dharokMaximum(getHitPoints(),getMaximumHitPoints())
                    :getId()==2025||getId()==2028?200:getId()==2029?230:240;
            CombatType type=getCombatType();
            if(type==CombatType.MELEE) {
                roll=getId()==2030&&proc?getRandom().nextInt(maximum+1)
                        :MeleeFormulae.getDamage(BarrowBrother.this,victim,1.0,maximum,1.0);
            }else if(type==CombatType.RANGE){
                roll=RangeFormulae.getDamage(BarrowBrother.this,victim,1.0,maximum,1.0);
            }else{
                roll=MagicFormulae.getDamage(BarrowBrother.this,victim,1.0,maximum,1.0);
                if(getRandom().nextInt(4)==0)debuff=new int[]{Skills.ATTACK,Skills.STRENGTH,Skills.DEFENCE}[getRandom().nextInt(3)];
            }
            endGraphic=getDefinition().getEndGraphics();
            animate(getId()==2025 ? (debuff>=0 ? 710 : 2791) : getAttackAnimation());
            if(type!=CombatType.MELEE){
                int startGraphic=getDefinition().getStartGraphics(), projectile=getDefinition().getProjectileId();
                if(getId()==2025) {
                    startGraphic=debuff<0?2728:debuff==Skills.ATTACK?102:debuff==Skills.STRENGTH?105:108;
                    projectile=debuff<0?2733:startGraphic+1;
                    endGraphic=debuff<0?2740:startGraphic+2;
                }
                if(startGraphic>=0)graphics(startGraphic);
                if(projectile>=0)ProjectileManager.sendDelayedProjectile(BarrowBrother.this,victim,projectile,false);
            }
            interaction.setTicks(type==CombatType.MELEE?0:1);
            return true;
        }
        public boolean executeSession(){interaction.setTicks(interaction.getTicks()-1);return interaction.getTicks()<1;}
        public boolean endSession(){
            Player p=interaction.getVictim().getPlayer();
            if(consumed || !interaction.isNPCContextCurrent() || !valid(p))return true;
            consumed=true;
            CombatType type=getCombatType();
            Damage damage;
            // Bypass only this Verac hit's protection check; all other reductions and godmode remain.
            defilerHit=getId()==2030&&proc;
            try{damage=Damage.getDamage(BarrowBrother.this,p,type,roll<0?-1:debuff>=0?0:roll);}
            finally{defilerHit=false;}
            damage.setMaximum(maximum);
            interaction.setDamage(damage);
            if(debuff>=0 && roll>=0) damage.onContact(() -> {
                if(CombatStatus.statusAllowed(p)) p.getSkills().decreaseLevelOnce(debuff,Math.max(1,p.getSkills().getLevelForExperience(debuff)*5/100));
            });
            if(proc && debuff<0) damage.onImpact(actual -> {
                if(getId()==2027)heal(actual);
                if(CombatStatus.statusAllowed(p)){
                    if(getId()==2025)p.getSkills().decreaseLevelToMinimum(Skills.STRENGTH,5);
                    if(getId()==2028)p.getSkills().decreaseLevelToMinimum(Skills.AGILITY,Math.max(1,p.getSkills().getLevel(Skills.AGILITY)/5));
                    if(getId()==2029&&actual>0)p.getWalkingQueue().setRunEnergy(Math.max(0,p.getWalkingQueue().getRunEnergy()-p.getWalkingQueue().getRunEnergy()/5));
                }
            });
            p.getDamageManager().damage(BarrowBrother.this,damage,type.getDamageType());
            if(roll<0&&type==CombatType.MAGIC)p.graphics(85);
            else if(type==CombatType.MAGIC&&endGraphic>=0)p.graphics(endGraphic);
            p.animate(p.getDefenceAnimation());



            p.retaliate(BarrowBrother.this);return true;
        }
    }
}
