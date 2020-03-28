package ugent.waves.healthrecommenderapp.Services;

import android.util.Log;

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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import ugent.waves.healthrecommenderapp.Enums.JumpMoves;
import ugent.waves.healthrecommenderapp.HelpClasses.SavGolFilter;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Session;
import ugent.waves.healthrecommenderapp.Persistance.SessionActivity;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

/*
1. accelerometer data in map "time" -> [], "x" -> []...
2. segmentation: shape = (segments, samples, 3, 1)
3. output = int per segment
4. group output per activity in transitions (startIndex -> activity)
5. start + end time of activities ( "start" -> [], "end" -> [], "activity"->[] )
6. post to firebase: users -> testuser -> sessions -> activities

7. heartrate data in map "time" -> [], "HR" -> []
8. calculate met_points
9. post to firebase: users -> testuser -> sessions
 */
//TODO: structurize
public class wearableService extends WearableListenerService {
    //TODO: save sessions to room db??

    private float[] output;

    //"start", "end", "activity"
    private Map<String, List<Float>> trantitions;
    private Map<JumpMoves, SavGolFilter> filters;

    private healthRecommenderApplication app;
    private FirebaseFirestore firestore;


    //MESSAGE PATHS
    private static final String START = "/START";
    private String ACCELEROMETER = "/ACCELEROMETER";
    private String STOP = "/STOP";
    private String HEARTRATE = "/HEARTRATE";

    //HEARTRATE CONSTANTS
    private int veryLightZone = 1;
    private int lightZone = 2;
    private int moderateZone = 3;
    private int hardZone = 4;
    private int maximumZone = 5;

    private static final double MAXHR = 200;

    //RAW DATA STORAGE
    private Map<String, List<Float>> session_heartbeat;
    private Map<String, List<Float>> session_accelerometer;

    //ROOM DB
    private Session s;
    private List<SessionActivity> activities;
    private AppDatabase appDb;


    //TODO: oncreate called maar onmessagereceived niet meer
    /*
    public void onCreate(){
        super.onCreate();
        app = (healthRecommenderApplication) this.getApplicationContext();
        firestore = app.getFirestore();
    }*/
    
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        app = (healthRecommenderApplication) this.getApplicationContext();
        //TODO: initialise weeknr 0 bij eerste opstart
        app.setWeeknr(0);
        firestore = app.getFirestore();
        //TODO: parameters bepalen
        filters = new HashMap<>();
        filters.put(JumpMoves.SLOW, new SavGolFilter(0, 51, 3));
        filters.put(JumpMoves.FAST, new SavGolFilter(0, 33, 5));
        filters.put(JumpMoves.SIDE_SWING, new SavGolFilter(0, 51, 3));
        filters.put(JumpMoves.CROSS_OVER, new SavGolFilter(0, 41, 3));
        filters.put(JumpMoves.FORWARD_180, new SavGolFilter(0, 51, 3));

        s = new Session();
        activities = new ArrayList<>();
        appDb = app.getAppDb();

        //TODO: message ordering niet ok??
        if(messageEvent.getPath().equalsIgnoreCase(START) ){
            session_accelerometer = new HashMap<>();
            session_accelerometer.put("time", new ArrayList<>());
            session_accelerometer.put("x", new ArrayList<>());
            session_accelerometer.put("y", new ArrayList<>());
            session_accelerometer.put("z", new ArrayList<>());

            session_heartbeat = new HashMap<>();
            session_heartbeat.put("time", new ArrayList<>());
            session_heartbeat.put("HR", new ArrayList<>());

        }

        else if( messageEvent.getPath().equalsIgnoreCase(ACCELEROMETER) ){
            FloatBuffer values = ByteBuffer.wrap(messageEvent.getData()).asFloatBuffer();
            final float[] dst = new float[values.capacity()];
            values.get(dst);
            if(session_accelerometer == null){
                session_accelerometer = new HashMap<>();
                session_accelerometer.put("time", new ArrayList<>());
                session_accelerometer.put("x", new ArrayList<>());
                session_accelerometer.put("y", new ArrayList<>());
                session_accelerometer.put("z", new ArrayList<>());
            }
            session_accelerometer.get("time").add(dst[0]);
            session_accelerometer.get("x").add(dst[1]);
            session_accelerometer.get("y").add(dst[2]);
            session_accelerometer.get("z").add(dst[3]);
        }

