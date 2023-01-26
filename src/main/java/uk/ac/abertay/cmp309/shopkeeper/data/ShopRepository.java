package uk.ac.abertay.cmp309.shopkeeper.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;

import uk.ac.abertay.cmp309.shopkeeper.Item;
import uk.ac.abertay.cmp309.shopkeeper.OnFirestoreEventListener;

public class ShopRepository {
    private FirebaseFirestore shopRemoteDataSource;

    private FirebaseUser user;

    private OnFirestoreEventListener<Task<QuerySnapshot>> resultListener;
    private OnFirestoreEventListener<Boolean> booleanListener;

    //constants for firebase collections and fields
    private final String SHOPS_COLLECTION = "shops";
    private final String USERS_COLLECTION = "users";
    private final String LISTS_COLLECTION = "lists";
    private final String ITEMS_COLLECTION = "items";
    private final String FAVOURITES_COLLECITON = "favourites";
    private final String LIST_REFERENCES_FIELD = "listReferences";
    private final String LAST_MODIFIED_FIELD = "lastModified";
    private final String HAS_LISTS_FIELD = "hasLists";
    private final String IS_FAVOURITE_FIELD = "isFavourite";

    public ShopRepository()
    {
        shopRemoteDataSource = FirebaseFirestore.getInstance();

        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    //listeners for returning results
    public void registerOnFirestoreEventListenerForResult(OnFirestoreEventListener mListener)
    {
        this.resultListener = mListener;
    }

    public void registerOnFirestoreEventListenerForBoolean(OnFirestoreEventListener<Boolean> bListener)
    {
        this.booleanListener = bListener;
    }

    //get all the shops that this user has lists on
    public void doGetShopData()
    {
        shopRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION)
                .whereEqualTo(HAS_LISTS_FIELD, true)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        resultListener.onCallback(task);
                    }
                });
    }

    //get all the lists for a shop
    public void doGetShopLists(String shopId)
    {
        CollectionReference lists = shopRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION).document(shopId)
                .collection(LISTS_COLLECTION);

        lists.orderBy(LAST_MODIFIED_FIELD, Query.Direction.DESCENDING).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                resultListener.onCallback(task);
            }
        });
    }

    //get the favourites for a shop
    public void doGetShopFavourites(String shopId)
    {
        CollectionReference favourites = shopRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION).document(shopId).collection(FAVOURITES_COLLECITON);

        favourites.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                resultListener.onCallback(task);
            }
        });
    }

    //add a favourite for a shop
    public void doAddFavourite(String shopId, Item item, Boolean isOnline)
    {
        WriteBatch batch = shopRemoteDataSource.batch();

        //create favourite record
        DocumentReference favourite = shopRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION).document(shopId).collection(FAVOURITES_COLLECITON).document(item.getId());
        batch.set(favourite, item, SetOptions.merge());

        //add list id to collection in the favourites
        batch.update(favourite, LIST_REFERENCES_FIELD, FieldValue.arrayUnion(item.getListId()));

        //update item to be tagged as a favourite
        DocumentReference itemRef = shopRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION).document(shopId)
                .collection(LISTS_COLLECTION).document(item.getListId()).collection(ITEMS_COLLECTION).document(item.getId());
        batch.update(itemRef, IS_FAVOURITE_FIELD, true);

        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                booleanListener.onCallback(task.isSuccessful());
            }
        });

        //add snapshot listener to favourite reference to notify the user if there are pending writes (offline mode)
        favourite.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (value != null)
                {
                    if (value.getMetadata().hasPendingWrites() && !isOnline) {
                        booleanListener.onCallback(true);
                    }
                }
            }
        });
    }

    //remove a favourite for a shop
    public void doRemoveFavourite(String shopId, Item item)
    {
        //get favourite
        DocumentReference favourite = shopRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid())
                .collection(SHOPS_COLLECTION).document(shopId).collection(FAVOURITES_COLLECITON).document(item.getId());

        shopRemoteDataSource.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                //for all list references in the favourites list references collection, set the items isFavourite tag to false
                DocumentSnapshot snapshot = transaction.get(favourite);

                Item favouriteItem = snapshot.toObject(Item.class);

                for (String list : favouriteItem.getListReferences())
                {
                    DocumentReference itemRef = shopRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION)
                            .document(shopId).collection(LISTS_COLLECTION)
                            .document(list).collection(ITEMS_COLLECTION).document(snapshot.getId());

                    transaction.update(itemRef, IS_FAVOURITE_FIELD, false);
                }

                //delete the favourite
                transaction.delete(favourite);

                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                booleanListener.onCallback(task.isSuccessful());
            }
        });
    }

    //delete a list from a shop
    public void doDeleteShopList(String shopId, String listId)
    {
        //get list items
        CollectionReference items = shopRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION).document(shopId)
                .collection(LISTS_COLLECTION).document(listId).collection(ITEMS_COLLECTION);

        CollectionReference lists = shopRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid())
                .collection(SHOPS_COLLECTION).document(shopId).collection(LISTS_COLLECTION);

        DocumentReference listRef = shopRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION).document(shopId)
                .collection(LISTS_COLLECTION).document(listId);

        items.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        shopRemoteDataSource.runTransaction(new Transaction.Function<Void>() {
                            @Nullable
                            @Override
                            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                                //for all items in the list, check if its a favourite, then delete it
                                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                                    Item item = documentSnapshot.toObject(Item.class);
                                    item.setId(documentSnapshot.getId());

                                    if (item.getIsFavourite() != null)
                                    {
                                        if (item.getIsFavourite())
                                        {
                                            //remove list reference from favourites
                                            DocumentReference favouriteRef = shopRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION)
                                                    .document(shopId).collection(FAVOURITES_COLLECITON).document(item.getId());

                                            transaction.update(favouriteRef, LIST_REFERENCES_FIELD, FieldValue.arrayRemove(listId));
                                        }
                                    }

                                    //delete the item
                                    transaction.delete(documentSnapshot.getReference());
                                }

                                //delete the list
                                transaction.delete(listRef);

                                return null;
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //if this was successful, check if the lists collection is now empty
                                if (task.isSuccessful())
                                {
                                    lists.get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> checkLastList) {
                                                    if (checkLastList.isSuccessful())
                                                    {
                                                        if (checkLastList.getResult().isEmpty())
                                                        {
                                                            //set shop "hasLists" to false
                                                            DocumentReference shopRef = shopRemoteDataSource.collection(USERS_COLLECTION).document(user.getUid()).collection(SHOPS_COLLECTION).document(shopId);
                                                            shopRef.update(HAS_LISTS_FIELD, false).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> updateShop) {
                                                                    booleanListener.onCallback(updateShop.isSuccessful());
                                                                }
                                                            });
                                                        }
                                                        else {
                                                            booleanListener.onCallback(task.isSuccessful());
                                                        }
                                                    }
                                                    else {
                                                        booleanListener.onCallback(task.isSuccessful());
                                                    }
                                                }
                                            });
                                }
                                else
                                {
                                    booleanListener.onCallback(task.isSuccessful());
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        booleanListener.onCallback(false);
                    }
                });
    }
}
