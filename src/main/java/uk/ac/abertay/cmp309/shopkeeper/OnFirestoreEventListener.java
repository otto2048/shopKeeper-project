package uk.ac.abertay.cmp309.shopkeeper;

//interface to listen for firestore callbacks
public interface OnFirestoreEventListener<T> {
    void onCallback(T result);
}
