package ugent.waves.healthrecommenderapp.HelpClasses;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import androidx.work.WorkerParameters;
import ugent.waves.healthrecommenderapp.Persistance.ActivityDao;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.Persistance.Session;
import ugent.waves.healthrecommenderapp.Persistance.SessionActivity;
import ugent.waves.healthrecommenderapp.Persistance.SessionDao;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

public class goalHandler {//extends Worker {

    private healthRecommenderApplication app;
    private FirebaseFirestore db;
    private Context context;
    private double goal;
    private ScoreCalculation r;

    private AppDatabase appDb;

    private Map<Integer, Double> score;
    private Random rand;
    //"start" -> [], "end" -> [], "activity" -> []
    //private Map<String, List<String>>

    public goalHandler(WorkerParameters params, Context context, healthRecommenderApplication app){
        //super(context, params);
        this.context = context;
        this.app =  app;
        this.db = app.getFirestore();
        this.score = new HashMap<>();
        this.appDb = app.getAppDb();
        this.rand = new Random();
    }

    //TODO: wss verkeerd
    private double Percentile(List<Double> mets, double Percentile)
    {
        Collections.sort(mets);
        int Index = (int)Math.ceil(((double)Percentile / (double)100) * (double)mets.size());
        return mets.get(Index-1);
    }

    private double getNewGoal(List<Double> s){
        return Percentile(s, 0.60);
    }

