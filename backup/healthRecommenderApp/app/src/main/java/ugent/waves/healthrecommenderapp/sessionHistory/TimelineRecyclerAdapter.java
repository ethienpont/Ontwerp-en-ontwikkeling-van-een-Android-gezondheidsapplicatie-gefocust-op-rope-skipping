package ugent.waves.healthrecommenderapp.sessionHistory;

import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class TimelineRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<ViewType> items;

    private Map<Integer,ViewTypeDelegateAdapter> delegateAdapters;

    public TimelineRecyclerAdapter(){
        items = new ArrayList<>();
        delegateAdapters = new HashMap<>();
        //delegateAdapters.put(ViewType.HEADER, WeatherHeaderItemDelegateAdapter());
        delegateAdapters.put(ViewType.LINE, new TimepointItemDelegateAdapter());
        delegateAdapters.put(ViewType.ITEM, new ActivityItemDelegateAdapter());
    }

    public int getItemViewType(int position){
        return items.get(position).getViewType();
    }

    public void addActivity(ActivityItem item) {
        this.items.add(item);
        notifyDataSetChanged();
    }

    public void addTimepoint(TimePoint item) {
        this.items.add(item);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return delegateAdapters.get(viewType).onCreateViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        delegateAdapters.get(getItemViewType(position)).onBindViewHolder(holder, items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
