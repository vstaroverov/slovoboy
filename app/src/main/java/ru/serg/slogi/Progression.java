package ru.serg.slogi;

/** Personal progression is independent of the selected game level. */
public final class Progression {
    public static final int XP_PER_WIN=100;
    public int victories;
    public String lastVictory;
    public Progression(int wins,String last){victories=Math.max(0,Math.min(20000000,wins));lastVictory=last==null?"":last;}
    public int level(){int n=(int)((Math.sqrt(1+8.0*victories)-1)/2);while((long)(n+1)*(n+2)/2<=victories)n++;while((long)n*(n+1)/2>victories)n--;return n;}
    public int winsInLevel(){int n=level();return victories-(int)((long)n*(n+1)/2);}
    public int xpInLevel(){return winsInLevel()*XP_PER_WIN;}
    public int xpNeeded(){return (level()+1)*XP_PER_WIN;}
    public long totalXp(){return (long)victories*XP_PER_WIN;}
    /** Durable caller stores wins and lastVictory atomically before saving the match. */
    public boolean reward(String matchId){if(matchId==null||matchId.equals(lastVictory)||victories>=20000000)return false;victories++;lastVictory=matchId;return true;}
}
