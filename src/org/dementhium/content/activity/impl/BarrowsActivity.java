package org.dementhium.content.activity.impl;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.dementhium.content.activity.Activity;
import org.dementhium.content.activity.impl.barrows.BarrowsConstants;
import org.dementhium.content.activity.impl.barrows.BarrowsCrypt;
import org.dementhium.content.activity.impl.barrows.BarrowsTunnels;
import org.dementhium.content.activity.impl.barrows.Gate;
import org.dementhium.content.dialogue.Dialogue;
import org.dementhium.content.dialogue.DialogueType;
import org.dementhium.content.dialogue.OptionAction;
import org.dementhium.content.misc.Following;
import org.dementhium.event.EventListener.ClickOption;
import org.dementhium.event.impl.object.BarrowsTunnelListener;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.map.Directions;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.map.Region;
import org.dementhium.model.map.path.DefaultPathFinder;
import org.dementhium.model.map.path.PrimitivePathFinder;
import org.dementhium.model.misc.IconManager;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Misc;

/**
 * Handles the barrows activity.
 * @author Emperor
 *
 */
public class BarrowsActivity extends Activity<BarrowsCrypt> {

	/**
	 * The main activity interface.
	 */
	public static final int MAIN_INTERFACE = 24;
	
	/**
	 * The {@code Random} instance used.
	 */
	private static final Random RANDOM = new Random();
	
	/**
	 * The barrows tunnels instance we're using.
	 */
	private BarrowsTunnels barrowsTunnels;
	
	/**
	 * The DIALOGUE send to enter the tunnels.
	 */
	private static final Dialogue DIALOGUE = new Dialogue();
	
	/**
	 * Prepare the DIALOGUE.
	 */
	static {
		DIALOGUE.setType(DialogueType.DISPLAY_BOX);
		DIALOGUE.getMessage().add("You find a hidden tunnel, do you want to enter?");
		DIALOGUE.getActions().add(new OptionAction() {
			@Override
			public boolean handle(Player player) {
				Dialogue d = new Dialogue();
				d.setType(DialogueType.OPTION);
				d.getMessage().add("Yeah I'm fearless!");
				d.getMessage().add("No way, that looks scary!");
				d.getActions().add(new OptionAction() {
					@Override
					public boolean handle(Player player) {
						player.teleport(3568, 9712, 0, false);
						player.setAttribute("newBarrowsRun", true);
						return true;
					}
				});
				d.getActions().add(new OptionAction() {
					@Override
					public boolean handle(Player player) {
						return true;
					}
				});
				d.send(player);
				return false;
			}			
		});
	}
	
	/**
	 * Constructs a new {@code BarrowsActivity} {@code Object}.
	 * @param player
	 */
	public BarrowsActivity(Player player) {
		super(player);
		player.setActivity(this);
	}
	
	@Override
	public boolean initializeActivity() {
		for (BarrowsCrypt b : BarrowsConstants.BARROWS_CRYPT) {
			//if (!getPlayer().getSettings().getBarrowsKilled()
				//	.contains(b.getNPC().getId())) {
				addEntity(b.duplicate());
			//}
		}
		
		int tunnelsId = RANDOM.nextInt(BarrowsConstants.TUNNEL_CONFIG.length);
		if (getPlayer().getSettings().getTunnelId() > -1) { //incase the activity is re-initialized for a player who logged out of the activity
			tunnelsId = getPlayer().getSettings().getTunnelId();
		} else {
			getPlayer().getSettings().setTunnelId(tunnelsId);
		}
		this.barrowsTunnels = new BarrowsTunnels(tunnelsId); //TODO: Find out what was wrong with this
		
		int cryptId = RANDOM.nextInt(getEntities().size());
		if (getPlayer().getSettings().getTunnelEntranceId() > -1) { //incase the activity is re-initialized for a player who logged out of the activity
			cryptId = getPlayer().getSettings().getTunnelEntranceId();
		} else {
			getPlayer().getSettings().setTunnelEntranceId(cryptId);
		}
		getEntities().get(cryptId).setTunnelsEntrance(true);
		
		/*for (BarrowsCrypt crypt : getEntities()) {
			if (crypt.isTunnelsEntrance()) {
				System.out.println(crypt.getNPC().getName() + "'s crypt is tunnel entrance; NPC id " + crypt.getNPC().getId() + ".");
			}
		}*/
		return true;
	}

