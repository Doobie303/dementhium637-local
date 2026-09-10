package org.dementhium.content.activity.impl.barrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.dementhium.model.Container;
import org.dementhium.model.Item;

/** Six-brother rules, independent of NPC and player lifecycle. */
public final class BarrowsRules {
    private BarrowsRules() {}
    // Intentional private-server loot boost, requested by the owner.
    private static final double EQUIPMENT_CHANCE_MULTIPLIER = 1.5;
    private static final int SUPPLY_STACK_PERCENT = 150;
    private static final int COIN_STACK_PERCENT = 125;

    private static int equipmentRollDenominator(int brothers) {
        return (int)Math.ceil((450 - 58 * brothers) / EQUIPMENT_CHANCE_MULTIPLIER);
    }

    private static int boostedStack(int id, int amount) {
        int percent = id == 995 ? COIN_STACK_PERCENT
                : id == 558 || id == 562 || id == 560 || id == 565 || id == 4740 ? SUPPLY_STACK_PERCENT : 100;
        return (amount * percent + 99) / 100;
    }
    public static final int PRAYER_INTERVAL = 30; // 18 seconds at 600 ms/tick.
    public static final int[] BROTHERS = {2030, 2026, 2025, 2027, 2028, 2029};
    private static final int[][] EQUIPMENT = {
        {4753,4755,4757,4759}, {4716,4718,4720,4722},
        {4708,4710,4712,4714}, {4724,4726,4728,4730},
        {4732,4734,4736,4738}, {4745,4747,4749,4751}
    };
    public static int index(int id) {
        for (int i=0;i<BROTHERS.length;i++) if(BROTHERS[i]==id) return i;
        return -1;
    }
    public static long gateKey(int id, int x, int y, int plane) {
        return ((long)id << 30) | ((long)(x & 16383) << 16)
                | ((long)(y & 16383) << 2) | (plane & 3);
    }
    public static org.dementhium.model.Location crossingDestination(
            org.dementhium.model.map.GameObject object, org.dementhium.model.Location origin) {
        int x=object.getLocation().getX(), y=object.getLocation().getY();
        switch(object.getRotation()) {
            case 0: if(origin.getX()>=x)x--; break;
            case 2: if(origin.getX()<=x)x++; break;
            case 1: if(origin.getY()<=y)y++; break;
            case 3: if(origin.getY()>=y)y--; break;
            default: return origin;
        }
        return org.dementhium.model.Location.locate(x,y,object.getLocation().getZ());
    }
    public static int count(List<Integer> killed) {
        int count=0;
        for(int id:BROTHERS) if(killed.contains(id)) count++;
        return count;
    }
    public static int prayerDrain(int killed) { return 8 + Math.max(0, Math.min(6,killed)); }
    public static int dharokMaximum(int hp, int maximumHp) {
        int missing=Math.max(0, maximumHp-hp);
        return 290 + (290 * missing / Math.max(1,maximumHp));
    }
    public static boolean tunnelMonster(int id) { return id>=2031 && id<=2037; }
    public static int addPotential(int potential, int combatLevel) {
        return Math.max(0,Math.min(1000,potential+Math.max(0,combatLevel)));
    }
    /** Stored potential counts tunnel monsters; derive brother levels so old saves also work. */
    public static int rewardPotential(List<Integer> killed, int monsterPotential) {
        int total=Math.max(0,Math.min(1000,monsterPotential));
        for(int id:BROTHERS) if(killed.contains(id)) total+=id==2025||id==2028?98:115;
        return Math.min(1000,total);
    }
    public static List<Item> roll(Random random, List<Integer> killed, int potential) {
        List<Integer> eligible=new ArrayList<Integer>();
        for(int i=0;i<BROTHERS.length;i++) if(killed.contains(BROTHERS[i])) eligible.add(i);
        List<Item> result=new ArrayList<Item>();
        int count=eligible.size();
        int rewardPotential=rewardPotential(killed,potential);
        for(int roll=0;roll<count+1;roll++) {
            if(count>0 && random.nextInt(equipmentRollDenominator(count))==0) {
                int[] set=EQUIPMENT[eligible.get(random.nextInt(count))];
                result.add(new Item(set[random.nextInt(4)],1));
                continue;
            }
            int value=random.nextInt(Math.max(1,rewardPotential+count*2));
            int id, max;
            if(value<380) {id=995;max=Math.max(1,rewardPotential*4);}
            else if(value<505) {id=558;max=(value-380)*4+1;}
            else if(value<630) {id=562;max=(value-505)*2+1;}
            else if(value<755) {id=560;max=value-630+1;}
            else if(value<880) {id=565;max=(value-755)/2+1;}
            else if(value<1005) {id=4740;max=value-880+1;}
            else if(value<1011) {id=random.nextBoolean()?985:987;max=1;}
            else {id=1149;max=1;}
            result.add(new Item(id,boostedStack(id,1+random.nextInt(max))));
        }
        return result;
    }
    public static Container rewards(Random random, List<Integer> killed, int potential) {
        Container result=new Container(28,false);
        for(Item item:roll(random,killed,potential)) result.add(item);
        return result;
    }
}
