package org.dementhium.model.npc.godwars;

/** Provisional pre-EoC profile; hidden packed accuracy bonuses are intentionally separate. */
public enum GodWarsType {
    BANDOS(6260,new int[]{6261,6263,6265},2864,5351,2876,5369,2,6),
    SARADOMIN(6247,new int[]{6248,6250,6252},2895,5258,2909,5275,0,2),
    ZAMORAK(6203,new int[]{6204,6206,6208},2923,5316,2937,5330,2,6),
    ARMADYL(6222,new int[]{6223,6225,6227},2824,5296,2842,5308,2,3);
    public final int boss,minX,minY,maxX,maxY,plane,speed;
    public final int[] followers;
    GodWarsType(int boss,int[] followers,int minX,int minY,int maxX,int maxY,int plane,int speed){
        this.boss=boss;this.followers=followers;this.minX=minX;this.minY=minY;this.maxX=maxX;this.maxY=maxY;this.plane=plane;this.speed=speed;
    }
    public boolean has(int id){if(id==boss)return true;for(int f:followers)if(f==id)return true;return false;}
    public static GodWarsType forId(int id){for(GodWarsType type:values())if(type.has(id))return type;throw new IllegalArgumentException("Not original GWD: "+id);}
}