	@Override
	public boolean commenceSession() {
		if (!BarrowsConstants.BARROWS_AREA.isInArea(getPlayer().getLocation())) {
			ActionSender.sendConfig(getPlayer(), 1270, BarrowsConstants.isInMiniTunnel(getPlayer()) ? 1 : 0);
			ActionSender.sendOverlay(getPlayer(), MAIN_INTERFACE);
			ActionSender.updateMinimap(getPlayer(), ActionSender.BLACKOUT_MAP);
		}
		int hash = 0;
		for (int id : getPlayer().getSettings().getBarrowsKilled()) {
			hash |= 1 << (id - 2025);
		}
		ActionSender.sendConfig(getPlayer(), 453, getPlayer().getSettings().getBarrowsKillcount() << 17 | hash);
		return true;
	}
	
	@Override
	public boolean updateSession() {
		if (!BarrowsConstants.isInBarrowsZone(getPlayer())) {
			setActivityState(SessionStates.END_STATE);
			return true;
		}
		return true;
	}

	@Override
	public boolean endSession() {
		ActionSender.sendCloseOverlay(getPlayer());
		ActionSender.updateMinimap(getPlayer(), ActionSender.NO_BLACKOUT);
		for (BarrowsCrypt b : getEntities()) {
			Player owner = b.getNPC().getAttribute("barrowsOwner");
			if (owner != null)
				IconManager.removeIcon(owner, b.getNPC());
			b.getNPC().instantDeath();
		}
		getPlayer().setActivity(Mob.DEFAULT_ACTIVITY);
		return true;
	}
	
	@Override
	public boolean onDeath(Player player) {
		setActivityState(SessionStates.END_STATE);
		return false;
	}
	
	@Override
	public boolean canLogout(Player player, boolean logoutButton) {
		stop(true);
		return true;
	}
    
	@Override
    public boolean isCombatActivity(Mob mob, Mob victim, boolean sendMessages) {
		if (!mob.isNPC() && !victim.isNPC()) {
			return false;
		}
		Player owner = victim.getAttribute("barrowsOwner", null);
		if (victim.isNPC() && owner != null) {
			if (owner != mob) {
				if (sendMessages && mob.isPlayer())
					mob.getPlayer().sendMessage("This monster is not after you.");
				return false;
			}
			return owner.equals(mob);
		}
		owner = mob.getAttribute("barrowsOwner", null);
		if (mob.isNPC() && owner != null) {
			return owner.equals(victim);
		}
		if (victim.isNPC() || mob.isNPC())
			return true;
        return false;
    }
	
