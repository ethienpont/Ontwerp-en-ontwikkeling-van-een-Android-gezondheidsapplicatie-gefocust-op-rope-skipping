package ugent.waves.healthrecommenderapp.Services;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;

import ugent.waves.healthrecommenderapp.Enums.JumpMoves;
import ugent.waves.healthrecommenderapp.HelpClasses.Constants;
import ugent.waves.healthrecommenderapp.HelpClasses.SavGolFilter;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Mistake;
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.Persistance.Session;
import ugent.waves.healthrecommenderapp.Persistance.SessionActivity;
import ugent.waves.healthrecommenderapp.Persistance.User;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

public class wearableService extends WearableListenerService {

    private List<JumpMoves> output;

    //"start", "end", "activity"
    private Map<String, List<Float>> trantitions;
    private Map<JumpMoves, SavGolFilter> filters;

    private healthRecommenderApplication app;

    private double MAXHR;

    //Raw data storage
    private Map<String, List<Float>> session_heartbeat;
    private Map<String, List<Float>> session_accelerometer;

    private Session s;
    private List<SessionActivity> activities;
    private AppDatabase appDb;
    private Map<Integer, JumpMoves> activityOutput;
    private User user;
    private Map<JumpMoves, Integer> iterations;

    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        app = (healthRecommenderApplication) this.getApplicationContext();

        //Filter parameters
        filters = new HashMap<>();
        filters.put(JumpMoves.SLOW, new SavGolFilter(25, 25, 3));
        filters.put(JumpMoves.FAST, new SavGolFilter(16, 16, 5));
        filters.put(JumpMoves.SIDE_SWING, new SavGolFilter(75, 75, 5));
        filters.put(JumpMoves.CROSS_OVER, new SavGolFilter(20, 20, 3));
        filters.put(JumpMoves.RUN, new SavGolFilter(20, 20, 3));

        iterations = new HashMap<>();
        iterations.put(JumpMoves.SLOW, 1);
        iterations.put(JumpMoves.FAST, 4);
        iterations.put(JumpMoves.CROSS_OVER, 2);
        iterations.put(JumpMoves.SIDE_SWING, 2);
        iterations.put(JumpMoves.RUN, 2);

        s = new Session();
        activities = new ArrayList<>();
        appDb = app.getAppDb();
        user = appDb.userDao().getCurrent(true);
        this.MAXHR = 220 - user.getAge();

