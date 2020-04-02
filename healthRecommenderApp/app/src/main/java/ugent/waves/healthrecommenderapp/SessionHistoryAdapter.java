package ugent.waves.healthrecommenderapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import ugent.waves.healthrecommenderapp.dataclasses.SessionHistoryData;

public class SessionHistoryAdapter extends Adapter<SessionHistoryAdapter.SessionViewHolder> {
    private final Context context;
    private List<SessionHistoryData> mDataset;
    private SessionHistoryFragment mFragment;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class SessionViewHolder extends ViewHolder{
        public ImageView imageView;
        public TextView activity;
        public TextView startTime;
        public TextView endTime;
        public RelativeLayout relativeLayout;
        public SessionViewHolder(View itemView) {
            super(itemView);
            this.imageView = (ImageView) itemView.findViewById(R.id.thumbnail);
            this.activity = (TextView) itemView.findViewById(R.id.activity);
            this.startTime = (TextView) itemView.findViewById(R.id.start);
            this.endTime = (TextView) itemView.findViewById(R.id.end);
            relativeLayout = (RelativeLayout)itemView.findViewById(R.id.layout);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public SessionHistoryAdapter(List<SessionHistoryData> myDataset, Context c) {
        mDataset = myDataset;
        context = c;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SessionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.session_history_list_item, parent, false);
        SessionViewHolder viewHolder = new SessionViewHolder(listItem);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(SessionViewHolder holder, final int position) {
        holder.activity.setText(mDataset.get(position).getDescription());
        holder.imageView.setImageResource(mDataset.get(position).getImgId());
        holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fragmentJump(mDataset.get(position));
            }
        });

    }

    private void fragmentJump(SessionHistoryData mItemSelected) {
        mFragment = SessionHistoryFragment.newInstance(mItemSelected);
        switchContent(R.id.container, mFragment);
    }

    public void switchContent(int id, Fragment fragment) {
        if (context == null)
            return;
        if (context instanceof SessionHistoryActivity) {
            SessionHistoryActivity activity = (SessionHistoryActivity) context;
            activity.switchContent(id, fragment);
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
