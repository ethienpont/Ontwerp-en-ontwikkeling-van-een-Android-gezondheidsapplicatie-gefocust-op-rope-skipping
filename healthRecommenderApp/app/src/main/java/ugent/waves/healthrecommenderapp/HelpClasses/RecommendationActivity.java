package ugent.waves.healthrecommenderapp.HelpClasses;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.R;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

public class RecommendationActivity extends Fragment {

    private healthRecommenderApplication app;

    private AppDatabase appDb;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = (healthRecommenderApplication) getContext().getApplicationContext();

        appDb = app.getAppDb();

        View view = inflater.inflate(R.layout.activity_recommendation, container, false);

        return view;

    }

}
