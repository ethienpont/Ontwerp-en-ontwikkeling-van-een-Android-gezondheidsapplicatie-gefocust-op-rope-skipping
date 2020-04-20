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

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import ugent.waves.healthrecommenderapp.Enums.JumpMoves;
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

//TODO: WEKELIJKS: week instellen, goal instellen, in snooze alle entries ouder dan 10 weken verwijderen, recommendations genereren
//TODO: DAGELIJKS: timestill in app resetten
//TODO: initiele setup van week en goal
public class goalHandler extends Worker {

    private healthRecommenderApplication app;
    private FirebaseFirestore db;
    private Context context;
    private double goal;
    private ScoreCalculation r;

    private AppDatabase appDb;

    private Map<Integer, Double> score;

    public goalHandler(Context context, WorkerParameters params){
        super(context, params);
        this.app =  (healthRecommenderApplication) getApplicationContext();
        this.context = app.getContext();
        this.score = new HashMap<>();
        this.appDb = app.getAppDb();
    }

    @NonNull
    @Override
    public Result doWork() {
        //set week
        app.setWeeknr(app.getWeeknr()+1);
        //set goal
        app.setGoal((int)calculateGoal());

        //TODO: data niet verwijderen?
        appDb.snoozeDao().deleteAllFromWeek(app.getWeeknr()-10);

        generateRecommendations();

        return Result.retry(); //failure of retry
    }

    //TODO: in papers documentatie zoeken over 60 percentiel methode
    private double getPercentile(List<Double> mets, double Percentile) {
        Collections.sort(mets);
        int i = (int)Math.floor((Percentile / (double)100) * (double)mets.size());
        return mets.get(i);
    }

    //10 weken sessies inlezen
    private double calculateGoal(){
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

            //TODO: recommended mets per week nakijken
            //if not enough data fill with default value of 600
            if(score.size() < 10){
                for(int i = 0; i < 10-score.size(); i++){
                    scores.add((double) 600);
                }
            }

            //new goal is 60th percentile of distribution
            return getPercentile(scores, 60);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 600;
    }

    private List<Double> CollectionToList(Collection<Double> c){
        List<Double> list = new ArrayList<>();
        for(Double d: c){
            list.add(d);
        }
        return list;
    }

    //TODO: wat doen met data ouder dan 10 weken (activiteiten, sessions)?
    //TODO: test act per sessie
    //TODO: duration in sec?
    public void generateRecommendations(){
        try {
            //TODO: door async niet ok??
            //recommendations van vorige week verwijderen
            new RecommendationAsyncTask((Activity) context, appDb).execute();

            //activiteiten van voorbije 10 weken
            SessionActivity[] activities = new ActivityAsyncTask((Activity) context, appDb, app.getWeeknr()-10).execute().get();

            //mistakes in last 10 weeks
            Mistake[] mistakes = new MistakeAsyncTask((Activity) context, appDb, app.getWeeknr()-10).execute().get();

            //Count
            Map<Integer, Integer> activity_count = new HashMap<>();
            Map<Integer, Integer> previous_session_id = new HashMap<>();
            //duration
            Map<Integer, Long> activity_duration = new HashMap<>();
            //mets
            Map<Integer, Double> activity_mets = new HashMap<>();

            for(SessionActivity sa: activities){
                //als er geen hartslagdata, wordt act niet meegerekend
                if(sa.getMET_score() != -1){
                    if(!activity_count.containsKey(sa.getActivity())){
                        activity_count.put(sa.getActivity(), 0);
                    }
                    //tel hoeveel keer act voorkomt in sessies
                    if(!previous_session_id.containsKey(sa.getActivity()) || previous_session_id.get(sa.getActivity()) != sa.getSessionId()){
                        activity_count.put(sa.getActivity(), activity_count.get(sa.getActivity()) + 1);
                    }
                    previous_session_id.put(sa.getActivity(), sa.getSessionId());

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

            JumpMoves[] j = JumpMoves.values();

            //als niet alle activiteiten aanwezig zijn
            if(weights.size() != j.length){
                for(JumpMoves m: j){
                    if(!weights.containsKey(m.getValue())){
                        //geef default gewicht 1
                        weights.put(m.getValue(), 1);
                    }
                }
            }

            int totalWeight = 0;
            for (int i : weights.keySet()) {
                totalWeight += weights.get(i);
            }

            //recommendations
            double recommendedMets = 0;

            //TODO: wat als nog geen activiteiten: gewicht 1 -> TESTEN
            while(recommendedMets < app.getGoal()){
                int act = getRecommendedActivity(weights, totalWeight);
                //act werd nog niet beoefend
                double mets;
                Long duration;

                //TODO: TESTEN
                if(!activity_mean_duration.containsKey(act)){
                    //standaar duur van 10 tot 30 min
                    duration = (long) (Math.random() * (30 - 10)) + 10;
                    mets = getMetsForDuration(duration);

                } else{
                    duration = activity_mean_duration.get(act);
                    double metsPerSec = activity_metsPerSec.get(act);

                    mets = (double) duration * metsPerSec;
                }

                Recommendation r = new Recommendation();
                r.setActivity(act);
                r.setDuration(duration);
                r.setMets(mets);
                r.setPending(false);
                r.setDone(false);

                AsyncTask.execute(() -> appDb.recommendationDao().insertRecommendation(r));

                recommendedMets += mets;
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private double getMetsForDuration(Long duration) {
        double timeMPA = Math.random() * duration;
        double timeMPV = duration - timeMPA;

        double METmin = 4 * timeMPA + 8 * timeMPV;

        return METmin;
    }

    //TODO: test with model
    //TODO: randomact is -1?
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
        return randomAct == -1 ?  0 : randomAct;
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

    //TODO: gewichten ok?
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

        //count activiteit * aantal fouten tijdes activiteit
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
        private int week;

        public ActivityAsyncTask(Activity activity, AppDatabase db, int week) {
            weakActivity = new WeakReference<>(activity);
            this.db = db;
            this.week = week;
        }

        @Override
        protected SessionActivity[] doInBackground(Void... params) {
            ActivityDao activityDao = db.activityDao();
            return activityDao.getActivitiesFromWeek(week);
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
}
