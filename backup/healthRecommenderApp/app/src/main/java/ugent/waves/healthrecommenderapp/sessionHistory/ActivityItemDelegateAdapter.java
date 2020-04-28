package ugent.waves.healthrecommenderapp.sessionHistory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import ugent.waves.healthrecommenderapp.Enums.JumpMoves;
import ugent.waves.healthrecommenderapp.R;

public class ActivityItemDelegateAdapter implements ViewTypeDelegateAdapter{

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p) {
            LayoutInflater layoutInflater = LayoutInflater.from(p.getContext());
            View listItem= layoutInflater.inflate(R.layout.item_activity, p, false);
            ActivityItemViewHolder viewHolder = new ActivityItemViewHolder(listItem);
            return viewHolder;
        }

        //TODO: error id not found, maar werkt wel?
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, ViewType item) {
            try{
                ActivityItemViewHolder activityHolder = (ActivityItemViewHolder) holder;
                ActivityItem t = (ActivityItem) item;
                activityHolder.start.setText("start: " + t.getStart());
                activityHolder.end.setText("end: " + t.getEnd());
                activityHolder.date.setText("activity: " +JumpMoves.getJumpName(t.getActivity()));
                activityHolder.mets.setText(t.getMets()+"");
                activityHolder.mistakes.setText(t.getMistakes()+"");
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        public static class ActivityItemViewHolder extends RecyclerView.ViewHolder{
            public TextView date;
            public TextView start;
            public TextView end;
            public TextView mets;
            public TextView mistakes;

            public ActivityItemViewHolder(View itemView) {
                super(itemView);

                this.date = (TextView) itemView.findViewById(R.id.date_timeline);
                this.start = (TextView) itemView.findViewById(R.id.start_timeline);
                this.end = (TextView) itemView.findViewById(R.id.end_timeline);
                this.mets = (TextView) itemView.findViewById(R.id.mets_timeline);
                this.mistakes = (TextView) itemView.findViewById(R.id.mistakes_timeline);
            }
        }
}
