package ugent.waves.healthrecommendersystems.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Locale;

import ugent.waves.healthrecommendersystems.persistance.AppDatabase;

public class SensorBackgroundService extends Service implements SensorEventListener {

    protected static final String TAG = "SensorBackgroundService";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    OutputStreamWriter writer;

    private AppDatabase db;

    public SensorBackgroundService(){
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            File myFile = new File("data/data/ugent.waves.healthrecommendersystems/sensors_" + System.currentTimeMillis() + ".csv");
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            writer =new OutputStreamWriter(fOut);
            writer.write(String.format(Locale.ENGLISH,"time; x; y; z\n"));
            //writer = new FileWriter(new File("sensors_" + System.currentTimeMillis() + ".csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        db = AppDatabase.getInstance(this);
    }


    @Override
    public void onDestroy(){
        sensorManager.unregisterListener(this);
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        } else {
            // fail! we dont have an accelerometer!
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    @Override
    public void onSensorChanged(final SensorEvent evt) {
        //Log.d(TAG, "ACCELEROMETER "+ event.values[0] + " " + event.values[1] + " " + event.values[2]);

        try{
            writer.write(String.format(Locale.ENGLISH,"%d; %f; %f; %f\n", evt.timestamp, evt.values[0], evt.values[1], evt.values[2]));
        } catch (IOException e){
            e.printStackTrace();
        }
         //TODO: persist to database
        /*
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                db.accelerometerDataDao().insertAccelerometerData(new accelerometerData(event.values[0], event.values[1], event.values[2]));
            }
        });*/
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
