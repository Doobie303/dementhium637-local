package org.dementhium.content.skills.fishing;

public enum Fish {

    SHRIMP(317, 1, 1),
    SARDINE(327, 3, 5),
    HERRING(345, 5, 10),
    ANCHOVIES(321, 7, 15),
    MACKEREL(353, 9, 16),
    TROUT(335, 11, 20),
    COD(341, 13, 23),
    PIKE(349, 15, 25),
    SALMON(331, 17, 30),
    TUNA(359, 19, 35),
    LOBSTER(377, 22, 40),
    BASS(363, 24, 46),
    SWORDFISH(371, 27, 50),
    MONKFISH(7944, 30, 62),
    SHARK(383, 32, 76),
    SEA_TURTLE(395, 36, 79),
    MANTA_RAY(389, 40, 81),
    CAVE_FISH(15264, 43, 85),
    ROCKTAIL(15270, 54, 90);

    private final int id;
    private final int xp;
    private final int level;

    private Fish(int id, int xp, int level) {
        this.id = id;
        this.xp = xp;
        this.level = level;
    }

    public int getId() {
        return id;
    }

    public int getXp() {
        return xp;
    }

    public int getLevel() {
        return level;
    }
}