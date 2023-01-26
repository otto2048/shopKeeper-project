package uk.ac.abertay.cmp309.shopkeeper.data;

import android.util.Log;
import android.util.Pair;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.google.firestore.v1.Write;
import com.google.gson.Gson;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import uk.ac.abertay.cmp309.shopkeeper.Item;
import uk.ac.abertay.cmp309.shopkeeper.List;
import uk.ac.abertay.cmp309.shopkeeper.OnFirestoreEventListener;
import uk.ac.abertay.cmp309.shopkeeper.R;
import uk.ac.abertay.cmp309.shopkeeper.Shop;

public class ListRepository {
    private FirebaseFirestore listRemoteDataSource;
    private final Gson gson;

    private FirebaseUser user;

    private OnFirestoreEventListener<Boolean> booleanListener;
    private OnFirestoreEventListener<Task<QuerySnapshot>> resultListener;
    private OnFirestoreEventListener<String> stringListener;

    //constants for firebase collections and fields
    private final String SHOPS_COLLECTION = "shops";
    private final String USERS_COLLECTION = "users";
    private final String LISTS_COLLECTION = "lists";
    private final String ITEMS_COLLECTION = "items";
    private final String FAVOURITES_COLLECITON = "favourites";
    private final String LIST_REFERENCES_FIELD = "listReferences";
    private final String LAST_MODIFIED_FIELD = "lastModified";
    private final String NAME_FIELD = "name";
    private final String NOTES_FIELD = "notes";

    public ListRepository()
    {
        listRemoteDataSource = FirebaseFirestore.getInstance();
        gson = new Gson();

        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    //listeners for returning results
    public void registerOnFirestoreEventListenerForBoolean(OnFirestoreEventListener<Boolean> mListener)
    {
        this.booleanListener = mListener;
    }

    public void registerOnFirestoreEventListenerForResult(OnFirestoreEventListener<Task<QuerySnapshot>> listener)
    {
        this.resultListener = listener;
    }

    public void registerOnFirestoreEventListenerForString(OnFirestoreEventListener<String> createListener)
    {
        this.stringListener = createListener;
    }

    //create a new List/update a list
    public void doCreateAndUpdateList(String shopJSON, int listIndex, boolean isOnline, ArrayList<Item> itemsToBeRemoved)
    {
        //get shop object
        Shop shop = gson.fromJson(shopJSON, Shop.class);

        //send data to Firestore
        WriteBatch batch = listRemoteDataSource.batch();

        //create record for this shop if it doesn't exist
        DocumentReference shopRef = listRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION).document(shop.getShopId());
        batch.set(shopRef, shop);

        DocumentReference listRef;
        //if list id exists
        if (shop.getLists().get(listIndex).getId() != null)
        {
            //updating a list
            listRef = listRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION).document(shop.getShopId())
                    .collection(LISTS_COLLECTION).document(shop.getLists().get(listIndex).getId());
            shop.getLists().get(listIndex).setLastModified(Timestamp.now());
            batch.update(listRef, NAME_FIELD, shop.getLists().get(listIndex).getName(), NOTES_FIELD, shop.getLists().get(listIndex).getNotes(),
                    LAST_MODIFIED_FIELD, shop.getLists().get(listIndex).getLastModified());
        }
        else
        {
            //creating a list
            //create new list document for this shop
            listRef = listRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION).document(shop.getShopId()).collection(LISTS_COLLECTION).document();
            shop.getLists().get(listIndex).setLastModified(Timestamp.now());
            batch.set(listRef, shop.getLists().get(listIndex));
        }

        //create items collection for new list document
        //add items to list's items collection
        for (int i = 0; i < shop.getLists().get(listIndex).getItems().size(); i++)
        {
            DocumentReference itemRef;
            if (shop.getLists().get(listIndex).getItems().get(i).getId() == null)
            {
                itemRef = listRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION).document(shop.getShopId())
                        .collection(LISTS_COLLECTION).document(listRef.getId()).collection(ITEMS_COLLECTION).document();
            }
            else
            {
                itemRef = listRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION).document(shop.getShopId())
                        .collection(LISTS_COLLECTION).document(listRef.getId()).collection(ITEMS_COLLECTION).document(
                                shop.getLists().get(listIndex).getItems().get(i).getId()
                        );
            }
            batch.set(itemRef, shop.getLists().get(listIndex).getItems().get(i));

            //check if this item is a favourite
            if (shop.getLists().get(listIndex).getItems().get(i).getIsFavourite() != null)
            {
                if (shop.getLists().get(listIndex).getItems().get(i).getIsFavourite())
                {
                    //add new list reference to the list references in the favourite
                    DocumentReference favourite = listRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION)
                            .document(shop.getShopId()).collection(FAVOURITES_COLLECITON).document(shop.getLists().get(listIndex).getItems().get(i).getId());
                    batch.update(favourite, LIST_REFERENCES_FIELD, FieldValue.arrayUnion(listRef.getId()));
                }
            }
        }

        //if this list is being updated, remove any items to be removed
        if (shop.getLists().get(listIndex).getId() != null && !itemsToBeRemoved.isEmpty())
        {
            for (Item item : itemsToBeRemoved)
            {
                //get item
                DocumentReference i = listRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid())
                        .collection(SHOPS_COLLECTION).document(shop.getShopId())
                        .collection(LISTS_COLLECTION).document(shop.getLists().get(listIndex).getId())
                        .collection(ITEMS_COLLECTION).document(item.getId());

                batch.delete(i);

                if (item.getIsFavourite() != null) {
                    if (item.getIsFavourite()) {
                        //remove the item from the list references in the favourite
                        DocumentReference favourite = listRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION).
                                document(shop.getShopId()).collection(FAVOURITES_COLLECITON).document(item.getId());

                        batch.update(favourite, LIST_REFERENCES_FIELD, FieldValue.arrayRemove(shop.getLists().get(listIndex).getId()));
                    }
                }
            }
        }

        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                {
                    stringListener.onCallback(listRef.getId());
                }
                else
                {
                    stringListener.onCallback(null);
                }
            }
        });

        //add snapshot listener to shop reference to notify the user if there are pending writes (offline mode)
        shopRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (value != null)
                {
                    if (value.getMetadata().hasPendingWrites() && !isOnline) {
                        Log.i("shopKeeper", "using pending");
                        stringListener.onCallback(listRef.getId());
                    }
                }
            }
        });
    }

    //get list items
    public void doGetListItems(String listId, String shopId)
    {
        //get list items
        CollectionReference items = listRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION)
                .document(shopId).collection(LISTS_COLLECTION).document(listId).collection(ITEMS_COLLECTION);

        items.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                resultListener.onCallback(task);
            }
        });

    }
}

