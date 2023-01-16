package ugent.waves.healthrecommenderapp.Persistance;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Activity")
public class SessionActivity{
    @PrimaryKey(autoGenerate = true)
    private int uid;

    @ColumnInfo(name = "userId")
    private String userId;

    @ColumnInfo(name = "sessionId")
    private int sessionId;

    @ColumnInfo(name = "start")
    private Long start;

    @ColumnInfo(name = "end")
    private Long end;

    @ColumnInfo(name = "MET_score")
    private double MET_score;

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

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public double getMET_score() {
        return MET_score;
    }

    public void setMET_score(double MET_score) {
        this.MET_score = MET_score;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public int getActivity() {
        return activity;
    }

    public void setActivity(int activity) {
        this.activity = activity;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

