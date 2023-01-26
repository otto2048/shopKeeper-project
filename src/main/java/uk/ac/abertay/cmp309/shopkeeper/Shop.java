package uk.ac.abertay.cmp309.shopkeeper;

import androidx.navigation.NavType;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;

public class Shop {

    private String name;

    //id of the POI that this object is storing
    private String shopId;

    private GeoPoint location;

    //a list of items that the user has set as favourite at this shop
    private List favourites;

    //a collection of lists the user has made for this shop
    private ArrayList<List> lists;

    //whether this shop contains any lists
    private boolean hasLists;

    public Shop()
    {
        lists = new ArrayList<>();
        favourites = new List();
    }

    public Shop(String n, String i, GeoPoint l)
    {
        name = n;
        shopId = i;
        location = l;

        lists = new ArrayList<>();
        favourites = new List();
    }

    public String getName() {
        return name;
    }

    public String getShopId()
    {
        return shopId;
    }

    public GeoPoint getLocation() {
        return location;
    }

    @Exclude
    public ArrayList<List> getLists() {
        return lists;
    }

    @Exclude
    public List getFavourites() {
        return favourites;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public void addList(List list)
    {
        lists.add(list);
    }

    public void addItemToList(int listIndex, Item item)
    {
        lists.get(listIndex).addItem(item);
    }

    public void setLists(ArrayList<List> lists) {
        this.lists = lists;
    }

    public boolean getHasLists() {
        return hasLists;
    }

    public void setHasLists(boolean hasLists) {
        this.hasLists = hasLists;
    }
}
