import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;import ru.serg.slogi.*;
/** Golden frames verified against archived RC3/RC5, also consumed by web tests. */
public class LevelCheck {
 static String frame(Game g){StringBuilder b=new StringBuilder();for(Game.Player p:new Game.Player[]{g.human,g.bot}){b.append(p.hp).append(p.hand).append(p.offer).append(p.replace);for(Game.Card c:p.cards)b.append(Arrays.toString(c.parts)).append(Arrays.toString(c.filled));}return b.toString();}
 public static void main(String[] args)throws Exception{
  for(String line:Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8)){if(line.trim().isEmpty())continue;String[] a=line.trim().split(" ");int level=Integer.parseInt(a[0]),seed=Integer.parseInt(a[1]);Game g=new Game(seed,level,null);MessageDigest hash=MessageDigest.getInstance("SHA-256");
   for(int t=0;t<450;t++){hash.update(frame(g).getBytes(StandardCharsets.UTF_8));boolean placed=false;for(int h=0;h<g.human.hand.size()&&!placed;h++)for(int c=0;c<6&&!placed;c++)for(int s=0;s<g.human.cards.get(c).parts.length&&!placed;s++)if(!g.human.cards.get(c).filled[s]&&g.human.cards.get(c).parts[s].equals(g.human.hand.get(h)))placed=g.place(h,c,s);if(t%7==0&&!g.human.hand.isEmpty())g.offer(g.human.hand.size()-1);if(t%17==0)g.requestCard(5);g.tick();if(g.canContinue())g.continueRound();}
   StringBuilder hex=new StringBuilder();for(byte b:hash.digest())hex.append(String.format("%02x",b));if(!hex.toString().equals(a[2]))throw new AssertionError("Level "+level+" seed "+seed);
  }
  System.out.println("Level RC3/RC5 golden frames passed: 60 duels x 450 states.");
 }
}
