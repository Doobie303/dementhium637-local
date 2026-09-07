package org.dementhium.content.cutscenes;

import org.dementhium.content.cutscenes.actions.DialogueAction;
import org.dementhium.content.cutscenes.actions.DialogueOptionAction;
import org.dementhium.content.cutscenes.actions.ShowTabsAction;
import org.dementhium.model.Item;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;

/**
 * @author Steve
 */
public class Cutscene {

    private final CutsceneAction[] actions;
    private final Player player;
    int currentActionId;
    int delay;

    public Cutscene(Player p, CutsceneAction[] actions) {
        this.player = p;
        this.actions = actions;
    }

    public void start() {
        ActionSender.sendCloseChatBox(player);
        ActionSender.closeInventoryInterface(player);
        ActionSender.sendCloseInterface(player);
        player.setAttribute("currentScene", this);
        player.getActionManager().stopAction();
        player.getCombatExecutor().reset();
        player.getMask().setFacePosition(null, 1, 1);
        if (player.getMask().getInteractingEntity() != null) {
            player.resetTurnTo();
        }
        player.setAttribute("cantMove", Boolean.TRUE);
        player.submitTick("cutscene", new Tick(1) {

            @Override
            public void execute() {
                if (currentActionId == actions.length) {
                    this.stop();
                }
                if (delay > 0) {
                    if ((!(actions[currentActionId] instanceof DialogueAction) || !((DialogueAction) actions[currentActionId]).isExecuted())) {
                        if ((!(actions[currentActionId] instanceof DialogueOptionAction) || !((DialogueOptionAction) actions[currentActionId]).isExecuted())) {
                        	if ((!(actions[currentActionId] instanceof ShowTabsAction) || !((ShowTabsAction) actions[currentActionId]).isExecuted())) {
                            delay--;
                        }
                        }
                    }
                } else if (currentActionId < actions.length) {
                    CutsceneAction action = actions[currentActionId];
                    player.setAttribute("cutsceneAction", action);
                    action.execute(null);
                    //System.out.println((actions[currentActionId] instanceof DialogueOptionAction));
                    if (!(actions[currentActionId] instanceof DialogueAction) && !(actions[currentActionId] instanceof DialogueOptionAction)) {
                        currentActionId++;
                    }
                    delay = action.getDelay();
                }

            }

            @Override
            public void stop() {
                super.stop();
                giveStarter(player);
            }

        });
    }
    
    public static void giveStarter(Player player) {
        player.removeAttribute("cutsceneAction");
        ActionSender.resetCamera(player);
        player.removeAttribute("cantMove");
        if(!player.hasReceivedStarter()){
        	player.getInventory().getContainer().add(new Item(995, 1000000));
        	player.getInventory().getContainer().add(new Item(380, 100));
        	player.getInventory().getContainer().add(new Item(757, 1));
        	player.getInventory().getContainer().add(new Item(1856, 1));
        	player.getInventory().getContainer().add(new Item(6568, 1));
        	player.getInventory().getContainer().add(new Item(3105, 1));
        	player.getInventory().getContainer().add(new Item(1067, 1));
        	player.getInventory().getContainer().add(new Item(1115, 1));
        	player.getInventory().getContainer().add(new Item(1153, 1));
        	player.getInventory().getContainer().add(new Item(1191, 1));
        	player.getInventory().getContainer().add(new Item(1323, 1));
        	player.getInventory().getContainer().add(new Item(1309, 1));
        	player.getInventory().getContainer().add(new Item(1725, 1));
        	player.getInventory().getContainer().add(new Item(1061, 1));
        	player.getInventory().getContainer().add(new Item(1095, 1));
        	player.getInventory().getContainer().add(new Item(1129, 1));
        	player.getInventory().getContainer().add(new Item(1167, 1));
        	player.getInventory().getContainer().add(new Item(841, 1));
        	player.getInventory().getContainer().add(new Item(882, 1000));
        	player.getInventory().getContainer().add(new Item(1063, 1));
        	player.getInventory().getContainer().add(new Item(1731, 1));
        	player.getInventory().getContainer().add(new Item(1011, 1));
        	player.getInventory().getContainer().add(new Item(577, 1));
        	player.getInventory().getContainer().add(new Item(579, 1));
        	player.getInventory().getContainer().add(new Item(1727, 1));
        	player.getInventory().getContainer().add(new Item(558, 100));
        	player.getInventory().getContainer().add(new Item(556, 200));
        	
        	player.getInventory().refresh();
			player.setDoubleXpTimer(System.currentTimeMillis() + 7200000);
			player.setHasReceivedStarter(true);
			player.setInvisible(false);
			player.sendMessage("<col=ff0000>Your 120 minutes of Double experience will now start!");
        }
    }

    public CutsceneAction[] getActions() {
        return actions;
    }

    public Player getPlayer() {
        return player;
    }

    public void advanceToAction(int advanceAmount) {
        delay = 0;
        currentActionId += advanceAmount;
    }

    public void advanceAction() {
        delay = 0;
        currentActionId++;
    }

}
