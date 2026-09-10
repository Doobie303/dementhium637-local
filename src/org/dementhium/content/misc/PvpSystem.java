package org.dementhium.content.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.dementhium.content.interfaces.ItemsKeptOnDeath;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.misc.GroundItem;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.misc.IconManager;
import org.dementhium.model.player.Player;

/** Custom Wilderness bounties. Mutations run on the existing world tick. */
public final class PvpSystem {
    private static final Properties SETTINGS = loadSettings();
    public static final int EP_INTERVAL = setting("epIntervalTicks", 81, 1, 10000);
    public static final int EP_GAIN = setting("epGain", 2, 1, 100);
    public static final int CREDIT_IDLE_TICKS = setting("damageCreditIdleTicks", 100, 16, 6000);
    private static final int REPEAT_TICKS = setting("repeatRewardTicks", 500, 0, 100000);
    private static final int BASE_CHANCE = setting("baseDropChancePercent", 10, 0, 100);
    private static final int TARGET_ROLLS = setting("targetExtraRolls", 1, 0, 5);
    private static final Map<String, Integer> RECENT = new LinkedHashMap<String, Integer>();
    private PvpSystem() { }

    private static Properties loadSettings() {
        Properties p = new Properties();
        File file = new File("settings/pvp.properties");
        if (file.isFile()) try (FileInputStream input = new FileInputStream(file)) { p.load(input); }
        catch (IOException e) { throw new IllegalStateException("Cannot read PvP settings", e); }
        return p;
    }
    private static int setting(String key, int fallback, int min, int max) {
        int value = Integer.parseInt(SETTINGS.getProperty(key, Integer.toString(fallback)).trim());
        if (value < min || value > max) throw new IllegalArgumentException("Invalid PvP setting: " + key);
        return value;
    }
    public static int clampEp(int ep) { return Math.max(0, Math.min(100, ep)); }
    public static int dropChance(int ep) { return BASE_CHANCE + (100 - BASE_CHANCE) * clampEp(ep) / 100; }
    public static int[] combatRange(Player player) {
        if (player.inPVPZone() || player.inSafePk()) return new int[]{3,138};
        int level = player.getSkills().getCombatLevelWithoutSummoning();
        int depth = player.isInWilderness() ? Math.max(0, player.getLocation().getWildernessLevel()) : 0;
        return new int[]{Math.max(3, level-depth), Math.min(138, level+depth)};
    }
    public static boolean inCombatRange(Player a, Player b) {
        if (a.inPVPZone() || b.inPVPZone() || a.inSafePk() || b.inSafePk()) return true;
        int difference = Math.abs(a.getSkills().getCombatLevelWithoutSummoning() - b.getSkills().getCombatLevelWithoutSummoning());
        return difference <= Math.max(0,a.getLocation().getWildernessLevel())
                && difference <= Math.max(0,b.getLocation().getWildernessLevel());
    }
    private static boolean available(Player p) {
        return p != null && p.isOnline() && !p.destroyed() && !p.isDead()
                && p.getConnection() != null && !p.getConnection().isDisconnected()
                && p.isInWilderness() && org.dementhium.model.instance.InstanceAccess.owner(p) == null;
    }
    public static boolean canPair(Player a, Player b) {
        return a != b && available(a) && available(b) && !a.getUsername().equals(b.getUsername())
                && a.target == null && b.target == null && a.targetLikelihood >= 30 && b.targetLikelihood >= 30
                && a.getLocation().getZ() == b.getLocation().getZ() && inCombatRange(a,b);
    }
    public static boolean pair(Player a, Player b) {
        if (!canPair(a,b)) return false;
        a.target=b; b.target=a;
        refreshTarget(a); refreshTarget(b);
        return true;
    }
    public static void findTarget(Player p) {
        if (!available(p) || p.target != null || p.targetLikelihood < 30
                || p.getRandom().nextInt(31) > p.targetLikelihood - 30) return;
        List<Player> candidates = new ArrayList<Player>();
        for (Player other : World.getWorld().getPlayers()) if (canPair(p,other)) candidates.add(other);
        if (!candidates.isEmpty()) pair(p,candidates.get(p.getRandom().nextInt(candidates.size())));
    }
    public static void tick(Player p) {
        p.pvpZoneEp=clampEp(p.pvpZoneEp);
        if (!available(p)) {
            depart(p);
            return;
        }
        if (p.target != null && (!available(p.target) || p.target.target != p
                || !inCombatRange(p,p.target) || p.getLocation().getZ()!=p.target.getLocation().getZ())) {
            clearPair(p,false);
        }
        refreshTarget(p);
    }
    /** Keep the custom immediate departure policy; never penalise the remaining partner. */
    public static void depart(Player p) {
        clearPair(p,false);
        p.targetLikelihood=5;
    }
    private static void clearOne(Player p) {
        Player previous=p.target;
        boolean changed=previous!=null || p.hasTargetArrow;
        p.target=null;
        if (p.hasTargetArrow && previous!=null) IconManager.removeIcon(p,previous);
        p.hasTargetArrow=false;
        if (changed && p.isOnline()) org.dementhium.net.ActionSender.sendString(p,591,8,"None");
    }
    private static void clearPair(Player p, boolean completed) {
        Player other=p.target;
        clearOne(p);
        if (other!=null && other.target==p) {
            clearOne(other);
            if (completed) other.targetLikelihood=5;
        }
        if (completed) p.targetLikelihood=5;
    }
    private static void refreshTarget(Player p) {
        if (p.target!=null && !p.hasTargetArrow)
            p.hasTargetArrow=IconManager.iconOnMob(p,p.target,4,-1)>=0;
        org.dementhium.net.ActionSender.sendString(p,591,8,p.target==null?"None":p.target.getDisplayName());
    }
    /** Capture assignment before area/death ticks can remove it. Fatal damage credit is added later. */
    public static void beginDeath(Player victim) {
        victim.removeAttribute("pvpSettled");
        victim.removeAttribute("deathItemsApplied");
        victim.setAttribute("pvpDeathKiller",victim.getDamageManager().getKiller());
        victim.setAttribute("pvpDeathTarget",victim.target==null?"":victim.target.getUsername());
        victim.setAttribute("pvpDeathWilderness",World.getWorld().getAreaManager()!=null && victim.isInWilderness());
        victim.setAttribute("pvpDeathLocation",victim.getLocation());
        victim.setAttribute("pvpDeathLastHitter",victim.getCombatExecutor().getLastAttacker());
        clearPair(victim,false);
    }
    public static void finishDeath(Player victim) {
        // Keep the once-per-life guards, but release references to departed players.
        for (String key : new String[]{"pvpDeathKiller","pvpDeathLastHitter","pvpDeathTarget","pvpDeathLocation","pvpDeathWilderness"})
            victim.removeAttribute(key);
    }
    public static int points(int risk, boolean staffVictim, boolean donor, boolean assist) {
        long reward=(assist?2:1)+(staffVictim?(assist?5:10):0)+Math.max(0,risk)/(assist?200000000:100000000);
        // Retain the main donor x2; repair the intended 30% assist bonus with upward rounding.
        if (donor) reward=assist?(reward*130+99)/100:reward*2;
        return (int)Math.min(Integer.MAX_VALUE,reward);
    }
    private static void awardPoints(Player p, Player victim, boolean assist) {
        int risk=ItemsKeptOnDeath.getRiskedWealth(ItemsKeptOnDeath.getDeathContainers(p)[1]);
        int award=points(risk,victim.getRights()>=2,p.getDonor()>0,assist);
        int before=p.getPkPoints();
        p.addPkPoints(award);
        p.sendMessage("You have been awarded "+(p.getPkPoints()-before)+" pk points. You now have "+p.getPkPoints()+".");
    }
    private static String pairKey(Player a, Player b) {
        String x=a.getUsername(),y=b.getUsername();
        return x.compareTo(y)<0?x+"\n"+y:y+"\n"+x;
    }
    public static boolean rewardEligible(Player a, Player b) {
        int now=World.getTicks();
        Iterator<Map.Entry<String,Integer>> it=RECENT.entrySet().iterator();
        while(it.hasNext()) if((long)now-it.next().getValue()>=REPEAT_TICKS) it.remove();
        String key=pairKey(a,b);
        if (RECENT.containsKey(key)) return false;
        // Bounded, fail closed for extra rewards when the table is saturated.
        if (RECENT.size()>=8192) return false;
        RECENT.put(key,now);
        return true;
    }
    public static void rewardDeath(Player victim, Player killer, Player lastHitter) {
        if (victim==null || killer==null || victim==killer || victim.getUsername().equals(killer.getUsername())
                || victim.getAttribute("pvpSettled",false)) return;
        victim.setAttribute("pvpSettled",true);
        if (!victim.getAttribute("pvpDeathWilderness",victim.isInWilderness())) return;
        boolean targetKill=killer.getUsername().equals(victim.getAttribute("pvpDeathTarget",""))
                || victim.target==killer && killer.target==victim;
        victim.incrementPkDeaths(); killer.incrementPkKills();
        clearPair(victim,targetKill);
        if (targetKill) killer.targetLikelihood=5;
        if (!rewardEligible(killer,victim)) {
            killer.sendMessage("This recent opponent gives no extra PK points or EP loot. Their ordinary loot is unchanged.");
            return;
        }
        awardPoints(killer,victim,false);
        if (lastHitter!=null && lastHitter!=killer && lastHitter!=victim && lastHitter.isOnline()
                && !lastHitter.getUsername().equals(killer.getUsername()) && rewardEligible(lastHitter,victim))
            awardPoints(lastHitter,victim,true);
        int ep=clampEp(killer.pvpZoneEp);
        int rolls=(killer.getRandom().nextInt(100)<dropChance(ep)?1:0)+(targetKill?TARGET_ROLLS:0);
        Location location=victim.getAttribute("pvpDeathLocation",victim.getLocation());
        // Claim/debit once before publishing any bonus; recipient/location are explicit.
        killer.pvpZoneEp=0;
        killer.getPlayerArea().refreshPvpStrings();
        for (int i=0;i<rolls;i++) {
            Item item=new Item(Player.PVPItems[killer.getRandom().nextInt(Player.PVPItems.length)],1);
            GroundItemManager.createGroundItem(new GroundItem(killer,item,location,false,killer.getRights()>=2,GroundItemManager.groundItemIndex++));
        }
        if (targetKill) killer.sendMessage("Bounty complete! You earned "+TARGET_ROLLS+" extra loot roll(s).");
        System.out.println("[PvP reward] victim="+victim.getUsername()+" killer="+killer.getUsername()+" EP="+ep+" target="+targetKill+" rolls="+rolls);
    }
}
