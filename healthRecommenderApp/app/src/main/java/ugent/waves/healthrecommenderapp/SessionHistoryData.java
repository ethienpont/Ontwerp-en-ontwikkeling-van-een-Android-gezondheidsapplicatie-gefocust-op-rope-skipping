package ugent.waves.healthrecommenderapp;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.fitness.data.DataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SessionHistoryData implements Parcelable {
    private String description;
    private int imgId;
    private String startTime;
    private String endTime;
    private List<DataSet> datasets;
    private String turns;
    private String mets_points;

    public SessionHistoryData(String activity, int imgId, Long startTime, Long endTime, List<DataSet> datasets, String turns, String met_points) {
        this.description = activity;
        this.imgId = imgId;
        this.startTime = setDate(new Date(startTime));
        this.endTime = setDate(new Date(endTime));
        this.datasets = datasets;
        this.turns = turns+"";
        this.mets_points = met_points+"";
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

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setDatasetst(List<DataSet> d){
        this.datasets = d;
    }

    public List<DataSet> getDatasets (){
        return datasets;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.description);
        dest.writeInt(this.imgId);
        dest.writeString(this.startTime);
        dest.writeString(this.endTime);
        dest.writeTypedList(this.datasets);
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
        this.startTime = in.readString();
        this.endTime = in.readString();
        in.readTypedList(this.datasets, DataSet.CREATOR);
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
