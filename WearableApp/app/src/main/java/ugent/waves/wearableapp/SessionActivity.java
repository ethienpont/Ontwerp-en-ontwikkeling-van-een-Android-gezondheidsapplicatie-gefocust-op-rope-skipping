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

//TODO: stop session
//TODO: connecteer met juiste node
public class SessionActivity extends FragmentActivity implements SensorEventListener, AmbientMode.AmbientCallbackProvider {

    private String TAG="SessionActivity";
    private String ACCELEROMETER = "/ACCELEROMETER";

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
    private HeartRateDisplayFragment displayFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        // Enables Always-on
        ambientController = AmbientModeSupport.attach(this);

        firestore = FirebaseFirestore.getInstance();

        sensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);
        acceleroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_BEAT);

        nodeClient = Wearable.getNodeClient(this);
        messageClient = Wearable.getMessageClient(this);

        Fragment f = SessionFragment.newInstance(null);
        switchContent(R.id.container, f);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

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


    public void startSession() {
        startRopeSkippingSession();
        Fragment f = HeartRateDisplayFragment.newInstance(null);
        displayFragment = (HeartRateDisplayFragment) f;
        switchContent(R.id.container, f);
    }


    public void onClickStopSession(View view) {
        endRopeSkippingSession();
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
            displayFragment.showHeartRate(event.values[0]);
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
