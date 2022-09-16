package com.example.surfacedetector;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final String START = "START";
    private static final String STOP = "STOP";
    private static final String BEGIN_TEXT = "TAP TO START!";
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor gyroscopeSensor;
    private GraphView graph;
    private TextView mtext;
    private Button mButton;
    private ArrayList<Float> slopeIndex;
    private ArrayList<Float> slopeData;
    private float xSpeed;
    private float acceleration, zAcceleration;
    private float xDistance;
    private float angle;
    private float lastGyrY;
    public long timestamp;
    public long gyroscopeTimestamp;


    @Override
    protected void onCreate(Bundle savedInstaceState) {
        super.onCreate(savedInstaceState);
        setContentView(R.layout.activity_main);
        mtext = findViewById(R.id.text);
        mButton = findViewById(R.id.button);
        graph = findViewById(R.id.graph);
        slopeData = new ArrayList<>();
        slopeIndex = new ArrayList<>();
        configGraphAxis();

        mButton.setOnClickListener(v -> {
            if (mButton.getText().equals(START)) {
                initialize();
            } else {
                reset();
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.accuracy != 3)
            return;

        if (event.sensor == accelerometerSensor) {
            setSpeed(event);
            sensorManager.registerListener(MainActivity.this, gyroscopeSensor, (int) (100 / Math.abs(xSpeed)));
        } else {
            setAngle(event);
            graph.removeAllSeries();
            showData();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void setAngle(SensorEvent event) {
        float dT = (event.timestamp - gyroscopeTimestamp) * NS2S;
        float axisSpeedY = event.values[1];

        gyroscopeTimestamp = event.timestamp;
        if (Math.abs(zAcceleration) > 0.05 && Math.abs(axisSpeedY) > 0.08) {
            angle += axisSpeedY * dT;
            lastGyrY -= Math.sin(angle) * 2.5f;
        }
        slopeIndex.add(xDistance);
        slopeData.add(lastGyrY);
    }

    private float getCoefficient() {
        float absXSpeed = Math.abs(xSpeed);
        if (absXSpeed < 0.01f) {
            return 5f;
        } else if (absXSpeed < 0.02f) {
            return 3.5f;
        } else if (absXSpeed < 0.03f) {
            return 2f;
        }
        return 1.2f;
    }

    private void setSpeed(SensorEvent event) {
        zAcceleration = event.values[2];

        float deltaAcceleration = Math.abs(acceleration - event.values[0]);

        if (deltaAcceleration > 0.15 && Math.abs(event.values[0]) > 0.05) {
            float dT = (event.timestamp - timestamp) * NS2S;
            float coefficient = getCoefficient();

            acceleration = event.values[0];

            float distance = Math.abs(0.5f * acceleration * dT * dT + xSpeed * dT) * coefficient;
            xDistance += Math.abs(distance * Math.cos(angle));
            mtext.setText(Float.toString(xDistance));

            xSpeed += acceleration * dT;
        }
        timestamp = event.timestamp;
    }

    private void initialize() {
        xSpeed = 0;
        acceleration = zAcceleration = 0;
        xDistance = 0;
        lastGyrY = 0;
        angle = 0;
        timestamp = SystemClock.elapsedRealtimeNanos();
        gyroscopeTimestamp = SystemClock.elapsedRealtimeNanos();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(MainActivity.this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(MainActivity.this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);

        mButton.setText(STOP);
    }

    private void reset() {
        sensorManager.unregisterListener(MainActivity.this, accelerometerSensor);
        sensorManager.unregisterListener(MainActivity.this, gyroscopeSensor);

        slopeData.clear();
        slopeIndex.clear();

        mButton.setText(START);
        mtext.setText(BEGIN_TEXT);
    }

    private void showData() {
        DataPoint[] dataPoints = new DataPoint[slopeData.size()];
        for (Integer i = 0; i < slopeData.size(); i++) {
            dataPoints[i] = new DataPoint(slopeIndex.get(i) * 100, slopeData.get(i));
        }
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
        graph.removeAllSeries();
        graph.setTitle("Scan Result");
        graph.setTitleColor(R.color.purple_200);
        graph.setTitleTextSize(40);
        graph.addSeries(series);
    }

    private void configGraphAxis() {
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(100);
        graph.getViewport().setMinY(-25);
        graph.getViewport().setMaxY(25);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setScalable(true);
    }
}
