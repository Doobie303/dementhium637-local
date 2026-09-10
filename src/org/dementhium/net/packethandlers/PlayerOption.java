package org.dementhium.net.packethandlers;

import org.dementhium.content.DialogueManager;
import org.dementhium.content.activity.ActivityManager;
import org.dementhium.content.activity.impl.CastleWarsActivity;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.areas.CoordinateEvent;
import org.dementhium.content.misc.Following;
import org.dementhium.io.PlayerLoader;
import org.dementhium.model.Item;
import org.dementhium.model.World;
import org.dementhium.model.map.path.DefaultPathFinder;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.IconManager;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.TradeSession;
import org.dementhium.net.ActionSender;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Constants;
import org.dementhium.util.handlers.OffencesHandler;

public class PlayerOption extends PacketHandler {

	//OLD:
    /*public static final int FIRST_OPTION = 70;
    public static final int TRADE_PLAYER = 27; // RIGHT! <- wrong
    public static final int FOLLOW_PLAYER = 80;*/
	
    public static final int FIRST_OPTION = 70;
    public static final int FOLLOW_PLAYER = 27;
    public static final int TRADE_PLAYER = 47; //previously handled in TradePacketHandler.java
    public static final int REQ_ASSISTANCE_PLAYER = 8;
    public static final int REPORT = 68;
    public static final int HEAL_PLAYER = 80; //Castle Wars
    public static final int TAKE_FROM_PLAYER = 64; //Castle Wars

    @Override
    public void handlePacket(Player player, Message packet) {
        if(!player.hasReceivedStarter()) {
        	return;
        }
        switch (packet.getOpcode()) {
            case FIRST_OPTION:
                handleFirstOption(player, packet);
                break;
            case FOLLOW_PLAYER:
        		player.closeAll(true, true);
                Following.playerFollow(player, World.getWorld().getPlayers().get(packet.readShort()));
                break;
            case TRADE_PLAYER:
                handleTradeRequest(player, packet);
                break;
            case REQ_ASSISTANCE_PLAYER:
            	handleReqAssist(player, packet);
            	break;
            case REPORT:
            	handleReportOption(player, packet);
            	break;
            case HEAL_PLAYER:
            	healOption(player, packet);
            	break;
            case TAKE_FROM_PLAYER:
            	handleTakeFromOption(player, packet);
            	break;
        }
    }

