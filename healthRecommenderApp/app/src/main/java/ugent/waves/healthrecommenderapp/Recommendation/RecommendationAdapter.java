package ugent.waves.healthrecommenderapp.Recommendation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import ugent.waves.healthrecommenderapp.Enums.JumpMoves;
import ugent.waves.healthrecommenderapp.HelpClasses.Constants;
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.R;
import ugent.waves.healthrecommenderapp.StartSessionActivity;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder> {
    private final Context context;
    private Recommendation[] mDataset;

    public static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView activity;
        public TextView duration;
        public TextView pending;
        public RelativeLayout relativeLayout;
        public RecommendationViewHolder(View itemView) {
            super(itemView);
            this.imageView = (ImageView) itemView.findViewById(R.id.thumbnail_recommendation);
            this.activity = (TextView) itemView.findViewById(R.id.activity_recommendation);
            this.duration = (TextView) itemView.findViewById(R.id.duration);
            this.pending = (TextView) itemView.findViewById(R.id.pending);
            relativeLayout = (RelativeLayout)itemView.findViewById(R.id.layout_recommendation);
        }
    }

    // Provide a suitable constructor
    public RecommendationAdapter(Recommendation[] d, Context c) {
        mDataset = d;
        context = c;
    }

    // Create new views
    @Override
    public RecommendationAdapter.RecommendationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.recommendation_list_item, parent, false);
        RecommendationAdapter.RecommendationViewHolder viewHolder = new RecommendationAdapter.RecommendationViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecommendationAdapter.RecommendationViewHolder holder, final int position) {
        holder.duration.setText(toMinutes(mDataset[position].getDuration()));
        holder.pending.setText("");
        holder.activity.setText(JumpMoves.getJumpName(mDataset[position].getActivity())+"");
        holder.imageView.setImageResource(R.drawable.heart_icon);
        if(mDataset[position].isPending()){
            holder.pending.setText(R.string.Pending);
        }
        if(mDataset[position].isDone()){
            holder.relativeLayout.setEnabled(false);
            holder.relativeLayout.setBackgroundColor(Color.GRAY);
        }
        holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, StartSessionActivity.class);
                intent.putExtra(Constants.ACTIVITY_ID, mDataset[position].getActivity());
                intent.putExtra(Constants.RECOMMENDATION_ID, mDataset[position].getUid());
                context.startActivity(intent);
            }
        });
    }

    //Persisted duration in seconds -> displayed in minutes when possible
    private String toMinutes(Long duration) {
        int m = (int) (duration/60);
        int s = (int) (duration%60);
        if(s >= 30){
            m++;
        }
        return m == 0 ? s + " second(s)" : m + " minute(s)";
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}
