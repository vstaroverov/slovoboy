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
    private float scale=1,ox,oy,navigationOffset;
    private Canvas canvas;
    private Hit pressed;
    private long downAt,flashUntil,attackUntil;
    private int flashCard=-1, errorCard=-1,errorSlot=-1;
    private long errorUntil;
    private static final int BG=0xfff4f2fa,INK=0xff29243f,MUTED=0xff79738e,PURPLE=0xff7051df,PALE=0xffece6ff,GREEN=0xff23896a,MINT=0xffe0f4eb,WHITE=0xffffffff,LINE=0xffe2deed,ROSE=0xffe07685;
    private static final float W=432,H=935;
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
    private void boundedCenter(String s,float x,float y,float maxWidth,float size,int color,boolean strong) {
        p.setTypeface(strong?bold:regular);size=readableSize(size);p.setTextSize(size);
        size*=Math.min(1,maxWidth/Math.max(1,p.measureText(s)));p.setTextSize(size);
        rawText(s,x-p.measureText(s)/2,y,size,color,strong);
    }
    private void hit(float x,float y,float w,float h,Runnable action) {hit(x,y,w,h,action,null);}
    private void hit(float x,float y,float w,float h,Runnable action,Runnable hold) {
        y+=navigationOffset;
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
        navigationOffset=0;
        if(!a.screen.equals("splash")){playerProgress();navigationOffset=55;c.translate(0,55);}
        Game g=a.game;String chosen=a.chosen();
        if(!a.screen.equals("game")){shell();c.restore();return;}
        if(a.matching()){matchmaking();c.restore();return;}
        text("словобой",20,39,24,INK,true);text("РАУНД "+g.round,156,37,11,PURPLE,true);
        int remaining=Game.LIMIT-g.elapsed;
        box(270,13,89,36,WHITE,18);center(String.format(Locale.ROOT,"%02d:%02d",remaining/60,remaining%60),314,37,16,INK,true);
        box(370,13,42,36,WHITE,14);center("Ⅱ",391,37,18,PURPLE,true);hit(366,9,48,44,a::pause);label("Пауза");
        health(20,65,"ВЫ",g.human.hp,g.human.score(),PURPLE);
        health(222,65,"СОПЕРНИК",g.bot.hp,g.bot.score(),ROSE);
        boolean recentAttack=SystemClock.uptimeMillis()<attackUntil;
        String attack=recentAttack?(g.lastAttackHuman?"Ваш удар! ":"Удар соперника! ")+"«"+g.lastAttackWord+"» → −"+g.lastAttackDamage+" HP":"Слово целиком = мгновенный удар на 1 / 3 / 5 HP";
        center(attack,216,153,13,recentAttack?(g.lastAttackHuman?GREEN:ROSE):MUTED,recentAttack);
        text("Слова соперника",22,181,12,MUTED,false);
        for(int i=0;i<6;i++) {
            Game.Card card=g.bot.cards.get(i);float x=170+i*31;
            for(int s=0;s<card.parts.length;s++) box(x+s*7,172,5,8,card.filled[s]?ROSE:LINE,2);
        }
        text("↗",392,182,18,MUTED,true);hit(20,160,392,32,a::opponent);label("Открытые карточки соперника");
        box(20,200,392,65,PALE,18);
        String title=recentAttack && g.lastAttackHuman?"Удар! Уже появилось новое слово":a.hasTarget()?"Нажмите фишку для выбранного слота":chosen==null?"Выберите фишку или слот":"Теперь выберите слот в слове";
        String subtitle=recentAttack && g.lastAttackHuman?"«"+g.lastAttackWord+"» собрано: −"+g.lastAttackDamage+" HP сопернику. Продолжайте!":a.hasTarget()?"Выбран слот «"+a.targetPart()+"». Теперь выберите фишку.":chosen==null?Rules.GOAL:"Нажмите на слот. Проверим, совпадают ли слоги.";
        fit(title,36,226,360,17,PURPLE,true);fit(subtitle,36,249,360,13,0xff645b84,false);
        text("ВАШИ СЛОВА",22,291,12,MUTED,true);text("Ударов: "+g.human.completed(),310,291,12,PURPLE,true);
        for(int i=0;i<3;i++) card(i,20+i*134,304,124,84);
        for(int i=0;i<2;i++) card(i+3,20+i*202,400,190,84);
        card(5,20,496,392,84);
        String notice=SystemClock.uptimeMillis()<a.messageUntil?a.message:"Собрали → ударили → получили новое слово";
        fit(notice,22,605,390,12,SystemClock.uptimeMillis()<errorUntil?ROSE:SystemClock.uptimeMillis()<a.messageUntil?GREEN:MUTED,false);
        box(8,622,416,246,WHITE,24);
        text("ВАШИ ФИШКИ",24,649,12,MUTED,true);text(g.human.hand.size()+"/12",149,649,12,MUTED,false);
        text(g.human.hand.size()+(g.human.offer==null?0:1)>=Rules.HAND_LIMIT?"Рука полна":"+"+Rules.DRAW_COUNT+" через "+g.secondsToDraw()+" с",302,649,12,PURPLE,true);
        for(int i=0;i<12;i++) {
            float x=24+(i%6)*65,y=663+(i/6)*55;
            if(i<g.human.hand.size()) {
                final int index=i;String syllable=g.human.hand.get(i);boolean selected=a.selected==i;
                box(x,y+3,58,46,selected?0xff5032b0:0xffded8ed,12);box(x,y,58,46,selected?PURPLE:0xfff3effb,12);
                boundedCenter(syllable,x+29,y+29,50,18,selected?WHITE:INK,true);
                hit(x,y,58,49,()->a.choose(index),()->a.discard(index));label("Фишка «"+syllable+"»"+(selected?", выбрана":"")+". Долгое нажатие — сбросить.");
            } else box(x,y,58,46,0xfffaf9fc,12);
        }
        text(g.human.offer==null?"СЛЕПОЙ ОБМЕН":"ПРЕДЛОЖЕНО: "+g.human.offer,25,794,11,MUTED,true);
        text(g.bot.offer==null?"Соперник выбирает фишку":"Соперник готов к обмену",25,815,12,g.bot.offer==null?MUTED:GREEN,false);
        String exchangeLabel=g.human.offer!=null?"Забрать обратно":chosen!=null?"Обменять «"+chosen+"»":"Выберите фишку";
        button(exchangeLabel,223,774,185,53,chosen!=null || g.human.offer!=null,a::exchange);
        text("Правила",25,852,13,PURPLE,true);hit(20,830,85,38,a::rules);label("Правила игры и сведения о приложении");
        center(a.soundEnabled()?"Звук: вкл":"Звук: выкл",151,852,12,PURPLE,true);hit(108,830,86,38,a::soundSettings);label("Звук: настройки громкости");
        center(g.human.replace>=0?"Заменить слово ✓":"Заменить слово",309,852,13,PURPLE,true);hit(205,830,207,38,a::dealer);label("Заменить слово: замена по взаимной заявке");
        if(g.result!=null) finish();else if(a.awaitingResume) resumeOverlay();
        if(g.result!=null || a.awaitingResume)button(a.soundEnabled()?"Звук: вкл":"Звук: выкл",130,790,172,44,false,a::soundSettings);
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
            boundedCenter(card.parts[s],sx+tw/2,y+59,tw-6,tw<50?16:18,filled?WHITE:wrong?ROSE:target?PURPLE:0xff9b94af,true);
            hit(sx-1,y+30,tw+2,47,()->a.slot(index,si));label("Слово «"+card.word()+"», слог «"+card.parts[s]+"», "+(filled?"установлен; нажмите без выбора фишки, чтобы снять":target?"выбранный пустой слот":"пустой слот"));
        }
    }
    private void veil() {box(0,0,W,H,0xc925203d,0);hits.clear();}
    private void matchmaking() {
        hits.clear();center("словобой",216,147,34,INK,true);
        box(24,246,384,376,WHITE,28);
        center("Подбираем соперника...",216,307,25,PURPLE,true);
        center("До начала дуэли",216,351,16,MUTED,false);
        center(String.valueOf(a.countdown.remaining()),216,461,88,PURPLE,true);
        for(int i=0;i<Rules.START_COUNTDOWN_SECONDS;i++)box(181+i*35,498,26,7,i<Rules.START_COUNTDOWN_SECONDS-a.countdown.remaining()?PURPLE:PALE,4);
        center("Собирайте слова и наносите урон",216,552,15,INK,true);
        center("Выбирайте фишку и слот в любом порядке",216,580,12,MUTED,false);
        center("Офлайн-дуэль · раунды по "+Rules.DURATION,216,655,13,MUTED,false);
        button(a.soundEnabled()?"Звук: вкл":"Звук: выкл",130,690,172,44,false,a::soundSettings);
    }
        private void resumeOverlay() {
        veil();box(34,294,364,254,WHITE,28);
        center("Партия сохранена",216,344,26,INK,true);
        center("Соперник и таймер ждут вас",216,379,15,MUTED,false);
        center("Ничего не пропущено — продолжайте со своего места",216,411,11,MUTED,false);
        button("Продолжить дуэль",58,453,316,56,true,()->a.resumeMatch());
    }
    private void star(float x,float y,float radius,int color) {
        Path shape=new Path();for(int i=0;i<10;i++){double angle=-Math.PI/2+i*Math.PI/5;float r=i%2==0?radius:radius*.46f;float px=x+(float)Math.cos(angle)*r,py=y+(float)Math.sin(angle)*r;if(i==0)shape.moveTo(px,py);else shape.lineTo(px,py);}shape.close();p.setColor(color);canvas.drawPath(shape,p);
    }
    private final Typeface brandFont=Typeface.create(Typeface.createFromAsset(getContext().getAssets(),"fonts/Manrope-variable.ttf"),Typeface.BOLD);
    private void brand(float y,boolean dark) {
        box(42,y-80,348,232,WHITE,20);
        Path mark=new Path();float[] points={54,15,64,39,90,42,70,60,76,87,54,73,32,87,38,60,18,42,44,39};
        canvas.save();canvas.translate(162,y-54);for(int i=0;i<points.length;i+=2){if(i==0)mark.moveTo(points[i],points[i+1]);else mark.lineTo(points[i],points[i+1]);}mark.close();p.setColor(0xff061014);canvas.drawPath(mark,p);
        Path cut=new Path();cut.moveTo(38,45);cut.lineTo(54,64);cut.lineTo(70,45);cut.lineTo(65,41);cut.lineTo(54,54);cut.lineTo(43,41);cut.close();p.setColor(0xffd8ff18);canvas.drawPath(cut,p);canvas.restore();
        p.setTypeface(brandFont);p.setTextSize(24);p.setColor(0xff061014);canvas.drawText("V-STAR GAMES",216-p.measureText("V-STAR GAMES")/2,y+80,p);
        center("by V-STAR Group",216,y+109,11,MUTED,false);
    }
    private void playerProgress(){
        Progression profile=a.progression;box(20,4,392,45,WHITE,12);
        text("Уровень игрока "+profile.level(),32,23,12,INK,true);
        text(profile.xpInLevel()+" / "+profile.xpNeeded()+" XP",273,23,11,PURPLE,true);
        box(32,33,368,5,PALE,3);float fill=368f*profile.xpInLevel()/profile.xpNeeded();if(fill>0)box(32,33,fill,5,PURPLE,3);
    }
    private void shell(){
        hits.clear();
        if(a.screen.equals("splash")){box(0,0,W,H,WHITE,0);brand(345,true);return;}
        if(a.screen.equals("menu")){
            brand(140,false);center("СЛОВОБОЙ",216,355,42,INK,true);
            center("Каждое слово — твой удар",216,389,16,MUTED,false);
            box(72,425,84,68,WHITE,18);center("СЛО",114,469,24,PURPLE,true);
            box(166,425,84,68,WHITE,18);center("ВО",208,469,24,PURPLE,true);
            box(260,425,100,68,0xffffcc66,18);center("БОЙ",310,469,24,INK,true);
            button("Начать играть",42,546,348,60,true,a::showMap);
            if(a.started)button("Продолжить дуэль",42,620,348,52,false,a::continueSaved);
            button("Выход",42,696,348,52,false,a::exitGame);
            center("Звук: "+(a.soundEnabled()?"включён":"выключен"),216,799,14,PURPLE,true);hit(120,773,192,44,a::soundSettings);label("Звук: настройки громкости");
            return;
        }
        text("Карта уровней",24,60,29,INK,true);text("Выберите свой следующий вызов",24,92,14,MUTED,false);
        p.setColor(LINE);p.setStrokeWidth(6);canvas.drawLine(110,246,320,473,p);canvas.drawLine(320,473,170,700,p);
        levelNode(1,103,222,"Классика","36 слов · правила RC3","Раздача каждые 10 секунд",a.won(1));
        levelNode(2,316,464,"Открытия","360 слов · правила RC5","При пустой руке — фишка через 1 с",a.won(2));
        box(101,662,130,64,LINE,26);center("Скоро",166,702,18,MUTED,true);
        center("Новые правила — на следующих уровнях",216,763,13,MUTED,false);
        button("На главную",24,802,384,54,false,a::home);
    }
    private void levelNode(int level,float x,float y,String title,String rules,String detail,boolean won){
        box(24,y-45,384,186,WHITE,24);box(x-33,y-29,66,66,won?GREEN:PURPLE,23);center(won?"✓":String.valueOf(level),x,y+16,28,WHITE,true);
        float tx=level==1?153:47;text(title,tx,y-6,24,INK,true);text(won?"ПОБЕДА ЗАСЧИТАНА":"ДОСТУПЕН",tx,y+19,10,won?GREEN:MUTED,true);
        text(rules,43,y+67,15,INK,true);text(detail,43,y+94,12,MUTED,false);
        hit(24,y-45,384,186,()->a.enterLevel(level));label("Уровень "+level+". "+title+". "+rules+". "+detail);
    }
    private void resultArt(boolean win){
        if(win){
            for(int i=0;i<22;i++){float x=47+(i*73)%338,y=117+(i*41)%153;box(x,y,5,11,i%3==0?PURPLE:i%3==1?0xffffc65c:GREEN,2);}
            box(161,192,110,53,0xffffc65c,28);box(206,232,20,39,0xffe5a541,5);box(175,267,82,13,0xffffc65c,6);star(216,196,44,0xffffcf65);star(216,196,20,WHITE);
        }else{
            box(160,145,112,112,PALE,25);star(216,184,25,MUTED);p.setColor(MUTED);p.setStrokeWidth(4);canvas.drawLine(188,217,201,217,p);canvas.drawLine(231,217,244,217,p);p.setStyle(Paint.Style.STROKE);canvas.drawArc(204,231,228,250,200,140,false,p);p.setStyle(Paint.Style.FILL);
        }
    }
    private void finish() {
        Game g=a.game;boolean next=g.canContinue(),win=g.bot.hp==0;
        veil();box(24,next?224:88,384,next?443:666,WHITE,28);
        if(!next){resultArt(win);center(win?"Вы победили!":"Вы проиграли",216,333,33,win?GREEN:INK,true);center((win?"Победа":"Поражение")+" в "+g.round+" раунде!",216,369,19,win?PURPLE:MUTED,true);center(win?"+100 XP · Отличная игра!":"Новая дуэль — новый шанс на победу",216,403,13,MUTED,false);}
        else {center(g.result,216,275,26,PURPLE,true);center("Раунд завершён. Дуэль продолжается",216,311,13,MUTED,false);}
        float y=next?359:458;
        box(48,y-25,336,99,BG,18);center("Здоровье   "+g.human.hp+" : "+g.bot.hp,216,y+6,22,INK,true);
        center("Урон за раунд   "+g.human.roundDamage+" : "+g.bot.roundDamage,216,y+35,15,MUTED,false);center("Слов собрано   "+g.human.completed()+" : "+g.bot.completed(),216,y+59,13,MUTED,false);
        if(next){center("Раздача раз в "+(g.level==1?10:Rules.drawSecondsForRound(g.round+1))+" с · HP сохранены",216,469,13,MUTED,false);button("Продолжить → раунд "+(g.round+1),48,491,336,50,true,a::nextRound);}
        button("Ещё одна дуэль",48,next?552:568,336,52,!next,a::fresh);
        button("Выйти на главную",48,next?613:632,336,50,false,a::home);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;
        if(e.getAction()==MotionEvent.ACTION_DOWN) {
            pressed=null;float nearest=Float.MAX_VALUE;for(Hit h:hits) if(h.box.contains(x,y)){float distance=(x-h.box.centerX())*(x-h.box.centerX())+(y-h.box.centerY())*(y-h.box.centerY());if(distance<nearest){nearest=distance;pressed=h;}}
            downAt=SystemClock.uptimeMillis();return true;
        }
        if(e.getAction()==MotionEvent.ACTION_UP) {
            Hit action=pressed;pressed=null;
            if(a.matching() && action!=null && !action.label.startsWith("Звук"))return true;
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
                    node.setContentDescription(!a.screen.equals("game")?(a.screen.equals("map")?"Карта уровней. Выберите классику RC3 или открытия RC5.":a.screen.equals("splash")?"V-Star Games":"Словобой. Главное меню."):a.matching()?a.countdown.description():(a.game.result!=null?a.game.result+". ":"Словобой. Раунд "+a.game.round+". ")+"Ваши HP "+a.game.human.hp+", HP соперника "+a.game.bot.hp+". Осталось "+(Game.LIMIT-a.game.elapsed)+" секунд. Раздача каждые "+a.game.drawInterval()+" с, следующая через "+a.game.secondsToDraw()+" с.");
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
                if(a.matching() && !hit.label.startsWith("Звук"))return false;
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
