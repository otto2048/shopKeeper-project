package uk.ac.abertay.cmp309.shopkeeper;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.Objects;

public class Item {
    private String name;
    private String notes;
    private Float price;
    private String id;
    private Boolean isFavourite;
    private String listId;
    private ArrayList<String> listReferences;

    public Item(String n, String n_, Float p)
    {
        name = n;
        notes = n_;
        price = p;
    }

    public Item()
    {

    }

    public String getName() {
        return name;
    }

    public String getNotes() {
        return notes;
    }

    public Float getPrice() {
        return price;
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public String getListId() {
        return listId;
    }

    public ArrayList<String> getListReferences() {
        return listReferences;
    }

    public Boolean getIsFavourite() {
        return isFavourite;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setIsFavourite(Boolean favourite) {
        isFavourite = favourite;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public void setListReferences(ArrayList<String> listReferences) {
        this.listReferences = listReferences;
    }

    //override equals to determine if items exist in a container of items
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return name.equals(item.name) && Objects.equals(notes, item.notes) && Objects.equals(price, item.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, notes, price);
    }
}
