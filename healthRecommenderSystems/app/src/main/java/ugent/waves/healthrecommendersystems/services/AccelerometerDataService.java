package ugent.waves.healthrecommendersystems.services;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

public class AccelerometerDataService extends WearableListenerService {
    private static final String ACCELERATION = "/ACCELERATION";
    private String ACCELERATION_STOP = "/ACCELERATION_STOP";
    private OutputStreamWriter writer;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Log.i("lala", getExternalFilesDir(null).getAbsolutePath());
            File myFile = new File(getExternalFilesDir(null),"sensors_" + System.currentTimeMillis() + ".csv"); //"data/data/ugent.waves.healthrecommendersystems/
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            writer =new OutputStreamWriter(fOut);
            writer.write(String.format(Locale.ENGLISH,"time; x; y; z\n"));
            //writer = new FileWriter(new File("sensors_" + System.currentTimeMillis() + ".csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if( messageEvent.getPath().equalsIgnoreCase(ACCELERATION) ){
            try{
                FloatBuffer values = ByteBuffer.wrap(messageEvent.getData()).asFloatBuffer();
                final float[] dst = new float[values.capacity()];
                values.get(dst);
                writer.write(String.format(Locale.ENGLISH,"%s; %f; %f; %f\n", new Date((long)dst[0]), dst[1], dst[2], dst[3]));
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        if(messageEvent.getPath().equalsIgnoreCase(ACCELERATION_STOP)){
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
