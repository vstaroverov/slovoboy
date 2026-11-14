package ru.serg.slogi;
import android.app.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;

/** Accessible native switch followed by the requested volume slider. */
final class SoundSettingsDialog {
    private static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    static Dialog create(MainActivity a){
        Dialog d=new Dialog(a);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(a,24),dp(a,26),dp(a,24),dp(a,24));GradientDrawable bg=new GradientDrawable();bg.setColor(0xffffffff);bg.setCornerRadius(dp(a,26));card.setBackground(bg);
        TextView title=new TextView(a);title.setText("Звук");title.setTextSize(26);title.setTextColor(0xff29243f);title.setGravity(Gravity.CENTER);card.addView(title,new LinearLayout.LayoutParams(-1,-2));
        Switch toggle=new Switch(a);toggle.setText("Включить звук");toggle.setTextSize(17);toggle.setTextColor(0xff29243f);toggle.setChecked(a.soundEnabled());LinearLayout.LayoutParams row=new LinearLayout.LayoutParams(-1,dp(a,58));row.topMargin=dp(a,18);card.addView(toggle,row);toggle.setOnCheckedChangeListener((v,checked)->{if(checked!=a.soundEnabled())a.toggleSound();});
        TextView value=new TextView(a);value.setText("Громкость · "+a.soundVolume()+"%");value.setTextSize(15);value.setTextColor(0xff79738e);card.addView(value,new LinearLayout.LayoutParams(-1,-2));
        SeekBar slider=new SeekBar(a);slider.setMax(100);slider.setProgress(a.soundVolume());slider.setContentDescription("Громкость звука");slider.setProgressTintList(ColorStateList.valueOf(0xff7051df));slider.setThumbTintList(ColorStateList.valueOf(0xff7051df));card.addView(slider,new LinearLayout.LayoutParams(-1,dp(a,56)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int n,boolean user){value.setText("Громкость · "+n+"%");if(user)a.setSoundVolume(n);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){a.previewSound();}});
        Button close=new Button(a);close.setText("Готово");close.setAllCaps(false);close.setTextColor(0xffffffff);close.setTextSize(16);GradientDrawable button=new GradientDrawable();button.setColor(0xff7051df);button.setCornerRadius(dp(a,14));close.setBackground(button);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(a,52));lp.topMargin=dp(a,16);card.addView(close,lp);close.setOnClickListener(v->d.dismiss());
        d.setContentView(card);Window w=d.getWindow();w.setBackgroundDrawable(new ColorDrawable(0));w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams attrs=w.getAttributes();attrs.width=Math.min(dp(a,400),a.getResources().getDisplayMetrics().widthPixels-dp(a,40));attrs.height=-2;attrs.gravity=Gravity.CENTER;attrs.dimAmount=.45f;w.setAttributes(attrs);return d;
    }
}
