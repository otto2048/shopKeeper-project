package uk.ac.abertay.cmp309.shopkeeper.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import uk.ac.abertay.cmp309.shopkeeper.Item;
import uk.ac.abertay.cmp309.shopkeeper.List;
import uk.ac.abertay.cmp309.shopkeeper.OnEmptyListView;
import uk.ac.abertay.cmp309.shopkeeper.OnFirestoreEventListener;
import uk.ac.abertay.cmp309.shopkeeper.R;
import uk.ac.abertay.cmp309.shopkeeper.data.ListRepository;
import uk.ac.abertay.cmp309.shopkeeper.data.ShopRepository;

public class ItemsAdapter extends ArrayAdapter<Item> implements OnFirestoreEventListener<Boolean> {
    private final List list;
    private String shopId;
    private boolean favouriteSwitchMode;
    private boolean removingFavouriteSwitchMode;
    private boolean removingFavourite = false;
    private boolean removableMode;
    private boolean selectableMode;
    private final ShopRepository shopRepository;
    private final ListRepository listRepository;
    private ArrayList<Item> selected;
    private Item itemToRemove;

    private ArrayList<Item> itemsToRemove;

    private boolean hasInternetConnection;

    private OnEmptyListView<Boolean> emptyListView;

    //set up items adapter with an ItemsAdapterBuilder object
    private ItemsAdapter(ItemsAdapterBuilder builder)
    {
        super(builder.context, 0, builder.list.getItems());

        list = builder.list;

        shopRepository = new ShopRepository();

        shopId = builder.shopId;

        favouriteSwitchMode = builder.favouriteSwitchMode;
        removableMode = builder.removableMode;
        selectableMode = builder.selectableMode;

        selected = new ArrayList<>();

        removingFavouriteSwitchMode = builder.removingFavouriteSwitchMode;
        listRepository = new ListRepository();

        hasInternetConnection = builder.hasConnection;

        itemsToRemove = new ArrayList<>();
    }

    public ArrayList<Item> getSelected() {
        return selected;
    }

    public void setSelected(ArrayList<Item> selected) {
        this.selected = selected;
    }

    public void addToSelected(Item item)
    {
        selected.add(item);
    }

    public void removeFromSelected(Item item)
    {
        selected.remove(item);
    }

    public void registerOnEmptyListViewListener(OnEmptyListView emptyListView)
    {
        this.emptyListView = emptyListView;
    }

    public ArrayList<Item> getItemsToRemove() {
        return itemsToRemove;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        /* Get the item data for this position. */
        Item i = getItem(position);

        /* Check if an existing view is being reused, otherwise inflate the view. */
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        }

        /* Lookup views. */
        TextView itemNameTV = (TextView) convertView.findViewById(R.id.itemNameTV);
        TextView itemNotesTV = (TextView) convertView.findViewById(R.id.itemNotesTV);
        TextView itemPriceTV = (TextView) convertView.findViewById(R.id.itemPriceTV);
        Switch favouriteSwitch = convertView.findViewById(R.id.favouriteSwitch);
        Button removeBtn = convertView.findViewById(R.id.removeItemFromListBtn);

        /* Add the data to the template view. */
        itemNameTV.setText(i.getName());

        if (i.getNotes() != null)
        {
            itemNotesTV.setText(i.getNotes());
            itemNotesTV.setVisibility(View.VISIBLE);
        }
        else
        {
            itemNotesTV.setVisibility(View.GONE);
        }

        if (i.getPrice() != null)
        {
            itemPriceTV.setText("£" + Float.toString(i.getPrice()));
            itemPriceTV.setVisibility(View.VISIBLE);
        }
        else
        {
            itemPriceTV.setVisibility(View.GONE);
        }

