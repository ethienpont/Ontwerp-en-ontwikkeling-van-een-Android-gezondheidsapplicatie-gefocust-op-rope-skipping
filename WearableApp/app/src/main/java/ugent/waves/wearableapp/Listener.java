package ugent.waves.wearableapp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

public class Listener implements SensorEventListener {
    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d("e", "e");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
