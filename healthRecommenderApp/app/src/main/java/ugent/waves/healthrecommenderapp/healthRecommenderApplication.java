package ugent.waves.healthrecommenderapp;

import android.app.Application;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.room.Room;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;

public class healthRecommenderApplication extends Application {

    //TODO: iedere week resetten
    private int weeknr;
    private int goal;

    private long timeStill;
    private long startStill;

    private FirebaseFirestore db;
    private AppDatabase appDb;

    private int rank;

    private GoogleSignInAccount account;

    private GoogleSignInClient mGoogleSignInClient;

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

    public FirebaseFirestore getFirestore() {
        if(db == null){
            db = FirebaseFirestore.getInstance();
        }
        return db;
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

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
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
