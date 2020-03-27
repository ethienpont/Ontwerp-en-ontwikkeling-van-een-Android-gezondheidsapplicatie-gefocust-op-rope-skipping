package ugent.waves.healthrecommenderapp.DataClasses;

import android.os.Parcel;
import android.os.Parcelable;

public class Activity implements Parcelable {

    private String start;
    private String end;
    private String move;

    public Activity(String start, String end, String move){
        this.start = start;
        this.end = end;
        this.move = move;
    }

    protected Activity(Parcel in) {
        this.start = in.readString();
        this.end = in.readString();
        this.move = in.readString();
    }

    public static final Creator<Activity> CREATOR = new Creator<Activity>() {
        @Override
        public Activity createFromParcel(Parcel in) {
            return new Activity(in);
        }

        @Override
        public Activity[] newArray(int size) {
            return new Activity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.start);
        dest.writeString(this.end);
        dest.writeString(this.move);
    }
}
