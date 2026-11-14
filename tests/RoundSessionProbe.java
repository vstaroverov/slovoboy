import ru.serg.slogi.*;
import java.util.*;

/** Full-duel model audit with acceleration, exchanges and multi-letter syllables. */
public class RoundSessionProbe {
    public static void main(String[] args) {
        int count=args.length>0?Integer.parseInt(args[0]):500;
        for(int pace:new int[]{2,3}) {
            Random seeds=new Random(628179);
            long seconds=0,rounds=0,wins=0,losses=0,exchange=0,useful=0;int maxRound=0;
            for(int seed=0;seed<count;seed++) {
                Game g=new Game(seeds.nextLong());
                while(g.totalElapsed<Rules.MATCH_SECONDS*20) {
                    if(g.result!=null) {if(g.canContinue())g.continueRound();else break;}
                    if(g.totalElapsed%pace==0){BalanceProbe.act(g);BalanceProbe.offer(g);}
                    g.tick();
                    for(Game.Player p:new Game.Player[]{g.human,g.bot})
                        if(p.hand.size()+(p.offer==null?0:1)>12)throw new AssertionError("Hand overflow");
                }
                if(g.human.hp>0 && g.bot.hp>0)throw new AssertionError("Model stalled after 20 rounds");
                if(g.bot.hp==0)wins++;else losses++;
                seconds+=g.totalElapsed;rounds+=g.round;maxRound=Math.max(maxRound,g.round);
                exchange+=g.exchangeCount;useful+=g.usefulExchangeHuman;
            }
            System.out.printf(Locale.ROOT,"n=%d pace=%d finalWins=%.1f finalLosses=%.1f meanGameSeconds=%.1f meanRounds=%.2f maxRound=%d exchanges=%.2f useful=%.1f noHandOverflow=true%n",count,pace,wins*100.0/count,losses*100.0/count,seconds*1.0/count,rounds*1.0/count,maxRound,exchange*1.0/count,useful*100.0/Math.max(1,exchange));
        }
    }
}