	@Override
	public boolean objectAction(final Player player, GameObject object, ClickOption actionId) {
		int id = object.getId();
		
		if (id == 6707 || id == 6703 || id == 6702 || id == 6704 || id == 6705 || id == 6706) { //staircases
			for (int i = 0; i < BarrowsConstants.CRYPT_AREA.length; i++) {
				if (BarrowsConstants.CRYPT_AREA[i].isInArea(getPlayer().getLocation())) {
					Location l = BarrowsConstants.HILL_AREA[i].getSouthWest();
					getPlayer().teleport(l.getX() + RANDOM.nextInt(4), l.getY() + RANDOM.nextInt(4), l.getZ(), false);
					//getPlayer().sendMessage("You leave the crypt.");
					ActionSender.sendCloseOverlay(player);
					ActionSender.updateMinimap(player, ActionSender.NO_BLACKOUT);
					return true;
				}
			}
			return false;
		}
		
        if (id == 10284) { //closed chest
        	boolean canOpenChest = true;
            for (int i = 0; i < player.getSettings().getKilledBrothers().length; i++) {
                if (!player.getSettings().getKilledBrothers()[i]) {
                	canOpenChest = false;
                	break;
                }
            }
        	if (player.getAttribute("canLootBarrowsChest", Boolean.FALSE) == Boolean.TRUE
        			|| canOpenChest) {
                if (player.getCombatExecutor().getLastAttacker() != null) {
                    player.sendMessage("You can't open this while being under attack!");
                    return true;
                }
                player.getMask().setFacePosition(object.getLocation(), object.getDefinition().getSizeX(), object.getDefinition().getSizeY());
                ActionSender.sendObject(player, 6775, 3551, 9695, 0, 10, 0);
        	} else if (player.getAttribute("newBarrowsRun", false)) {
        		boolean sendMessage = true;
        		for (BarrowsCrypt crypt : getEntities()) {
        			if (crypt.isTunnelsEntrance() && !player.getSettings().getBarrowsKilled().contains(crypt.getNPC().getId())
        					&& !crypt.getNPC().getAttribute("isSpawned", false)) {
        				sendMessage = false;
						crypt.getNPC().setDead(false);
						crypt.getNPC().setLocation(getBrotherSpawnLocation(getPlayer()));
						crypt.getNPC().turnTo(getPlayer(), false);
						crypt.getNPC().setAttribute("activity", "BarrowsActivity");
						crypt.getNPC().getCombatExecutor().setVictim(getPlayer());
						crypt.getNPC().setAttribute("barrowsOwner", getPlayer());
						crypt.getNPC().forceText("You dare steal from us!");
						crypt.getNPC().setAttribute("isSpawned", true);
						World.getWorld().getNpcs().add(crypt.getNPC());
						IconManager.iconOnMob(player, crypt.getNPC(), 1, 65535);
						return true;
        			}
        		}
        		if (sendMessage)
        			player.sendMessage("The chest is locked."); //wrong message
               /* if (player.getAttribute(Barrows.FIGHTING_ATTRIBUTE) == null) {
                    Brother brother = player.getAttribute(Barrows.TUNNEL_CRYPT);
                    NPC spawnedBrother = new BarrowBrother(brother.getNpcId(), player.getLocation().transform(-1, 0, 0));
                    spawnedBrother.loadEntityVariables();
                    World.getWorld().getNpcs().add(spawnedBrother);
                    spawnedBrother.getCombatExecutor().setVictim(player);
                    player.setAttribute(Barrows.FIGHTING_ATTRIBUTE, spawnedBrother);
                }*/
            }
            return true;
        }
        
        if (id == 6775) { //opened chest
        	boolean canOpenChest = true;
            for (int i = 0; i < player.getSettings().getKilledBrothers().length; i++) {
                if (!player.getSettings().getKilledBrothers()[i] == true) {
                	canOpenChest = false;
                	break;
                }
            }
            if (player.getAttribute("canLootBarrowsChest", Boolean.FALSE) == Boolean.TRUE
            		|| canOpenChest) {
                player.removeAttribute("canLootBarrowsChest");
        		player.removeAttribute("newBarrowsRun");
                for (int[] data : BarrowsTunnelListener.COMMON_REWARDS) {
                    if (player.getRandom().nextDouble() > 0.40) {
                        int itemId = data[0];
                        int amount = Misc.random(data[1], data[2]);
                        player.getInventory().addDropable(new Item(itemId, amount));
                    }
                }
                int chance = 2;
                chance += Math.round(player.getSettings().getBarrowsKillcount()/1.5);
                for (int i = 0; i < player.getSettings().getKilledBrothers().length; i++) {
                    if (player.getSettings().getKilledBrothers()[i]) {
                        player.getSettings().getKilledBrothers()[i] = false;
                        chance += 4;
                    }
                }
                int random = player.getRandom().nextInt(110);
                if (random <= (chance > 40 ? 40 : chance)
                		|| (Misc.random(4) == 4 && random <= chance)) {
                    int item = BarrowsTunnelListener.BARROW_REWARDS[player.getRandom().nextInt(BarrowsTunnelListener.BARROW_REWARDS.length)];
                    player.getInventory().addDropable(new Item(item, 1));
                }
                ActionSender.sendObject(player, 10284, 3551, 9695, 0, 10, 0);
                player.setAttribute("looted_barrows_request_shake", Boolean.TRUE);
                
        		for (BarrowsCrypt b : getEntities()) {
        			b.getNPC().instantDeath();
        		}
        		for (int i = 0; i < player.getSettings().getKilledBrothers().length; i++) {
        			player.getSettings().getKilledBrothers()[i] = false;
        		}
        		player.getSettings().getBarrowsKilled().clear();
        		player.getSettings().setBarrowsKillcount(0);
        		player.getSettings().setTunnelId(-1);
        		player.getSettings().setTunnelEntranceId(-1);
        		int hash = 0;
        		for (int id2 : player.getSettings().getBarrowsKilled()) {
        			hash |= 1 << (id2 - 2025);
        		}
        		ActionSender.sendConfig(player, 453, player.getSettings().getBarrowsKillcount() << 17 | hash);
        		for (BarrowsCrypt b : getEntities()) {
        			Player owner = b.getNPC().getAttribute("barrowsOwner");
        			if (owner != null)
        				IconManager.removeIcon(owner, b.getNPC());
        			b.getNPC().instantDeath();
        		}
            } else
            	player.sendMessage("The chest is empty."); //wrong message
            return true;
        }
        
		if (id == 6823 || id == 6771 || id == 6821 || id == 6773 || id == 6822 || id == 6772) { //Sarcophagi
			for (int i = 0; i < BarrowsConstants.CRYPT_AREA.length; i++) {
				if (BarrowsConstants.CRYPT_AREA[i].isInArea(getPlayer().getLocation())/* && getEntities().contains(BarrowsConstants.BARROWS_CRYPT[i])*/) {
					BarrowsCrypt b = getEntities().get(getEntities().indexOf(BarrowsConstants.BARROWS_CRYPT[i]));
					if (b.isTunnelsEntrance()) {
						DIALOGUE.send(player);
						return true;
					}
					if (getPlayer().getSettings().getBarrowsKilled().contains(b.getNPC().getId())
							|| b.getNPC().getAttribute("isSpawned", false)) {
						getPlayer().sendMessage("You don't find anything.");
						return true;
					}
					/*for (NPC n : Region.getLocalNPCs(player.getLocation())) {
						if (n != null && n.getId() == b.getNPC().getId() && n.getAttribute("barrowsOwner", null) == getPlayer()) {
							getPlayer().sendMessage("You don't find anything.");
							return true;
						}
					}*/
					b.getNPC().setDead(false);
					b.getNPC().setLocation(getBrotherSpawnLocation(getPlayer()));
					b.getNPC().turnTo(getPlayer(), false);
					b.getNPC().setAttribute("activity", "BarrowsActivity");
					b.getNPC().getCombatExecutor().setVictim(getPlayer());
					b.getNPC().setAttribute("barrowsOwner", getPlayer());
					b.getNPC().forceText("You dare disturb my rest!");
					b.getNPC().setAttribute("isSpawned", true);
					World.getWorld().getNpcs().add(b.getNPC());
					IconManager.iconOnMob(player, b.getNPC(), 1, 65535);
					return true;
				}
			}
		} else { //gates
			int hash = id << 16 | object.getLocation().getX() << 14 | object.getLocation().getY() << 12;
			final Gate gate = barrowsTunnels.getGates().get(hash);
			//System.out.println("Hash: " + hash);
			if (gate != null) {
				//if (gate.isClosed()) {
				//	player.sendMessage("The door seems to be locked.");
					//return true;
				}
				if (RANDOM.nextInt(15) < 2) {
					for (BarrowsCrypt crypt : getEntities()) {
						if (!crypt.getNPC().isDead() && !player.getSettings().getBarrowsKilled().contains(crypt.getNPC().getId())
								&& !crypt.getNPC().getAttribute("isSpawned", false)) {
							crypt.getNPC().setDead(false);
							crypt.getNPC().setLocation(getBrotherSpawnLocation(getPlayer()));
							crypt.getNPC().turnTo(getPlayer(), false);
							crypt.getNPC().setAttribute("activity", "BarrowsActivity");
							crypt.getNPC().getCombatExecutor().setVictim(getPlayer());
							crypt.getNPC().setAttribute("barrowsOwner", getPlayer());
							crypt.getNPC().forceText("You dare disturb my rest!");
							crypt.getNPC().setAttribute("isSpawned", true);
							World.getWorld().getNpcs().add(crypt.getNPC());
							IconManager.iconOnMob(player, crypt.getNPC(), 1, 65535);
							return true;
						}
					}
				}
				player.setAttribute("cantMove", true);
				player.getMask().setFacePosition(gate.getLocation(), 1, 1);
				final GameObject o = object;
		        GameObject secondO = null;
		        for (int i = -2; i < 3; i++) {
		            for (int j = -2; j < 3; j++) {
		                secondO = object.getLocation().transform(i, j, 0).getGameObject(object.getLocation().transform(i, j, 0));
		                if (secondO != null && !object.equals(secondO) 
		                		&& secondO.getId() >= 6713 && secondO.getId() <= 6750) {
		                	i = 3;
		                	j = 3;
		                	break;
		                }
		            }
		        }
				final GameObject secondDoor = secondO;
				int hashSecondDoor = secondDoor != null ? (secondDoor.getId() << 16 | secondDoor.getLocation().getX() << 14 | secondDoor.getLocation().getY() << 12)
						: -1;
				final Gate secondGate = hash == -1 ? null : barrowsTunnels.getGates().get(hashSecondDoor);
				for (Player p : Region.getLocalPlayers(gate.getLocation())) {
					ActionSender.deleteObject(p, o.getId(), o.getLocation().getX(), o.getLocation().getY(), player.getLocation().getZ(), o.getType(), o.getRotation());
					ActionSender.sendObject(p, gate.getToReplace());
					if (secondDoor != null && secondGate != null) {
						ActionSender.deleteObject(p, secondDoor.getId(), secondDoor.getLocation().getX(), secondDoor.getLocation().getY(), player.getLocation().getZ(), secondDoor.getType(), secondDoor.getRotation());
						ActionSender.sendObject(p, secondGate.getToReplace());
					}
				}
				World.getWorld().submit(new Tick(1) {
					boolean walked = false;
					@Override
					public void execute() {
						if (!walked) {
							int x = o.getLocation().getX();
							int y = o.getLocation().getY();
							if (o.getRotation() == 0 && player.getLocation().getX() >= x) {
								x--;
							} else if (o.getRotation() == 2 && player.getLocation().getX() <= x) {
								x++;
							} else if (o.getRotation() == 1 && player.getLocation().getY() <= y) {
								y++;
							} else if (o.getRotation() == 3 && player.getLocation().getY() >= y) {
								y--;
							}
							player.requestWalk(x, y); //this is not clipped so wrong doors make you walk to the weirdest locations
							walked = true;
							return;
						}
						player.setAttribute("cantMove", false);
						for (Player p : Region.getLocalPlayers(gate.getLocation())) {
							ActionSender.deleteObject(p, gate.getToReplace().getId(), 
									gate.getToReplace().getLocation().getX(), gate.getToReplace().getLocation().getY(), 
									player.getLocation().getZ(), gate.getToReplace().getType(), 
									gate.getToReplace().getRotation());
							ActionSender.sendObject(p, o);
							if (secondDoor != null && secondGate != null) {
								ActionSender.deleteObject(p, secondGate.getToReplace().getId(), 
										secondGate.getToReplace().getLocation().getX(), secondGate.getToReplace().getLocation().getY(), 
										player.getLocation().getZ(), secondGate.getToReplace().getType(), 
										secondGate.getToReplace().getRotation());
								ActionSender.sendObject(p, secondDoor);
							}
						}
						ActionSender.sendConfig(player, 1270, BarrowsConstants.isInMiniTunnel(player) ? 1 : 0);
						/*boolean isInMiniTunnel = BarrowsConstants.isInMiniTunnel(getPlayer());
						boolean wasInMiniTunnel = getPlayer().getAttribute("miniTunnel", false);
						if (isInMiniTunnel && !wasInMiniTunnel) {
							ActionSender.sendConfig(getPlayer(), 1270, 1);
							getPlayer().setAttribute("miniTunnel", true);
						} else if (!isInMiniTunnel && wasInMiniTunnel) {
							ActionSender.sendConfig(getPlayer(), 1270, 0);
							getPlayer().setAttribute("miniTunnel", false);
						}*/
						stop();
					}
				});
			}
		return false;
	}
	
