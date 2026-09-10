package org.dementhium.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** One durable redo record coordinates the account, finite house and round receipt. */
public final class GamblerJournal {
    private final Path directory, journal, house, marker;
    public GamblerJournal(Path directory) {
        this.directory=directory; journal=directory.resolve("gamble-commit.bin");
        house=directory.resolve("gamble-house.bin");marker=directory.resolve("gamble-initialized");
    }
    private Path account(String name) throws IOException {
        if(!name.matches("[a-zA-Z0-9 _-]{1,12}"))throw new IOException("Invalid account filename");
        return directory.resolve(name+".bin");
    }
    public static byte[] encode(Map<Integer,Long> balances) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.writeInt(0x47485331);out.writeInt(balances.size());
            for(Map.Entry<Integer,Long> e:balances.entrySet()) {out.writeInt(e.getKey());out.writeLong(e.getValue());}
        }
        return bytes.toByteArray();
    }
    public static Map<Integer,Long> decode(byte[] bytes) throws IOException {
        Map<Integer,Long> balances=new LinkedHashMap<Integer,Long>();
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))) {
            if(in.readInt()!=0x47485331)throw new IOException("Invalid house");
            int count=in.readInt();if(count<1||count>100)throw new IOException("Invalid house count");
            for(int n=0;n<count;n++) {
                int id=in.readInt();long stock=in.readLong();
                if(id<0||stock<0||balances.put(id,stock)!=null)throw new IOException("Invalid house stock");
            }
            if(in.read()!=-1)throw new IOException("Unexpected house trailer");
        }
        return balances;
    }
    public Map<Integer,Long> balances(Map<Integer,Long> seeds) throws IOException {
        recover();
        if(!Files.exists(house)) {
            if(Files.exists(marker))throw new IOException("House missing; refusing to reseed");
            DuelJournal.atomicWrite(house,encode(seeds));
        }
        Map<Integer,Long> result=decode(Files.readAllBytes(house));
        if(!Files.exists(marker))DuelJournal.atomicWrite(marker,new byte[]{1});
        return result;
    }
    private static byte[] readImage(DataInputStream in,int max) throws IOException {
        int size=in.readInt();if(size<=0||size>max)throw new IOException("Invalid image size");
        byte[] image=new byte[size];in.readFully(image);return image;
    }
    private static void writeImage(DataOutputStream out,byte[] image) throws IOException {
        out.writeInt(image.length);out.write(image);
    }
    public void recover() throws IOException {
        if(!Files.exists(journal))return;
        String name;UUID id;byte[] player,stock,receipt;
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(journal)))) {
            if(in.readInt()!=0x474a5231)throw new IOException("Invalid gambler journal");
            name=in.readUTF();account(name);id=new UUID(in.readLong(),in.readLong());
            player=readImage(in,16000000);stock=readImage(in,1600);receipt=readImage(in,4096);decode(stock);
            if(in.read()!=-1)throw new IOException("Unexpected gambler journal trailer");
        }
        // Retrying writes the same images/receipt, never adds a second reward.
        DuelJournal.atomicWrite(account(name),player);
        DuelJournal.atomicWrite(house,stock);
        DuelJournal.atomicWrite(directory.resolve("gamble-rounds").resolve(id+".txt"),receipt);
        Files.delete(journal);
    }
    /** Once the journal is durable, callers must retain their new in-memory state. */
    public void commit(String name,byte[] player,Map<Integer,Long> stock,UUID id,String receipt) throws IOException {
        recover();account(name);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.writeInt(0x474a5231);out.writeUTF(name);out.writeLong(id.getMostSignificantBits());out.writeLong(id.getLeastSignificantBits());
            writeImage(out,player);writeImage(out,encode(stock));writeImage(out,receipt.getBytes(StandardCharsets.UTF_8));
        }
        DuelJournal.atomicWrite(journal,bytes.toByteArray());
        try {recover();} catch(Exception pending) {
            System.err.println("Gambler round committed; installation will retry before account I/O.");
        }
    }
}
