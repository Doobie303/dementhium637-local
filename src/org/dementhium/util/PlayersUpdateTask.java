/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dementhium.util;

import java.util.Timer;
import java.util.TimerTask;
import org.dementhium.mysql.PlayersOnline;

/**
 *
 * @author James <james@lacunapk.info>
 */
public class PlayersUpdateTask {

    Timer timer;

    public static void init() {
        new PlayersUpdateTask();
        System.out.println("Registering player update task.");
    }

    public PlayersUpdateTask() {
        timer = new Timer();
        timer.schedule(new RemindTask(), 0, 1 * 1000);
    }

    class RemindTask extends TimerTask {

        int timer;

        public void run() {
            if (timer == 0) {
                timer = 120;
            }
            if (timer == 1) {
                PlayersOnline.createConnection();
                PlayersOnline.updatePlayers();
            }
            if (timer > 0) {
                timer--;
            }
        }
    }
}