	private Location getBrotherSpawnLocation(Player player) {
		List<Location> closeLocs = Following.getExternTiles(player, true);
		int areaCount = 3; //0 included, so 4
		int index = 0;
		List<Location> closeLocs2 = new CopyOnWriteArrayList<Location>(closeLocs);
		for (Location l : closeLocs) {
            int clippingMask = Region.getClippingMask(l.getX(), l.getY(), player.getLocation().getZ());
            if (((clippingMask & 0x1280180) != 0 && (clippingMask & 0x1280108) != 0
                    && (clippingMask & 0x1280120) != 0 && (clippingMask & 0x1280102) != 0)
                    || !World.getWorld().doPath(new DefaultPathFinder(), player, l.getX(), l.getY(), false, false).isRouteFound()) {
            	closeLocs2.remove(index);
            	areaCount--;
            	index--;
            }
            index++;
		}
		if (closeLocs2.isEmpty())
			return player.getLocation();
		Location closeLoc = closeLocs2.get(Misc.random(areaCount));
		closeLoc = Location.locate(closeLoc.getX(), closeLoc.getY(), player.getLocation().getZ());
		return closeLoc;
	}
	
	@Override
	public boolean itemAction(Player player, Item item, int actionId, String action, Object... params) {
		if (item.getId() == 952) {
			player.animate(830);
			for (int i = 0; i < BarrowsConstants.HILL_AREA.length; i++) {
				if (BarrowsConstants.HILL_AREA[i].isInArea(getPlayer().getLocation())) {
					final Location l = BarrowsConstants.CRYPT_TELEPORT_LOCATIONS[i];
					World.getWorld().submit(new Tick(1) {
						@Override
						public void execute() {
							stop();
							if (getPlayer().getFamiliar() != null) {
								getPlayer().sendMessage("You'll have to dismiss your familiar if you want to enter this area.");
							} else {
								getPlayer().teleport(l.getX(), l.getY(), l.getZ(), false);
								getPlayer().sendMessage("You've broken into a crypt!");
								ActionSender.sendOverlay(getPlayer(), MAIN_INTERFACE);
								ActionSender.updateMinimap(getPlayer(), ActionSender.BLACKOUT_MAP);
							}
						}
					});
					return true;
				}
			}
			World.getWorld().submit(new Tick(1) {
				@Override
				public void execute() {
					stop();
					getPlayer().sendMessage("You find nothing.");
				}
			});
			return true;
		}
		return false;
	}

}