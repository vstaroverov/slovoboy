import ru.serg.slogi.*;
import java.io.*;
import java.util.*;

public class UpdateCheck {
    static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);}
    static <T> T copy(T g)throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ObjectOutputStream out=new ObjectOutputStream(bytes)){out.writeObject(g);}
        try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){@SuppressWarnings("unchecked") T result=(T)in.readObject();return result;}
    }
    static void valid(Game.Player p){
        HashSet<String> names=new HashSet<>();int[] lengths=new int[5];
        for(Game.Card c:p.cards){check(names.add(c.word()),"No duplicate on table");lengths[c.parts.length]++;for(String part:c.parts)check(part.length()>=2,"No single-letter slot: "+c.word());}
        check(lengths[2]==3 && lengths[3]==2 && lengths[4]==1,"Table composition");
        for(String token:p.hand)check(token.length()>=2,"No single-letter tile in hand");
        check(p.offer==null || p.offer.length()>=2,"No single-letter offer");
    }
    public static void main(String[] args)throws Exception {
        StartCountdown start=new StartCountdown();check(start.remaining()==2,"User requested two seconds");
        check(!start.tick() && start.remaining()==1,"First second still loading");start=copy(start);
        check(start.remaining()==1 && start.tick() && start.remaining()==0,"Second second starts, save survives");
        HashSet<String> dictionary=new HashSet<>();int[] lengths=new int[5];
        for(WordCatalog.Entry e:WordCatalog.all()) {
            check(dictionary.add(e.word),"Unique dictionary");lengths[e.parts.length]++;
            for(String part:e.parts){check(part.matches("[А-ЯЁ]{2,}"),"At least two Russian letters: "+e.split);int vowels=0;for(char c:part.toCharArray())if("АЕЁИОУЫЭЮЯ".indexOf(c)>=0)vowels++;check(vowels==1,"One vowel: "+e.split);}
        }
        check(dictionary.size()==327 && lengths[2]==166 && lengths[3]==109 && lengths[4]==52,"Expected expanded catalogue");
        Random seeds=new Random(910381);HashSet<String> shown=new HashSet<>();
        for(int i=0;i<1500;i++) {
            Game g=new Game(seeds.nextLong());valid(g.human);valid(g.bot);
            for(Game.Card c:g.human.cards)shown.add(c.word());
            check(g.human.hand.size()==6 && g.bot.hand.size()==6,"Starting hands preserved");
        }
        check(shown.size()==327,"Every catalogue word is reachable");
        for(int round=1;round<=20;round++) {
            Game g=new Game(round);g.round=round;g.bot.hand.clear();for(int n=0;n<12;n++)g.bot.hand.add("НЕСЛОГ");
            int interval=Math.max(1,11-round);check(g.drawInterval()==interval,"Round interval");
            for(int second=1;second<=90;second++){g.human.hand.clear();g.tick();check(g.human.hand.size()==(second%interval==0?1:0),"Exact draw timing");check(g.secondsToDraw()==interval-second%interval,"Display countdown");}
            check(g.canContinue(),"Round result allows continuation");int hand=g.human.hand.size();g.continueRound();check(g.human.hand.size()==hand && g.secondsToDraw()==Math.max(1,10-round),"Next round, no bonus hand");
        }
        check(new Game(71).drawInterval()==10 && Rules.drawSecondsForRound(Integer.MAX_VALUE)==1,"Reset and minimum interval");
        if(args.length>0)try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(args[0]))) {
            Game migrated=(Game)in.readObject();valid(migrated.human);valid(migrated.bot);
            check(migrated.dictionaryUpdatedOnLoad,"Migration detected");
            check(migrated.human.hp==23 && migrated.bot.hp==21 && migrated.round==2 && migrated.elapsed==17 && migrated.totalElapsed==107,"Migration keeps HP and timing");
            check(migrated.human.damageDealt==9 && migrated.human.wordsCompleted==3 && migrated.bot.damageDealt==7,"Migration does not attack or reset stats");
            check(migrated.human.hand.contains("КА") && migrated.human.hand.contains("МА") && migrated.bot.hand.contains("ПА"),"Usable tokens returned");
            check(migrated.human.replace==-1 && migrated.bot.replace==-1,"Old requests cancelled for replaced words");
            Game restored=copy(migrated);check(!restored.dictionaryUpdatedOnLoad,"Migration occurs once");
        }
        System.out.println("Update passed: 327 words, no one-letter syllables; all words reachable; two-second countdown; draws in rounds 1-20; RC3 save migration.");
    }
}
