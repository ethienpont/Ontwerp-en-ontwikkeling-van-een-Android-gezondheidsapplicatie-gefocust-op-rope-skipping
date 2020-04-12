package ugent.waves.wearableapp;

import android.app.Application;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseUser;

public class wearableAppApplication extends Application {
    private GoogleSignInClient client;

    public GoogleSignInClient getClient() {
        return client;
    }

    public void setClient(GoogleSignInClient client) {
        this.client = client;
    }
}
