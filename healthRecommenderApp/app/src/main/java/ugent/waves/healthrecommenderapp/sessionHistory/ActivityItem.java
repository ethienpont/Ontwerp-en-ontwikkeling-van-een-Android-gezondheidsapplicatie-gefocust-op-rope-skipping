package ugent.waves.healthrecommenderapp.sessionHistory;

public class ActivityItem implements ViewType{

    private String start;
    private String end;
    private int activity;

    public ActivityItem(String start, String end, int activity){
        this.start = start;
        this.end = end;
        this.activity = activity;
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
}
