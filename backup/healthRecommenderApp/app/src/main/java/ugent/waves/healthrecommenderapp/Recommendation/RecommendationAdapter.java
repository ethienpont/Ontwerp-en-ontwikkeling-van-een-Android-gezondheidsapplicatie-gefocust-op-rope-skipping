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
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.R;
import ugent.waves.healthrecommenderapp.StartSessionActivity;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder> {
    private static final String ACTIVITY_ID = "ACTIVITY_ID";
    private static final String RECOMMENDATION_ID = "RECOMMENDATION_ID";
    private final Context context;
    private Recommendation[] mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
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

    // Provide a suitable constructor (depends on the kind of dataset)
    public RecommendationAdapter(Recommendation[] myDataset, Context c) {
        mDataset = myDataset;
        context = c;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecommendationAdapter.RecommendationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.recommendation_list_item, parent, false);
        RecommendationAdapter.RecommendationViewHolder viewHolder = new RecommendationAdapter.RecommendationViewHolder(listItem);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecommendationAdapter.RecommendationViewHolder holder, final int position) {
        holder.duration.setText(mDataset[position].getDuration() + " duration");
        holder.pending.setText("");
        holder.activity.setText(mDataset[position].getActivity()+"");
        holder.imageView.setImageResource(R.drawable.running);
        if(mDataset[position].isPending()){
            holder.pending.setText("Pending");
        }
        if(mDataset[position].isDone()){
            holder.relativeLayout.setBackgroundColor(Color.TRANSPARENT);
        }
        holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, StartSessionActivity.class);
                intent.putExtra(ACTIVITY_ID, mDataset[position].getActivity());
                intent.putExtra(RECOMMENDATION_ID, mDataset[position].getUid());
                context.startActivity(intent);
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}
