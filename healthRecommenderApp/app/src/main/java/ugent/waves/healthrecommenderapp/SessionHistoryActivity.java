package ugent.waves.healthrecommenderapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import ugent.waves.healthrecommenderapp.HelpClasses.goalHandler;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.Recommendation;
import ugent.waves.healthrecommenderapp.Persistance.RecommendationDao;
import ugent.waves.healthrecommenderapp.Services.NotificationCallback;
import ugent.waves.healthrecommenderapp.dataclasses.SessionHistoryData;
import ugent.waves.healthrecommenderapp.sessionHistory.SessionHistoryListFragment;

//TODO: show start + end time rope skipping session
//TODO: show turns + mistake timestamps
public class SessionHistoryActivity extends AppCompatActivity implements NotificationCallback {

    //recycler view
    private List<SessionHistoryData> data = new ArrayList<SessionHistoryData>();

    //permissions
    String[] appPermissions = {}; //Manifest.permission.ACTIVITY_RECOGNITION
    private boolean permissionsGranted;

    //constants
    private static final String TAG = "SessionHistoryActivity";
    private static final int RC_SIGN_IN = 1;
    private static final int PERMISSION_REQUEST = 2;
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 3;

    //google sign in
    private GoogleSignInAccount account;
    private FitnessOptions fitnessOptions;

    private healthRecommenderApplication app;

    //firebase
    private FirebaseFirestore db;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private Random rand;
    private ArrayList<ActivityTransition> transitions;
    private PendingIntent pendingIntent;

