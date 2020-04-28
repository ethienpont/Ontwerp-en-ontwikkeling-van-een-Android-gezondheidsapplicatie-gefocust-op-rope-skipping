package ugent.waves.healthrecommenderapp;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.room.Room;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;

public class healthRecommenderApplication extends Application {

    private int weeknr = 0;
    private int goal = 600;

    private long timeStill = 0;
    private long startStill = 0;

    private FirebaseFirestore db;
    private AppDatabase appDb;


    private GoogleSignInAccount account;

    private GoogleSignInClient mGoogleSignInClient;

    //TODO: each time activity switches, change this
    private Context mContext;

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public int getWeeknr() {
        return weeknr;
    }

    public void setWeeknr(int weeknr) {
        this.weeknr = weeknr;
    }

    public int getGoal() {
        return goal;
    }

    public void setGoal(int goal) {
        this.goal = goal;
    }

    public AppDatabase getAppDb(){
        if(appDb == null){
            appDb = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "session-database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return appDb;
    }

    public long getTimeStill() {
        return timeStill;
    }

    public void setTimeStill(long timeStill) {
        this.timeStill = timeStill;
    }

    public long getStartStill() {
        return startStill;
    }

    public void setStartStill(long startStill) {
        this.startStill = startStill;
    }

    public GoogleSignInAccount getAccount() {
        return account;
    }

    public void setAccount(GoogleSignInAccount account) {
        this.account = account;
    }

    public GoogleSignInClient getmGoogleSignInClient() {
        return mGoogleSignInClient;
    }

    public void setmGoogleSignInClient(GoogleSignInClient mGoogleSignInClient) {
        this.mGoogleSignInClient = mGoogleSignInClient;
    }
}
