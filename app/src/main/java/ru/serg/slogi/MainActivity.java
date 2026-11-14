package ru.serg.slogi;

import android.app.*;
import android.os.*;
import android.view.*;

public final class MainActivity extends Activity {
    Game game;
    Progression progression;
    private android.content.SharedPreferences progressPreferences;
    String screen="splash";
    private android.content.SharedPreferences campaign;
    StartCountdown countdown;
    BoardView board;
    boolean started, dialogOpen, foreground, awaitingResume;
    private SessionStore store;
    private SoundEffects sounds;
    private Dialog currentDialog;
    int selected=-1,targetCard=-1,targetSlot=-1;
    String targetWord="";
    String message="";
    long messageUntil;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable clock=new Runnable() {
        public void run() {
            if(matching() && foreground && !dialogOpen) {
                if(countdown.tick()){beginMatch();return;}
                save();board.invalidate();board.announceForAccessibility(String.valueOf(countdown.remaining()));
            } else if(screen.equals("game") && started && !dialogOpen && !awaitingResume && foreground && game.result==null) {
                int event=game.eventId, attack=game.attackId; int count=game.human.hand.size();
                GameAudio.Before before=new GameAudio.Before(game);
                game.tick();
                sounds.play(GameAudio.changed(before,game));recordVictory();
                if(attack!=game.attackId) { board.showAttack(); say(game.attackText()); }
                else if(event!=game.eventId) say(game.event.startsWith("Скрытый")?"Обмен состоялся! Новая фишка уже в руке.":"Слова заменены. Слоги вернулись в руку.");
                else if(game.human.hand.size()>count) say("Новые слоги! Выберите фишку внизу.");
                if(game.elapsed%5==0 || game.result!=null) save();
                board.invalidate();
            }
            handler.postDelayed(this,1000);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        campaign=getSharedPreferences("campaign",MODE_PRIVATE);
        progressPreferences=getSharedPreferences("player-progression",MODE_PRIVATE);
        progression=new Progression(progressPreferences.getInt("wins",0),progressPreferences.getString("last-victory",""));
        sounds=new SoundEffects(this);
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
                store=new SessionStore(this);
        SessionStore.State saved=state==null?store.load():null;
        game=state!=null?(Game)state.getSerializable("game"):saved!=null?saved.game:new Game(System.currentTimeMillis());
        if(game==null) game=new Game(System.currentTimeMillis());
        started=state!=null?state.getBoolean("started"):saved!=null && saved.started;
        countdown=state!=null?(StartCountdown)state.getSerializable("countdown"):saved!=null?saved.countdown:null;
        if(countdown==null)countdown=new StartCountdown();
        selected=state!=null?state.getInt("selected",-1):saved!=null?saved.selected:-1;
                targetCard=state!=null?state.getInt("targetCard",-1):saved!=null && saved.inputSchema==2?saved.targetCard:-1;
        targetSlot=state!=null?state.getInt("targetSlot",-1):saved!=null && saved.inputSchema==2?saved.targetSlot:-1;
        targetWord=state!=null?state.getString("targetWord",""):saved!=null && saved.inputSchema==2?saved.targetWord:"";
        if(targetWord==null) targetWord="";
        if(game.dictionaryUpdatedOnLoad){selected=-1;clearTarget();}
        awaitingResume=started && game.result==null;
        getWindow().setStatusBarColor(0xfff4f2fa);
        getWindow().setNavigationBarColor(0xfff4f2fa);
        if(Build.VERSION.SDK_INT>=30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                board=new BoardView(this);setContentView(board);
        board.setOnApplyWindowInsetsListener((view,insets)->{
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                view.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        board.requestApplyInsets();
        handler.postDelayed(()->{if(screen.equals("splash")){screen="menu";board.invalidate();maybeLevelUp();}},1400);
    }
    @Override public void onResume() {super.onResume();foreground=true;sounds.setActive(true);restartClock();handler.postDelayed(this::maybeLevelUp,1600);}
    @Override public void onPause() {foreground=false;sounds.setActive(false);handler.removeCallbacks(clock);awaitingResume=started && game.result==null;save();super.onPause();}
    @Override public void onDestroy() {handler.removeCallbacks(clock);if(currentDialog!=null)currentDialog.dismiss();sounds.close();super.onDestroy();}
    @Override public void onSaveInstanceState(Bundle b) {b.putSerializable("game",game);b.putSerializable("countdown",countdown);b.putBoolean("started",started);b.putInt("selected",selected);b.putInt("targetCard",targetCard);b.putInt("targetSlot",targetSlot);b.putString("targetWord",targetWord);super.onSaveInstanceState(b);}
        void save() {if(store!=null && game!=null){store.save(game,started,selected,targetCard,targetSlot,targetWord,countdown);if(game.level==2)saveHistory();}}
    boolean matching() {return screen.equals("game") && !started && game.result==null;}
        private void restartClock() {handler.removeCallbacks(clock);if(foreground)handler.postDelayed(clock,1000);}
    void beginMatch() {started=true;awaitingResume=false;restartClock();sounds.play(GameAudio.Cue.START);say("Выберите фишку и слот — в любом порядке.");}
    void resumeMatch() {awaitingResume=false;restartClock();sounds.play(GameAudio.Cue.SELECT);if(game.dictionaryUpdatedOnLoad){game.dictionaryUpdatedOnLoad=false;say("Слова с одиночными буквами заменены. Подходящие фишки возвращены в руку.");}save();board.invalidate();}
    boolean soundEnabled() {return sounds.enabled();}
    void toggleSound() {sounds.setEnabled(!sounds.enabled());if(sounds.enabled())sounds.play(GameAudio.Cue.SELECT);board.invalidate();board.announceForAccessibility(sounds.enabled()?"Звук включён":"Звук выключен");}
    private void changedSound(GameAudio.Before before,GameAudio.Cue fallback) {GameAudio.Cue cue=GameAudio.changed(before,game);sounds.play(cue==null?fallback:cue);}
    @Override public void onBackPressed() {if(screen.equals("game")) {if(game.result==null)pause();else home();}else if(screen.equals("map")){screen="menu";board.invalidate();}else exitGame();}
    void say(String s) {message=s;messageUntil=SystemClock.uptimeMillis()+4500;board.invalidate();save();board.announceForAccessibility(s);}
    String chosen() {return selected>=0 && selected<game.human.hand.size()?game.human.hand.get(selected):null;}
    void clearTarget() {targetCard=-1;targetSlot=-1;targetWord="";}
    boolean hasTarget() {
        if(targetCard<0 || targetCard>=game.human.cards.size()) return false;
        Game.Card card=game.human.cards.get(targetCard);
        if(targetSlot<0 || targetSlot>=card.parts.length || card.filled[targetSlot] || !card.word().equals(targetWord)) {clearTarget();return false;}
        return true;
    }
    boolean isTarget(int card,int slot) {return hasTarget() && targetCard==card && targetSlot==slot;}
    String targetPart() {return hasTarget()?game.human.cards.get(targetCard).parts[targetSlot]:"";}
    void choose(int index) {
        if(index<0 || index>=game.human.hand.size()) return;
        if(selected==index){selected=-1;say(hasTarget()?"Фишка не выбрана. Можно выбрать другую для слота.":"Выбор фишки снят.");return;}
        selected=index;
        if(hasTarget()) placeSelected(targetCard,targetSlot);
        else {sounds.play(GameAudio.Cue.SELECT);say("Выбрано «"+chosen()+"». Нажмите на слот в слове.");}
        board.invalidate();
    }
    private void placeSelected(int ci,int si) {
        Game.Card card=game.human.cards.get(ci);
        targetCard=ci;targetSlot=si;targetWord=card.word();
        GameAudio.Before before=new GameAudio.Before(game);
        if(game.place(selected,ci,si)) {
            changedSound(before,GameAudio.Cue.PLACE);recordVictory();
            selected=-1;clearTarget();
            if(card.complete()){board.showAttack();say(game.attackText());}
            else say("Слог поставлен. Заполните всё слово, чтобы ударить.");
            board.celebrate(ci);
        } else {sounds.play(GameAudio.Cue.ERROR);board.reject(ci,si);say("Не подходит. Фишка «"+chosen()+"» осталась в руке.");}
    }
    void slot(int ci,int si) {
        Game.Card card=game.human.cards.get(ci);
        if(card.filled[si]) {
            if(chosen()!=null){sounds.play(GameAudio.Cue.ERROR);board.reject(ci,si);say("Слот уже заполнен. Снимите выбор фишки для переноса.");return;}
            clearTarget();int count=game.human.hand.size();game.take(ci,si);
            if(game.human.hand.size()>count){sounds.play(GameAudio.Cue.REMOVE);selected=game.human.hand.size()-1;say("Слог снят. Теперь выберите ему новое место.");}
            else {sounds.play(GameAudio.Cue.ERROR);say("Рука заполнена. Сначала освободите место.");}
        } else if(chosen()!=null) placeSelected(ci,si);
        else if(isTarget(ci,si)){clearTarget();say("Выбор слота снят.");}
        else {sounds.play(GameAudio.Cue.SELECT);targetCard=ci;targetSlot=si;targetWord=card.word();say("Выбран слот «"+card.parts[si]+"». Теперь нажмите фишку внизу.");}
        board.invalidate();
    }
    void exchange() {
        if(game.human.offer==null && chosen()==null) {sounds.play(GameAudio.Cue.ERROR);say("Выберите фишку в руке, чтобы предложить её сопернику.");return;}
        boolean cancel=game.human.offer!=null;
        GameAudio.Before before=new GameAudio.Before(game);
        game.offer(selected);selected=-1;
        if(cancel)sounds.play(GameAudio.Cue.REMOVE);else changedSound(before,GameAudio.Cue.SELECT);
        say(cancel?"Предложение отменено. Фишка снова в руке.":game.human.offer==null?"Обмен состоялся! Посмотрите новую фишку в руке.":"Предложение скрыто от соперника. Ждём его фишку.");
    }
    void showDialog(Dialog d) {
        currentDialog=d;dialogOpen=true;save();
        d.setOnDismissListener(v->{if(currentDialog==d){currentDialog=null;dialogOpen=false;if(!awaitingResume)restartClock();handler.post(this::maybeLevelUp);}board.invalidate();});d.show();
    }
    void rules() {
        showDialog(new AlertDialog.Builder(this).setTitle(Rules.VERSION)
            .setMessage(levelRules()+"\n\n"+Rules.TEXT).setPositiveButton("На стол",null).setNeutralButton("Об игре",(d,w)->handler.post(this::about)).create());
    }
        void about() {
        showDialog(new AlertDialog.Builder(this).setTitle("Словобой · "+BuildConfig.VERSION_NAME)
            .setMessage("Староверов Вячеслав\nКонтакт: w1ret@yandex.ru\n\nОфлайн-игра без рекламы, аккаунтов, платежей и аналитики. Партия сохраняется только на этом устройстве. Сетевого доступа и передачи игровых данных нет. Сохранение удаляется при очистке данных приложения.\n\nИгра использует слоговые фрагменты слов из авторского набора. Это игровая механика, а не логопедический учебник.")
            .setPositiveButton("Закрыть",null).create());
    }
    void pause() {
        showDialog(GameDialog.create(this,"Ⅱ","Пауза", "Уровень "+displayLevel()+" · раунд "+game.round+"\nТаймер и соперник ждут. Слово = удар на 1 / 3 / 5 HP.",
            new String[]{"Продолжить","Новая дуэль","На главную"},new Runnable[]{this::resumeMatch,()->handler.post(this::restart),this::home}));
    }
    void dealer() {
        String[] labels=new String[6];
        for(int i=0;i<6;i++) labels[i]=(game.human.replace==i?"✓ ":"")+game.human.cards.get(i).word()+" · "+game.human.cards.get(i).parts.length+" слога";
        // Delay opening the next dialog until the list dialog has dismissed.
        showDialog(new AlertDialog.Builder(this).setTitle("Какое слово заменить?")
            .setItems(labels,(d,i)->handler.post(()->{
                showDialog(new AlertDialog.Builder(this).setTitle(labels[i]).setMessage("Замена произойдёт, когда соперник тоже подаст заявку. Новое слово будет той же длины. Выложенные слоги вернутся в руку до лимита; лишние сбросятся. Повторный выбор отменяет заявку.")
                    .setPositiveButton(game.human.replace==i?"Отменить заявку":"Предложить замену",(a,b)->{GameAudio.Before before=new GameAudio.Before(game);game.requestCard(i);changedSound(before,GameAudio.Cue.SELECT);selected=-1;say(game.human.replace>=0?"Запрос на замену принят. Ждём решения соперника.":"Заявка обработана.");})
                    .setNegativeButton("Назад",null).create());
            })).setNegativeButton("Закрыть",null).create());
    }
    void discard(int index) {
        showDialog(DiscardDialog.create(this,game.human.hand.get(index),()->{game.discard(index);sounds.play(GameAudio.Cue.REMOVE);selected=-1;say("Место в руке освобождено.");}));
    }
    void opponent() {
        StringBuilder s=new StringBuilder("Видно только заполнение слов. Рука и предложение соперника скрыты.\n\n");
        for(Game.Card c:game.bot.cards) {s.append(c.word()).append("  ");for(boolean b:c.filled) s.append(b?"●":"○");s.append("\n\n");}
        showDialog(new AlertDialog.Builder(this).setTitle("Карточки соперника").setMessage(s).setPositiveButton("На стол",null).create());
    }
    void restart() {
        showDialog(GameDialog.create(this,"↻","Начать заново?","Текущая дуэль закончится. Вы останетесь на том же уровне.",new String[]{"Новая дуэль","Продолжить"},new Runnable[]{this::fresh,this::resumeMatch}));
    }
    void nextRound() {
        if(game.continueRound()) {awaitingResume=false;started=true;restartClock();board.clearFeedback();sounds.play(GameAudio.Cue.START);say("Раунд "+game.round+" начался. HP, слова и фишки сохранены.");}
    }
    void exitGame() {save();finish();}
    void fresh() {screen="game";game=new Game(System.currentTimeMillis(),displayLevel(),history());countdown=new StartCountdown();selected=-1;clearTarget();started=false;awaitingResume=false;message="";messageUntil=0;board.clearFeedback();restartClock();save();board.invalidate();}

    int displayLevel(){return game.level==1?1:2;}
    String levelRules(){return displayLevel()==1?"Уровень 1 · Классика RC3\n36 исходных слов. Раздача раз в 10 секунд во всех раундах.":"Уровень 2 · Открытия RC5\n360 слов, в том числе редкие. Новые слова вероятнее, повторы возможны. Раздача 10/9/8/.../1 с по раундам.";}
    void home(){save();screen="menu";awaitingResume=started&&game.result==null;selected=-1;clearTarget();board.invalidate();}
    void showMap(){screen="map";board.invalidate();}
    void enterLevel(int level){save();game=new Game(System.currentTimeMillis(),level,history());freshPrepared();}
    private void freshPrepared(){screen="game";countdown=new StartCountdown();started=false;awaitingResume=false;selected=-1;clearTarget();board.clearFeedback();message="";messageUntil=0;save();restartClock();board.invalidate();}
    void continueSaved(){screen="game";awaitingResume=started&&game.result==null;restartClock();board.invalidate();}
    boolean won(int level){return campaign.getBoolean("won-"+level,false);}
    private void recordVictory(){
        if(game.result==null||game.canContinue()||game.bot.hp!=0)return;
        campaign.edit().putBoolean("won-"+displayLevel(),true).apply();
        if(game.victoryRewarded)return;
        int previous=progression.level();
        if(progression.reward(game.matchId)){
            android.content.SharedPreferences.Editor editor=progressPreferences.edit().putInt("wins",progression.victories).putString("last-victory",progression.lastVictory);
            if(progression.level()>previous)editor.putInt("pending-level",progression.level());
            if(!editor.commit()){progression=new Progression(progressPreferences.getInt("wins",0),progressPreferences.getString("last-victory",""));return;}
        }
        game.victoryRewarded=true;save();handler.postDelayed(this::maybeLevelUp,850);
    }
    private void maybeLevelUp(){
        int level=progressPreferences.getInt("pending-level",0);
        if(level<=0||!foreground||dialogOpen||screen.equals("splash"))return;
        showDialog(GameDialog.create(this,"★","Новый уровень!","Поздравляем! Уровень игрока "+level+".\n+100 XP за победу.\nПобед до следующего уровня: "+(level+1)+".",new String[]{"Отлично!"},new Runnable[]{()->{}}));
        progressPreferences.edit().remove("pending-level").apply();sounds.play(GameAudio.Cue.START);
    }
    int soundVolume(){return sounds.volume();}
    void setSoundVolume(int value){sounds.setVolume(value);}
    void previewSound(){sounds.play(GameAudio.Cue.SELECT);}
    void soundSettings(){showDialog(SoundSettingsDialog.create(this));}
    private WordHistory.Profile history(){
        try{String s=campaign.getString("history","");if(!s.isEmpty())try(java.io.ObjectInputStream in=new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(android.util.Base64.decode(s,0)))){return (WordHistory.Profile)in.readObject();}}catch(Exception ignored){}
        return new WordHistory.Profile();
    }
    private void saveHistory(){try{java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();try(java.io.ObjectOutputStream out=new java.io.ObjectOutputStream(bytes)){out.writeObject(game.vocabulary);}campaign.edit().putString("history",android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP)).apply();}catch(java.io.IOException ignored){}}
}
