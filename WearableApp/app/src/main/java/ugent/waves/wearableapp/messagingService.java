package ugent.waves.wearableapp;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

//Messagehandler to communicate with smartwatch
public class messagingService extends WearableListenerService {
    private String PATH = "/SESSION_START";
    private String AGE = "/AGE";
    private wearableAppApplication app;

    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        //Start session from smartphone
        if(messageEvent.getPath().equalsIgnoreCase(PATH) ){
            Intent newIntent = new Intent(getApplicationContext(), SessionActivity.class);
            newIntent.putExtra("NOTIFICATION_MESSAGE", messageEvent.getSourceNodeId());
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(newIntent);
        }
        //Get age from smartwatch
        else if(messageEvent.getPath().equalsIgnoreCase(AGE) ){
            try{
                app = (wearableAppApplication) getApplicationContext();
                int data = messageEvent.getData()[0];
                app.setAge(data);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
