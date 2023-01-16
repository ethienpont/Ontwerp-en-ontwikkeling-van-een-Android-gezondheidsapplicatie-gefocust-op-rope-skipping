package ugent.waves.healthrecommenderapp;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.wearable.Node;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.room.Room;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.User;

public class healthRecommenderApplication extends Application {

    private User user;

    private AppDatabase appDb;

    private Node node;

    private GoogleSignInAccount account;

    private GoogleSignInClient mGoogleSignInClient;

    public AppDatabase getAppDb(){
        if(appDb == null){
            appDb = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "session-database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return appDb;
    }

    public GoogleSignInAccount getAccount() {
        return account;
    }

    public void setAccount(GoogleSignInAccount account) {
        this.account = account;
    }

    public GoogleSignInClient getmGoogleSignInClient() {
        return mGoogleSignInClient;
    }

    public void setmGoogleSignInClient(GoogleSignInClient mGoogleSignInClient) {
        this.mGoogleSignInClient = mGoogleSignInClient;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User u) {
        this.user = u;
    }
}
