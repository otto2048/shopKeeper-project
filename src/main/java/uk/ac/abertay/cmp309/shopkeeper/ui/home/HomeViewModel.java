package uk.ac.abertay.cmp309.shopkeeper.ui.home;

import android.util.Pair;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import uk.ac.abertay.cmp309.shopkeeper.OnFirestoreEventListener;
import uk.ac.abertay.cmp309.shopkeeper.Shop;
import uk.ac.abertay.cmp309.shopkeeper.data.ShopRepository;

public class HomeViewModel extends ViewModel implements OnFirestoreEventListener<Task<QuerySnapshot>> {

    private MutableLiveData<Boolean> drawMarkers;
    private ArrayList<Pair<Shop, Circle>> shopsWithMarkers;
    private ShopRepository shopRepository;

    public HomeViewModel() {
        drawMarkers = new MutableLiveData<>();
        drawMarkers.setValue(null);

        shopRepository = new ShopRepository();
        shopsWithMarkers = new ArrayList<Pair<Shop, Circle>>();
    }

    public ArrayList<Pair<Shop, Circle>> getShopsWithMarkers() {
        return shopsWithMarkers;
    }

    public void setCircleForShop(int index, Circle circle)
    {
        shopsWithMarkers.set(index, new Pair<>(shopsWithMarkers.get(index).first, circle));
    }

    public MutableLiveData<Boolean> getDrawMarkers() {
        return drawMarkers;
    }

    public void getShopData()
    {
        if (shopsWithMarkers.isEmpty())
        {
            shopRepository.registerOnFirestoreEventListenerForResult(this);
            shopRepository.doGetShopData();
        }
    }

    //set all circle values to null
    public void resetCircles()
    {
        for (int i = 0; i<shopsWithMarkers.size(); i++)
        {
            shopsWithMarkers.set(i, new Pair<>(shopsWithMarkers.get(i).first, null));
        }
    }

    //get result from loading shops
    @Override
    public void onCallback(Task<QuerySnapshot> result) {
        if (result.isSuccessful())
        {
            for (QueryDocumentSnapshot r : result.getResult())
            {
                shopsWithMarkers.add(new Pair<>(r.toObject(Shop.class), null));
            }

            drawMarkers.setValue(true);
        }
        else
        {
            drawMarkers.setValue(false);
        }
    }
}