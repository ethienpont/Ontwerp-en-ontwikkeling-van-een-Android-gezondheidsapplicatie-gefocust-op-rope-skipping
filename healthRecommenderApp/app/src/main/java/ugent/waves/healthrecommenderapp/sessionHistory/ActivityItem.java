package ugent.waves.healthrecommenderapp.sessionHistory;

public class ActivityItem implements ViewType{

    private int mistakes;
    private String start;
    private String end;
    private int activity;

    public ActivityItem(String start, String end, int activity, int mis){
        this.start = start;
        this.end = end;
        this.activity = activity;
        this.mistakes = mis;
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

    public int getMistakes() {
        return mistakes;
    }

}
