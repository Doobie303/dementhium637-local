import org.dementhium.cache.format.CacheNPCDefinition;
import org.dementhium.content.skills.summoning.Familiar;
import org.dementhium.content.skills.summoning.SummoningPouch;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.impl.npc.npcspecs.SteelTitanAction;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;

/** Production construction, commands, retaliation and successive presentation ticks. */
public final class FamiliarCombatDefinitionRegression {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static NPC target(){NPC n=new NPC(1);n.setLocation(Location.locate(3200,3600,0));return n;}
    private static Familiar familiar(Player owner,int id){Familiar f=new Familiar(owner,id,140);f.setLocation(owner.getLocation().transform(2,0,0));return f;}
    private static Object field(Object target,String name)throws Exception{java.lang.reflect.Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    /** Cancelling one delayed magic impact must not shorten the next session. */
    private static void steelMagicCancellation()throws Exception{
        NPC target=new NPC(1);target.setLocation(Location.locate(3210,3200,0));target.setAttribute("fightcaves",true);target.setHp(100000);
        Player owner=CombatFixtures.player(target);owner.setAttribute("inFightCaves",true);
        Familiar titan=new Familiar(owner,7343,1000);owner.setFamiliar(titan);titan.setAttribute("fightcaves",true);titan.setLocation(Location.locate(3200,3200,0));
        titan.getRandom().setSeed(0);titan.getCombatExecutor().setVictim(target);
        java.lang.reflect.Field clock=World.class.getDeclaredField("ticksPassed");clock.setAccessible(true);
        Interaction firstMagic=null;boolean overlap=false,delaying=false;int tick=0;
        for(tick=1;tick<=30;tick++){
            clock.setInt(null,tick);target.getMask().reset();titan.getCombatExecutor().tick();
            java.util.List<?> pending=(java.util.List<?>)field(titan.getCombatExecutor(),"currentActions");
            overlap|=pending.size()>1;
            for(Object queued:pending){CombatAction action=(CombatAction)field(queued,"action");if(firstMagic==null&&action.getCombatType()==CombatType.MAGIC)firstMagic=(Interaction)field(queued,"interaction");}
            if(target.getMask().getLastGraphics()!=null&&target.getMask().getLastGraphics().getId()==1455){delaying=true;break;}
        }
        check(overlap,"Maximum-range steel flights overlap later natural launches");
        check(delaying&&firstMagic!=null&&firstMagic.getState()==org.dementhium.util.misc.CycleState.FINALIZE,"Magic has reached its separate impact delay");
        int hp=target.getHitPoints();titan.getCombatExecutor().cancelPending();
        check(((java.util.List<?>)field(titan.getCombatExecutor(),"currentActions")).isEmpty(),"Actual executor cancellation removes old sessions");
        clock.setInt(null,++tick);titan.getCombatExecutor().tick();check(target.getHitPoints()==hp,"Cancelled delayed magic cannot land");
        titan.getRandom().setSeed(0);titan.getCombatExecutor().setVictim(target);
        Interaction nextMagic=null;int launch=-1,flight=-1;boolean delayed=false,finished=false;
        for(int end=tick+30;++tick<=end;){
            clock.setInt(null,tick);titan.getCombatExecutor().tick();
            if(nextMagic==null)for(Object queued:(java.util.List<?>)field(titan.getCombatExecutor(),"currentActions")){
                CombatAction action=(CombatAction)field(queued,"action");if(action.getCombatType()==CombatType.MAGIC){nextMagic=(Interaction)field(queued,"interaction");launch=tick;flight=nextMagic.getTicks();break;}
            }
            if(nextMagic==null)continue;
            int endGraphicTick=launch+Math.max(1,flight)+1;
            if(tick==endGraphicTick){check(nextMagic.getState()==org.dementhium.util.misc.CycleState.FINALIZE,"New magic keeps its full delay after cancellation");delayed=true;}
            if(tick==endGraphicTick+1){check(nextMagic.getState()==org.dementhium.util.misc.CycleState.FINISHED,"New magic finishes exactly one tick after its graphic");finished=true;break;}
        }
        check(delayed&&finished,"Replacement magic completed through normal executor timing");titan.getCombatExecutor().cancelPending();
    }
    /** Leaves the executor's initial cooldown and consecutive attack scheduling intact. */
    private static void steelCombat()throws Exception{
        NPC target=new NPC(1);target.setLocation(Location.locate(3200,3200,0));target.setAttribute("fightcaves",true);target.setHp(100000);
        Player owner=CombatFixtures.player(target);owner.setAttribute("inFightCaves",true);
        Familiar titan=new Familiar(owner,7343,1000);owner.setFamiliar(titan);titan.setAttribute("fightcaves",true);
        titan.setLocation(Location.locate(3203,3200,0));target.setLocation(titan.getLocation().transform(titan.size(),0,0));
        titan.getRandom().setSeed(731);
        check(titan.getCombatAction() instanceof SteelTitanAction,"Ordinary steel titan uses its scripted attacks");
        titan.getCombatExecutor().setVictim(target);
        java.lang.reflect.Field clock=World.class.getDeclaredField("ticksPassed");clock.setAccessible(true);
        java.util.Set<Interaction> seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Interaction,Boolean>());
        java.util.Set<CombatType> closeStyles=new java.util.HashSet<CombatType>();int farAttacks=0;
        for(int tick=1;tick<=140;tick++){
            if(tick==81)target.setLocation(titan.getLocation().transform(titan.size()+4,0,0));
            clock.setInt(null,tick);titan.getCombatExecutor().tick();
            for(Object pending:(java.util.List<?>)field(titan.getCombatExecutor(),"currentActions")){
                Interaction interaction=(Interaction)field(pending,"interaction");if(!seen.add(interaction))continue;
                CombatAction action=(CombatAction)field(pending,"action");check(action instanceof SteelTitanAction,"Executor launched steel script");
                CombatType type=action.getCombatType();
                if(tick<=80)closeStyles.add(type);else{check(type!=CombatType.MELEE,"Distant steel target never receives unreachable melee");farAttacks++;}
                Object attack=field(action,"attack");org.dementhium.model.Projectile projectile=(org.dementhium.model.Projectile)field(attack,"projectile");
                check(type==CombatType.MELEE?projectile==null:projectile!=null&&projectile.getProjectileId()>0,"Selected steel style has appropriate projectile");
            }
        }
        check(closeStyles.size()==3,"Consecutive ordinary steel attacks select all three existing styles");check(farAttacks>=10,"Consecutive distant attacks continue");
        owner.getInventory().addItem(12825,2);owner.getCombatExecutor().setVictim(target);
        int points=titan.getSpecialPoints(),cost=titan.getSpecialCost();titan.specialMove(target);boolean scroll=false;
        for(int tick=141;tick<=152;tick++){
            clock.setInt(null,tick);titan.getCombatExecutor().tick();
            for(Object pending:(java.util.List<?>)field(titan.getCombatExecutor(),"currentActions")){
                Interaction interaction=(Interaction)field(pending,"interaction");
                if(interaction.getTargets()!=null&&interaction.getTargets().size()==4)scroll=true;
            }
        }
        check(scroll,"Existing steel scroll still launches four damage targets");
        check(owner.getInventory().getContainer().getItemCount(12825)==1,"Steel scroll consumed once");
        check(titan.getSpecialPoints()==points-cost,"Steel scroll retains special point cost");
    }
    public static void main(String[] args)throws Exception{
        CombatFixtures.init();NPC target=target();Player owner=CombatFixtures.player(target);
        owner.setAttribute("inFightCaves",true);target.setAttribute("fightcaves",true);
        check(SummoningPouch.HYDRA_POUCH.getNpcId()==6811,"Hydra pouch uses genuine base NPC");
        Familiar migrated=familiar(owner,9488);
        check(migrated.getId()==6811&&migrated.getDefinition().getId()==6811,"Saved Hydra ID migrates both identity and definition");
        check(migrated.getPouchId()==12025,"Migrated Hydra retains its pouch");
        int[] skilling={6808,6851,6824,6991,6817};
        int combatRoles=0;
        for(SummoningPouch pouch:SummoningPouch.values()){
            Familiar f=familiar(owner,pouch.getNpcId());boolean skill=false;
            for(int id:skilling)if(id==pouch.getNpcId())skill=true;
            check(f.isCombatFamiliar()!=skill,"Pouch role and valid cache combat pair "+pouch);
            if(f.isCombatFamiliar())combatRoles++;
            if(args.length>0 && "profiles".equals(args[0])){
                boolean style=f.getDefinition().isUsingMelee()||f.getDefinition().isUsingRange()||f.getDefinition().isUsingMagic();
                check(style!=skill,"Effective loaded familiar style "+pouch);
            }
        }
        check(combatRoles==73,"Supported roster contains 73 combat and five skilling familiars");
        for(int id:skilling){
            Familiar f=familiar(owner,id);f.setAttribute("fightcaves",true);owner.setFamiliar(f);
            check(!f.isCombatFamiliar(),"Skill-only role "+id);
            check(!f.canCastAttack(target),"Skill-only command blocked "+id);
            f.retaliate(target);check(f.getCombatExecutor().getVictim()==null,"Skill-only retaliation blocked "+id);
            owner.getCombatExecutor().setVictim(target);
            f.getCombatExecutor().setVictim(target);
            for(int tick=0;tick<3;tick++){f.tick();check(f.getCombatExecutor().getVictim()==null,"Skill-only assist blocked "+id);check(f.getMask().getSwitchId()==id,"Skill-only presentation remains correct "+id);}
            owner.getCombatExecutor().reset();
        }
        for(int id:new int[]{6829,6811,6873}){
            Familiar f=familiar(owner,id);f.setAttribute("fightcaves",true);owner.setFamiliar(f);
            check(f.isCombatFamiliar(),"Fighter/BoB role "+id);
            check(f.isBeastOfBurden()==(id==6873),"Existing BoB classification "+id);
            check(f.canCastAttack(target)==(id!=6873),"Existing attack command policy "+id);
            if(id==6873){owner.getSettings().setAutoRetaliate(false);f.retaliate(target);check(f.getCombatExecutor().getVictim()==null,"BoB respects disabled owner retaliation");}
            owner.getSettings().setAutoRetaliate(true);f.retaliate(target);
            check(f.getCombatExecutor().getVictim()==target,"Combat familiar retaliation retained "+id);
            f.getCombatExecutor().reset();
            for(int tick=0;tick<3;tick++){f.tick();check(f.getMask().getSwitchId()==id+1,"Combat form stable across successive ticks "+id);}
            CacheNPCDefinition shape=CacheNPCDefinition.forID(f.getMask().getSwitchId());
            check(shape.name.equals(CacheNPCDefinition.forID(id).name),"Combat form matches familiar identity "+id);
            owner.setLocation(Location.locate(3200,3200,0));f.setLocation(owner.getLocation().transform(2,0,0));f.tick();
            check(f.getMask().getSwitchId()==id,"Leaving combat area restores base form "+id);
            owner.setLocation(Location.locate(3201,3600,0));
        }
        steelCombat();steelMagicCancellation();CombatFixtures.clearPlayers();System.out.println("Familiar combat definition regression: "+checks+" checks passed");
    }
}
