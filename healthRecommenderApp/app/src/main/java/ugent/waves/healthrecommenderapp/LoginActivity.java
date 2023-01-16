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
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
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
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import ugent.waves.healthrecommenderapp.Asynctasks.UserAsyncTask;
import ugent.waves.healthrecommenderapp.HelpClasses.Constants;
import ugent.waves.healthrecommenderapp.HelpClasses.goalHandler;
import ugent.waves.healthrecommenderapp.Persistance.AppDatabase;
import ugent.waves.healthrecommenderapp.Persistance.SessionDao;
import ugent.waves.healthrecommenderapp.Persistance.User;
import ugent.waves.healthrecommenderapp.Persistance.UserDao;

//<uses-permission android:name="android.permission.USE_CREDENTIALS" />
//    <uses-permission android:name="android.permission.MANAGE_ACCOUNTS" />
// <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
public class LoginActivity extends Activity implements View.OnClickListener {

    private healthRecommenderApplication app;
    //permissions
    String[] appPermissions = {
    };

    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInAccount account;

    private TextInputLayout ageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        app = (healthRecommenderApplication) this.getApplicationContext();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestId()
                .requestServerAuthCode("726533384380-p8e9er5fialjs892tu5c5aub0dgqsbb0.apps.googleusercontent.com")
                .requestIdToken("726533384380-p8e9er5fialjs892tu5c5aub0dgqsbb0.apps.googleusercontent.com")
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        account = GoogleSignIn.getLastSignedInAccount(this);
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        ageView = (TextInputLayout) findViewById(R.id.personAge);
        SignInButton signInButton = findViewById(R.id.sign_in_button);

        TextView textView = (TextView) signInButton.getChildAt(0);
        textView.setText(R.string.SignIn);

        if(account != null){
            accessApp();
        }
    }

    private void accessApp(){
        app.setAccount(account);
        app.setmGoogleSignInClient(mGoogleSignInClient);

        //When available set age and set logged in user as current
        int age =0;
        if(ageView.getEditText().getText() != null && isNumeric(ageView.getEditText().getText().toString())){
            age = Integer.parseInt(ageView.getEditText().getText().toString());
        }

        User u = null;
        try {
            u = new UserAsyncTask(app.getAppDb(), account.getId(), Constants.GET).execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(u == null){
            User newUser = new User();
            newUser.setGoal((int)Constants.GOAL);
            newUser.setWeek(0);
            newUser.setUid(account.getId());
            newUser.setAge(age != 0 ? age : 25);
            newUser.setCurrent(true);
            AsyncTask.execute(() -> app.getAppDb().userDao().insertUser(newUser));
        } else{
            if(age != 0){
                u.setAge(age);
            }
            u.setCurrent(true);
            User finalU = u;
            AsyncTask.execute(() -> app.getAppDb().userDao().updateUser(finalU));
        }
        Intent intent = new Intent(this, NavigationActivity.class);
        startActivity(intent);
    }

     /*
    GOOGLE SIGNIN
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == Constants.RC_SIGN_IN) {
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
                    Constants.PERMISSION_REQUEST
            );
        }else{
            accessApp();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Constants.PERMISSION_REQUEST) {
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
            } else{
                accessApp();
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

    @Override
    public void onClick(View v) {
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent, Constants.RC_SIGN_IN);
    }

    public boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

}
