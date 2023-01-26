package uk.ac.abertay.cmp309.shopkeeper.ui.viewlist;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import uk.ac.abertay.cmp309.shopkeeper.Item;
import uk.ac.abertay.cmp309.shopkeeper.OnFirestoreEventListener;
import uk.ac.abertay.cmp309.shopkeeper.Shop;
import uk.ac.abertay.cmp309.shopkeeper.data.ListRepository;

public class ViewListViewModel extends ViewModel implements OnFirestoreEventListener<Task<QuerySnapshot>> {
    private Shop shop;
    private int listIndex;
    private ListRepository listRepository;
    private MutableLiveData<Boolean> loadItems;

    public ViewListViewModel()
    {
        listRepository = new ListRepository();
        loadItems = new MutableLiveData<>();
    }

    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

    public void setListIndex(int listIndex) {
        this.listIndex = listIndex;
    }

    public int getListIndex() {
        return listIndex;
    }

    public void getListItems()
    {
        //use list repository to get list items
        listRepository.registerOnFirestoreEventListenerForResult(this);
        listRepository.doGetListItems(shop.getLists().get(listIndex).getId(), shop.getShopId());
    }

    public MutableLiveData<Boolean> getLoadItems() {
        return loadItems;
    }

    //getting database result
    @Override
    public void onCallback(Task<QuerySnapshot> result) {
        if (result.isSuccessful())
        {
            //set up itemAdapter
            for (QueryDocumentSnapshot document : result.getResult()) {
                Item item = document.toObject(Item.class);
                item.setId(document.getId());
                item.setListId(shop.getLists().get(listIndex).getId());
                shop.getLists().get(listIndex).addItem(item);
            }
            if (result.getResult().isEmpty())
            {
                loadItems.setValue(false);
            }
            else
            {
                loadItems.setValue(true);
            }

            }
        else
        {
            loadItems.setValue(false);
        }
    }

}