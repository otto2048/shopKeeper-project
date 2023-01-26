package uk.ac.abertay.cmp309.shopkeeper.ui.shopfavourites;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import uk.ac.abertay.cmp309.shopkeeper.adapters.ItemsAdapter;
import uk.ac.abertay.cmp309.shopkeeper.OnEmptyListView;
import uk.ac.abertay.cmp309.shopkeeper.R;
import uk.ac.abertay.cmp309.shopkeeper.Shop;
import uk.ac.abertay.cmp309.shopkeeper.databinding.ShopFavouritesFragmentBinding;

public class ShopFavouritesFragment extends Fragment implements OnEmptyListView<Boolean> {

    private ShopFavouritesViewModel mViewModel;
    private ShopFavouritesFragmentBinding binding;

    private ItemsAdapter itemsAdapter;

    private ConnectivityManager connectivityManager;

    private ConnectivityManager.NetworkCallback netCallback;

    private boolean hasConnection;

    public static ShopFavouritesFragment newInstance() {
        return new ShopFavouritesFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(ShopFavouritesViewModel.class);

        binding = ShopFavouritesFragmentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //get shop info
        String shopJSON = getArguments().getString("shopDetails");

        Gson gson = new Gson();
        mViewModel.setShop(gson.fromJson(shopJSON, Shop.class));

        //observe favourites loading
        mViewModel.getLoadFavourites().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean load) {
                if (load)
                {
                    //hide no favourite message
                    TextView noFavouriteMessage = getActivity().findViewById(R.id.noFavouritesTV);
                    noFavouriteMessage.setVisibility(View.GONE);

                    itemsAdapter.notifyDataSetChanged();
                }
            }
        });

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

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //set up list adapter
        itemsAdapter = new ItemsAdapter.ItemsAdapterBuilder(mViewModel.getShop().getFavourites(), getActivity(), hasConnection).favouriteSwitchMode(true).removingFavouriteSwitchMode(true)
                .shopId(mViewModel.getShop().getShopId()).build();
        ListView favouritesLV = binding.favouritesLV;

        itemsAdapter.registerOnEmptyListViewListener(this);

        //add header
        View favouriteTitle = getLayoutInflater().inflate(R.layout.favourites_title_layout, null);
        favouritesLV.addHeaderView(favouriteTitle);

        favouritesLV.setAdapter(itemsAdapter);

        //get favourites
        mViewModel.getFavourites();


    }

    @Override
    public void onPause() {
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
    public void onCallback(Boolean result) {
        if (result)
        {
            //show empty favourites message
            TextView noFavouriteMessage = getActivity().findViewById(R.id.noFavouritesTV);
            noFavouriteMessage.setVisibility(View.VISIBLE);
        }
    }
}