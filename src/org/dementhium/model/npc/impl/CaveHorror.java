package org.dementhium.model.npc.impl;

import org.dementhium.model.Mob;
import org.dementhium.model.combat.*;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;

/**
 * Contact attacks roll against magic defence, then use melee protection.
 * Period observation: forum.tip.it/topic/229226-cave-horrors-task/ (2009).
 * The unwarded scream's one-in-five rate/10% LP are conservative server estimates;
 * the witchwood icon and contact-only reach preserve documented counterplay.
 */
public final class CaveHorror extends NPC {
    public CaveHorror(int id) { super(id); }
    @Override public boolean isAttackable(Mob source) {
        Player player=source.isPlayer()?source.getPlayer():source.isFamiliar()?source.getFamiliar().getOwner():null;
        if(player!=null && player.getSkills().getLevel(Skills.SLAYER)<58) {
            player.sendMessage("You need a Slayer level of 58 to attack this creature.");return false;
        }
        return super.isAttackable(source);
    }
    @Override public CombatAction getCombatAction() { return new HorrorAttack(); }
    private final class HorrorAttack extends CombatAction {
        HorrorAttack(){super(true);}
        @Override public CombatAction newSession(){return new HorrorAttack();}
        @Override public CombatType getCombatType(){return CombatType.MELEE;}
        @Override public boolean commenceSession(){
            Mob victim=interaction.getVictim();
            if(!interaction.isNPCContextCurrent()||!CombatMovement.canMelee(CaveHorror.this,victim))return false;
            getCombatExecutor().setTicks(getAttackDelay());
            boolean scream=victim.isPlayer()&&victim.getPlayer().getEquipment().getSlot(2)!=8923&&getRandom().nextInt(5)==0;
            int max=scream?Math.max(1,victim.getMaximumHitPoints()/10):MeleeFormulae.getMeleeDamage(CaveHorror.this,1);
            int raw=scream?max:SlayerNPC.magicMeleeDamage(CaveHorror.this,victim,max);
            Damage damage=Damage.getDamage(CaveHorror.this,victim,CombatType.MELEE,raw);damage.setMaximum(max);
            interaction.setDamage(damage);animate(scream?4237:getAttackAnimation());return true;
        }
        @Override public boolean executeSession(){return true;}
        @Override public boolean endSession(){
            if(!interaction.isNPCContextCurrent())return true;
            Mob victim=interaction.getVictim();victim.animate(victim.getDefenceAnimation());
            victim.getDamageManager().damage(CaveHorror.this,interaction.getDamage(),CombatType.MELEE.getDamageType());
            victim.retaliate(CaveHorror.this);return true;
        }
    }
}
