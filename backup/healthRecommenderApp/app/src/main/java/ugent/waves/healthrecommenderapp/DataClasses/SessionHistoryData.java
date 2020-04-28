package ugent.waves.healthrecommenderapp.dataclasses;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SessionHistoryData {
    private String description;
    private int imgId;
    private String turns;
    private String mets_points;
    private int sessionId;


    public SessionHistoryData(String activity, int imgId, int turns, double mets, int s) {
        this.description = activity;
        this.imgId = imgId;
        this.turns = turns+"";
        this.sessionId = s;
        this.mets_points = mets + "";
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

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }
}
