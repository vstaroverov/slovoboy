package ru.serg.slogi;

import android.app.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Native controls in the same palette as the canvas table. */
final class GameDialog {
    private static int dp(Activity a,int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
    private static GradientDrawable bg(Activity a,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(a,radius));return d;}
    static Dialog create(Activity a,String badge,String title,String body,String[] labels,Runnable[] actions){
        Dialog d=new Dialog(a);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(a,24),dp(a,24),dp(a,24),dp(a,24));content.setBackground(bg(a,0xffffffff,26));
        for(int i=0;i<3;i++){
            TextView t=new TextView(a);t.setText(i==0?badge:i==1?title:body);t.setTextSize(i==0?32:i==1?26:15);t.setTextColor(i==0?0xff7051df:i==1?0xff29243f:0xff79738e);t.setGravity(Gravity.CENTER);t.setTypeface(Typeface.create(i<2?"sans-serif-medium":"sans-serif",Typeface.NORMAL));t.setLineSpacing(dp(a,4),1);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(a,i==2?20:12);content.addView(t,lp);
        }
        for(int i=0;i<labels.length;i++){
            final int index=i;Button b=new Button(a);b.setText(labels[i]);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(i==0?0xffffffff:0xff7051df);b.setBackground(bg(a,i==0?0xff7051df:0xffece6ff,14));b.setStateListAnimator(null);b.setElevation(0);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(a,52));lp.topMargin=dp(a,10);content.addView(b,lp);b.setOnClickListener(v->{d.dismiss();actions[index].run();});
        }
        d.setContentView(content);Window w=d.getWindow();w.setBackgroundDrawable(new ColorDrawable(0));w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams attrs=w.getAttributes();attrs.width=Math.min(dp(a,400),a.getResources().getDisplayMetrics().widthPixels-dp(a,40));attrs.height=-2;attrs.gravity=Gravity.CENTER;attrs.dimAmount=.45f;w.setAttributes(attrs);return d;
    }
}

