package org.dementhium.io;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Redo journal: a durable commit contains BOTH account post-images. Never store passwords in logs. */
public final class DuelJournal {
    private final Path directory, journal;
    public DuelJournal(Path directory) { this.directory=directory; journal=directory.resolve("duel-commit.bin"); }
    private Path account(String name) throws IOException {
        if (!name.matches("[a-zA-Z0-9 _-]{1,12}")) throw new IOException("Invalid account filename");
        return directory.resolve(name+".bin");
    }
    public static void atomicWrite(Path path, byte[] bytes) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path tmp=Files.createTempFile(path.toAbsolutePath().getParent(),"duel-",".tmp");
        try {
            try(FileOutputStream out=new FileOutputStream(tmp.toFile())) { out.write(bytes); out.getFD().sync(); }
            Files.move(tmp,path.toAbsolutePath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(tmp); }
    }
    public void recover() throws IOException {
        if (!Files.exists(journal)) return;
        Map<String,byte[]> images=new LinkedHashMap<String,byte[]>();
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(journal)))) {
            if(in.readInt()!=0x44554c31 || in.readInt()!=2) throw new IOException("Invalid duel journal");
            for(int n=0;n<2;n++) {
                String name=in.readUTF(); account(name);
                int size=in.readInt(); if(size<1 || size>16000000) throw new IOException("Invalid account image size");
                byte[] data=new byte[size];in.readFully(data);
                if(images.put(name,data)!=null) throw new IOException("Duplicate journal account");
            }
            if(in.read()!=-1) throw new IOException("Unexpected journal trailer");
        }
        for(Map.Entry<String,byte[]> e:images.entrySet()) atomicWrite(account(e.getKey()),e.getValue());
        Files.delete(journal);
    }
    /** A return means committed, even if installation must be retried before the next read/write. */
    public void commit(String a,byte[] first,String b,byte[] second) throws IOException {
        recover(); account(a);account(b);
        if(a.equalsIgnoreCase(b)) throw new IOException("Same duel account");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.writeInt(0x44554c31);out.writeInt(2);
            out.writeUTF(a);out.writeInt(first.length);out.write(first);
            out.writeUTF(b);out.writeInt(second.length);out.write(second);
        }
        atomicWrite(journal,bytes.toByteArray());
        try { recover(); } catch(IOException pending) {
            System.err.println("Duel committed; account installation pending. Further account I/O will retry recovery.");
        }
    }
}
