package ugent.waves.wearableapp;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import androidx.annotation.NonNull;

//TODO: communicatie werkt niet
public class messagingService extends WearableListenerService {
    private String PATH = "/SESSION_START";

    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if(messageEvent.getPath().equalsIgnoreCase(PATH) ){
            Log.d("TAG", "hh");
        }
    }
}
