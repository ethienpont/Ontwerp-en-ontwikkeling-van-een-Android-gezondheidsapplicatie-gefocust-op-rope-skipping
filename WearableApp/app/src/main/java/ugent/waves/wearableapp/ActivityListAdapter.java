package ugent.waves.wearableapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import androidx.annotation.NonNull;
import androidx.wear.widget.WearableRecyclerView;

public class ActivityListAdapter extends WearableRecyclerView.Adapter<ActivityListAdapter.ViewHolder> {

    public static final String ACTIVITY = "workout";
    private ActivityListData[] data;
    private GoogleSignInAccount account;
    private Context context;

    // RecyclerView recyclerView;
    public ActivityListAdapter(Context context, ActivityListData[] listdata, GoogleSignInAccount account) {
        this.data = listdata;
        this.account = account;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.activity_list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final ActivityListData myListData = data[position];
        holder.textView.setText(data[position].getDescription());
        holder.imageView.setImageResource(data[position].getImgId());
        holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, SessionActivity.class);
                String activity = myListData.getDescription();
                intent.putExtra(ACTIVITY, activity);
                context.startActivity(intent);
            }
        });
    }


    @Override
    public int getItemCount() {
        return data.length;
    }

    public static class ViewHolder extends WearableRecyclerView.ViewHolder {

        public ImageView imageView;
        public TextView textView;
        public RelativeLayout relativeLayout;
        public ViewHolder(View itemView) {
            super(itemView);
            this.imageView = (ImageView) itemView.findViewById(R.id.imageView);
            this.textView = (TextView) itemView.findViewById(R.id.textView);
            relativeLayout = (RelativeLayout)itemView.findViewById(R.id.relativeLayout);
        }
    }
}
