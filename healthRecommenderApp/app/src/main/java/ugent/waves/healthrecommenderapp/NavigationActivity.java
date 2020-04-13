package ugent.waves.healthrecommenderapp;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import ugent.waves.healthrecommenderapp.HelpClasses.goalHandler;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.Persistance.RecommendationDao;
import ugent.waves.healthrecommenderapp.Recommendation.RecommendationListFragment;
import ugent.waves.healthrecommenderapp.Services.NotificationCallback;
import ugent.waves.healthrecommenderapp.Services.broadcastReceiver;
import ugent.waves.healthrecommenderapp.dataclasses.SessionHistoryData;
import ugent.waves.healthrecommenderapp.sessionHistory.SessionHistoryListFragment;

public class NavigationActivity extends AppCompatActivity implements NotificationCallback {

    private static final String TAG = "NavigationActivity";
    private static final String ACTION_SNOOZE = "SNOOZE";
    private static final String ACTIVITY_ID = "ACTIVITY_ID";
    private static final String RECOMMENDATION_ID = "RECOMMENDATION_ID";
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView nvDrawer;

    // Make sure to be using androidx.appcompat.app.ActionBarDrawerToggle version.
    private ActionBarDrawerToggle drawerToggle;

    //recycler view
    private List<SessionHistoryData> data = new ArrayList<SessionHistoryData>();
    private healthRecommenderApplication app;
    private AppDatabase appDb;

    private ArrayList<ActivityTransition> transitions;
    private PendingIntent pendingIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
        NAVIGATION INITIALISATION
         */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_navigation);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // This will display an Up icon (<-), we will replace it with hamburger later
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        nvDrawer = (NavigationView) findViewById(R.id.nvView);

        nvDrawer.getMenu().findItem(R.id.logout).setOnMenuItemClickListener(menuItem -> {
            logout();
            return true;
        });

        setupDrawerContent(nvDrawer);

        drawerToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open,  R.string.drawer_close);
        // Setup toggle to display hamburger icon with nice animation
        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerToggle.syncState();

        // Tie DrawerLayout events to the ActionBarToggle
        mDrawer.addDrawerListener(drawerToggle);


        /*
        APP INITIALISATION
         */
        Class fragmentClass = SessionHistoryListFragment.class;
        try {
            Fragment fragment = (Fragment) fragmentClass.newInstance();
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        app = (healthRecommenderApplication) this.getApplicationContext();
        appDb = app.getAppDb();

        if (nvDrawer.getHeaderCount() > 0) {
            TextView mName = (TextView) nvDrawer.getHeaderView(0).findViewById(R.id.email);
            ImageView mImageView = (ImageView) nvDrawer.getHeaderView(0).findViewById(R.id.circleImage);

            mName.setText(app.getAccount().getEmail());
            Picasso.get().load(app.getAccount().getPhotoUrl()).into(mImageView);
        }

        registerReceiver(broadcast_receiver, new IntentFilter("SEND_NOTIFICATION"));

        goalHandler g = new goalHandler(null, this, app);

        //g.generateRecommendations();
        //sendRecommendation();

    }

    private void logout() {
        app.getmGoogleSignInClient().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    public void selectDrawerItem(MenuItem menuItem) {
        // Create a new fragment and specify the fragment to show based on nav item clicked
        Fragment fragment = null;
        Class fragmentClass;
        Log.e("", menuItem.getItemId()+" " + R.id.history);
        switch(menuItem.getItemId()) {
            case R.id.history:
                fragmentClass = SessionHistoryListFragment.class;
                break;
            case R.id.recommendation:
                fragmentClass = RecommendationListFragment.class;
                break;
            default:
                fragmentClass = SessionHistoryListFragment.class;
        }

        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();

        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);
        // Set action bar title
        setTitle(menuItem.getTitle());
        // Close the navigation drawer
        mDrawer.closeDrawers();
    }

    private void initActivityDetection(){
        transitions = new ArrayList<>();
        //detect when user can't or should perform a physical activity
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());

        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        Intent i = new Intent(this, ugent.waves.healthrecommenderapp.Services.broadcastReceiver.class);
        i.setAction("ACTIVITY_RECOGNITION");

        pendingIntent = PendingIntent.getBroadcast(this, 7, i, PendingIntent.FLAG_UPDATE_CURRENT);

        ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("", "");
                    }
                });
    }

    /*
 RECOMMENDATION NOTIFICATION
  */
    BroadcastReceiver broadcast_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sendRecommendation();
        }
    };

    @Override
    public void sendRecommendation(){
        //TODO: testen
        try {
            Recommendation[] r = new RecommendationAsyncTask(this, appDb).execute().get();
            //random recommendation
            int index = (int) Math.floor(Math.random()*r.length);
            sendNotification(r[index].getUid(), r[index].getActivity(), r[index].getActivity()+r[index].getDuration()+"");
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendNotification(int recommendationId, int activityId, String messageBody) {
        //**add this line**
        int requestID = (int) System.currentTimeMillis();

        //TODO: check flags
        Intent intent = new Intent(getApplicationContext(), StartSessionActivity.class);
        intent.putExtra(ACTIVITY_ID, activityId);
        intent.putExtra(RECOMMENDATION_ID, recommendationId);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID , intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = "id";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Intent snoozeIntent = new Intent(this, broadcastReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        //snoozeIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        PendingIntent snoozePendingIntent =
                PendingIntent.getBroadcast(getApplicationContext(), requestID, snoozeIntent, 0);

        //TODO: taal
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.rope_skipping)
                        .setContentTitle("recommendation")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .addAction(R.drawable.rope_skipping, "snooze", snoozePendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
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
            return recommendationDao.getRecommendationWithDone(false);
        }
    }
}
