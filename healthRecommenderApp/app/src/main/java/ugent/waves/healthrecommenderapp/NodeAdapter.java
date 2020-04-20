package ugent.waves.healthrecommenderapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.wearable.Node;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class NodeAdapter extends RecyclerView.Adapter<NodeAdapter.NodeViewHolder>  {
    private List<Node>  mDataset;
    RecyclerViewItemClickListener recyclerViewItemClickListener;

    public NodeAdapter(List<Node> myDataset, RecyclerViewItemClickListener listener) {
        mDataset = myDataset;
        this.recyclerViewItemClickListener = listener;
    }

    @NonNull
    @Override
    public NodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.node_item, parent, false);

        NodeViewHolder vh = new NodeViewHolder(v);
        return vh;

    }

    @Override
    public void onBindViewHolder(@NonNull NodeViewHolder fruitViewHolder, int i) {
        fruitViewHolder.mTextView.setText(mDataset.get(i).getDisplayName());
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }



    public  class NodeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView mTextView;

        public NodeViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.textView);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            recyclerViewItemClickListener.clickOnItem(mDataset.get(this.getAdapterPosition()));

        }
    }

    public interface RecyclerViewItemClickListener {
        void clickOnItem(Node data);
    }
}
