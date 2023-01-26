package uk.ac.abertay.cmp309.shopkeeper.ui.createlist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

import uk.ac.abertay.cmp309.shopkeeper.Item;
import uk.ac.abertay.cmp309.shopkeeper.adapters.ItemsAdapter;
import uk.ac.abertay.cmp309.shopkeeper.List;
import uk.ac.abertay.cmp309.shopkeeper.MainActivity;
import uk.ac.abertay.cmp309.shopkeeper.OnEmptyListView;
import uk.ac.abertay.cmp309.shopkeeper.R;
import uk.ac.abertay.cmp309.shopkeeper.Shop;
import uk.ac.abertay.cmp309.shopkeeper.ui.shopfavourites.CreateListWithShopFavouritesActivity;

public class CreateListActivity extends AppCompatActivity implements OnEmptyListView<Boolean> {
    private CreateListViewModel createListViewModel;
    private ItemsAdapter itemsAdapter;

    private ConnectivityManager connectivityManager;

    private boolean hasConnection;

    private boolean rotated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_list);

        //set view model
        createListViewModel = new ViewModelProvider(this).get(CreateListViewModel.class);

        //set up list view
        //get list view
        ListView itemLV = findViewById(R.id.itemLV);

        //add footer view - ability to input a new item
        View itemInput = getLayoutInflater().inflate(R.layout.create_item_layout, null);
        itemLV.addFooterView(itemInput);

        //item input text watchers
        EditText itemName = findViewById(R.id.itemNameET);
        itemName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                createListViewModel.getItemName().setValue(s.toString());
            }
        });

        EditText itemNotes = findViewById(R.id.itemNotesET);
        itemNotes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                createListViewModel.getItemNotes().setValue(s.toString());
            }
        });

        EditText itemPrice = findViewById(R.id.itemPriceET);
        itemPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                createListViewModel.getItemPrice().setValue(s.toString());
            }
        });

        //set text fields to values in view model
        itemName.setText(createListViewModel.getItemName().getValue());
        itemNotes.setText(createListViewModel.getItemNotes().getValue());
        itemPrice.setText(createListViewModel.getItemPrice().getValue());

        //add header view - containing shop details
        //get intent extra
        Gson gson = new Gson();

        //check if we are loading the shop from initial instance, or from a saved instance (view model exists)
        if (savedInstanceState == null)
        {
            String shopJSON = getIntent().getStringExtra("shopDetails");
            createListViewModel.setShop(gson.fromJson(shopJSON, Shop.class));
            rotated = false;
        }
        else
        {
            rotated = true;
        }

        //create header views
        View shopDetails = getLayoutInflater().inflate(R.layout.display_shop_details_layout, null);
        itemLV.addHeaderView(shopDetails);

        View listDetailInput = getLayoutInflater().inflate(R.layout.create_list_layout, null);
        itemLV.addHeaderView(listDetailInput);

        View itemTitle = getLayoutInflater().inflate(R.layout.items_title_layout, null);
        itemLV.addHeaderView(itemTitle);

        //create text watchers for list detail input
        EditText listName = findViewById(R.id.listNameET);
        listName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                createListViewModel.getListName().setValue(s.toString());
            }
        });

        EditText listNotes = findViewById(R.id.listNotesET);
        listNotes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                createListViewModel.getListNotes().setValue(s.toString());
            }
        });

        //set text fields to view model values for list detail input
        listName.setText(createListViewModel.getListName().getValue());
        listNotes.setText(createListViewModel.getListNotes().getValue());

        //add padding to list view so that action buttons do not overlap it
        LinearLayout actionBtns = findViewById(R.id.actionButtons);
        ViewTreeObserver viewTreeObserver = actionBtns.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                actionBtns.getViewTreeObserver()
                        .removeOnGlobalLayoutListener(this);

                int height = actionBtns.getMeasuredHeight();
                int padding = height + 50;

                itemLV.setPadding(0,0,0, padding);
            }
        });

        //observe send to view, this will tell us if the user needs to be sent to the new list page for this shop
        createListViewModel.getSendToView().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean)
                {
                    //send to lists page
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);

                    //set up extras - list details
                    String shopData = gson.toJson(createListViewModel.getShop());

                    intent.putExtra("shopDetails", shopData);
                    intent.putExtra("listIndex", createListViewModel.getListIndex());

                    //move main activity to front of stack if it is running
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

                    startActivity(intent);
                    finish();
                }
            }
        });

        //observe if items have changed
        createListViewModel.getShopListChanged().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean)
                {
                    //hide no items message
                    TextView noItemsTV = findViewById(R.id.noItemsTV);
                    noItemsTV.setVisibility(View.GONE);

                    EditText nameET = findViewById(R.id.itemNameET);
                    EditText notesET = findViewById(R.id.itemNotesET);
                    EditText priceET = findViewById(R.id.itemPriceET);

                    //clear item fields
                    nameET.getText().clear();
                    notesET.getText().clear();
                    priceET.getText().clear();

                    if (itemsAdapter == null)
                    {
                        ListView itemLV = findViewById(R.id.itemLV);
                        itemsAdapter = new ItemsAdapter.ItemsAdapterBuilder(createListViewModel.getShop().getLists().get(createListViewModel.getListIndex()), getApplicationContext(), hasConnection)
                                .removableMode(true).shopId(createListViewModel.getShop().getShopId()).build();
                        itemLV.setAdapter(itemsAdapter);
                    }

                    itemsAdapter.notifyDataSetChanged();

                    createListViewModel.getShopListChanged().setValue(false);
                }
            }
        });

        //observe any error messages from adding items
        createListViewModel.getErrorMessages().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                //decode error messages
                Gson gson = new Gson();
                ArrayList<String> messages = gson.fromJson(s,
                        new TypeToken<ArrayList<String>>(){}.getType());

                //display as toasts
                for (String message : messages)
                {
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });

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

    }

    @Override
    protected void onResume() {
        super.onResume();

        //set up text for shop details
        TextView shopName = findViewById(R.id.shopNameTV);
        shopName.setText(createListViewModel.getShop().getName());

        Intent intent = getIntent();

        Bundle extras = intent.getExtras();

        if (extras != null)
        {
            //check if the list needs to be updated from adding favourites
            if (extras.getBoolean("updateShop"))
            {
                //get intent extra
                String shopJSON = getIntent().getStringExtra("shopDetails");

                //if there was shopJSON returned
                if (shopJSON != null)
                {
                    //convert to Shop object
                    Gson gson = new Gson();

                    createListViewModel.updateListUsed(gson.fromJson(shopJSON, Shop.class).getLists().get(createListViewModel.getListIndex()));
                }
            }
            //check if we're loading a list that is being updated
            else if (extras.getBoolean("updatingList", false))
            {
                //get intent extra
                int listIndex = getIntent().getIntExtra("listIndex", 0);

                createListViewModel.setListIndex(listIndex);

                //add name and notes to list fields if they exist (adding an existing list)
                if (createListViewModel.getShop().getLists().get(createListViewModel.getListIndex()).getName() != null)
                {
                    //add list name to list detail form
                    EditText name = findViewById(R.id.listNameET);
                    name.setText(createListViewModel.getShop().getLists().get(createListViewModel.getListIndex()).getName());
                }

                if (createListViewModel.getShop().getLists().get(createListViewModel.getListIndex()).getNotes() != null)
                {
                    //add list notes to list detail form
                    EditText notes = findViewById(R.id.listNotesET);
                    notes.setText(createListViewModel.getShop().getLists().get(createListViewModel.getListIndex()).getNotes());
                }
            }
            else
            {
                if (createListViewModel.getListIndex() == -1) {
                    //create a new list
                    createListViewModel.getShop().addList(new List());

                    //set list index
                    createListViewModel.setListIndex(createListViewModel.getShop().getLists().size() - 1);
                }
            }
        }
        else
        {
            if (createListViewModel.getListIndex() == -1)
            {
                //create a new list
                createListViewModel.getShop().addList(new List());

                //set list index
                createListViewModel.setListIndex(createListViewModel.getShop().getLists().size() - 1);
            }

        }

        //set up itemAdapter
        ListView itemLV = findViewById(R.id.itemLV);
        itemsAdapter = new ItemsAdapter.ItemsAdapterBuilder(createListViewModel.getShop().getLists().get(createListViewModel.getListIndex()), getApplicationContext(), hasConnection).removableMode(true).shopId(createListViewModel.getShop().getShopId()).build();
        itemsAdapter.registerOnEmptyListViewListener(this);
        itemLV.setAdapter(itemsAdapter);

        //check if no items message needs to be displayed
        if (rotated)
        {
            TextView noItemsTV = findViewById(R.id.noItemsTV);

            if (!createListViewModel.getShop().getLists().get(createListViewModel.getListIndex()).getItems().isEmpty())
            {
                //hide no items message
                noItemsTV.setVisibility(View.GONE);
            }
            else
            {
                //show no items message
                noItemsTV.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onPause() {
        //unregister network callback
        connectivityManager.unregisterNetworkCallback(netCallback);
        super.onPause();
    }

    //add new item to list
    public void addItem(View v)
    {
        //get field input
        EditText nameET = findViewById(R.id.itemNameET);
        String name = nameET.getText().toString();

        EditText notesET = findViewById(R.id.itemNotesET);
        String notes = null;

        //optional field, check if null
        if (StringUtils.isNotEmpty(notesET.getText()))
        {
            notes = notesET.getText().toString();
        }

        EditText priceET = findViewById(R.id.itemPriceET);
        String price = null;

        //optional field, check if null
        if (StringUtils.isNotEmpty(priceET.getText()))
        {
            price = priceET.getText().toString();
        }

        //create Item object
        Float priceFloat = null;
        if (price != null)
        {
            priceFloat = Float.parseFloat(price);
        }

        Item newItem = new Item(name, notes, priceFloat);

        //add Item to list view
        createListViewModel.addItemToList(newItem);
    }

    //create list
    public void createList(View v) {
        //get list info
        EditText listNameET = findViewById(R.id.listNameET);
        String name = listNameET.getText().toString();

        EditText listNotesET = findViewById(R.id.listNotesET);
        String notes = listNotesET.getText().toString();

        createListViewModel.createList(name, notes, hasConnection, itemsAdapter.getItemsToRemove());
    }

    //select favourites to add to list
    public void selectFavourites(View v)
    {
        //send to select favourites page
        Intent intent = new Intent(getApplicationContext(), CreateListWithShopFavouritesActivity.class);

        //set up extras - shop details
        Gson gson = new Gson();
        String shopData = gson.toJson(createListViewModel.getShop());

        intent.putExtra("shopDetails", shopData);
        intent.putExtra("listIndex", createListViewModel.getListIndex());

        startActivity(intent);
    }

    //source: https://developer.android.com/reference/android/app/Activity#onNewIntent(android.content.Intent)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    @Override
    public void onCallback(Boolean result) {
        //show no items message
        TextView noItemsTV = findViewById(R.id.noItemsTV);
        noItemsTV.setVisibility(View.VISIBLE);
    }

    ConnectivityManager.NetworkCallback netCallback = new ConnectivityManager.NetworkCallback()
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
}