package uk.ac.abertay.cmp309.shopkeeper;

//interface to listen for when a list view is empty
public interface OnEmptyListView<T> {
    void onCallback(T result);
}
