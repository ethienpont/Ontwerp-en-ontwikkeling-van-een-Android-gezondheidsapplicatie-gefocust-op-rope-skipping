package ugent.waves.healthrecommenderapp.Asynctasks;

import android.os.AsyncTask;

import ugent.waves.healthrecommenderapp.HelpClasses.Constants;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Session;
import ugent.waves.healthrecommenderapp.Persistance.SessionDao;

//Interact with session table
public class SessionAsyncTask extends AsyncTask<Void, Void, Session[]> {
    private String user;
    private AppDatabase db;
    private String operation;

    public SessionAsyncTask(AppDatabase db, String user, String operation) {
        this.db = db;
        this.user = user;
        this.operation = operation;
    }

    @Override
    protected ugent.waves.healthrecommenderapp.Persistance.Session[] doInBackground(Void... params) {
        SessionDao sessionDao = db.sessionDao();
        if(operation.equalsIgnoreCase(Constants.GET_ALL)){
            return sessionDao.loadAllSessions(user);
        }
        return null;
    }
}
