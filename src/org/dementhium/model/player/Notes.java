package org.dementhium.model.player;

import org.dementhium.net.ActionSender;
import org.dementhium.util.Constants;

import java.util.ArrayList;

/**
 * @author Steve <golden_32@live.com>
 * @author Lumby <lumbyjr@hotmail.com>
 */
public class Notes {


    public class Note {

        private int color = 0;
        private String text = "";

        public Note(int color, String text) {
            this.setColor(color);
            this.setText(text);
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setColor(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }

    private ArrayList<Note> notes = new ArrayList<Note>(30);
    private Player player;

    public Notes(Player p) {
        this.player = p;
    }

    public void addNote(String text) {
        text = text.replaceAll("gay", "happy");
        if (text.length() > 50) {
            player.sendMessage("You can only enter notes containing up to 50 characters!");
            return;
        }
        if (notes.size() < 30) {
            notes.add(new Note(0, text));
        } else {
            player.sendMessage("You cannot add more than 30 notes!");
            return;
        }
        if (text.equalsIgnoreCase("all your base are belong to us"))
        	text = "orly?";
        else if (text.equalsIgnoreCase("orly"))
        	text = "yarly";
        else if (text.equalsIgnoreCase("murder") || text.equalsIgnoreCase("redrum"))
        	text = "All rest and no play makes Guthix a dull boy.";
        else if (text.equalsIgnoreCase("andrew"))
        	text = "Cabbage.";
        else if (text.equalsIgnoreCase("paul"))
        	text = "Rargh, I'm a lava monster!";
        else if (text.equalsIgnoreCase("i am your father"))
        	text = "Nooooooooooooooooooooooooo!";
        else if (text.equalsIgnoreCase("i'll be back"))
        	text = "Come with me if you want to live.";
        else if (text.equalsIgnoreCase("finish the fight"))
        	text = "They must love the smell of hero.";
        else if (text.equalsIgnoreCase("there is no spoon"))
        	text = "Then you will see, it is not the spoon that bends, it is only yourself.";
        else if (text.equalsIgnoreCase("you fight like a dairy farmer"))
        	text = "How appropriate. You fight like a cow.";
        else if (text.equalsIgnoreCase("bangin'"))
        	text = "donk";
        else if (text.equalsIgnoreCase("humperdinck"))
        	text = "Have fun storming the castle!";
        else if (text.equalsIgnoreCase("milton waddams"))
        	text = "The ratio of people to cake is too big.";
        else if (text.equalsIgnoreCase("r.i.p. runescape") || text.equalsIgnoreCase("r.i.p. "+Constants.SERVER_NAME))
        	text = "Wanna bet?";
        else if (text.equalsIgnoreCase("penso, logo existo"))
        	text = "Borboletas salpicadas de goiabada...";
        else if (text.equalsIgnoreCase("le temps passe"))
        	text = "L'œuf dur";
        else if (text.equalsIgnoreCase("sevga"))
        	text = "Marmaros had a close encounter with a prayer-eating behemoth.";
        int NoteId = notes.size() - 1;
        ActionSender.sendConfig(player, 1439, NoteId);
        player.setAttribute("selectedNote", NoteId);
        refreshNotes(false);
    }

    public void addNote(String text, int color) {
        notes.add(new Note(color, text));
    }

    public void loadNotes() {
        ActionSender.sendAMask(player, 2621470, 34, 9, 0, 29);
        ActionSender.sendString(player, "Loading notes<br>Please wait...", 34, 13);
        ActionSender.sendInterfaceConfig(player, 34, 13, true);
        ActionSender.sendInterfaceConfig(player, 34, 3, true);
        ActionSender.sendConfig(player, 1439, -1);
        refreshNotes(true);
    }

    public void refreshNotes(boolean sendStartConfigs) {
        for (int i = 0; i < 30; i++) {
            ActionSender.sendSpecialString(player, 149 + i, i < notes.size() ? notes.get(i).getText() : "");
        }
        if (sendStartConfigs) {
            for (int i = 1430; i < 1450; i++)
                ActionSender.sendConfig(player, i, i);
        }
        ActionSender.sendConfig(player, 1440, getFirstTotalColorValue());
        ActionSender.sendConfig(player, 1441, getSecondTotalColorValue());
    }


    public int intColorValue(int color, int noteId) {
        return (int) (Math.pow(4, noteId) * color);
    }

    public int getFirstTotalColorValue() {
        int Color = 0;
        for (int i = 0; i < 15; i++) {
            if (notes.size() > i)
                Color += intColorValue(notes.get(i).getColor(), i);
        }
        return Color;
    }

    public int getSecondTotalColorValue() {
        int color = 0;
        for (int i = 0; i < 15; i++) {
            if (notes.size() > (i + 16))
                color += intColorValue(notes.get(i + 16).getColor(), i);
        }
        return color;
    }

    public void deleteSelectedNote() {
        if (player.getAttribute("selectedNote", -1) > -1) {
            int slot = player.getAttribute("selectedNote", -1);
            notes.remove(slot);
            player.setAttribute("selectedNote", -1);
            ActionSender.sendConfig(player, 1439, -1);
            refreshNotes(false);
        }
    }

    public void clear() {
        notes.clear();
        refreshNotes(false);
    }

    public void editNote(String string, Integer attribute) {
        notes.get(attribute).setText(string);
        refreshNotes(false);
    }

    public void setColor(int color, Integer attribute) {
        notes.get(attribute).setColor(color);
        refreshNotes(false);

    }

    public void deleteNote(int slot) {
        notes.remove(slot);
        refreshNotes(false);

    }

    public void setNotes(ArrayList<Note> setNotes) {
        notes = setNotes;
        refreshNotes(false);
    }

    public ArrayList<Note> getList() {
        return notes;
    }


}