        if(messageEvent.getPath().equalsIgnoreCase(Constants.START) ){

            // Send age to smartwatch
            Integer age = new Integer(appDb.userDao().getUser(app.getAccount().getId()).getAge());
            MessageClient messageClient = Wearable.getMessageClient(this);
            messageClient.sendMessage(messageEvent.getSourceNodeId(), Constants.AGE, new byte[]{age.byteValue()})
                    .addOnSuccessListener(new OnSuccessListener<Integer>() {
                        @Override
                        public void onSuccess(Integer integer) {
                            //gelukt
                        }
                    });

            // Initialize data structures
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
        else if( messageEvent.getPath().equalsIgnoreCase(Constants.ACCELEROMETER) ){
            // Get accelerometer data from message
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

            // Every 4 numbers are part of the same feature vector
            for(int i=0; i < dst.length; i = i+4){
                session_accelerometer.get("time").add(dst[i]);
                session_accelerometer.get("x").add(dst[i+1]);
                session_accelerometer.get("y").add(dst[i+2]);
                session_accelerometer.get("z").add(dst[i+3]);
            }
        }

        else if(messageEvent.getPath().equalsIgnoreCase(Constants.HEARTRATE)){
            //Get heartrate data from message
            FloatBuffer values = ByteBuffer.wrap(messageEvent.getData()).asFloatBuffer();
            final float[] dst = new float[values.capacity()];
            values.get(dst);

            if(session_heartbeat == null){
                session_heartbeat = new HashMap<>();
                session_heartbeat.put("time", new ArrayList<>());
                session_heartbeat.put("HR", new ArrayList<>());
            }

            //Every 2 number are part of the same feature vector
            for(int i=0; i < dst.length; i = i+2){
                session_heartbeat.get("time").add(dst[i]);
                session_heartbeat.get("HR").add(dst[i+1]);
            }
        }
        else if(messageEvent.getPath().equalsIgnoreCase(Constants.STOP)){
            //remove duplicate data points
            checkDuplicates(session_heartbeat != null && session_heartbeat.get("time").size()>0);
            //If at least one segment can be made
            if(session_accelerometer != null && session_accelerometer.get("x").size() > 52){
                try  {
                    //Get predictions
                    output = getActivityPredictions();
                    //Transform predictions to trantition array
                    trantitions = get_trantitions();
                    calculateSessionData(session_heartbeat != null && session_heartbeat.get("time").size()>0);
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

        session_accelerometer_preprocessed.get("time_delta").add((float) 0);
        session_accelerometer_preprocessed.get("time").add(session_accelerometer.get("time").get(0));
        session_accelerometer_preprocessed.get("x").add(session_accelerometer.get("x").get(0));
        session_accelerometer_preprocessed.get("y").add(session_accelerometer.get("y").get(0));
        session_accelerometer_preprocessed.get("z").add(session_accelerometer.get("z").get(0));

        float delta = 0;
        int j=1;
        for(int i=1; i < session_accelerometer.get("time").size(); i++){
            //If time is the same as previous
            if(!session_accelerometer.get("time").get(i).equals(session_accelerometer.get("time").get(i-1))){
                session_accelerometer_preprocessed.get("x").add(session_accelerometer.get("x").get(i));
                session_accelerometer_preprocessed.get("y").add(session_accelerometer.get("y").get(i));
                session_accelerometer_preprocessed.get("z").add(session_accelerometer.get("z").get(i));
                session_accelerometer_preprocessed.get("time").add(session_accelerometer.get("time").get(i));
                session_accelerometer_preprocessed.get("time_delta").add(delta);

                //Time in seconds since start
                j++;
                if(j%52 == 0){
                    delta++;
                }
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
        }
        session_accelerometer = session_accelerometer_preprocessed;
    }

    private void calculateSessionData(boolean heartrate) {
        //Calculate turns
        int turns = numberTurns();
        //Calculate mistakes
        List<Mistake> m = mistakesTimestamps_deravative();

        s.setWeek(user.getWeek());
        s.setTurns(turns);

        double totalMets = 0;

        int[] activityDuration = new int[JumpMoves.values().length];
        int[] activityMets = new int[JumpMoves.values().length];

        //activities
        for(int i = 0; i < trantitions.get("start").size(); i++){
            //Calculate MET score
            double met_score = 0;
            if(heartrate){
                met_score = processMETscore(trantitions.get("start").get(i), trantitions.get("end").get(i));
                met_score = BigDecimal.valueOf(met_score)
                        .setScale(2,RoundingMode.HALF_UP)
                        .doubleValue();
                totalMets += met_score;
            }

            Float start = Float.parseFloat(String.valueOf(trantitions.get("start_delta").get(i)));
            Float end = Float.parseFloat(String.valueOf(trantitions.get("end_delta").get(i)));
            int act = (int) Float.parseFloat(String.valueOf(trantitions.get("activity").get(i)));

            //Keep duration
            activityDuration[act] += (int) (end-start);
            //Keep mets
            activityMets[act] += (int) met_score;

            SessionActivity a = new SessionActivity();
            a.setEnd(end.longValue());
            a.setStart(start.longValue());
            a.setMET_score(met_score);
            a.setActivity(act);
            a.setWeek(user.getWeek());
            a.setUserId(user.getUid());

            activities.add(a);
        }

        s.setMets(totalMets);
        s.setMistakes(m.size());
        s.setUserId(user.getUid());

        //Insert session and activities
        long id = appDb.sessionDao().insertSession(s);
        for(SessionActivity a: activities){
            a.setSessionId((int) id);
            appDb.activityDao().insertActivity(a);
        }

        //Insert mistakes
        for(Mistake mk: m){
            mk.setSessionId((int) id);
            mk.setWeek(user.getWeek());
            mk.setUserId(app.getAccount().getId());
            appDb.mistakeDao().insertMistake(mk);
        }

        Recommendation[] pending = appDb.recommendationDao().getPendingRecommendation(true, user.getUid());

        //Check if pending recommendation is completed by session
        int fulfilledRecommendation = -1;
        for(int i = 0; i < pending.length; i++){
            if(pending[i].getDuration() <= activityDuration[pending[i].getActivity()] ){
                fulfilledRecommendation = i;
            }
        }
        //Set fulfilled recommendations as done
        if(fulfilledRecommendation != -1){
            Recommendation d = pending[fulfilledRecommendation];
            d.setDone(true);
            d.setPending(false);
            appDb.recommendationDao().updateRecommendation(d);
        }
    }

    //Get predictions from 4 models
    private List<JumpMoves> getActivityPredictions() {
        File internal = getFilesDir();
        Interpreter interpreter1 = new Interpreter(new File(internal != null ? internal.getAbsolutePath() : null, "model1.tflite"));
        Interpreter interpreter2 = new Interpreter(new File(internal != null ? internal.getAbsolutePath() : null, "model2.tflite"));
        Interpreter interpreter3 = new Interpreter(new File(internal != null ? internal.getAbsolutePath() : null, "model3.tflite"));
        Interpreter interpreter4 = new Interpreter(new File(internal != null ? internal.getAbsolutePath() : null, "model4.tflite"));

        interpreter1.allocateTensors();
        interpreter2.allocateTensors();
        interpreter3.allocateTensors();
        interpreter4.allocateTensors();
        float[][][] segments = segmentation();

        List<JumpMoves> out = new ArrayList<>();
        int[] probabilityShape =
                interpreter1.getOutputTensor(0).shape();
        DataType probabilityDataType = interpreter1.getOutputTensor(0).dataType();


        for(float[][] segment: segments){
            float[][][] s = new float[1][segment.length][segment[0].length];
            s[0] = segment;
            TensorBuffer outputProbabilityBuffer1 = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
            TensorBuffer outputProbabilityBuffer2 = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
            TensorBuffer outputProbabilityBuffer3 = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
            TensorBuffer outputProbabilityBuffer4 = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

            interpreter1.run(s, outputProbabilityBuffer1.getBuffer().rewind());
            interpreter2.run(s, outputProbabilityBuffer2.getBuffer().rewind());
            interpreter3.run(s, outputProbabilityBuffer3.getBuffer().rewind());
            interpreter4.run(s, outputProbabilityBuffer4.getBuffer().rewind());
            //Get most probable label
            out.add(getMostProbable(outputProbabilityBuffer1.getFloatArray(), outputProbabilityBuffer2.getFloatArray(), outputProbabilityBuffer3.getFloatArray(), outputProbabilityBuffer4.getFloatArray()));
        }

        interpreter1.close();
        interpreter2.close();
        interpreter3.close();
        interpreter4.close();
        try{
            if(out.size()>4){
                //Get most common label excluding first and last few segments
                JumpMoves mode = findPopular(out.subList(2,out.size()-2));
                int i;
                //first and last 2 segments = mode
                for(i=0; i < 2; i++){
                    if(out.get(i) != mode){
                        out.set(i, mode);
                    }
                    if(out.get(out.size()-1-i) != mode){
                        out.set(out.size()-1-i, mode);
                    }
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        return out;
    }

    //Find most common in list
    private JumpMoves findPopular(List<JumpMoves> a) {

        Collections.sort(a);

        JumpMoves previous = a.get(0);
        JumpMoves popular = a.get(0);
        int count = 1;
        int maxCount = 1;

        for (int i = 1; i < a.size(); i++) {
            if (a.get(i) == previous)
                count++;
            else {
                if (count > maxCount) {
                    popular = a.get(i-1);
                    maxCount = count;
                }
                previous = a.get(i);
                count = 1;
            }
        }
        return count > maxCount ? a.get(a.size()-1) : popular;
    }

    //Get most probable by combining the 4 models
    private JumpMoves getMostProbable(float[] floatArray1, float[] floatArray2, float[] floatArray3, float[] floatArray4 ) {
        float[] floatArray = new float[floatArray1.length];
        for(int i=0; i<floatArray.length; i++){
            //To reduce common errors caused by jump fast -> multiply by 2
            if(i == JumpMoves.FAST.getValue()){
                floatArray[i] = (floatArray1[i]+floatArray2[i]+floatArray3[i]+floatArray4[i])*2;
            } else{
                floatArray[i] = (floatArray1[i]+floatArray2[i]+floatArray3[i]+floatArray4[i]);
            }
        }
        Map<Float, Integer> m = arrayToMap(floatArray);
        Arrays.sort(floatArray);
        return JumpMoves.getJump(m.get(floatArray[floatArray.length-1]));
    }

    //Map value on label
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

    //Calculate deravative of signal
    private float[] getDeravative(List<Float> signal, List<Float> time){
        float[] deravative = new float[signal.size()-1];
        for(int i=1; i < signal.size(); i++){
            deravative[i-1] = (signal.get(i) - signal.get(i-1))/(time.get(i) - time.get(i-1));
        }
        return deravative;
    }

    private List<Mistake> mistakesTimestamps_deravative(){
        float[] d = getDeravative(session_accelerometer.get("x"), session_accelerometer.get("time"));
        List<Mistake> mistakes = new ArrayList<>();

        double interval_high = Double.valueOf(0.0000001);
        double interval_low = -Double.valueOf(0.0000001);

        int start = -1;
        int threshold = 52; //number of datapoints needed to be classified as mistake
        for(int i = 1; i < d.length; i++){
            //from not in interval to in interval = start
            if(( (d[i] < interval_high && d[i] > interval_low)) && ((d[i-1] > interval_high || d[i-1] < interval_low)) ){
                start = i;
            }
            //from in interval to not in interval = end
            if( (d[i-1] < interval_high && d[i-1] > interval_low) && (d[i] > interval_high || d[i] < interval_low) ){
                if((start != -1) && ((i - start) >= threshold)){
                    Mistake mis = new Mistake();
                    int segment = getActivitySegmentForMistake(start);
                    if(segment != -1){
                        mis.setActivity((int) Float.parseFloat(String.valueOf(output.get(segment).getValue())));
                        mis.setTime(start/52);
                        mistakes.add(mis);
                    }
                    start = -1;
                }
            }
        }
        return mistakes;
    }

    //Get activity corresponding to time of mistake
    private int getActivitySegmentForMistake(int startIndex) {
        int segmentSize = (int) (Constants.SEGMENT_SIZE*Constants.WINDOW);
        for(int i = 0; i < output.size(); i++){
            int s = (i*segmentSize);
            int e = i+1 == output.size() ? session_accelerometer.get("time").size() : (i+1)*segmentSize;

            if( (startIndex >= s) && (startIndex < e)){
                return (i==0) ? i : i-1;
            }
        }
        return -1;
    }

    //Count local maxima of signal
    private int localMaxima(float[] a) {
        int count = 0;
        try{
            for (int i = 1; i < a.length - 1; i++)
            {
                if(a[i] > a[i - 1] && a[i] > a[i + 1])
                    count += 1;
            }
            return count;
        } catch (Exception e){
            e.printStackTrace();
        }
        return count;
    }

    //Calculate number of turns by using Savgol filter
    private int numberTurns(){
        Map<JumpMoves, Integer> turns = new HashMap<>();
        turns.put(JumpMoves.SLOW, 0);
        turns.put(JumpMoves.FAST, 0);
        turns.put(JumpMoves.CROSS_OVER, 0);
        turns.put(JumpMoves.SIDE_SWING, 0);
        turns.put(JumpMoves.RUN, 0);

        float [] x_savgol,y_savgol, z_savgol;
        int x_turns, y_turns, z_turns;
        int sum_turns = 0;

        try{
            for(int i = 0; i < trantitions.get("start").size(); i++){
                int act = (int) Float.parseFloat(String.valueOf(trantitions.get("activity").get(i)));
                if(act != -1){
                    int s = session_accelerometer.get("time").indexOf(trantitions.get("start").get(i)) == -1 ?  session_accelerometer.get("time").size()-1 : session_accelerometer.get("time").indexOf(trantitions.get("start").get(i));
                    int e = session_accelerometer.get("time").indexOf(trantitions.get("end").get(i)) == -1 ?  session_accelerometer.get("time").size()-1 : session_accelerometer.get("time").indexOf(trantitions.get("end").get(i));
                    SavGolFilter f = filters.get(JumpMoves.getJump(act));

                    if(act == JumpMoves.SIDE_SWING.getValue()){
                        z_savgol = f.filterData(Floattofloat(session_accelerometer.get("z").subList(s, e)));

                        for(int j = 0; j < iterations.get(JumpMoves.getJump(act)); j++){
                            z_savgol = f.filterData(z_savgol);
                        }

                        z_turns = localMaxima(z_savgol);

                        turns.put(JumpMoves.getJump(act),turns.get(JumpMoves.getJump(act)) + (z_turns));
                    } else{
                        x_savgol = f.filterData(Floattofloat(session_accelerometer.get("x").subList(s, e)));
                        y_savgol = f.filterData(Floattofloat(session_accelerometer.get("y").subList(s, e)));
                        z_savgol = f.filterData(Floattofloat(session_accelerometer.get("z").subList(s, e)));

                        for(int j = 0; j < iterations.get(JumpMoves.getJump(act)); j++){
                            x_savgol = f.filterData(x_savgol);
                            y_savgol = f.filterData(y_savgol);
                            z_savgol = f.filterData(z_savgol);
                        }

                        x_turns = localMaxima(x_savgol);
                        y_turns = localMaxima(y_savgol);
                        z_turns = localMaxima(z_savgol);

                        turns.put(JumpMoves.getJump(act),turns.get(JumpMoves.getJump(act)) + ((x_turns+y_turns+z_turns)/3));
                    }
                }
            }

            for(int t: turns.values()){
                sum_turns += t;
            }

            return sum_turns;
        } catch(Exception e){
            e.printStackTrace();
        }
        return  0;
    }

    //Calculate MET score
    private double processMETscore(Float start, Float end){
        BigDecimal s = new BigDecimal(start);
        BigDecimal e = new BigDecimal(end);

        List<Float> heartRateFiltered = new ArrayList<>();
        List<Float> timeFiltered = session_heartbeat.get("time").stream()
                .filter(t -> {
                    BigDecimal b = new BigDecimal(t);
                    return ((b.compareTo(s) == 1 ) && (b.compareTo(e) == -1));
                        }
                )
                .collect(Collectors.toList());
        List<Float> timeFiltered_ = new CopyOnWriteArrayList<Float>(timeFiltered);
        for(Float t: timeFiltered_){
            Float i = session_heartbeat.get("HR").get(session_heartbeat.get("time").indexOf(t));
            if(i == 0){
                timeFiltered_.remove(t);
            } else{
                heartRateFiltered.add(i);
            }
        }

        //MPA = ligthzone, MPV = moderate zone
        double timeMPA, timeMPV;
        double sumMETmin = 0;
        timeMPA = 0;
        timeMPV = 0;
        for(int i = 1; i < heartRateFiltered.size() ; i++){
                if((getHeartRateZone(heartRateFiltered.get(i)) == getHeartRateZone(heartRateFiltered.get(i-1)))){
                    if(getHeartRateZone(heartRateFiltered.get(i)) == Constants.lightZone){
                            timeMPA += timeFiltered.get(i) - timeFiltered.get(i-1);
                    } else if(getHeartRateZone(heartRateFiltered.get(i)) == Constants.moderateZone){
                            timeMPV += timeFiltered.get(i) - timeFiltered.get(i-1);
                    }
                }
        }
        timeMPA = (timeMPA * Math.pow(10,-9))/60;
        timeMPV = (timeMPV * Math.pow(10,-9))/60;
        sumMETmin += 4 * timeMPA + 8 * timeMPV;

        return sumMETmin;
    }

    private int getHeartRateZone(double v){
        if( (v >= 0.5*MAXHR) && (v < 0.6*MAXHR) ){
            return Constants.veryLightZone;
        } else if( (v >= 0.6*MAXHR) && (v < 0.7*MAXHR) ){
            return Constants.lightZone;
        } else if( (v >= 0.7*MAXHR) && (v < 0.8*MAXHR) ){
            return Constants.moderateZone;
        } else if( (v >= 0.8*MAXHR) && (v < 0.9*MAXHR) ){
            return Constants.hardZone;
        } else if( (v >= 0.9*MAXHR) && (v < MAXHR) ){
            return Constants.maximumZone;
        }
        return 0;
    }

    /*
    ACTIVITY RECOGNITION: PREPROCESSING
     */

    private float[][][] segmentation(){
        int FRAME_SIZE = (Constants.SEGMENT_SIZE * Constants.WINDOW);

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

    private Map<String, List<Float>> get_trantitions() {
        activityOutput = makeActivities();
        int FRAME_SIZE = (Constants.SEGMENT_SIZE * Constants.WINDOW);
        //start, end, activiteit
        Map<String, List<Float>> trantitions_ = new HashMap<>();
        trantitions_.put("start_delta", new ArrayList<>());
        trantitions_.put("end_delta", new ArrayList<>());
        trantitions_.put("start", new ArrayList<>());
        trantitions_.put("end", new ArrayList<>());
        trantitions_.put("activity", new ArrayList<>());

        //Get indexes in order of when activity started
        List<Integer> indexes=new ArrayList(activityOutput.keySet());
        Collections.sort(indexes);

        float end_delta, start_delta, end, start;
        if(indexes.size() == 1){
            int numberSensorSamples = (output.size())*FRAME_SIZE;
            start_delta = session_accelerometer.get("time_delta").get(0);
            end_delta = session_accelerometer.get("time_delta").get(numberSensorSamples);
            start = session_accelerometer.get("time").get(0);
            end = session_accelerometer.get("time").get(numberSensorSamples);
            trantitions_.get("start_delta").add(start_delta);
            trantitions_.get("end_delta").add(end_delta);
            trantitions_.get("start").add(start);
            trantitions_.get("end").add(end);
            trantitions_.get("activity").add((float) activityOutput.get(0).getValue());
        } else{
            int indexTime = 0;
            for (int i = 0; i<indexes.size()-1; i++) {
                int numberSensorSamples = (indexes.get(i + 1) - indexes.get(i))*FRAME_SIZE;
                start_delta = session_accelerometer.get("time_delta").get(indexTime);
                end_delta = session_accelerometer.get("time_delta").get(indexTime+numberSensorSamples);
                start = session_accelerometer.get("time").get(indexTime);
                end = session_accelerometer.get("time").get(indexTime+numberSensorSamples);
                trantitions_.get("start_delta").add(start_delta);
                trantitions_.get("end_delta").add(end_delta);
                trantitions_.get("start").add(start);
                trantitions_.get("end").add(end);
                trantitions_.get("activity").add((float) activityOutput.get(indexes.get(i)).getValue());

                indexTime = indexTime+numberSensorSamples;
            }
            trantitions_.get("start").add(session_accelerometer.get("time").get(indexTime));
            trantitions_.get("end").add(session_accelerometer.get("time").get(session_accelerometer.get("time").size()-1));
            trantitions_.get("start_delta").add(session_accelerometer.get("time_delta").get(indexTime));
            trantitions_.get("end_delta").add(session_accelerometer.get("time_delta").get(session_accelerometer.get("time_delta").size()-1));
            trantitions_.get("activity").add((float) activityOutput.get(indexes.get(indexes.size()-1)).getValue());
        }
        return trantitions_;
    }

    //map start index from output array on activity that begins then
    //OUTPUT: index1 -> activity1, index2 -> activity2....
    private Map<Integer,JumpMoves> makeActivities(){
        Map<Integer,JumpMoves> t = new HashMap<>();
        int start = 0;
        for (int i = 1; i < output.size(); i++) {
            if(output.get(i) != output.get(i-1)){
                t.put(start, output.get(start));
                start = i;
            }
        }
        t.put(start, output.get(start));

        return t;
    }

    private float[] Floattofloat(List<Float> l){
        float[] floatArray = new float[l.size()];
        int i = 0;

        for (Float f : l) {
            floatArray[i++] = (f != null ? f : Float.NaN);
        }
        return floatArray;
    }
}
