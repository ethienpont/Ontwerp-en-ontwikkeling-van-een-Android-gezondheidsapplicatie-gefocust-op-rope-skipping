package ugent.waves.healthrecommenderapp;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeClient;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.appcompat.app.AppCompatActivity;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.Persistance.RecommendationDao;

public class StartSessionActivity extends AppCompatActivity {
    private static final String ACTIVITY_ID = "ACTIVITY_ID";
    private static final String RECOMMENDATION_ID = "RECOMMENDATION_ID";
    private String PATH = "/SESSION_START";
    private int activity;
    private int recommendationId;
    private healthRecommenderApplication app;
    private NodeClient nodeClient;
    private MessageClient messageClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_session);

        app = (healthRecommenderApplication) getApplicationContext();
        nodeClient = Wearable.getNodeClient(this);
        messageClient = Wearable.getMessageClient(this);

        try {
            Intent i = getIntent();
            activity = i.getIntExtra(ACTIVITY_ID, -1);
            recommendationId = i.getIntExtra(RECOMMENDATION_ID, -1);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void startSession(View view) {
        try {
            Recommendation r = new RecommendationAsyncTask(this, app.getAppDb(), recommendationId).execute().get();
            r.setPending(true);
            AsyncTask.execute(
                    () -> {app.getAppDb().recommendationDao().updateRecommendation(r);}
            );

            //TODO: choose node to connect too
            nodeClient.getConnectedNodes()
                    .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                        @Override
                        public void onSuccess(List<Node> nodes) {
                            for(Node node : nodes) {
                                messageClient.sendMessage(node.getId(), PATH, new byte[]{})
                                        .addOnSuccessListener(new OnSuccessListener<Integer>() {
                                            @Override
                                            public void onSuccess(Integer integer) {
                                                //gelukt
                                            }
                                        });
                            }
                        }
                    });

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(){

    }

    private static class RecommendationAsyncTask extends AsyncTask<Void, Void, Recommendation> {
        //Prevent leak
        private WeakReference<Activity> weakActivity;
        private AppDatabase db;
        private int id;

        public RecommendationAsyncTask(Activity activity, AppDatabase db, int id) {
            weakActivity = new WeakReference<>(activity);
            this.db = db;
            this.id = id;
        }

        @Override
        protected Recommendation doInBackground(Void... params) {
            RecommendationDao recommendationDao = db.recommendationDao();
            return recommendationDao.getRecommendationForId(id);
        }
    }
}