        else if(messageEvent.getPath().equalsIgnoreCase(HEARTRATE)){
            FloatBuffer values = ByteBuffer.wrap(messageEvent.getData()).asFloatBuffer();
            final float[] dst = new float[values.capacity()];
            values.get(dst);

            if(session_heartbeat == null){
                session_heartbeat = new HashMap<>();
                session_heartbeat.put("time", new ArrayList<>());
                session_heartbeat.put("HR", new ArrayList<>());
            }

            session_heartbeat.get("time").add(dst[0]);
            session_heartbeat.get("HR").add(dst[1]);

            Log.e(HEARTRATE, dst[0]+"" + dst[1]);
        }

        else if(messageEvent.getPath().equalsIgnoreCase(STOP)){
            //TODO: lange delay
            try  {
                //output = getActivityPredictions();
                output = new float[]{(float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0};
                trantitions = get_trantitions();
                Log.d("hh", "tlukt");
                calculateSessionData();
            } catch(Exception e){

                calculateSessionData();

            }
        }
    }

    private void calculateSessionData(){
        //mets + turns + weeknumber
        Map<String, String> session_calc = new HashMap<>();
        String id = UUID.randomUUID().toString();
        int turns = numberTurns();
        //double met_score = processMETscore();
        int week = app.getWeeknr();
        //TODO: mets moet int zijn
        //session_calc.put("met_score", met_score+"");
        session_calc.put("turns", turns+"");
        //TODO: week moet int zijn
        session_calc.put("week", week+"");
        //TODO: mistakes in room
        List<Float> mistakes = mistakesTimestamps();

        s.setWeek(app.getWeeknr());
        s.setTurns(turns);

        //post_data(session_calc, null, id);

        /*
        //mistakes
        for (int i=0; i < mistakes.size(); i++){
            Map<String, String> mis = new HashMap<>();
            mis.put("time", mistakes.get(i)+"");
            post_data(mis, "mistakes", id);
        }*/

        double totalMets = 0;
        //activities
        for(int i = 0; i < trantitions.get("start").size(); i++){
            Map<String, String> d = new HashMap<>();
            double met_score = processMETscore(trantitions.get("start").get(i), trantitions.get("end").get(i));
            Long start = (long) floatToTimeDouble(trantitions.get("start").get(i));
            Long end = (long) floatToTimeDouble(trantitions.get("end").get(i));
            d.put("start", start+"");
            d.put("end", end+"");
            d.put("activity", trantitions.get("activity").get(i).toString());

            totalMets += met_score;

            SessionActivity a = new SessionActivity();
            a.setEnd(end);
            a.setStart(start);
            a.setMET_score(met_score);

            activities.add(a);

            //post_data(d, "activities", id);
        }
        s.setMets(totalMets);
        //appDb.sessionDao().insertActivitiesForSession(s ,activities);
        appDb.sessionDao().insertSession(s);
        appDb.sessionDao().insertActivitiesForSession(s, activities);
    }

    private float[] getActivityPredictions(){
        File sdcard = getExternalFilesDir(null);
        Interpreter interpreter = new Interpreter(new File(sdcard != null ? sdcard.getAbsolutePath() : null, "rope_skipping_simple.tflite"));
        float[][][][] input = segmentation();
        //TODO: transform output naar enum
        float[] out = new float[input.length*100];
        //final ByteBuffer buffer = ByteBuffer.allocate(input.size()*input.get(0).size()*input.get(0).get(0).size()*4);
        //buffer.put(input);
        interpreter.run(input, out);
        return out;
    }

