import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.map.Region;
import org.dementhium.model.map.path.ProjectilePathFinder;
import org.dementhium.model.npc.*;
import org.dementhium.model.npc.encounter.*;
import org.dementhium.model.npc.godwars.*;
import org.dementhium.model.npc.impl.*;
import org.dementhium.model.player.*;
import org.dementhium.task.impl.NPCTickTask;

/** Actual task/executor/queue paths and isolated cache terrain. No server or real drops. */
public class NPCMovementTimingRegression {
 static int checks,cycle,routes;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static void set(Object o,Class<?> c,String key,Object value)throws Exception{Field f=c.getDeclaredField(key);f.setAccessible(true);f.set(o,value);}
 static void clean()throws Exception{CombatFixtures.clearPlayers();cycle=0;set(null,World.class,"ticksPassed",0);Region r=Region.forCoords(3216,3216);r.clippingMasks=new int[4][128][128];r.setClipped(true);}
 static NPC npc(int id,int z){NPC n=NPCLoader.getNPC(id);n.setLocation(Location.locate(3216,3216,z));n.setOriginalLocation(n.getLocation());n.setDoesWalk(false);
  if(n instanceof EncounterNPC)((EncounterNPC)n).bindArena(3200,3200,3260,3260,z);
  if(n instanceof GodWarsNPC)new GodWarsRoom(((GodWarsNPC)n).getGodWarsType(),3200,3200,3260,3260,z,World::getTicks).attach((GodWarsNPC)n);
  return n;
 }
 static Player player(NPC n){Player p=CombatFixtures.player(n);p.getSkills().setMaximumLifePoints(100000);p.getSkills().setHitPoints(100000);World.getWorld().getPlayers().add(p);return p;}
 static void close(NPC n){if(n instanceof GodWarsNPC)((GodWarsNPC)n).getRoom().close();else n.destroy();}
 static void task(NPC n)throws Exception{set(null,World.class,"ticksPassed",++cycle);n.getMask().reset();new NPCTickTask(n).execute();}
 static void wall(int x,int y,int z,int type,int dir,boolean projectiles)throws Exception{
  Method m=Region.class.getDeclaredMethod("addClippingForVariableObject",int.class,int.class,int.class,int.class,int.class,boolean.class,boolean.class);m.setAccessible(true);m.invoke(null,x,y,z,type,dir,projectiles,true);
 }
 static void unwall(int x,int y,int z,int type,int dir,boolean projectiles){Region.removeClippingForVariableObject(x,y,z,type,dir,projectiles,true);}
 static void route(NPC n,Player p,Location start,int dx,int dy){
  n.setLocation(start);n.getWalkingQueue().reset();p.setLocation(start.transform(dx,dy,0));boolean ready=false;
  for(int tick=0;tick<30;tick++){ready=CombatMovement.combatFollow(n,p,CombatType.MELEE);if(ready)break;Location before=n.getLocation();n.getWalkingQueue().getNextEntityMovement();check(before.distance(n.getLocation())<=2,"Single clipped step");}
  check(ready,"Contact id="+n.getId()+" start="+start+" offset="+dx+","+dy+" ended="+n.getLocation());routes++;
 }
 static void routes()throws Exception{
  for(int z:new int[]{0,2})for(int id:new int[]{1,6260,6247,2883,50,2745}){
   clean();NPC n=npc(id,z);Player p=player(n);
   for(int x=0;x<8;x++)for(int y=0;y<8;y++)for(int[] d:new int[][]{{7,0},{-7,0},{0,7},{0,-7},{7,7},{7,-7},{-7,7},{-7,-7}})route(n,p,Location.locate(3216+x,3216+y,z),d[0],d[1]);
   for(int dx=-7;dx<=7;dx++)for(int dy=-7;dy<=7;dy++)route(n,p,Location.locate(3223,3223,z),dx,dy);
   // A moving target changes sides once, then stops; no fake attacker/cooldown is supplied.
   n.setLocation(Location.locate(3216,3216,z));p.setLocation(Location.locate(3227,3216,z));
   for(int t=0;t<3;t++){CombatMovement.combatFollow(n,p,CombatType.MELEE);n.getWalkingQueue().getNextEntityMovement();}
   Location from=n.getLocation();route(n,p,from,-6,4);
   close(n);
  }
  System.out.println("Pursuit routes completed: "+routes);
 }
 static void taskPursuit()throws Exception{
  // Expected: an ordinary attack request at range reaches a legal launch and HP impact
  // through real tasks, with the normal initial cooldown and no fake last attacker.
  for(int z:new int[]{0,2})for(int id:new int[]{6260,6247,2883,50,2745})for(int[] offset:new int[][]{{7,0},{-7,5},{5,-7},{7,7}}){
   clean();NPC n=npc(id,z);n.setLocation(Location.locate(3223,3223,z));n.setOriginalLocation(n.getLocation());Player p=player(n);
   p.setLocation(n.getLocation().transform(offset[0],offset[1],0));n.getCombatExecutor().setVictim(p);n.getRandom().setSeed(1200+id);
   int launch=-1,impact=-1;
   for(int t=0;t<60&&impact<0;t++){task(n);if(launch<0&&n.getMask().getLastAnimation()!=null)launch=cycle;if(p.getHitPoints()<100000)impact=cycle;}
   check(launch>=3&&impact>launch,"Natural task pursuit launches then impacts id="+id+" plane="+z+" offset="+Arrays.toString(offset)+" launch="+launch+" impact="+impact);
   close(n);
  }
 }
 static void clipping()throws Exception{
  clean();
  for(int z:new int[]{0,2})for(int type:new int[]{0,1,2,3})for(int dir=0;dir<4;dir++)for(boolean projectiles:new boolean[]{false,true}){
   Location a=Location.locate(3220,3220,z);int[][] card={{-1,0},{0,1},{1,0},{0,-1}},diag={{-1,1},{1,1},{1,-1},{-1,-1}};
   int[] d=(type==1||type==3?diag:card)[dir];Location b=a.transform(d[0],d[1],0);
   wall(a.getX(),a.getY(),z,type,dir,projectiles);
   check(!ProjectilePathFinder.clearMeleePath(a,b)&&!ProjectilePathFinder.clearMeleePath(b,a),"Melee wall both directions type="+type+" orientation="+dir);
   check(ProjectilePathFinder.clearPath(a,b)==!projectiles&&ProjectilePathFinder.clearPath(b,a)==!projectiles,"Projectile versus movement wall flags");
   unwall(a.getX(),a.getY(),z,type,dir,projectiles);
   check(ProjectilePathFinder.clearPath(a,b)&&ProjectilePathFinder.clearMeleePath(a,b),"Removed wall opens boundary");
  }
  NPC ordinary=npc(1,0);Player attacker=player(ordinary);attacker.setLocation(ordinary.getLocation().transform(1,0,0));
  wall(attacker.getLocation().getX(),attacker.getLocation().getY(),0,0,0,false);
  check(!CombatMovement.combatFollow(attacker,ordinary,CombatType.MELEE),"Player melee cannot fall through projectile-passable wall");
  check(!CombatMovement.combatFollow(ordinary,attacker,CombatType.MELEE),"Ordinary melee respects low wall");
  unwall(attacker.getLocation().getX(),attacker.getLocation().getY(),0,0,0,false);close(ordinary);
  Location a=Location.locate(3220,3220,0),b=a.transform(1,1,0);wall(3220,3220,0,0,2,true);
  check(!ProjectilePathFinder.clearPath(a,b)&&!ProjectilePathFinder.clearPath(b,a),"Diagonal cannot cut a closed corner");unwall(3220,3220,0,0,2,true);
  Region.addClipping(3221,3220,0,256);check(ProjectilePathFinder.clearPath(a,a.transform(3,0,0)),"Low solid cover passes projectiles");check(!ProjectilePathFinder.clearMeleePath(a,a.transform(3,0,0)),"Low solid cover blocks melee");
  Region.addClipping(3221,3220,0,0x20000);check(!ProjectilePathFinder.clearPath(a,a.transform(3,0,0)),"Projectile-solid intermediate tile");Region.removeClipping(3221,3220,0,256|0x20000);
  check(!ProjectilePathFinder.clearPath(a,Location.locate(3220,3220,2)),"Cross-plane ray");
  // Check symmetry for every octant with mixed boundary and solid obstacles.
  wall(3220,3220,0,2,1,true);Region.addClipping(3218,3222,0,0x20000);
  for(int x=-5;x<=5;x++)for(int y=-5;y<=5;y++){Location end=a.transform(x,y,0);check(ProjectilePathFinder.clearPath(a,end)==ProjectilePathFinder.clearPath(end,a),"Symmetric ray "+x+","+y);}
  clean();NPC n=npc(2883,2);Player p=player(n);p.setLocation(n.getLocation().transform(n.size()+4,n.size()+4,0));Location before=n.getLocation();
  Region.addClipping(before.getX()+n.size(),before.getY()+n.size(),2,256|0x20000);
  CombatMovement.combatFollow(n,p,CombatType.MELEE);n.getWalkingQueue().getNextEntityMovement();
  check(n.getLocation().equals(before.transform(1,0,0))||n.getLocation().equals(before.transform(0,1,0)),"Blocked diagonal has a legal cardinal fallback");
  close(n);
  // Through the real task, a full edge wall must prevent Rex launch and damage.
  clean();n=npc(2883,0);p=player(n);n.getCombatExecutor().setVictim(p);n.setAttribute("freezeTime",100);
  for(int y=3215;y<=3220;y++)wall(3219,y,0,0,0,true);
  for(int t=0;t<8;t++)task(n);
  check(p.getHitPoints()==100000&&n.getMask().getLastAnimation()==null,"No Rex attack through directional wall");
  close(n);
 }
 static void aggression()throws Exception{
  clean();NPC n=npc(60,0);Player p=player(n);p.setLocation(n.getLocation().transform(4,0,0));for(int skill:new int[]{0,1,2,4,5,6})p.getSkills().setLevelAndXP(skill,1,0);
  boolean launch=false,hit=false;for(int t=0;t<30;t++){task(n);launch|=n.getMask().getLastAnimation()!=null;hit|=p.getHitPoints()<100000;}
  check(launch&&hit,"Natural ordinary aggression reaches first real impact");check(n.getCombatExecutor().getVictim()==p,"Aggressive target retained without artificial last-attacker setup");
  p.setOnline(false);task(n);check(n.getCombatExecutor().getVictim()==null,"Offline target cleared");p.setOnline(true);p.setLocation(n.getLocation().transform(0,0,2));task(n);check(n.getCombatExecutor().getVictim()==null,"Cross-plane acquisition blocked");
  close(n);
  for(String boundary:new String[]{"freeze","invisible","dead","single"}){
   clean();n=npc(60,0);p=player(n);p.setLocation(n.getLocation().transform(4,0,0));for(int skill:new int[]{0,1,2,4,5,6})p.getSkills().setLevelAndXP(skill,1,0);
   if(boundary.equals("freeze")){n.setAttribute("freezeTime",100);n.getCombatExecutor().setVictim(p);}
   if(boundary.equals("invisible"))p.setInvisible(true);
   if(boundary.equals("dead"))p.getSkills().setHitPoints(0);
   if(boundary.equals("single")){NPC other=npc(1,0);p.getCombatExecutor().setLastAttacker(other);p.setAttribute("combatTicks",100);}
   Location before=n.getLocation();for(int t=0;t<6;t++)task(n);check(n.getLocation().equals(before)&&p.getHitPoints()==(boundary.equals("dead")?0:100000),"Preserve aggression boundary "+boundary);close(n);
  }
 }
 static void selection()throws Exception{
  clean();GodWarsNPC n=(GodWarsNPC)npc(6222,0);Player p=player(n);p.setLocation(n.getLocation().transform(n.size()+4,0,0));n.getCombatExecutor().setVictim(p);
  task(n);check(n.prepareAttack()==GodWarsAction.Attack.KREE_MELEE,"Unopposed Kree pursues melee");p.getCombatExecutor().setVictim(n);task(n);GodWarsAction.Attack tornado=n.prepareAttack();
  check(tornado!=GodWarsAction.Attack.KREE_MELEE,"Attacker invalidates unlaunched melee");for(int i=0;i<20;i++)check(n.prepareAttack()==tornado,"Unchanged selection does not reroll");
  GodWarsAction launched=(GodWarsAction)n.getCombatAction().newSession();launched.setInteraction(new Interaction(n,p));check(launched.commenceSession(),"Captured tornado launches");
  p.getCombatExecutor().reset();check(n.prepareAttack()==GodWarsAction.Attack.KREE_MELEE&&launched.attack()==tornado,"Reverse eligibility cannot mutate launched tornado");
  close(n);
  clean();n=(GodWarsNPC)npc(6203,0);p=player(n);n.getCombatExecutor().setVictim(p);boolean[][] prayers=BossBatchOneRegression.prayers(p);prayers[0][19]=true;
  for(int i=0;i<1000&&n.prepareAttack()!=GodWarsAction.Attack.ZAM_SPECIAL;i++)n.takeAttack();check(n.prepareAttack()==GodWarsAction.Attack.ZAM_SPECIAL,"Seeded eligible smash selection");
  prayers[0][19]=false;check(n.prepareAttack()!=GodWarsAction.Attack.ZAM_SPECIAL,"Prayer departure invalidates unlaunched smash");
  Player next=player(n);n.getCombatExecutor().setVictim(next);check(n.prepareAttack()!=GodWarsAction.Attack.ZAM_SPECIAL,"New target uses its own prayer eligibility");close(n);
 }
 static class ClockNPC extends EncounterNPC {
  int outer,nested,loot;boolean started;
  ClockNPC(){super(1);setLocation(Location.locate(3216,3216,0));setOriginalLocation(getLocation());bindArena(3200,3200,3260,3260,0);setDoesWalk(false);}
  @Override public int aggression(){return 0;}
  @Override protected void mechanics(){if(!started){started=true;schedule(2,()->{outer++;schedule(1,()->nested++);});}}
  @Override public void loot(Mob m){loot++;}
 }
 static void clocks()throws Exception{
  clean();AdvancedNPC n=(AdvancedNPC)npc(50,0);Player p=player(n);n.getCombatExecutor().setVictim(p);set(n,AdvancedNPC.class,"selected",AdvancedAttack.Kind.KBD_MELEE);n.getRandom().setSeed(1234);
  task(n);task(n);check(p.getHitPoints()==100000,"Initial cooldown preserves HP");task(n);
  check(n.getMask().getLastAnimation()!=null&&n.getMask().getLastAnimation().getId()==80,"KBD launch through normal initial cooldown");check(p.getHitPoints()==100000,"One-tick melee does not land in launch task");
  task(n);check(p.getHitPoints()<100000,"Melee lands in following task");int hp=p.getHitPoints();task(n);check(p.getHitPoints()==hp,"No replay on following task");close(n);
  // Damage from a player can precede the NPC task in the same World cycle.
  clean();KalphiteQueen queen=(KalphiteQueen)npc(1158,0);Player killer=player(queen);
  queen.getDamageManager().damage(killer,queen.getHitPoints(),-1,org.dementhium.model.misc.DamageManager.DamageType.MELEE);
  new NPCTickTask(queen).execute();check(queen.getId()==1158,"Phase delay cannot consume launch World cycle");
  for(int t=1;t<4;t++){task(queen);check(queen.getId()==1158,"Four complete ticks before KQ form change");}
  task(queen);check(queen.getId()==1160&&!queen.isDead(),"KQ phase completes exactly on fourth later World tick");close(queen);
  clean();ClockNPC c=new ClockNPC();p=player(c);p.setLocation(c.getLocation().transform(10,0,0));task(c);check(c.outer==0,"Mechanics schedule waits");task(c);check(c.outer==0,"Two-tick mechanics no early fire");task(c);check(c.outer==1&&c.nested==0,"Mechanics fires on due tick, nested waits");task(c);check(c.outer==1&&c.nested==1,"Nested one tick fires once");
  final ClockNPC resetClock=c;c.schedule(1,()->resetClock.outer++);c.resetEncounter();task(c);check(c.outer==1,"Reset cancels pending callbacks");close(c);
  clean();c=new ClockNPC();p=player(c);c.started=true;c.getDamageManager().damage(p,c.getHitPoints(),-1,org.dementhium.model.misc.DamageManager.DamageType.MELEE);
  int due=Math.max(1,c.getDeathTick());for(int t=1;t<due;t++){task(c);check(c.loot==0,"Death reward not early");}task(c);check(c.loot==1,"Death reward exact delay");for(int t=due;t<c.respawnDelay();t++)task(c);check(!c.isDead()&&!c.isHidden()&&c.loot==1,"Respawn retains one reward");close(c);
 }
 static void maps()throws Exception{
  // Current real cache landscapes, not zero-mask substitutes: sample walkable approaches
  // and real directional projectile walls near the supported boss spawns.
  clean();check(org.dementhium.util.MapXTEA.loadPackedFile(),"Read existing map keys");Set<Integer> loaded=new HashSet<Integer>();int contacts=0,walls=0;
  for(String line:Files.readAllLines(Paths.get("data/npcs/npcspawns.txt"))){String[] row=line.trim().split("\\s+");if(row.length<4||!row[0].matches("[0-9]+"))continue;int id=Integer.parseInt(row[0]);if(id!=6260&&id!=6247&&id!=2883&&id!=50)continue;
   int x=Integer.parseInt(row[1]),y=Integer.parseInt(row[2]),z=Integer.parseInt(row[3]),region=((x>>6)<<8)|(y>>6);
   if(loaded.add(region))check(org.dementhium.cache.format.LandscapeParser.parseLandscape(region,org.dementhium.util.MapXTEA.getMapKeys().get(region)),"Load real boss landscape "+region);
   NPC n=NPCLoader.getNPC(id);n.setLocation(Location.locate(x,y,z));n.setOriginalLocation(n.getLocation());n.setDoesWalk(false);Player p=player(n);
   for(int dx=-3;dx<=n.size()+2;dx++)for(int dy=-3;dy<=n.size()+2;dy++){
    Location tile=n.getOriginalLocation().transform(dx,dy,0);if((Region.getClippingMask(tile.getX(),tile.getY(),z)&(256|0x200000))!=0)continue;
    n.setLocation(n.getOriginalLocation());n.getWalkingQueue().reset();p.setLocation(tile);boolean ready=false;
    for(int t=0;t<12;t++){if(CombatMovement.combatFollow(n,p,CombatType.MELEE)){ready=true;break;}Location before=n.getLocation();n.getWalkingQueue().getNextEntityMovement();if(!before.equals(n.getLocation()))for(int xx=0;xx<n.size();xx++)for(int yy=0;yy<n.size();yy++)check(ProjectilePathFinder.clearMeleePath(before.transform(xx,yy,0),n.getLocation().transform(xx,yy,0)),"Real footprint never crosses blocked boundary");}
    if(ready)contacts++;
   }
   for(int xx=x-8;xx<=x+8;xx++)for(int yy=y-8;yy<=y+8;yy++)if((Region.getClippingMask(xx,yy,z)&0x10000)!=0){check(!ProjectilePathFinder.clearPath(Location.locate(xx,yy,z),Location.locate(xx-1,yy,z)),"Real cache projectile wall respected");walls++;}
   close(n);CombatFixtures.clearPlayers();
  }
  check(contacts>20&&walls>0,"Real cache movement and directional wall samples exist");System.out.println("Real terrain contacts="+contacts+" projectile wall samples="+walls);
 }
 public static void main(String[] args)throws Exception{CombatFixtures.init();routes();taskPursuit();clipping();aggression();selection();clocks();maps();clean();System.out.println("NPC movement/timing: "+checks+" checks passed");}
}
