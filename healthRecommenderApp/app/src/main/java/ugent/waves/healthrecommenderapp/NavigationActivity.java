package ugent.waves.healthrecommenderapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeClient;
import com.google.android.gms.wearable.Wearable;
import com.google.android.material.navigation.NavigationView;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import ugent.waves.healthrecommenderapp.Asynctasks.UserAsyncTask;
import ugent.waves.healthrecommenderapp.HelpClasses.Constants;
import ugent.waves.healthrecommenderapp.HelpClasses.goalHandler;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.User;
import ugent.waves.healthrecommenderapp.Recommendation.RecommendationListFragment;
import ugent.waves.healthrecommenderapp.sessionHistory.SessionHistoryListFragment;

public class NavigationActivity extends AppCompatActivity {

    private static final String TAG = "NavigationActivity";
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView nvDrawer;

    private ActionBarDrawerToggle drawerToggle;

    //recycler view
    private healthRecommenderApplication app;

    private NodeClient nodeClient;
    private NodeDialog nodeDialog;
    private AppDatabase appDb;


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
        app = (healthRecommenderApplication) this.getApplicationContext();
        appDb = app.getAppDb();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        int firstRun = prefs.getInt(Constants.PREF_FIRST_RUN, Constants.DOESNT_EXIST);

        OneTimeWorkRequest d = new OneTimeWorkRequest.Builder(goalHandler.class).build();
        WorkManager.getInstance(this).enqueue(d);
        if (firstRun == Constants.DOESNT_EXIST) {
            try{
                PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(goalHandler.class, 24, TimeUnit.HOURS, 23, TimeUnit.HOURS).build();
                WorkManager.getInstance(this).enqueueUniquePeriodicWork("Recommendations", ExistingPeriodicWorkPolicy.REPLACE, work);
                OneTimeWorkRequest one = new OneTimeWorkRequest.Builder(goalHandler.class).build();
                WorkManager.getInstance(this).enqueue(one);
            } catch(Exception e){
                e.printStackTrace();
            }

            prefs.edit().putInt(Constants.PREF_FIRST_RUN, 1).apply();
        }

        nodeClient = Wearable.getNodeClient(this);

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

        if (nvDrawer.getHeaderCount() > 0) {
            TextView mName = (TextView) nvDrawer.getHeaderView(0).findViewById(R.id.email);
            ImageView mImageView = (ImageView) nvDrawer.getHeaderView(0).findViewById(R.id.circleImage);

            mName.setText(app.getAccount().getEmail());
            Picasso.get().load(app.getAccount().getPhotoUrl()).into(mImageView);
        }
    }

    private void logout() {
        app.getmGoogleSignInClient().signOut();
        try {
            User u = new UserAsyncTask(app.getAppDb(), app.getAccount().getId(), Constants.GET).execute().get();
            u.setCurrent(false);
            AsyncTask.execute(() -> app.getAppDb().userDao().updateUser(u));
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

        if(fragmentClass != null){
            try {
                fragment = (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Insert the fragment by replacing any existing fragment
            switchContent(R.id.flContent, fragment);
        }

        // Set action bar title
        setTitle(menuItem.getTitle());
        // Close the navigation drawer
        mDrawer.closeDrawers();
    }

    public void switchContent(int id, Fragment f) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(id, f).commit();
    }

}
