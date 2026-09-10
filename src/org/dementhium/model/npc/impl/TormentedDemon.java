package org.dementhium.model.npc.impl;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.npc.encounter.*;
import org.dementhium.model.player.*;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.net.ActionSender;

public class TormentedDemon extends AdvancedNPC {
    private final int spawnId,baseId;
    private CombatType protection=CombatType.MELEE,offence=CombatType.RANGE;
    private final int[] received=new int[3];
    private boolean shield=true;
    private int restoreAt,nextSwitch=27;
    public TormentedDemon(int id){super(id);spawnId=id;baseId=id<=8364?8349+(id-8349)/4*4:8349;bindArena(2570,5690,2640,5768,0);showProtection();}
    @Override public int size(){return 4;}
    @Override public int getAttackDelay(){return 6;}
    @Override public int aggression(){return 6;}
    public boolean shieldActive(){return shield;}
    public CombatType protection(){return protection;}
    public CombatType offence(){return offence;}
    public Projectile getProjectile(){return Projectile.create(this,null,1884,43,0,56,76,3,size());}
    private void showProtection(){
        // Mask transformation also replaces the definition. Preserve mutable drains across overhead changes.
        int[] skills={Skills.ATTACK,Skills.STRENGTH,Skills.DEFENCE,Skills.RANGED,Skills.MAGIC};int[] levels=new int[skills.length];
        for(int i=0;i<skills.length;i++)levels[i]=getCombatLevel(skills[i]);
        getMask().setSwitchId(baseId+3-protection.ordinal());
        for(int i=0;i<skills.length;i++)getCombatStats().drain(skills[i],Math.max(0,getCombatLevel(skills[i])-levels[i]));
    }
    @Override public int getDefenceAnimation(){if(shield)graphics(1885);return super.getDefenceAnimation();}
    @Override public Damage updateHit(Mob source,int hit,CombatType type){
        return updateHit(source,hit,type,false);
    }
    public Damage updateHit(Mob source,int hit,CombatType type,boolean bypassProtection){
        final boolean blocked=type==protection&&!bypassProtection;final int counted=Math.max(20,hit);
        final boolean darklight=source!=null&&source.isPlayer()&&type==CombatType.MELEE&&source.getPlayer().getEquipment().getSlot(3)==6746;
        Damage damage=new Damage(blocked?0:shield?Math.max(0,hit)/4:Math.max(0,hit));
        if(type.ordinal()<3)damage.onContact(()->{
            if(isDead()||blocked)return;int index=type.ordinal();received[index]=Math.min(310,received[index]+counted);
            if(received[index]>=310){protection=type;Arrays.fill(received,0);showProtection();}
        });
        if(darklight)damage.onImpact(actual->{if(!isDead()){shield=false;restoreAt=clock()+100;source.getPlayer().sendMessage("The demon is temporarily weakened by your weapon.");}});
        return damage;
    }
    @Override protected void restoreForm(){shield=true;restoreAt=0;protection=CombatType.MELEE;offence=CombatType.RANGE;Arrays.fill(received,0);nextSwitch=clock()+27;clearSelection();showProtection();}
    @Override protected void mechanics(){
        if(!shield&&clock()>=restoreAt)shield=true;
        if(getCombatExecutor().getVictim()==null||clock()<nextSwitch)return;
        nextSwitch=clock()+27;animate(10917);getCombatExecutor().setTicks(3);
        CombatType previous=offence;do{offence=CombatType.values()[getRandom().nextInt(3)];}while(offence==previous);clearSelection();
        List<Player> targets=players();if(targets.isEmpty())return;Location tile=targets.get(getRandom().nextInt(targets.size())).getLocation();
        ProjectileManager.sendProjectile(1884,getLocation(),tile,46,10,90,0,0,11);
        for(Player viewer:targets)ActionSender.sendPositionedGraphic(viewer,tile,1883);
        schedule(3,()->{for(Player p:players())if(near(p.getLocation(),tile,1)&&NPCCombatContext.validPair(this,p)){
            Damage d=Damage.getDamage(this,p,CombatType.MAGIC,getRandom().nextInt(270));d.setMaximum(269);
            AdvancedAttack.impact(this,p,CombatType.MAGIC,d,1883);
        }});
    }
    @Override public void loot(Mob killer){int current=getId();setId(spawnId);try{super.loot(killer);}finally{setId(current);}}
}
