package org.dementhium.content.cutscenes.impl;

import java.util.ArrayList;

import org.dementhium.content.DialogueManager;
import org.dementhium.content.cutscenes.Cutscene;
import org.dementhium.content.cutscenes.CutsceneAction;
import org.dementhium.content.cutscenes.actions.CameraMoveAction;
import org.dementhium.content.cutscenes.actions.DialogueAction;
import org.dementhium.content.cutscenes.actions.TeleportAction;
import org.dementhium.model.player.Player;

/**
 * @author 'Lumby <lumbyjr@hotmail.com>
 */
public class TutorialScene {

    private Cutscene scene;

    public TutorialScene(Player p) {
        scene = new Cutscene(p, constructActions(p));
    }

    /*
     * tips for making scenes, if you're going to have camera movement and dialogue at the same time,
     * ALWAYS do the camera first, because the next action will not advance until you click the continue button
     * on a dialogue action.
     */
   private CutsceneAction[] constructActions(final Player p) {
       ArrayList<CutsceneAction> actions = new ArrayList<CutsceneAction>();
       //actions.add(new AnimationAction(p, 0, Animation.create(4367)));
       //actions.add(new InterfaceAction(p, 0, 177)); //inter 384 (make the screen black with an inter be4 that (454 e.g.))
       //actions.add(new InterfaceAction(p, 0, -1));
       actions.add(new DialogueAction(p, -1, DialogueManager.SECRELTY_TALKING, false, "whaa....??? Where am I?!"));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "No need to be scared!", "This is DyNamic PvP and I will be your guide today!"));
       actions.add(new CameraMoveAction(p, 3, 2340, 3675, 15, 10));
       actions.add(new DialogueAction(p, 13280, DialogueManager.TALKING_ALOT, true, "Now what you see here is our home area,","here you can find just about everything!","Shops, items, players, portals and more!"));
       actions.add(new DialogueAction(p, 13280, DialogueManager.TALKING_ALOT, true, "Here at the home area will be your way around the server.","All of these portals take you somewhere!","Each place is different, it's a lot of fun!"));
       actions.add(new TeleportAction(p, 3, 2883, 9812, 0, 0, false));
       actions.add(new DialogueAction(p, 13280, DialogueManager.CALM_TALK, true, "Here we are....","One of the many training dungeons on DyNamic's!","You can train your skills here and collect coins!"));
       actions.add(new TeleportAction(p, 3, 2782, 10102, 0, 0, false));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "I bring you to one of the most popular"," train areas on DyNamic's! Yes this is rock crabs!","You can get here by using the purple portal at home!"));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "Now... Let's take you somewhere a little more interesting!"));
      // actions.add(new TeleportAction(p, 3, 2815, 5511, 0, 0, false));
      // actions.add(new CameraMoveAction(p, 3, 2813, 5506, 1, 10));
       //actions.add(new CameraMoveAction(p, 3, 2810, 5541, 15, 6));
     //  actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "Here players can let off steam!","This is the SAFE PvP area!","Now this means that you DON'T loose items on death!","You get no pk points here."));
      // actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "Next area....!"));
       actions.add(new TeleportAction(p, 3, 2416, 3526, 0, 0, false));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "This is the Donor Zone!","Donators can have fast skilling here.","And much, much more benefits!"));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "If you are interested in sending a donation.","Go to www.dynamicpvp.com","Send the money and let doobie know how much you donated."));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "Moving on!"));
       //actions.add(new TeleportAction(p, 2, 2866, 5205, 0, 0, false));
      // actions.add(new CameraMoveAction(p, 4, 2924, 5203, 65, 4));
       //actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "Nex....!","The most powerful monster in DyNamic's.","She is well worth the fight, she drops","some of the best armour in the game!"));
       //actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "Moving on!"));
       actions.add(new TeleportAction(p, 3, 3006, 5511, 0, 0, false));
       actions.add(new CameraMoveAction(p, 3, 3001, 5506, 15, 7));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "This is Dangerous PvP, players lose items here."," It's 1V1 combat let's you safeley fight others here","without interuption."));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "Back to the home area...","I am sure you are very tired of me talking so much!..."));
       actions.add(new TeleportAction(p, 3, 2343, 3683, 0, 0, false));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "Now that you know your way around...","You can get started!"));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "Before that though...."));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "Remember to ALWAYS follow the rules!","You can read them at","www.dynamicpvp.com"));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "Don't forget to register on forums also,","you can vote every 24 hours for vote","tickets you get special prizes!","Vote at www.dynamicpvp.com"));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "Respect ALL staff and Mod's/Admin's","The owner of DyNamic's is Doobie."));
       actions.add(new TeleportAction(p, 3, 2784, 10099, 0, 0, false));
       actions.add(new DialogueAction(p, 13280, DialogueManager.HAPPY_TALKING, true, "Now You are finished!","Have fun on DyNamic's play safe!"));
		return actions.toArray(new CutsceneAction[0]);
   }
   public void start() {
       scene.start();
   }
}