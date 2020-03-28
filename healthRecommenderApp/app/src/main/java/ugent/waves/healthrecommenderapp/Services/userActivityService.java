package ugent.waves.healthrecommenderapp.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

public class userActivityService extends BroadcastReceiver {

    private NotificationCallback notificationCallback;

    //TODO: zo zullen niet alle recommendations een notificatie versturen, lijst met alle
    //TODO: gebruiker laten snoozen
    @Override
    public void onReceive(Context context, Intent intent) {
        healthRecommenderApplication app = (healthRecommenderApplication) context.getApplicationContext();
        Log.d("G", "g");
        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                int transitionType = event.getTransitionType();
                int type = event.getActivityType();
                //onthoud wanneer beginnen stil te zitten
                if(type == DetectedActivity.STILL && transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER){
                    app.setStartStill(System.currentTimeMillis());
                    //context.sendBroadcast(new Intent("SEND_NOTIFICATION"));
                }
                //exit stil, check of al te lang
                if(type == DetectedActivity.STILL && transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT){
                    app.setTimeStill(app.getTimeStill() + (System.currentTimeMillis() - app.getStartStill()));
                    //als tijd stil gezeten te lang is
                    if(app.getTimeStill() + (System.currentTimeMillis() - app.getStartStill()) > 300){
                        //zet time still terug op 0
                        app.setTimeStill(0);
                        //send notification
                        context.sendBroadcast(new Intent("SEND_NOTIFICATION"));
                    }
                }
            }
        }
    }

    public void setCallbacks(NotificationCallback callbacks) {
        notificationCallback = callbacks;
    }
    
}
