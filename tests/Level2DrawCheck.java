import ru.serg.slogi.*;import java.io.*;
public class Level2DrawCheck {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static Game at(int level,int elapsed){Game g=new Game(7,level,null);g.elapsed=elapsed;g.totalElapsed=elapsed;return g;}
 static Game reload(Game g)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();try(ObjectOutputStream o=new ObjectOutputStream(b)){o.writeObject(g);}try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(b.toByteArray()))){return (Game)in.readObject();}}
 public static void main(String[] args)throws Exception{
  Game g=at(2,4);g.human.hand.clear();int botCount=g.bot.hand.size();check(g.secondsToDraw()==1,"Empty hand shows 1 second");g.tick();check(g.human.hand.size()==1,"One tile after one tick");check(g.human.nextDrawAt==15&&g.secondsToDraw()==10,"Normal interval resets after emergency tile");check(g.bot.hand.size()==botCount&&g.bot.nextDrawAt==10,"Opponent clock unchanged");
  g=at(2,4);g.human.hand.clear();g.bot.hand.clear();g.tick();check(g.human.hand.size()==1&&g.bot.hand.size()==1,"Both empty receive one tile");
  g=at(2,9);g.human.hand.clear();g.tick();check(g.human.hand.size()==1,"Regular and emergency draw coincide once");
  g=at(1,4);g.human.hand.clear();check(g.secondsToDraw()==6,"RC3 unchanged timer");g.tick();check(g.human.hand.isEmpty(),"RC3 has no boost");
  g=at(2,0);g.bot.hand.clear();Game.Card c=g.bot.cards.get(0);c.filled[0]=true;g.bot.hand.add(c.parts[1]);g.tick();g.tick();check(g.bot.hand.isEmpty(),"Opponent used last tile");check(g.bot.nextDrawAt==3,"Bot waits until following tick");g.tick();check(g.bot.hand.size()==1,"Bot receives next tick, not immediately");
  g=at(2,4);g.human.hand.clear();g=reload(g);g.tick();check(g.human.hand.size()==1,"Save empty hand retains boost");g=reload(g);check(g.secondsToDraw()==10,"Save reset timer retains remaining interval");
  g=at(2,11);g.human.nextDrawAt=0;g.bot.nextDrawAt=0;g=reload(g);check(g.human.nextDrawAt==20,"Old save migrates to old scheduled draw");
  g=at(2,89);g.human.hand.clear();g.tick();check(g.canContinue(),"Round result");int hand=g.human.hand.size();g.continueRound();check(g.human.hand.size()==hand&&g.secondsToDraw()==9,"Next round uses new interval without new starting hand");
  g=at(2,4);g.human.hand.clear();g.result="Победа";g.tick();check(g.human.hand.isEmpty(),"No draw after terminal result");
  System.out.println("Level2 empty hand passed: one second, independent clocks, simultaneous boundary, bot delay, saves, old saves, round continuation; RC3 unchanged.");
 }
}
