package ru.serg.slogi;

import java.io.Serializable;
import java.util.*;

/** Bounded by the dictionary; counts presentations, never guesses by completion. */
public final class WordHistory implements Serializable {
    private static final long serialVersionUID=1L;
    private final HashMap<String,Integer> seen=new HashMap<>();
    private final HashMap<String,Long> last=new HashMap<>();
    private long draws;
    public int count(String word) {Integer n=seen.get(word);return n==null?0:n;}
    public void shown(String word) {
        seen.put(word,Math.min(1000000,count(word)+1)); last.put(word,++draws);
    }
    public double weight(String word) {
        int n=count(word);
        if(n==0) return 12.0;
        Long when=last.get(word);
        double recency=when!=null && draws-when<12 ? 0.25 : 1.0;
        return Math.max(1.0,12.0/(1.0+n))*recency;
    }
    public void merge(WordHistory other) {
        if(other==null || other==this) return;
        for(Map.Entry<String,Integer> e:other.seen.entrySet())
            seen.put(e.getKey(),Math.max(count(e.getKey()),e.getValue()));
        for(Map.Entry<String,Long> e:other.last.entrySet()) {
            Long old=last.get(e.getKey());
            if(old==null || e.getValue()>old) last.put(e.getKey(),e.getValue());
        }
        draws=Math.max(draws,other.draws);
    }
    public static final class Profile implements Serializable {
        private static final long serialVersionUID=1L;
        public final WordHistory human=new WordHistory(),bot=new WordHistory();
        public void merge(Profile other) {if(other!=null){human.merge(other.human);bot.merge(other.bot);}}
    }
}
