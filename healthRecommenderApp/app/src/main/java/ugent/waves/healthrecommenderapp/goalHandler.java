package ugent.waves.healthrecommenderapp;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.result.SessionReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import androidx.annotation.NonNull;

public class goalHandler {

    private healthRecommenderApplication app;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private GoogleSignInAccount account;
    private Context context;
    private Random rand;

    public goalHandler(FirebaseFirestore db, FirebaseUser user, GoogleSignInAccount account, Context context, healthRecommenderApplication app){
        this.account = account;
        this.db = db;
        this.user = user;
        this.context = context;
        this.app =  app;
    }

    public void postGoalReached(){
        //TODO: deze logica moet 1 keer per week uitgevoerd worden
        //TODO: als er te weinig entries zijn, goal in app berekenen
        //TODO: als bereikte aantal hoger is dan goal
        Calendar cal = Calendar.getInstance();
        long end = app.getNowMilliSec(cal);
        long start = app.getWeeksAgoMilliSec(cal, 3);
        ScoreCalculation r = new ScoreCalculation(account, context, 21);
        r.getHistory(start, end, DataType.TYPE_HEART_RATE_BPM)
                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                    @Override
                    public void onSuccess(SessionReadResponse dataReadResponse) {
                        double score = r.processMETscore(dataReadResponse);
                        int g = rand.nextInt(700)+100;
                        int reach = rand.nextInt(g);
                        Map<String, Object> goal = new HashMap<>();
                        goal.put("week_number", 0);
                        goal.put("mets_goal", g);
                        goal.put("mets_reached", reach);

                        //TODO: firestore populating with testdata
                        //TODO: document moet naast collectie ook een veld hebben om zichtbaar te zijn
                        for(int i= 0; i<52; i++){
                            db.collection("users")
                                    .document(user.getUid())
                                    .collection("goals")
                                    .document(i+"") //TODO: wrong week of year //cal.get(Calendar.WEEK_OF_YEAR)+""
                                    .set(goal)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("kk", "DocumentSnapshot successfully written!");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w("l", "Error writing document", e);
                                        }
                                    });
                            g = rand.nextInt(700)+100;
                            reach = rand.nextInt(g);
                            goal.put("week_number", i+1);
                            goal.put("mets_goal", g);
                            goal.put("mets_reached", reach);
                        }
                    }
                });
    }

    public void setNewGoal(int week){
        DocumentReference doc = db.collection("users")
                .document(user.getUid())
                .collection("goals")
                .document(week+"");

        doc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("l", document.get("mets_goal").toString());
                        app.setWeeknr(week);
                        app.setGoal(((Long) document.get("mets_goal")).intValue());
                    } else {
                        Log.d("l", "No such document");
                    }
                } else {
                    Log.d("l", "get failed with ", task.getException());
                }
            }
        });
    }
}