    private AppDatabase appDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_history);

        //TODO: remove random when done testing
        rand = new Random();

        app = (healthRecommenderApplication) this.getApplicationContext();

        //google sign in
        // Set options for Google Sign-In and get client instance
        fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_HEART_POINTS, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                .build();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestId()
                .requestIdToken("726533384380-p8e9er5fialjs892tu5c5aub0dgqsbb0.apps.googleusercontent.com")
                .addExtension(fitnessOptions)
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        appDb = app.getAppDb();

        registerReceiver(broadcastReceiver, new IntentFilter("SEND_NOTIFICATION"));

        //TODO: voor firebase auth moet iedere keer expliciet ingelogd worden...
        //account = GoogleSignIn.getLastSignedInAccount(this);
        if(account == null){
            signIn();
        } else{
            permissionsGranted = checkAndRequestPermissions();
            firebaseAuthWithGoogle(account);
            accessApp();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        account = GoogleSignIn.getLastSignedInAccount(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(broadcastReceiver);
        ActivityRecognition.getClient(this)
                .removeActivityTransitionUpdates(pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e(TAG, "suc");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "suc");
                    }
                });
    }

    private void accessApp(){
        // Check for necessary permissions (and request in case no permissions were granted)
        if (permissionsGranted) {
            // Add the fragment to the 'fragment_container' FrameLayout
            switchContent(R.id.container,SessionHistoryListFragment.newInstance(data));
            
            //initialize firestore db
            db = app.getFirestore();

            //get_session_data();
            showRecyclerView();

            goalHandler g = new goalHandler(null, this, app);
            //g.generateRecommendations();
            initActivityDetection();
        }
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
                        Log.d(TAG, "");
                    }
                });
    }

    /*
    FRAGMENT/RECYCLERVIEW
     */

    private void showRecyclerView() {
        Fragment fr = SessionHistoryListFragment.newInstance(data);
        switchContent(R.id.container, fr);
    }

    public void switchContent(int id, Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(id, fragment, fragment.toString());
        ft.addToBackStack(null);
        ft.commit();
    }


        /*
    RECOMMENDATION NOTIFICATION
     */
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sendRecommendation();
            }
        };

    @Override
    public void sendRecommendation(){
        //TODO: get rank from app
        try {
            Recommendation r = new RecommendationAsyncTask(this, appDb, 0).execute().get();
            sendNotification(r.getActivity()+r.getDuration()+"");
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, SessionHistoryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 , intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = "id";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.badminton)
                        .setContentTitle("notification")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

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

    private void initHistoryData() {
        /*
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long endTime = cal.getTimeInMillis();
        //cal.add(Calendar.DAY_OF_WEEK, -1);
        cal.add(Calendar.WEEK_OF_YEAR, -3);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startTime = cal.getTimeInMillis();


        SessionReadRequest readRequest = new SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(DataType.TYPE_HEART_RATE_BPM)
                .readSessionsFromAllApps()
                .enableServerQueries()
                .build();

        Fitness.getSessionsClient(this, account)
                .readSession(readRequest)
                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                    @Override
                    public void onSuccess(SessionReadResponse sessionReadResponse) {
                        List<Session> sessions = sessionReadResponse.getSessions();
                        for (Session session : sessions) {
                            String activity = session.getActivity();
                            int imgId = getImage(activity);
                            long start = session.getStartTime(TimeUnit.MILLISECONDS);
                            long end = session.getEndTime(TimeUnit.MILLISECONDS);
                            List<DataSet> dataSets = sessionReadResponse.getDataSet(session);
                            //SessionHistoryData s = new SessionHistoryData(activity, imgId, dataSets, null, null);
                            //data.add(s);
                        }
                        showRecyclerView();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });*/
    }

    /*
    GOOGLE + FIREBASE SIGNIN
     */

    private void signIn() {
        // Launches the sign in flow, the result is returned in onActivityResult
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            account = completedTask.getResult(ApiException.class);
            permissionsGranted = checkAndRequestPermissions();
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("tag", "signInResult:failed code=" + e.getStatusCode());
        }
    }

    // Check permissions
    private boolean checkAndRequestPermissions() {
        // Check which permissions are granted
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : appPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }

        // Ask for non-granted permissions
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    PERMISSION_REQUEST
            );
            //TODO: nadat user permissies heeft toegestaan true returnen?
            return false;
        } else if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    account,
                    fitnessOptions);
            //TODO: nadat user permissies heeft toegestaan true returnen?
            return false;
        }

        // has all permissions
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            HashMap<String, Integer> permissionResults = new HashMap<>();

            // Gather permission grant results
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults.put(permissions[i], grantResults[i]);
                }
            }
            // At least one or all permissions are denied --> Ask again with rationale
            if(permissionResults.keySet().size() > 0) {
                for (Map.Entry<String, Integer> entry : permissionResults.entrySet()) {
                    String permName = entry.getKey();
                    int permResult = entry.getValue();

                    // Permission is denied (this is the first time, when "never ask again" is not checked)
                    // so ask again explaining the usage of permission
                    // shouldShowRequestPermissionRationale will return true
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permName)) {
                        // Show dialog of explanation
                        showDialog("", "This app needs access to sensors and location to work without problems.",
                                "Yes, grant permissions",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        checkAndRequestPermissions();
                                    }
                                },
                                "No, exit app", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        finish();
                                    }
                                }, false);
                    } else {
                        // Ask user to go to settings and manually allow permissions
                        showDialog("",
                                "You have denied some permissions. Allow all permissions in [Settings] > [Permissions]",
                                "Go to settings",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        //Go to settings
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", getPackageName(), null));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    }
                                },
                                "No, exit app", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        finish();
                                    }
                                }, false);
                        break;
                    }
                }
            }
        }
    }

    public AlertDialog showDialog(String title, String msg, String positiveLabel,
                                  DialogInterface.OnClickListener positiveOnClick,
                                  String negativeLabel, DialogInterface.OnClickListener negativeOnClick,
                                  boolean isCancelAble) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(isCancelAble);
        builder.setMessage(msg);
        builder.setPositiveButton(positiveLabel, positiveOnClick);
        builder.setNegativeButton(negativeLabel, negativeOnClick);

        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    //TODO: security rules so users can only access their own data
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.e(TAG, "firebaseAuthWithGoogle:" + acct.getIdToken());
        mAuth = FirebaseAuth.getInstance();

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            user = mAuth.getCurrentUser();
                            accessApp();
                            Log.e(TAG, user.getDisplayName());
                            Log.d(TAG, "signInWithCredential:success");
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                        }
                    }
                });
    }


    private static class RecommendationAsyncTask extends AsyncTask<Void, Void, Recommendation> {
        //Prevent leak
        private WeakReference<Activity> weakActivity;
        private AppDatabase db;
        private int rank;

        public RecommendationAsyncTask(Activity activity, AppDatabase db, int rank) {
            weakActivity = new WeakReference<>(activity);
            this.db = db;
            this.rank = rank;
        }

        @Override
        protected Recommendation doInBackground(Void... params) {
            RecommendationDao recommendationDao = db.recommendationDao();
            return recommendationDao.getRecommendationWithNr(rank);
        }
    }

}
