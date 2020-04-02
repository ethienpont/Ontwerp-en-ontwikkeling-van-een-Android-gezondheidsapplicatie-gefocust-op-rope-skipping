package ugent.waves.healthrecommenderapp;

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
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Session;
import ugent.waves.healthrecommenderapp.Persistance.SessionDao;
import ugent.waves.healthrecommenderapp.dataclasses.SessionHistoryData;

public class SessionHistoryListFragment extends Fragment {

    private static final String ARG_DATA = "DATA1";
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private SessionHistoryAdapter mAdapter;
    private List<SessionHistoryData> data;
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

        //List<SessionHistoryData> data = getArguments().<SessionHistoryData>getParcelableArrayList(ARG_DATA);
        data = new ArrayList<>();
        get_session_data();

        mAdapter = new SessionHistoryAdapter(data, getContext());
        recyclerView.setAdapter(mAdapter);

        return rootView;
    }

    private void get_session_data(){
        try{
            Session[] s = new SessionAsyncTask(getActivity(), appDb).execute().get();

            String activity = "rope skipping";
            int imgId = getImage(activity);
            for(Session ses: s){
                SessionHistoryData s_recyclerview = new SessionHistoryData(activity, imgId, ses.getTurns(),ses.getMets(), ses.getUid());
                data.add(s_recyclerview);
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static SessionHistoryListFragment newInstance(List<SessionHistoryData> data) {
        SessionHistoryListFragment fragment = new SessionHistoryListFragment();
        Bundle args = new Bundle();
        //args.putParcelableArrayList(ARG_DATA, (ArrayList<SessionHistoryData>) data);
        fragment.setArguments(args);
        return fragment;
    }

    private int getImage(String activity) {
        if(activity.equals("running")){
            return R.drawable.running;
        } else if(activity.equals("biking")){
            return R.drawable.biking;
        } else if(activity.equals("badminton")){
            return R.drawable.badminton;
        } else if(activity.equals("rope skipping")){
            return R.drawable.rope_skipping;
        }
        return R.drawable.other;
    }

    private static class SessionAsyncTask extends AsyncTask<Void, Void, ugent.waves.healthrecommenderapp.Persistance.Session[]> {
        //Prevent leak
        private WeakReference<Activity> weakActivity;
        private AppDatabase db;

        public SessionAsyncTask(Activity activity, AppDatabase db) {
            weakActivity = new WeakReference<>(activity);
            this.db = db;
        }

        @Override
        protected ugent.waves.healthrecommenderapp.Persistance.Session[] doInBackground(Void... params) {
            SessionDao sessionDao = db.sessionDao();
            return sessionDao.loadAllSessions();
        }
    }
}