    private void post_data(Map<String, String> data, String path, String id){
        if(path != null){
            firestore.collection("users")
                    .document("testUser")
                    .collection("sessions")
                    .document(id)
                    .collection(path)
                    .add(data)
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
        } else{
            firestore.collection("users")
                    .document("testUser")
                    .collection("sessions")
                    .document(id)
                    .set(data)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.w("dd", "Error adding document");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("dd", "Error adding document", e);
                        }
                    });
        }
    }

    /*
    SESSION CALCULATIONS
     */

    //TODO: alle assen bekijken en hier overlapping van nemen
    //als data in alle assen bijna 0 is voor bepaalde duur = mistake
    private List<Float> mistakesTimestamps(){
        List<Float> mistakes = new ArrayList<>();
        double interval_high = 0.5;
        double interval_low = -0.5;
        int start = -1, end;
        int threshold = 6; //aantal datapunten nodig om geclassificeerd te worden als mistake
        for(int i = 1; i < session_accelerometer.get("x").size(); i++){
            //overgang van niet in interval naar wel = start
            if(
                    ((session_accelerometer.get("x").get(i) < interval_high && session_accelerometer.get("x").get(i) > interval_low)) && ((session_accelerometer.get("x").get(i-1) > interval_high || session_accelerometer.get("x").get(i-1) < interval_low))
                    ){
                start = i;
            }
            //overgang van in interval naar niet = end
            if(
                    ((session_accelerometer.get("x").get(i-1) < interval_high && session_accelerometer.get("x").get(i-1) > interval_low)) && ((session_accelerometer.get("x").get(i) > interval_high || session_accelerometer.get("x").get(i) < interval_low))
                    ){
                if(start != -1 && start - i > threshold){
                    mistakes.add(session_accelerometer.get("time").get(start));
                    start = -1;
                }
            }
        }
        return mistakes;
    }

    private int localMaxima(float a[])
    {
        int count = 0;

        for (int i = 1; i < a.length - 1; i++)
        {
            if(a[i] > a[i - 1] && a[i] > a[i + 1])
                count += 1;
        }
        return count;
    }

    //TODO: voor elke afzonderlijke beweging -> optellen (return)
    private int numberTurns(){
        Map<JumpMoves, Integer> turns = new HashMap<>();
        turns.put(JumpMoves.SLOW, 0);
        float [] x_savgol,y_savgol, z_savgol;
        int x_turns, y_turns, z_turns;
        //voor elke activiteit bereken draaiingen
        for(int i = 0; i < trantitions.get("start").size(); i++){
           //TODO: if -> voor elke afzonderlijke beweging
            if(true){
                SavGolFilter f = filters.get(JumpMoves.SLOW);
                //TODO: aantal keer toepassen
                x_savgol = f.filterData(Floattofloat(session_accelerometer.get("x")));
                y_savgol = f.filterData(Floattofloat(session_accelerometer.get("y")));
                z_savgol = f.filterData(Floattofloat(session_accelerometer.get("z")));

                //TODO: savgol geeft overal zelfde waarde
                x_turns = localMaxima(x_savgol);
                y_turns = localMaxima(y_savgol);
                z_turns = localMaxima(z_savgol);

                turns.put(JumpMoves.SLOW,turns.get(JumpMoves.SLOW) + ((x_turns+y_turns+z_turns)/3));
            }

        }
        return 0;
    }

    //TODO: tijdstippen bekijken
    //TODO: MET per activiteit
    //TODO: wat als geen heartdata
    private double processMETscore(Float start, Float end){
        List<Float> heartRateFiltered = session_heartbeat.get("HR").stream()
                .filter(h -> session_heartbeat.get("time").get(session_heartbeat.get("HR").indexOf(h)) > start && session_heartbeat.get("time").get(session_heartbeat.get("HR").indexOf(h)) < end)
                .collect(Collectors.toList());
        List<Float> timeFiltered = session_heartbeat.get("time").stream()
                .filter(t -> t > start && t < end)
                .collect(Collectors.toList());
        int score = 0;
        //MPA = ligthzone, MPV = moderate zone
        double timeMPA, timeMPV;
        double sumMETmin = 0;
        timeMPA = 0;
        timeMPV = 0;
        for(int i = 1; i < heartRateFiltered.size() ; i++){
                if((getHeartRateZone(heartRateFiltered.get(i)) == getHeartRateZone(session_heartbeat.get("HR").get(i-1)))){
                    if(getHeartRateZone(heartRateFiltered.get(i)) == lightZone){
                            timeMPA += timeFiltered.get(i) - timeFiltered.get(i-1);
                    } else if(getHeartRateZone(heartRateFiltered.get(i)) == moderateZone){
                            timeMPV += timeFiltered.get(i) - timeFiltered.get(i-1);
                    }
                }
        }
        timeMPA = (timeMPA * Math.pow(10,-3))/60;
        timeMPV = (timeMPV * Math.pow(10,-3))/60;
        sumMETmin += 4 * timeMPA + 8 * timeMPV;

        return sumMETmin;
    }

    private int getHeartRateZone(double v){
        if( (v >= 0.5*MAXHR) && (v < 0.6*MAXHR) ){
            return veryLightZone;
        } else if( (v >= 0.6*MAXHR) && (v < 0.7*MAXHR) ){
            return lightZone;
        } else if( (v >= 0.7*MAXHR) && (v < 0.8*MAXHR) ){
            return moderateZone;
        } else if( (v >= 0.8*MAXHR) && (v < 0.9*MAXHR) ){
            return hardZone;
        } else if( (v >= 0.9*MAXHR) && (v < MAXHR) ){
            return maximumZone;
        }
        return 0;
    }

    /*
    ACTIVITY RECOGNITION: PREPROCESSING
     */

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

        for( int i = 0; i < session_accelerometer.get("x").size() - FRAME_SIZE; i += HOP_SIZE){
            float[][] x = float1DTo2D(Floattofloat(session_accelerometer.get("x").subList(i, i+FRAME_SIZE)));
            float[][] y = float1DTo2D(Floattofloat(session_accelerometer.get("y").subList(i, i+FRAME_SIZE)));
            float[][] z = float1DTo2D(Floattofloat(session_accelerometer.get("z").subList(i, i+FRAME_SIZE)));

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

    /*
    ACTIVITY RECOGNITION: PROCES OUTPUT
     */

    //TODO: met overlap omgaan
    private Map<String, List<Float>> get_trantitions() {
        Map<Integer,Float> t = makeActivities();
        int FRAME_SIZE = getSamplingFrequentie() * 1;
        int HOP_SIZE = 1;
        //start, end, activiteit
        Map<String, List<Float>> trantitions_ = new HashMap<>();
        trantitions_.put("start", new ArrayList<>());
        trantitions_.put("end", new ArrayList<>());
        trantitions_.put("activity", new ArrayList<>());

        List<Integer> indexes=new ArrayList(t.keySet());
        Collections.sort(indexes);

        float end, start;
        if(indexes.size() == 1){
            int numberSensorSamples = (output.length)*FRAME_SIZE; //TODO: drop partial frames
            start = session_accelerometer.get("time").get(0);
            end = session_accelerometer.get("time").get(1); //TODO: remove dummydata (make number samples correct)
            trantitions_.get("start").add(start);
            trantitions_.get("end").add(end);
            trantitions_.get("activity").add(t.get(0));
        } else{
            int indexTime = 0;
            for (int i = 0; i<indexes.size()-1; i++) {
                int numberSensorSamples = (indexes.get(i + 1) - indexes.get(i))*FRAME_SIZE;
                start = session_accelerometer.get("time").get(indexTime);
                end = session_accelerometer.get("time").get(indexTime+numberSensorSamples);
                trantitions_.get("start").add(start);
                trantitions_.get("end").add(end);
                trantitions_.get("activity").add(t.get(i));

                indexTime = indexTime+numberSensorSamples;
            }

        }
        return trantitions_;
    }

    //map start index uit output array op de activity die daar begint
    //OUTPUT: index1 -> activiteit1, index2 -> activiteit2....
    private Map<Integer,Float> makeActivities(){
        Map<Integer,Float> t = new HashMap<>();
        float activity = output[0];
        int start = 0;
        for (int i = 1; i < output.length-1; i++) {
            if(output[i] != output[i-1]){
                t.put(start, output[start]);
                start = i;
            }
        }
        if(t.size() == 0){
            t.put(start, output[start]);
        }
        //beginindex uit output array gemapt op waarde
        return t;
    }

    /*
    HELP FUNCTIONS
     */

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
        int size=src.size()*3*session_accelerometer.get("x").size()*4;

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


}
