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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import androidx.annotation.NonNull;

public class AccelerometerDataService extends WearableListenerService {
    private static final String ACCELERATION = "/ACCELERATION";
    private static final String GYROSCOPE = "/GYROSCOOP";
    private static final String HEARTRATE = "/HEARTRATE";
    private String ACCELERATION_STOP = "/ACCELERATION_STOP";
    private OutputStreamWriter writerAcc;
    private OutputStreamWriter writerGyr;
    private OutputStreamWriter writerHeart;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Log.i("lala", getExternalFilesDir(null).getAbsolutePath());
            File accelero = new File(getExternalFilesDir(null),"acceleroData" + ".csv"); //"data/data/ugent.waves.healthrecommendersystems/
            File gyroscoop = new File(getExternalFilesDir(null),"gyroscoopData" + ".csv");
            File heartrate = new File(getExternalFilesDir(null),"heartrateData" + ".csv");
            accelero.createNewFile();
            gyroscoop.createNewFile();
            heartrate.createNewFile();
            FileOutputStream facc = new FileOutputStream(accelero);
            FileOutputStream fgyr = new FileOutputStream(gyroscoop);
            FileOutputStream fheart = new FileOutputStream(heartrate);
            writerAcc =new OutputStreamWriter(facc);
            writerGyr =new OutputStreamWriter(fgyr);
            writerHeart =new OutputStreamWriter(fheart);
            writerAcc.write(String.format(Locale.ENGLISH,"time; x; y; z\n"));
            writerGyr.write(String.format(Locale.ENGLISH,"time; x; y; z\n"));
            writerHeart.write(String.format(Locale.ENGLISH,"time; hr\n"));
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
                writerAcc.write(String.format(Locale.ENGLISH,"%s; %f; %f; %f\n", (long)dst[0], dst[1], dst[2], dst[3]));
            } catch (IOException e){
                e.printStackTrace();
            }
        }else if(messageEvent.getPath().equalsIgnoreCase(HEARTRATE)){
            try{
                FloatBuffer values = ByteBuffer.wrap(messageEvent.getData()).asFloatBuffer();
                final float[] dst = new float[values.capacity()];
                values.get(dst);
                writerHeart.write(String.format(Locale.ENGLISH,"%s; %f\n", (long)dst[0], dst[1]));
            } catch (IOException e){
                e.printStackTrace();
            }
        } else if( messageEvent.getPath().equalsIgnoreCase(GYROSCOPE) ) {
            try {
                FloatBuffer values = ByteBuffer.wrap(messageEvent.getData()).asFloatBuffer();
                final float[] dst = new float[values.capacity()];
                values.get(dst);
                writerGyr.write(String.format(Locale.ENGLISH, "%s; %f; %f; %f\n", (long) dst[0], dst[1], dst[2], dst[3]));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(messageEvent.getPath().equalsIgnoreCase(ACCELERATION_STOP)){
            try {
                writerAcc.close();
                writerGyr.close();
                writerHeart.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
