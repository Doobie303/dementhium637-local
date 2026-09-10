package org.dementhium.model.mask;

import java.util.concurrent.ConcurrentHashMap;
import org.dementhium.cache.CacheManager;

/** Native sequence frame lengths, in the client's 20 ms frames. No cache mutation. */
public final class NPCAnimation {
    private static final ConcurrentHashMap<Integer,Integer> frames = new ConcurrentHashMap<Integer,Integer>();
    private NPCAnimation() { }
    public static int frames(int id) {
        if(id < 0) return 0;
        Integer known=frames.get(id); if(known!=null)return known;
        int duration=0;
        try {
            byte[] data=CacheManager.getData(20,id>>7,id&127);
            // The opcode-1 frame block need not be first (e.g. Zilyana melee).
            // Skip the native 637 sequence fields; unknown layouts fail open.
            java.nio.ByteBuffer input=java.nio.ByteBuffer.wrap(data);
            while(input.hasRemaining()) {
                int opcode=input.get()&255;
                if(opcode==0)break;
                if(opcode==1) {
                    int count=input.getShort()&65535;
                    if(count*6>input.remaining())return 0;
                    for(int i=0;i<count;i++)duration+=input.getShort()&65535;
                    break;
                }
                int skip;
                switch(opcode) {
                    case 2:case 6:case 7:case 19:skip=2;break;
                    case 3:skip=input.get()&255;break;
                    case 5:case 8:case 9:case 10:case 11:skip=1;break;
                    case 12:skip=(input.get()&255)*4;break;
                    case 13:
                        int count=input.getShort()&65535;
                        for(int i=0;i<count;i++){int sounds=input.get()&255;if(sounds>0)input.position(input.position()+3+(sounds-1)*2);}
                        continue;
                    case 14:case 15:case 16:case 18:skip=0;break;
                    case 20:skip=5;break;
                    default:return 0;
                }
                input.position(input.position()+skip);
            }
        } catch(java.io.IOException | RuntimeException unavailable) { return 0; }
        frames.put(id,duration);return duration;
    }
}
