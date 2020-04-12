package ugent.waves.wearableapp;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class messagingService extends WearableListenerService {
    private String PATH = "/SESSION_START";

    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if(messageEvent.getPath().equalsIgnoreCase(PATH) ){
            Intent intent = new Intent("SEND_NOTIFICATION");
            intent.putExtra("NOTIFICATION_MESSAGE", messageEvent.getSourceNodeId());
            try{
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
