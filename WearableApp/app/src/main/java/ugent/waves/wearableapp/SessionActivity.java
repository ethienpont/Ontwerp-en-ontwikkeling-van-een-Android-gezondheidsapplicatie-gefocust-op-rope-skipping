package ugent.waves.wearableapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.wear.ambient.AmbientMode;
import androidx.wear.ambient.AmbientModeSupport;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
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

//TODO: ambedient
public class SessionActivity extends AppCompatActivity implements SensorEventListener, AmbientMode.AmbientCallbackProvider {

    private String TAG="SessionActivity";
    private String ACCELEROMETER = "/ACCELEROMETER";

    private String NOTIFICATION = "/SEND_NOTIFICATION";
    private String NOTIFICATION_MESSAGE = "/NOTIFICATION_MESSAGE";

    private SensorManager sensorManager;
    private Sensor acceleroSensor;
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
    private wearableAppApplication app;

    private List<Byte> samples_accelerometer;
    private List<Byte> samples_heartbeat;

    private Node nodeChosen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        app = (wearableAppApplication) this.getApplicationContext();

        // Enables Always-on
        ambientController = AmbientModeSupport.attach(this);

        LocalBroadcastManager.getInstance(this).registerReceiver((broadcast_receiver),
                new IntentFilter(NOTIFICATION)
        );

        sensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);
        acceleroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        nodeClient = Wearable.getNodeClient(this);
        messageClient = Wearable.getMessageClient(this);

        Fragment f = SessionFragment.newInstance(null);
        switchContent(R.id.container, f);
    }

    //TODO: broadcast doesnt arrive
    BroadcastReceiver broadcast_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String id = intent.getStringExtra(NOTIFICATION_MESSAGE);
            nodeClient.getConnectedNodes()
                    .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                        @Override
                        public void onSuccess(List<Node> nodes) {
                            for (Node n : nodes) {
                                if(n.getId().equals(id)){
                                    nodeChosen = n;
                                }
                            }
                            startSession();
                        }
                    });
        }
    };

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
        sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, acceleroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        messageClient.sendMessage(nodeChosen.getId(), START, new byte[]{})
                                    .addOnSuccessListener(new OnSuccessListener<Integer>() {
                                        @Override
                                        public void onSuccess(Integer integer) {
                                            //gelukt
                                        }
                                    });
    }

    private void endRopeSkippingSession() {
        sensorManager.unregisterListener(this);
        currentSession = UUID.randomUUID().toString();
        messageClient.sendMessage(nodeChosen.getId(), STOP, new byte[]{})
                                    .addOnSuccessListener(new OnSuccessListener<Integer>() {
                                        @Override
                                        public void onSuccess(Integer integer) {
                                            //gelukt
                                        }
                                    });
    }

    public void switchContent(int id, Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(id, fragment, fragment.toString());
        ft.addToBackStack(null);
        ft.commit();
    }

    public void chooseNode(){
        nodeClient.getConnectedNodes()
                .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                                          @Override
                                          public void onSuccess(List<Node> nodes) {
                                              ArrayList<String> nodeNames = new ArrayList<>();
                                              for(Node n: nodes){
                                                  nodeNames.add(n.getDisplayName());
                                              }

                                              Fragment f = NodeFragment.newInstance(nodeNames);
                                              switchContent(R.id.container, f);
                                          }
                                      });
    }

    public void setNode(final String name){
        nodeClient.getConnectedNodes()
                .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                    @Override
                    public void onSuccess(List<Node> nodes) {
                        for (Node node : nodes) {
                            if(node.getDisplayName().equals(name)){
                                nodeChosen = node;
                                startSession();
                            }
                        }
                    }
                });
    }

    //start session begint hier
    public void showDialog(){
        BluetoothDialogFragment b = BluetoothDialogFragment.newInstance();
        FragmentManager fm = getSupportFragmentManager();
        b.show(fm, "");
        //transaction.commit();
    }

    public void startSession() {
        Fragment f = HeartRateDisplayFragment.newInstance(null);
        displayFragment = (HeartRateDisplayFragment) f;
        switchContent(R.id.container, f);
        startRopeSkippingSession();
    }


    public void stopSession() {
        endRopeSkippingSession();
        Fragment f = SessionFragment.newInstance(null);
        switchContent(R.id.container, f);
    }

    //TODO: test batch grootte
    @Override
    public void onSensorChanged(final SensorEvent event) {
        //TODO: leeftijd
        if(event.values[0] > 100){
            sendAlarm();
        }

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            if(samples_accelerometer == null){
                samples_accelerometer = new ArrayList<>();
            } else if(samples_accelerometer.size() == (104*4)){
                messageClient.sendMessage(nodeChosen.getId(),ACCELEROMETER, toByteArray(samples_accelerometer))
                        .addOnSuccessListener(new OnSuccessListener<Integer>() {
                            @Override
                            public void onSuccess(Integer integer) {
                                Log.e("e","e");
                            }
                        });
                samples_accelerometer = null;
            }else{
                byte[] byteArray = FloatArray2ByteArray(event.values, System.nanoTime());
                for(byte b: byteArray){
                    samples_accelerometer.add(b);
                }
            }

        } else if(event.sensor.getType() == Sensor.TYPE_HEART_RATE){
            if (displayFragment.isAdded() && displayFragment.isVisible() && displayFragment.getUserVisibleHint()) {
                displayFragment.showHeartRate(event.values[0]);
            }

            if(samples_heartbeat == null){
                samples_heartbeat = new ArrayList<>();
            } else if(samples_heartbeat.size() == (52*2)){
                messageClient.sendMessage(nodeChosen.getId(),HEARTRATE, toByteArray(samples_heartbeat))
                        .addOnSuccessListener(new OnSuccessListener<Integer>() {
                            @Override
                            public void onSuccess(Integer integer) {
                            }
                        });
                samples_heartbeat = null;
            }else{
                for(byte b: FloatArray2ByteArray(event.values, System.nanoTime())){
                    samples_heartbeat.add(b);
                }
            }
        }
    }

    //TODO: vibrate
    private void sendAlarm() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel("channel", "yourSubjectName",NotificationManager.IMPORTANCE_HIGH);

        notificationManager.createNotificationChannel(mChannel);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, mChannel.getId());

        //FIX android O bug Notification add setChannelId("shipnow-message")
        b.setSmallIcon(R.drawable.stop_icon) // vector (doesn't work with png as well)
                .setContentTitle("get")
                .setContentText("test")
                .setChannelId(mChannel.getId())
                .setDefaults(Notification.DEFAULT_ALL)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        notificationManager.notify(0, b.build());
    }

    private byte[] toByteArray(List<Byte> samples_accelerometer) {
        byte[] byteArray = new byte[samples_accelerometer.size()];
        int i = 0;
        for(Byte b: samples_accelerometer){
            byteArray[i] = b.byteValue();
            i++;
        }
        return byteArray;
    }

    public void signOut(){
        app.getClient().signOut();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public byte[] FloatArray2ByteArray(float[] values, Long time){
        Log.d(TAG, time+"");
        ByteBuffer buffer = ByteBuffer.allocate(4*values.length+4);

        buffer.putFloat(time.floatValue());

        for (float value : values){
            buffer.putFloat(value);
        }
        byte[] b = buffer.array();
        return b ;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("", "");
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
