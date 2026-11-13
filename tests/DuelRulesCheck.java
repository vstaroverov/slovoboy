import ru.serg.slogi.Game;
import ru.serg.slogi.Rules;
import java.util.Arrays;

/** Regression contract: 1:30, complete a word -> one immediate hit -> new word. */
public class DuelRulesCheck {
    static void check(boolean ok,String message) {if(!ok) throw new AssertionError(message);}
    static void finish(Game g,int index) {
        Game.Card card=g.human.cards.get(index);g.human.hand.clear();
        for(int s=0;s<card.parts.length;s++) {
            g.human.hand.add(card.parts[s]);check(g.place(0,index,s),"Valid syllable placement");
        }
    }
    static void checkReplacement(Game.Player p,int index,Game.Card old) {
        Game.Card now=p.cards.get(index);
        check(now!=old && !now.word().equals(old.word()),"Completed word replaced by different word");
        check(now.parts.length==old.parts.length,"Replacement preserves layer");
        for(boolean b:now.filled) check(!b,"New word is empty");
        for(int i=0;i<p.cards.size();i++) if(i!=index) check(!p.cards.get(i).word().equals(now.word()),"No duplicate word");
    }
    public static void main(String[] args) {
        check(Rules.MATCH_SECONDS==90,"User-requested 1:30");
                check(Rules.DRAW_SECONDS==10 && Rules.DRAW_COUNT==1,"User-requested draw: one per ten seconds");
        Game draw=new Game(20);draw.human.hand.clear();draw.bot.hand.clear();
        for(int t=0;t<9;t++) draw.tick();
        check(draw.human.hand.isEmpty() && draw.bot.hand.isEmpty(),"No new cards before ten seconds");
        draw.tick();check(draw.human.hand.size()==1 && draw.bot.hand.size()==1,"Both receive exactly one card at ten seconds");
        draw=new Game(21);draw.human.hand.clear();draw.bot.hand.clear();
        for(int i=0;i<12;i++) draw.human.hand.add("НЕСЛОГ");draw.offer(0);draw.elapsed=9;draw.tick();
        check(draw.human.hand.size()==11 && draw.human.offer!=null,"Offer reserves hand capacity at draw");
                Game invalid=new Game(1);invalid.human.hand.clear();invalid.human.hand.add("НЕСЛОГ");
        check(!invalid.place(0,0,0),"Wrong syllable rejected after attempt");
        check(invalid.human.hand.size()==1 && invalid.human.hand.get(0).equals("НЕСЛОГ") && !invalid.human.cards.get(0).filled[0] && invalid.bot.hp==30,"Mismatch preserves token, slot and HP");
        Game g=new Game(1);Game.Card old=g.human.cards.get(0);
        g.human.hand.clear();g.human.hand.add(old.parts[0]);check(g.place(0,0,0),"First syllable placed");
        check(g.bot.hp==30 && g.attackId==0,"Partial word does not damage");
        g.take(0,0);check(g.human.hand.size()==1 && !old.filled[0],"Incomplete word can be transferred");
        finish(g,0);check(g.bot.hp==29 && g.elapsed==0,"Damage occurs immediately, without a timer tick");
        check(g.human.completed()==1 && g.human.score()==1 && g.attackId==1,"One hit counted");
        check(g.human.hand.isEmpty(),"Completed word consumes its tokens");checkReplacement(g.human,0,old);
        g.tick();check(g.bot.hp==29 && g.attackId==1,"No repeat attack after completion");
        old=g.human.cards.get(0);finish(g,0);check(g.bot.hp==28 && g.human.completed()==2,"New word can attack again");checkReplacement(g.human,0,old);
        g=new Game(2);finish(g,3);check(g.bot.hp==27,"Three syllables deal 3 damage");
        g=new Game(2);finish(g,5);check(g.bot.hp==25,"Four syllables deal 5 damage");
        g=new Game(2);g.human.replace=0;finish(g,0);check(g.human.replace==-1,"Completion cancels request belonging to old word");
        g=new Game(3);old=g.bot.cards.get(0);
        Arrays.fill(old.filled,true);old.filled[old.parts.length-1]=false;
        g.bot.hand.clear();g.bot.hand.add(old.parts[old.parts.length-1]);g.elapsed=1;g.tick();
        check(g.human.hp==29 && g.bot.completed()==1 && !g.lastAttackHuman,"Bot has same immediate-hit rule");checkReplacement(g.bot,0,old);
        g=new Game(4);g.bot.hp=1;finish(g,0);
        check(g.bot.hp==0 && "Победа в 1 раунде!".equals(g.result),"Lethal human hit ends immediately and clamps HP");
        int time=g.elapsed;g.tick();check(g.elapsed==time,"Finished clock frozen");
        check(!g.place(0,0,0),"Finished actions rejected");
        g=new Game(5);old=g.bot.cards.get(0);Arrays.fill(old.filled,true);old.filled[0]=false;
        g.bot.hand.clear();g.bot.hand.add(old.parts[0]);g.human.hp=1;g.elapsed=1;g.tick();
        check(g.human.hp==0 && "Поражение в 1 раунде!".equals(g.result),"Lethal bot hit ends immediately");
        g=new Game(6);g.bot.hand.clear();for(int i=0;i<12;i++) g.bot.hand.add("НЕСЛОГ");
        for(int t=0;t<89;t++) {g.tick();check(g.result==null,"No premature timeout or passive damage");}
        g.tick();check(g.elapsed==90 && g.human.hp==30 && g.bot.hp==30 && "Результат 1 раунда!".equals(g.result) && g.canContinue(),"90-second equal HP draw, no passive attacks");
        g=new Game(7);g.elapsed=89;g.human.hp=21;g.bot.hp=20;g.bot.hand.clear();g.tick();
        check("Результат 1 раунда!".equals(g.result) && g.canContinue(),"Timeout reports round regardless of HP lead");
        g=new Game(8);g.human.hand.clear();g.bot.hand.clear();g.human.hand.add("А");g.bot.offer="Б";g.offer(0);
        check(g.human.hand.get(0).equals("Б") && g.bot.hand.get(0).equals("А") && g.human.offer==null && g.bot.offer==null,"Blind exchange conserves tokens");
        g.offer(0);g.offer(-1);check(g.human.offer==null && g.human.hand.contains("Б"),"Offer cancellable");
        g=new Game(2);old=g.human.cards.get(0);g.requestCard(0);check(g.human.cards.get(0)==old,"Dealer waits for consent");
        g.bot.replace=3;g.requestCard(1);check(g.human.cards.get(1).parts.length==2 && g.bot.cards.get(3).parts.length==3,"Mutual dealer preserves layers");
        check(g.human.hp==30 && g.bot.hp==30 && g.attackId==0,"Dealer does not inflict damage");
        for(int seed=0;seed<500;seed++) {
            g=new Game(seed);
            for(int t=0;t<Rules.MATCH_SECONDS;t++) {
                if(g.result==null) {
                    boolean placed=false;
                    for(int h=0;h<g.human.hand.size() && !placed;h++) for(int c=0;c<6 && !placed;c++)
                        for(int s=0;s<g.human.cards.get(c).parts.length && !placed;s++) placed=g.place(h,c,s);
                    if(t%5==0 && !g.human.hand.isEmpty()) g.offer(0);
                    if(t%11==0) g.requestCard(seed%6);
                    g.tick();
                }
                for(Game.Player player:new Game.Player[]{g.human,g.bot}) {
                    check(player.hp>=0 && player.hp<=Rules.START_HP,"HP bounds");
                    check(player.hand.size()+(player.offer==null?0:1)<=Rules.HAND_LIMIT,"Hand capacity");
                    int[] counts=new int[5];for(Game.Card c:player.cards) {
                        counts[c.parts.length]++;check(!c.complete(),"No completed word remains to attack repeatedly");
                    }
                    check(counts[2]==3 && counts[3]==2 && counts[4]==1,"Six slots preserve 3/2/1 composition");
                }
            }
            check(g.result!=null && g.elapsed<=90,"Every match ends by 90 seconds");
        }
                for(int seed=0;seed<200;seed++) {
            Game opening=new Game(seed);
            for(Game.Player player:new Game.Player[]{opening.human,opening.bot}) {
                check(player.hand.size()==Rules.START_HAND,"Opening hand count unchanged");
                boolean possible=false;
                for(int i=0;i<3;i++) {
                    java.util.ArrayList<String> copy=new java.util.ArrayList<>(player.hand);boolean fits=true;
                    for(String token:player.cards.get(i).parts) if(!copy.remove(token)) fits=false;
                    possible|=fits;
                }
                check(possible,"Each side has a completable two-syllable opening");
            }
        }
        Game cooldown=new Game(11);cooldown.bot.hand.clear();cooldown.bot.hand.add("НЕСЛОГ");
        cooldown.bot.lastReceived="НЕСЛОГ";cooldown.bot.receivedAt=0;cooldown.elapsed=1;cooldown.totalElapsed=1;cooldown.tick();
        check(cooldown.bot.offer==null,"Bot does not immediately bounce a received token");
        while(cooldown.elapsed<Rules.EXCHANGE_HOLD_SECONDS) cooldown.tick();
        check("НЕСЛОГ".equals(cooldown.bot.offer),"Cooldown ends; useless token may be offered again");
                java.util.HashSet<String> dictionary=new java.util.HashSet<>();
        for(String[] group:Game.SETS) {
            check(group.length==6,"Six words per group");
            for(int i=0;i<group.length;i++) {
                String[] parts=group[i].split("-");int expected=i<3?2:i<5?3:4;
                check(parts.length==expected,"Dictionary layer lengths");
                check(dictionary.add(group[i].replace("-","")),"Dictionary contains no duplicate word");
                for(String part:parts) {
                    int vowels=0;for(char ch:part.toCharArray()) if("АЕЁИОУЫЭЮЯ".indexOf(ch)>=0) vowels++;
                    check(vowels==1,"One vowel per syllable fragment");
                }
            }
        }
        check(dictionary.size()==36,"Release dictionary has 36 words");
        try {
            Game original=new Game(99);original.tick();
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
            try(java.io.ObjectOutputStream out=new java.io.ObjectOutputStream(bytes)){out.writeObject(original);}
            Game restored;
            try(java.io.ObjectInputStream in=new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))){restored=(Game)in.readObject();}
            for(int i=0;i<89;i++) {original.tick();restored.tick();}
            check(original.elapsed==restored.elapsed && original.human.hp==restored.human.hp && original.bot.hp==restored.bot.hp,"Restored game preserves timing and damage");
            check(original.human.hand.equals(restored.human.hand) && original.bot.hand.equals(restored.bot.hand),"Restored random sequence preserves subsequent deals");
        } catch(Exception e){throw new AssertionError("Save roundtrip",e);}
                check(Rules.damageForLength(2)==1 && Rules.damageForLength(3)==3 && Rules.damageForLength(4)==5,"Pinned damage values 1/3/5");
        Game rounds=new Game(41);rounds.elapsed=89;rounds.totalElapsed=89;rounds.bot.hand.clear();
        rounds.human.cards.get(0).filled[0]=true;rounds.human.hp=24;rounds.bot.hp=28;
        rounds.human.damageDealt=2;rounds.human.roundDamage=2;rounds.human.wordsCompleted=1;rounds.human.roundWords=1;
        for(Game.Card card:rounds.bot.cards) card.filled[0]=true;
        rounds.human.offer="ОБМЕН";rounds.human.replace=4;rounds.tick();
        check(rounds.canContinue() && rounds.result.equals("Результат 1 раунда!"),"Round timeout is not victory or defeat");
        Game.Card cardBefore=rounds.human.cards.get(0);java.util.ArrayList<String> handBefore=new java.util.ArrayList<>(rounds.human.hand);
        check(rounds.continueRound(),"Next round allowed after timeout");
        check(rounds.round==2 && rounds.elapsed==0 && rounds.totalElapsed==90 && rounds.result==null,"Next round resets only round clock and result");
        check(rounds.human.hp==24 && rounds.bot.hp==28 && rounds.human.cards.get(0)==cardBefore && cardBefore.filled[0],"HP and card progress preserved");
        check(rounds.human.hand.equals(handBefore) && "ОБМЕН".equals(rounds.human.offer) && rounds.human.replace==4,"Hand, offer and dealer request preserved");
        check(rounds.human.damageDealt==2 && rounds.human.roundDamage==0 && rounds.human.wordsCompleted==1 && rounds.human.roundWords==0,"Per-round stats reset while duel totals remain");
        check(!rounds.continueRound(),"Cannot skip an active round");
        rounds.human.offer=null;rounds.bot.hand.clear();rounds.bot.hand.add("НЕСЛОГ");rounds.bot.offer=null;rounds.bot.lastReceived="НЕСЛОГ";rounds.bot.receivedAt=88;
        rounds.tick();rounds.tick();check(rounds.bot.offer==null,"Exchange cooldown survives round reset");
        rounds.tick();rounds.tick();check("НЕСЛОГ".equals(rounds.bot.offer),"Cooldown expires using monotonic duel time");
        rounds.bot.hp=1;finish(rounds,1);
        check("Победа в 2 раунде!".equals(rounds.result) && !rounds.canContinue() && !rounds.continueRound(),"Lethal hit gives round-specific victory with no continuation");
        System.out.println("Immediate-hit rules: completion, renewal, HP, exchange, timeout and 500 seeded matches passed");
    }
}
