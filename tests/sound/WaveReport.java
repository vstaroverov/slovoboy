import javax.sound.sampled.*;
import java.io.*;
import java.util.*;

/** Signal-level QA for recordings of Android playback, never microphone input. */
public class WaveReport {
    public static void main(String[] args) throws Exception {
        try(AudioInputStream input=AudioSystem.getAudioInputStream(new File(args[0]))) {
            AudioFormat original=input.getFormat();
            AudioFormat pcm=new AudioFormat(original.getSampleRate(),16,original.getChannels(),true,false);
            try(AudioInputStream stream=AudioSystem.getAudioInputStream(pcm,input)) {
                byte[] bytes=stream.readAllBytes();double energy=0;int peak=0;long nonzero=0;
                for(int i=0;i+1<bytes.length;i+=2) {
                    int sample=(short)((bytes[i]&255)|(bytes[i+1]<<8));int abs=Math.abs(sample);
                    peak=Math.max(peak,abs);energy+=(double)sample*sample;if(abs>2)nonzero++;
                }
                double rms=Math.sqrt(energy/(bytes.length/2.0))/32768;
                System.out.printf(Locale.ROOT,"%s seconds=%.3f channels=%d rate=%.0f peak=%.6f rms=%.8f nonzeroSamples=%d%n",new File(args[0]).getName(),bytes.length/(double)pcm.getFrameSize()/pcm.getFrameRate(),pcm.getChannels(),pcm.getSampleRate(),peak/32768.0,rms,nonzero);
                if(args.length>1 && args[1].equals("silent") && rms>.0001)throw new AssertionError("Muted game produced audio");
                if(args.length>1 && args[1].equals("audible") && (rms<.0005 || nonzero<1000))throw new AssertionError("No game audio captured");
            }
        }
    }
}
