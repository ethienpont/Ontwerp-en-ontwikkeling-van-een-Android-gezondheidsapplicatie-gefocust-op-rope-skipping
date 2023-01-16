package ugent.waves.healthrecommenderapp.HelpClasses;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import ugent.waves.healthrecommenderapp.Enums.JumpMoves;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Mistake;
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.Persistance.Session;
import ugent.waves.healthrecommenderapp.Persistance.SessionActivity;
import ugent.waves.healthrecommenderapp.Persistance.User;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

public class goalHandler extends Worker {

    private AppDatabase appDb;

    public goalHandler(@NonNull Context context, @NonNull WorkerParameters params){
        super(context, params);
        healthRecommenderApplication app = (healthRecommenderApplication) context.getApplicationContext();
        this.appDb = app.getAppDb();
    }

    @Override
    @NonNull
    public Result doWork() {
        User u = appDb.userDao().getCurrent(true);
        int goal = (int) calculateGoal(u.getWeek(), u.getUid());
        u.setWeek(u.getWeek() + 1);

        u.setGoal(goal);

        generateRecommendations(goal, u.getWeek(), u.getUid());

        appDb.userDao().updateUser(u);

        appDb.activityDao().deleteActivities(u.getWeek()-10, u.getUid());
        appDb.mistakeDao().deleteMistakes(u.getWeek()-10, u.getUid());
        appDb.sessionDao().deleteSessions(u.getWeek()-10, u.getUid());

        return Result.success();
    }

    //Calculate new goal based on historic MET data
    private double calculateGoal(int week, String id){
        Session[] sessions = appDb.sessionDao().getSessionsFromWeek(week-10, id);
        Map<Integer, Double> score = new HashMap<>();
        for(Session s: sessions){
            if(!score.containsKey(s.getWeek())){
                score.put(s.getWeek(), s.getMets());
            } else{
                score.put(s.getWeek(), score.get(s.getWeek()) + s.getMets());
            }
        }
        List<Double> scores = CollectionToList(score.values());

        //If not enough data fill with default value of 600
        if(score.size() < 10){
            for(int i = 0; i < 10-score.size(); i++){
                scores.add(Constants.GOAL);
            }
        }

        return getMean(scores);
    }

    private double getMean(List<Double> scores) {
        double sum=0;
        for(int i=0; i<scores.size(); i++){
            sum += scores.get(i);
        }
        return sum/scores.size();
    }

    private List<Double> CollectionToList(Collection<Double> c){
        List<Double> list = new ArrayList<>();
        for(Double d: c){
            list.add(d);
        }
        return list;
    }

    private void generateRecommendations(int goal, int week, String id){
        //Delete previous recommendation
        appDb.recommendationDao().deleteAllRecommendations(id);

        //Get activities
        SessionActivity[] activities = appDb.activityDao().getActivitiesFromWeek(week-10, id);
        //get mistakes
        Mistake[] mistakes = appDb.mistakeDao().getMistakesFromWeek(week-10, id);

        //Count
        int[] activity_count = new int[JumpMoves.values().length];
        //Only count activity in different sessions
        int[] previous_session_id = new int[JumpMoves.values().length];
        //Duration
        long[] activity_duration = new long[JumpMoves.values().length];
        //Mets
        double[] activity_mets = new double[JumpMoves.values().length];

        for(SessionActivity sa: activities){
            //Count occurrences
            if(previous_session_id[sa.getActivity()] == 0 || previous_session_id[sa.getActivity()] != sa.getSessionId()){
                activity_count[sa.getActivity()] += 1;
            }
            previous_session_id[sa.getActivity()]  = sa.getSessionId();

            activity_duration[sa.getActivity()] += (sa.getEnd()-sa.getStart());
            activity_mets[sa.getActivity()] += (sa.getActivity()) + sa.getMET_score();
        }

        double[] activity_metsPerSec = new double[JumpMoves.values().length];

        long[] activity_mean_duration = new long[JumpMoves.values().length];

        for(int c=0; c<JumpMoves.values().length; c++){
            if(activity_count[c] != 0){
                //Mean duration
                long meanDuration = activity_duration[c]/activity_count[c];

                //Mean mets
                double meanMets = activity_mets[c]/activity_count[c];

                double metsPerSec = meanMets/meanDuration;
                activity_metsPerSec[c] = metsPerSec;
                activity_mean_duration[c] = meanDuration;
            }
        }

        //Calculate weights per activity
        int[] weights = calculateWeights(mistakes, activity_count);

        //Calculate total weight
        int totalWeight = 0;
        for (int i=0; i<weights.length; i++) {
            totalWeight += weights[i];
        }

        //Generate recommendations until MET goal is reached
        double recommendedMets = 0;

        while(recommendedMets < goal){
            int act = getRecommendedActivity(weights, totalWeight);
            double mets;
            Long duration;

            //Activity wasn't yet performed
            if(activity_mean_duration[act] == 0){
                //Default duration
                duration = (long) (Math.random() * (Constants.DEFAULT2 - Constants.DEFAULT1)) + Constants.DEFAULT1;
                //Random number of METs
                mets = getMetsForDuration(duration);

            } else{
                //Mean duration
                duration = activity_mean_duration[act];
                //Mets according to mean duration
                double metsPerSec = activity_metsPerSec[act];

                mets = (double) duration * metsPerSec;
            }

            Recommendation r = new Recommendation();
            r.setActivity(act);
            r.setDuration(duration);
            r.setMets(mets);
            r.setPending(false);
            r.setDone(false);
            r.setUserId(id);

            AsyncTask.execute(() -> appDb.recommendationDao().insertRecommendation(r));

            recommendedMets += mets;
        }
    }

    private double getMetsForDuration(Long duration) {
        double timeMPA = (Math.random() * duration)/60;
        double timeMPV = (duration - timeMPA)/60;

        double METmin = 4 * timeMPA + 8 * timeMPV;

        return METmin;
    }

    //Get weighted random activity
    private int getRecommendedActivity(int[] weights, int totalWeight){
        int randomAct = -1;
        double random = Math.random() * totalWeight;
        for (int i=0; i<weights.length; i++)
        {
            random -= weights[i];
            if (random <= 0.0d)
            {
                randomAct = i;
                break;
            }
        }
        return randomAct == -1 ?  0 : randomAct;
    }

    //Calculate weights
    private int[] calculateWeights(Mistake[] m, int[] c) {
        //Mistake count for activity
        int[] mistakeCounts = new int[JumpMoves.values().length];
        for(Mistake mis: m){
            mistakeCounts[mis.getActivity()] += 1;
        }

        int[] w = new int[JumpMoves.values().length];

        //Count activity * mistakes
        //More weight when mistakes
        //default weight = 1
        for(int i=0; i<c.length; i++){
            if(mistakeCounts[i] != 0){
                w[i] = c[i]*mistakeCounts[i];
            } else if(c[i] != 0){
                w[i] = c[i];
            } else{
                w[i] = 1;
            }
        }
        return w;
    }
}
