package ugent.waves.wearable;

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
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.provider.Contacts.SettingsColumns.KEY;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private static final int RC_SIGN_IN = 1;
    private static final int PERMISSION_REQUEST = 2;
    private static final String TAG = "WEARABLE";
    private static final String ACCELERATION = "/ACCELERATION";
    private static final String SL = "VALUES";
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInAccount account;
    private OnDataPointListener  mListener;
    String[] appPermissions = {Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACCESS_FINE_LOCATION};
    String workoutType = null;
    private Session session;
    private DataSource heartRateDataSource;
    private SensorManager sensorManager;
    private Sensor sensor;
    private DataClient dataClient;
    private NodeClient nodeClient;
    private MessageClient messageClient;
    private String ACCELERATION_STOP = "/ACCELERATION_STOP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled();

        // Set options for Google Sign-In and get client instance
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestScopes(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .requestScopes(new Scope(Scopes.FITNESS_BODY_READ))
                .requestScopes(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, options);

        // Check for necessary permissions (and request in case no permissions were granted)
        if (checkAndRequestPermissions()) {
            // Check if Google account is signed in
            account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                // Start heart rate listener
                initFitnessListener(account);
                // Initiate recording and create session
                //initSession(account, "RUNNING");
            } else {
                signIn();
            }
        }

        //accelerometer data
        sensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        dataClient = Wearable.getDataClient(this);
        nodeClient = Wearable.getNodeClient(this);
        messageClient = Wearable.getMessageClient(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            initFitnessListener(account);
            //initSession(account, "RUNNING");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterFitnessDataListener();
        //endSession(GoogleSignIn.getLastSignedInAccount(this));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterFitnessDataListener();
        //endSession(GoogleSignIn.getLastSignedInAccount(this));
    }

    // Initiate real time session using Recording API and Sessions API
    private void initSession(final GoogleSignInAccount account, String workoutType) {
        // Subscribe to TYPE_HEART_RATE_BPM and AGGREGATE_DISTANCE_DELTA
        Fitness.getRecordingClient(this, account)
                .subscribe(DataType.TYPE_HEART_RATE_BPM)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("lal", "Successfully subscribed (heart rate)!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("tal", "There was a problem subscribing (heart rate).");
                    }
                });

        /*
        Fitness.getRecordingClient(this, account)
                .subscribe(DataType.AGGREGATE_DISTANCE_DELTA)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Successfully subscribed (distance)!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "There was a problem subscribing (distance)." + e);
                    }
                });*/


        // Retrieve current time in milliseconds
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long startTime = cal.getTimeInMillis();
        // Set type of workout
        String activityType = null;
        if (workoutType.equalsIgnoreCase("running")) {
            activityType = FitnessActivities.RUNNING;
        } else if (workoutType.equalsIgnoreCase("biking")) {
            activityType = FitnessActivities.BIKING;
        } else if (workoutType.equalsIgnoreCase("swimming")) {
            activityType = FitnessActivities.SWIMMING;
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

    // Stop real time session by using stop button or when workout duration is reached
    // Unsubscribe from registered DataTypes
    private void endSession(final GoogleSignInAccount account) {
        Task<List<Session>> response = Fitness.getSessionsClient(this, account)
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
/*
        Fitness.getRecordingClient(this, account)
                .unsubscribe(DataType.AGGREGATE_DISTANCE_DELTA)
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
                });*/
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
                                        Log.d("hdd", dataSource.toString());
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
                            Log.d("wearble", val.toString());
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
                                    Log.i("acti", "Listener registered!");
                                } else {
                                    Log.e("acti", "Listener not registered.", task.getException());
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
                                    Log.i("act", "Listener was removed!");
                                } else {
                                    Log.i("act", "Listener was not removed." + task.getException());
                                }
                            }
                        });
        // [END unregister_data_listener]
    }

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
            GoogleSignInResult signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (signInResult.isSuccess()) {
                GoogleSignInAccount acct = signInResult.getSignInAccount();
            }
            /*
            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);*/
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            initFitnessListener(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("tag", "signInResult:failed code=" + e.getStatusCode());
        }
    }

    // Check wear permissions
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
            return false;
        }

        // Wear has all permissions
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i("act", "onRequestPermissionResult");
        if (requestCode == PERMISSION_REQUEST) {
            HashMap<String, Integer> permissionResults = new HashMap<>();
            int deniedCount = 0;

            // Gather permission grant results
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults.put(permissions[i], grantResults[i]);
                    deniedCount++;
                }
            }

            // Check if all permissions are granted
            if (deniedCount == 0) {
                // Initiate
                initFitnessListener(GoogleSignIn.getLastSignedInAccount(this));
            }

            // At least one or all permissions are denied --> Ask again with rationale
            else {
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


    public void onClickGetHRToday(View view) {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long endTime = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startTime = cal.getTimeInMillis();


        DataReadRequest readRequest =
                new DataReadRequest.Builder()
                        .read(DataType.TYPE_HEART_RATE_BPM)
                        .bucketByTime(1, TimeUnit.DAYS)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();


        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        List<Bucket> buckets = dataReadResponse.getBuckets();
                        for(Bucket b: buckets){
                            for(DataPoint p : b.getDataSet(DataType.TYPE_HEART_RATE_BPM).getDataPoints()){
                                for (Field field : p.getDataType().getFields()) {
                                   // Log.i(TAG, "\tField: " + field.getName() + " Value: " + p.getValue(field));
                                }
                            }
                        }
                    }
                });
    }

    public void onClickStartAccelerometer(View view) {
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        /*
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(ACCELERATION);
        putDataMapReq.getDataMap().putFloatArray(SL,  event.values);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        dataClient.putDataItem(putDataReq)
                .addOnSuccessListener(new OnSuccessListener<DataItem>() {
                    @Override
                    public void onSuccess(DataItem dataItem) {
                        Log.d(TAG, dataItem.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, e.getMessage());
                    }
                });
        //Wearable.DataApi.putDataItem(apiClient,putDataReq);*/
        nodeClient.getConnectedNodes()
                .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                    @Override
                    public void onSuccess(List<Node> nodes) {
                        for(Node node : nodes) {
                            messageClient.sendMessage(node.getId(), ACCELERATION, FloatArray2ByteArray(event.values, System.nanoTime()))
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

    public void onClickStopAccelerometer(View view) {
        sensorManager.unregisterListener(this);
        nodeClient.getConnectedNodes()
                .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                    @Override
                    public void onSuccess(List<Node> nodes) {
                        for(Node node : nodes) {
                            messageClient.sendMessage(node.getId(), ACCELERATION_STOP, new byte[]{})
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
