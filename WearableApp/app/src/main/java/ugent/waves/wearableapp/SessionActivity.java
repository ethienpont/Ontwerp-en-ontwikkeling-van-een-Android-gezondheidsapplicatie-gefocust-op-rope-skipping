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
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeClient;
import com.google.android.gms.wearable.Wearable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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
    private String ACCELEROMETER = "/ACCELEROMETER";

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
    private NodeClient nodeClient;
    private MessageClient messageClient;
    private String STOP = "/STOP";
    private static final String START = "/START";

    private String HEARTRATE = "/HEARTRATE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        // Enables Always-on
        ambientController = AmbientModeSupport.attach(this);

        Intent intent = getIntent();
        activity = intent.getStringExtra(ActivityListAdapter.ACTIVITY);

        firestore = FirebaseFirestore.getInstance();

        sensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);
        acceleroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_BEAT);

        nodeClient = Wearable.getNodeClient(this);
        messageClient = Wearable.getMessageClient(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            //initFitnessListener();
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

    /* TODO: GOOGLE FIT
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

*/
    private void startRopeSkippingSession() {
        accelero_dataPoints = new ArrayList<>();
        heart_rate_dataPoints = new ArrayList<>();
        sensorManager.registerListener(this, acceleroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        nodeClient.getConnectedNodes()
                .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                    @Override
                    public void onSuccess(List<Node> nodes) {
                        for(Node node : nodes) {
                            messageClient.sendMessage(node.getId(), START, new byte[]{})
                                    .addOnSuccessListener(new OnSuccessListener<Integer>() {
                                        @Override
                                        public void onSuccess(Integer integer) {
                                            //gelukt
                                        }
                                    });
                        }
                    }
                });
    }

    private void endRopeSkippingSession() {
        sensorManager.unregisterListener(this);
        currentSession = UUID.randomUUID().toString();

        nodeClient.getConnectedNodes()
                .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                    @Override
                    public void onSuccess(List<Node> nodes) {
                        for(Node node : nodes) {
                            messageClient.sendMessage(node.getId(), STOP, new byte[]{})
                                    .addOnSuccessListener(new OnSuccessListener<Integer>() {
                                        @Override
                                        public void onSuccess(Integer integer) {
                                            //gelukt
                                        }
                                    });
                        }
                    }
                });
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
            //initFitnessListener();
            //startSession();
        }
    }


    public void onClickStopSession(View view) {
        if(activity.equals("rope skipping")){
            endRopeSkippingSession();
        } else{
            //unregisterFitnessDataListener();
            //endSession();
        }
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            nodeClient.getConnectedNodes()
                    .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                        @Override
                        public void onSuccess(List<Node> nodes) {
                            for(Node node : nodes) {
                                messageClient.sendMessage(node.getId(),ACCELEROMETER, FloatArray2ByteArray(event.values, System.nanoTime()))
                                        .addOnSuccessListener(new OnSuccessListener<Integer>() {
                                            @Override
                                            public void onSuccess(Integer integer) {
                                                //gelukt
                                            }
                                        });
                            }
                        }
                    });
            //TODO: heartbeat constant 0???
        } else if(event.sensor.getType() == Sensor.TYPE_HEART_BEAT){
            Log.d(TAG, event.values[0]+"");
            nodeClient.getConnectedNodes()
                    .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                        @Override
                        public void onSuccess(List<Node> nodes) {
                            for(Node node : nodes) {
                                messageClient.sendMessage(node.getId(),HEARTRATE, FloatArray2ByteArray(event.values, System.nanoTime()))
                                        .addOnSuccessListener(new OnSuccessListener<Integer>() {
                                            @Override
                                            public void onSuccess(Integer integer) {
                                                //gelukt
                                            }
                                        });
                            }
                        }
                    });
        }
    }

    public byte[] FloatArray2ByteArray(float[] values, Long time){
        Log.d(TAG, time+"");
        ByteBuffer buffer = ByteBuffer.allocate(4 * values.length+4);

        buffer.putFloat(time.floatValue());

        for (float value : values){
            buffer.putFloat(value);
        }

        return buffer.array();
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
