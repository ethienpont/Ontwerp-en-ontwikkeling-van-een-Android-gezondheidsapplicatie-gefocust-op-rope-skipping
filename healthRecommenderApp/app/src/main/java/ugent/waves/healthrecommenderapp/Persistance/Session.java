package ugent.waves.healthrecommenderapp.Persistance;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Session")
public class Session {
    @PrimaryKey(autoGenerate = true)
    private int uid;

    @ColumnInfo(name = "userId")
    private String userId;

    @ColumnInfo(name = "turns")
    private int turns;

    @ColumnInfo(name = "mets")
    private double mets;

    @ColumnInfo(name = "week")
    private int week;

    @ColumnInfo(name = "mistakes")
    private int mistakes;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getTurns() {
        return turns;
    }

    public void setTurns(int turns) {
        this.turns = turns;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public double getMets() {
        return mets;
    }

    public void setMets(double mets) {
        this.mets = mets;
    }

    public int getMistakes() {
        return mistakes;
    }

    public void setMistakes(int mistakes) {
        this.mistakes = mistakes;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
