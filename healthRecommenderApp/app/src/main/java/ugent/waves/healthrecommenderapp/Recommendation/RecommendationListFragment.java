package ugent.waves.healthrecommenderapp.Recommendation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.ExecutionException;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ugent.waves.healthrecommenderapp.Asynctasks.RecommendationAsyncTask;
import ugent.waves.healthrecommenderapp.HelpClasses.Constants;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.R;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

//Show recommendations in recyclerview
public class RecommendationListFragment extends Fragment {

    private healthRecommenderApplication app;

    private AppDatabase appDb;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private Recommendation[] data;
    private RecommendationAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recommendation_list_fragment, container, false);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recommendation_recycler_view);

        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        app = (healthRecommenderApplication) getContext().getApplicationContext();

        appDb = app.getAppDb();

        get_recommendations();

        mAdapter = new RecommendationAdapter(data, getContext());
        recyclerView.setAdapter(mAdapter);

        return rootView;
    }

    private void get_recommendations() {
        try {
            data = new RecommendationAsyncTask(appDb,app.getAccount().getId(), Constants.GET_ALL, null).execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
