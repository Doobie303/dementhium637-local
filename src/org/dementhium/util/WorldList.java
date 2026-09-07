package org.dementhium.util;

import org.dementhium.model.World;
import org.dementhium.net.message.Message;
import org.dementhium.net.message.Message.PacketType;
import org.dementhium.net.message.MessageBuilder;

import java.util.ArrayList;

public class WorldList {
    public static final int COUNTRY_AUSTRALIA = 16;

    public static final int COUNTRY_BELGIUM = 22;

    public static final int COUNTRY_BRAZIL = 31;

    public static final int COUNTRY_CANADA = 38;

    public static final int COUNTRY_DENMARK = 58;

    public static final int COUNTRY_FINLAND = 69;

    public static final int COUNTRY_IRELAND = 101;

    public static final int COUNTRY_MEXICO = 152;

    public static final int COUNTRY_NETHERLANDS = 161;

    public static final int COUNTRY_NORWAY = 162;

    public static final int COUNTRY_SWEDEN = 191;

    public static final int COUNTRY_UK = 77;

    public static final int COUNTRY_USA = 225;

    public static final int FLAG_HIGHLIGHT = 16;

    public static final int FLAG_LOOTSHARE = 8;

    public static final int FLAG_MEMBERS = 1;

    public static final int FLAG_NON_MEMBERS = 0;

    public static final int FLAG_PVP = 4;

    private static final ArrayList<WorldDef> worldList = new ArrayList<WorldDef>();

    static {
    	//Use Rune-Locus to find other servers' no-ip's.
        worldList.add(new WorldDef(1, 0, FLAG_MEMBERS, "DyNamic PvP World 1", "216.55.161.248", "USA", COUNTRY_USA)); //184.82.211.134
        //worldList.add(new WorldDef(2, 0, FLAG_MEMBERS, "DyNamic PvP World 2", "168.144.43.216", "CANADA", COUNTRY_CANADA));
    }

    public static Message getData(boolean worldConfiguration, boolean worldStatus) {
        MessageBuilder bldr = new MessageBuilder(98, PacketType.VAR_SHORT);
        bldr.writeByte((byte) 1);
        bldr.writeByte((byte) 2);
        bldr.writeByte((byte) 1);
        if (worldConfiguration)
            populateConfiguration(bldr);
        if (worldStatus)
            populateStatus(bldr);
        return bldr.toMessage();
    }

    private static void populateConfiguration(MessageBuilder buffer) {
        writeSmart(buffer, worldList.size());
        setCountry(buffer);
        writeSmart(buffer, 0);
        writeSmart(buffer, (worldList.size() + 1));
        writeSmart(buffer, worldList.size());
        for (WorldDef w : worldList) {
            writeSmart(buffer, w.getWorldId());
            buffer.writeByte((byte) w.getLocation());
            buffer.writeInt(w.getFlag());
            buffer.putGJString(w.getActivity()); // activity
            buffer.putGJString(w.getIp()); // ip writeress
        }
        buffer.writeInt(0x94DA4A87); // != 0
    }

    private static void populateStatus(MessageBuilder buffer) {
        for (WorldDef w : worldList) {
            writeSmart(buffer, w.getWorldId()); // world id
            if (w.getWorldId() != 1)
            	buffer.writeShort(0);
            else
            	buffer.writeShort((short) World.getWorld().getPlayers().size()); // player count
        }
    }

    private static void writeSmart(MessageBuilder buffer, int value) {
        buffer.writeSmart(value);
    }

    private static void setCountry(MessageBuilder buffer) {
        for (WorldDef w : worldList) {
            writeSmart(buffer, w.getCountry());
            buffer.putGJString(w.getRegion());
        }
    }
    
    public static Message getDataForOneWorld(boolean worldConfiguration, boolean worldStatus) {
        MessageBuilder bldr = new MessageBuilder(98, PacketType.VAR_SHORT);
        bldr.writeByte((byte) 1);
        bldr.writeByte((byte) 2);
        bldr.writeByte((byte) 1);
        if (worldConfiguration) {
            writeSmart(bldr, 1); //worldlist size
            setCountry(bldr);
            writeSmart(bldr, 0);
            writeSmart(bldr, (1 + 1)); //worldlist size + 1
            writeSmart(bldr, 1); //wordlist size
            writeSmart(bldr, 1);
            bldr.writeByte(0);
            bldr.writeInt(FLAG_MEMBERS);
            bldr.putGJString("World 1"); // activity
            bldr.putGJString("127.0.0.1"); // ip writeress
            bldr.writeInt(0x94DA4A87); // != 0
        }
        if (worldStatus) {
            writeSmart(bldr, 1); // world id
            bldr.writeShort((short) World.getWorld().getPlayers().size()); // player count
        }
        return bldr.toMessage();
    }
}
