package uk.ac.abertay.cmp309.shopkeeper.ui.login;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import uk.ac.abertay.cmp309.shopkeeper.MainActivity;
import uk.ac.abertay.cmp309.shopkeeper.R;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "shopKeeper";

    private int SIGN_IN_CODE = 1;
    private GoogleSignInClient signInClient;

    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    private FirebaseAuth mAuth;

    //source: https://blog.devgenius.io/integrating-google-sign-in-into-android-app-70d582f4eed8

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //configure Google Sign-In to request users' ID and basic profile information
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                        .requestEmail()
                                                        .requestIdToken()
                                                        .build();

        //create GoogleSignInClient with options specified
        signInClient = GoogleSignIn.getClient(this, signInOptions);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        //check for existing signed in user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null)
        {
            //go to home
            Toast.makeText(getApplicationContext(), "Successfully logged in", Toast.LENGTH_SHORT).show();

            //send user to the homepage
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        }
    }

    public void signInGoogle(View v)
    {
        signIn();
    }

    //load sign in from Google
    private void signIn()
    {
        Intent intent = signInClient.getSignInIntent();
        startActivityForResult(intent, SIGN_IN_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null)
        {
            //check the request code
            if (requestCode == SIGN_IN_CODE)
            {
                try {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

                    if (task.getResult(ApiException.class).getIdToken() != null) {
                        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(task.getResult().getIdToken(), null);

                        //sign into firebase
                        mAuth.signInWithCredential(firebaseCredential)
                                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            //send user to the homepage
                                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                            startActivity(intent);
                                        } else {
                                            Toast.makeText(getApplicationContext(), R.string.failed_to_login_message, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                }
                catch (ApiException e)
                {
                    Toast.makeText(getApplicationContext(), R.string.failed_to_login_message, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}