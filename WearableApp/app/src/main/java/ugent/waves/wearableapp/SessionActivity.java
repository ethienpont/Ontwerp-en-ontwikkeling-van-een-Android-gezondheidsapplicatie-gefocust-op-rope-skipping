package ugent.waves.wearableapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.wear.ambient.AmbientModeSupport;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeClient;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SessionActivity extends AppCompatActivity implements SensorEventListener, AmbientModeSupport.AmbientCallbackProvider {

    private String TAG="SessionActivity";
    private String ACCELEROMETER = "/ACCELEROMETER";

    private String NOTIFICATION_MESSAGE = "NOTIFICATION_MESSAGE";

    private SensorManager sensorManager;
    private Sensor acceleroSensor;
    private AmbientModeSupport.AmbientController ambientController;
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
        ambientController.setAmbientOffloadEnabled(true);


        sensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);
        acceleroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        nodeClient = Wearable.getNodeClient(this);
        messageClient = Wearable.getMessageClient(this);

        Fragment f = SessionFragment.newInstance();
        switchContent(R.id.container, f);

    }

    //Start session from smartphone
    @Override
    protected void onNewIntent(Intent intent) {
        // continue with your work here
        super.onNewIntent(intent);
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
                        try{
                            startSession();
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
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

    //Send start message
    private void startRopeSkippingSession() {
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

    //Send end message
    private void endRopeSkippingSession() {
        sensorManager.unregisterListener(this);

        messageClient.sendMessage(nodeChosen.getId(), STOP, new byte[]{})
                                    .addOnSuccessListener(new OnSuccessListener<Integer>() {
                                        @Override
                                        public void onSuccess(Integer integer) {
                                            //gelukt
                                        }
                                    });
    }

    //Replace fragment
    public void switchContent(int id, Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(id, fragment, fragment.toString());
        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();
    }


    //Initialize nodefragment with available nodes
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


    //Set node name
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

    //Show bluetooth dialog
    public void showDialog(){

        BluetoothDialogFragment b = BluetoothDialogFragment.newInstance();
        FragmentManager fm = getSupportFragmentManager();
        b.show(fm, "");
    }

    //Initialize heartratedisplayfragment and start session
    public void startSession() {
        Fragment f = HeartRateDisplayFragment.newInstance();
        displayFragment = (HeartRateDisplayFragment) f;
        switchContent(R.id.container, f);
        startRopeSkippingSession();
    }


    //Stop session
    public void stopSession() {
        endRopeSkippingSession();
        Fragment f = SessionFragment.newInstance();
        switchContent(R.id.container, f);
    }

    //Catch sensorEvents and send corresponding message
    @Override
    public void onSensorChanged(final SensorEvent event) {
        //Calculate HRMAX and send alarm if above
        int HRMAX = app.getAge() == 0 ? 195 : 220 - app.getAge();
        if(event.values[0] > HRMAX){
            sendAlarm();
        }
        //Send accelerometer data in batches
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            if(samples_accelerometer == null){
                samples_accelerometer = new ArrayList<>();
            } else if(samples_accelerometer.size() >= (104*4)){

                messageClient.sendMessage(nodeChosen.getId(),ACCELEROMETER, toByteArray(samples_accelerometer))
                        .addOnSuccessListener(new OnSuccessListener<Integer>() {
                            @Override
                            public void onSuccess(Integer integer) {
                                Log.e("e","e");
                            }
                        });
                samples_accelerometer = null;
            }else{
                long time = System.nanoTime();
                byte[] byteArray = FloatArray2ByteArray(event.values, time);
                for(byte b: byteArray){
                    samples_accelerometer.add(b);
                }
            }
        }
        //Send heartrate data in batches
        else if(event.sensor.getType() == Sensor.TYPE_HEART_RATE){
            if (displayFragment.isAdded() && displayFragment.isVisible() && displayFragment.getUserVisibleHint()) {
                displayFragment.showHeartRate(event.values[0]);
            }

            if(samples_heartbeat == null){
                samples_heartbeat = new ArrayList<>();
            } else if(samples_heartbeat.size() >= 52){
                byte[] a = toByteArray(samples_heartbeat);
                int s = event.values.length;
                messageClient.sendMessage(nodeChosen.getId(),HEARTRATE, a)
                        .addOnSuccessListener(new OnSuccessListener<Integer>() {
                            @Override
                            public void onSuccess(Integer integer) {
                            }
                        });

                samples_heartbeat = null;
            }else{
                long time = System.nanoTime();
                for(byte b: FloatArray2ByteArray(event.values, time)){
                    samples_heartbeat.add(b);
                }
            }
        }
    }

    //Vibrate during alarm
    private void sendAlarm() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] vibrationPattern = {0, 500, 50, 300};
        vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1));
    }

    //List of bytes to bytearray for sending in message
    private byte[] toByteArray(List<Byte> samples_accelerometer) {
        byte[] byteArray = new byte[samples_accelerometer.size()];
        int i = 0;
        for(Byte b: samples_accelerometer){
            byteArray[i] = b.byteValue();
            i++;
        }
        return byteArray;
    }

    //Transform float array to byte array
    public byte[] FloatArray2ByteArray(float[] values, Long time){
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

    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }


    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
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
