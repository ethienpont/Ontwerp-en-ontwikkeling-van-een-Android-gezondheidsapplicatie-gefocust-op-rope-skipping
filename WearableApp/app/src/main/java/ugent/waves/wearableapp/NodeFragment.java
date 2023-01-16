package ugent.waves.wearableapp;


import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.wearable.Node;

import java.util.ArrayList;
import java.util.Map;

import androidx.fragment.app.Fragment;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

//Display available nodes
public class NodeFragment extends Fragment {

    private static final String KEY = "DATA";

    //Get nodes as input
    public static Fragment newInstance(ArrayList<String> data) {
        NodeFragment fragment = new NodeFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(KEY, data);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.node_fragment, container, false);

        WearableRecyclerView recyclerView = (WearableRecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(
                new WearableLinearLayoutManager(getActivity()));

        final ArrayList<String> items = this.getArguments().getStringArrayList(KEY);

        //call method from parent activity when clicking on node item
        recyclerView.setAdapter(new NodeAdapter(getActivity(), items, new NodeAdapter.AdapterCallback() {
            @Override
            public void onItemClicked(final Integer menuPosition) {
                ((SessionActivity)getActivity()).setNode(items.get(menuPosition));
            }
        }));

        return view;
    }
}
