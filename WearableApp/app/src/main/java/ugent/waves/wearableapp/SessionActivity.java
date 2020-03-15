package ugent.waves.wearableapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.wear.ambient.AmbientMode;
import androidx.wear.ambient.AmbientModeSupport;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SessionActivity extends FragmentActivity implements SensorEventListener, AmbientMode.AmbientCallbackProvider {

    private String TAG="SessionActivity";

    private GoogleSignInAccount account;
    private OnDataPointListener mListener;

    private DataSource heartRateDataSource;
    private Session session;
    private String activity;
    private GoogleSignInClient mGoogleSignInClient;

    private DatabaseReference mDatabase;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private SensorManager sensorManager;
    private Sensor acceleroSensor;
    private FirebaseFirestore firestore;
    private List<Map> accelero_dataPoints;
    private List<Map> heart_rate_dataPoints;
    private String currentSession;
    private AmbientModeSupport.AmbientController ambientController;
    private Map<String,Object> sessionData;
    private Sensor heartRateSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        // Enables Always-on
        ambientController = AmbientModeSupport.attach(this);

        Intent intent = getIntent();
        activity = intent.getStringExtra(ActivityListAdapter.ACTIVITY);

        //account = GoogleSignIn.getLastSignedInAccount(this);

        //mDatabase = FirebaseDatabase.getInstance().getReference();

        firestore = FirebaseFirestore.getInstance();

        sensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);
        acceleroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_BEAT);
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
        //unregisterFitnessDataListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //unregisterFitnessDataListener();
        //endSession();
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

    private void startRopeSkippingSession() {
        accelero_dataPoints = new ArrayList<>();
        heart_rate_dataPoints = new ArrayList<>();
        sensorManager.registerListener(this, acceleroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void endRopeSkippingSession() {
        sensorManager.unregisterListener(this);
        currentSession = UUID.randomUUID().toString();
        //ACCELEROMETER
        for(Map point : accelero_dataPoints){
            firestore.collection("users")
                    .document("testUser")
                    .collection("sessions")
                    .document("rope_skipping_accelerometer")
                    .collection(currentSession)
                    .add(point)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding document", e);
                        }
                    });
        }
        HashMap<String,Object> sessionId = new HashMap<>();
        sessionId.put("id",currentSession);
        firestore.collection("users")
                .document("testUser")
                .collection("sessions")
                .document("rope_skipping_accelerometer")
                .collection("session_ids")
                .add(sessionId)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
        firestore.collection("users")
                .document("testUser")
                .collection("sessionCalculations")
                .document(currentSession)
                .set(sessionId)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.w(TAG, "Error adding document");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });

        //HEART RATE
        for(Map point : heart_rate_dataPoints){
            firestore.collection("users")
                    .document("testUser")
                    .collection("sessions")
                    .document("rope_skipping_heart_rate")
                    .collection(currentSession)
                    .add(point)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding document", e);
                        }
                    });
        }
        /*TODO: niet hier history opvragen
        final DocumentReference docRef = firestore.collection("users")
                .document("testUser")
                .collection("sessionCalculations")
                .document(currentSession);
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                String source = snapshot != null && snapshot.getMetadata().hasPendingWrites()
                        ? "Local" : "Server";

                if (snapshot != null && snapshot.exists() && source.equals("Server")) {
                    Log.d(TAG, "Current data: " + snapshot.getData());
                    sessionData = snapshot.getData();
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });*/
    }

    public void switchContent(int id, Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(id, fragment, fragment.toString());
        ft.addToBackStack(null);
        ft.commit();
    }

    public void onClickStartSession(View view) {
        if(activity.equals("rope skipping")){
            startRopeSkippingSession();
        } else{
            initFitnessListener();
            startSession();
        }
    }

    public void onClickStopSession(View view) {
        if(activity.equals("rope skipping")){
            endRopeSkippingSession();
        } else{
            unregisterFitnessDataListener();
            endSession();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("time", System.nanoTime());
            dataPoint.put("x", event.values[0]);
            dataPoint.put("y", event.values[1]);
            dataPoint.put("z", event.values[2]);
            accelero_dataPoints.add(dataPoint);
        } else if(event.sensor.getType() == Sensor.TYPE_HEART_BEAT){
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("time", System.nanoTime());
            dataPoint.put("heart_rate", event.values[0]);
            heart_rate_dataPoints.add(dataPoint);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public AmbientMode.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    //TODO: hier niet history sessies tonen
    public void onClickData(View view) {
        switchContent(R.id.container,SessionFragment.newInstance(sessionData));
    }

    private class MyAmbientCallback extends AmbientMode.AmbientCallback {
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            // Handle entering ambient mode
        }

        @Override
        public void onExitAmbient() {
            // Handle exiting ambient mode
        }

        @Override
        public void onUpdateAmbient() {
            // Update the content
        }
    }
}
