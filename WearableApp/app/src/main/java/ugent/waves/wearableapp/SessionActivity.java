package ugent.waves.wearableapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SessionActivity extends WearableActivity {

    private String TAG="SessionActivity";
    private GoogleSignInAccount account;
    private OnDataPointListener mListener;

    private DataSource heartRateDataSource;
    private Session session;
    private String activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        Intent intent = getIntent();
        activity = intent.getStringExtra(ActivityListAdapter.ACTIVITY);

        account = GoogleSignIn.getLastSignedInAccount(this);


    }

    private void startSession(String activity, GoogleSignInAccount account) {
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
        } else if (activity.equalsIgnoreCase("other")) {
            activityType = FitnessActivities.OTHER;
        }

        session = new Session.Builder()
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

    private void endSession(final GoogleSignInAccount account) {
        Fitness.getSessionsClient(this, account)
                .stopSession(session.getIdentifier())
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

        /*
        DataSet heartRateDataSet = DataSet.create(heartRateDataSource);
        DataPoint p = DataPoint.builder(heartRateDataSource)
                .setTimeInterval(session.getStartTime(TimeUnit.MILLISECONDS), session.getEndTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
                .setTimestamp(new Date().getTime(), TimeUnit.MILLISECONDS)
                .setField(Field.FIELD_BPM, 65)
                .build();
        heartRateDataSet.add(p);

        Session sessionInsert = new Session.Builder()
                .setName("test")
                .setDescription("Long run around Shoreline Park")
                .setIdentifier(session.getIdentifier())
                .setActivity(FitnessActivities.OTHER)
                .setStartTime(session.getStartTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
                .setEndTime(session.getEndTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
                .build();

        SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                .setSession(sessionInsert)
                .addDataSet(heartRateDataSet)
                .build();

        Fitness.getSessionsClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .insertSession(insertRequest)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // At this point, the session has been inserted and can be read.
                        Log.i(TAG, "Session insert was successful!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "There was a problem inserting the session: " +
                                e.getLocalizedMessage());
                    }
                });*/

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

    private void initFitnessListener(final GoogleSignInAccount account) {
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
                            Log.d(TAG, val.toString());
                        }
                    }
                };

        SensorRequest requestSensor = new SensorRequest.Builder()
                .setDataSource(dataSource)
                .setDataType(dataType)
                .setSamplingRate(1, TimeUnit.SECONDS)
                .build();

        Fitness.getSensorsClient(this, GoogleSignIn.getLastSignedInAccount(this))
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
        Fitness.getSensorsClient(this, GoogleSignIn.getLastSignedInAccount(this))
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
        startSession(activity, account);
        initFitnessListener(account);
    }

    public void onClickStopSession(View view) {
        endSession(account);
        unregisterFitnessDataListener();
    }
}
