package uk.ac.abertay.cmp309.shopkeeper.ui.shopfavourites;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import uk.ac.abertay.cmp309.shopkeeper.Item;
import uk.ac.abertay.cmp309.shopkeeper.OnFirestoreEventListener;
import uk.ac.abertay.cmp309.shopkeeper.Shop;
import uk.ac.abertay.cmp309.shopkeeper.data.ShopRepository;

public class ShopFavouritesViewModel extends ViewModel implements OnFirestoreEventListener<Task<QuerySnapshot>> {

    private ShopRepository shopRepository;
    private Shop shop;
    private MutableLiveData<Boolean> loadFavourites;
    private int listIndex;

    public ShopFavouritesViewModel()
    {
        shopRepository = new ShopRepository();
        loadFavourites = new MutableLiveData<>();
        loadFavourites.setValue(false);
    }

    public MutableLiveData<Boolean> getLoadFavourites() {
        return loadFavourites;
    }

    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

    public int getListIndex() {
        return listIndex;
    }

    public void setListIndex(int listIndex) {
        this.listIndex = listIndex;
    }

    public void getFavourites()
    {
        shopRepository.registerOnFirestoreEventListenerForResult(this);
        shopRepository.doGetShopFavourites(shop.getShopId());
    }

    //getting results back from database
    @Override
    public void onCallback(Task<QuerySnapshot> result) {
        if (result.isSuccessful())
        {
            for (QueryDocumentSnapshot documentSnapshots : result.getResult())
            {
                Item item = documentSnapshots.toObject(Item.class);
                item.setId(documentSnapshots.getId());
                shop.getFavourites().addItem(item);
            }

            if (result.getResult().isEmpty())
            {
                loadFavourites.setValue(false);
            }
            else
            {
                loadFavourites.setValue(true);
            }

        }
        else
        {
            loadFavourites.setValue(false);
        }
    }
}