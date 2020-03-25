package ugent.waves.healthrecommenderapp.Services;

import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.view.animation.ScaleAnimation;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.lang.Object;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

/*
1. accelerometer data in map "time" -> [], "x" -> []...
2. segmentation: shape = (segments, samples, 3, 1)
3. output = int per segment
4. group output per activity in transitions (startIndex -> activity)
5. start + end time of activities ( "start" -> [], "end" -> [], "activity"->[] )
6. post to firebase: users -> testuser -> sessions -> activities
 */
//TODO: calculate turns + mistakes
//TODO: get heartrate data
//TODO: calculate met_points
public class wearableService extends WearableListenerService {
    //TODO: save sessions to room db??
    private static final String ACCELEROMETER_START = "/ACCELEROMETER_START";
    private String ACCELEROMETER = "/ACCELEROMETER";

    private String ACCELEROMETER_STOP = "/ACCELEROMETER_STOP";

    //dict with accelerometer data
    private Map<String, List<Float>> session;
    private float[] output;
    //beginindex uit output array gemapt op waarde
    private Map<Integer,Float> trantitions;
    private healthRecommenderApplication app;
    private FirebaseFirestore firestore;

    //TODO: oncreate called maar onmessagereceived niet meer
    /*
    public void onCreate(){
        super.onCreate();
        app = (healthRecommenderApplication) this.getApplicationContext();
        firestore = app.getFirestore();
    }*/
    
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        app = (healthRecommenderApplication) this.getApplicationContext();
        firestore = app.getFirestore();
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
        } else if(messageEvent.getPath().equalsIgnoreCase(ACCELEROMETER_STOP)){
            //TODO: lange delay
            Log.d(ACCELEROMETER_STOP, "xe zijn er");
            File sdcard = getExternalFilesDir(null);
            try (Interpreter interpreter = new Interpreter(new File(sdcard.getAbsolutePath(), "rope_skipping_simple.tflite"))) {
                float[][][][] input = segmentation();
                output = new float[input.length];
                //final ByteBuffer buffer = ByteBuffer.allocate(input.size()*input.get(0).size()*input.get(0).get(0).size()*4);
                //buffer.put(input);
                interpreter.run(input, output);
                makeActivities();
                Map<String, List<Float>> t = get_trantitions();
                Log.d("hh", "tlukt");
            } catch(Exception e){
                makeActivities();
                Map<String, List<Float>> t = get_trantitions();
                for(int i = 0; i < t.get("start").size(); i++){
                    Map<String, String> d = new HashMap<>();
                    double start = floatToTimeDouble(t.get("start").get(i));
                    double end = floatToTimeDouble(t.get("end").get(i));
                    d.put("start", start+"");
                    d.put("end", end+"");
                    d.put("activity", t.get("activity").get(i).toString());
                    firestore.collection("users")
                            .document("testUser")
                            .collection("sessions")
                            .document(UUID.randomUUID().toString())
                            .collection("activities")
                            .add(d)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d("dd", "DocumentSnapshot added with ID: " + documentReference.getId());
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w("dd", "Error adding document", e);
                                }
                            });
                }
                Log.d("ddd",e.getMessage());
            }
        }
    }

    //TODO: wrong date
    private double floatToTimeDouble(Float time) {
        Calendar cal = Calendar.getInstance();
        Date date_start = new Date((long) time.floatValue());
        cal.setTime(date_start);
        int h = cal.get(Calendar.HOUR);
        int m = cal.get(Calendar.MINUTE);
        int s = cal.get(Calendar.SECOND);

        double total = h + (m/60) + (s/3600);
        return total;
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
    //TODO: hopsize
    private float[][][][] segmentation(){
        int N_FEATURES = 3;
        int FRAME_SIZE = getSamplingFrequentie() * 1;
        int HOP_SIZE = 1;

        List<float[][][]> frames = new ArrayList<>();

        for( int i = 0; i < session.get("x").size() - FRAME_SIZE; i += HOP_SIZE){
            float[][] x = float1DTo2D(Floattofloat(session.get("x").subList(i, i+FRAME_SIZE)));
            float[][] y = float1DTo2D(Floattofloat(session.get("y").subList(i, i+FRAME_SIZE)));
            float[][] z = float1DTo2D(Floattofloat(session.get("z").subList(i, i+FRAME_SIZE)));

            float[][][] segment = new float[3][FRAME_SIZE][1];
            //ByteBuffer segment = ByteBuffer.allocate(3*x.position());
            segment[0] = x;
            segment[1] = y;
            segment[2] = z;

            frames.add(segment);
        }

        //frames = np.asarray(frames).reshape(-1, frame_size, N_FEATURES)

        return floatListTofloat4D(frames);
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

    private float[][][][] floatListTofloat4D(List<float[][][]> l){
        float[][][][] floatArray = new float[l.size()][l.get(0).length][l.get(0)[0].length][1];
        int i = 0;

        for (float[][][] f : l) {
            floatArray[i++] = f;
        }
        return floatArray;
    }

    private float[][] float1DTo2D(float[] l){
        float[][] floatArray = new float[l.length][1];
        int i=0;
        for(float f : l){
            float[] ff = new float[1];
            ff[0] = f;
            floatArray[i++] = ff;
        }
        return floatArray;
    }

    //TODO: met overlap omgaan
    private Map<String, List<Float>> get_trantitions() {
        int FRAME_SIZE = getSamplingFrequentie() * 1;
        int HOP_SIZE = 1;
        //start, end, activiteit
        Map<String, List<Float>> trantitions_ = new HashMap<>();
        trantitions_.put("start", new ArrayList<>());
        trantitions_.put("end", new ArrayList<>());
        trantitions_.put("activity", new ArrayList<>());

        List<Integer> indexes=new ArrayList(trantitions.keySet());
        Collections.sort(indexes);

        float end, start;
        if(indexes.size() == 1){
            int numberSensorSamples = (output.length)*FRAME_SIZE;
            start = session.get("time").get(0);
            end = session.get("time").get(1); //TODO: remove dummydata (make number samples correct)
            trantitions_.get("start").add(start);
            trantitions_.get("end").add(end);
            trantitions_.get("activity").add(trantitions.get(0));
        } else{
            int indexTime = 0;
            for (int i = 0; i<indexes.size()-1; i++) {
                int numberSensorSamples = (indexes.get(i + 1) - indexes.get(i))*FRAME_SIZE;
                start = session.get("time").get(indexTime);
                end = session.get("time").get(indexTime+numberSensorSamples);
                trantitions_.get("start").add(start);
                trantitions_.get("end").add(end);
                trantitions_.get("activity").add(trantitions.get(i));

                indexTime = indexTime+numberSensorSamples;
            }

        }
        return trantitions_;
    }

    //map start index uit output array op de activity die daar begint
    //OUTPUT: index1 -> activiteit1, index2 -> activiteit2....
    private void makeActivities(){
        trantitions = new HashMap<>();
        float activity = output[0];
        int start = 0;
        for (int i = 1; i < output.length-1; i++) {
            if(output[i] != output[i-1]){
                trantitions.put(start, output[start]);
                start = i;
            }
        }
        if(trantitions.size() == 0){
            trantitions.put(start, output[start]);
        }
    }
}
