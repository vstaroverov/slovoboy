import ru.serg.slogi.*;
import java.util.*;

/** Actual game transitions: no premature hit, false exchange, or result replay. */
public class GameAudioCheck {
    static void check(boolean yes,String message) {if(!yes)throw new AssertionError(message);}
    static void finish(Game g,int card) {
        Game.Card word=g.human.cards.get(card);g.human.hand.clear();
        for(int s=0;s<word.parts.length;s++){g.human.hand.add(word.parts[s]);g.place(0,card,s);}
    }
    public static void main(String[] args) {
        Game g=new Game(3);GameAudio.Before before=new GameAudio.Before(g);
        check(GameAudio.changed(before,g)==null,"No cue on restore or rendering");
        Game.Card card=g.human.cards.get(0);g.human.hand.clear();g.human.hand.add(card.parts[0]);
        before=new GameAudio.Before(g);g.place(0,0,0);
        check(GameAudio.changed(before,g)==null,"Partial word uses placement, not attack sound");
        g.human.hand.clear();g.human.hand.add(card.parts[1]);before=new GameAudio.Before(g);g.place(0,0,1);
        check(GameAudio.changed(before,g)==GameAudio.Cue.HIT,"Human completion is a hit");
        before=new GameAudio.Before(g);g.bot.hp=1;finish(g,0);
        check(GameAudio.changed(before,g)==GameAudio.Cue.WIN,"Victory overrides hit");
        before=new GameAudio.Before(g);g.tick();
        check(GameAudio.changed(before,g)==null,"Result never repeats while stopped");
        g=new Game(4);card=g.bot.cards.get(0);Arrays.fill(card.filled,true);card.filled[0]=false;
        g.bot.hand.clear();g.bot.hand.add(card.parts[0]);g.elapsed=1;before=new GameAudio.Before(g);g.tick();
        check(GameAudio.changed(before,g)==GameAudio.Cue.BOT_HIT,"Bot completion has different sound");
        g=new Game(4);card=g.bot.cards.get(0);Arrays.fill(card.filled,true);card.filled[0]=false;
        g.bot.hand.clear();g.bot.hand.add(card.parts[0]);g.elapsed=1;g.human.hp=1;before=new GameAudio.Before(g);g.tick();
        check(GameAudio.changed(before,g)==GameAudio.Cue.LOSE,"Defeat overrides bot hit");
        g=new Game(8);g.elapsed=89;g.bot.hand.clear();before=new GameAudio.Before(g);g.tick();
        check(GameAudio.changed(before,g)==GameAudio.Cue.ROUND,"Round result overrides final draw");
        before=new GameAudio.Before(g);g.continueRound();
        check(GameAudio.changed(before,g)==null,"Round start is explicit, old result is not replayed");
        g=new Game(9);g.bot.hand.clear();g.human.hand.clear();g.human.hand.add("А");before=new GameAudio.Before(g);g.offer(0);
        check(GameAudio.changed(before,g)==null,"Pending offer is not an exchange");
        g.offer(-1);g.bot.offer="Б";before=new GameAudio.Before(g);g.offer(0);
        check(GameAudio.changed(before,g)==GameAudio.Cue.EXCHANGE,"Actual swap is an exchange, not draw");
        g=new Game(10);g.human.hand.clear();g.bot.hand.clear();g.elapsed=9;before=new GameAudio.Before(g);g.tick();
        check(GameAudio.changed(before,g)==GameAudio.Cue.DRAW,"Ten-second deal has sound");
        g=new Game(11);g.bot.replace=3;before=new GameAudio.Before(g);g.requestCard(0);
        check(GameAudio.changed(before,g)==GameAudio.Cue.DEALER,"Mutual dealer exchange distinct from token swap");
        System.out.println("Game audio passed: hits, terminal priority, true exchanges, draw, dealer, no result replay.");
    }
}
