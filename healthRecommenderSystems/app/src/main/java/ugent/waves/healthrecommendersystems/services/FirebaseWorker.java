package ugent.waves.healthrecommendersystems.services;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import ugent.waves.healthrecommendersystems.persistance.AppDatabase;
import ugent.waves.healthrecommendersystems.persistance.activityAPIData;

public class FirebaseWorker extends Worker {

    private AppDatabase db;
    private FirebaseFirestore firestore;

    public FirebaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        db = AppDatabase.getInstance(getApplicationContext());
        firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("FireBase", "FIREBASEWORKER: lukt");
        /*
        //get data from sqllite
        db.activityAPIDataDao();

        //post to firebase
        firestore.collection("activityAPIData")
                .add();*/
        return null;
    }
}
