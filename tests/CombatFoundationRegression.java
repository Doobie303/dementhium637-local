import java.util.*;
import org.dementhium.cache.Cache;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.misc.DamageManager;
import org.dementhium.model.misc.DamageManager.*;
import org.dementhium.model.player.*;
import org.dementhium.net.GameSession;
public class CombatFoundationRegression {
 static int checks;
 static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static Player player(String name){Player p=new Player(new GameSession(null),new PlayerDefinition(name,"unused"));p.getSkills().setMaximumLifePoints(1000);p.getSkills().setHitPoints(1000);return p;}
 public static void main(String[] args)throws Exception {
  Cache.init();ItemDefinition.init();NPCDefinition.init();
  Player source=player("attacker");
  int[] absorb=ItemDefinition.forId(1127).getAbsorptionBonus();int[] saved=absorb.clone();
  try {
   absorb[0]=20;absorb[1]=30;absorb[2]=40;
   DamageType[] types={DamageType.MELEE,DamageType.MAGE,DamageType.RANGE};
   int[] expected={440,410,380};
   for(int i=0;i<3;i++)for(int overload=0;overload<6;overload++) {
    Player p=player("victim");p.getEquipment().set(Equipment.SLOT_CHEST,new Item(1127));
    DamageManager dm=p.getDamageManager();Damage d=new Damage(500);
    switch(overload){
     case 0:dm.damage(source,500,500,types[i]);break;
     case 1:dm.damage(source,500,500,types[i],18);break;
     case 2:dm.damage(source,d,types[i]);break;
     case 3:dm.damage(source,d,types[i],18);break;
     case 4:dm.miscDamage(500,types[i]);break;
     default:dm.miscDamage(500,types[i],18);
    }
    DamageHit h=dm.getHits().getLast();
    check(p.getHitPoints()==1000-expected[i],"HP overload "+overload+" "+types[i]);
    check(h.getDamage()==expected[i],"Hitsplat agrees with HP");
    check(h.getPartner().getDamage()==500-expected[i],"Soak partner");
    check(h.getPartner().getDelay()==h.getDelay(),"Partner delay");
    if(overload<4)check(dm.getEnemyHits().get(source)==expected[i],"Actual kill credit");
    else check(dm.getEnemyHits().isEmpty(),"Environment has no kill credit");
    if(overload==2||overload==3)check(d.getHit()==expected[i]&&d.getSoaked()==500-expected[i],"Final Damage result");
   }
   Player p=player("edges");p.getEquipment().set(Equipment.SLOT_CHEST,new Item(1127));
   check(Damage.calculateSoaked(p,200,CombatType.MELEE)==0,"Threshold");
   check(Damage.calculateSoaked(p,204,CombatType.MELEE)==0,"Round down");
   check(Damage.calculateSoaked(p,205,CombatType.MELEE)==1,"First absorbed LP");
   p.getDamageManager().miscDamage(500,DamageType.RED_DAMAGE);
   check(p.getHitPoints()==500,"Untyped damage bypasses absorption");
   p.getDamageManager().miscDamage(100,DamageType.HEAL);
   check(p.getHitPoints()==600&&p.getDamageManager().getHits().getLast().getDamage()==100,"Healing heals");
   p.getDamageManager().miscDamage(-100,DamageType.RED_DAMAGE);
   check(p.getHitPoints()==600,"Negative damage cannot heal");
   p.setAttribute("godmode",true);p.getDamageManager().damage(source,500,500,DamageType.MELEE);
   check(p.getHitPoints()==600,"Godmode immunity");
   p.removeAttribute("godmode");p.setAttribute("hitImmunity",World.getTicks()+100);
   p.getDamageManager().damage(source,new Damage(500),DamageType.MELEE);
   check(p.getHitPoints()==600,"Damage-object immunity");
   p.removeAttribute("hitImmunity");p.getSkills().setHitPoints(1100);
   p.getDamageManager().miscDamage(0,DamageType.RED_DAMAGE);
   check(p.getHitPoints()==1100,"Miss preserves boosted HP");
   p.getDamageManager().miscDamage(10,DamageType.RED_DAMAGE);
   check(p.getHitPoints()==1090,"Damage preserves remaining boosted HP");
   p.getSkills().setHitPoints(1000);p.getEquipment().set(Equipment.SLOT_RING,new Item(2550));p.getSettings().setRecoilDamage(400);
   p.setAttribute("vengeance",true);Damage d=Damage.getDamage(source,p,CombatType.MELEE,500);
   check(p.getSettings().getRecoilDamage()==400,"No recoil charge at launch");
   p.getDamageManager().damage(source,d,DamageType.MELEE);
   check(d.getRecoiled()==44&&d.getVenged()==330,"Reflection uses absorbed damage");
   check(p.getSettings().getRecoilDamage()==356,"Recoil charge at impact");
   source.setAttribute("godmode",true);p.getEquipment().set(Equipment.SLOT_CHEST,null);p.getSkills().setHitPoints(1000);
   p.getDamageManager().damage(source,50,50,DamageType.MELEE);check(p.getHitPoints()==250,"Owner 750 hit preserved");
   source.removeAttribute("godmode");
   p.getDamageManager().clearEnemyHits();p.getSkills().setHitPoints(10);
   p.getDamageManager().damage(source,500,500,DamageType.MELEE);
   check(p.getHitPoints()==0&&p.getDamageManager().getEnemyHits().get(source)==10,"Overkill capped to HP");
  } finally {System.arraycopy(saved,0,absorb,0,3);}
  for(int mode=0;mode<2;mode++) {
   org.dementhium.model.npc.NPC nex=new org.dementhium.model.npc.NPC(13447);nex.setHp(2000);
   if(mode==0)nex.getDamageManager().damage(source,900,900,DamageType.RANGE);else nex.getDamageManager().damage(source,new Damage(900),DamageType.RANGE);
   check(nex.getHitPoints()==1500,"Nex 500 cap, overload "+mode);
   org.dementhium.model.npc.NPC minion=new org.dementhium.model.npc.NPC(13451);minion.setHp(2000);
   minion.getDamageManager().damage(source,900,900,DamageType.MELEE);check(minion.getHitPoints()==2000,"Nex minion shield");
   minion.setAttribute("nex_vulnerable",true);minion.getDamageManager().damage(source,new Damage(900),DamageType.MELEE);
   check(minion.getHitPoints()==1400,"Nex minion 600 cap");
  }
  System.out.println("PASS: "+checks+" shared damage checks");
 }
}
