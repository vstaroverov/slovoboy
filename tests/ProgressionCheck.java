import ru.serg.slogi.*;
public class ProgressionCheck {
 public static void main(String[] args){Progression p=new Progression(0,"");int level=0;for(int wins=1;wins<=5050;wins++){String id="duel-"+wins;if(!p.reward(id)||p.reward(id))throw new AssertionError("Duplicate reward");while((level+1)*(level+2)/2<=wins)level++;if(p.level()!=level||p.xpNeeded()!=(level+1)*100||p.totalXp()!=wins*100L)throw new AssertionError("Progression "+wins);Progression reloaded=new Progression(p.victories,p.lastVictory);if(reloaded.reward(id)||reloaded.level()!=level)throw new AssertionError("Reload reward");}if(p.level()!=100)throw new AssertionError("Level 100");System.out.println("Progression passed: thresholds 1,3,6,10,...5050 wins; 100 XP/win; no duplicate on replay/reload.");}
}
