package ugent.waves.healthrecommenderapp.Asynctasks;

import android.app.Activity;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import ugent.waves.healthrecommenderapp.HelpClasses.Constants;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Mistake;
import ugent.waves.healthrecommenderapp.Persistance.MistakeDao;

//Interact with mistake table
public class MistakeAsyncTask extends AsyncTask<Void, Void, Mistake[]> {
    private String user;
    private AppDatabase db;
    private int id;
    private String operation;

    public MistakeAsyncTask(AppDatabase db, int id, String user, String operation) {
        this.db = db;
        this.id = id;
        this.user = user;
        this.operation = operation;
    }

    @Override
    protected Mistake[] doInBackground(Void... params) {
        MistakeDao mistakeDao = db.mistakeDao();
        if(operation.equalsIgnoreCase(Constants.GET_ALL)){
            return mistakeDao.getMistakesForSession(id, user);
        }
        return null;
    }
}
