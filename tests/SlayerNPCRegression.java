import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.npc.NPCLoader;
import org.dementhium.model.npc.impl.SlayerNPC;
import org.dementhium.model.player.*;
import org.dementhium.task.impl.NPCTickTask;
import org.dementhium.tickable.Tick;

/** Registry/executor sessions and actual DamageManager HP/credit, with fixture loot recording only. */
public final class SlayerNPCRegression {
    private static int checks;
    private static final List<Tick> active = new ArrayList<Tick>();
    private static void check(boolean ok, String reason) { checks++; if (!ok) throw new AssertionError(reason); }
    private static Field field(Class<?> owner, String name) throws Exception {
        Field f = owner.getDeclaredField(name); f.setAccessible(true); return f;
    }
    @SuppressWarnings("unchecked") private static void callbacks() throws Exception {
        LinkedList<Tick> queue = (LinkedList<Tick>)field(World.class,"ticksToAdd").get(World.getWorld());
        active.addAll(queue); queue.clear();
        for (Iterator<Tick> it = active.iterator(); it.hasNext();) if (!it.next().run()) it.remove();
    }
    private static class LootSlayer extends SlayerNPC {
        int rewards; Mob paid;
        LootSlayer(int id) { super(id); }
        @Override public void loot(Mob player) { rewards++; paid = player; }
    }
    private static final class Fixture {
        final SlayerNPC npc;
        final Player player;
        Fixture(int id) throws Exception { this(id, false); }
        Fixture(int id, boolean recordLoot) throws Exception {
            CombatFixtures.clearPlayers(); active.clear();
            ((List<?>)field(World.class,"ticksToAdd").get(World.getWorld())).clear();
            npc = recordLoot ? new LootSlayer(id) : (SlayerNPC)NPCLoader.getNPC(id);
            npc.setLocation(Location.locate(3216,3216,0)); npc.setOriginalLocation(npc.getLocation()); npc.setDoesWalk(false);
            npc.getRandom().setSeed(945); player = CombatFixtures.player(npc);
            player.getSkills().set(Skills.SLAYER,99);
            player.getSkills().setMaximumLifePoints(100000); player.getSkills().setHitPoints(100000);
            npc.getCombatExecutor().setVictim(player);
        }
        void clock() throws Exception { field(World.class,"ticksPassed").setInt(null,World.getTicks()+1); callbacks(); }
        void step() throws Exception { clock(); npc.getMask().reset(); new NPCTickTask(npc).execute(); }
        void steps(int n) throws Exception { for (int i=0;i<n;i++) step(); }
        void prayer(CombatType type) throws Exception {
            boolean[][] p = (boolean[][])field(player.getPrayer().getClass(),"onPrayers").get(player.getPrayer());
            p[0][type.getProtectionPrayer()] = true;
        }
        void gear(int slot,int id) { player.getEquipment().set(slot,id<0?null:new Item(id)); player.getBonuses().calculate(); }
    }
    private static void registryAndGates() throws Exception {
        int[][] groups = {{60,1604,1605,1606,1607,7801,7802,7803,7804},{15,1612,7786},{40,1616},
                {50,1618,6215,7642,7643},{25,1620},{75,1610},{52,1637,1638,1639,1640,1641,1642,7459,7460},
                {70,1608,1609},{30,1633,1634,1635,1636,6216},{55,1623,1626,1627,1628,1629}};
        for (int[] group:groups) for (int i=1;i<group.length;i++) {
            check(NPCLoader.getNPC(group[i]) instanceof SlayerNPC,"Exact family registry: "+group[i]);
            Fixture f = new Fixture(group[i]); f.player.getSkills().set(Skills.SLAYER,group[0]-1);
            check(!f.npc.isAttackable(f.player),"Alias cannot bypass Slayer gate");
            f.player.getSkills().set(Skills.SLAYER,group[0]); check(f.npc.isAttackable(f.player),"Slayer boundary admitted");
        }
        check(!(NPCLoader.getNPC(7789) instanceof SlayerNPC),"Unverified boss variant excluded");
    }
    private static void contactMagic() throws Exception {
        for (int id:new int[]{1618,6215,7642,1637,7459,1633,6216,1612,7786}) for (CombatType prayer:new CombatType[]{CombatType.MELEE,CombatType.MAGIC}) {
            Fixture f = new Fixture(id); f.gear(Equipment.SLOT_HAT,15492); f.prayer(prayer);
            f.player.getSkills().set(Skills.MAGIC,1); f.player.getSkills().set(Skills.DEFENCE,1);
            int first=-1,last=-1,launches=0;
            for (int tick=1;tick<=120;tick++) {
                f.step();
                if (f.npc.getMask().getLastAnimation()!=null) {
                    if (first<0) first=tick;
                    if (last>=0) check(tick-last==f.npc.getAttackDelay(),"Contact-magic natural cadence");
                    last=tick; launches++;
                }
            }
            check(first==3&&launches>5,"Natural contact-magic launch path");
            check(prayer==CombatType.MELEE?f.player.getHitPoints()==100000:f.player.getHitPoints()<100000,
                    "Contact magical accuracy still uses MELEE protection: "+id);
        }
        Fixture f = new Fixture(1618);
        int[] bonus=(int[])field(Bonuses.class,"bonuses").get(f.player.getBonuses());
        int[] successes=new int[2];
        for(int mode=0;mode<2;mode++) {
            bonus[Bonuses.MAGIC_DEFENCE]=mode==0?0:500;
            f.npc.getRandom().setSeed(44);f.player.getRandom().setSeed(55);
            for(int i=0;i<2000;i++)if(SlayerNPC.magicMeleeDamage(f.npc,f.player,100)>=0)successes[mode]++;
        }
        check(successes[0]>successes[1]*3,"Magic defence materially reduces contact accuracy");
        Fixture approach=new Fixture(6216); Location start=approach.npc.getLocation();
        approach.player.setLocation(start.transform(8,0,0)); approach.steps(3);
        check(approach.player.getDamageManager().getHits().isEmpty(),"Pyrefiend cannot cast at range");
        approach.steps(12); check(!approach.npc.getLocation().equals(start)&&!approach.player.getDamageManager().getHits().isEmpty(),"Native pursuit reaches contact before hitting");
    }
    private static void equipment() throws Exception {
        int[][] cases={{1612,Equipment.SLOT_HAT,4166},{7786,Equipment.SLOT_HAT,13277},{1604,Equipment.SLOT_HAT,4168},
                {7804,Equipment.SLOT_HAT,15492},{1616,Equipment.SLOT_SHIELD,4156},{1620,Equipment.SLOT_SHIELD,4156}};
        for(int[] c:cases)for(int mode=0;mode<4;mode++) {
            Fixture f=new Fixture(c[0]); f.prayer(c[0]==1604||c[0]==7804?CombatType.MAGIC:CombatType.MELEE);
            f.steps(3); // Protection is deliberately equipped after the attack has launched.
            if(mode==1)f.gear(c[1],c[2]);
            if(mode==2)f.player.setAttribute("godmode",true);
            if(mode==3)f.npc.resetCombatState();
            f.step();
            check(mode==0?f.player.getSkills().getLevel(Skills.ATTACK)<99:f.player.getSkills().getLevel(Skills.ATTACK)==99,
                    "Protection, immunity and cancellation evaluated at actual contact");
            if(mode==0)check(f.player.getHitPoints()<100000,"Missing gear pierces prayer with bounded penalty");
        }
        Fixture mighty=new Fixture(7786); mighty.gear(Equipment.SLOT_HAT,4166);mighty.steps(4);
        check(mighty.player.getSkills().getLevel(Skills.ATTACK)<99,"Mighty banshee requires masked earmuffs/full helmet");
    }
    private static void weaponFilters() throws Exception {
        for(int id:new int[]{1608,1627}) {
            Fixture f=new Fixture(id); f.npc.setHp(1000);
            f.gear(Equipment.SLOT_WEAPON,13290);
            Damage allowed=Damage.getDamage(f.player,f.npc,CombatType.MELEE,100);
            f.gear(Equipment.SLOT_WEAPON,4151);
            f.npc.getDamageManager().damage(f.player,allowed,DamageType.MELEE);
            check(f.npc.getHp()==900,"Leaf weapon is captured at launch");
            f.npc.getDamageManager().damage(f.player,allowed,DamageType.MELEE);
            check(f.npc.getHp()==900,"Permitted hit cannot replay");
            f.npc.getDamageManager().damage(f.player,Damage.getDamage(f.player,f.npc,CombatType.MELEE,100),DamageType.MELEE);
            for(DamageType type:new DamageType[]{DamageType.MELEE,DamageType.RANGE,DamageType.MAGE,DamageType.POSION,DamageType.DEFLECT})
                f.npc.getDamageManager().damage(f.player,100,100,type);
            check(f.npc.getHp()==900&&f.npc.getDamageManager().getEnemyHits().get(f.player)==100,"Unsupported typed/raw/poison/reflection hits earn no damage or credit");
            f.gear(Equipment.SLOT_WEAPON,9185); f.gear(Equipment.SLOT_ARROWS,13280);
            Damage broad=Damage.getDamage(f.player,f.npc,CombatType.RANGE,100);
            f.gear(Equipment.SLOT_ARROWS,-1);
            f.npc.getDamageManager().damage(f.player,broad,DamageType.RANGE);
            check(f.npc.getHp()==800,"Consumed broad ammunition still permits its launched hit");
            f.gear(Equipment.SLOT_ARROWS,4160);
            f.npc.getDamageManager().damage(f.player,Damage.getDamage(f.player,f.npc,CombatType.RANGE,100),DamageType.RANGE);
            check(f.npc.getHp()==800,"Mismatched broad arrow and crossbow cannot bypass restriction");
            f.gear(Equipment.SLOT_WEAPON,4158);
            Damage stale=Damage.getDamage(f.player,f.npc,CombatType.MELEE,100);
            f.npc.resetCombatState(); f.npc.getDamageManager().damage(f.player,stale,DamageType.MELEE);
            check(f.npc.getHp()==800,"Reset invalidates launched leaf-hit permission");
            f.npc.getPoisonManager().poison(f.player,20);
            for(int tick=0;tick<31;tick++)f.clock();
            check(f.npc.getHp()==800,"Actual poison callback cannot bypass leaf-only damage");
        }
    }
    private static void deathCancellation() throws Exception {
        Fixture attacker=new Fixture(7804,true);attacker.steps(3);
        attacker.npc.getDamageManager().damage(null,attacker.npc.getHp(),attacker.npc.getHp(),DamageType.MELEE);
        check(attacker.npc.isDead(),"Actual source death begins before queued spectre contact");
        attacker.steps(5);
        check(attacker.player.getHitPoints()==100000&&attacker.player.getSkills().getLevel(Skills.ATTACK)==99,
                "Death cancels queued Slayer damage and missing-gear drain together");
        Fixture victim=new Fixture(1623,true);victim.npc.getCombatExecutor().reset();
        victim.gear(Equipment.SLOT_WEAPON,4158);victim.npc.setHp(1);
        Damage stale=Damage.getDamage(victim.player,victim.npc,CombatType.MELEE,100);
        victim.npc.getDamageManager().damage(victim.player,Damage.getDamage(victim.player,victim.npc,CombatType.MELEE,1),DamageType.MELEE);
        check(victim.npc.isDead(),"Permitted leaf contact starts ordinary death");
        victim.steps(victim.npc.getDeathTick()+60);
        check(!victim.npc.isDead(),"Leaf-only creature respawns normally");
        victim.npc.getDamageManager().damage(victim.player,stale,DamageType.MELEE);
        check(victim.npc.getHp()==victim.npc.getMaxHp(),"Old-life approved damage cannot strike respawn");
    }
    private static void spells() throws Exception {
        SpellContainer.initialize();
        for(int spell:new int[]{25,56}) {
            Fixture f=new Fixture(1626);f.npc.getCombatExecutor().reset();f.npc.setHp(10000);
            f.gear(Equipment.SLOT_WEAPON,4170);f.player.getSkills().set(Skills.SLAYER,99);
            f.player.getInventory().addItem(556,1000);f.player.getInventory().addItem(558,1000);f.player.getInventory().addItem(560,1000);
            f.player.getSettings().setSpellBook(192);f.player.setAttribute("autocastId",spell);
            f.player.getCombatExecutor().setVictim(f.npc);f.player.getRandom().setSeed(7);
            for(int tick=0;tick<80;tick++){f.clock();f.player.getCombatExecutor().tick();}
            check(spell==56?f.npc.getHp()<10000:f.npc.getHp()==10000,"Native spell sessions permit Magic Dart only");
            check(!f.player.getInventory().contains(new Item(558,1000)),"Native cast consumed its runes");
        }
    }
    private static void ranged() throws Exception {
        for(int[] equipment:new int[][]{{9185,13280},{861,4160},{9185,9143},{861,892}}) {
            Fixture f=new Fixture(1609);f.npc.getCombatExecutor().reset();f.npc.setHp(10000);
            f.player.setLocation(f.npc.getLocation().transform(6,0,0));
            f.gear(Equipment.SLOT_WEAPON,equipment[0]);
            f.player.getEquipment().set(Equipment.SLOT_ARROWS,new Item(equipment[1],100));f.player.getBonuses().calculate();
            f.player.getCombatExecutor().setVictim(f.npc);f.player.getRandom().setSeed(7);
            for(int tick=0;tick<80;tick++){f.clock();f.player.getCombatExecutor().tick();}
            boolean broad=equipment[1]==4160||equipment[1]==13280;
            check(broad?f.npc.getHp()<10000:f.npc.getHp()==10000,"Native ranged sessions damage leaf-only targets with broad ammo only");
            check(f.player.getEquipment().get(Equipment.SLOT_ARROWS).getAmount()<100,"Native ranged launch consumes real ammunition");
        }
    }
    private static void gargoyle() throws Exception {
        for(boolean typed:new boolean[]{false,true}) {
            Fixture f=new Fixture(1610,true);f.npc.setHp(100);
            Damage pending=Damage.getDamage(f.player,f.npc,CombatType.MELEE,120);
            if(typed)f.npc.getDamageManager().damage(f.player,pending,DamageType.MELEE);
            else f.npc.getDamageManager().damage(f.player,120,120,DamageType.POSION);
            check(f.npc.getHp()==1&&!f.npc.isDead()&&f.npc.getDamageManager().getEnemyHits().get(f.player)==99,"Missing hammer clamps real HP and credit at impact");
            f.npc.getDamageManager().damage(f.player,10,10,DamageType.DEFLECT);
            check(f.npc.getHp()==1&&f.npc.getDamageManager().getEnemyHits().get(f.player)==99,"Unfinishable one HP cannot create phantom credit");
            f.npc.getDamageManager().damage(null,20,20,DamageType.HEAL);
            check(f.npc.getHp()==21,"Finisher filter preserves healing");
            Damage finish=Damage.getDamage(f.player,f.npc,CombatType.MELEE,100);
            f.player.getInventory().addItem(4162,1); // Carried at impact, including after launch.
            f.npc.getDamageManager().damage(f.player,finish,DamageType.MELEE);
            check(f.npc.isDead(),"Carried hammer permits automatic lethal finish");
            f.steps(f.npc.getDeathTick());LootSlayer n=(LootSlayer)f.npc;
            check(n.rewards==1&&n.paid==f.player&&n.isHidden(),"Ordinary finisher pays one correctly credited reward");
            f.steps(60);check(!n.isDead()&&n.getHp()==n.getMaxHp()&&n.rewards==1,"Ordinary respawn follows finisher exactly once");
        }
    }
    public static void main(String[] args)throws Exception {
        CombatFixtures.init();org.dementhium.model.misc.GroundItemManager.load();
        registryAndGates();contactMagic();equipment();weaponFilters();spells();ranged();gargoyle();deathCancellation();
        CombatFixtures.clearPlayers();System.out.println("Slayer NPC combat: "+checks+" checks passed");
    }
}
