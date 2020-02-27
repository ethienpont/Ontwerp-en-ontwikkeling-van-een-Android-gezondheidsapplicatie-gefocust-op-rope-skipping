package ugent.waves.healthrecommenderapp;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.fitness.data.DataSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SessionHistoryData implements Parcelable {
    private String activity;
    private int imgId;
    private Date startTime;
    private Date endTime;
    private List<DataSet> datasets;

    public SessionHistoryData(String activity, int imgId, Long startTime, Long endTime, List<DataSet> datasets) {
        this.activity = activity;
        this.imgId = imgId;
        this.startTime = new Date(startTime);
        this.endTime = new Date(endTime);
        this.datasets = datasets;
    }

    public String getDescription() {
        return activity;
    }
    public void setDescription(String description) {
        this.activity = description;
    }
    public int getImgId() {
        return imgId;
    }
    public void setImgId(int imgId) {
        this.imgId = imgId;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
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
        dest.writeString(this.activity);
        dest.writeInt(this.imgId);
        dest.writeSerializable(this.startTime);
        dest.writeSerializable(this.endTime);
        dest.writeTypedList(this.datasets);
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
        this.activity = in.readString();
        this.imgId = in.readInt();
        this.startTime = (Date) in.readSerializable();
        this.endTime = (Date) in.readSerializable();
        in.readTypedList(this.datasets, DataSet.CREATOR);
    }
}
