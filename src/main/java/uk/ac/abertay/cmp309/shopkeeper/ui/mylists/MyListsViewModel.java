package uk.ac.abertay.cmp309.shopkeeper.ui.mylists;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import uk.ac.abertay.cmp309.shopkeeper.List;
import uk.ac.abertay.cmp309.shopkeeper.OnFirestoreEventListener;
import uk.ac.abertay.cmp309.shopkeeper.Shop;
import uk.ac.abertay.cmp309.shopkeeper.data.ShopRepository;

public class MyListsViewModel extends ViewModel implements OnFirestoreEventListener<Task<QuerySnapshot>> {

    private Shop shop;
    private MutableLiveData<Boolean> loadLists;
    private ShopRepository shopRepository;

    public MyListsViewModel()
    {
        shopRepository = new ShopRepository();
        loadLists = new MutableLiveData<>();
    }

    public MutableLiveData<Boolean> getLoadLists() {
        return loadLists;
    }

    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

    public void getListData()
    {
        //use shop repository to get shop lists
        shopRepository.registerOnFirestoreEventListenerForResult(this);
        shopRepository.doGetShopLists(shop.getShopId());
    }

    //getting result back from database
    @Override
    public void onCallback(Task<QuerySnapshot> result) {
        if (result.isSuccessful())
        {
            //set up listAdapter
            for (QueryDocumentSnapshot document : result.getResult()) {
                List list = document.toObject(List.class);
                list.setId(document.getId());
                shop.addList(list);
            }

            if (result.getResult().isEmpty())
            {
                loadLists.setValue(false);
            }
            else
            {
                loadLists.setValue(true);
            }

        }
        else
        {
            loadLists.setValue(false);
        }
    }
}