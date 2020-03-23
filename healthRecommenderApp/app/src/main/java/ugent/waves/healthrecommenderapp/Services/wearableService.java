package ugent.waves.healthrecommenderapp.Services;

import android.os.Environment;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.lang.Object;

import androidx.annotation.NonNull;

//TODO: save sessions to room db??
public class wearableService extends WearableListenerService {
    private static final String ACCELEROMETER_START = "/ACCELEROMETER_START";
    private String ACCELEROMETER = "/ACCELEROMETER";

    private String ACCELEROMETER_STOP = "/ACCELEROMETER_STOP";

    //dict with accelerometer data
    private Map<String, List<Float>> session;
    private int[] output;

    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        //TODO: message ordering niet ok??
        if(messageEvent.getPath().equalsIgnoreCase(ACCELEROMETER_START) ){
            session = new HashMap<>();
            session.put("time", new ArrayList<>());
            session.put("x", new ArrayList<>());
            session.put("y", new ArrayList<>());
            session.put("z", new ArrayList<>());
        } else if( messageEvent.getPath().equalsIgnoreCase(ACCELEROMETER) ){
            FloatBuffer values = ByteBuffer.wrap(messageEvent.getData()).asFloatBuffer();
            final float[] dst = new float[values.capacity()];
            values.get(dst);
            if(session == null){
                session = new HashMap<>();
                session.put("time", new ArrayList<>());
                session.put("x", new ArrayList<>());
                session.put("y", new ArrayList<>());
                session.put("z", new ArrayList<>());
            }
            session.get("time").add(dst[0]);
            session.get("x").add(dst[1]);
            session.get("y").add(dst[2]);
            session.get("z").add(dst[3]);
            //writer.write(String.format(Locale.ENGLISH,"%s; %f; %f; %f\n", (long)dst[0], dst[1], dst[2], dst[3]));
        } else if(messageEvent.getPath().equalsIgnoreCase(ACCELEROMETER_STOP)){
            //TODO: lange delay
            Log.d(ACCELEROMETER_STOP, "xe zijn er");
            File sdcard = getExternalFilesDir(null);
            try (Interpreter interpreter = new Interpreter(new File(sdcard.getAbsolutePath(), "rope_skipping_simple.tflite"))) {
                float[][][] input = segmentation();
                output = new int[input.length];
                //final ByteBuffer buffer = ByteBuffer.allocate(input.size()*input.get(0).size()*input.get(0).get(0).size()*4);
                //buffer.put(input);
                interpreter.run(input, output);
                Log.d("hh", "tlukt");
            } catch(Exception e){
                e.getMessage();
            }
        }
    }

    //TODO
    private void normalization(){
        //for( int i = 0; i < session.get("x").size(); i++){}
    }

    //TODO: sampling mss in realtime schatten?
    private int getSamplingFrequentie(){
        return 51;
    }

    //TODO: reshape zodat individuele samples samenzitten in een array
    private float[][][] segmentation(){
        int N_FEATURES = 3;
        int FRAME_SIZE = getSamplingFrequentie() * 1;
        int HOP_SIZE = FRAME_SIZE/2;

        List<float[][]> frames = new ArrayList<>();

        for( int i = 0; i < session.get("x").size() - FRAME_SIZE; i += HOP_SIZE){
            float[] x = Floattofloat(session.get("x").subList(i, i+FRAME_SIZE));
            float[] y = Floattofloat(session.get("y").subList(i, i+FRAME_SIZE));
            float[] z = Floattofloat(session.get("z").subList(i, i+FRAME_SIZE));

            float[][] segment = new float[3][FRAME_SIZE];
            //ByteBuffer segment = ByteBuffer.allocate(3*x.position());
            segment[0] = x;
            segment[1] = y;
            segment[2] = z;

            frames.add(segment);
        }

        //frames = np.asarray(frames).reshape(-1, frame_size, N_FEATURES)

        return floatListTofloat3D(frames);
    }

    private byte[] toPrimitives(Byte[] oBytes)
    {

        byte[] bytes = new byte[oBytes.length];
        for(int i = 0; i < oBytes.length; i++){
            bytes[i] = oBytes[i];
        }
        return bytes;

    }

    private ByteBuffer convertToBytebuffer(List<ByteBuffer> src)
    {
        int size=src.size()*3*session.get("x").size()*4;

        ByteBuffer newBuffer = ByteBuffer.allocate(size);

        for(int i = 0 ; i < src.size() ; i++) {
            newBuffer.put(src.get(i));
        }
        return newBuffer;
    }

    private float[] Floattofloat(List<Float> l){
        float[] floatArray = new float[l.size()];
        int i = 0;

        for (Float f : l) {
            floatArray[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
        }
        return floatArray;
    }

    private float[][][] floatListTofloat3D(List<float[][]> l){
        float[][][] floatArray = new float[l.size()][l.get(0).length][l.get(0)[0].length];
        int i = 0;

        for (float[][] f : l) {
            floatArray[i++] = f;
        }
        return floatArray;
    }
}
