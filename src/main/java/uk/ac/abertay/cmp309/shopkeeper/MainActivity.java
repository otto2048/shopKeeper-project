package uk.ac.abertay.cmp309.shopkeeper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import uk.ac.abertay.cmp309.shopkeeper.databinding.ActivityMainBinding;
import uk.ac.abertay.cmp309.shopkeeper.ui.home.HomeFragment;
import uk.ac.abertay.cmp309.shopkeeper.ui.myshops.MyShopsFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppBarConfiguration appBarConfiguration;

    private final static int LOCATION_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_my_shops, R.id.navigation_user_info)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        NavigationUI.setupWithNavController(binding.navView, navController);

        //check if user has been sent here after creating/updating a list
        Bundle extras = getIntent().getExtras();

        if (extras != null)
        {
            sendUserToList(extras);
        }

        //set up location permission
        int checkResult = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        if (checkResult != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do stuff
                    Toast.makeText(this, "Location permission granted",
                            Toast.LENGTH_SHORT).show();
                } else {
                    //permission denied, don't do stuff
                    Toast.makeText(this, "Permission denied",
                            Toast.LENGTH_SHORT).show();
                }
                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //check if user has been sent here after creating/updating a list
        Bundle extras = getIntent().getExtras();

        if (extras != null)
        {
            sendUserToList(extras);
        }

        Log.i("shopKeeper", "In main activity on resume");
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void sendUserToList(Bundle extras)
    {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        NavDestination currentDestination = navController.getCurrentDestination();

        //check where we're navigating from
        if (currentDestination.getId() == R.id.navigation_home)
        {
            navController.navigate(R.id.action_navigation_home_to_viewList, extras);
        }
        else if (currentDestination.getId() == R.id.myLists)
        {
            navController.navigate(R.id.action_myLists_to_viewList, extras);
        }
    }

    //source: https://developer.android.com/reference/android/app/Activity#onNewIntent(android.content.Intent)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }
}