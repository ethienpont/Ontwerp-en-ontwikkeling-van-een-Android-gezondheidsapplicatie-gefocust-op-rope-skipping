package ugent.waves.healthrecommenderapp;

import android.app.Application;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

public class healthRecommenderApplication extends Application {

    //TODO: iedere week resetten
    private int weeknr;
    private int goal;
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
}
