package ugent.waves.wearableapp;

import android.app.Application;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;

public class wearableAppApplication extends Application {
    private GoogleSignInClient client;

    public GoogleSignInClient getClient() {
        return client;
    }

    public void setClient(GoogleSignInClient client) {
        this.client = client;
    }
}
