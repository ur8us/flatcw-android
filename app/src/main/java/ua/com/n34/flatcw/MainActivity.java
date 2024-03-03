package ua.com.n34.flatcw;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
//import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    // Preferences
    SharedPreferences sPref;

    void savePrefs() {
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putInt("speedWpm", speedWpm);
        ed.putBoolean("reversePaddles", reversePaddles);
        ed.commit();
//        Toast.makeText(this, "Text saved", Toast.LENGTH_SHORT).show();
    }

    void loadPrefs() {
        sPref = getPreferences(MODE_PRIVATE);
        speedWpm = sPref.getInt("speedWpm", INIT_WPM_20);
        reversePaddles = sPref.getBoolean("reversePaddles", false);
//        Toast.makeText(this, "Text loaded", Toast.LENGTH_SHORT).show();
    }

//    TextView tv;

    // Tone generators
    ToneGenerator toneGenerator1;
    ToneGenerator toneGenerator2;

    // Touch
    boolean inTouch = false;
    boolean inTouchOld = false;
    String result = "";
    StringBuilder sb = new StringBuilder();
    int upPI = 0;
    int downPI = 0;

    int viewWidth = 0, viewHeight = 0;

    int touchIdDit = -1, touchIdDah = -1;
    boolean memDit = false, memDah = false;

    // CW machine

    boolean isDitPressed() {return (touchIdDit != -1); }
    boolean isDahPressed() {return (touchIdDah != -1); }

    enum CWSTATES {CW_NONE, CW_SENDING_DIT, CW_SENDING_DAH, CW_PAUSE_AFTER_DIT, CW_PAUSE_AFTER_DAH };
    CWSTATES cwState = CWSTATES.CW_NONE;

    // CW thread

    class CwThread extends Thread
    {
        public void run() {

            long curTime, endTime=0;

            while(true) {
                try {
                    this.sleep(inForeground ? 1: 1000); // 1ms delay, more delay if running in background
                } catch (InterruptedException e) {
                }

//                if (cwState == CWSTATES.CW_NONE) {
//
//
//                }

                curTime = System.currentTimeMillis();

                // CW state machine

                switch (cwState) {

                    case CW_NONE:
                        if (isDitPressed()) {
                            endTime = curTime + getDitTime();
                            soundOn();
                            memDit = false;
                            cwState = CWSTATES.CW_SENDING_DIT;
                        }
                        else
                        if (isDahPressed()) {
                            endTime = curTime + getDahTime();
                            soundOn();
                            memDah = false;
                            cwState = CWSTATES.CW_SENDING_DAH;
                        }
                        break;

                    case CW_SENDING_DIT:
                        if (curTime >= endTime) {
                            endTime += getPauseTime();
                            soundOff();
                            cwState = CWSTATES.CW_PAUSE_AFTER_DIT;
                        }
                        break;

                    case CW_SENDING_DAH:
                        if (curTime >= endTime) {
                            endTime += getPauseTime();
                            soundOff();
                            cwState = CWSTATES.CW_PAUSE_AFTER_DAH;
                        }
                        break;

                    case CW_PAUSE_AFTER_DIT:
                        if (curTime >= endTime) {
                            if (isDahPressed() || memDah) {
                                endTime += getDahTime();
                                soundOn();
                                memDah = false;
                                cwState = CWSTATES.CW_SENDING_DAH;
                            }
                            else if (isDitPressed() || memDit) {
                                endTime += getDitTime();
                                soundOn();
                                memDit = false;
                                cwState = CWSTATES.CW_SENDING_DIT;
                            }
                            else
                                cwState = CWSTATES.CW_NONE;
                        }
                        break;

                    case CW_PAUSE_AFTER_DAH:
                        if (curTime >= endTime) {
                            if (isDitPressed() || memDit) {
                                endTime += getDitTime();
                                soundOn();
                                memDit = false;
                                cwState = CWSTATES.CW_SENDING_DIT;
                            }
                            else if (isDahPressed() || memDah) {
                                endTime += getDahTime();
                                soundOn();
                                memDah = false;
                                cwState = CWSTATES.CW_SENDING_DAH;
                            }
                            else
                                cwState = CWSTATES.CW_NONE;
                        }
                        break;
                }

//                long currentTimeMillis = System.currentTimeMillis();
//                Log.d("CW", "ms: " + currentTimeMillis);
            }
        }

    }

    CwThread cwThread = null;

    //Timer

    int getDitTime() {
        return 1200/speedWpm;
    }

    int getDahTime() {
        return 3*1200/speedWpm;
    }

    int getPauseTime() {
        return 1200/speedWpm;
    }


    final int INIT_WPM_20 = 20;
    int speedWpm = INIT_WPM_20;

    boolean reversePaddles = false;

