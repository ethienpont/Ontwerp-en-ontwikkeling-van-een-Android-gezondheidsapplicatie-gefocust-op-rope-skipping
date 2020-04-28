package ugent.waves.healthrecommenderapp.Persistance;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Snooze")
public class Snooze {
    @PrimaryKey(autoGenerate = true)
    private int uid;

    @ColumnInfo(name = "weekday")
    private int weekday;

    @ColumnInfo(name = "hour")
    private int hour;

    @ColumnInfo(name = "week")
    private int week;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getWeekday() {
        return weekday;
    }

    public void setWeekday(int weekday) {
        this.weekday = weekday;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }
}
