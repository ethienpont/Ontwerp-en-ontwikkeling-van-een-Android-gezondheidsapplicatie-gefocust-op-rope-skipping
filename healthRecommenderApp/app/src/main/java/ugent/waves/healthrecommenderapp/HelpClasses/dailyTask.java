package ugent.waves.healthrecommenderapp.HelpClasses;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import ugent.waves.healthrecommenderapp.R;
import ugent.waves.healthrecommenderapp.healthRecommenderApplication;

import static android.content.Context.NOTIFICATION_SERVICE;

public class dailyTask extends Worker {


    private final healthRecommenderApplication app;

    public dailyTask(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.app =  (healthRecommenderApplication) getApplicationContext();
    }

    @NonNull
    @Override
    public Result doWork() {
        sendNotification(app.getTimeStill()+"");
        app.setTimeStill(0);
        app.setStartStill(0);
        return Result.success();
    }

    private void sendNotification(String messageBody) {
        //**add this line**
        int requestID = (int) System.currentTimeMillis();

        //TODO: check flags
        /*
        Intent intent = new Intent(getApplicationContext(), StartSessionActivity.class);
        intent.putExtra(ACTIVITY_ID, activityId);
        intent.putExtra(RECOMMENDATION_ID, recommendationId);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID , intent,
                PendingIntent.FLAG_UPDATE_CURRENT);*/

        String channelId = "id";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        //TODO: taal
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(getApplicationContext(), channelId)
                        .setSmallIcon(R.drawable.rope_skipping)
                        .setContentTitle("time still")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri);

        NotificationManager notificationManager =
                (NotificationManager) app.getSystemService(NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
}
