package ugent.waves.healthrecommenderapp;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import ugent.waves.healthrecommenderapp.Persistance.ActivityDao;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Mistake;
import ugent.waves.healthrecommenderapp.Persistance.MistakeDao;
import ugent.waves.healthrecommenderapp.Persistance.SessionActivity;
import ugent.waves.healthrecommenderapp.databinding.SessionHistoryFragmentBinding;

public class SessionHistoryFragment extends Fragment {

    /*
        <data>
        <variable name="sessionData" type="ugent.waves.healthrecommenderapp.dataclasses.SessionHistoryData"/>
        <import type="android.view.View"/>
    </data>
     */

    private static final String ARG_DATA = "sessionId";
    private static final String TAG = "SessionHistoryFragment";
    private int id;
    private healthRecommenderApplication app;

    private AppDatabase appDb;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = (healthRecommenderApplication) getContext().getApplicationContext();

        appDb = app.getAppDb();

        id = getArguments().getInt(ARG_DATA);

        getSessionActivities();
        // Inflate the layout for this fragment
        SessionHistoryFragmentBinding binding = DataBindingUtil.inflate(inflater, R.layout.session_history_fragment, container, false);
        View view = binding.getRoot();
        //binding.setSessionData(data);
        return view;

    }

    //TODO: hoeveel minuten gedaan van elke beweging?
    //TODO: tijdlijn
    private void getSessionActivities() {
        try {
            SessionActivity[] a = new ActivityAsyncTask(getActivity(), appDb, id).execute().get();
            Mistake[] m = new MistakeAsyncTask(getActivity(), appDb, id).execute().get();

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static SessionHistoryFragment newInstance(int sessionId) {
        SessionHistoryFragment fragment = new SessionHistoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DATA, sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    private static class ActivityAsyncTask extends AsyncTask<Void, Void, SessionActivity[]> {
        //Prevent leak
        private WeakReference<Activity> weakActivity;
        private AppDatabase db;
        private int id;

        public ActivityAsyncTask(Activity activity, AppDatabase db, int id) {
            weakActivity = new WeakReference<>(activity);
            this.db = db;
            this.id = id;
        }

        @Override
        protected SessionActivity[] doInBackground(Void... params) {
            ActivityDao activityDao = db.activityDao();
            return activityDao.getActivitiesForSession(id);
        }
    }

    private static class MistakeAsyncTask extends AsyncTask<Void, Void, Mistake[]> {
        //Prevent leak
        private WeakReference<Activity> weakActivity;
        private AppDatabase db;
        private int id;

        public MistakeAsyncTask(Activity activity, AppDatabase db, int id) {
            weakActivity = new WeakReference<>(activity);
            this.db = db;
            this.id = id;
        }

        @Override
        protected Mistake[] doInBackground(Void... params) {
            MistakeDao mistakeDao = db.mistakeDao();
            return mistakeDao.getMistakesForSession(id);
        }
    }
}
