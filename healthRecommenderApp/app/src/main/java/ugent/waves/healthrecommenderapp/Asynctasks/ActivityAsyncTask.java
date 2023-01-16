package ugent.waves.healthrecommenderapp.Asynctasks;

import android.os.AsyncTask;

import ugent.waves.healthrecommenderapp.HelpClasses.Constants;
import ugent.waves.healthrecommenderapp.Persistance.ActivityDao;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.SessionActivity;

//Interact with activity table
public class ActivityAsyncTask extends AsyncTask<Void, Void, SessionActivity[]> {
    private String user;
    private AppDatabase db;
    private int id;
    private String operation;

    public ActivityAsyncTask(AppDatabase db, int id, String user, String operation) {
        this.db = db;
        this.id = id;
        this.user = user;
        this.operation = operation;
    }

    @Override
    protected SessionActivity[] doInBackground(Void... params) {
        ActivityDao activityDao = db.activityDao();
        if(operation.equalsIgnoreCase(Constants.GET_ALL)){
            return activityDao.getActivitiesForSession(id, user);
        }
        return null;
    }
}
