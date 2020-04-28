package ugent.waves.healthrecommenderapp.Recommendation;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.Persistance.RecommendationDao;
import ugent.waves.healthrecommenderapp.R;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

//TODO: uitgrijzen van done recommendations
public class RecommendationListFragment extends Fragment {

    private healthRecommenderApplication app;

    private AppDatabase appDb;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private Recommendation[] data;
    private RecommendationAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //recyclerview
        View rootView = inflater.inflate(R.layout.recommendation_list_fragment, container, false);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recommendation_recycler_view);

        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        app = (healthRecommenderApplication) getContext().getApplicationContext();

        appDb = app.getAppDb();

        //List<SessionHistoryData> data = getArguments().<SessionHistoryData>getParcelableArrayList(ARG_DATA);
        get_recommendations();

        mAdapter = new RecommendationAdapter(data, getContext());
        recyclerView.setAdapter(mAdapter);

        return rootView;

    }

    private void get_recommendations() {
        try {
            data = new RecommendationAsyncTask(getActivity(), appDb).execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class RecommendationAsyncTask extends AsyncTask<Void, Void, Recommendation[]> {
        //Prevent leak
        private WeakReference<Activity> weakActivity;
        private AppDatabase db;

        public RecommendationAsyncTask(Activity activity, AppDatabase db) {
            weakActivity = new WeakReference<>(activity);
            this.db = db;
        }

        @Override
        protected Recommendation[] doInBackground(Void... params) {
            RecommendationDao recommendationDao = db.recommendationDao();
            return recommendationDao.getAllRecommendations();
        }
    }

}