    private void handleFirstOption(final Player player, Message packet) {
        int playerIndex = packet.readLEShort();
        final Player other = World.getWorld().getPlayers().get(playerIndex);
        if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,other)) return;
        if (other == null || !other.isOnline() || player.getIndex() == other.getIndex()) {
            return;
        }
		player.closeAll(true, true);
        player.getActionManager().stopAction();
        if (World.getWorld().getAreaManager().getAreaByName("Duel").contains(player.getLocation()) && !World.getWorld().getAreaManager().getAreaByName("AreaNotBeloningToDuel").contains(player.getLocation()) && player.getAttribute("duelingWith") == null) {
            if (!org.dementhium.content.activity.impl.duel.DuelChallenge.eligible(player,other)) {
                player.sendMessage("That player is not available to duel.");return;
            }
            Following.combatFollow(player,other);
            World.getWorld().submitAreaEvent(player,new CoordinateEvent(player,other.getLocation().getX(),other.getLocation().getY(),other.size(),other.size()) {
                public void execute() {
                    if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,other)) return;
                    org.dementhium.content.activity.impl.duel.DuelChallenge.select(player,other);
                }
            });        } else if (player.getActivity().isCombatActivity(player, other, true) || player.getPlayerArea().inWilderness() || (player.getAttribute("duelingWith") != null && World.getWorld().getAreaManager().getAreaByName("Duel").contains(player.getLocation()))) {
        	player.turnTo(other, false);
			player.setAttribute("Droptick", World.getTicks() + 20);
        	if (player.isInWilderness()) {
            	boolean allowAdminAttack = true;
        		for(String name : PlayerLoader.superMods) {
        			if(player.getUsername().equals(name) || other.getUsername().equals(name)) {
        				allowAdminAttack = true;
        			}
        		}
                if (other.getRights() >= 2 && player.getRights() < 2 && !allowAdminAttack) {
                	player.sendMessage("You can't attack an administrator.");
                	return;
                } else if (player.getRights() >= 2 && other.getRights() < 2 && !allowAdminAttack) {
                	player.sendMessage("Administrators can't attack players.");
                	return;
                } else if (other.getAttribute("loginImmunity") != null) {
                	player.sendMessage("This player is still under login protection and can't be attacked yet.");
                	return;
                } else if (player.getAttribute("loginImmunity") != null) {
                	player.sendMessage("You are still under login protection and can't attack others yet.");
                	return;
                }
        	}
    		if (player.getAttribute("superhit") != null) {
    			final int damage = player.getAttribute("superhit");
    			player.removeAttribute("superhit");
    			player.animate(842);
    			final Player p = player;
    			final Player o = other;
    			p.setCanAnimate(false);
    			World.getWorld().submit(new Tick(3) {
    				@Override
    				public void execute() {
    					p.setCanAnimate(true);
    					p.animate(1500);
    					p.setCanAnimate(false);
    					stop();
    				}
    			});
    			World.getWorld().submit(new Tick(5) {
    				@Override
    				public void execute() {
    					p.setCanAnimate(true);
    					p.animate(1501);
    					p.setCanAnimate(false);
    					stop();
    				}
    			});
    			World.getWorld().submit(new Tick(8) {
    				@Override
    				public void execute() {
    					p.setCanAnimate(true);
    					p.animate(1502);
    					p.setCanAnimate(false);
    					stop();
    				}
    			});
    			World.getWorld().submit(new Tick(9) {
    				@Override
    				public void execute() {
    					p.setCanAnimate(true);
    					p.getMask().setAppearanceUpdate(true);
    					p.graphics(287);
    					o.getDamageManager().miscDamage(damage, DamageType.MAGE);
    					p.getCombatExecutor().setVictim(o);
    					stop();
    				}
    			});
    		} else
    			player.getCombatExecutor().setVictim(other);
        }
    }

    private void handleTradeRequest(final Player player, Message packet) {
        packet.readByteS();
        int partnerIndex = packet.readShort();
        if (partnerIndex < 0 || partnerIndex >= Constants.MAX_AMT_OF_PLAYERS) {
            return;
        }
        final Player partner = World.getWorld().getPlayers().get(partnerIndex);
        if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,partner)) return;
        if (partner == null || !partner.isOnline() || player.getIndex() == partner.getIndex()) {
            return;
        }
		player.closeAll(true, true);
        player.getActionManager().stopAction();
        player.turnTo(partner, false);
        if (!World.getWorld().doPath(new DefaultPathFinder(), player, partner.getLocation().getX(), partner.getLocation().getY(), false, false).isRouteFound()) {
            player.sendMessage("I can't reach that!");
            return;
        } else {
            if(!partner.hasReceivedStarter()){//Can't trade people during Tutorial cutscene!
            	player.sendMessage("Let this new adventurer finish his tutorial first.");
            	return;
            }
        	boolean allowAdminTrade = false;
    		for(String name : PlayerLoader.superMods) {
    			if(player.getUsername().equals(name) || partner.getUsername().equals(name)) {
    				allowAdminTrade = true;
    			}
    		//}
           // if (partner.getRights() >= 2 && player.getRights() < 2 && !allowAdminTrade) {
            //	player.sendMessage("You can't trade with an administrator.");
            //	return;
          //  } else if (player.getRights() >= 2 && partner.getRights() < 2 && !allowAdminTrade) {
            	//player.sendMessage("Administrators can't trade with players.");
            //	return;
            }
            if ((player.isInDuelArenaDuel() || partner.isInDuelArenaDuel()) && player.getRights() < 2 && partner.getRights() < 2) {
            	if (player.isInDuelArenaDuel() || (player.isInDuelArenaDuel() && partner.isInDuelArenaDuel()))
            		player.sendMessage("You can't trade during a duel.");
            	else if (partner.isInDuelArenaDuel())
            		player.sendMessage("This player is in a duel and can't be traded.");
            	return;
            }
            if ((player.getActivity() instanceof CastleWarsActivity || partner.getActivity() instanceof CastleWarsActivity)
            	&& player.getRights() < 2 && partner.getRights() < 2) {
            	if (player.getActivity() instanceof CastleWarsActivity || (player.getActivity() instanceof CastleWarsActivity && partner.getActivity() instanceof CastleWarsActivity))
            		player.sendMessage("You can't trade during this minigame.");
            	else if (partner.getActivity() instanceof CastleWarsActivity)
            		player.sendMessage("This player is in a castle wars minigame and can't be traded.");
            	return;
            }
            Following.combatFollow(player, partner);
        }
        World.getWorld().submitAreaEvent(player, new CoordinateEvent(player, partner.getLocation().getX(), partner.getLocation().getY(), partner.size(), partner.size()) {

            @Override
            public void execute() {
                if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,partner)) return;
                if (partner.getTradeSession() != null) {
                    ActionSender.sendMessage(player, "The other player is busy.");
                    return;
                }
                if (partner.getAttribute("didRequestTrade") == Boolean.TRUE && ((Short) partner.getAttribute("tradeWithIndex") == player.getIndex())) {
                    TradeSession session = new TradeSession(player, partner);
                    player.setTradeSession(session);
                    partner.setTradePartner(player);
                    session.start();
                } else {
                    ActionSender.sendMessage(player, "Sending trade offer...");
                    ActionSender.sendTradeReq(partner, player.getDisplayName(), "wishes to trade with you.");
                    player.setAttribute("tradeWithIndex", partner.getIndex());
                    player.setAttribute("didRequestTrade", Boolean.TRUE);
                }
            }
        });
    }
    
    private void healOption(final Player player, Message packet) {
        int playerIndex = packet.readLEShortA();
        if (playerIndex < 0 || playerIndex >= Constants.MAX_AMT_OF_PLAYERS) {
            return;
        }
        final Player other = World.getWorld().getPlayers().get(playerIndex);
        if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,other)) return;
        if (other == null || !other.isOnline() || player.getIndex() == other.getIndex()) {
            return;
        }
		player.closeAll(true, true);
        player.getActionManager().stopAction();
        player.turnTo(other, false);
        if (!World.getWorld().doPath(new DefaultPathFinder(), player, other.getLocation().getX(), other.getLocation().getY(), false, false).isRouteFound()) {
            player.sendMessage("I can't reach that!");
            return;
        }
        if (player.getActivity() instanceof CastleWarsActivity) {
        	if (other.getActivity() instanceof CastleWarsActivity) {
        		if ((CastleWarsActivity.getSingleton().getSaradominTeam().getPlayers().contains(player)
        				&& CastleWarsActivity.getSingleton().getSaradominTeam().getPlayers().contains(other))
        				|| (CastleWarsActivity.getSingleton().getZamorakTeam().getPlayers().contains(player)
                				&& CastleWarsActivity.getSingleton().getZamorakTeam().getPlayers().contains(other))) {
        			 if (player.getInventory().contains(4049)) {
        				 Following.combatFollow(player, other);
        				 World.getWorld().submitAreaEvent(player, new CoordinateEvent(player, other.getLocation().getX(), other.getLocation().getY(), other.size(), other.size()) {

        		                @Override
        		                public void execute() {
                				 	player.getInventory().deleteItem(new Item(4049));
                	                other.getSkills().heal(other.getSkills().getMaximumLifePoints() / 10);
                	                other.getPoisonManager().removePoison();
                	                int energy = other.getWalkingQueue().getRunEnergy() + 30;
                	                if (energy > 100) {
                	                    energy = 100;
                	                }
                	                other.getWalkingQueue().setRunEnergy(energy);
        		                }
        		            });
        	            } else
        	            	player.sendMessage("You need bandages in order to heal this player.");
        		} else
        			player.sendMessage("You can only heal a teammate.");
        	} else
        		player.sendMessage("This player is not in a castle wars game.");
        } else
        	player.sendMessage("You can't do that here.");
    }
    
    private void handleTakeFromOption(final Player player, Message packet) {
        int playerIndex = packet.readLEShortA();
        if (playerIndex < 0 || playerIndex >= Constants.MAX_AMT_OF_PLAYERS) {
            return;
        }
        final Player other = World.getWorld().getPlayers().get(playerIndex);
        if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,other)) return;
        if (other == null || !other.isOnline() || player.getIndex() == other.getIndex()) {
            return;
        }
		player.closeAll(true, true);
        player.getActionManager().stopAction();
        player.turnTo(other, false);
        if (!World.getWorld().doPath(new DefaultPathFinder(), player, other.getLocation().getX(), other.getLocation().getY(), false, false).isRouteFound()) {
            player.sendMessage("I can't reach that!");
            return;
        }
        if (player.getActivity() instanceof CastleWarsActivity) {
        	if (other.getActivity() instanceof CastleWarsActivity) {
        		if ((CastleWarsActivity.getSingleton().getSaradominTeam().getPlayers().contains(player)
        				&& CastleWarsActivity.getSingleton().getSaradominTeam().getPlayers().contains(other))
        				|| (CastleWarsActivity.getSingleton().getZamorakTeam().getPlayers().contains(player)
                				&& CastleWarsActivity.getSingleton().getZamorakTeam().getPlayers().contains(other))) {
            		if (other.getEquipment().getSlot(Equipment.SLOT_WEAPON) != 4037
        					&& other.getEquipment().getSlot(Equipment.SLOT_WEAPON) != 4039/*other.getAttribute("flagTakenStartTime") == null*/) {
            			player.sendMessage("This player is not carrying a flag.");
            			return;
            		}
                	if ((other.getAttribute("flagTakenStartTime", World.getTicks()) + 500) < World.getTicks()) {
                		int neededInvSpace = 0;
                		if (player.getEquipment().get(Equipment.SLOT_WEAPON) != null)
                			neededInvSpace++;
                		if (player.getEquipment().get(Equipment.SLOT_SHIELD) != null)
                			neededInvSpace++;
                		if (player.getInventory().getFreeSlots() < neededInvSpace) {
                			player.sendMessage("You do not enough space in your inventory to take the flag.");
                			return;
                		}
                        Following.combatFollow(player, other);
    		            World.getWorld().submitAreaEvent(player, new CoordinateEvent(player, other.getLocation().getX(), other.getLocation().getY(), other.size(), other.size()) {

    		                @Override
    		                public void execute() {
    	                		Item item = new Item(other.getEquipment().getSlot(Equipment.SLOT_WEAPON));
    	                		other.getEquipment().set(Equipment.SLOT_WEAPON, null);
    	                		other.removeAttribute("flagTakenStartTime");
    	                		if (player.getEquipment().get(Equipment.SLOT_WEAPON) != null)
    	                			player.getEquipment().unEquip(player, player.getEquipment().getSlot(Equipment.SLOT_WEAPON), -1, Equipment.SLOT_WEAPON, true);
    	                		if (player.getEquipment().get(Equipment.SLOT_SHIELD) != null)
    	                			player.getEquipment().unEquip(player, player.getEquipment().getSlot(Equipment.SLOT_SHIELD), -1, Equipment.SLOT_SHIELD, true);
    	                        player.getEquipment().set(Equipment.SLOT_WEAPON, item);
    	                        player.setAttribute("flagTakenStartTime", World.getTicks());
    	                        if (CastleWarsActivity.getSingleton().currentFlagHolderOfTeamSaradomin.equals(other))
    	                        	CastleWarsActivity.getSingleton().currentFlagHolderOfTeamSaradomin = player;
    	                        else if (CastleWarsActivity.getSingleton().currentFlagHolderOfTeamZamorak.equals(other))
    	                        	CastleWarsActivity.getSingleton().currentFlagHolderOfTeamZamorak = player;
    	                        for (Player p : CastleWarsActivity.getSingleton().getZamorakTeam().getPlayers()) {
    	                            IconManager.removeIcon(p, other);
    	                            IconManager.iconOnMob(p, player, 1, 65535);
    	                        }
    	                        for (Player p : CastleWarsActivity.getSingleton().getSaradominTeam().getPlayers()) {
    	                            IconManager.removeIcon(p, other);
    	                            IconManager.iconOnMob(p, player, 1, 65535);
    	                        }
    		                }
    		            });
                	} else {
                		player.sendMessage("You can only take a flag from a teammate who has been carrying the flag for at least");
                		player.sendMessage("5 minutes.");
                	}
        		} else
        			player.sendMessage("You can only take a flag from a teammate.");
        	} else
        		player.sendMessage("This player is not in a castle wars game.");
        } else
        	player.sendMessage("You can't do that here.");
    }
    
    private void handleReqAssist(final Player player, Message packet) {
    	packet.readByteS();
        int playerIndex = packet.readShort();
        if (playerIndex < 0 || playerIndex >= Constants.MAX_AMT_OF_PLAYERS) {
            return;
        }
        final Player other = World.getWorld().getPlayers().get(playerIndex);
        if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,other)) return;
        if (other == null || !other.isOnline() || player.getIndex() == other.getIndex()) {
            return;
        }
		player.closeAll(true, true);
        player.getActionManager().stopAction();
        player.turnTo(other, false);
        if (!World.getWorld().doPath(new DefaultPathFinder(), player, other.getLocation().getX(), other.getLocation().getY(), false, false).isRouteFound()) {
            player.sendMessage("I can't reach that!");
            return;
        }
        Following.combatFollow(player, other);
        World.getWorld().submitAreaEvent(player, new CoordinateEvent(player, other.getLocation().getX(), other.getLocation().getY(), other.size(), other.size()) {

            @Override
            public void execute() {
            	player.sendMessage("This feature hasn't been added yet.");
            }
        });
    }
    
    private void handleReportOption(final Player player, Message packet) {
    	packet.readByteS();
        int playerIndex = packet.readShort();
        if (playerIndex < 0 || playerIndex >= Constants.MAX_AMT_OF_PLAYERS) {
            return;
        }
        final Player other = World.getWorld().getPlayers().get(playerIndex);
        if (other == null || !other.isOnline() || player.getIndex() == other.getIndex()) {
            return;
        }
		player.closeAll(true, true);
        player.getActionManager().stopAction();
        
        player.setAttribute("reportedPlayer", other);
        if (player.getRights() >= 2) {
			//Protection:
			if (OffencesHandler.formatIp(player.getConnection().getChannel().getRemoteAddress().toString()).equals("127.0.0.1"))
				DialogueManager.sendOptionDialogue(player, new int[]{780, 793, 782, 794, 788}, "Kick this player.", "Mute this player.", "Unmute this player.", "Ban this player.", "More options.");
			else
				DialogueManager.sendOptionDialogue(player, new int[]{780, 793, 782, 794, 786}, "Kick this player.", "Mute this player.", "Unmute this player.", "Ban this player.", "More options.");
        } else if (player.getRights() == 1) {
        	player.sendMessage("This feature hasn't been added yet.");
        	//show report interface (with mute option).
        } else {
        	player.sendMessage("This feature hasn't been added yet.");
        	//show report interface.
        }
    }
    
}
