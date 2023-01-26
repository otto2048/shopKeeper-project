package uk.ac.abertay.cmp309.shopkeeper.ui.userinfo;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserInfoViewModel extends ViewModel {
    FirebaseUser firebaseUser;
    FirebaseAuth firebaseAuth;

    private MutableLiveData<String> displayName;
    private MutableLiveData<String> email;
    private MutableLiveData<Uri> profilePicture;

    private MutableLiveData<Boolean> loginStatus;

    public UserInfoViewModel()
    {
        displayName = new MutableLiveData<>();
        email = new MutableLiveData<>();
        profilePicture = new MutableLiveData<>();
        loginStatus = new MutableLiveData<>();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null)
        {
            loginStatus.setValue(true);
            displayName.setValue(firebaseUser.getDisplayName());
            email.setValue(firebaseUser.getEmail());
            profilePicture.setValue(firebaseUser.getPhotoUrl());
        }
    }

    public MutableLiveData<String> getDisplayName() {
        return displayName;
    }

    public MutableLiveData<String> getEmail() {
        return email;
    }

    public MutableLiveData<Uri> getProfilePicture() {
        return profilePicture;
    }

    public MutableLiveData<Boolean> getLoginStatus() {
        return loginStatus;
    }

    public void logoutUser()
    {
        //logout user
        Log.i("shopKeeper", "Signing out");
        firebaseAuth.signOut();

        loginStatus.setValue(false);
    }
}
