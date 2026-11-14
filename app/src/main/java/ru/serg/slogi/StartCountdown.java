package ru.serg.slogi;

import java.io.Serializable;

/** Foreground seconds only; gameplay has not started while this counts down. */
public final class StartCountdown implements Serializable {
    private static final long serialVersionUID=1L;
    private int remaining=Rules.START_COUNTDOWN_SECONDS;
    public int remaining() {return remaining;}
    public boolean tick() {if(remaining>0)remaining--;return remaining==0;}
    public String description() {
        return "Подбираем соперника... Старт через "+remaining+" "+(remaining==1?"секунду":remaining<5?"секунды":"секунд")+".";
    }
}
