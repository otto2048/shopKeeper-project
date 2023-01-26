package uk.ac.abertay.cmp309.shopkeeper.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

import uk.ac.abertay.cmp309.shopkeeper.R;
import uk.ac.abertay.cmp309.shopkeeper.Shop;
import uk.ac.abertay.cmp309.shopkeeper.databinding.FragmentHomeBinding;
import uk.ac.abertay.cmp309.shopkeeper.ui.createlist.CreateListActivity;

public class HomeFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnPoiClickListener, View.OnClickListener, GoogleMap.OnCircleClickListener, GoogleMap.OnPolygonClickListener {

    private FragmentHomeBinding binding;
    private GoogleMap map;
    private Shop selectedShop;
    private HomeViewModel homeViewModel;
    private Polygon selectedShopSquare;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location oldLocation;

    final private float defaultZoom = 12.0f;
    final private float higherZoom = 15.0f;

    private GeoPoint returnedShopLocation;

    private LatLng defaultLocation;

    private LocationCallback locationCallback = null;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback netCallback;
    private boolean hasConnection;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map));
        mapFragment.getMapAsync(this);

        //check that shop data returned
        homeViewModel.getDrawMarkers().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean != null)
                {
                    if (!aBoolean)
                    {
                        //display some error
                        Toast.makeText(getActivity(), "Failed to load your shop data", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        homeViewModel.getShopData();

        //set up on click listeners for buttons
        FloatingActionButton createBtn = binding.createNewListAB;
        FloatingActionButton viewBtn = binding.viewListsAB;
        FloatingActionButton exitBtn = binding.exitAB;

        createBtn.setOnClickListener(this);
        viewBtn.setOnClickListener(this);
        exitBtn.setOnClickListener(this);

        returnedShopLocation = null;

        //default location
        defaultLocation = new LatLng(56.4633, -2.9739);

        //see if we have to focus on a shop
        Bundle extras = getArguments();

        if (extras != null)
        {
            if (extras.getBoolean("viewingShop"))
            {
                //get intent extra
                String shopLocation = extras.getString("shopLocation");

                if (shopLocation != null)
                {
                    Gson gson = new Gson();
                    returnedShopLocation = gson.fromJson(shopLocation, GeoPoint.class);
                }
            }
        }

        netCallback = new ConnectivityManager.NetworkCallback()
        {
            @Override
            public void onLost(Network network) {
                Toast.makeText(getContext(), R.string.warning_connection_lost_message, Toast.LENGTH_SHORT).show();
                hasConnection = false;
            }

            @Override
            public void onLosing(Network network, int maxMsToLive) {
                Toast.makeText(getContext(), R.string.warning_connection_losing_message, Toast.LENGTH_SHORT).show();
                hasConnection = false;
            }

            @Override
            public void onUnavailable() {
                Toast.makeText(getContext(), R.string.warning_connection_unavailable_message, Toast.LENGTH_SHORT).show();
                hasConnection = false;
            }

            @Override
            public void onAvailable(Network network) {
                hasConnection = true;
            }
        };

        //init connectivity manager
        connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        //register callback to monitor for connection changes
        connectivityManager.registerNetworkCallback(networkRequest, netCallback);

        //check if we are connected to network
        hasConnection = false;
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null)
        {
            if (networkInfo.isConnected())
            {
                hasConnection = true;
            }
        }

        if (!hasConnection)
        {
            Toast.makeText(getActivity(), R.string.no_internet_message, Toast.LENGTH_SHORT).show();
        }

        return root;
    }

    @Override
    public void onPause() {

        //if a shop is selected, unselect
        if (selectedShopSquare != null)
        {
            selectedShop = null;
            selectedShopSquare.remove();
            selectedShopSquare = null;

            //hide button panel
            LinearLayout btnPanel = binding.btnLayout;
            btnPanel.setVisibility(View.GONE);
        }

        //remove all circles
        for (Pair<Shop, Circle> circlePair : homeViewModel.getShopsWithMarkers())
        {
            if (circlePair.second != null)
            {
                circlePair.second.remove();
            }
        }

        //set all circles to null
        homeViewModel.resetCircles();

        //clear reference to viewing a shop
        returnedShopLocation = null;

        //clear intent extras
        if (getArguments() != null)
        {
            this.getArguments().clear();
        }

        //remove location callback
        if (fusedLocationProviderClient != null)
        {
            if (locationCallback != null)
            {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            }
        }

        //unregister network callback
        connectivityManager.unregisterNetworkCallback(netCallback);

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        //add styling to map to remove all points of interest that aren't businesses
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            getActivity(), R.raw.map_style));

            if (!success) {
                Log.e("i", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("i", "Can't find style. Error: ", e);
        }
        
        //move camera to current location if available
        if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && returnedShopLocation == null)
        {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            oldLocation = location;
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), defaultZoom));
                        }
                    });
        }
        else if (returnedShopLocation == null)
        {
            //move camera to default location
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(defaultLocation.latitude, defaultLocation.longitude), defaultZoom));
        }
        //else we need to look at a specific shop returned by an intent
        else
        {
            //go to returned shop
            googleMap.animateCamera(CameraUpdateFactory
                    .newLatLngZoom(new LatLng(returnedShopLocation.getLatitude(), returnedShopLocation.getLongitude()), defaultZoom));


            oldLocation = new Location(LocationManager.NETWORK_PROVIDER);
            oldLocation.setLatitude(returnedShopLocation.getLatitude());
            oldLocation.setLongitude(returnedShopLocation.getLongitude());
        }

        //set up location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                {
                    if (locationResult == null || returnedShopLocation != null)
                    {
                        return;
                    }

                    //if there is a significant difference between new and old location, update location
                    float distance = oldLocation.distanceTo(locationResult.getLastLocation());

                    if (distance > 2500)
                    {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()), map.getCameraPosition().zoom));
                        oldLocation = locationResult.getLastLocation();
                    }

                }
            }
        };

        //if we have permission, request location updates from location callback
        if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationProviderClient = new FusedLocationProviderClient(getActivity());
            LocationRequest locationRequest = LocationRequest.create();

            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                    locationCallback, Looper.getMainLooper());
        }

        map = googleMap;

        //set on point listener to google map
        map.setOnPoiClickListener(this);

        //set up listener for when camera moves
        map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {

                //if we have got "marker" locations, draw them
                if (homeViewModel.getDrawMarkers().getValue() == Boolean.TRUE)
                {
                    for (int i = 0; i < homeViewModel.getShopsWithMarkers().size(); i++)
                    {
                        homeViewModel.setCircleForShop(i, drawMapCircle(
                                map, new LatLng(homeViewModel.getShopsWithMarkers().get(i).first.getLocation().getLatitude(),
                                                homeViewModel.getShopsWithMarkers().get(i).first.getLocation().getLongitude()),
                                homeViewModel.getShopsWithMarkers().get(i).second, ContextCompat.getColor(getActivity(), R.color.primaryDarkColor)));
                    }
                }

                //if we have a selected shop
                if (selectedShop != null)
                {
                    //draw square for selected shop
                    selectedShopSquare = drawMapSquare(map, new LatLng(selectedShop.getLocation().getLatitude(), selectedShop.getLocation().getLongitude()), selectedShopSquare,
                            ContextCompat.getColor(getActivity(), R.color.selected));
                }
            }
        });

        //set listeners for circles and polygons
        map.setOnCircleClickListener(this);
        map.setOnPolygonClickListener(this);
    }

    @Override
    public void onPoiClick(@NonNull PointOfInterest poi) {
        //if a shop is already selected, remove the square around that shop
        if (selectedShopSquare != null)
        {
            selectedShopSquare.remove();
            selectedShopSquare = null;
        }

        //move camera to poi
        if (map.getCameraPosition().zoom > defaultZoom)
        {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(poi.latLng, map.getCameraPosition().zoom));
        }
        else
        {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(poi.latLng, defaultZoom));
        }

        //set up shop object
        selectedShop = new Shop(poi.name, poi.placeId, new GeoPoint(poi.latLng.latitude, poi.latLng.longitude));
        selectedShopSquare = drawMapSquare(map, new LatLng(selectedShop.getLocation().getLatitude(), selectedShop.getLocation().getLongitude()), selectedShopSquare,
                ContextCompat.getColor(getActivity(), R.color.selected));;

        LinearLayout btnPanel = getActivity().findViewById(R.id.btnLayout);
        btnPanel.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        //check what button was pressed
        switch (v.getId())
        {
            case R.id.createNewListAB:
                //go to create list activity
                Intent intent = new Intent(getActivity(), CreateListActivity.class);

                //set up extras - shop details
                Gson gson = new Gson();
                String shopData = gson.toJson(selectedShop);

                intent.putExtra("shopDetails", shopData);

                startActivity(intent);
                break;
            case R.id.viewListsAB:
                //navigate user to list fragment for this shop
                Gson gson2 = new Gson();
                String shopData2 = gson2.toJson(selectedShop);

                Bundle shopDataBundle = new Bundle();
                shopDataBundle.putString("shopDetails", shopData2);

                NavHostFragment.findNavController(this).navigate(R.id.action_navigation_home_to_myLists, shopDataBundle);

                break;
            case R.id.exitAB:
                //close button panel
                LinearLayout btnPanel = getActivity().findViewById(R.id.btnLayout);
                btnPanel.setVisibility(View.GONE);
                selectedShop = null;
                selectedShopSquare.remove();
                selectedShopSquare = null;

                break;
            default:
                Log.i("shopKeeper", "Default");
                break;
        }
    }

    //draw a circle that scales based on the map zoom level
    //source: https://stackoverflow.com/questions/16421937/draw-circles-of-constant-size-on-screen-in-google-maps-api-v2
    public Circle drawMapCircle(GoogleMap googleMap, LatLng latLng, Circle currentCircle, int colour) {

        // get 2 of the visible diagonal corners of the map (could also use farRight and nearLeft)
        LatLng topLeft = googleMap.getProjection().getVisibleRegion().farLeft;
        LatLng bottomRight = googleMap.getProjection().getVisibleRegion().nearRight;

        // use the Location class to calculate the distance between the 2 diagonal map points
        float results[] = new float[4]; // probably only need 3
        Location.distanceBetween(topLeft.latitude,topLeft.longitude,bottomRight.latitude,bottomRight.longitude,results);
        float diagonal = results[0];

        float radius = diagonal / 40;

        Circle circle = null;
        if (currentCircle != null) {
            // change the radius if the circle already exists (result of a zoom change)
            circle = currentCircle;

            circle.setRadius(radius);
        } else {
            // draw a new circle
            circle = googleMap.addCircle(new CircleOptions()
                    .center(latLng)
                    .radius(radius)
                    .strokeColor(colour)
                    .strokeWidth(10)
                    .clickable(true));
        }

        return circle;
    }

    //draw a square that scales based on the map zoom level
    public Polygon drawMapSquare(GoogleMap googleMap, LatLng latLng, Polygon currentPolygon, int colour)
    {
        // get 2 of the visible diagonal corners of the map (could also use farRight and nearLeft)
        LatLng topLeft = googleMap.getProjection().getVisibleRegion().farLeft;
        LatLng bottomRight = googleMap.getProjection().getVisibleRegion().nearRight;

        // use the Location class to calculate the distance between the 2 diagonal map points
        float results[] = new float[4]; // probably only need 3
        Location.distanceBetween(topLeft.latitude,topLeft.longitude,bottomRight.latitude,bottomRight.longitude,results);
        float diagonal = results[0];

        float radius = diagonal / 40;

        Polygon square = null;
        List<LatLng> points = new ArrayList<>();
        points.add(new LatLng(SphericalUtil.computeOffset(latLng, radius, 270).latitude, SphericalUtil.computeOffset(latLng, radius, 270).longitude));
        points.add(new LatLng(SphericalUtil.computeOffset(latLng, radius, 180).latitude, SphericalUtil.computeOffset(latLng, radius, 180).longitude));
        points.add(new LatLng(SphericalUtil.computeOffset(latLng, -radius, 90).latitude, SphericalUtil.computeOffset(latLng, radius, 90).longitude));
        points.add(new LatLng(SphericalUtil.computeOffset(latLng, radius, 360).latitude, SphericalUtil.computeOffset(latLng, radius, 360).longitude));

        if (currentPolygon != null)
        {
            //change the "radius" if the square already exists
            square = currentPolygon;

            square.setPoints(points);
        }
        else
        {
            //draw a new square
            square = googleMap.addPolygon(new PolygonOptions()
                    .addAll(points)
                    .strokeColor(colour)
                    .strokeWidth(10)
                    .clickable(true));
        }

        return square;
    }

    @Override
    public void onCircleClick(@NonNull Circle circle) {
        //zoom to circle
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(circle.getCenter(), higherZoom));
    }

    @Override
    public void onPolygonClick(@NonNull Polygon polygon) {
        //zoom to square
        LatLngBounds bounds = new LatLngBounds(polygon.getPoints().get(1),
                polygon.getPoints().get(3));

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), higherZoom));
    }
}