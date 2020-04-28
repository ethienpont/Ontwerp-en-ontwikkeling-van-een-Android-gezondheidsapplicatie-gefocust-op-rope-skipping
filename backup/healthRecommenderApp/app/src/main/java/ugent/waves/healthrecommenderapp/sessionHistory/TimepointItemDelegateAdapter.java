package ugent.waves.healthrecommenderapp.sessionHistory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import ugent.waves.healthrecommenderapp.R;

public class TimepointItemDelegateAdapter implements ViewTypeDelegateAdapter{
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p) {
        LayoutInflater layoutInflater = LayoutInflater.from(p.getContext());
        View listItem= layoutInflater.inflate(R.layout.item_time_point, p, false);
        TimeLineViewHolder viewHolder = new TimeLineViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, ViewType item) {
        TimeLineViewHolder timeHolder = (TimeLineViewHolder) holder;
        TimePoint t = (TimePoint) item;
        timeHolder.description.setText(t.getDescription());
        timeHolder.time.setText(t.getTimePoint());
    }

    public static class TimeLineViewHolder extends RecyclerView.ViewHolder{
        public TextView time;
        public TextView description;

        public TimeLineViewHolder(View itemView) {
            super(itemView);
            this.time = (TextView) itemView.findViewById(R.id.time);
            this.description = (TextView) itemView.findViewById(R.id.time_description);
        }
    }
}
