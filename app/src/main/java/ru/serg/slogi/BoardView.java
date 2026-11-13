package ru.serg.slogi;

import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import java.util.*;

/** A stable table: clock updates never replace touch targets. */
@android.annotation.SuppressLint("ViewConstructor") // Created only by MainActivity, never inflated from XML.
final class BoardView extends View {
    private final MainActivity a;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Hit> hits=new ArrayList<>();
    private final ArrayList<Hit> hitPool=new ArrayList<>();
    private final Typeface regular=Typeface.create("sans-serif",Typeface.NORMAL),bold=Typeface.create("sans-serif-medium",Typeface.NORMAL);
    private float scale=1,ox,oy;
    private Canvas canvas;
    private Hit pressed;
    private long downAt,flashUntil,attackUntil;
    private int flashCard=-1, errorCard=-1,errorSlot=-1;
    private long errorUntil;
    private static final int BG=0xfff4f2fa,INK=0xff29243f,MUTED=0xff79738e,PURPLE=0xff7051df,PALE=0xffece6ff,GREEN=0xff23896a,MINT=0xffe0f4eb,WHITE=0xffffffff,LINE=0xffe2deed,ROSE=0xffe07685;
    private static final float W=432,H=880;
    private static final class Hit {
        RectF box;Runnable tap,hold;String label;
        Hit(RectF r,Runnable t,Runnable h){box=r;tap=t;hold=h;}
    }
    BoardView(MainActivity activity) {super(activity);a=activity;setFocusable(true);setContentDescription("Слоги. Игровой стол: слова сверху, фишки внизу, кнопка обмена под фишками.");}
    void clearFeedback() {attackUntil=0;flashUntil=0;errorUntil=0;flashCard=-1;errorCard=-1;errorSlot=-1;}
    void reject(int card,int slot) {errorCard=card;errorSlot=slot;errorUntil=SystemClock.uptimeMillis()+1100;invalidate();postInvalidateDelayed(1150);}
    void showAttack() { attackUntil=SystemClock.uptimeMillis()+2000; celebrate(a.game.lastAttackHuman?a.game.lastAttackCard:-1); invalidate();postInvalidateDelayed(2050); }
    void celebrate(int card) {flashCard=card;flashUntil=SystemClock.uptimeMillis()+650;postInvalidateDelayed(660);}
    private void box(float x,float y,float w,float h,int color,float radius) {p.setColor(color);p.setStyle(Paint.Style.FILL);canvas.drawRoundRect(x,y,x+w,y+h,radius,radius,p);}
    private void outline(float x,float y,float w,float h,int color) {p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);canvas.drawRoundRect(x,y,x+w,y+h,12,12,p);p.setStyle(Paint.Style.FILL);}
        private float readableSize(float size) {return Math.max(size,(size>=16?14:10)*getResources().getDisplayMetrics().density/Math.max(0.1f,scale));}
    private void rawText(String s,float x,float y,float size,int color,boolean strong) {p.setColor(color);p.setTypeface(strong?bold:regular);p.setTextSize(size);canvas.drawText(s,x,y,p);}
    private void text(String s,float x,float y,float size,int color,boolean strong) {rawText(s,x,y,readableSize(size),color,strong);}
        private void center(String s,float x,float y,float size,int color,boolean strong) {
        p.setTypeface(strong?bold:regular);size=readableSize(size);p.setTextSize(size);
        if(p.measureText(s)>350){size*=350/p.measureText(s);p.setTextSize(size);}
        rawText(s,x-p.measureText(s)/2,y,size,color,strong);
    }
    private void fit(String s,float x,float y,float maxWidth,float size,int color,boolean strong) {p.setTypeface(strong?bold:regular);p.setTextSize(size);float actual=Math.min(size,size*maxWidth/Math.max(1,p.measureText(s)));rawText(s,x,y,actual,color,strong);}
    private void hit(float x,float y,float w,float h,Runnable action) {hit(x,y,w,h,action,null);}
    private void hit(float x,float y,float w,float h,Runnable action,Runnable hold) {
        int index=hits.size();
        if(index==hitPool.size()) hitPool.add(new Hit(new RectF(),null,null));
        Hit hit=hitPool.get(index);hit.box.set(x,y,x+w,y+h);float target=44*getResources().getDisplayMetrics().density/Math.max(0.1f,scale);hit.box.inset(-Math.max(0,target-w)/2,-Math.max(0,target-h)/2);hit.tap=action;hit.hold=hold;hit.label="Игровое действие";hits.add(hit);
    }
    private void label(String text) {if(!hits.isEmpty()) hits.get(hits.size()-1).label=text;}
    private void button(String s,float x,float y,float w,float h,boolean active,Runnable action) {box(x,y,w,h,active?PURPLE:PALE,14);center(s,x+w/2,y+h/2+6,16,active?WHITE:PURPLE,true);hit(x,y,w,h,action);label(s);}
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);canvas=c;hits.clear();c.drawColor(BG);
        float width=getWidth()-getPaddingLeft()-getPaddingRight(),height=getHeight()-getPaddingTop()-getPaddingBottom();
        scale=Math.min(width/W,height/H);ox=getPaddingLeft()+(width-W*scale)/2;oy=getPaddingTop()+(height-H*scale)/2;
        c.save();c.translate(ox,oy);c.scale(scale,scale);
        Game g=a.game;String chosen=a.chosen();
        text("словобой",20,39,24,INK,true);text("РАУНД "+g.round,156,37,11,PURPLE,true);
        int remaining=Game.LIMIT-g.elapsed;
        box(270,13,89,36,WHITE,18);center(String.format(Locale.ROOT,"%02d:%02d",remaining/60,remaining%60),314,37,16,INK,true);
        box(370,13,42,36,WHITE,14);center("Ⅱ",391,37,18,PURPLE,true);hit(366,9,48,44,a::pause);label("Пауза");
        health(20,65,"ВЫ",g.human.hp,g.human.score(),PURPLE);
        health(222,65,"БОТ",g.bot.hp,g.bot.score(),ROSE);
        boolean recentAttack=SystemClock.uptimeMillis()<attackUntil;
        String attack=recentAttack?(g.lastAttackHuman?"Ваш удар! ":"Удар бота! ")+"«"+g.lastAttackWord+"» → −"+g.lastAttackDamage+" HP":"Слово целиком = мгновенный удар на 1 / 3 / 5 HP";
        center(attack,216,153,13,recentAttack?(g.lastAttackHuman?GREEN:ROSE):MUTED,recentAttack);
        text("Слова бота",22,181,12,MUTED,false);
        for(int i=0;i<6;i++) {
            Game.Card card=g.bot.cards.get(i);float x=111+i*39;
            for(int s=0;s<card.parts.length;s++) box(x+s*7,172,5,8,card.filled[s]?ROSE:LINE,2);
        }
        text("↗",392,182,18,MUTED,true);hit(20,160,392,32,a::opponent);label("Открытые карточки бота");
        box(20,200,392,65,PALE,18);
        String title=recentAttack && g.lastAttackHuman?"Удар! Уже появилось новое слово":a.hasTarget()?"Нажмите фишку для выбранного слота":chosen==null?"Выберите фишку или слот":"Теперь выберите слот в слове";
        String subtitle=recentAttack && g.lastAttackHuman?"«"+g.lastAttackWord+"» собрано: −"+g.lastAttackDamage+" HP боту. Продолжайте!":a.hasTarget()?"Выбран слот «"+a.targetPart()+"». Теперь выберите фишку.":chosen==null?Rules.GOAL:"Нажмите на слот. Проверим, совпадают ли слоги.";
        fit(title,36,226,360,17,PURPLE,true);fit(subtitle,36,249,360,13,0xff645b84,false);
        text("ВАШИ СЛОВА",22,291,12,MUTED,true);text("Ударов: "+g.human.completed(),310,291,12,PURPLE,true);
        for(int i=0;i<3;i++) card(i,20+i*134,304,124,84);
        for(int i=0;i<2;i++) card(i+3,20+i*202,400,190,84);
        card(5,20,496,392,84);
        String notice=SystemClock.uptimeMillis()<a.messageUntil?a.message:"Собрали → ударили → получили новое слово";
        fit(notice,22,605,390,12,SystemClock.uptimeMillis()<errorUntil?ROSE:SystemClock.uptimeMillis()<a.messageUntil?GREEN:MUTED,false);
        box(8,622,416,246,WHITE,24);
        text("ВАШИ ФИШКИ",24,649,12,MUTED,true);text(g.human.hand.size()+"/12",149,649,12,MUTED,false);
        text(g.human.hand.size()+(g.human.offer==null?0:1)>=Rules.HAND_LIMIT?"Рука полна":"+"+Rules.DRAW_COUNT+" через "+(Rules.DRAW_SECONDS-g.elapsed%Rules.DRAW_SECONDS)+" с",302,649,12,PURPLE,true);
        for(int i=0;i<12;i++) {
            float x=24+(i%6)*65,y=663+(i/6)*55;
            if(i<g.human.hand.size()) {
                final int index=i;String syllable=g.human.hand.get(i);boolean selected=a.selected==i;
                box(x,y+3,58,46,selected?0xff5032b0:0xffded8ed,12);box(x,y,58,46,selected?PURPLE:0xfff3effb,12);
                center(syllable,x+29,y+29,18,selected?WHITE:INK,true);
                hit(x,y,58,49,()->a.choose(index),()->a.discard(index));label("Фишка «"+syllable+"»"+(selected?", выбрана":"")+". Долгое нажатие — сбросить.");
            } else box(x,y,58,46,0xfffaf9fc,12);
        }
        text(g.human.offer==null?"СЛЕПОЙ ОБМЕН":"ПРЕДЛОЖЕНО: "+g.human.offer,25,794,11,MUTED,true);
        text(g.bot.offer==null?"Бот выбирает фишку":"Бот готов к обмену",25,815,12,g.bot.offer==null?MUTED:GREEN,false);
        String exchangeLabel=g.human.offer!=null?"Забрать обратно":chosen!=null?"Обменять «"+chosen+"»":"Выберите фишку";
        button(exchangeLabel,223,774,185,53,chosen!=null || g.human.offer!=null,a::exchange);
        text("Правила",25,852,13,PURPLE,true);hit(20,830,85,38,a::rules);label("Правила игры и сведения о приложении");
        center(a.soundEnabled()?"Звук: вкл":"Звук: выкл",151,852,12,PURPLE,true);hit(108,830,86,38,a::toggleSound);label(a.soundEnabled()?"Звук включён. Выключить звук":"Звук выключен. Включить звук");
        center(g.human.replace>=0?"Крупье ✓":"Крупье",254,852,13,PURPLE,true);hit(205,830,97,38,a::dealer);label("Крупье: замена слова по взаимной заявке");
        text("Заново ↻",338,852,13,MUTED,false);hit(323,830,90,38,a::restart);label("Новая партия");
        if(!a.started) intro();else if(g.result!=null) finish();else if(a.awaitingResume) resumeOverlay();
        if(!a.started || g.result!=null || a.awaitingResume)button(a.soundEnabled()?"Звук: вкл":"Звук: выкл",130,690,172,44,false,a::toggleSound);
        c.restore();
    }
    private void health(float x,float y,String label,int hp,int score,int accent) {
        box(x,y,190,66,WHITE,18);text(label,x+14,y+23,11,MUTED,true);
        text("♥ "+hp,x+14,y+48,22,accent,true);text("Урон "+score,x+109,y+29,14,INK,true);
        box(x+109,y+41,65,5,LINE,3);box(x+109,y+41,65*hp/Rules.START_HP,5,accent,3);
    }
    private void card(int index,float x,float y,float w,float h) {
        Game.Card card=a.game.human.cards.get(index);boolean complete=card.complete();
        box(x,y+2,w,h,0xffe6e1ef,17);box(x,y,w,h,index==flashCard && SystemClock.uptimeMillis()<flashUntil?MINT:WHITE,17);
        if(index==flashCard && SystemClock.uptimeMillis()<flashUntil) outline(x,y,w,h,0xffd7b34e);
        text(card.word(),x+11,y+22,w<130?13:15,complete?GREEN:INK,true);
        String power="−"+Rules.damageForLength(card.parts.length);
        p.setTextSize(11);p.setTypeface(bold);text(power,x+w-11-p.measureText(power),y+22,11,complete?GREEN:MUTED,true);
        int count=card.parts.length;float gap=5,tw=(w-20-gap*(count-1))/count;
        for(int s=0;s<count;s++) {
            final int si=s;float sx=x+10+s*(tw+gap);boolean filled=card.filled[s];boolean wrong=index==errorCard && s==errorSlot && SystemClock.uptimeMillis()<errorUntil;boolean target=a.isTarget(index,s);
            box(sx,y+33,tw,41,filled?GREEN:wrong?0xffffe7eb:target?PALE:0xfff5f3fa,10);if(wrong) outline(sx,y+33,tw,41,ROSE);else if(target) outline(sx,y+33,tw,41,PURPLE);
            center(card.parts[s],sx+tw/2,y+59,tw<50?16:18,filled?WHITE:wrong?ROSE:target?PURPLE:0xff9b94af,true);
            hit(sx-1,y+30,tw+2,47,()->a.slot(index,si));label("Слово «"+card.word()+"», слог «"+card.parts[s]+"», "+(filled?"установлен; нажмите без выбора фишки, чтобы снять":target?"выбранный пустой слот":"пустой слот"));
        }
    }
    private void veil() {box(0,0,W,H,0xc925203d,0);hits.clear();}
    private void intro() {
        veil();box(24,201,384,466,WHITE,28);box(170,225,92,34,PALE,17);center(Rules.DURATION+" · ДУЭЛЬ",216,247,11,PURPLE,true);
        center("Слово — это удар!",216,295,29,INK,true);center("Собрали слово? Бот сразу теряет HP",216,322,14,MUTED,false);
        step("1","Выберите фишку или слот","Начинать можно с любого из них",363);
        step("2","Нажмите на второй элемент","Совпал слог — ячейка заполнится",434);
        step("3","Ударьте и собирайте снова","Слово нанесёт 1 / 3 / 5 урона и сменится новым",505);
        center("Раунд — 1:30. Потом можно продолжить дуэль",216,573,12,MUTED,false);
        button("Понятно, начинаем →",48,594,336,51,true,()->{a.beginMatch();invalidate();});
    }
    private void step(String number,String title,String desc,float y) {
        box(46,y-19,36,36,PALE,12);center(number,64,y+5,19,PURPLE,true);
        text(title,96,y,18,INK,true);fit(desc,96,y+23,287,12,MUTED,false);
    }
        private void resumeOverlay() {
        veil();box(34,294,364,254,WHITE,28);
        center("Партия сохранена",216,344,26,INK,true);
        center("Бот и таймер ждут вас",216,379,15,MUTED,false);
        center("Ничего не пропущено — продолжайте со своего места",216,411,11,MUTED,false);
        button("Продолжить дуэль",58,453,316,56,true,()->a.resumeMatch());
    }
    private void finish() {
        Game g=a.game;boolean next=g.canContinue();
        veil();box(24,250,384,405,WHITE,28);
        p.setTypeface(bold);p.setTextSize(26);float size=Math.min(26,26*350/Math.max(1,p.measureText(g.result)));
        center(g.result,216,297,size,PURPLE,true);
        center("Здоровье: "+g.human.hp+" : "+g.bot.hp,216,337,20,INK,true);
        center("Урон за раунд: "+g.human.roundDamage+" : "+g.bot.roundDamage,216,371,16,MUTED,false);
        center("Всего собрано слов: "+g.human.completed()+" : "+g.bot.completed(),216,398,14,MUTED,false);
        center(next?"Следующий раунд — 1:30. HP и фишки остаются.":g.endReason,216,430,12,MUTED,false);
        if(next) button("Продолжить → раунд "+(g.round+1),48,451,336,50,true,()->a.nextRound());
        button("Ещё одна дуэль",48,next?512:465,336,50,!next,()->a.fresh());
        button("Выйти",48,next?573:526,336,50,false,()->a.exitGame());
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;
        if(e.getAction()==MotionEvent.ACTION_DOWN) {
            pressed=null;float nearest=Float.MAX_VALUE;for(Hit h:hits) if(h.box.contains(x,y)){float distance=(x-h.box.centerX())*(x-h.box.centerX())+(y-h.box.centerY())*(y-h.box.centerY());if(distance<nearest){nearest=distance;pressed=h;}}
            downAt=SystemClock.uptimeMillis();return true;
        }
        if(e.getAction()==MotionEvent.ACTION_UP) {
            Hit action=pressed;pressed=null;
            if(action!=null && action.box.contains(x,y)) {performClick();if(action.hold!=null && SystemClock.uptimeMillis()-downAt>550) action.hold.run();else action.tap.run();invalidate();}
            return true;
        }
        if(e.getAction()==MotionEvent.ACTION_CANCEL) pressed=null;return true;
    }
    private int accessibleFocus=-1,hovered=-1;
    private void accessibilityEvent(int id,int type) {
        if(id<0 || id>=hits.size() || getParent()==null) return;
        android.view.accessibility.AccessibilityEvent event=android.view.accessibility.AccessibilityEvent.obtain(type);
        event.setPackageName(getContext().getPackageName());event.setClassName("android.widget.Button");
        event.setSource(this,id);event.getText().add(hits.get(id).label);getParent().requestSendAccessibilityEvent(this,event);
    }
    @Override public android.view.accessibility.AccessibilityNodeProvider getAccessibilityNodeProvider() {
        return new android.view.accessibility.AccessibilityNodeProvider() {
            @Override public android.view.accessibility.AccessibilityNodeInfo createAccessibilityNodeInfo(int id) {
                if(id==HOST_VIEW_ID) {
                    android.view.accessibility.AccessibilityNodeInfo node=android.view.accessibility.AccessibilityNodeInfo.obtain(BoardView.this);
                    BoardView.this.onInitializeAccessibilityNodeInfo(node);
                    node.setContentDescription((a.game.result!=null?a.game.result+". ":"Словобой. Раунд "+a.game.round+". ")+"Ваши HP "+a.game.human.hp+", HP бота "+a.game.bot.hp+". Осталось "+(Game.LIMIT-a.game.elapsed)+" секунд.");
                    for(int i=0;i<hits.size();i++) node.addChild(BoardView.this,i);
                    return node;
                }
                if(id<0 || id>=hits.size()) return null;
                Hit hit=hits.get(id);android.view.accessibility.AccessibilityNodeInfo node=android.view.accessibility.AccessibilityNodeInfo.obtain();
                node.setSource(BoardView.this,id);node.setParent(BoardView.this);node.setPackageName(getContext().getPackageName());
                node.setClassName("android.widget.Button");node.setText(hit.label);node.setContentDescription(hit.label);
                node.setClickable(true);node.setFocusable(true);node.setEnabled(true);node.setVisibleToUser(isShown());
                Rect bounds=new Rect((int)(ox+hit.box.left*scale),(int)(oy+hit.box.top*scale),(int)(ox+hit.box.right*scale),(int)(oy+hit.box.bottom*scale));
                node.setBoundsInParent(bounds);int[] location=new int[2];getLocationOnScreen(location);bounds.offset(location[0],location[1]);node.setBoundsInScreen(bounds);
                node.addAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
                if(hit.hold!=null){node.setLongClickable(true);node.addAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK);}
                node.setAccessibilityFocused(accessibleFocus==id);
                node.addAction(accessibleFocus==id?android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS:android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
                return node;
            }
            @Override public boolean performAction(int id,int action,android.os.Bundle args) {
                if(id==HOST_VIEW_ID) return BoardView.this.performAccessibilityAction(action,args);
                if(id<0 || id>=hits.size()) return false;
                Hit hit=hits.get(id);
                if(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK){hit.tap.run();invalidate();return true;}
                if(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK && hit.hold!=null){hit.hold.run();invalidate();return true;}
                if(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS){
                    if(accessibleFocus>=0) accessibilityEvent(accessibleFocus,android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
                    accessibleFocus=id;accessibilityEvent(id,android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);invalidate();return true;
                }
                if(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS){accessibleFocus=-1;accessibilityEvent(id,android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);invalidate();return true;}
                return false;
            }
        };
    }
    @Override public boolean dispatchHoverEvent(MotionEvent event) {
        android.view.accessibility.AccessibilityManager manager=(android.view.accessibility.AccessibilityManager)getContext().getSystemService(android.content.Context.ACCESSIBILITY_SERVICE);
        if(manager!=null && manager.isTouchExplorationEnabled()) {
            int id=-1;float x=(event.getX()-ox)/scale,y=(event.getY()-oy)/scale;
            if(event.getAction()!=MotionEvent.ACTION_HOVER_EXIT) for(int i=hits.size()-1;i>=0;i--) if(hits.get(i).box.contains(x,y)){id=i;break;}
            if(id!=hovered){if(hovered>=0) accessibilityEvent(hovered,android.view.accessibility.AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);hovered=id;if(id>=0)accessibilityEvent(id,android.view.accessibility.AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);}
            return true;
        }
        return super.dispatchHoverEvent(event);
    }
    @Override public boolean performClick() {super.performClick();return true;}
}
