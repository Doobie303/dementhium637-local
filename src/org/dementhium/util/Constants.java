package org.dementhium.util;

public class Constants {

    public static final int[] PVP_REWARDS = {
            13858, 13861, 13864, 13867,
            13870, 13873, 13876, 13879, 13883, 13884, 13890, 13896, 13902,
            13887, 13893, 13899, 13905, 13932, 13935, 13938, 13941, 13944,
            13947, 13950, 13953, 13908, 13914, 13920, 13926, 13911, 13917,
            13923, 13929,
    };

    public static final int[] W_GUILD_CATAPULT_TABS = {
            2, 3, 4, 6, 7, 8, 12, 13, 15
    };

    public static final int BARROW_KILL_COUNT = 0, ARMADYL_KILL_COUNT = 1, BANDOS_KILL_COUNT = 2, SARA_KILL_COUNT = 3, ZAMMY_KILL_COUNT = 4, ZAROS_KILL_COUNT = 5;


    public final static int REVISION = 637;
    public final static String SERVER_NAME = "DyNamics";
    public final static int PORT_ID = 43594;

    public static final int SPEC_BAR_FULL = 1000;
    public static final int SPEC_BAR_HALF = 500;

    public final static byte DISCONNECT = -1;
    public final static byte GET_CONNECTION_ID = 0;
    public final static byte LOGIN_START = 1;
    public final static byte LOGIN_CYPTION = 2;
    public final static byte CHECK_ACC_NAME = 3;
    public final static byte CHECK_ACC_COUNTRY = 4;
    public final static byte UPDATE_SERVER = 6;

	/*
	 * 1=could not display vid advert, 2=stuck at performing login, 3=inv.username or pass, 4=acc disabled, 5=not logged out from last session
	 * 6=RSPS has been updated, reload page, 7=server full, 8=loginserver offline, 9=login limit exceeded, 10=bad session ID
	 * 11=weak pass, 12=Members world (when f2p), 13=try different server, 14=wait few mins and try again (update), 15=try different server (unexpected server response)
	 * 16=too many wrong pass tries, wait 5 mins, 17=move to f2p area, 18=Locked acc, 19=Fullscreen is p2p only, 20=invalid loginserver requested
	 * 21=performing login (stuck), 22=Malformed login packet, 23=wait min try again, 24=contact customer support, 25=try diff server
	 * 26=ip ban, 27=Service unavailable., 28=try diff server, 29=performing login stuck, 30=this is not a mems acc
	 * 31=try diff server, 32=try diff server, 33=try diff server, 34=try diff server, 35=try diff server
	 */
    
    public final static byte MAKE_ACC = 5;
    public final static byte REMOVE_ID = 100;
    public static final byte LOGIN_OK = 2;
    public static final byte INVALID_PASSWORD = 3;
    public static final byte BANNED = 4;
    public static final byte IP_BANNED = 26;
    public static final byte ALREADY_ONLINE = 5;
    public static final byte WORLD_FULL = 7;
    public static final byte TRY_AGAIN = 11;
    public static final byte ERROR_LOADING_PROFILE = 24;

    public static final short MAX_AMT_OF_PLAYERS = 2048;
    public static final int MAX_AMT_OF_NPCS = 25000;

    public static final byte LOBBY_PM_CHAT_MESSAGE = 0;
    public static final byte LOBBY_CLAN_CHAT_MESSAGE = 11;
    public static final byte COMMANDS_MESSAGE = 99;

    public static final boolean CONNECTING_TO_FORUMS = true;

    public static final byte[] PACKET_SIZES = new byte[]{
            8, -1, -1, 16, 6, 2,
            8, 6, 3, -1, 16, 15, 0, 8, 11, 8, -1, -1, 3, 2, -3, -1, 7, 2, -1,
            7, -1, 3, 3, 6, 4, 3, 0, 3, 4, 5, -1, -1, 7, 8, 4, -1, 4, 7, 3, 15,
            8, 3, 2, 4, 18, -1, 1, 3, 7, 7, 4, -1, 8, 2, 7, -1, 1, -1, 3, 2,
            -1, 8, 3, 2, 3, 7, 3, 8, -1, 0, 7, -1, 11, -1, 3, 7, 8, 12, 4, -3,
            -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
            -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
            -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
            -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
            -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
            -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
            -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
            -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
            -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
            -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3};

    public static final String CACHE_DIRECTORY = "data/cache/";

    static {
        PACKET_SIZES[35] = 5;
    }

}
