package ugent.waves.healthrecommenderapp;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.SessionReadResponse;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Recommendations {

    private GoogleSignInAccount account;
    private Context context;
    private int age;

    private int veryLightZone = 5;
    private int lightZone = 6;
    private int moderateZone = 7;
    private int hardZone = 8;
    private int maximumZone = 9;

    public Recommendations(GoogleSignInAccount account, Context c, int age){
        this.account = account;
        this.context = c;
        this.age = age;
    }

    public Task<SessionReadResponse> getHistory(long startTime, long endTime, DataType type){
        SessionReadRequest request =
                new SessionReadRequest.Builder()
                        .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                        .read(type)
                        .readSessionsFromAllApps()
                        .build();

        return Fitness.getSessionsClient(context, account)
                .readSession(request);
    }

    //TODO: cant access heartpoints because no goal set? + cant programmatically set goal
    public double processHeartPointResponse(SessionReadResponse res){
        double heartPointSum = 0;
        for(Session s : res.getSessions()){
            for(DataSet ds: res.getDataSet(s)){
                heartPointSum += ds.getDataPoints().stream()
                        .mapToDouble(point -> (double) point.getValue(Field.FIELD_INTENSITY).asFloat())
                        .sum();
            }
        }

        return heartPointSum;
    }

    //TODO: grenzen?
    public double processHeartRateScoreResponse(SessionReadResponse res){
        int score = 0;
        int MAXHR = 220 - age;
        for(Session s : res.getSessions()){
            for(DataSet ds: res.getDataSet(s)){
                for(DataPoint dp : ds.getDataPoints()){
                    double v = dp.getValue(Field.FIELD_BPM).asFloat();
                    if( (v >= 0.5*MAXHR) && (v < 0.6*MAXHR) ){
                        score += v*veryLightZone;
                    } else if( (v >= 0.6*MAXHR) && (v < 0.7*MAXHR) ){
                        score += v*lightZone;
                    } else if( (v >= 0.7*MAXHR) && (v < 0.8*MAXHR) ){
                        score += v*moderateZone;
                    } else if( (v >= 0.8*MAXHR) && (v < 0.9*MAXHR) ){
                        score += v*hardZone;
                    } else if( (v >= 0.9*MAXHR) && (v < MAXHR) ){
                        score += v*maximumZone;
                    }
                }
            }
        }

        return score;
    }

}
