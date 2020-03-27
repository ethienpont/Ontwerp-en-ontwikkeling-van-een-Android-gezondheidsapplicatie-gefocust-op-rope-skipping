package ugent.waves.healthrecommenderapp.dataclasses;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ugent.waves.healthrecommenderapp.Persistance.SessionActivity;

public class SessionHistoryData implements Parcelable {
    private String description;
    private int imgId;
    private String turns;
    private String mets_points;
    private List<SessionActivity> activities;


    public SessionHistoryData(String activity, int imgId, int turns, List<SessionActivity> activities) {
        this.description = activity;
        this.imgId = imgId;
        this.turns = turns+"";
        this.activities = activities;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public int getImgId() {
        return imgId;
    }
    public void setImgId(int imgId) {
        this.imgId = imgId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.description);
        dest.writeInt(this.imgId);
        dest.writeTypedList(this.activities);
        dest.writeString(this.turns);
        dest.writeString(this.mets_points);
    }

    public static final Parcelable.Creator<SessionHistoryData> CREATOR
            = new Parcelable.Creator<SessionHistoryData>() {
        public SessionHistoryData createFromParcel(Parcel in) {
            return new SessionHistoryData(in);
        }

        public SessionHistoryData[] newArray(int size) {
            return new SessionHistoryData[size];
        }
    };

    private SessionHistoryData(Parcel in) {
        this.description = in.readString();
        this.imgId = in.readInt();
        in.readTypedList(this.activities, SessionActivity.CREATOR);
        this.turns = in.readString();
        this.mets_points = in.readString();
    }

    public String getTurns() {
        return turns;
    }

    public void setTurns(String turns) {
        this.turns = turns;
    }

    public String getMets_points() {
        return mets_points;
    }

    public void setMets_points(String mets_points) {
        this.mets_points = mets_points;
    }

    public String setDate (Date d){

        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        return formatter.format(d);
    }
}
