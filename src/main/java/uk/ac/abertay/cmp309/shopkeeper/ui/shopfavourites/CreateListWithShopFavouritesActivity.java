package uk.ac.abertay.cmp309.shopkeeper.ui.shopfavourites;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import uk.ac.abertay.cmp309.shopkeeper.Item;
import uk.ac.abertay.cmp309.shopkeeper.adapters.ItemsAdapter;
import uk.ac.abertay.cmp309.shopkeeper.R;
import uk.ac.abertay.cmp309.shopkeeper.Shop;
import uk.ac.abertay.cmp309.shopkeeper.ui.createlist.CreateListActivity;

public class CreateListWithShopFavouritesActivity extends AppCompatActivity implements View.OnClickListener {
    private ShopFavouritesViewModel mViewModel;
    private ItemsAdapter itemsAdapter;

    private ConnectivityManager connectivityManager;

    private boolean hasConnection;
    private ConnectivityManager.NetworkCallback netCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_list_with_shop_favourites_layout);

        mViewModel = new ViewModelProvider(this).get(ShopFavouritesViewModel.class);

        //get shop info
        String shopJSON = getIntent().getStringExtra("shopDetails");
        int listIndex = getIntent().getIntExtra("listIndex", 0);

        mViewModel.setListIndex(listIndex);

        Gson gson = new Gson();

        if (savedInstanceState == null) {
            mViewModel.setShop(gson.fromJson(shopJSON, Shop.class));
        }

        //observe loading favourite items
        mViewModel.getLoadFavourites().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean load) {
                if (load)
                {
                    //hide no favourite message
                    TextView noFavouriteMessage = findViewById(R.id.noFavouritesTV);
                    noFavouriteMessage.setVisibility(View.GONE);

                    itemsAdapter.notifyDataSetChanged();
                }
            }
        });

        //set up list adapter
        itemsAdapter = new ItemsAdapter.ItemsAdapterBuilder(mViewModel.getShop().getFavourites(), getApplicationContext(), hasConnection).selectableMode(true).build();
        ListView favouritesLV = findViewById(R.id.selectFavouritesLV);

        //add header
        View favouriteTitle = getLayoutInflater().inflate(R.layout.favourites_title_layout, null);
        favouritesLV.addHeaderView(favouriteTitle);

        favouritesLV.setAdapter(itemsAdapter);

        favouritesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //handle header views
                if (position >= favouritesLV.getHeaderViewsCount())
                {
                    if (!itemsAdapter.getSelected().contains((Item) parent.getAdapter().getItem(position)))
                    {
                        itemsAdapter.addToSelected((Item) parent.getAdapter().getItem(position));
                        mViewModel.getShop().getLists().get(mViewModel.getListIndex()).addItem((Item) parent.getAdapter().getItem(position));
                        view.findViewById(R.id.displayItemLL).setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.primaryLightColor));
                    }
                    else
                    {
                        itemsAdapter.removeFromSelected((Item) parent.getAdapter().getItem(position));
                        mViewModel.getShop().getLists().get(mViewModel.getListIndex()).removeItem((Item) parent.getAdapter().getItem(position));
                        view.findViewById(R.id.displayItemLL).setBackgroundColor(0x00000000);
                    }
                }
            }
        });

        //get favourites
        if (mViewModel.getShop().getFavourites().getItems().isEmpty())
        {
            mViewModel.getFavourites();
        }

        //add event listeners to buttons
        FloatingActionButton exitBtn = (FloatingActionButton) findViewById(R.id.cancelAddingItemsAB);
        FloatingActionButton confirmBtn = (FloatingActionButton) findViewById(R.id.confirmAddingItemsAB);

        exitBtn.setOnClickListener(this);
        confirmBtn.setOnClickListener(this);

        //add padding to listview
        ViewTreeObserver viewTreeObserver = exitBtn.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                exitBtn.getViewTreeObserver()
                        .removeOnGlobalLayoutListener(this);

                int height = exitBtn.getMeasuredHeight();
                int padding = height + 50;

                favouritesLV.setPadding(0,0,0, padding);
            }
        });

        netCallback = new ConnectivityManager.NetworkCallback()
        {
            @Override
            public void onLost(Network network) {
                Toast.makeText(getApplicationContext(), R.string.warning_connection_lost_message, Toast.LENGTH_SHORT).show();
                hasConnection = false;
            }

            @Override
            public void onLosing(Network network, int maxMsToLive) {
                Toast.makeText(getApplicationContext(), R.string.warning_connection_losing_message, Toast.LENGTH_SHORT).show();
                hasConnection = false;
            }

            @Override
            public void onUnavailable() {
                Toast.makeText(getApplicationContext(), R.string.warning_connection_unavailable_message, Toast.LENGTH_SHORT).show();
                hasConnection = false;
            }

            @Override
            public void onAvailable(Network network) {
                hasConnection = true;
            }
        };

        //init connectivity manager
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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

        //check if any items are selected
        if (savedInstanceState != null)
        {
            String selected = savedInstanceState.getString("selectedItems");

            if (selected != null)
            {
                itemsAdapter.setSelected(gson.fromJson(selected, new TypeToken<ArrayList<Item>>(){}.getType()));
            }
        }
    }

    @Override
    protected void onPause() {
        //unregister network callback
        connectivityManager.unregisterNetworkCallback(netCallback);
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.confirmAddingItemsAB:
                //send shop back to the create page
                Intent intent = new Intent(getApplicationContext(), CreateListActivity.class);

                Gson gson = new Gson();
                String shopDetails = gson.toJson(mViewModel.getShop());

                intent.putExtra("shopDetails", shopDetails);
                intent.putExtra("updateShop", true);

                startActivity(intent);
                finish();

                break;
            case R.id.cancelAddingItemsAB:
                //close the favourites section
                Intent intentCancel = new Intent(getApplicationContext(), CreateListActivity.class);

                intentCancel.putExtra("updateShop", true);

                startActivity(intentCancel);
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //save the items that have been selected
        Gson gson = new Gson();

        if (!itemsAdapter.getSelected().isEmpty())
        {
            String selected = gson.toJson(itemsAdapter.getSelected());
            outState.putString("selectedItems", selected);
        }

    }
}
