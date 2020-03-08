package ugent.waves.wearableapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SessionActivity extends WearableActivity {

    private String TAG="SessionActivity";

    private GoogleSignInAccount account;
    private OnDataPointListener mListener;

    private DataSource heartRateDataSource;
    private Session session;
    private String activity;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        // Enables Always-on
        setAmbientEnabled();

        Intent intent = getIntent();
        activity = intent.getStringExtra(ActivityListAdapter.ACTIVITY);

        account = GoogleSignIn.getLastSignedInAccount(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            initFitnessListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterFitnessDataListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterFitnessDataListener();
        endSession();
    }

    private void startSession() {
        Fitness.getRecordingClient(this, account)
                .subscribe(DataType.TYPE_HEART_RATE_BPM)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Successfully subscribed (heart rate)!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "There was a problem subscribing (heart rate).");
                    }
                });

        // Retrieve current time in milliseconds
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long startTime = cal.getTimeInMillis();
        // Set type of workout
        String activityType = null;
        if (activity.equalsIgnoreCase("running")) {
            activityType = FitnessActivities.RUNNING;
        } else if (activity.equalsIgnoreCase("biking")) {
            activityType = FitnessActivities.BIKING;
        } else if (activity.equalsIgnoreCase("badminton")) {
            activityType = FitnessActivities.BADMINTON;
        } else {
            activityType = FitnessActivities.OTHER;
        }

        session = new Session.Builder()
                //.setName(startTime+"")
                //.setIdentifier(startTime+"")
                //.setDescription(startTime+"")
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .setActivity(activityType)
                .build();

        Task<Void> response = Fitness.getSessionsClient(this, account)
                .startSession(session)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Session successfully started");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "There was a problem starting session");
                    }
                });
    }

    private void endSession() {
        Log.e(TAG, session.getActivity());
        Fitness.getSessionsClient(this, account)
                .stopSession(null)
                .addOnSuccessListener(new OnSuccessListener<List<Session>>() {
                    @Override
                    public void onSuccess(List<Session> sessions) {
                        Log.i(TAG, "Session successfully stopped");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "There was a problem stopping session");
                    }
                });

        Fitness.getRecordingClient(this, account)
                .unsubscribe(DataType.TYPE_HEART_RATE_BPM)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Successfully unsubscribed for data type");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Failed to unsubscribe for data type");
                    }
                });
    }

    private void initFitnessListener() {
        DataSourcesRequest requestData = new DataSourcesRequest.Builder()
                .setDataTypes(DataType.TYPE_HEART_RATE_BPM)
                .setDataSourceTypes(DataSource.TYPE_RAW)
                .build();

        Fitness.getSensorsClient(this, account)
                .findDataSources(requestData)
                .addOnSuccessListener(
                        new OnSuccessListener<List<DataSource>>() {
                            @Override
                            public void onSuccess(List<DataSource> dataSources) {
                                for (DataSource dataSource : dataSources) {
                                    if (dataSource.getDataType().equals(DataType.TYPE_HEART_RATE_BPM)
                                            && mListener == null) {
                                        Log.d(TAG, dataSource.toString());
                                        heartRateDataSource = dataSource;
                                        // Call method to register HR sensor
                                        registerFitnessDataListener(dataSource, DataType.TYPE_HEART_RATE_BPM);
                                    }
                                }
                            }
                        });
    }
    private void registerFitnessDataListener(final DataSource dataSource, final DataType dataType) {
        // [START register_data_listener]
        mListener =
                new OnDataPointListener() {
                    @Override
                    public void onDataPoint(DataPoint dataPoint) {
                        for (Field field : dataPoint.getDataType().getFields()) {
                            Value val = dataPoint.getValue(field);
                            Log.e(TAG, val.toString());
                        }
                    }
                };

        SensorRequest requestSensor = new SensorRequest.Builder()
                .setDataSource(dataSource)
                .setDataType(dataType)
                .setSamplingRate(1, TimeUnit.SECONDS)
                .build();

        Fitness.getSensorsClient(this, account)
                .add(requestSensor, mListener)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.i(TAG, "Listener registered!");
                                } else {
                                    Log.e(TAG, "Listener not registered.", task.getException());
                                }
                            }
                        });
        // [END register_data_listener]
    }

    private void unregisterFitnessDataListener() {
        if (mListener == null) {
            // If there is no registered listener, there is nothing to unregister
            return;
        }

        // [START unregister_data_listener]
        Fitness.getSensorsClient(this, account)
                .remove(mListener)
                .addOnCompleteListener(
                        new OnCompleteListener<Boolean>() {
                            @Override
                            public void onComplete(@NonNull Task<Boolean> task) {
                                if (task.isSuccessful() && task.getResult()) {
                                    Log.i(TAG, "Listener was removed!");
                                } else {
                                    Log.i(TAG, "Listener was not removed." + task.getException());
                                }
                            }
                        });
        // [END unregister_data_listener]
    }

    public void onClickStartSession(View view) {
        initFitnessListener();
        startSession();
    }

    public void onClickStopSession(View view) {
        unregisterFitnessDataListener();
        endSession();
    }
}
