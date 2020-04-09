package ugent.waves.healthrecommenderapp.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.Calendar;

import ugent.waves.healthrecommenderapp.Persistance.Snooze;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

public class broadcastReceiver extends BroadcastReceiver {
    private static final String ACTION_SNOOZE = "SNOOZE";
    private healthRecommenderApplication app;

    //TODO: gebruiker laten snoozen: testen
    @Override
    public void onReceive(Context context, Intent intent) {
        app = (healthRecommenderApplication) context.getApplicationContext();
        Calendar c = Calendar.getInstance();

        if(intent.getAction() == ACTION_SNOOZE){
            //TODO: weekdag + uur in db waarop gesnoozed
            //TODO: threshold van een uur op een dag: goede waarde?? want in 1 hour kan meerdere keren snoozen

            Snooze s = new Snooze();
            s.setHour(c.get(Calendar.HOUR));
            s.setWeekday(c.get(Calendar.DAY_OF_WEEK));
            s.setWeek(app.getWeeknr());

            try{
                AsyncTask.execute(() -> {app.getAppDb().snoozeDao().insertSnooze(s);});
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        else if (ActivityTransitionResult.hasResult(intent)) {
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
                    //cumulatief stil zitten
                    app.setTimeStill(app.getTimeStill() + (System.currentTimeMillis() - app.getStartStill()));

                    //als tijd stil gezeten te lang is
                    //TODO: correcte waarde voor te lang stilzitten per dag??
                    //TODO: elke dag time still resetten
                    if(app.getTimeStill()  > 300){
                        //zet time still terug op 0
                        app.setTimeStill(0);
                        //send notification
                        if(checkAvailibility(c.get(Calendar.DAY_OF_WEEK), c.get(Calendar.HOUR))){
                            context.sendBroadcast(new Intent("SEND_NOTIFICATION"));
                        }
                    }
                }
            }
        }
    }

    private boolean checkAvailibility(int weekday, int hour){
        Snooze[] snoozed = app.getAppDb().snoozeDao().getSnoozeForDayAndHour(weekday, hour);
        return snoozed.length < 10;
    }
    
}
