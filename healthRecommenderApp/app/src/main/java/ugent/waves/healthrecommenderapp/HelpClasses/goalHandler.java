package ugent.waves.healthrecommenderapp.HelpClasses;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import androidx.work.WorkerParameters;
import ugent.waves.healthrecommenderapp.Persistance.ActivityDao;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Mistake;
import ugent.waves.healthrecommenderapp.Persistance.MistakeDao;
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.Persistance.RecommendationDao;
import ugent.waves.healthrecommenderapp.Persistance.Session;
import ugent.waves.healthrecommenderapp.Persistance.SessionActivity;
import ugent.waves.healthrecommenderapp.Persistance.SessionDao;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

//TODO: in snooze alle entries ouder dan 10 weken verwijderen
//TODO: week instellen
//TODO: goal instellen
//TODO: initiele setup van week en goal
public class goalHandler {//extends Worker {

    private healthRecommenderApplication app;
    private FirebaseFirestore db;
    private Context context;
    private double goal;
    private ScoreCalculation r;

    private AppDatabase appDb;

    private Map<Integer, Double> score;

    public goalHandler(WorkerParameters params, Context context, healthRecommenderApplication app){
        //super(context, params);
        this.context = context;
        this.app =  app;
        this.db = app.getFirestore();
        this.score = new HashMap<>();
        this.appDb = app.getAppDb();
    }

    //TODO: controleren
    private double getPercentile(List<Double> mets, double Percentile) {
        Collections.sort(mets);
        int i = (int)Math.floor((Percentile / (double)100) * (double)mets.size());
        return mets.get(i);
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

            //if not enough data fill with defailt value of 600
            if(score.size() < 10){
                for(int i = 0; i < 10-score.size(); i++){
                    scores.add((double) 600);
                }
            }

            //new goal is 60th percentile of distribution
            goal = getPercentile(scores, 60);
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
        try {
            //TODO: door async niet ok??
            new RecommendationAsyncTask((Activity) context, appDb).execute();
            SessionActivity[] activities = new ActivityAsyncTask((Activity) context, appDb).execute().get();

            //mistakes in last 10 weeks
            Mistake[] mistakes = new MistakeAsyncTask((Activity) context, appDb, app.getWeeknr()-10).execute().get();

            //Count
            Map<Integer, Integer> activity_count = new HashMap<>();
            //duration
            Map<Integer, Long> activity_duration = new HashMap<>();
            //mets
            Map<Integer, Double> activity_mets = new HashMap<>();

            for(SessionActivity sa: activities){
                if(!activity_count.containsKey(sa.getActivity())){
                    activity_count.put(sa.getActivity(), 0);
                }
                activity_count.put(sa.getActivity(), activity_count.get(sa.getActivity()) + 1);

                //sum duration
                if(!activity_duration.containsKey(sa.getActivity())){
                    activity_duration.put(sa.getActivity(), (long) 0);
                }
                //sum mets
                if(!activity_mets.containsKey(sa.getActivity())){
                    activity_mets.put(sa.getActivity(), (double) 0);
                }
                activity_duration.put(sa.getActivity(), activity_duration.get(sa.getActivity()) + (sa.getEnd()-sa.getStart()));
                activity_mets.put(sa.getActivity(), activity_mets.get(sa.getActivity()) + sa.getMET_score());
            }

            //metsPerMin
            Map<Integer, Double> activity_metsPerSec = new HashMap<>();

            Map<Integer, Long> activity_mean_duration = new HashMap<>();

            for(int c: activity_count.keySet()){
                //mean duration
                long meanDuration = activity_duration.get(c)/activity_count.get(c);

                //mean mets
                double meanMets = activity_mets.get(c)/activity_count.get(c);

                double metsPerSec = meanMets/meanDuration;
                activity_metsPerSec.put(c, metsPerSec);
                activity_mean_duration.put(c, meanDuration);
            }

            //1 keer gewichten berekenen
            Map<Integer, Integer> weights = calculateWeights(mistakes, activity_count);

            int totalWeight = 0;
            for (int i : weights.keySet()) {
                totalWeight += weights.get(i);
            }

            //recommendations
            double recommendedMets = 0;

            //recommendations nummeren zodat 1 willekeurige kan opgevraagd worden uit de db
            int number = 0;

            //TODO: wat als nog geen activiteiten
            //TODO: met goal uit app
            while(recommendedMets < 300){
                int act = getRecommendedActivity(weights, totalWeight);
                Long mean = activity_mean_duration.get(act);
                double metsPerSec = activity_metsPerSec.get(act);

                double mets = (double) mean * metsPerSec;

                Recommendation r = new Recommendation();
                r.setActivity(act);
                r.setDuration(activity_mean_duration.get(act));
                r.setMets(mets);
                r.setNr(number);
                r.setPending(false);
                r.setDone(false);

                AsyncTask.execute(() -> appDb.recommendationDao().insertRecommendation(r));

                recommendedMets += mets;
                number++;
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //TODO: test with model
    private int getRecommendedActivity(Map<Integer, Integer> weights, int totalWeight){

        int randomAct = -1;
        double random = Math.random() * totalWeight;
        for (int i: weights.keySet())
        {
            random -= weights.get(i);
            if (random <= 0.0d)
            {
                randomAct = i;
                break;
            }
        }
        return randomAct;
        /*
        List<Integer> distribution = new ArrayList<>();
        for(int c: counts.keySet()){
            //TODO: afronding niet ok
            double procent = counts.get(c)/total*10;

            for(int i = 0; i < procent; i++){
                distribution.add(c);
            }
        }
        return distribution.get(rand.nextInt(distribution.size()));*/
    }

    //enkel laatste 10 weken zodat verandering in rekening kan gebracht worden
    private Map<Integer, Integer> calculateWeights(Mistake[] m, Map<Integer, Integer> c) {
        Map<Integer, Integer> mistakeCounts = new HashMap<>();
        for(Mistake mis: m){
            if(!mistakeCounts.containsKey(mis.getActivity())){
                    mistakeCounts.put(mis.getActivity(), 0);
            }
            mistakeCounts.put(mis.getActivity(), mistakeCounts.get(mis.getActivity()) + 1);
        }

        Map<Integer, Integer> w = new HashMap<>();

        //als er fouten gemaakt zijn, meer gewicht geven
        for(int i: c.keySet()){
            if(mistakeCounts.containsKey(i)){
                w.put(i, c.get(i)*mistakeCounts.get(i));
            } else{
                w.put(i, c.get(i));
            }
        }
        return w;
    }


    private static class MistakeAsyncTask extends AsyncTask<Void, Void, Mistake[]> {
        //Prevent leak
        private WeakReference<Activity> weakActivity;
        private AppDatabase db;
        private int week;

        public MistakeAsyncTask(Activity activity, AppDatabase db, int week) {
            weakActivity = new WeakReference<>(activity);
            this.db = db;
            this.week = week;
        }

        @Override
        protected Mistake[] doInBackground(Void... params) {
            MistakeDao mistakeDao = db.mistakeDao();
            return mistakeDao.getMistakesFromWeek(week);
        }
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
    }

    private static class RecommendationAsyncTask extends AsyncTask<Void, Void, Void> {
        //Prevent leak
        private WeakReference<Activity> weakActivity;
        private AppDatabase db;

        public RecommendationAsyncTask(Activity activity, AppDatabase db) {
            weakActivity = new WeakReference<>(activity);
            this.db = db;
        }

        @Override
        protected Void doInBackground(Void... params) {
            RecommendationDao recommendationDao = db.recommendationDao();
            recommendationDao.deleteAllRecommendations();
            return null;
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
