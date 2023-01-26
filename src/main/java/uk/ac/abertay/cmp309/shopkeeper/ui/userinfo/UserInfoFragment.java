package uk.ac.abertay.cmp309.shopkeeper.ui.userinfo;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import uk.ac.abertay.cmp309.shopkeeper.R;

import uk.ac.abertay.cmp309.shopkeeper.databinding.FragmentUserInfoBinding;
import uk.ac.abertay.cmp309.shopkeeper.ui.login.LoginActivity;

public class UserInfoFragment extends Fragment implements View.OnClickListener {

    private FragmentUserInfoBinding binding;
    private UserInfoViewModel mViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentUserInfoBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //display info in text views
        TextView displayTV = binding.displayNameTV;
        TextView emailTV = binding.emailTV;
        ImageView profilePictureIV = binding.profilePictureIV;

        mViewModel = new UserInfoViewModel();

        mViewModel.getDisplayName().observe(getViewLifecycleOwner(), displayTV::setText);
        mViewModel.getEmail().observe(getViewLifecycleOwner(), emailTV::setText);
        mViewModel.getProfilePicture().observe(getViewLifecycleOwner(), new Observer<Uri>() {
            @Override
            public void onChanged(Uri uri) {
                Glide.with(getActivity()).load(uri).into(profilePictureIV);
                DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
                int height = displayMetrics.heightPixels;
                profilePictureIV.getLayoutParams().height = height / 5;
            }
        });

        mViewModel.getLoginStatus().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (!aBoolean)
                {
                    //send user back to the login screen
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                }
            }
        });

        binding.logoutBtn.setOnClickListener(this);

        return root;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.logoutBtn:
                //configure Google Sign-In to request users' ID and basic profile information
                GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestIdToken()
                        .build();

                //create GoogleSignInClient with options specified
                GoogleSignInClient signOutClient = GoogleSignIn.getClient(getActivity(), signInOptions);

                //google sign out
                signOutClient.signOut().addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mViewModel.logoutUser();
                    }
                });

                break;
        }
    }
}