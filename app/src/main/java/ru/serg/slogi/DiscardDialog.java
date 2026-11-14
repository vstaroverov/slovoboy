package ru.serg.slogi;

import android.app.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Confirmation styled like the game table, with native accessible controls. */
final class DiscardDialog {
    private static final int INK=0xff29243f,MUTED=0xff79738e,PURPLE=0xff7051df,PALE=0xffece6ff;
    private static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    private static GradientDrawable shape(Activity a,int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(a,radius));return g;}
    private static TextView text(Activity a,String value,int size,int color,boolean bold){
        TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));t.setGravity(Gravity.CENTER);return t;
    }
    private static Button button(Activity a,String value,int color) {
        Button b=new Button(a);b.setText(value);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(color);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setGravity(Gravity.CENTER);
        b.setPadding(0,0,0,0);b.setStateListAnimator(null);b.setElevation(0);return b;
    }
    static Dialog create(Activity a,String syllable,Runnable discard) {
        Dialog dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(a,24),dp(a,26),dp(a,24),dp(a,24));card.setBackground(shape(a,Color.WHITE,26));
        card.addView(text(a,"Сбросить фишку?",23,INK,true),new LinearLayout.LayoutParams(-1,-2));
        TextView chip=text(a,syllable,syllable.length()>3?26:32,PURPLE,true);chip.setBackground(shape(a,PALE,16));chip.setContentDescription("Фишка «"+syllable+"»");
        LinearLayout.LayoutParams tile=new LinearLayout.LayoutParams(dp(a,112),dp(a,72));tile.gravity=Gravity.CENTER;tile.topMargin=dp(a,22);tile.bottomMargin=dp(a,18);card.addView(chip,tile);
        TextView body=text(a,"Фишка исчезнет из руки.\nМожно обменять её на полезную.",15,MUTED,false);body.setLineSpacing(dp(a,3),1);card.addView(body,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams buttons=new LinearLayout.LayoutParams(-1,-2);buttons.topMargin=dp(a,24);card.addView(row,buttons);
        TextView cancel=button(a,"Оставить",PURPLE),confirm=button(a,"Сбросить",Color.WHITE);
        cancel.setBackground(shape(a,PALE,14));confirm.setBackground(shape(a,PURPLE,14));
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,dp(a,52),1),right=new LinearLayout.LayoutParams(0,dp(a,52),1);left.rightMargin=dp(a,10);row.addView(cancel,left);row.addView(confirm,right);
        for(TextView button:new TextView[]{cancel,confirm}){button.setFocusable(true);button.setClickable(true);button.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);}
        cancel.setOnClickListener(v->dialog.dismiss());confirm.setOnClickListener(v->{confirm.setEnabled(false);discard.run();dialog.dismiss();});
        dialog.setContentView(card);dialog.setCanceledOnTouchOutside(true);
        Window w=dialog.getWindow();w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attrs=w.getAttributes();attrs.width=Math.min(dp(a,400),a.getResources().getDisplayMetrics().widthPixels-dp(a,40));attrs.height=WindowManager.LayoutParams.WRAP_CONTENT;attrs.gravity=Gravity.CENTER;attrs.dimAmount=.42f;w.setAttributes(attrs);
        return dialog;
    }
}
