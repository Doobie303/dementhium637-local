package org.dementhium.content.activity.impl.barrows;

import java.util.LinkedHashMap;
import java.util.Map;

/** A player's door state. Keys never overlap object or coordinate bits. */
public final class BarrowsTunnels {
    private final Map<Long,Gate> gates=new LinkedHashMap<Long,Gate>();
    private final int centralSide;
    public BarrowsTunnels(int layout) {
        centralSide=Math.floorMod(layout,4);
        for(Gate original:BarrowsConstants.GATES) {
            Gate gate=original.duplicate();
            int side=centralSide(gate);
            int edge=perimeterEdge(gate);
            gate.setClosed(side>=0 ? side!=centralSide : edge<0 || edge==Math.floorMod(layout/4,8));
            gates.put(BarrowsRules.gateKey(gate.getId(),gate.getLocation().getX(),
                    gate.getLocation().getY(),gate.getLocation().getZ()),gate);
        }
    }
    /** Closing one edge of the outer-room cycle keeps all eight rooms connected. */
    public static int perimeterEdge(Gate gate) {
        int x=gate.getLocation().getX(),y=gate.getLocation().getY();
        if(y==9711||y==9712) {
            if(x==3541||x==3545)return 0;
            if(x==3558||x==3562)return 1;
        }
        if(x==3568||x==3569) {
            if(y==9701||y==9705)return 2;
            if(y==9684||y==9688)return 3;
        }
        if(y==9677||y==9678) {
            if(x==3558||x==3562)return 4;
            if(x==3541||x==3545)return 5;
        }
        if(x==3534||x==3535) {
            if(y==9684||y==9688)return 6;
            if(y==9701||y==9705)return 7;
        }
        return -1;
    }
    public Map<Long,Gate> getGates(){return gates;}
    public Gate get(int id,int x,int y,int z){return gates.get(BarrowsRules.gateKey(id,x,y,z));}
    public boolean isPuzzleGate(Gate gate){return centralSide(gate)>=0 && !gate.isClosed();}
    public static int centralSide(Gate gate) {
        int x=gate.getLocation().getX(), y=gate.getLocation().getY();
        if((x==3551||x==3552) && (y==9701||y==9705))return 0;
        if((x==3558||x==3562) && (y==9694||y==9695))return 1;
        if((x==3551||x==3552) && (y==9684||y==9688))return 2;
        if((x==3541||x==3545) && (y==9694||y==9695))return 3;
        return -1;
    }
}
