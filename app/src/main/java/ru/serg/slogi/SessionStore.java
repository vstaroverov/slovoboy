package ru.serg.slogi;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;

/** Private on-device save; no network, permissions or external-storage access. */
final class SessionStore {
    static final class State implements Serializable {
        private static final long serialVersionUID=1L;
        final int schema=1;
        final Game game;
        final boolean started;
        final int selected;
        final int inputSchema;
        final int targetCard,targetSlot;
        final String targetWord;
        final StartCountdown countdown;
        State(Game g,boolean s,int i,int c,int slot,String word,StartCountdown start) {inputSchema=2;game=g;started=s;selected=i;targetCard=c;targetSlot=slot;targetWord=word;countdown=start;}
    }
    private final AtomicFile file;
    SessionStore(Context context) {file=new AtomicFile(new File(context.getFilesDir(),"match-v1.bin"));}
    State load() {
        try {
            byte[] bytes=file.readFully();
            if(bytes.length>262144) throw new IOException("Invalid save size");
            try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                State s=(State)in.readObject();
                if(s.schema!=1 || s.game==null || s.game.elapsed<0 || s.game.elapsed>Rules.MATCH_SECONDS || s.game.human.cards.size()!=6 || s.game.bot.cards.size()!=6) throw new IOException("Invalid save");
                return s;
            }
        } catch(FileNotFoundException e) {return null;}
        catch(IOException|ClassNotFoundException|ClassCastException e) {file.delete();return null;}
    }
    boolean save(Game game,boolean started,int selected,int targetCard,int targetSlot,String targetWord,StartCountdown countdown) {
        FileOutputStream out=null;
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(ObjectOutputStream objects=new ObjectOutputStream(bytes)) {objects.writeObject(new State(game,started,selected,targetCard,targetSlot,targetWord,countdown));}
            out=file.startWrite();out.write(bytes.toByteArray());file.finishWrite(out);return true;
        } catch(IOException e) {if(out!=null)file.failWrite(out);return false;}
    }
}
