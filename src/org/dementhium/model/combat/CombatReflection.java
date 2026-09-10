package org.dementhium.model.combat;

import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.instance.InstanceAccess;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.tickable.Tick;

/** One delivery for every resolved typed hit; raw replies cannot recursively reflect. */
public final class CombatReflection {
    private CombatReflection() { }
    public static void deliver(Mob source,Mob victim,Damage damage) {
        if(source==null || victim==null || !damage.claimReflection() || !damage.isReflectionSourceCurrent()
                || source.getHitPoints()<=0 || source.isDead() || !InstanceAccess.canInteract(victim,source))return;
        if(damage.getVenged()>0)vengeance(victim,source,damage.getVenged());
        // Retain separate Deflect/recoil splats and the reflector's credit; no offensive XP.
        if(damage.getDeflected()>0)source.getDamageManager().damage(victim,damage.getDeflected(),-1,DamageType.DEFLECT);
        int recoil=damage.getRecoiled();damage.setRecoiled(0);
        if(recoil>0 && source.getHitPoints()>0 && damage.isReflectionSourceCurrent()){
            int before=source.getHitPoints();
            source.getDamageManager().damage(victim,recoil,-1,DamageType.DEFLECT);
            int applied=Math.min(recoil,Math.max(0,before-source.getHitPoints()));
            damage.setRecoiled(applied);Damage.consumeRecoil(victim,applied);
        }
    }
    public static void vengeance(final Mob reflector,final Mob receiver,final int amount) {
        if(reflector==null || !reflector.isPlayer() || receiver==null || amount<=0
                || !reflector.getAttribute("vengeance",false) || reflector.getHitPoints()<=0
                || receiver.getHitPoints()<=0 || !NPCCombatContext.validPair(reflector,receiver)
                || !InstanceAccess.canInteract(reflector,receiver))return;
        final NPCCombatContext context=new NPCCombatContext(reflector,receiver);
        final Object sourceActivity=reflector.getActivity(),targetActivity=receiver.getActivity();
        reflector.setAttribute("vengeance",false);reflector.forceText("Taste vengeance!");
        World.getWorld().submit(new Tick(1){@Override public void execute(){
            stop();
            if(!context.isCurrent() || reflector.getHitPoints()<=0 || receiver.getHitPoints()<=0
                    || !reflector.getPlayer().isOnline() || receiver.isPlayer()&&!receiver.getPlayer().isOnline()
                    || reflector.getActivity()!=sourceActivity || receiver.getActivity()!=targetActivity
                    || !InstanceAccess.canInteract(reflector,receiver))return;
            receiver.getDamageManager().damage(reflector,amount,-1,DamageType.RED_DAMAGE);
        }});
    }
}
