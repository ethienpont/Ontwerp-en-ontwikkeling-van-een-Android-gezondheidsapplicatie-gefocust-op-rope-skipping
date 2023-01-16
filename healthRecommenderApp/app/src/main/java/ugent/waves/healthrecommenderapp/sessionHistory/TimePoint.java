package ugent.waves.healthrecommenderapp.sessionHistory;

public class TimePoint implements ViewType{

    private String timePoint;

    public TimePoint(String timePoint){
        this.timePoint = timePoint;
    }

    @Override
    public int getViewType() {
        return ViewType.LINE;
    }

    public String getTimePoint() {
        return timePoint;
    }
}
