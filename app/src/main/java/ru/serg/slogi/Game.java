package ru.serg.slogi;

import java.io.Serializable;
import java.util.*;

/** All hidden state stays here; the UI renders only opponent cards and health. */
public final class Game implements Serializable {
    private static final long serialVersionUID=1L;
    public static final int LIMIT = Rules.MATCH_SECONDS, HAND_LIMIT = Rules.HAND_LIMIT;
    public transient boolean dictionaryUpdatedOnLoad;
    public int level; // 0: legacy update1 save, 1: exact RC3, 2: RC5
    public WordHistory.Profile vocabulary;
    public static final String[][] SETS = {
        {"МАР-КА", "РУЧ-КА", "КНИ-ГА", "ГА-ЛО-ШИ", "КО-ЧЕР-ГА", "КА-РАН-ДА-ШИ"},
        {"ВА-ЗА", "Я-МА", "ВА-ТА", "Я-ГО-ДА", "ГО-ЛО-ВА", "ПО-ЗО-ЛО-ТА"},
        {"ПО-ЛЕ", "ЗО-ЛА", "ПО-РА", "РА-ДУ-ГА", "ГА-ЗЕ-ТА", "ГА-ЗО-ПРО-ВОД"},
        {"ШИ-ЛО", "КИ-НО", "ШИ-НА", "МА-ШИ-НА", "КА-ЛИ-НА", "ПА-У-ТИ-НА"},
        {"ЛИ-СА", "ПИ-ЛА", "ЛУ-НА", "МА-ЛИ-НА", "ПО-ЛО-СА", "БА-ЛЕ-РИ-НА"},
        {"КО-ЗА", "ВО-ДА", "РО-ЗА", "КО-РО-ВА", "ПО-ГО-ДА", "ВО-ДО-ПА-ДЫ"}
    };
    public static final class Card implements Serializable {
        private static final long serialVersionUID=1L;
        public final String[] parts;
        public final boolean[] filled;
        Card(String word) { parts = word.split("-"); filled = new boolean[parts.length]; }
        public boolean complete() { for (boolean b : filled) if (!b) return false; return true; }
        public String word() { StringBuilder s=new StringBuilder(); for(String part:parts) s.append(part); return s.toString(); }
    }
    public static final class Player implements Serializable {
        private static final long serialVersionUID=1L;
        public final ArrayList<Card> cards = new ArrayList<>();
        public final ArrayList<String> hand = new ArrayList<>();
        public String offer, lastReceived;
        public int receivedAt=-100;
        public int replace = -1, hp = Rules.START_HP, wordsCompleted, damageDealt, roundWords, roundDamage;
        public int completed() { return wordsCompleted; }
        public int score() { return damageDealt; }
    }
    public final Player human = new Player(), bot = new Player();
    private final Random random;
    private final ArrayList<String> bag = new ArrayList<>();
    public int elapsed, totalElapsed, round=1, eventId, exchangeCount, usefulExchangeHuman;
    public int attackId, lastAttackDamage, lastAttackCard;
    public boolean lastAttackHuman;
    public String lastAttackWord="";
    public String event = "Выберите слог в руке, затем свободное место в слове.";
    public String result;
    public String endReason = "";
    public Game(long seed) {this(seed,0,null);}
    public Game(long seed,int level,WordHistory.Profile profile) {
        this.level=level;vocabulary=profile==null?new WordHistory.Profile():profile;
        random = new Random(seed);
        if(level==1){int first=random.nextInt(SETS.length);for(String w:SETS[first])human.cards.add(new Card(w));for(String w:SETS[(first+1)%SETS.length])bot.cards.add(new Card(w));}
        else {dealCards(human);dealCards(bot);}
        startHand(human); startHand(bot);
    }
    private static int count(Map<String,Integer> map,String token) { Integer value=map.get(token);return value==null?0:value; }
    private void startHand(Player p) {
        Card opening=p.cards.get(random.nextInt(3));
        Collections.addAll(p.hand,opening.parts);
        draw(p,Rules.START_HAND-p.hand.size());
    }
    private void dealCards(Player p) {
        for(int length:new int[]{4,3,3,2,2,2})p.cards.add(selectCard(p,length));
        if(level==2){Card a=p.cards.get(0),b=p.cards.get(1),c=p.cards.get(2);p.cards.remove(0);p.cards.remove(0);p.cards.remove(0);p.cards.add(b);p.cards.add(c);p.cards.add(a);}else Collections.reverse(p.cards);
    }
    private Card selectRc5Card(Player player,int length) {
        WordHistory history=player==human?vocabulary.human:vocabulary.bot;
        ArrayList<Rc5Catalog.Entry> choices=new ArrayList<>();
        ArrayList<Double> weights=new ArrayList<>();double total=0;
        for(Rc5Catalog.Entry entry:Rc5Catalog.length(length)) {
            boolean duplicate=false;HashSet<String> shared=new HashSet<>();
            for(Card card:player.cards) {
                if(card.word().equals(entry.word)) {duplicate=true;break;}
                if(card.parts.length!=length) for(String part:entry.parts)
                    if(Arrays.asList(card.parts).contains(part)) shared.add(part);
            }
            if(duplicate) continue;
            // Soft preference, never an ordered deck or exclusion of seen words.
            double weight=history.weight(entry.word)*(1+Math.min(3,shared.size())*2);
            choices.add(entry);weights.add(weight);total+=weight;
        }
        double draw=random.nextDouble()*total;int index=choices.size()-1;
        for(int i=0;i<choices.size();i++) {draw-=weights.get(i);if(draw<0){index=i;break;}}
        Rc5Catalog.Entry entry=choices.get(index);history.shown(entry.word);
        return new Card(entry.split);
    }
    private Card selectCard(Player player,int length) {
        if(level==2)return selectRc5Card(player,length);
        ArrayList<WordCatalog.Entry> choices=new ArrayList<>();
        ArrayList<Integer> weights=new ArrayList<>();int total=0;
        for(WordCatalog.Entry entry:WordCatalog.length(length)) {
            boolean duplicate=false;HashSet<String> shared=new HashSet<>();
            for(Card c:player.cards) {
                if(c.word().equals(entry.word)){duplicate=true;break;}
                if(c.parts.length!=length)for(String part:entry.parts)
                    if(Arrays.asList(c.parts).contains(part))shared.add(part);
            }
            if(duplicate)continue;
            int weight=1+2*Math.min(3,shared.size());choices.add(entry);weights.add(weight);total+=weight;
        }
        int pick=random.nextInt(total),index=choices.size()-1;
        for(int i=0;i<choices.size();i++){pick-=weights.get(i);if(pick<0){index=i;break;}}
        return new Card(choices.get(index).split);
    }
    private String token() {
        if(bag.isEmpty()) {
            // Shared hidden bag: account for filled slots and tokens already owned.
            for(Player player:new Player[]{human,bot}) {
                HashMap<String,Integer> owned=new HashMap<>();
                for(String s:player.hand) owned.put(s,count(owned,s)+1);
                if(player.offer!=null) owned.put(player.offer,count(owned,player.offer)+1);
                for(Card card:player.cards) for(int s=0;s<card.parts.length;s++) if(!card.filled[s]) {
                    String part=card.parts[s];int count=count(owned,part);
                    if(count>0) owned.put(part,count-1);else bag.add(part);
                }
            }
            if(bag.isEmpty()) for(Player player:new Player[]{human,bot}) for(Card card:player.cards) Collections.addAll(bag,card.parts);
            Collections.shuffle(bag, random);
        }
        return bag.remove(bag.size()-1);
    }
    private void draw(Player p,int count) { while(count-->0 && p.hand.size()+(p.offer==null?0:1)<HAND_LIMIT) p.hand.add(token()); }
    public boolean place(int hand,int card,int slot) {
        if(result!=null || hand<0 || hand>=human.hand.size()) return false;
        Card c=human.cards.get(card);
        if(c.filled[slot] || !c.parts[slot].equals(human.hand.get(hand))) return false;
        c.filled[slot]=true; human.hand.remove(hand);
        completeWord(human,bot,card,true);
        return true;
    }
    private Card freshCard(Player player,int index) {
        if(level!=1)return selectCard(player,player.cards.get(index).parts.length);
        Card old=player.cards.get(index);
        ArrayList<String> choices=new ArrayList<>();
        for(String[] set:SETS) for(String word:set) {
            if(word.split("-").length!=old.parts.length) continue;
            boolean duplicate=false;
            for(Card c:player.cards) if(c.word().equals(word.replace("-",""))) duplicate=true;
            if(!duplicate) choices.add(word);
        }
        return new Card(choices.get(random.nextInt(choices.size())));
    }
    private void completeWord(Player attacker,Player defender,int card,boolean isHuman) {
        Card word=attacker.cards.get(card);
        if(!word.complete()) return;
        lastAttackDamage=Math.min(defender.hp,Rules.damageForLength(word.parts.length));
        defender.hp-=lastAttackDamage;
        attacker.wordsCompleted++;attacker.damageDealt+=lastAttackDamage;
        attacker.roundWords++;attacker.roundDamage+=lastAttackDamage;
        lastAttackWord=word.word();lastAttackHuman=isHuman;lastAttackCard=card;attackId++;
        attacker.cards.set(card,freshCard(attacker,card));
        // A request on this slot belonged to the consumed word, never to its replacement.
        if(attacker.replace==card) attacker.replace=-1;
        bag.clear();
        resolveEnd();
    }
    public String attackText() {
        return (lastAttackHuman?"Вы собрали «":"Соперник собрал «")+lastAttackWord+"»: −"+lastAttackDamage+" HP "+(lastAttackHuman?"сопернику":"вам")+". Новое слово на столе!";
    }
    private void resolveEnd() {
        if(human.hp==0 || bot.hp==0) {
            result=(bot.hp==0?"Победа":"Поражение")+" в "+round+" раунде!";
            endReason=human.hp==0?"Ваши HP закончились":"HP соперника закончились";
        } else if(elapsed>=LIMIT) {
            result="Результат "+round+" раунда!";
            endReason="Время раунда вышло. Прогресс сохранён.";
        }
    }
    public boolean canContinue() {return result!=null && elapsed>=LIMIT && human.hp>0 && bot.hp>0;}
    public int drawInterval() {return level==1?10:Rules.drawSecondsForRound(round);}
    public int secondsToDraw() {int interval=drawInterval();return interval-elapsed%interval;}
    public boolean continueRound() {
        if(!canContinue()) return false;
        round++;elapsed=0;result=null;endReason="";
        human.roundWords=0;human.roundDamage=0;bot.roundWords=0;bot.roundDamage=0;
        return true;
    }
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException,ClassNotFoundException {
        in.defaultReadObject();
        if(round<1) round=1;
        if(totalElapsed<elapsed) totalElapsed=elapsed;
        if(round==1) {
            if(human.roundDamage==0) human.roundDamage=human.damageDealt;
            if(bot.roundDamage==0) bot.roundDamage=bot.damageDealt;
            if(human.roundWords==0) human.roundWords=human.wordsCompleted;
            if(bot.roundWords==0) bot.roundWords=bot.wordsCompleted;
        }
        if(vocabulary==null)vocabulary=new WordHistory.Profile();
        if(level==0)migrateSingleLetters();
        if(result!=null) resolveEnd();
    }
    private void migrateSingleLetters() {
        boolean changed=false;
        for(Player p:new Player[]{human,bot}) {
            changed|=removeSingleLetters(p.hand);
            if(p.offer!=null && p.offer.length()<2){p.offer=null;changed=true;}
            if(p.lastReceived!=null && p.lastReceived.length()<2)p.lastReceived=null;
            for(int index=0;index<p.cards.size();index++) {
                Card old=p.cards.get(index);boolean invalid=false;
                for(String part:old.parts)if(part.length()<2)invalid=true;
                if(!invalid)continue;
                for(int s=0;s<old.parts.length;s++)if(old.filled[s] && old.parts[s].length()>=2 && p.hand.size()+(p.offer==null?0:1)<HAND_LIMIT)p.hand.add(old.parts[s]);
                p.cards.set(index,freshCard(p,index));if(p.replace==index)p.replace=-1;changed=true;
            }
        }
        changed|=removeSingleLetters(bag);
        if(changed){bag.clear();dictionaryUpdatedOnLoad=true;}
    }
    private static boolean removeSingleLetters(ArrayList<String> tokens) {
        boolean changed=false;Iterator<String> iterator=tokens.iterator();
        while(iterator.hasNext())if(iterator.next().length()<2){iterator.remove();changed=true;}
        return changed;
    }
    public void take(int card,int slot) {
        if(result!=null || human.hand.size()+(human.offer==null?0:1)>=HAND_LIMIT) return;
        Card c=human.cards.get(card);
        if(c.filled[slot]) { c.filled[slot]=false; human.hand.add(c.parts[slot]); }
    }
    public void offer(int index) {
        if(result!=null) return;
        if(human.offer!=null) { if(human.hand.size()<HAND_LIMIT) { human.hand.add(human.offer); human.offer=null; } return; }
        if(index>=0 && index<human.hand.size()) human.offer=human.hand.remove(index);
        exchange();
    }
        private boolean needs(Player player,String token) {
        int missing=0;for(Card c:player.cards) for(int s=0;s<c.parts.length;s++) if(!c.filled[s] && c.parts[s].equals(token)) missing++;
        return missing>Collections.frequency(player.hand,token);
    }
    private void exchange() {
        if(human.offer==null || bot.offer==null) return;
        exchangeCount++;if(needs(human,bot.offer)) usefulExchangeHuman++;
        human.lastReceived=bot.offer;bot.lastReceived=human.offer;
        human.receivedAt=totalElapsed;bot.receivedAt=totalElapsed;
        human.hand.add(bot.offer); bot.hand.add(human.offer);
        human.offer=null; bot.offer=null;
        event="Скрытый обмен состоялся."; eventId++;
    }
    public void discard(int index) { if(result==null && index>=0 && index<human.hand.size()) human.hand.remove(index); }
    public void requestCard(int card) {
        if(result!=null) return;
        human.replace=human.replace==card ? -1 : card;
        replaceCards();
    }
    private void replaceCards() {
        if(human.replace<0 || bot.replace<0) return;
        for(Player p:new Player[]{human,bot}) {
            Card old=p.cards.get(p.replace);
            for(int s=0;s<old.parts.length;s++) if(old.filled[s] && p.hand.size()+(p.offer==null?0:1)<HAND_LIMIT) p.hand.add(old.parts[s]);
            p.cards.set(p.replace,freshCard(p,p.replace)); p.replace=-1;
        }
        bag.clear(); eventId++; event="Заменено по одному слову той же длины. Слоги возвращены в руку, насколько хватило места.";
    }
    private void botAction() {
        // Bot consults only its own cards and hand. Prefer completing a nearly finished word.
        int bestCard=-1,bestSlot=-1,bestHand=-1,bestValue=Integer.MIN_VALUE;
        for(int h=0;h<bot.hand.size();h++) for(int c=0;c<bot.cards.size();c++) {
            Card card=bot.cards.get(c); int filled=0,missing=0,found=0;
            HashMap<String,Integer> available=new HashMap<>();
            for(String token:bot.hand) available.put(token,count(available,token)+1);
            for(int s=0;s<card.parts.length;s++) if(card.filled[s]) filled++;else {
                missing++;int count=count(available,card.parts[s]);
                if(count>0){found++;available.put(card.parts[s],count-1);}
            }
            for(int s=0;s<card.parts.length;s++) if(!card.filled[s] && card.parts[s].equals(bot.hand.get(h))) {
                int value=(found==missing?100:0)+filled*8+found*5-missing*3;
                if(value>bestValue) { bestValue=value;bestCard=c;bestSlot=s;bestHand=h; }
            }
        }
        if(bestHand>=0) {
            bot.cards.get(bestCard).filled[bestSlot]=true;bot.hand.remove(bestHand);
            completeWord(bot,human,bestCard,false);
            if(result!=null) return;
        }
        if(bot.offer==null && !bot.hand.isEmpty()) {
            int choice=-1;
            for(int h=0;h<bot.hand.size();h++) {
                if(bot.hand.get(h).equals(bot.lastReceived) && totalElapsed-bot.receivedAt<Rules.EXCHANGE_HOLD_SECONDS) continue;
                String token=bot.hand.get(h);int demand=0;
                for(Card c:bot.cards) for(int s=0;s<c.parts.length;s++) if(!c.filled[s] && c.parts[s].equals(token)) demand++;
                if(Collections.frequency(bot.hand,token)>demand) { choice=h;break; }
            }
            if(choice>=0) bot.offer=bot.hand.remove(choice);
        }
        exchange();
        if(elapsed%Rules.DEALER_SECONDS==0 && bot.replace<0) {
            for(int c=bot.cards.size()-1;c>=0;c--) {
                boolean empty=true; for(boolean b:bot.cards.get(c).filled) if(b) empty=false;
                if(empty) {bot.replace=c;break;}
            }
        }
        replaceCards();
    }
    public void tick() {
        if(result!=null) return;
        elapsed++;totalElapsed++;
        if(elapsed%Rules.BOT_SECONDS==0) botAction();
        if(result!=null) return;
        if(elapsed%drawInterval()==0) {draw(human,Rules.DRAW_COUNT);draw(bot,Rules.DRAW_COUNT);}
        resolveEnd();
    }
}
