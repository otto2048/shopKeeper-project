package uk.ac.abertay.cmp309.shopkeeper.ui.createlist;

import android.util.Pair;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import uk.ac.abertay.cmp309.shopkeeper.Item;
import uk.ac.abertay.cmp309.shopkeeper.List;
import uk.ac.abertay.cmp309.shopkeeper.OnFirestoreEventListener;
import uk.ac.abertay.cmp309.shopkeeper.Shop;
import uk.ac.abertay.cmp309.shopkeeper.Validation;
import uk.ac.abertay.cmp309.shopkeeper.data.ListRepository;

public class CreateListViewModel extends ViewModel implements OnFirestoreEventListener<String> {

    private Shop shop;

    private ListRepository listRepository;

    private Validation validation;

    private MutableLiveData<Boolean> sendToView;
    private MutableLiveData<Boolean> shopListChanged;
    private MutableLiveData<String> errorMessages;

    //tracking input field state
    private MutableLiveData<String> listName;
    private MutableLiveData<String> listNotes;
    private MutableLiveData<String> itemName;
    private MutableLiveData<String> itemNotes;
    private MutableLiveData<String> itemPrice;

    private final Gson gson;

    private int listIndex;

    public CreateListViewModel()
    {
        listRepository = new ListRepository();

        validation = new Validation();

        sendToView = new MutableLiveData<>();
        shopListChanged = new MutableLiveData<>();
        errorMessages = new MutableLiveData<>();

        listName = new MutableLiveData<>();
        listNotes = new MutableLiveData<>();
        itemName = new MutableLiveData<>();
        itemNotes = new MutableLiveData<>();
        itemPrice = new MutableLiveData<>();

        listName.setValue("");
        listNotes.setValue("");
        itemName.setValue("");
        itemNotes.setValue("");
        itemPrice.setValue("");

        listIndex = -1;
        gson = new Gson();
    }

    public MutableLiveData<String> getItemName() {
        return itemName;
    }

    public MutableLiveData<String> getItemNotes() {
        return itemNotes;
    }

    public MutableLiveData<String> getItemPrice() {
        return itemPrice;
    }

    public MutableLiveData<String> getListName() {
        return listName;
    }

    public MutableLiveData<String> getListNotes() {
        return listNotes;
    }

    public int getListIndex() {
        return listIndex;
    }

    public void setListIndex(int listIndex) {
        this.listIndex = listIndex;
    }

    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

    public MutableLiveData<Boolean> getShopListChanged() {
        return shopListChanged;
    }

    public MutableLiveData<Boolean> getSendToView() {
        return sendToView;
    }

    public MutableLiveData<String> getErrorMessages() {
        return errorMessages;
    }

    public void addItemToList(Item item)
    {
        //add item to list in shop object
        //validate item
        String validationResult = validation.validateItemDetails(item);

        if (validationResult == null)
        {
            //check if item is already in list
            if (shop.getLists().get(listIndex).getItems().contains(item))
            {
                ArrayList<String> errors = new ArrayList<>();
                errors.add("Failed to add item, " + item.getName() + " is already on the list!");

                errorMessages.setValue(gson.toJson(errors));

                return;
            }

            //add item to list
            shop.addItemToList(listIndex, item);
            shopListChanged.setValue(true);
        }
        else
        {
            //send error messages
            errorMessages.setValue(validationResult);
        }

    }

    public void createList(String listName, String listNotes, Boolean activeInternetConnection, ArrayList<Item> removeItems)
    {
        //assign list information
        shop.getLists().get(listIndex).setName(listName);
        shop.getLists().get(listIndex).setNotes(listNotes);

        //validate list
        String listJSON = gson.toJson(shop.getLists().get(listIndex));

        String validationResult = validation.validateListDetails(listJSON);
        if (validationResult == null)
        {
            shop.setHasLists(true);

            //serialize shop object
            String shopData = gson.toJson(shop);

            //use list repository to create/update the list
            listRepository.registerOnFirestoreEventListenerForString(this);
            listRepository.doCreateAndUpdateList(shopData, listIndex, activeInternetConnection, removeItems);
        }
        else
        {
            //send error messages
            errorMessages.setValue(validationResult);
        }
    }

    @Override
    public void onCallback(String resultId) {
        if (resultId != null)
        {
            //send to list
            shop.getLists().get(listIndex).setId(resultId);
            sendToView.setValue(true);
        }
        else
        {
            //display error message
            sendToView.setValue(false);
        }
    }

    //update the list in the shops list collection
    public void updateListUsed(List list)
    {
        ArrayList<Item> items = list.getItems();

        //validate that the list has no duplicates
        String validationResult = validation.removeListDuplicates(items);
        if (validationResult == null)
        {
            shop.getLists().get(listIndex).setItems(items);
            shopListChanged.setValue(true);
        }
        else
        {
            //work out if any of the items can still be added
            Gson gson = new Gson();

            ArrayList<Pair<String, Item>> errors = gson.fromJson(validationResult, new TypeToken<ArrayList<Pair<String, Item>>>(){}.getType());
            ArrayList<String> errorStrings = new ArrayList<String>();

            for (Pair<String, Item> error : errors)
            {
                items.remove(error.second);
                errorStrings.add(error.first);
            }

            if (!items.isEmpty())
            {
                //can still add some items
                shop.getLists().get(listIndex).setItems(items);
                shopListChanged.setValue(true);
            }

            errorMessages.setValue(gson.toJson(errorStrings));
        }
    }
}
