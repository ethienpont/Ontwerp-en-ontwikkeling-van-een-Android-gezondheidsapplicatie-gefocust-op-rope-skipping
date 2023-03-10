package ugent.waves.healthrecommenderapp.Services;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.firebase.firestore.FirebaseFirestore;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.IOException;
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
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.Persistance.Session;
import ugent.waves.healthrecommenderapp.Persistance.SessionActivity;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;


//TODO: structurize
//TODO: works met debugger, niet zonder
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

    private double MAXHR = 200;

    //RAW DATA STORAGE
    private Map<String, List<Float>> session_heartbeat;
    private Map<String, List<Float>> session_accelerometer;

    //ROOM DB
    private Session s;
    private List<SessionActivity> activities;
    private AppDatabase appDb;
    private double window = 1.5;


    //TODO: oncreate called maar onmessagereceived niet meer
    /*
    public void onCreate(){
        super.onCreate();
        app = (healthRecommenderApplication) this.getApplicationContext();
        firestore = app.getFirestore();
    }*/
    
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        app = (healthRecommenderApplication) this.getApplicationContext();
        SharedPreferences sharedPref = getSharedPreferences(app.getAccount().getId(), MODE_PRIVATE);


        //TODO: get age from account (via shared pref mss of get profile in loginact)
        //this.MAXHR = app.getAccount().getRequestedScopes()
        String bday = sharedPref.getString("bday", "");
        String bmonth = sharedPref.getString("bmonth", "");
        String byear = sharedPref.getString("byear", "");

        filters = new HashMap<>();
        filters.put(JumpMoves.SLOW, new SavGolFilter(25, 25, 3));
        filters.put(JumpMoves.FAST, new SavGolFilter(16, 16, 5));
        filters.put(JumpMoves.SIDE_SWING, new SavGolFilter(50, 50, 5));
        filters.put(JumpMoves.CROSS_OVER, new SavGolFilter(20, 20, 3));
        filters.put(JumpMoves.FORWARD_180, new SavGolFilter(25, 25, 3));

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
        //TODO: eerste en laatste 3 sec ervan doen?: bij predictie -> testen
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

            //TODO: mss fout hier?
            for(int i=0; i < dst.length; i = i+4){
                session_accelerometer.get("time").add(dst[i]);
                session_accelerometer.get("x").add(dst[i+1]);
                session_accelerometer.get("y").add(dst[i+2]);
                session_accelerometer.get("z").add(dst[i+3]);
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

            for(int i=0; i < dst.length; i = i+2){
                session_heartbeat.get("time").add(dst[i]);
                session_heartbeat.get("HR").add(dst[i+1]);
            }
        }
        else if(messageEvent.getPath().equalsIgnoreCase(STOP)){
            //TODO: heartbeat soms null soms niet???
            if(session_heartbeat != null && session_heartbeat.get("time").size() > 0) {
            }else{
                    //TODO: alert dat geen hartslag
                }
            //als minstens 1 segment kan gemaakt worden
            //TODO: komt er nog altijd in als minder dan 52???
            if(session_accelerometer != null && session_accelerometer.get("x").size() > 52){
                try  {
                    checkDuplicates(session_heartbeat == null || session_heartbeat.get("time").size()>0);
                    normalization();
                    output = getActivityPredictions();
                    trantitions = get_trantitions();
                    calculateSessionData(session_heartbeat == null || session_heartbeat.get("time").size()>0);
                } catch(Exception e){
                    e.printStackTrace();

                }
            }

        }
    }
    
    private void checkDuplicates(boolean heartrate){
        Map<String, List<Float>> session_accelerometer_preprocessed = new HashMap<>();
        session_accelerometer_preprocessed.put("time", new ArrayList<>());
        session_accelerometer_preprocessed.put("time_delta", new ArrayList<>());
        session_accelerometer_preprocessed.put("x", new ArrayList<>());
        session_accelerometer_preprocessed.put("y", new ArrayList<>());
        session_accelerometer_preprocessed.put("z", new ArrayList<>());

        session_accelerometer_preprocessed.get("time").add(session_accelerometer.get("time").get(0));
        session_accelerometer_preprocessed.get("x").add(session_accelerometer.get("x").get(0));
        session_accelerometer_preprocessed.get("y").add(session_accelerometer.get("y").get(0));
        session_accelerometer_preprocessed.get("z").add(session_accelerometer.get("z").get(0));
        session_accelerometer_preprocessed.get("time_delta").add((float) 0);

        for(int i=1; i < session_accelerometer.get("time").size(); i++){
            if(!session_accelerometer.get("time").get(i).equals(session_accelerometer.get("time").get(i-1))){
                double delta = floatToTimeDouble(session_accelerometer_preprocessed.get("time").get(0), session_accelerometer.get("time").get(i));

                session_accelerometer_preprocessed.get("time").add(session_accelerometer.get("time").get(i));
                session_accelerometer_preprocessed.get("x").add(session_accelerometer.get("x").get(i));
                session_accelerometer_preprocessed.get("y").add(session_accelerometer.get("y").get(i));
                session_accelerometer_preprocessed.get("z").add(session_accelerometer.get("z").get(i));
                session_accelerometer_preprocessed.get("time_delta").add((float) delta);
            }
        }
        if(heartrate){
            Map<String, List<Float>> session_heartbeat_preprocessed = new HashMap<>();
            session_heartbeat_preprocessed.put("time", new ArrayList<>());
            session_heartbeat_preprocessed.put("HR", new ArrayList<>());

            session_heartbeat_preprocessed.get("time").add(session_heartbeat.get("time").get(0));
            session_heartbeat_preprocessed.get("HR").add(session_heartbeat.get("HR").get(0));

            for(int i=1; i < session_heartbeat.get("time").size(); i++){
                if(!session_heartbeat.get("time").get(i).equals(session_heartbeat.get("time").get(i-1))){
                    session_heartbeat_preprocessed.get("time").add(session_heartbeat.get("time").get(i));
                    session_heartbeat_preprocessed.get("HR").add(session_heartbeat.get("HR").get(i));
                }
            }
            session_heartbeat = session_heartbeat_preprocessed;
            Log.d("","");
        }
        session_accelerometer = session_accelerometer_preprocessed;
    }

    private void calculateSessionData(boolean heartrate){
        int turns = numberTurns();
        List<Mistake> m = mistakesTimestamps_deravative();

        s.setWeek(app.getWeeknr());
        s.setTurns(turns);

        double totalMets = 0;

        Map<JumpMoves, Integer> activityDuration = new HashMap<>();
        Map<JumpMoves, Integer> activityMets = new HashMap<>();

        //activities
        for(int i = 0; i < trantitions.get("start").size(); i++){
            double met_score = 0;
            if(heartrate){
                met_score = processMETscore(trantitions.get("start").get(i), trantitions.get("end").get(i));
                totalMets += met_score;
            }

            Long start = (long) Float.parseFloat(String.valueOf(trantitions.get("start").get(i)));
            Long end = (long) Float.parseFloat(String.valueOf(trantitions.get("end").get(i)));
            int act = (int) Float.parseFloat(String.valueOf(trantitions.get("activity").get(i)));

            //keep duration
            if(!activityDuration.containsKey(JumpMoves.getJump(act))){
                activityDuration.put(JumpMoves.getJump(act), 0);
            }
            activityDuration.put(JumpMoves.getJump(act), (int) (activityDuration.get(JumpMoves.getJump(act)) + (end-start)));
            //keep mets
            if(!activityMets.containsKey(JumpMoves.getJump(act))){
                activityMets.put(JumpMoves.getJump(act), 0);
            }
            activityMets.put(JumpMoves.getJump(act), (int) (activityMets.get(JumpMoves.getJump(act)) + met_score));

            SessionActivity a = new SessionActivity();
            a.setEnd(end);
            a.setStart(start);
            a.setMET_score(met_score);
            a.setActivity(act);
            a.setWeek(app.getWeeknr());

            activities.add(a);

        }
        s.setMets(totalMets == 0 ? -1 : totalMets);
        long id = appDb.sessionDao().insertSession(s);
        for(SessionActivity a: activities){
            a.setSessionId((int) id);
            appDb.activityDao().insertActivity(a);
        }
        for(Mistake mk: m){
            mk.setSessionId((int) id);
            mk.setWeek(app.getWeeknr());
            appDb.mistakeDao().insertMistake(mk);
        }

        Recommendation[] pending = appDb.recommendationDao().getPendingRecommendation(true);

        //check of pending recommendation voldoet aan sessie
        int fulfilledRecommendation = -1;
        for(int i = 0; i < pending.length; i++){
            if( (pending[i].getMets() >= activityMets.get(JumpMoves.getJump(pending[i].getActivity()))) && (pending[i].getDuration() >= activityDuration.get(JumpMoves.getJump(pending[i].getActivity()))) ){
                fulfilledRecommendation = i;
            }
        }
        if(fulfilledRecommendation != -1){
            Recommendation d = pending[fulfilledRecommendation];
            d.setDone(true);
            appDb.recommendationDao().updateRecommendation(d);
        }

        //TODO: als niet fulfilled toon alert dat een pending recommendation nog niet gedaan is
        //TODO: alert als pending wel fulfilled
    }

    private List<JumpMoves> getActivityPredictions() {
        File sdcard = getExternalFilesDir(null);
        Interpreter interpreter = new Interpreter(new File(sdcard != null ? sdcard.getAbsolutePath() : null, "test.tflite"));


            /*
            List<String> labels = FileUtil.loadLabels(this,  "labels.txt");
            TensorProcessor probabilityProcessor =
                    new TensorProcessor.Builder().build();*/
            interpreter.allocateTensors();
            float[][][] segments = segmentation();

            List<JumpMoves> out = new ArrayList<>();
            int[] probabilityShape =
                    interpreter.getOutputTensor(0).shape();
            DataType probabilityDataType = interpreter.getOutputTensor(0).dataType();


            for(float[][] segment: segments){
                float[][][] s = new float[1][segment.length][segment[0].length];
                s[0] = segment;
                TensorBuffer outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

                interpreter.run(s, outputProbabilityBuffer.getBuffer().rewind());

                //TensorLabel l = new TensorLabel(labels,probabilityProcessor.process(outputProbabilityBuffer));

                // Create a map to access the result based on label
                //Map<String, Float> floatMap = l.getMapWithFloatValue();
                out.add(getMostProbable(outputProbabilityBuffer.getFloatArray()));
            }

            interpreter.close();

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

    private float[] getDeravative(List<Float> signal, List<Float> time){
        float[] deravative = new float[signal.size()-1];
        for(int i=1; i < signal.size(); i++){
            deravative[i-1] = (signal.get(i) - signal.get(i-1))/(time.get(i) - time.get(i-1));
        }
        return deravative;
    }

    //TODO: tijd mss niet juist door afgeleide te nemen, want verschuift een pt
    //TODO: alle assen bekijken en hier overlapping van nemen
    //als data in alle assen bijna 0 is voor bepaalde duur = mistake
    private List<Mistake> mistakesTimestamps_deravative(){
        float[] d = getDeravative(session_accelerometer.get("x"), session_accelerometer.get("time"));
        List<Mistake> mistakes = new ArrayList<>();
        double interval_high = 0.0000000001;
        double interval_low = -0.0000000001;
        int start = -1, end;
        int threshold = 1; //aantal datapunten nodig om geclassificeerd te worden als mistake
        for(int i = 1; i < d.length; i++){
            //overgang van niet in interval naar wel = start
            if(( (d[i] < interval_high && d[i] > interval_low)) && ((d[i-1] > interval_high || d[i-1] < interval_low)) ){
                start = i;
            }
            //overgang van in interval naar niet = end
            if( (d[i-1] < interval_high && d[i-1] > interval_low) && (d[i] > interval_high || d[i] < interval_low) ){
                if((start != -1) && ((i - start) >= threshold)){
                    Mistake mis = new Mistake();
                    mis.setActivity((int) Float.parseFloat(String.valueOf(trantitions.get("activity").get(i-1))));
                    mis.setTime((int) Float.parseFloat(String.valueOf(trantitions.get("start").get(i))));
                    mistakes.add(mis);
                    //mistakes.add(session_accelerometer.get("time").get(start));
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
            //kan niet hier inserten want sessionid nog niet geweten
            if(m == JumpMoves.MISTAKE && time_delta > 1){
                Mistake mis = new Mistake();
                mis.setActivity((int) Float.parseFloat(String.valueOf(trantitions.get("activity").get(i-1))));
                mis.setTime((int) Float.parseFloat(String.valueOf(trantitions.get("start").get(i))));
                mistakes.add(mis);
            }
        }
        return mistakes;
    }

    private int localMaxima(float a[]) {
        int count = 0;

        for (int i = 1; i < a.length - 1; i++)
        {
            if(a[i] > a[i - 1] && a[i] > a[i + 1])
                count += 1;
        }
        return count;
    }

    //TODO: soms negatieve indexen???
    //TODO: finetunen met definitief model
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
    //TODO: 2x verylight, 16x hard, 32x maximum? mag eigen draai aan geven?
    //TODO: met minutes -> mag omrekenen naar seconden bij recommendation calculation?
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

    //normalizeren in scikit: zorgen dat unit norm van de vector 1 is
    //dus delen door unit norm?
    private void normalization(){
        float x, y, z;
        double squared_sum, norm;
        for( int i = 0; i < session_accelerometer.get("x").size(); i++){
            x = session_accelerometer.get("x").get(i);
            y = session_accelerometer.get("y").get(i);
            z = session_accelerometer.get("z").get(i);
            squared_sum = Double.parseDouble(Float.toString((x*x) + (y*y) + (z*z)));
            norm = Math.sqrt(squared_sum);
            session_accelerometer.get("x").set(i, Float.parseFloat(Double.toString(x/norm)));
            session_accelerometer.get("y").set(i, Float.parseFloat(Double.toString(y/norm)));
            session_accelerometer.get("z").set(i, Float.parseFloat(Double.toString(z/norm)));
        }
    }

    //segmentgrootte is bepaald door data waarop model getraind is, dus vaste samplingwaarde
    private int getSamplingFrequentie(){
        return 52;
    }

    private float[][][] segmentation(){
        int FRAME_SIZE = (int) (getSamplingFrequentie() * window);

        List<float[][]> frames = new ArrayList<>();

        //TODO: laatste datapunten die niet in een frame passen toch meenemen?
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

    private Map<String, List<Float>> get_trantitions() {
        Map<Integer,JumpMoves> t = makeActivities();
        int FRAME_SIZE = (int) (getSamplingFrequentie() * window);
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
            int numberSensorSamples = (output.size())*FRAME_SIZE;
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

    //TODO: check
    //SECONDEN
    private double floatToTimeDouble(float time1,float time2) {
        //verschil heeft grootteorde tot de 10e, verschil in nanoseconden te groot
        float verschil = (time2 - time1);
        //seconden
        long timedelta_seconden = (long) verschil/1000000000;
        //TODO: probleem met timestamps gekregen via bluetooth
        //timedelta_seconden = timedelta_seconden/1000;

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
