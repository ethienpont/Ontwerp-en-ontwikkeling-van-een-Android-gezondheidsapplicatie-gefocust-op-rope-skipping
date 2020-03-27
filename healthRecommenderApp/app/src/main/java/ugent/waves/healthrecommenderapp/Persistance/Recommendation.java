package ugent.waves.healthrecommenderapp.Persistance;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Recommendation")
public class Recommendation {

    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int uid;

    @ColumnInfo(name = "activity")
    private int activity;

    @ColumnInfo(name = "mets")
    private double mets;

    @ColumnInfo(name = "duration")
    private Long duration;

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

    public void setUid(@NonNull int uid) {
        this.uid = uid;
    }
}
