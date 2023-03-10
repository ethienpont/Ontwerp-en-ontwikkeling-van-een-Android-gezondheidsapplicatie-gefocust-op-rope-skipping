package ugent.waves.healthrecommenderapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.people.v1.model.Birthday;
import com.google.api.services.people.v1.model.Person;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

//<uses-permission android:name="android.permission.USE_CREDENTIALS" />
//    <uses-permission android:name="android.permission.MANAGE_ACCOUNTS" />
// <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
//TODO: wat als user permissies zoals age niet toelaat, alert tonen --> request age permission via google??? people  API
//TODO: people api: secret van webclient, google sign in kan enkel met webclient??? -> sha updaten?, application name??
//TODO: sign in doesnt work, door veranderne van pc mss
public class LoginActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "LoginFragment";
    private healthRecommenderApplication app;
    //permissions
    String[] appPermissions = {
            //https://developer.android.com/about/versions/10/privacy/changes#physical-activity-recognition
            //api level above 29 different permission for activity recognition
            //TODO: werkt ook met api level hoger dn 28? want permission niet in manifest?
            Build.VERSION.SDK_INT < 29 ? "com.google.android.gms.permission.ACTIVITY_RECOGNITION" : Manifest.permission.ACTIVITY_RECOGNITION,
            //"com.google.android.gms.permission.ACTIVITY_RECOGNITION",
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.GET_ACCOUNTS
    };
    private boolean permissionsGranted;

    //constants
    private static final int RC_SIGN_IN = 1;
    private static final int PERMISSION_REQUEST = 2;
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 3;


    //firebase
    private FirebaseFirestore db;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInAccount account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        app = (healthRecommenderApplication) this.getApplicationContext();

        Scope age = new Scope(PeopleServiceScopes.USER_BIRTHDAY_READ);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestId()
                .requestServerAuthCode("726533384380-p8e9er5fialjs892tu5c5aub0dgqsbb0.apps.googleusercontent.com")
                .requestIdToken("726533384380-p8e9er5fialjs892tu5c5aub0dgqsbb0.apps.googleusercontent.com")
                //.requestIdToken("726533384380-7ratqe860v6dkplnjengumll4ltc3ngg.apps.googleusercontent.com")
                .requestScopes(age)
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        account = GoogleSignIn.getLastSignedInAccount(this);
        findViewById(R.id.sign_in_button).setOnClickListener(this);

        //GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        //int d = api.isGooglePlayServicesAvailable(this);

        if(account != null){
            accessApp();
        }
    }

    private void accessApp(){
        app.setAccount(account);
        app.setmGoogleSignInClient(mGoogleSignInClient);
        /*
        try {
            //TODO: getting profile info doesnt finish
            Person pr = new GetProfileDetails(account, this, "la").execute().get();
            List<Birthday> b = pr.getBirthdays();
            Log.d("d", "d");
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        Intent intent = new Intent(this, NavigationActivity.class);
        startActivity(intent);
    }

     /*
    GOOGLE + FIREBASE SIGNIN
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            account = completedTask.getResult(ApiException.class);
            checkAndRequestPermissions();
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("tag", "signInResult:failed code=" + e.getStatusCode());
        }
    }

    // Check permissions
    private void checkAndRequestPermissions() {
        // Check which permissions are granted
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : appPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }

        // Ask for non-granted permissions
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    PERMISSION_REQUEST
            );
        }/*else if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    account,
                    fitnessOptions);
        } */else{
            accessApp();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            HashMap<String, Integer> permissionResults = new HashMap<>();

            // Gather permission grant results
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults.put(permissions[i], grantResults[i]);
                }
            }

            // At least one or all permissions are denied --> Ask again with rationale
            if(permissionResults.keySet().size() > 0) {
                for (Map.Entry<String, Integer> entry : permissionResults.entrySet()) {
                    String permName = entry.getKey();
                    int permResult = entry.getValue();

                    // Permission is denied (this is the first time, when "never ask again" is not checked)
                    // so ask again explaining the usage of permission
                    // shouldShowRequestPermissionRationale will return true
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permName)) {
                        // Show dialog of explanation
                        showDialog("", "This app needs access to sensors and location to work without problems.",
                                "Yes, grant permissions",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        checkAndRequestPermissions();
                                    }
                                },
                                "No, exit app", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        finish();
                                    }
                                }, false);
                    } else {
                        // Ask user to go to settings and manually allow permissions
                        showDialog("",
                                "You have denied some permissions. Allow all permissions in [Settings] > [Permissions]",
                                "Go to settings",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        //TODO: als nr settings moet gaan, nie omleiden nr accesApp?
                                        dialogInterface.dismiss();
                                        //Go to settings
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", getPackageName(), null));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    }
                                },
                                "No, exit app", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        finish();
                                    }
                                }, false);
                        break;
                    }
                }
            }
        }
    }

    public AlertDialog showDialog(String title, String msg, String positiveLabel,
                                  DialogInterface.OnClickListener positiveOnClick,
                                  String negativeLabel, DialogInterface.OnClickListener negativeOnClick,
                                  boolean isCancelAble) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(isCancelAble);
        builder.setMessage(msg);
        builder.setPositiveButton(positiveLabel, positiveOnClick);
        builder.setNegativeButton(negativeLabel, negativeOnClick);

        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }


    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        /*
        Log.e(TAG, "firebaseAuthWithGoogle:" + acct.getIdToken());
        mAuth = FirebaseAuth.getInstance();

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            user = mAuth.getCurrentUser();
                            accessApp();
                            Log.e(TAG, user.getDisplayName());
                            Log.d(TAG, "signInWithCredential:success");
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                        }
                    }
                });*/
    }

    @Override
    public void onClick(View v) {
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    static class GetProfileDetails extends AsyncTask<Void, Void, Person> {

        private final GoogleSignInAccount account;
        private PeopleService ps;
        private int authError = -1;
        private WeakReference<Activity> weakAct;
        private String TAG;

        GetProfileDetails(GoogleSignInAccount account, Activity activity, String TAG) {
            this.TAG = TAG;
            this.weakAct = new WeakReference<>(activity);
            this.account = account;
            Collection<String> scopes = new ArrayList<>();
            scopes.add(Scopes.PROFILE);


            //GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(this.weakAct.get(), scopes);
            //credential.setSelectedAccountName(account.getEmail());
            //credential.setSelectedAccount(account.getAccount());

            HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
            JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

            // Redirect URL for web based applications.
            // Can be empty too.
            String redirectUrl = "urn:ietf:wg:oauth:2.0:oob";


            // Exchange auth code for access token
            GoogleTokenResponse tokenResponse = null;
            String token = account.getServerAuthCode();
            try {
                tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                        HTTP_TRANSPORT,
                        JSON_FACTORY,
                        "726533384380-p8e9er5fialjs892tu5c5aub0dgqsbb0.apps.googleusercontent.com",
                        "AqOEU2CUJsj7L6gND-8yi1jR",
                        account.getServerAuthCode(),
                        null).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Then, create a GoogleCredential object using the tokens from GoogleTokenResponse
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setClientSecrets("726533384380-p8e9er5fialjs892tu5c5aub0dgqsbb0.apps.googleusercontent.com", "AqOEU2CUJsj7L6gND-8yi1jR")
                    .setTransport(HTTP_TRANSPORT)
                    .setJsonFactory(JSON_FACTORY)
                    .build();

            credential.setFromTokenResponse(tokenResponse);
/*
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(HTTP_TRANSPORT)
                    .setJsonFactory(JSON_FACTORY)
                    .setClientSecrets("726533384380-p8e9er5fialjs892tu5c5aub0dgqsbb0.apps.googleusercontent.com", "AIzaSyBLGLcrbk13p3N5S_4OULdMKOHOzv-WODc")
                    .build();
*/
            ps = new PeopleService.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName("healthRecommenderApp")
                    .build();
        }

        @Override
        protected Person doInBackground(Void... params) {
            Person meProfile = null;
            try {
                String d = account.getId();
                String l = account.getIdToken();
                meProfile = ps
                        .people()
                        .get("people/me")
                        //.get("people/" + account.getId())
                        .set("personFields","birthdays")
                        //.setPersonFields("birthdays")
                        //.setRequestMaskIncludeField("person.birthdays")
                        .setAccessToken(account.getIdToken())
                        .setOauthToken(account.getIdToken())
                        .setKey("726533384380-p8e9er5fialjs892tu5c5aub0dgqsbb0.apps.googleusercontent.com")
                        .execute();
                Log.d("", "");
            } catch (UserRecoverableAuthIOException e) {
                e.printStackTrace();
                authError = 0;
            } catch (GoogleJsonResponseException e) {
                e.printStackTrace();
                authError = 1;
            } catch (IOException e) {
                e.printStackTrace();
                authError = 2;
            } catch (Exception e){
                e.printStackTrace();
            }
            return meProfile;
        }

    }
}
