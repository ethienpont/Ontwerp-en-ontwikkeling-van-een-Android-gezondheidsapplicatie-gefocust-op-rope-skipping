package ugent.waves.healthrecommenderapp.sessionHistory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import ugent.waves.healthrecommenderapp.NavigationActivity;
import ugent.waves.healthrecommenderapp.Persistance.Session;
import ugent.waves.healthrecommenderapp.R;

public class SessionHistoryAdapter extends Adapter<SessionHistoryAdapter.SessionViewHolder> {
    private final Context context;
    private Session[] mDataset;
    private SessionHistoryFragment mFragment;

    public static class SessionViewHolder extends ViewHolder{
        public ImageView imageView;
        public TextView mistakes;
        public TextView turns;
        public TextView mets;
        public RelativeLayout relativeLayout;
        public SessionViewHolder(View itemView) {
            super(itemView);
            this.imageView = (ImageView) itemView.findViewById(R.id.thumbnail);
            this.mistakes = (TextView) itemView.findViewById(R.id.mistakes);
            this.turns = (TextView) itemView.findViewById(R.id.turns);
            this.mets = (TextView) itemView.findViewById(R.id.points);
            relativeLayout = (RelativeLayout)itemView.findViewById(R.id.layout);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public SessionHistoryAdapter(Session[] myDataset, Context c) {
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
        holder.turns.setText(mDataset[position].getTurns() + " turns");
        holder.mistakes.setText(mDataset[position].getMistakes() + " mistake(s)");
        holder.mets.setText(mDataset[position].getMets() + " points");
        holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fragmentJump(mDataset[position]);
            }
        });

    }

    private void fragmentJump(Session mItemSelected) {
        mFragment = SessionHistoryFragment.newInstance(mItemSelected.getUid());
        switchContent(R.id.flContent, mFragment);
    }

    public void switchContent(int id, Fragment fragment) {
        if (context instanceof NavigationActivity) {
            NavigationActivity activity = (NavigationActivity) context;
            activity.switchContent(id, fragment);
        }

    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}
