package ugent.waves.healthrecommenderapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ugent.waves.healthrecommenderapp.dataclasses.SessionHistoryData;

public class SessionHistoryListFragment extends Fragment {

    private static final String ARG_DATA = "DATA1";
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private SessionHistoryAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //recyclerview
        View rootView = inflater.inflate(R.layout.session_history_list_fragment, container, false);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.history_recycler_view);

        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        List<SessionHistoryData> data = getArguments().<SessionHistoryData>getParcelableArrayList(ARG_DATA);

        mAdapter = new SessionHistoryAdapter(data, getContext());
        recyclerView.setAdapter(mAdapter);

        return rootView;
    }

    public static SessionHistoryListFragment newInstance(List<SessionHistoryData> data) {
        SessionHistoryListFragment fragment = new SessionHistoryListFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_DATA, (ArrayList<SessionHistoryData>) data);
        fragment.setArguments(args);
        return fragment;
    }
}
