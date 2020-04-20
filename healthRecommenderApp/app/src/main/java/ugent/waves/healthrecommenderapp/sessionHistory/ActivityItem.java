package ugent.waves.healthrecommenderapp.sessionHistory;

public class ActivityItem implements ViewType{

    private double mets;
    private String start;
    private String end;
    private int activity;

    public ActivityItem(String start, String end, int activity, double mets){
        this.start = start;
        this.end = end;
        this.activity = activity;
        this.mets = mets;
    }

    @Override
    public int getViewType() {
        return ViewType.ITEM;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public int getActivity() {
        return activity;
    }

    public double getMets() {
        return mets;
    }
}
