package com.example.k2.d2.k2d2pf;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static com.example.k2.d2.k2d2pf.PF.InitPF;
import static com.example.k2.d2.k2d2pf.PF.convergence;
import static com.example.k2.d2.k2d2pf.PF.drawing;
import static com.example.k2.d2.k2d2pf.PF.fp_movement;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private Button up, left, right, down, reset, calibration;
    private List<ShapeDrawable> walls, cells;

    public static TextView motion_detail, sensor_detail;
    private Canvas canvas;
    public int width=1080,height = 378;
    int number=2000;
    public List<PF> Particles =new ArrayList<>();

    private float aX,aY,aZ,aZBias =0;
    private boolean steps;
    private int step;
    public String direction;
    private float azimuth, offset ;
    private SensorManager sensorManager;
    private Sensor accelerometer,mRotationSensor;
    public boolean activityRunning, calibration_done ;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ImageView canvasView = findViewById(R.id.canvas);

        Bitmap blankBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(blankBitmap);
        canvasView.setImageBitmap(blankBitmap);
//        width=canvasView.getDrawable().getIntrinsicWidth();
//        height=canvas.getHeight();

        motion_detail = findViewById(R.id.textView1);
        motion_detail.setMovementMethod(new ScrollingMovementMethod());

        sensor_detail = findViewById(R.id.sensor);
        sensor_detail.setMovementMethod(new ScrollingMovementMethod());

        setup_buttons();
        Initialize_Sensors();

        walls = Walls.build_walls(width,height);

        for (int i = 0; i < number; i++) {
            PF pf = new PF(width / 10, height / 5, 1, Color.BLACK, new ShapeDrawable(new OvalShape()), 3);
            Particles.add(pf);
        }
        /* Initial Placement*/
        Particles =InitPF(width,height, Particles);

        canvas.drawColor(Color.WHITE);
        for(ShapeDrawable wall : walls)
            wall.draw(canvas);
;
        drawing(canvas, Particles);
    }

    private void setup_buttons(){

        up = findViewById(R.id.button1);
        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fp_movement("up", Particles, height/20);
            }
        });

        left = findViewById(R.id.button2);
        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fp_movement("left", Particles, 7*width/400);
            }
        });
        right = findViewById(R.id.button3);
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fp_movement("right", Particles, 7*width/400);
            }
        });
        down = findViewById(R.id.button4);
        down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fp_movement("down", Particles, height/20);
            }
        });
        reset = findViewById(R.id.reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Particles =InitPF(width,height, Particles);
                calibration_done=false;
                step=0;
            }
        });
        calibration = findViewById(R.id.calibration);
        calibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                azimuthOffset();
                calibration_done = true;
                aZBias = aZ;
            }
        });
    }
    private void Initialize_Sensors(){ sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // set accelerometer
            accelerometer = sensorManager
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            // register 'this' as a listener that updates values. Each time a sensor value changes,
            // the method 'onSensorChanged()' is called.
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "Hardware compatibility issue", Toast.LENGTH_LONG).show();
        }
        if(sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null){
            mRotationSensor = sensorManager
                    .getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            sensorManager.registerListener(this, mRotationSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }else
        {
            Toast.makeText(this, "Hardware compatibility issue", Toast.LENGTH_LONG).show();
        }

    }


    public void azimuthOffset(){
        offset = azimuth;
    }

    // onResume() registers the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        activityRunning = true;
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Count sensor not available!", Toast.LENGTH_LONG).show();
        }
    }

    // onPause() unregisters the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        activityRunning = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }

    ArrayList<Float> azArray = new ArrayList<>();
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (Sensor.TYPE_ACCELEROMETER == (event.sensor.getType())) {
//            motion_detail.setText("0.0");

            // get the the x,y,z values of the accelerometer
            aX = event.values[0];
            aY = event.values[1];
            aZ = event.values[2];
            azArray.add(aZ);

            if (azArray.size() >=30 && calibration_done){
                for(int i = 0 ; i<azArray.size();i++){
                    if(azArray.get(i) >= aZBias +2){
                        steps = true;

                    }
                }
                azArray.clear();

            }
        } else if (Sensor.TYPE_ROTATION_VECTOR == (event.sensor.getType())) {
            float rotationMatrix[] = new float[16];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, rotationMatrix);
            float rotationValues[] = new float[16];
            SensorManager.getOrientation(rotationMatrix, rotationValues);
            azimuth = (float) Math.toDegrees(rotationValues[0]) + 180;
            if(azimuth - offset < 0){
                azimuth = azimuth - offset + 360;
            }else
                azimuth = azimuth - offset;
        }

        if (azimuth >= 75 && azimuth < 135) {
            direction = "North";
        } else if (azimuth >= 135 && azimuth < 240) {
            direction = "East";
        } else if (azimuth < 330 && azimuth >= 240) {
            direction = "South";
        } else {
            direction = "West";
        }

        if (steps && calibration_done) {
            steps = false;
            step++;

            switch (direction) {
                // UP BUTTON
                case "West": {
                    sensor_detail.setText("up ...loading...");
                    fp_movement("up", Particles, height / 20);
                    break;
                }
                // DOWN BUTTON
                case "East": {
                    sensor_detail.setText("down...loading...");
                    fp_movement("down", Particles, height / 20);
                    break;
                }
                // LEFT BUTTON
                case "South": {
                    sensor_detail.setText("left...loading...");
                    fp_movement( "left", Particles, 7 * width / 400);
                    break;
                }
                // RIGHT BUTTON
                case "North": {
                    sensor_detail.setText("right...loading...");
                    fp_movement( "right", Particles, 7 * width / 400);
                    break;
                }
                default: {
                    //do nothing
                    break;
                }
            }

            // display the current x,y,z accelerometer values


        }
        sensor_detail.setText("Number : " + step + " \nsteps" + steps + "\ndir " + azimuth+"\n aX"+aX +"\n aZ"+aZ + "\nDir" + direction );
        List<PF> kmeans = PF.KMean(Particles);

        kmeans.get(0).color = Color.RED; //k
        kmeans.get(1).color = Color.BLUE; //j
        kmeans.get(2).color = Color.YELLOW; //l

        for (PF pf : kmeans) {
            pf.size = 10;
            pf.setBounds(pf);
        }


        ShapeDrawable cell = new ShapeDrawable(new RectShape());
        cell.getPaint().setColor(Color.rgb(240, 204, 194));
        cell.setBounds(0, 0, 0, 0);

        View v =findViewById(R.id.canvas);
        v.invalidate();

        PF centroid = PF.CheckConvergence(Particles);
        if (convergence) {
            cell = Walls.check_cells(centroid);
        }

        canvas.drawColor(Color.WHITE);
        cell.draw(canvas);
        for (ShapeDrawable wall : walls)
            wall.draw(canvas);

        drawing(canvas, Particles);
        drawing(canvas, kmeans);
        centroid.shapeDrawable.draw(canvas);
    }



}
