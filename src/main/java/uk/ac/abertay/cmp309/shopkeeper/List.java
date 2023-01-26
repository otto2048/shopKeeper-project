package uk.ac.abertay.cmp309.shopkeeper;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;

public class List {
    private String id;

    private String name;

    private String notes;

    private Timestamp lastModified;

    //a collection of items that make up this list
    private ArrayList<Item> items;

    public List()
    {
        items = new ArrayList<>();
    }

    public List(String n, String n_)
    {
        name = n;
        notes = n_;

        items = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getNotes() {
        return notes;
    }

    public Timestamp getLastModified() {
        return lastModified;
    }

    @Exclude
    public ArrayList<Item> getItems() {
        return items;
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setItems(ArrayList<Item> items) {
        this.items = items;
    }

    public void addItem(Item item)
    {
        items.add(item);
    }

    public void removeItem(Item item){items.remove(item);}

    public void setId(String id) {
        this.id = id;
    }

    public void setLastModified(Timestamp lastModified) {
        this.lastModified = lastModified;
    }
}
