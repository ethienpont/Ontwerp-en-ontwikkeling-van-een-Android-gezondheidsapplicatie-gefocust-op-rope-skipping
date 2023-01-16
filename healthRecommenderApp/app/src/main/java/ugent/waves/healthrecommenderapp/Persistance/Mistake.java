package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Mistake")
public class Mistake {
    @PrimaryKey(autoGenerate = true)
    private int uid;

    @ColumnInfo(name = "userId")
    private String userId;

    @ColumnInfo(name = "time")
    private int time;

    @ColumnInfo(name = "sessionId")
    private int sessionId;

    @ColumnInfo(name = "activity")
    private int activity;

    @ColumnInfo(name = "week")
    private int week;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public int getActivity() {
        return activity;
    }

    public void setActivity(int activity) {
        this.activity = activity;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
