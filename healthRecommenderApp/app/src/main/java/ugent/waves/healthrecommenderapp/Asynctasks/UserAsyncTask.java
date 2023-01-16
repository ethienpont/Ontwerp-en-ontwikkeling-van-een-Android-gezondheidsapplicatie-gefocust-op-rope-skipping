package ugent.waves.healthrecommenderapp.Asynctasks;

import android.os.AsyncTask;

import ugent.waves.healthrecommenderapp.HelpClasses.Constants;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.User;
import ugent.waves.healthrecommenderapp.Persistance.UserDao;

//Interact with user table
public class UserAsyncTask extends AsyncTask<Void, Void, User> {
    private String user;
    private AppDatabase db;
    private String operation;

    public UserAsyncTask(AppDatabase db, String id, String operation) {
        this.db = db;
        this.user = id;
        this.operation = operation;
    }

    @Override
    protected User doInBackground(Void... params) {
        UserDao userDao = db.userDao();
        if(operation.equalsIgnoreCase(Constants.GET)){
            return userDao.getUser(user);
        }
        return null;
    }
}
