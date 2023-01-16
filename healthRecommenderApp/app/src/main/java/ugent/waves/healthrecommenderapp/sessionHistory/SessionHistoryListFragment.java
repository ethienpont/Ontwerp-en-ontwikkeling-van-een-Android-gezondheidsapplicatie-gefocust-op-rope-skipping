package ugent.waves.healthrecommenderapp.sessionHistory;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ugent.waves.healthrecommenderapp.Asynctasks.SessionAsyncTask;
import ugent.waves.healthrecommenderapp.HelpClasses.Constants;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Session;
import ugent.waves.healthrecommenderapp.Persistance.SessionDao;
import ugent.waves.healthrecommenderapp.R;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

public class SessionHistoryListFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private SessionHistoryAdapter mAdapter;
    private AppDatabase appDb;
    private healthRecommenderApplication app;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //recyclerview
        View rootView = inflater.inflate(R.layout.session_history_list_fragment, container, false);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.history_recycler_view);

        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        app = (healthRecommenderApplication) getContext().getApplicationContext();

        appDb = app.getAppDb();

        Session[] data = get_session_data();

        mAdapter = new SessionHistoryAdapter(data, getContext());
        recyclerView.setAdapter(mAdapter);

        return rootView;
    }

    private Session[] get_session_data(){
        try{
            Session[] s = new SessionAsyncTask(appDb, app.getAccount().getId(), Constants.GET_ALL).execute().get();
            return s;
        } catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static SessionHistoryListFragment newInstance() {
        SessionHistoryListFragment fragment = new SessionHistoryListFragment();
        return fragment;
    }
}