//    CountDownTimer cTimer = null;

    //start timer function
//    void startTimer() {
//
//        int interval = ditMs;
//        if(cwState == CWSTATES.CW_SENDING_DAH)
//            interval = ditMs*3;
//
//        cTimer = new CountDownTimer(interval, interval) {
//            public void onTick(long millisUntilFinished) {
//            }
//            public void onFinish() {
//                soundOff();
//                cwState = CWSTATES.CW_NONE;
//            }
//        };
//        cTimer.start();
//    }
//
//    //cancel timer
//    void cancelTimer() {
//        if(cTimer!=null)
//            cTimer.cancel();
//    }

    // Sound

    void soundOn(){
        toneGenerator1.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE);
    }
    void soundOff(){
        toneGenerator1.stopTone();
    }

    void secondSoundOn(){
        toneGenerator2.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE);
    }
    void secondSoundOff(){
        toneGenerator2.stopTone();
    }


    // Lifecycle

    boolean inForeground = true;

    public class AppLifecycleObserver implements LifecycleObserver {

        //public final String TAG = AppLifecycleObserver.class.getName();

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        public void onEnterForeground() {
            inForeground = true;
            secondSoundOn();
            Log.d("FlatCW", "onEnterForeground");
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        public void onEnterBackground() {
            inForeground = false;
            secondSoundOff();
            soundOff();
            Log.d("FlatCW", "onEnterBackground");
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
/*
        // Init text view
        tv = new TextView(this);
        tv.setTextSize(30);
        tv.setOnTouchListener(this);
        setContentView(tv);
*/
        setContentView(R.layout.activity_main);

        // Read vars

        if ( savedInstanceState != null ) {

            reversePaddles = savedInstanceState.getBoolean( "REV_PADDLES", false );
            speedWpm = savedInstanceState.getInt( "SPEED_WPM", INIT_WPM_20 );

        }
        else {

            try {
                loadPrefs();
            }
            catch (Exception e) {
            }
        }

        // Lifecycle

        AppLifecycleObserver appLifecycleObserver = new AppLifecycleObserver();
        getLifecycle().addObserver(appLifecycleObserver);

        //        int vw = tv.getWidth();
//        int vh = tv.getHeight();

        // Create menu

        Toolbar myToolbar = findViewById(R.id.myToolbar);
        setSupportActionBar(myToolbar);

        // Seek bar

        final SeekBar seekSpeed = (SeekBar) findViewById(R.id.seekSpeed);
        seekSpeed.setProgress(speedWpm-5);

        seekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speedWpm = progress + 5;
                updateText();
                savePrefs();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

//        final LinearLayout layout = tv; //(LinearLayout) findViewById(R.id.layout);
//        final ViewTreeObserver observer= /*layout*/tv.getViewTreeObserver();

        // Button

        final View vv =  findViewById(R.id.buttonCw);
        final ViewTreeObserver observer= vv.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        //Log.d("Log", "Height: " + /*layout*/tv.getHeight());
                        viewHeight = vv.getHeight();
                        viewWidth = vv.getWidth();
                    }
                });
        vv.setOnTouchListener(this);
        updateText();

        // Init tone generators

//        int streamType = AudioManager.STREAM_DTMF; // good
        int streamType = AudioManager.STREAM_MUSIC; // best
