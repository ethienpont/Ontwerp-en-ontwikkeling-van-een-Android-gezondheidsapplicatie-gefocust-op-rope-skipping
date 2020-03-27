package ugent.waves.healthrecommenderapp.Persistance;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Activity")
public class SessionActivity implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int uid;

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

    public SessionActivity(){

    }

    public SessionActivity(int sessionId, Long start, Long end, double MET_score) {
        this.sessionId = sessionId;
        this.start = start;
        this.end = end;
        this.MET_score = MET_score;
    }

    protected SessionActivity(Parcel in) {
        uid = in.readInt();
        sessionId = in.readInt();
        if (in.readByte() == 0) {
            start = null;
        } else {
            start = in.readLong();
        }
        if (in.readByte() == 0) {
            end = null;
        } else {
            end = in.readLong();
        }
        MET_score = in.readDouble();
        activity = in.readInt();
    }

    public static final Creator<SessionActivity> CREATOR = new Creator<SessionActivity>() {
        @Override
        public SessionActivity createFromParcel(Parcel in) {
            return new SessionActivity(in);
        }

        @Override
        public SessionActivity[] newArray(int size) {
            return new SessionActivity[size];
        }
    };

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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.start);
        dest.writeLong(this.end);
        dest.writeDouble(this.MET_score);
        dest.writeInt(this.activity);
    }

    public int getActivity() {
        return activity;
    }

    public void setActivity(int activity) {
        this.activity = activity;
    }
}

