package ru.serg.slogi;

/** One foreground cue per transition, with results taking priority over hits. */
public final class GameAudio {
    private GameAudio() {}
    public enum Cue {SELECT, PLACE, REMOVE, ERROR, DRAW, EXCHANGE, DEALER, START, HIT, BOT_HIT, ROUND, WIN, LOSE}
    public static final class Before {
        final int attack,exchange,event,hand;
        final boolean ended;
        public Before(Game game) {
            attack=game.attackId;exchange=game.exchangeCount;event=game.eventId;
            hand=game.human.hand.size();ended=game.result!=null;
        }
    }
    public static Cue changed(Before before,Game game) {
        if(!before.ended && game.result!=null) {
            if(game.bot.hp==0)return Cue.WIN;
            if(game.human.hp==0)return Cue.LOSE;
            return Cue.ROUND;
        }
        if(before.attack!=game.attackId)return game.lastAttackHuman?Cue.HIT:Cue.BOT_HIT;
        if(before.exchange!=game.exchangeCount)return Cue.EXCHANGE;
        if(before.event!=game.eventId)return Cue.DEALER;
        if(before.hand<game.human.hand.size())return Cue.DRAW;
        return null;
    }
}
