package ugent.waves.healthrecommenderapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class StartSessionActivity extends AppCompatActivity {
    private static final String ACTIVITY_ID = "ACTIVITY_ID";
    private static final String RECOMMENDATION_ID = "RECOMMENDATION_ID";
    private int activity;
    private int recommendationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_session);

        try {
            Intent i = getIntent();
            activity = i.getIntExtra(ACTIVITY_ID, -1);
            recommendationId = i.getIntExtra(RECOMMENDATION_ID, -1);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void startSession(View view) {
        //TODO: set recommendation on pending
        Log.d("TAH", "lal");
    }
}
