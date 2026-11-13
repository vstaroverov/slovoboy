import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Original, deterministic synthesis; no external samples or dependencies. */
public class MakeSounds {
    static final int RATE=22050;
    static double[] wave;
    static void note(double at,double duration,double hz,double gain) {
        for(int i=(int)(at*RATE);i<Math.min(wave.length,(int)((at+duration)*RATE));i++) {
            double t=i/(double)RATE-at;
            double env=Math.min(1,t/0.006)*Math.min(1,(duration-t)/0.018)*Math.exp(-4*t/duration);
            wave[i]+=gain*env*(Math.sin(2*Math.PI*hz*t)+0.22*Math.sin(4*Math.PI*hz*t));
        }
    }
    static void thump(double at,double duration,double gain) {
        Random noise=new Random(127);
        for(int i=(int)(at*RATE);i<Math.min(wave.length,(int)((at+duration)*RATE));i++) {
            double t=i/(double)RATE-at;
            double env=Math.min(1,t/0.005)*Math.min(1,(duration-t)/0.02)*Math.exp(-7*t/duration);
            double phase=2*Math.PI*(70*t+5*(1-Math.exp(-35*t)));
            wave[i]+=gain*env*(Math.sin(phase)+0.08*(noise.nextDouble()*2-1));
        }
    }
    static void le(DataOutputStream out,int n,int bytes) throws IOException {for(int i=0;i<bytes;i++)out.write(n>>>(8*i));}
    static void write(Path directory,String name,double duration) throws IOException {
        wave=new double[(int)(duration*RATE)];
        switch(name) {
            case "select":note(0,.06,880,.13);break;
            case "place":note(0,.12,659.25,.28);note(.025,.10,987.77,.12);break;
            case "remove":note(0,.13,523.25,.21);note(.04,.10,392,.12);break;
            case "error":note(0,.10,220,.22);note(.09,.12,185,.20);break;
            case "draw":note(0,.12,783.99,.20);note(.065,.15,1046.5,.20);break;
            case "exchange":note(0,.15,523.25,.22);note(.065,.16,783.99,.23);note(.13,.15,659.25,.20);break;
            case "dealer":for(int i=0;i<3;i++){thump(i*.055,.10,.12);note(i*.055,.12,587.33+i*90,.14);}break;
            case "start":note(0,.18,523.25,.25);note(.09,.18,659.25,.25);note(.18,.22,783.99,.27);break;
            case "hit":thump(0,.20,.42);note(.025,.24,659.25,.27);note(.08,.22,987.77,.19);break;
            case "bot_hit":thump(0,.22,.34);note(.015,.20,293.66,.21);break;
            case "round":note(0,.27,587.33,.24);note(.16,.28,783.99,.23);break;
            case "win":for(int i=0;i<4;i++)note(i*.11,.28,new double[]{523.25,659.25,783.99,1046.5}[i],.28);break;
            case "lose":note(0,.26,392,.25);note(.14,.25,329.63,.24);note(.28,.27,261.63,.23);break;
            default:throw new IllegalArgumentException(name);
        }
        for(int i=0;i<wave.length;i++)wave[i]*=2; // Short effects need headroom without becoming inaudible at normal media volume.
        double peak=0,energy=0;for(double v:wave)peak=Math.max(peak,Math.abs(v));
        if(peak>=.9)throw new AssertionError("Clipping risk: "+name);
        for(double v:wave)energy+=v*v;
        if(peak<.01 || wave[0]!=0 || Math.abs(wave[wave.length-1])>.001)throw new AssertionError("Bad envelope: "+name);
        Path file=directory.resolve("snd_"+name+".wav");
        try(DataOutputStream out=new DataOutputStream(Files.newOutputStream(file))) {
            out.writeBytes("RIFF");le(out,36+wave.length*2,4);out.writeBytes("WAVEfmt ");le(out,16,4);
            le(out,1,2);le(out,1,2);le(out,RATE,4);le(out,RATE*2,4);le(out,2,2);le(out,16,2);
            out.writeBytes("data");le(out,wave.length*2,4);
            for(double v:wave)le(out,(int)Math.round(v*32767),2);
        }
        System.out.printf(Locale.ROOT,"%s,%.3f,%.4f,%.4f,%d%n",file.getFileName(),duration,peak,Math.sqrt(energy/wave.length),Files.size(file));
    }
    public static void main(String[] args) throws Exception {
        Path directory=Paths.get(args[0]);Files.createDirectories(directory);
        String[] names={"select","place","remove","error","draw","exchange","dealer","start","hit","bot_hit","round","win","lose"};
        double[] durations={.09,.16,.17,.24,.25,.32,.30,.44,.35,.27,.48,.66,.60};
        System.out.println("asset,seconds,peak,rms,bytes");
        for(int i=0;i<names.length;i++)write(directory,names[i],durations[i]);
    }
}
