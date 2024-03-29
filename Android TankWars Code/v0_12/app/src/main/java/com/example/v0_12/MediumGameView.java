package com.example.v0_12;

import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.hardware.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

public class MediumGameView extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener {
    private Bitmap oPlayerTankUp;
    private Bitmap oPlayerTankRight;
    private Bitmap oPlayerTankDown;
    private Bitmap oPlayerTankLeft;
    private Bitmap oEasyEnemyUp;
    private Bitmap oEasyEnemyRight;
    private Bitmap oEasyEnemyDown;
    private Bitmap oEasyEnemyLeft;
    private Bitmap oBullet;
    private Bitmap oObstacle;
    private Resources oResources;
    private Canvas oCanvas;
    private Paint oPaint;

    private volatile MediumGameThread thread;

    //private SensorEventListener sensorAccelerometer;

    //Handle communication from the GameThread to the View/Activity Thread
    private Handler mHandler;

    //Pointers to the views
    private TextView mScoreView;
    private TextView mStatusView;

    Sensor accelerometer;
    Sensor magnetometer;

    public MediumGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        handler();
    }

    private void handler() {
        //Set up a handler for messages from GameThread
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message m) {
                if(m.getData().getBoolean("score")) {
                    mScoreView.setText(m.getData().getString("text"));
                }
                else {
                    //So it is a status
                    int i = m.getData().getInt("viz");
                    switch(i) {
                        case View.VISIBLE:
                            mStatusView.setVisibility(View.VISIBLE);
                            break;
                        case View.INVISIBLE:
                            mStatusView.setVisibility(View.INVISIBLE);
                            break;
                        case View.GONE:
                            mStatusView.setVisibility(View.GONE);
                            break;
                    }
                    mStatusView.setText(m.getData().getString("text"));
                }
            }
        };
    }

    private void initialiseBitmaps() {
        oResources = getResources();
        //Store each image resource into a variable
        oPlayerTankUp = BitmapFactory.decodeResource(oResources, R.drawable.orangetank_up);
        oPlayerTankRight = BitmapFactory.decodeResource(oResources, R.drawable.orangetank_right);
        oPlayerTankDown = BitmapFactory.decodeResource(oResources, R.drawable.smallorangetank_down);
        oPlayerTankLeft = BitmapFactory.decodeResource(oResources, R.drawable.orangetank_left);
        oEasyEnemyUp = BitmapFactory.decodeResource(oResources, R.drawable.greentank_up);
        oEasyEnemyRight = BitmapFactory.decodeResource(oResources, R.drawable.greentank_right);
        oEasyEnemyDown = BitmapFactory.decodeResource(oResources, R.drawable.greentank_down);
        oEasyEnemyLeft = BitmapFactory.decodeResource(oResources, R.drawable.greentank_left);
        oBullet =  BitmapFactory.decodeResource(oResources, R.drawable.smallorangetank_down);
        oObstacle =  BitmapFactory.decodeResource(oResources, R.drawable.sad_ball);
    }

    /*private void draw() {
        if (getHolder().getSurface().isValid()) {
            oCanvas = getHolder().lockCanvas();
            oCanvas.drawBitmap(oPlayerTankDown, 50, 50, oPaint);
        }
    }*/

    //Used to release any resources.
    public void cleanup() {
        this.thread.setRunning(false);
        this.thread.cleanup();

        this.removeCallbacks(thread);
        thread = null;

        this.setOnTouchListener(null);

        SurfaceHolder holder = getHolder();
        holder.removeCallback(this);
    }

    /*
     * Setters and Getters
     */

    public void setThread(MediumGameThread newThread) {

        thread = newThread;

        setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                return thread != null && thread.onTouch(event);
            }
        });

        setClickable(true);
        setFocusable(true);
    }

    public MediumGameThread getThread() {
        return thread;
    }

    public TextView getStatusView() {
        return mStatusView;
    }

    public void setStatusView(TextView mStatusView) {
        this.mStatusView = mStatusView;
    }

    public TextView getScoreView() {
        return mScoreView;
    }

    public void setScoreView(TextView mScoreView) {
        this.mScoreView = mScoreView;
    }


    public Handler getmHandler() {
        return mHandler;
    }

    public void setmHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }


    /*
     * Screen functions
     */

    //ensure that we go into pause state if we go out of focus
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if(thread!=null) {
            if (!hasWindowFocus)
                thread.pause();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if(thread!=null) {
            thread.setRunning(true);

            if(thread.getState() == Thread.State.NEW){
                //Just start the new thread
                thread.start();
            }
            else {
                if(thread.getState() == Thread.State.TERMINATED){
                    //Start a new thread
                    //Should be this to update screen with old game: new GameThread(this, thread);
                    //The method should set all fields in new thread to the value of old thread's fields
                    //thread = new MediumLvlGame(this, null);
                    thread = new MediumLvlGame(this);
                    thread.setRunning(true);
                    thread.start();
                }
            }
        }
    }

    //Always called once after surfaceCreated. Tell the GameThread the actual size
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(thread!=null) {
            thread.setSurfaceSize(width, height);
        }
    }

    /*
     * Need to stop the GameThread if the surface is destroyed
     * Remember this doesn't need to happen when app is paused on even stopped.
     */
    public void surfaceDestroyed(SurfaceHolder arg0) {

        boolean retry = true;
        if(thread!=null) {
            thread.setRunning(false);
        }

        //join the thread with this thread
        while (retry) {
            try {
                if(thread!=null) {
                    thread.join();
                }
                retry = false;
            }
            catch (InterruptedException e) {
                //naugthy, ought to do something...
            }
        }
    }

    /*
     * Accelerometer
     */

    public void startSensor(SensorManager sm) {

        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);

    }

    public void removeSensor(SensorManager sm) {
        sm.unregisterListener(this);

        accelerometer = null;
        magnetometer = null;
    }

    //A sensor has changed, let the thread take care of it
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(thread!=null) {
            thread.onSensorChanged(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}