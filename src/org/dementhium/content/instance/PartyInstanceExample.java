package org.dementhium.content.instance;

import java.util.IdentityHashMap;
import java.util.Map;
import org.dementhium.model.World;
import org.dementhium.model.instance.GameInstance;
import org.dementhium.model.instance.InstanceAccess;
import org.dementhium.model.instance.InstanceManager;
import org.dementhium.model.instance.InstanceParty;
import org.dementhium.model.player.Player;

/** Admin-only acceptance harness for the party/expansion/respawn APIs. No dungeon gameplay. */
public final class PartyInstanceExample {
    private static final String ATTRIBUTE = "partyInstanceExample";
    private GameInstance instance;
    private InstanceParty party;
    private boolean expanded;
    private final Map<Player,Integer> deaths = new IdentityHashMap<Player,Integer>();
    private PartyInstanceExample() { }
    public static PartyInstanceExample get(Player player) { return player.getAttribute(ATTRIBUTE); }
    public GameInstance getInstance() { return instance; }
    public InstanceParty getParty() { return party; }
    public int getDeaths(Player player) { instance.getManager().checkThread(); return deaths.getOrDefault(player,0); }

    public static PartyInstanceExample start(InstanceManager manager, Player leader, InstanceParty.LeaderDeparture policy) {
        manager.checkThread();
        if (leader == null || leader.getRights() < 2 || get(leader) != null || InstanceAccess.owner(leader) != null) return null;
        PartyInstanceExample session = new PartyInstanceExample();
        try {
            session.instance = manager.create(16,16,4,leader.getLocation(),map -> {
                map.setActivity("party-example");
                session.instance = map;
                map.copyMap(360,648,0,0,8,8,new int[]{0},new int[]{0});
                session.party = map.createParty(leader,policy);
                map.setRespawnPolicy(player -> map.location(44,19,0), player -> {
                    session.deaths.put(player, session.getDeaths(player)+1);
                    player.sendMessage("You respawn inside the party room. Deaths: "+session.getDeaths(player));
                });
                map.setDepartureHandler(player -> {
                    if (get(player)==session) player.removeAttribute(ATTRIBUTE);
                    session.deaths.remove(player);
                });
            });
            if (session.instance == null) return null;
            session.join(leader);
            return session;
        } catch (RuntimeException failure) {
            if (session.instance != null) session.instance.close();
            throw failure;
        }
    }
    public void join(Player player) {
        instance.getManager().checkThread();
        if (player.getRights()<2 || get(player)!=null) throw new IllegalStateException("Administrator example only");
        party.join(player,instance.location(44,19,0),player.getLocation());
        player.setAttribute(ATTRIBUTE,this); deaths.put(player,0);
    }
    public void expand(Player actor) {
        instance.getManager().checkThread();
        if (!instance.isActive() || party.getLeader()!=actor || !instance.isMember(actor) || actor.isDead())
            throw new IllegalStateException("Only the active leader can expand this example");
        if (expanded) return;
        instance.buildRoom(360,648,8,0,8,8,new int[]{0},new int[]{0});
        expanded = true;
    }
    private static String name(String[] args) {
        StringBuilder name = new StringBuilder();
        for (int n=2;n<args.length;n++) { if(n>2) name.append(' '); name.append(args[n]); }
        return name.toString();
    }
    private static Player target(String[] args) {
        if(args.length<3) throw new IllegalArgumentException("Supply a player name");
        Player target = World.getWorld().getPlayerInServer(name(args));
        if(target==null || target.getRights()<2) throw new IllegalArgumentException("That administrator is not online");
        return target;
    }
    public static void command(Player player, String[] supplied) {
        if(player.getRights()<2) return;
        final String[] args = supplied.clone();
        InstanceManager manager=InstanceManager.getSingleton();
        Runnable action=()->{
            if(player.getRights()<2 || !player.isOnline() || player.getConnection().isDisconnected()) return;
            try {
                if(args.length<2) throw new IllegalArgumentException("Use ::instanceparty create, invite, join, revoke, transfer, kick, expand, room, entry, respawntest, status, leave or close");
                String option=args[1].toLowerCase(java.util.Locale.ROOT);
                PartyInstanceExample session=get(player);
                if(option.equals("create")) {
                    if(args.length>3 || (args.length==3 && !args[2].equalsIgnoreCase("promote") && !args[2].equalsIgnoreCase("close")))
                        throw new IllegalArgumentException("Use ::instanceparty create [promote|close]");
                    if(session!=null) throw new IllegalStateException("You already have an example party");
                    session=start(manager,player,args.length==3 && args[2].equalsIgnoreCase("close")
                            ? InstanceParty.LeaderDeparture.CLOSE_SESSION : InstanceParty.LeaderDeparture.PROMOTE_OLDEST);
                    player.sendMessage(session==null?"Could not create a party room.":"Party room created. Invite another administrator with ::instanceparty invite name.");
                    return;
                }
                if(option.equals("join")) {
                    Player leader=target(args); session=get(leader);
                    if(session==null || session.party.getLeader()!=leader) throw new IllegalStateException("That player is not an example party leader");
                    session.join(player); player.sendMessage("You joined the party room."); return;
                }
                if(session==null) throw new IllegalStateException("You are not in an example party");
                if(option.equals("invite")) {
                    Player target=target(args); session.party.invite(player,target,100);
                    player.sendMessage("Invitation sent for 100 world cycles.");
                    target.sendMessage(player.getUsername()+" invited you to a test party. Use ::instanceparty join "+player.getUsername());
                } else if(option.equals("revoke")) session.party.revoke(player,target(args));
                else if(option.equals("transfer")) session.party.transferLeadership(player,target(args));
                else if(option.equals("kick")) session.party.kick(player,target(args));
                else if(option.equals("leave")) session.party.leave(player);
                else if(option.equals("close")) session.party.close(player);
                else if(option.equals("expand")) { session.expand(player); player.sendMessage("Second room built. Use ::instanceparty room to visit it."); }
                else if(option.equals("entry")) player.teleport(session.instance.location(44,19,0),false);
                else if(option.equals("room")) {
                    if(!session.expanded) throw new IllegalStateException("The leader must expand the room first");
                    player.teleport(session.instance.location(108,19,0),false);
                } else if(option.equals("respawntest")) { player.getSkills().setHitPoints(0); player.getSkills().sendDead(); }
                else if(option.equals("status")) player.sendMessage("Instance "+session.instance.getId()+", leader "+session.party.getLeader().getUsername()
                        +", members "+session.instance.getMemberCount()+"/4, invitations "+session.party.getInvitationCount()+", deaths "+session.getDeaths(player));
                else throw new IllegalArgumentException("Unknown instanceparty option");
            } catch (RuntimeException failure) { player.sendMessage(failure.getMessage()==null?"Party request failed.":failure.getMessage()); }
        };
        if(manager.isCycleThread()) action.run();
        else manager.submit(()->{action.run();return null;}).exceptionally(failure->{System.err.println("Party example request failed: "+failure);return null;});
    }
}
