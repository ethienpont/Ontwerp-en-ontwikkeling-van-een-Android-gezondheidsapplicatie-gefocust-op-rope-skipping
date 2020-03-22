package ugent.waves.healthrecommendersystems;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;

import java.io.OutputStreamWriter;


public class  MainActivity extends AppCompatActivity implements MessageClient.OnMessageReceivedListener {

    private static final int MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION = 1;
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 2;
    private static final int RC_SIGN_IN = 3;
    private static final String ACCELERATION = "/ACCELERATION";
    private static final String SL = "VALUES";
    private ActivityRecognitionClient mActivityRecognitionClient;
    private static final String TAG = "MainActivity";
    private ScanCallback mScanCallback;
    private BluetoothAdapter mBluetoothAdapter;
    private GoogleSignInAccount account;
    private GoogleSignInClient mGoogleSignInClient;
    private OutputStreamWriter writer;

    @RequiresApi(api = 29)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    protected void onDestroy() {

        super.onDestroy();
    }



    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if( messageEvent.getPath().equalsIgnoreCase(ACCELERATION) ){
            Log.d(TAG, messageEvent.getSourceNodeId());
        }
    }
}
