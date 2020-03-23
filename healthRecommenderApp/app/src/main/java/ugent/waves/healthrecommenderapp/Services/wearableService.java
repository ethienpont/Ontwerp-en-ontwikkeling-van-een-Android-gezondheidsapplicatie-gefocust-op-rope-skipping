package ugent.waves.healthrecommenderapp.Services;

import android.os.Environment;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.tensorflow.lite.Interpreter;

import java.io.File;

import androidx.annotation.NonNull;

public class wearableService extends WearableListenerService {
    private String ACCELEROMETER = "/ACCELEROMETER";

    private String ACCELEROMETER_STOP = "/ACCELEROMETER_STOP";

    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if( messageEvent.getPath().equalsIgnoreCase(ACCELEROMETER) ){
            Log.d(ACCELEROMETER, "xe zijn er");
            File sdcard = Environment.getExternalStorageDirectory();
            try (Interpreter interpreter = new Interpreter(new File(sdcard, "rope_skipping.tflite"))) {
                //interpreter.run(input, output);
            } catch(Exception e){
                e.getMessage();
            }
        } else if(messageEvent.getPath().equalsIgnoreCase(ACCELEROMETER_STOP)){
            Log.d(ACCELEROMETER, "xe zijn er");
        }
    }
}
