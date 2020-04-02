package ugent.waves.healthrecommenderapp.Services;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.firebase.firestore.FirebaseFirestore;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import ugent.waves.healthrecommenderapp.Enums.JumpMoves;
import ugent.waves.healthrecommenderapp.HelpClasses.SavGolFilter;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Mistake;
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

    private List<JumpMoves> output;
    private List<Float> mistakes;

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
        filters.put(JumpMoves.SIDE_SWING, new SavGolFilter(0, 101, 5));
        filters.put(JumpMoves.CROSS_OVER, new SavGolFilter(0, 41, 3));
        filters.put(JumpMoves.FORWARD_180, new SavGolFilter(0, 51, 3));

        s = new Session();
        activities = new ArrayList<>();
        appDb = app.getAppDb();

        //TODO: message ordering niet ok??
        if(messageEvent.getPath().equalsIgnoreCase(START) ){
            session_accelerometer = new HashMap<>();
            session_accelerometer.put("time", new ArrayList<>());
            session_accelerometer.put("time_delta", new ArrayList<>());
            session_accelerometer.put("x", new ArrayList<>());
            session_accelerometer.put("y", new ArrayList<>());
            session_accelerometer.put("z", new ArrayList<>());

            session_heartbeat = new HashMap<>();
            session_heartbeat.put("time", new ArrayList<>());
            session_heartbeat.put("HR", new ArrayList<>());

        }
        //TODO: eerste en laatste 3 sec ervan doen?
        else if( messageEvent.getPath().equalsIgnoreCase(ACCELEROMETER) ){
            FloatBuffer values = ByteBuffer.wrap(messageEvent.getData()).asFloatBuffer();
            final float[] dst = new float[values.capacity()];
            values.get(dst);

            if(session_accelerometer == null){
                session_accelerometer = new HashMap<>();
                session_accelerometer.put("time", new ArrayList<>());
                session_accelerometer.put("time_delta", new ArrayList<>());
                session_accelerometer.put("x", new ArrayList<>());
                session_accelerometer.put("y", new ArrayList<>());
                session_accelerometer.put("z", new ArrayList<>());
            }
            //check for duplicate datapoints
            //dezelfde accelerometer waarde wordt kort na elkaar soms meerdere keren gesampled, dus kijken nr tijd werkt niet
            //TODO: check of door deleten van samples, samplingfreq wel nog ok is
            if(session_accelerometer.get("x").size() == 0 || session_accelerometer.get("x").get(session_accelerometer.get("x").size()-1) != dst[1]){
                double delta;
                if(session_accelerometer.get("x").size() == 0){
                    delta = 0;
                } else{
                    delta = floatToTimeDouble(session_accelerometer.get("time").get(0), dst[0]);
                }
                try{
                    session_accelerometer.get("time_delta").add((float)delta);
                } catch(Exception e){
                    e.printStackTrace();
                }
                session_accelerometer.get("time").add(dst[0]);
                session_accelerometer.get("x").add(dst[1]);
                session_accelerometer.get("y").add(dst[2]);
                session_accelerometer.get("z").add(dst[3]);
            }
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
            //check for duplicate datapoints
            if(session_heartbeat.get("time").size() == 0 || session_heartbeat.get("time").get(session_heartbeat.get("time").size()-1) != dst[0]){
                session_heartbeat.get("time").add(dst[0]);
                session_heartbeat.get("HR").add(dst[1]);
            }
        }
        else if(messageEvent.getPath().equalsIgnoreCase(STOP)){
            try  {
                normalization();
                output = getActivityPredictions();
                trantitions = get_trantitions();
                calculateSessionData();
            } catch(Exception e){
                e.printStackTrace();

            }
        }
    }

    private void calculateSessionData(){
        int turns = numberTurns();
        int week = app.getWeeknr();
        //TODO: mistakes in room
        List<Mistake> m = mistakes_ML();

        s.setWeek(app.getWeeknr());
        s.setTurns(turns);

        double totalMets = 0;
        //activities
        for(int i = 0; i < trantitions.get("start").size(); i++){
            double met_score = processMETscore(trantitions.get("start").get(i), trantitions.get("end").get(i));
            Long start = (long) Float.parseFloat(String.valueOf(trantitions.get("start").get(i)));
            Long end = (long) Float.parseFloat(String.valueOf(trantitions.get("end").get(i)));
            int act = (int) Float.parseFloat(String.valueOf(trantitions.get("activity").get(i)));

            totalMets += met_score;

            //mistakes are processed in another way
            if(JumpMoves.getJump(act) != JumpMoves.MISTAKE){
                SessionActivity a = new SessionActivity();
                a.setEnd(end);
                a.setStart(start);
                a.setMET_score(met_score);
                a.setActivity(act);

                activities.add(a);
            }
        }
        s.setMets(totalMets);
        long id = appDb.sessionDao().insertSession(s);
        for(SessionActivity a: activities){
            a.setSessionId((int) id);
            appDb.activityDao().insertActivity(a);
        }
        for(Mistake mk: m){
            mk.setSessionId((int) id);
            appDb.mistakeDao().insertMistake(mk);
        }

    }

    //TODO: crash als minder dan 1 sec sessie
    private List<JumpMoves> getActivityPredictions(){
        File sdcard = getExternalFilesDir(null);
        Interpreter interpreter = new Interpreter(new File(sdcard != null ? sdcard.getAbsolutePath() : null, "converted_model2.tflite"));
        float[][][] segments = segmentation();

        List<JumpMoves> out = new ArrayList<>();
        int[] probabilityShape =
                interpreter.getOutputTensor(0).shape();
        DataType probabilityDataType = interpreter.getOutputTensor(0).dataType();

        for(float[][] segment: segments){
            float[][][] s = new float[1][segment.length][segment[0].length];
            TensorBuffer outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

            interpreter.run(s, outputProbabilityBuffer.getBuffer());
            out.add(getMostProbable(outputProbabilityBuffer.getFloatArray()));
        }
        return out;
    }

    private JumpMoves getMostProbable(float[] floatArray) {
        //TODO: determine threshold for probability
        Map<Float, Integer> m = arrayToMap(floatArray);
        Arrays.sort(floatArray);
        if(floatArray[floatArray.length-1] > 0.80){
            return JumpMoves.getJump(m.get(floatArray[floatArray.length-1]));
        } else{
            return JumpMoves.OTHER;
        }
    }

    private Map<Float,Integer> arrayToMap(float[] floatArray) {
        Map<Float,Integer> probabilities = new HashMap<>();
        for(int i=0; i<floatArray.length; i++){
            probabilities.put(floatArray[i], i);
        }
        return probabilities;
    }

    /*
    SESSION CALCULATIONS
     */

    //TODO: met afgeleide
    //TODO: alle assen bekijken en hier overlapping van nemen
    //als data in alle assen bijna 0 is voor bepaalde duur = mistake
    private List<Float> mistakesTimestamps_deravative(){
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

    private List<Mistake> mistakes_ML(){
        //timestamp mistake + move that causes it
        List<Mistake> mistakes = new ArrayList<>();
        for(int i = 1; i < trantitions.get("start").size(); i++){
            JumpMoves m = JumpMoves.getJump((int)Float.parseFloat(String.valueOf(trantitions.get("activity").get(i))));
            float time_delta = trantitions.get("end").get(i) - trantitions.get("start").get(i);

            //ASSUME jump before causes mistake
            //TODO: kan niet hier inserten want sessionid nog niet geweten
            if(m == JumpMoves.MISTAKE && time_delta > 1){
                Mistake mis = new Mistake();
                mis.setActivity((int) Float.parseFloat(String.valueOf(trantitions.get("activity").get(i-1))));
                mis.setTime((int) Float.parseFloat(String.valueOf(trantitions.get("start").get(i))));
                mistakes.add(mis);
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

    //TODO: soms negatieve indexen???
    //TODO: finetunen
    private int numberTurns(){
        //TODO: put in different place
        Map<JumpMoves, Integer> iterations = new HashMap<>();
        iterations.put(JumpMoves.SLOW, 3);
        iterations.put(JumpMoves.FAST, 5);
        iterations.put(JumpMoves.CROSS_OVER, 2);
        iterations.put(JumpMoves.SIDE_SWING, 5);
        iterations.put(JumpMoves.FORWARD_180, 2);

        Map<JumpMoves, Integer> turns = new HashMap<>();
        turns.put(JumpMoves.SLOW, 0);
        turns.put(JumpMoves.FAST, 0);
        turns.put(JumpMoves.CROSS_OVER, 0);
        turns.put(JumpMoves.SIDE_SWING, 0);
        turns.put(JumpMoves.FORWARD_180, 0);

        float [] x_savgol,y_savgol, z_savgol;
        int x_turns, y_turns, z_turns;
        int sum_turns = 0;

        //voor elke activiteit bereken draaiingen
        for(int i = 0; i < trantitions.get("start").size(); i++){
            int act = (int) Float.parseFloat(String.valueOf(trantitions.get("activity").get(i)));
            int s = session_accelerometer.get("time_delta").indexOf(trantitions.get("start").get(i));
            int e = session_accelerometer.get("time_delta").indexOf(trantitions.get("end").get(i));
            SavGolFilter f = filters.get(JumpMoves.getJump(act));

            x_savgol = f.filterData(Floattofloat(session_accelerometer.get("x").subList(s, e)));
            y_savgol = f.filterData(Floattofloat(session_accelerometer.get("y").subList(s, e)));
            z_savgol = f.filterData(Floattofloat(session_accelerometer.get("z").subList(s, e)));

            for(int j = 0; j < iterations.get(JumpMoves.getJump(act))-1; j++){
                x_savgol = f.filterData(x_savgol);
                y_savgol = f.filterData(y_savgol);
                z_savgol = f.filterData(z_savgol);
            }

            x_turns = localMaxima(x_savgol);
            y_turns = localMaxima(y_savgol);
            z_turns = localMaxima(z_savgol);

            turns.put(JumpMoves.getJump(act),turns.get(JumpMoves.getJump(act)) + ((x_turns+y_turns+z_turns)/3));
        }

        for(int t: turns.values()){
            sum_turns += t;
        }

        return sum_turns;
    }

    //TODO: hartslag in lagere zones ook meerekenen? + formule herzien
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
    //normalizeren in scikit: zorgen dat unit norm van de vector 1 is
    //dus delen door unit norm?
    private void normalization(){
        float x, y, z;
        double squared_sum, norm, checksum;
        for( int i = 0; i < session_accelerometer.get("x").size(); i++){
            x = session_accelerometer.get("x").get(i);
            y = session_accelerometer.get("y").get(i);
            z = session_accelerometer.get("z").get(i);
            squared_sum = Double.parseDouble(Float.toString((x*x) + (y*y) + (z*z)));
            norm = Math.sqrt(squared_sum);
            session_accelerometer.get("x").set(i, Float.parseFloat(Double.toString(x/norm)));
            session_accelerometer.get("y").set(i, Float.parseFloat(Double.toString(y/norm)));
            session_accelerometer.get("z").set(i, Float.parseFloat(Double.toString(z/norm)));
            checksum = Math.sqrt((x/norm)*(x/norm) + (y/norm)*(y/norm) + (z/norm)*(z/norm));
            Log.d("d", "d");
        }
    }

    //TODO: sampling mss in realtime schatten?
    private int getSamplingFrequentie(){
        return 52;
    }

    //TODO: hopsize
    private float[][][] segmentation(){
        int N_FEATURES = 3;
        int FRAME_SIZE = getSamplingFrequentie() * 1;

        List<float[][]> frames = new ArrayList<>();

        for( int i = 0; i < session_accelerometer.get("x").size() - FRAME_SIZE; i += FRAME_SIZE){
            float[][] segment = listTosegment(session_accelerometer.get("x").subList(i, i+FRAME_SIZE), session_accelerometer.get("y").subList(i, i+FRAME_SIZE), session_accelerometer.get("x").subList(i, i+FRAME_SIZE));

            frames.add(segment);
        }

        return floatListTofloat3D(frames);
    }

    private float[][][] floatListTofloat3D(List<float[][]> l) {
        float[][][] floatArray = new float[l.size()][l.get(0).length][l.get(0)[0].length];
        int i = 0;
        for (float[][] f : l) {
            floatArray[i++] = f;
        }
        return floatArray;
    }

    private float[][] listTosegment(List<Float> x, List<Float> y, List<Float> z){
        float[][] segment = new float[x.size()][3];
        for(int i = 0; i < x.size(); i++){
            float[] sample = new float[3];
            sample[0] = x.get(i);
            sample[1] = y.get(i);
            sample[2] = z.get(i);
            segment[i] = sample;
        }
        return segment;
    }

    /*
    ACTIVITY RECOGNITION: PROCES OUTPUT
     */

    //TODO: met overlap omgaan
    private Map<String, List<Float>> get_trantitions() {
        Map<Integer,JumpMoves> t = makeActivities();
        int FRAME_SIZE = getSamplingFrequentie() * 1;
        //start, end, activiteit
        Map<String, List<Float>> trantitions_ = new HashMap<>();
        trantitions_.put("start", new ArrayList<>());
        trantitions_.put("end", new ArrayList<>());
        trantitions_.put("activity", new ArrayList<>());

        //get indexes in order of when activity started
        List<Integer> indexes=new ArrayList(t.keySet());
        Collections.sort(indexes);

        float end, start;
        if(indexes.size() == 1){
            int numberSensorSamples = (output.size())*FRAME_SIZE; //TODO: drop partial frames
            start = session_accelerometer.get("time_delta").get(0);
            end = session_accelerometer.get("time_delta").get(numberSensorSamples);
            trantitions_.get("start").add(start);
            trantitions_.get("end").add(end);
            trantitions_.get("activity").add((float) t.get(0).getValue());
        } else{
            int indexTime = 0;
            for (int i = 0; i<indexes.size()-1; i++) {
                int numberSensorSamples = (indexes.get(i + 1) - indexes.get(i))*FRAME_SIZE;
                start = session_accelerometer.get("time_delta").get(indexTime);
                end = session_accelerometer.get("time_delta").get(indexTime+numberSensorSamples);
                trantitions_.get("start").add(start);
                trantitions_.get("end").add(end);
                trantitions_.get("activity").add((float) t.get(i).getValue());

                indexTime = indexTime+numberSensorSamples;
            }

        }
        return trantitions_;
    }

    //map start index uit output array op de activity die daar begint
    //OUTPUT: index1 -> activiteit1, index2 -> activiteit2....
    private Map<Integer,JumpMoves> makeActivities(){
        Map<Integer,JumpMoves> t = new HashMap<>();
        JumpMoves activity = output.get(0);
        int start = 0;
        for (int i = 1; i < output.size()-1; i++) {
            if(output.get(i) != output.get(i-1)){
                t.put(start, output.get(start));
                start = i;
            }
        }
        if(t.size() == 0){
            t.put(start, output.get(start));
        }
        //beginindex uit output array gemapt op waarde
        return t;
    }

    /*
    HELP FUNCTIONS
     */

    //TODO: system.nanotime was time since epoch, nu rare waarden
    //SECONDEN
    private double floatToTimeDouble(float time1,float time2) {
        //seconden
        long timedelta_seconden = (long) (time2 - time1)/1000000000;

        return timedelta_seconden;
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
