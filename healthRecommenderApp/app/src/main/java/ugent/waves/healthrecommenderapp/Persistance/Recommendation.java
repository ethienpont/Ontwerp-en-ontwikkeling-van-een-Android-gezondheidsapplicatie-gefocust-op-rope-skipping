package ugent.waves.healthrecommenderapp.Persistance;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Recommendation")
public class Recommendation {

    @PrimaryKey(autoGenerate = true)
    private int uid;

    @ColumnInfo(name = "userId")
    private String userId;

    @ColumnInfo(name = "activity")
    private int activity;

    @ColumnInfo(name = "mets")
    private double mets;

    @ColumnInfo(name = "duration")
    private Long duration;

    @ColumnInfo(name = "pending")
    private boolean pending;

    @ColumnInfo(name = "done")
    private boolean done;

    public int getActivity() {
        return activity;
    }

    public void setActivity(int activity) {
        this.activity = activity;
    }

    public double getMets() {
        return mets;
    }

    public void setMets(double mets) {
        this.mets = mets;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