        //check if this is selected
        if (selectableMode)
        {
            if (selected.contains(i))
            {
                convertView.findViewById(R.id.displayItemLL).setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primaryLightColor));
            }
        }

        //set up favourite switch
        if (!favouriteSwitchMode)
        {
            favouriteSwitch.setVisibility(View.GONE);
        }
        else
        {
            //set as favourite if this item is one
            if (i.getIsFavourite() != null)
            {
                if (i.getIsFavourite())
                {
                    favouriteSwitch.setChecked(true);
                }
            }
            else
            {
                favouriteSwitch.setChecked(false);
            }

            //set up event listener
            shopRepository.registerOnFirestoreEventListenerForBoolean(this);
            favouriteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    //set/remove favourite for this shop
                    if (buttonView.isPressed())
                    {
                        if (isChecked)
                        {
                            //add favourite
                            i.setIsFavourite(true);
                            shopRepository.doAddFavourite(shopId, i, hasInternetConnection);
                        }
                        else
                        {
                            //remove favourite
                            if (!hasInternetConnection)
                            {
                                favouriteSwitch.setChecked(true);
                                Toast.makeText(getContext(), "Cannot remove favourites whilst offline", Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                i.setIsFavourite(false);

                                //check if we should remove the item from the list
                                if (removingFavouriteSwitchMode)
                                {
                                    itemToRemove = i;
                                    removingFavourite = true;
                                }

                                shopRepository.doRemoveFavourite(shopId, i);
                            }

                        }
                    }

                }
            });
        }

        //set up removable button
        if (!removableMode)
        {
            removeBtn.setVisibility(View.GONE);
        }
        else
        {
            //set up click listener to remove this item from the list when clicked
            listRepository.registerOnFirestoreEventListenerForBoolean(this);
            removeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Item removeItem = getItem(position);

                    //need to remove from firestore
                    if (removeItem.getId() != null && list.getId() != null)
                    {
                        //track this on itemsToRemove
                        itemsToRemove.add(removeItem);
                    }

                    list.removeItem(removeItem);

                    if (list.getItems().isEmpty())
                    {
                        emptyListView.onCallback(true);
                    }

                    notifyDataSetChanged();
                }
            });
        }

        /* Return the completed view to render on screen. */
        return convertView;
    }

    //callback for when firestore operations are done
    @Override
    public void onCallback(Boolean result) {
        if (result)
        {
            if (favouriteSwitchMode)
            {
                //check if the favourite is being removed or added
                //if removing
                if (removingFavourite)
                {
                    //remove the item
                    list.removeItem(itemToRemove);
                    this.notifyDataSetChanged();
                    removingFavourite = false;

                    if (list.getItems().isEmpty())
                    {
                        emptyListView.onCallback(true);
                    }
                }
            }
        }
        else
        {
            if (favouriteSwitchMode) {
                Toast.makeText(getContext(), R.string.failed_to_remove_add_favourite_message, Toast.LENGTH_SHORT).show();
            }

            if (removableMode)
            {
                Toast.makeText(getContext(), R.string.failed_to_remove_item_message, Toast.LENGTH_SHORT).show();
            }

        }
    }

    //build items adapter to either, display items
    //                               have favourites switches - requires shopId
    //                                  have favourites switches and be able to be removed from the list
    //                               display items that can be removed from the list - requires shopId
    //                              have items that can be selected
    public static class ItemsAdapterBuilder
    {
        private final List list;
        private String shopId;
        private boolean favouriteSwitchMode;
        private boolean removingFavouriteSwitchMode;
        private boolean removableMode;
        private boolean selectableMode;
        private Context context;
        private boolean hasConnection;

        public ItemsAdapterBuilder(List list, Context context, boolean hasConnection)
        {
            this.list = list;
            this.context = context;
            this.hasConnection = hasConnection;
        }

        //functions for optional parameters
        public ItemsAdapterBuilder shopId(String shopId)
        {
            this.shopId = shopId;

            return this;
        }

        public ItemsAdapterBuilder favouriteSwitchMode(boolean favouriteSwitchMode)
        {
            this.favouriteSwitchMode = favouriteSwitchMode;

            return this;
        }

        public ItemsAdapterBuilder removingFavouriteSwitchMode(boolean toggle)
        {
            this.removingFavouriteSwitchMode = toggle;

            return this;
        }

        public ItemsAdapterBuilder removableMode(boolean removableMode)
        {
            this.removableMode = removableMode;

            return this;
        }

        public ItemsAdapterBuilder selectableMode(boolean selectableMode)
        {
            this.selectableMode = selectableMode;
            return this;
        }

        public ItemsAdapter build()
        {
            ItemsAdapter itemsAdapter = new ItemsAdapter(this);
            return itemsAdapter;
        }


    }
}
