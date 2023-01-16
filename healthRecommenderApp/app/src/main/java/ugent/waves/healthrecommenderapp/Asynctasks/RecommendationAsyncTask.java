package ugent.waves.healthrecommenderapp.Asynctasks;

import android.app.Activity;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import ugent.waves.healthrecommenderapp.HelpClasses.Constants;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.Persistance.RecommendationDao;

//Interact with recommendation table
public class RecommendationAsyncTask extends AsyncTask<Void, Void, Recommendation[]> {
    private String user;
    private AppDatabase db;
    private String operation;
    private Recommendation r;

    public RecommendationAsyncTask(AppDatabase db, String user, String operation, Recommendation r) {
        this.db = db;
        this.user = user;
        this.operation = operation;
        this.r = r;
    }

    @Override
    protected Recommendation[] doInBackground(Void... params) {
        RecommendationDao recommendationDao = db.recommendationDao();
        if(operation.equalsIgnoreCase(Constants.GET_ALL)){
            return recommendationDao.getAllRecommendations(user);
        } else if(operation.equalsIgnoreCase(Constants.UPDATE)){
            recommendationDao.updateRecommendation(r);
        }
        return null;
    }
}
