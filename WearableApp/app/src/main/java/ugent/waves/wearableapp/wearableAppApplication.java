package ugent.waves.wearableapp;

import android.app.Application;

import com.google.firebase.auth.FirebaseUser;

public class wearableAppApplication extends Application {
    private FirebaseUser user;

    public FirebaseUser getUser() {
        return user;
    }

    public void setUser(FirebaseUser user) {
        this.user = user;
    }
}
