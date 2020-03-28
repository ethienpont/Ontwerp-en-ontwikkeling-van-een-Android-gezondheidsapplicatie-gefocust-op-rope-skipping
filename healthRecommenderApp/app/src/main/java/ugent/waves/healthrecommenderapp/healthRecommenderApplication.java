package ugent.waves.healthrecommenderapp;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

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

    public Long getNowMilliSec(Calendar cal){
        Date now = new Date();
        /*
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Log.e("a", cal.getTime().toString());*/
        return cal.getTimeInMillis();
    }

    public Long getWeeksAgoMilliSec(Calendar cal, int week){
        cal.add(Calendar.WEEK_OF_YEAR, -1*week);
        /*
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);*/
        return cal.getTimeInMillis();
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
}
