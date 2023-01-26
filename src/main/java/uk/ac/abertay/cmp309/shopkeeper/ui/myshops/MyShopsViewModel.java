package uk.ac.abertay.cmp309.shopkeeper.ui.myshops;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import uk.ac.abertay.cmp309.shopkeeper.OnFirestoreEventListener;
import uk.ac.abertay.cmp309.shopkeeper.Shop;
import uk.ac.abertay.cmp309.shopkeeper.data.ShopRepository;

public class MyShopsViewModel extends ViewModel implements OnFirestoreEventListener<Task<QuerySnapshot>> {
    private ShopRepository shopRepository;
    private ArrayList<Shop> shops;

    private MutableLiveData<Boolean> loadShops;

    public MyShopsViewModel()
    {
        shopRepository = new ShopRepository();
        loadShops = new MutableLiveData<>();
        loadShops.setValue(false);
        shops = new ArrayList<>();
    }

    public MutableLiveData<Boolean> getLoadShops() {
        return loadShops;
    }

    public ArrayList<Shop> getShops() {
        return shops;
    }

    public void getShopData()
    {
        shops.clear();

        //use shop repository to get shop data
        shopRepository.registerOnFirestoreEventListenerForResult(this);
        shopRepository.doGetShopData();
    }

    //getting results back from database
    @Override
    public void onCallback(Task<QuerySnapshot> result) {
        if (result.isSuccessful())
        {
            //set up shopAdapter
            for (QueryDocumentSnapshot document : result.getResult()) {
                Shop shop = document.toObject(Shop.class);
                shops.add(shop);
            }

            if (result.getResult().isEmpty())
            {
                loadShops.setValue(false);
            }
            else
            {
                loadShops.setValue(true);
            }

        }
        else
        {
            loadShops.setValue(false);
        }
    }
}