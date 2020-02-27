package ugent.waves.healthrecommendersystems.services;

import android.app.IntentService;
import android.content.Intent;
import androidx.annotation.Nullable;
import ugent.waves.healthrecommendersystems.persistance.AppDatabase;
import ugent.waves.healthrecommendersystems.persistance.accelerometerData;
import ugent.waves.healthrecommendersystems.persistance.activityAPIData;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

public class ActivityRecognizedService extends IntentService {

    protected static final String TAG = "Activity";
    private AppDatabase db;

    public ActivityRecognizedService(){
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = AppDatabase.getInstance(this);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)){
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivity(result.getProbableActivities());
        }
    }

    private void handleDetectedActivity(List<DetectedActivity> probable){
        for(final DetectedActivity ac : probable){
            /*
            switch(ac.getType()){
                case DetectedActivity.IN_VEHICLE:{
                    Log.d(TAG,"IN VEHICLE" + ac.getConfidence());
                    break;
                }
                case DetectedActivity.ON_BICYCLE:{
                    Log.d(TAG,"ON VEHICLE" + ac.getConfidence());
                    break;
                }
                case DetectedActivity.ON_FOOT:{
                    Log.d(TAG,"ON FOOT" + ac.getConfidence());
                    break;
                }
                case DetectedActivity.RUNNING:{
                    Log.d(TAG,"RUNNING" + ac.getConfidence());
                    break;
                }
                case DetectedActivity.STILL:{
                    Log.d(TAG,"STILL" + ac.getConfidence());
                    break;
                }
                case DetectedActivity.WALKING:{
                    Log.d(TAG,"WALKING" + ac.getConfidence());
                    break;
                }
                case DetectedActivity.UNKNOWN:{
                    Log.d(TAG,"UNKNOWN" + ac.getConfidence());
                    break;
                }
            }*/
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    db.activityAPIDataDao().insertActivityAPIData(new activityAPIData(ac.getType(), ac.getConfidence()));
                }
            });
        }
    }
}
