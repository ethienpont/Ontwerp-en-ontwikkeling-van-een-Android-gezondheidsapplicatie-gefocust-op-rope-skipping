package ugent.waves.healthrecommenderapp.sessionHistory;

public class TimePoint implements ViewType{

    private String timePoint;
    private String description;

    public TimePoint(String timePoint, String description){
        this.timePoint = timePoint;
        this.description = description;
    }

    @Override
    public int getViewType() {
        return ViewType.LINE;
    }

    public String getTimePoint() {
        return timePoint;
    }

    public String getDescription() {
        return description;
    }
}
