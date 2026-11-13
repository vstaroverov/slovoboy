import ru.serg.slogi.*;
import java.util.*;

public class BalanceProbe {
    static boolean useful(Game.Player p,String token) {for(Game.Card c:p.cards) for(int s=0;s<c.parts.length;s++) if(!c.filled[s]&&c.parts[s].equals(token)) return true;return false;}
    static boolean act(Game g) {
        int best=-999, bh=-1,bc=-1,bs=-1;
        for(int c=0;c<6;c++) {
            Game.Card card=g.human.cards.get(c);HashMap<String,Integer> available=new HashMap<>();
            for(String s:g.human.hand) available.put(s,available.getOrDefault(s,0)+1);
            int missing=0,filled=0,found=0;
            for(int s=0;s<card.parts.length;s++) if(card.filled[s]) filled++;else {
                missing++;int n=available.getOrDefault(card.parts[s],0);
                if(n>0){found++;available.put(card.parts[s],n-1);}
            }
            int value=(found==missing?100:0)+filled*8+found*5-missing*3;
            for(int h=0;h<g.human.hand.size();h++) for(int s=0;s<card.parts.length;s++)
                if(!card.filled[s]&&card.parts[s].equals(g.human.hand.get(h))&&value>best){best=value;bh=h;bc=c;bs=s;}
        }
        return bh>=0&&g.place(bh,bc,bs);
    }
    static void offer(Game g) {
        if(g.human.offer!=null)return;
        for(int h=0;h<g.human.hand.size();h++) if(!useful(g.human,g.human.hand.get(h))){g.offer(h);return;}
    }
    public static void main(String[] args) {
        int seeds=args.length>0?Integer.parseInt(args[0]):2000;
        for(boolean trades:new boolean[]{false,true}) for(int pace:new int[]{2,3}) {
            Random random=new Random(51423);long wins=0,loss=0,ties=0,idle=0,actions=0,words=0,botwords=0,damage=0,exchanges=0,good=0,drawsUseful=0,draws=0,zeroWords=0;
            for(int seed=0;seed<seeds;seed++) {
                Game g=new Game(random.nextLong());int lastEvent=g.eventId;
                for(int t=0;t<90&&g.result==null;t++) {
                    if(t%pace==0){actions++;if(!act(g))idle++;if(trades)offer(g);}
                    int handBefore=g.human.hand.size();g.tick();
                    lastEvent=g.eventId;
                    if(g.elapsed%10==0&&g.human.hand.size()>handBefore){draws++;if(useful(g.human,g.human.hand.get(g.human.hand.size()-1)))drawsUseful++;}
                }
                exchanges+=g.exchangeCount;good+=g.usefulExchangeHuman;if(g.human.wordsCompleted==0)zeroWords++;
                if(g.human.hp>g.bot.hp)wins++;else if(g.human.hp<g.bot.hp)loss++;else ties++;
                words+=g.human.wordsCompleted;botwords+=g.bot.wordsCompleted;damage+=g.human.damageDealt;
            }
            System.out.printf(Locale.ROOT,"trades=%s pace=%d n=%d win=%.1f loss=%.1f draw=%.1f words=%.2f botWords=%.2f damage=%.2f idle=%.1f exchanges/game=%.2f usefulExchange=%.1f usefulDraw=%.1f zeroHits=%.1f%n",trades,pace,seeds,100.0*wins/seeds,100.0*loss/seeds,100.0*ties/seeds,1.0*words/seeds,1.0*botwords/seeds,1.0*damage/seeds,100.0*idle/actions,1.0*exchanges/seeds,exchanges==0?0:100.0*good/exchanges,100.0*drawsUseful/Math.max(1,draws),100.0*zeroWords/seeds);
        }
    }
}
