package com.example.k2.d2.k2d2pf;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static com.example.k2.d2.k2d2pf.PF.*;


public class FullscreenActivity extends AppCompatActivity implements View.OnClickListener {

    private Button up, left, right, down, reset;

    public static TextView motion_detail;

    private Canvas canvas;

    private List<ShapeDrawable> walls;

    public int width=0,height = 0;
    private int stepsize =80;

    int number=2000;
    PF pf;
    public List<PF> Particals=new ArrayList<>();


    /*******************************************************/

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.canvas);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.

        /*******************set the buttons**********************/

        motion_detail = findViewById(R.id.textView1);
        motion_detail.setMovementMethod(new ScrollingMovementMethod());

        up = findViewById(R.id.button1);
        up.setOnClickListener(this);

        left = findViewById(R.id.button2);
        left.setOnClickListener(this);

        right = findViewById(R.id.button3);
        right.setOnClickListener(this);

        down = findViewById(R.id.button4);
        down.setOnClickListener(this);

        reset = findViewById(R.id.reset);
        reset.setOnClickListener(this);

        /*****************get the screen dimensions********************/

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        height = size.x;
        width= size.y;

        ImageView canvasView = findViewById(R.id.canvas);

        Bitmap blankBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(blankBitmap);
        canvasView.setImageBitmap(blankBitmap);

        walls = Walls.build_walls(width,height);

        /* create Particals */
        for (int i=0; i<number; i++){
            pf= new PF(width/10, height/5,1,Color.BLACK,  new ShapeDrawable(new OvalShape()),10);
            Particals.add(pf);
        }
        /* Initial Placement*/
        Particals=InitPF(width,height, Particals);

        canvas.drawColor(Color.WHITE);
        for(ShapeDrawable wall : walls)
            wall.draw(canvas);
        drawing(canvas,Particals);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);

    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // UP BUTTON
            case R.id.button1: {
//                Toast.makeText(getApplication(), "UP", Toast.LENGTH_SHORT).show();
                fp_movement(width,height,"up",Particals, stepsize);
//                motion_detail.setText(motion_detail.getText() + "\nuser=" + r.left + "," + r.top + "," + r.right + "," + r.bottom);
                break;
            }
            // DOWN BUTTON
            case R.id.button4: {
//                Toast.makeText(getApplication(), "DOWN", Toast.LENGTH_SHORT).show();
                fp_movement(width,height,"down",Particals, stepsize);
//                motion_detail.setText("\n\tMove Down" + "\n\tTop Margin = "
//                        + user.getBounds().top);
                break;
            }
            // LEFT BUTTON
            case R.id.button2: {
//                Toast.makeText(getApplication(), "LEFT", Toast.LENGTH_SHORT).show();

                fp_movement(width,height,"left",Particals, stepsize);
//                motion_detail.setText("\n\tMove Left" + "\n\tLeft Margin = "
//                        + user.getBounds().left);
                break;
            }
            // RIGHT BUTTON
            case R.id.button3: {
//                Toast.makeText(getApplication(), "RIGHT", Toast.LENGTH_SHORT).show();
                fp_movement(width,height,"right",Particals, stepsize);
//                motion_detail.setText("\n\tMove Right" + "\n\tLeft Margin = "
//                        + user.getBounds().left);
                break;
            }
            case R.id.reset: {

                Particals=InitPF(width,height, Particals);
                break;
            }
        }

        canvas.drawColor(Color.WHITE);
        for(ShapeDrawable wall : walls)
            wall.draw(canvas);

        drawing(canvas,Particals);

    }
}