    //10 weken sessies inlezen
    private void calculateGoal(){
        try {
            Session[] sessions = new SessionAsyncTask((Activity) context, app.getWeeknr()-10, appDb).execute().get();
            for(Session s: sessions){
                if(!score.containsKey(s.getWeek())){
                    score.put(s.getWeek(), s.getMets());
                } else{
                    score.put(s.getWeek(), score.get(s.getWeek()) + s.getMets());
                }
            }
            List<Double> scores = CollectionToList(score.values());
            if(score.size() < 10){
                for(int i = 0; i < 10-score.size(); i++){
                    scores.add((double) 600);
                }
            }
            goal = getNewGoal(scores);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Task<QuerySnapshot> readSessionData(){
        //weeknr -> score
        Task<QuerySnapshot> t = db.collection("users")
                .document("testUser")
                .collection("sessions")
                .whereGreaterThanOrEqualTo("week", app.getWeeknr()-10)
                .get();

        return t;
    }

    private Task<QuerySnapshot> get_activity_data(String id){
        //haal activities op
        Task<QuerySnapshot>  t = db.collection("users")
                .document("testUser")
                .collection("sessions")
                .document(id)
                .collection("activities")
                .get();

        return t;
    }

    public void getActivityData(){
        try {
            SessionActivity[] activities = new ActivityAsyncTask((Activity) context, appDb).execute().get();
            Log.d("d", "d");
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private List<Double> CollectionToList(Collection<Double> c){
        List<Double> list = new ArrayList<>();
        for(Double d: c){
            list.add(d);
        }
        return list;
    }

    public void generateRecommendations(){
        //TODO: delete previous recommendations
        try {
            SessionActivity[] activities = new ActivityAsyncTask((Activity) context, appDb).execute().get();

            //Count
            Map<String, Integer> activity_count = new HashMap<>();
            //duration
            Map<String, Long> activity_duration = new HashMap<>();
            //mets
            Map<String, Double> activity_mets = new HashMap<>();

            for(SessionActivity sa: activities){
                if(!activity_count.containsKey(sa.getActivity()+"")){
                    activity_count.put(sa.getActivity()+"", 0);
                }
                activity_count.put(sa.getActivity()+"", activity_count.get(sa.getActivity()+"") + 1);

                //sum duration
                if(!activity_duration.containsKey(sa.getActivity()+"")){
                    activity_duration.put(sa.getActivity()+"", (long) 0);
                }
                //sum mets
                if(!activity_mets.containsKey(sa.getActivity()+"")){
                    activity_mets.put(sa.getActivity()+"", (double) 0);
                }
                activity_duration.put(sa.getActivity()+"", activity_duration.get(sa.getActivity()+"") + (sa.getEnd()-sa.getStart()));
                activity_mets.put(sa.getActivity()+"", activity_mets.get(sa.getActivity()+"") + sa.getMET_score());
            }

            //metsPerMin
            Map<String, Double> activity_metsPerMin = new HashMap<>();

            for(String c: activity_count.keySet()){
                //mean duration
                long meanDuration = activity_duration.get(c)/activity_count.get(c);

                //mean mets
                double meanMets = activity_mets.get(c)/activity_count.get(c);

                double metsPerMin = metsPerMin(meanDuration, meanMets);
                activity_metsPerMin.put(c, metsPerMin);
            }

            //recommendations
            double recommendedMets = 0;

            //TODO: wat als nog geen activiteiten
            while(recommendedMets < 100){
                String act = getRecommendedActivity(activity_count, activities.length);
                //TODO: duration in min
                long duration = (long) ( 0.2 + (3 - 0.2) * rand.nextDouble());
                double mets = duration * activity_metsPerMin.get(act);

                Recommendation r = new Recommendation();
                r.setActivity(Integer.parseInt(act));
                r.setDuration(duration);
                r.setMets(mets);

                AsyncTask.execute(() -> appDb.recommendationDao().insertRecommendation(r));

                recommendedMets += mets;
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String getRecommendedActivity(Map<String, Integer> counts, int total){
        List<String> distribution = new ArrayList<>();
        for(String c: counts.keySet()){
            //TODO: afronding niet ok
            double procent = counts.get(c)/total*10;

            for(int i = 0; i < procent; i++){
                distribution.add(c);
            }
        }
        return distribution.get(rand.nextInt(distribution.size()));
    }

    private double metsPerMin(long time, double mets){
        return 0;
    }


    private static class SessionAsyncTask extends AsyncTask<Void, Void, Session[]> {
        //Prevent leak
        private WeakReference<Activity> weakActivity;
        private AppDatabase db;
        private int week;

        public SessionAsyncTask(Activity activity, int week ,AppDatabase db) {
            weakActivity = new WeakReference<>(activity);
            this.db = db;
            this.week = week;
        }

        @Override
        protected Session[] doInBackground(Void... params) {
            SessionDao sessionDao = db.sessionDao();
            return sessionDao.getSessionsFromWeek(week);
        }

        @Override
        protected void onPostExecute(Session[] agentsCount) {

        }
    }

    private static class ActivityAsyncTask extends AsyncTask<Void, Void, SessionActivity[]> {
        //Prevent leak
        private WeakReference<Activity> weakActivity;
        private AppDatabase db;

        public ActivityAsyncTask(Activity activity, AppDatabase db) {
            weakActivity = new WeakReference<>(activity);
            this.db = db;
        }

        @Override
        protected SessionActivity[] doInBackground(Void... params) {
            ActivityDao activityDao = db.activityDao();
            return activityDao.getAllActivities();
        }

        @Override
        protected void onPostExecute(SessionActivity[] agentsCount) {

        }
    }




    /*
    public void postGoalReached(){
        //TODO: deze logica moet 1 keer per week uitgevoerd worden
        //TODO: als er te weinig entries zijn, goal in app berekenen
        //TODO: als bereikte aantal hoger is dan goal
        Calendar cal = Calendar.getInstance();
        long end = app.getNowMilliSec(cal);
        long start = app.getWeeksAgoMilliSec(cal, 3);
        ScoreCalculation r = new ScoreCalculation(account, context, 21);
        r.getHistory(start, end, DataType.TYPE_HEART_RATE_BPM)
                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                    @Override
                    public void onSuccess(SessionReadResponse dataReadResponse) {
                        double score = r.processMETscore(dataReadResponse);
                        int g = rand.nextInt(700)+100;
                        int reach = rand.nextInt(g);
                        Map<String, Object> goal = new HashMap<>();
                        goal.put("week_number", 0);
                        goal.put("mets_goal", g);
                        goal.put("mets_reached", reach);

                        //TODO: firestore populating with testdata
                        //TODO: document moet naast collectie ook een veld hebben om zichtbaar te zijn
                        for(int i= 0; i<52; i++){
                            db.collection("users")
                                    .document(user.getUid())
                                    .collection("goals")
                                    .document(i+"") //TODO: wrong week of year //cal.get(Calendar.WEEK_OF_YEAR)+""
                                    .set(goal)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("kk", "DocumentSnapshot successfully written!");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w("l", "Error writing document", e);
                                        }
                                    });
                            g = rand.nextInt(700)+100;
                            reach = rand.nextInt(g);
                            goal.put("week_number", i+1);
                            goal.put("mets_goal", g);
                            goal.put("mets_reached", reach);
                        }
                    }
                });
    }

    public void setNewGoal(int week){
        DocumentReference doc = db.collection("users")
                .document(user.getUid())
                .collection("goals")
                .document(week+"");

        doc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("l", document.get("mets_goal").toString());
                        app.setWeeknr(week);
                        app.setGoal(((Long) document.get("mets_goal")).intValue());
                    } else {
                        Log.d("l", "No such document");
                    }
                } else {
                    Log.d("l", "get failed with ", task.getException());
                }
            }
        });
    }*/

    /*
    private Task<SessionReadResponse> getHistoryOneWeekAgo(){
        Calendar cal = Calendar.getInstance();

        long end = app.getNowMilliSec(cal);
        long start = app.getWeeksAgoMilliSec(cal, 1);
        Log.e("l", "START"+new Date(start).toString());
        Log.e("l", new Date(end).toString());
        return r.getHistory(start, end, DataType.TYPE_HEART_RATE_BPM);
    }

    private Task<SessionReadResponse> getHistoryTwoWeekAgo(){
        Calendar cal = Calendar.getInstance();
        app.getNowMilliSec(cal);

        long end = app.getWeeksAgoMilliSec(cal, 1);
        long start = app.getWeeksAgoMilliSec(cal, 1);
        Log.e("l","START"+ new Date(start).toString());
        Log.e("l", new Date(end).toString());
        return r.getHistory(start, end, DataType.TYPE_HEART_RATE_BPM);
    }

    private Task<SessionReadResponse> getHistoryThreeWeekAgo(){
        Calendar cal = Calendar.getInstance();
        app.getNowMilliSec(cal);

        long end = app.getWeeksAgoMilliSec(cal, 2);
        long start = app.getWeeksAgoMilliSec(cal, 1);
        Log.e("l", "START"+new Date(start).toString());
        Log.e("l", new Date(end).toString());
        return r.getHistory(start, end, DataType.TYPE_HEART_RATE_BPM);
    }

    //TODO: nieuwe doel berekenen door gemiddelde te nemen voorbije 3 weken?
    public void calculateNewGoal(){

        getHistoryOneWeekAgo()
                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                    @Override
                    public void onSuccess(SessionReadResponse dataReadResponse) {
                        score.add(0, r.processMETscore(dataReadResponse));
                        getHistoryTwoWeekAgo()
                                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                                    @Override
                                    public void onSuccess(SessionReadResponse dataReadResponse) {
                                        score.add(1, r.processMETscore(dataReadResponse));
                                        getHistoryThreeWeekAgo()
                                                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                                                    @Override
                                                    public void onSuccess(SessionReadResponse dataReadResponse) {
                                                        score.add(2, r.processMETscore(dataReadResponse));
                                                        app.setGoal((int)(score.get(0) + score.get(1) + score.get(2))/3);
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }*/

    /*
    @NonNull
    @Override
    public Result doWork() {
        calculateGoal();

        return null;
    }*/
}
