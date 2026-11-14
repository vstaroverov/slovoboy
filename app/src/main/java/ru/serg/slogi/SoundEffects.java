package ru.serg.slogi;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;
import java.util.HashSet;

/** Short local PCM effects. No loops, delayed replay, audio focus or network. */
final class SoundEffects {
    private final SharedPreferences preferences;
    private final SoundPool pool;
    private final int[] samples=new int[GameAudio.Cue.values().length],streams=new int[8];
    private final HashSet<Integer> loaded=new HashSet<>();
    private boolean enabled,active,closed;
    private int nextStream,volume;
    SoundEffects(Context context) {
        preferences=context.getSharedPreferences("sound-settings",Context.MODE_PRIVATE);
        enabled=preferences.getBoolean("enabled",true);
        volume=Math.max(0,Math.min(100,preferences.getInt("volume",100)));
        pool=new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        pool.setOnLoadCompleteListener((soundPool,id,status)->{if(!closed && status==0)loaded.add(id);});
        load(context,GameAudio.Cue.SELECT,R.raw.snd_select);
        load(context,GameAudio.Cue.PLACE,R.raw.snd_place);
        load(context,GameAudio.Cue.REMOVE,R.raw.snd_remove);
        load(context,GameAudio.Cue.ERROR,R.raw.snd_error);
        load(context,GameAudio.Cue.DRAW,R.raw.snd_draw);
        load(context,GameAudio.Cue.EXCHANGE,R.raw.snd_exchange);
        load(context,GameAudio.Cue.DEALER,R.raw.snd_dealer);
        load(context,GameAudio.Cue.START,R.raw.snd_start);
        load(context,GameAudio.Cue.HIT,R.raw.snd_hit);
        load(context,GameAudio.Cue.BOT_HIT,R.raw.snd_bot_hit);
        load(context,GameAudio.Cue.ROUND,R.raw.snd_round);
        load(context,GameAudio.Cue.WIN,R.raw.snd_win);
        load(context,GameAudio.Cue.LOSE,R.raw.snd_lose);
    }
    private void load(Context context,GameAudio.Cue cue,int resource) {samples[cue.ordinal()]=pool.load(context,resource,1);}
    int volume(){return volume;}
    void setVolume(int value){volume=Math.max(0,Math.min(100,value));preferences.edit().putInt("volume",volume).apply();if(!closed)for(int stream:streams)if(stream!=0)pool.setVolume(stream,volume/100f,volume/100f);}
    boolean enabled() {return enabled;}
    void setEnabled(boolean value) {
        enabled=value;preferences.edit().putBoolean("enabled",value).apply();
        if(!value)stop();
    }
    void setActive(boolean value) {active=value;if(!value)stop();}
    void play(GameAudio.Cue cue) {
        if(cue==null || closed || !active || !enabled)return;
        int sample=samples[cue.ordinal()];if(!loaded.contains(sample))return;
        int slot=nextStream++%streams.length;
        if(streams[slot]!=0)pool.stop(streams[slot]);
        int priority=cue==GameAudio.Cue.SELECT?1:cue==GameAudio.Cue.WIN || cue==GameAudio.Cue.LOSE?3:2;
        streams[slot]=pool.play(sample,volume/100f,volume/100f,priority,0,1f);
    }
    private void stop() {if(!closed)for(int i=0;i<streams.length;i++){if(streams[i]!=0)pool.stop(streams[i]);streams[i]=0;}}
    void close() {if(!closed){stop();closed=true;pool.release();loaded.clear();}}
}
