package uk.ac.abertay.cmp309.shopkeeper.ui.mylists;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.ArrayList;

import uk.ac.abertay.cmp309.shopkeeper.List;
import uk.ac.abertay.cmp309.shopkeeper.adapters.ListAdapter;
import uk.ac.abertay.cmp309.shopkeeper.OnEmptyListView;
import uk.ac.abertay.cmp309.shopkeeper.R;
import uk.ac.abertay.cmp309.shopkeeper.Shop;
import uk.ac.abertay.cmp309.shopkeeper.databinding.MyListsFragmentBinding;
import uk.ac.abertay.cmp309.shopkeeper.ui.createlist.CreateListActivity;

public class MyListsFragment extends Fragment implements View.OnClickListener, OnEmptyListView<Boolean> {

    private MyListsViewModel mViewModel;
    private MyListsFragmentBinding binding;

    private ListAdapter listAdapter;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback netCallback;
    private boolean hasConnection;

    public static MyListsFragment newInstance() {
        return new MyListsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mViewModel = new ViewModelProvider(this).get(MyListsViewModel.class);

        binding = MyListsFragmentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //get shop info
        String shopJSON = getArguments().getString("shopDetails");

        Gson gson = new Gson();
        mViewModel.setShop(gson.fromJson(shopJSON, Shop.class));

        //observe loadLists to see when data is got from database
        mViewModel.getLoadLists().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean)
                {
                    //hide no lists message
                    TextView noListsMessage = getActivity().findViewById(R.id.noListsTV);
                    noListsMessage.setVisibility(View.GONE);

                    listAdapter.notifyDataSetChanged();
                }
            }
        });

        //add onclick listeners to buttons
        FloatingActionButton favouriteBtn = binding.favouritesABForList;
        favouriteBtn.setOnClickListener(this);

        FloatingActionButton createBtn = binding.createABForList;
        createBtn.setOnClickListener(this);

        FloatingActionButton viewOnMap = binding.viewOnMapAB;
        viewOnMap.setOnClickListener(this);

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
        mViewModel.getShop().setLists(new ArrayList<>());
        listAdapter = new ListAdapter(getActivity(), mViewModel.getShop().getLists(),
                mViewModel.getShop().getShopId(), hasConnection);

        listAdapter.registerOnEmptyListViewListener(this);

        ListView listsLV = binding.listLV;
        listsLV.setAdapter(listAdapter);

        //add header view to listview
        View shopDetails = getLayoutInflater().inflate(R.layout.display_shop_details_layout, null);
        listsLV.addHeaderView(shopDetails);

        View listTitle = getLayoutInflater().inflate(R.layout.lists_title_layout, null);
        listsLV.addHeaderView(listTitle);

        Fragment fragment = this;

        //set click listener for link to individual list page
        listsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //handle header views
                if (position >= listsLV.getHeaderViewsCount())
                {
                    List list = (List) parent.getAdapter().getItem(position);

                    //setting up bundle - list details
                    Gson gson = new Gson();
                    String shopDetails = gson.toJson(mViewModel.getShop());

                    Bundle listDataBundle = new Bundle();

                    listDataBundle.putInt("listIndex", position - listsLV.getHeaderViewsCount());
                    listDataBundle.putString("shopDetails", shopDetails);

                    NavHostFragment.findNavController(fragment).navigate(R.id.action_myLists_to_viewList, listDataBundle);
                }

            }
        });

        //get lists
        mViewModel.getListData();

        //add padding to listview
        LinearLayout actionBtns = binding.actionButtonsForList;
        ViewTreeObserver viewTreeObserver = actionBtns.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                actionBtns.getViewTreeObserver()
                        .removeOnGlobalLayoutListener(this);

                int height = actionBtns.getMeasuredHeight();
                int padding = height + 50;

                listsLV.setPadding(0,0,0, padding);
            }
        });
    }

    @Override
    public void onPause() {
        //unregister network callback
        connectivityManager.unregisterNetworkCallback(netCallback);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        //set up text for shop details
        TextView shopName = getActivity().findViewById(R.id.shopNameTV);
        shopName.setText(mViewModel.getShop().getName());
        shopName.setTypeface(null, Typeface.BOLD);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onClick(View v) {
        Gson gson = new Gson();

        //check what button was pressed
        switch (v.getId())
        {
            case R.id.createABForList:
                //go to create list activity
                Intent intent = new Intent(getActivity(), CreateListActivity.class);

                //set up extras - shop details
                String shopData = gson.toJson(mViewModel.getShop());

                intent.putExtra("shopDetails", shopData);

                startActivity(intent);
                break;
            case R.id.favouritesABForList:
                //go to favourites list
                String shopDataFavourites = gson.toJson(mViewModel.getShop());

                Bundle shopDataBundle = new Bundle();
                shopDataBundle.putString("shopDetails", shopDataFavourites);

                NavHostFragment.findNavController(this).navigate(R.id.action_myLists_to_shopFavouritesFragment, shopDataBundle);

                break;
            case R.id.viewOnMapAB:
                //go to map
                Bundle shopID = new Bundle();
                shopID.putBoolean("viewingShop", true);

                String shopLatLng = gson.toJson(mViewModel.getShop().getLocation());
                shopID.putString("shopLocation", shopLatLng);

                NavHostFragment.findNavController(this).navigate(R.id.action_myLists_to_navigation_home, shopID);
            default:
                break;
        }
    }

    //handling when theres no items in the list view
    @Override
    public void onCallback(Boolean result) {
        if (result)
        {
            //show empty lists message
            TextView noListsMessage = getActivity().findViewById(R.id.noListsTV);
            noListsMessage.setVisibility(View.VISIBLE);
        }
    }
}