//        int streamType = AudioManager.STREAM_ALARM; // too bad
//        int streamType = AudioManager.STREAM_VOICE_CALL;
        //int streamType = AudioManager.STREAM_NOTIFICATION; // bad

        toneGenerator1 = new ToneGenerator(streamType, ToneGenerator.MAX_VOLUME);
        toneGenerator2 = new ToneGenerator(streamType, ToneGenerator.MAX_VOLUME/10);

        secondSoundOn();

        // Start CW thread

        cwThread = new CwThread();

        cwThread.setPriority(Thread.MAX_PRIORITY);
        cwThread.setDaemon(true);
        cwThread.start();
        Log.d("FlatCW", "Priority (loop thread): " + cwThread.getPriority());

        // Set highest priority of the main thread

 //       Looper.getMainLooper().getThread().setPriority(Thread.MAX_PRIORITY);

  //      Log.d("FlatCW", "Priority (main thread): " + Looper.getMainLooper().getThread().getPriority());
    }

    @Override
    protected void onDestroy() {
//        cancelTimer();
        soundOff();
        if(cwThread != null) {
            cwThread.interrupt();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_items, menu);

        MenuItem item = menu.findItem(R.id.item_rev);
        item.setChecked(reversePaddles);

        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
  //      TextView displayTextView = (TextView) findViewById(R.id.displayText);

        switch (item.getItemId()) {

            case R.id.item_rev:

                reversePaddles = !reversePaddles;
                item.setChecked(reversePaddles);
                updateText();
                savePrefs();

                return true;

            case R.id.item_set20:

                speedWpm = INIT_WPM_20;
                final SeekBar seekSpeed = (SeekBar) findViewById(R.id.seekSpeed);
                seekSpeed.setProgress(speedWpm-5);
                updateText();
                savePrefs();

                return true;


            default:

                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onSaveInstanceState(Bundle mySavedInstanceState ) {

        super.onSaveInstanceState( mySavedInstanceState );

        mySavedInstanceState.putBoolean( "REV_PADDLES", reversePaddles );
        mySavedInstanceState.putInt("SPEED_WPM", speedWpm);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // событие
        int actionMask = event.getActionMasked();
        // индекс касания
        int pointerIndex = event.getActionIndex();
        // число касаний
        int pointerCount = event.getPointerCount();
        float x = event.getX(pointerIndex);

        boolean changedDitDah = false;

        switch (actionMask) {
            case MotionEvent.ACTION_DOWN:


            case MotionEvent.ACTION_POINTER_DOWN:
                inTouch = true;


                downPI = pointerIndex;

                if ( (x < viewWidth/2) ^ reversePaddles) {
                    if(!isDitPressed()) {
                        touchIdDit = pointerIndex;
                        memDit = true;
                        changedDitDah = true;
                    }
                }
                else {
                    if(!isDahPressed()) {
                        touchIdDah = pointerIndex;
                        memDah = true;
                        changedDitDah = true;
                    }
                }

                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
//                sb.setLength(0);

                touchIdDit = -1;
                touchIdDah = -1;
                changedDitDah = true;

            case MotionEvent.ACTION_POINTER_UP:
                upPI = pointerIndex;

                inTouch = false;

                if (pointerIndex == touchIdDit)
                {
                    touchIdDit = -1;
                    changedDitDah = true;
                }
                else
                if (pointerIndex == touchIdDah)
                {
                    touchIdDah = -1;
                    changedDitDah = true;
                }


                break;

            case MotionEvent.ACTION_MOVE: // движение

                sb.setLength(0);

                for (int i = 0; i < 10; i++) {
                    sb.append("Index = " + i);
                    if (i < pointerCount) {
                        sb.append(", ID = " + event.getPointerId(i));
                        sb.append(", X = " + event.getX(i));
                        sb.append(", Y = " + event.getY(i));
                    } else {
                        sb.append(", ID = ");
                        sb.append(", X = ");
                        sb.append(", Y = ");
                    }
                    sb.append("\r\n");
                }
                break;
        }
        result = "down: " + downPI + "\n" + "up: " + upPI + "\n";

        result += "pointerCount = " + pointerCount + "\n" + sb.toString();



        if (inTouch && (!inTouchOld)) {
            //result += "pointerCount = " + pointerCount + "\n" + sb.toString();
            //result = "*";

//            if (event.getX() < viewWidth/2)
//                result = ".";
//            else
//                result = "-";

//            long startTime = System.currentTimeMillis();
//            toneGenerator1.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE);
            //          Log.d(TAG, "startTone: " + (System.currentTimeMillis() - startTime));

////            playSound();
//            audioTrack.play();

//            rtone.play();

//            sound.play(MediaActionSound.START_VIDEO_RECORDING);

            inTouchOld = true;
        }
        else if ((!inTouch) && inTouchOld) {

//            result = "";
//            toneGenerator1.stopTone();
            inTouchOld = false;
//            audioTrack.stop();

        }

/*
        // Start sending CW
        if ( (cwState == CWSTATES.CW_NONE) &&
                (isDitPressed() || isDahPressed()) ) {
            if (isDitPressed())
                cwState = CWSTATES.CW_SENDING_DIT;
            else
            if (isDahPressed())
                cwState = CWSTATES.CW_SENDING_DAH;

            soundOn();
//            startTimer();
        }
*/


        if (changedDitDah) {
            result = "";
            if (isDitPressed())
                result += ".";
            if (isDahPressed())
                result += "-";

//            if (( touchIdDit!=-1) || (touchIdDah!=-1)) {
//                toneGenerator1.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE);
//            } else
//                toneGenerator1.stopTone();

//            tv.setText(result);
        }

//        tv.setText(result);
        return true;
    }

    void updateText()
    {
        String s;

        s = speedWpm + " WPM\n\n";

        if (reversePaddles)
            s += "<--- Dah   Dit ---> \n";
        else
            s += "<--- Dit   Dah ---> \n";

        final Button buttonCw = (Button) findViewById(R.id.buttonCw);
        buttonCw.setText(s);
    }
}

