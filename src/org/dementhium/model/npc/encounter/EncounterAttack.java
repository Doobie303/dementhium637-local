package org.dementhium.model.npc.encounter;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.npc.godwars.GodWarsAction;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.player.*;
import org.dementhium.net.ActionSender;

/** A selection owns its targets; each queued impact also owns its source and victim life. */
public final class EncounterAttack extends CombatAction {
 public enum Kind {
  CORP_MELEE(CombatType.MELEE,513,10057,-1,-1),CORP_MAGIC(CombatType.MAGIC,699,10053,1825,-1),CORP_DRAIN(CombatType.MAGIC,499,10053,1823,-1),CORP_SPLIT(CombatType.MAGIC,399,10053,1824,1826),
  SUPREME(CombatType.RANGE,300,2855,475,-1),PRIME(CombatType.MAGIC,610,2854,162,163),REX(CombatType.MELEE,280,2851,-1,-1),
  KQ_MELEE(CombatType.MELEE,314,6241,-1,-1),KQ_RANGE(CombatType.RANGE,314,6240,288,-1),KQ_MAGIC(CombatType.MAGIC,314,6234,280,281),
  WORKER(CombatType.MELEE,30,6223,-1,-1),SPIN_RANGE(CombatType.RANGE,100,2868,475,-1),SPIN_MAGIC(CombatType.MAGIC,100,2868,162,163);
  public final CombatType type;public final int cap,animation,projectile,graphic;
  Kind(CombatType type,int cap,int animation,int projectile,int graphic){this.type=type;this.cap=cap;this.animation=animation;this.projectile=projectile;this.graphic=graphic;}
 }
 private final EncounterNPC npc;private final Kind selected;private boolean launched;
 public EncounterAttack(EncounterNPC npc){this(npc,null);}
 public EncounterAttack(EncounterNPC npc,Kind selected){super(true);this.npc=npc;this.selected=selected;}
 @Override public CombatAction newSession(){return new EncounterAttack(npc,npc.takeAttack());}
 public Kind kind(){return selected==null?npc.prepareAttack():selected;}
 @Override public CombatType getCombatType(){return kind().type;}
 public static Kind choose(EncounterNPC n,Mob victim){
  switch(n.getId()){
  case 8133:if(victim!=null&&GodWarsAction.contact(n,victim)&&n.getRandom().nextBoolean())return Kind.CORP_MELEE;return new Kind[]{Kind.CORP_MAGIC,Kind.CORP_DRAIN,Kind.CORP_SPLIT}[n.getRandom().nextInt(3)];
  case 2881:return Kind.SUPREME;case 2882:return Kind.PRIME;case 2883:return Kind.REX;
  case 1158:case 1160:if(victim!=null&&GodWarsAction.contact(n,victim)&&n.getRandom().nextInt(3)==0)return Kind.KQ_MELEE;return n.getRandom().nextBoolean()?Kind.KQ_MAGIC:Kind.KQ_RANGE;
  case 2892:case 2894:case 2896:return n.getRandom().nextBoolean()?Kind.SPIN_RANGE:Kind.SPIN_MAGIC;
  default:return Kind.WORKER;
  }
 }
 public int maximum(Kind k){int base=npc.getCombatStats().base(Skills.STRENGTH);return k.type!=CombatType.MELEE||base<=0?k.cap:(int)Math.max(1,(long)k.cap*npc.getCombatLevel(Skills.STRENGTH)/base);}
  // Supreme fans barbs through the forward half of the arena, oriented at the primary target.
 private boolean inFront(Player target,Player other){
  double x=npc.getLocation().getX()+(npc.size()-1)/2.0,y=npc.getLocation().getY()+(npc.size()-1)/2.0;
  return (target.getLocation().getX()-x)*(other.getLocation().getX()-x)+(target.getLocation().getY()-y)*(other.getLocation().getY()-y)>=0;
 }
 public static boolean protects(Player p,CombatType t){return p.getPrayer().usingPrayer(0,t.getProtectionPrayer())||p.getPrayer().usingPrayer(1,t.getDeflectCurse());}
 private int roll(Kind k,Player p,int max){
  if(k==Kind.KQ_MAGIC||k==Kind.KQ_RANGE)return npc.getRandom().nextInt(max+1);
  if(k.type==CombatType.MAGIC)return MagicFormulae.getDamage(npc,p,1,max,1);
  boolean hit=k.type==CombatType.MELEE?CombatRolls.hits(npc.getRandom(),MeleeFormulae.getMeleeAccuracy(npc,1),p.getRandom(),MeleeFormulae.getMeleeDefence(npc,p,1)):CombatRolls.hits(npc.getRandom(),RangeFormulae.getAccuracy(npc,1),p.getRandom(),RangeFormulae.getDefence(npc,p,1));
  return hit?npc.getRandom().nextInt(max+1):-1;
 }
 @Override public boolean commenceSession(){
  if(npc.getId()==8127||(npc.getId()>=2891&&npc.getId()<=2896&&(npc.getId()&1)==1)||launched||!interaction.isNPCContextCurrent()||!interaction.getVictim().isPlayer())return false;
  Player primary=interaction.getVictim().getPlayer();Kind k=kind();
  if(!npc.allows(primary)||EncounterNPC.gap(npc,primary)>npc.reach()||!GodWarsAction.clear(npc,primary)||k.type==CombatType.MELEE&&!GodWarsAction.contact(npc,primary))return false;
  launched=true;npc.getCombatExecutor().setTicks(npc.getAttackDelay());int animation=k.animation;
  if(npc.getId()==1160)animation=9454;npc.animate(animation);
  if(k==Kind.CORP_SPLIT){ground(primary);return true;}
  List<Player> targets=new ArrayList<Player>();targets.add(primary);
  if(k==Kind.SUPREME||k==Kind.PRIME||k==Kind.KQ_MAGIC||k==Kind.KQ_RANGE)for(Player p:npc.players())if(p!=primary&&(k!=Kind.SUPREME||inFront(primary,p))&&EncounterNPC.gap(npc,p)<=npc.reach()&&GodWarsAction.clear(npc,p)&&(k!=Kind.PRIME||EncounterNPC.near(p.getLocation(),primary.getLocation(),1)))targets.add(p);
  for(Player p:targets){
   if(!NPCCombatContext.validPair(npc,p))continue;
   int max=maximum(k),raw=roll(k,p,max);boolean corp=k==Kind.CORP_MAGIC||k==Kind.CORP_DRAIN;double protection=corp&&protects(p,CombatType.MAGIC)?0.5:1;
   Damage d=Damage.getDamage(npc,p,k.type,raw<0?-1:(int)(raw*protection),corp);d.setMaximum(max);if(corp&&raw>=0)d.withShieldInput(raw,protection,false);
   if(k==Kind.CORP_DRAIN){final boolean magic=npc.getRandom().nextBoolean();d.onImpact(actual->{
    boolean depleted=magic?p.getSkills().getLevel(Skills.MAGIC)<=0:p.getSkills().getPrayerPoints()<=0;
    int drain=Math.max(1,actual/10);
    if(magic)p.getSkills().decreaseLevelToZero(Skills.MAGIC,drain);else p.getSkills().drainPray(drain);
    // Explicit server rule: an already empty chosen stat converts the drain to LP damage.
    if(depleted&&!p.isDead())p.getDamageManager().damage(npc,Damage.getDamage(npc,p,CombatType.MAGIC,drain,true),DamageType.MAGE);
   });}
   if(k==Kind.SPIN_MAGIC)d.onImpact(actual->p.getSkills().drainPray(1));
   if(k==Kind.SPIN_RANGE&&npc.getRandom().nextInt(4)==0)d.onContact(()->p.getPoisonManager().poison(npc,68));
   int delay=k.projectile<0?1:Math.max(1,(int)(npc.getLocation().distance(p.getLocation())*.3));NPCCombatContext context=new NPCCombatContext(npc,p);
   npc.schedule(delay,()->{if(context.isCurrent()&&npc.allows(p))impact(npc,p,k,d);});if(p==primary)interaction.setDamage(d);
   if(k.projectile>=0)ProjectileManager.sendProjectile(Projectile.create(npc,p,k.projectile,30,32,52,80,3,11));
  }
  return true;
 }
 private void ground(Player target){
  final Location center=target.getLocation();final long life=npc.getCombatGeneration();
  ProjectileManager.sendProjectile(1824,npc.getLocation(),center,52,0,150,0,0,11);
  for(Player viewer:npc.players())ActionSender.sendPositionedGraphic(viewer,center,1826);
  npc.schedule(3,()->{if(npc.isDead()||npc.getCombatGeneration()!=life)return;groundHit(center,1,399);for(int[] d:new int[][]{{-2,0},{2,0},{0,2}}){Location tile=center.transform(d[0],d[1],0);if(!npc.contains(tile))continue;for(Player viewer:npc.players())ActionSender.sendPositionedGraphic(viewer,tile,1826);npc.schedule(2,()->groundHit(tile,0,150));}});
  interaction.setDamage(new Damage(0));interaction.getDamage().setMaximum(399);
 }
 private void groundHit(Location tile,int radius,int max){
  for(Player p:npc.players())if(EncounterNPC.near(p.getLocation(),tile,radius)&&NPCCombatContext.validPair(npc,p)&&GodWarsAction.clear(npc,p)){int raw=npc.getRandom().nextInt(max+1);double protection=protects(p,CombatType.MAGIC)?.5:1;Damage d=Damage.getDamage(npc,p,CombatType.MAGIC,(int)(raw*protection),true).withShieldInput(raw,protection,false);d.setMaximum(max);impact(npc,p,Kind.CORP_SPLIT,d);}
 }
 public static void impact(EncounterNPC npc,Player p,Kind k,Damage d){
  if(d.getHit()<0||!NPCCombatContext.validPair(npc,p)||!npc.allows(p))return;
  if(k.graphic>=0)p.graphics(k.graphic);p.animate(p.getDefenceAnimation());p.getDamageManager().damage(npc,d,k.type.getDamageType());
  p.retaliate(npc);
 }
 @Override public boolean executeSession(){return true;}
 @Override public boolean endSession(){return true;}
}
