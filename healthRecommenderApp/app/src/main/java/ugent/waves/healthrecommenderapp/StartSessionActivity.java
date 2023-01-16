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

import ugent.waves.healthrecommenderapp.Asynctasks.RecommendationAsyncTask;
import ugent.waves.healthrecommenderapp.HelpClasses.Constants;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.Persistance.RecommendationDao;

public class StartSessionActivity extends AppCompatActivity implements NodeAdapter.RecyclerViewItemClickListener {
    private int recommendationId;
    private healthRecommenderApplication app;
    private NodeClient nodeClient;
    private MessageClient messageClient;
    private NodeDialog nodeDialog;
    private StartSessionActivity context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_session);

        app = (healthRecommenderApplication) getApplicationContext();
        context = this;
        nodeClient = Wearable.getNodeClient(this);
        messageClient = Wearable.getMessageClient(this);

        try {
            Intent i = getIntent();
            recommendationId = i.getIntExtra(Constants.RECOMMENDATION_ID, -1);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void startSession(View view) {
        try {
            Recommendation r = new RecommendationIdAsyncTask(app.getAppDb(), recommendationId).execute().get();
            if(!r.isPending()){
                r.setPending(true);
                new RecommendationAsyncTask(app.getAppDb(),null,Constants.UPDATE, r).execute().get();
            }

            nodeClient.getConnectedNodes()
                    .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                        @Override
                        public void onSuccess(List<Node> nodes) {
                            try{
                                NodeAdapter dataAdapter = new NodeAdapter(nodes, context);
                                nodeDialog = new NodeDialog(StartSessionActivity.this, dataAdapter);

                                nodeDialog.show();
                            } catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                    });

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void clickOnItem(Node node) {
        messageClient.sendMessage(node.getId(), Constants.PATH, new byte[]{})
                .addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        //gelukt
                    }
                });

        if (nodeDialog != null){
            nodeDialog.dismiss();
        }
    }

    private static class RecommendationIdAsyncTask extends AsyncTask<Void, Void, Recommendation> {
        private AppDatabase db;
        private int id;

        public RecommendationIdAsyncTask( AppDatabase db, int id) {
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
