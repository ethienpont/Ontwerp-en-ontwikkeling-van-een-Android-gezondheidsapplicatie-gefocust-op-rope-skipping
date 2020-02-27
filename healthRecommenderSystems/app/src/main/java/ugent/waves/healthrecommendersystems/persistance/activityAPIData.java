package ugent.waves.healthrecommendersystems.persistance;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class activityAPIData {
    public activityAPIData(int activity, float probability){
        this.activity = activity;
        this.probability = probability;
    }

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "activity")
    public int activity;

    @ColumnInfo(name = "probability")
    public float probability;

}
