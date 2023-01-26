package uk.ac.abertay.cmp309.shopkeeper.ui.viewlist;

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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

import uk.ac.abertay.cmp309.shopkeeper.adapters.ItemsAdapter;
import uk.ac.abertay.cmp309.shopkeeper.List;
import uk.ac.abertay.cmp309.shopkeeper.R;
import uk.ac.abertay.cmp309.shopkeeper.Shop;
import uk.ac.abertay.cmp309.shopkeeper.databinding.ViewListFragmentBinding;
import uk.ac.abertay.cmp309.shopkeeper.ui.createlist.CreateListActivity;

public class ViewListFragment extends Fragment implements View.OnClickListener {

    private ViewListViewModel mViewModel;
    private ViewListFragmentBinding binding;

    private ItemsAdapter itemsAdapter;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback netCallback;

    private boolean hasConnection;

    public static ViewListFragment newInstance() {
        return new ViewListFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mViewModel = new ViewModelProvider(this).get(ViewListViewModel.class);

        binding = ViewListFragmentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //get list info
        String shopJSON = getArguments().getString("shopDetails");
        Integer listIndex = getArguments().getInt("listIndex");

        Gson gson = new Gson();
        mViewModel.setShop(gson.fromJson(shopJSON, Shop.class));
        mViewModel.setListIndex(listIndex);

        //observe loadItems to know when they are retrived from the database
        mViewModel.getLoadItems().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean)
                {
                    //hide no items message
                    TextView noItemsMessage = getActivity().findViewById(R.id.noItemsTV);
                    noItemsMessage.setVisibility(View.GONE);

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

        //set up item adapter
        mViewModel.getShop().getLists().get(mViewModel.getListIndex()).setItems(new ArrayList<>());
        itemsAdapter = new ItemsAdapter.ItemsAdapterBuilder(mViewModel.getShop().getLists().get(mViewModel.getListIndex()),
                getActivity(), hasConnection).favouriteSwitchMode(true).removingFavouriteSwitchMode(false).shopId(mViewModel.getShop().getShopId()).build();

        ListView itemsLV = binding.viewListItemsLV;
        itemsLV.setAdapter(itemsAdapter);

        //add header views to listview
        View shopDetails = getLayoutInflater().inflate(R.layout.display_shop_details_layout, null);
        itemsLV.addHeaderView(shopDetails);

        View listDetails = getLayoutInflater().inflate(R.layout.display_list_details_layout, null);
        itemsLV.addHeaderView(listDetails);

        View itemTitle = getLayoutInflater().inflate(R.layout.items_title_layout, null);
        itemsLV.addHeaderView(itemTitle);

        //get items
        mViewModel.getListItems();

        //add padding to listview
        LinearLayout actionBtns = binding.viewListActionBtns;
        ViewTreeObserver viewTreeObserver = actionBtns.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                actionBtns.getViewTreeObserver()
                        .removeOnGlobalLayoutListener(this);

                int height = actionBtns.getMeasuredHeight();
                int padding = height + 50;

                itemsLV.setPadding(0,0,0, padding);
            }
        });

        //set up click listener for action button
        FloatingActionButton updateList = binding.updateListAB;
        updateList.setOnClickListener(this);


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

        //set up text for list details
        TextView listName = getActivity().findViewById(R.id.listNameTV);
        TextView listNotes = getActivity().findViewById(R.id.listNotesTV);

        List l = mViewModel.getShop().getLists().get(mViewModel.getListIndex());

        if (StringUtils.isNotEmpty(l.getName()))
        {
            listName.setText(l.getName());
            listName.setVisibility(View.VISIBLE);
            listName.setTypeface(null, Typeface.BOLD);
        }
        else
        {
            listName.setVisibility(View.GONE);
        }

        if (StringUtils.isNotEmpty(l.getNotes()))
        {
            listNotes.setText(l.getNotes());
            listNotes.setVisibility(View.VISIBLE);
        }
        else
        {
            listNotes.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.updateListAB:
                //go to create list activity
                Intent intent = new Intent(getActivity(), CreateListActivity.class);

                //set up extras - shop details
                Gson gson = new Gson();
                String shopData = gson.toJson(mViewModel.getShop());

                intent.putExtra("shopDetails", shopData);
                intent.putExtra("listIndex", mViewModel.getListIndex());
                intent.putExtra("updatingList", true);

                startActivity(intent);

                break;
        }
    }
}