package uk.ac.abertay.cmp309.shopkeeper;

import android.util.Pair;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

public class Validation {
    private final int LIST_NAME_LENGTH = 50;
    private final int LIST_NOTES_LENGTH = 500;

    private final int ITEM_NAME_LENGTH = 50;
    private final int ITEM_NOTES_LENGTH = 250;

    //source: https://stackoverflow.com/questions/17804887/java-validate-price-with-comma-or-dot-and-two-decimal-value
    final String priceRegularExpression = "[0-9]+([,.][0-9]{1,2})?";

    private final Gson gson;

    public Validation() {
        gson = new Gson();
    }

    //validate an item
    //if item is valid, return null
    //else return a json encoded array of errors
    public String validateItemDetails(Item item)
    {
        ArrayList<String> errorMessages = new ArrayList<>();

        if (!validateStringLength(ITEM_NAME_LENGTH, item.getName()))
        {
            errorMessages.add("Invalid item name!");
        }
        else if (StringUtils.isEmpty(item.getName()))
        {
            errorMessages.add("An item name is required");
        }

        if (StringUtils.isNotEmpty(item.getNotes()))
        {
            if (!validateStringLength(ITEM_NOTES_LENGTH, item.getNotes()))
            {
                errorMessages.add("Invalid item notes!");
            }
        }

        if (item.getPrice() != null) {
            if (!validateItemPrice(item.getPrice())) {
                errorMessages.add("Invalid item price!");
            }
        }

        if (errorMessages.isEmpty())
        {
            return null;
        }

        return gson.toJson(errorMessages);
    }

    //clearing duplicates away from a list
    //if no duplicates were found, return null
    //else return a json encoded array of errors
    public String removeListDuplicates(ArrayList<Item> items)
    {
        ArrayList<Pair<String, Item>> errorMessages = new ArrayList<>();
        ArrayList<Item> newList = new ArrayList<>();

        for (Item i : items)
        {
            if (!newList.contains(i))
            {
                newList.add(i);
            }
            else
            {
                errorMessages.add(new Pair<>("Failed to add item, " + i.getName() + " is already in the list!", i));
            }
        }

        if (errorMessages.isEmpty())
        {
            return null;
        }

        return gson.toJson(errorMessages);
    }


    //validating list details
    //if list is valid, return null
    //else return a json encoded array of errors
    public String validateListDetails(String listJSON)
    {
        List list = gson.fromJson(listJSON, List.class);

        ArrayList<String> errorMessages = new ArrayList<>();

        if (!validateStringLength(LIST_NAME_LENGTH, list.getName()))
        {
            errorMessages.add("Invalid list name!");
        }
        else if (StringUtils.isEmpty(list.getName()))
        {
            errorMessages.add("A list name is required");
        }

        if (StringUtils.isNotEmpty(list.getNotes())) {
            if (!validateStringLength(LIST_NOTES_LENGTH, list.getNotes()))
            {
                errorMessages.add("Invalid list notes!");
            }
        }

        if (list.getItems().isEmpty())
        {
            errorMessages.add("List must contain some items!");
        }

        if (errorMessages.isEmpty())
        {
            return null;
        }

        return gson.toJson(errorMessages);
    }

    //validate item price
    private boolean validateItemPrice(Float price)
    {
        //check if input is a whole number
        if (price % 1 == 0)
        {
            return true;
        }

        String priceString = price.toString();
        
        return priceString.matches(priceRegularExpression);
    }

    //validate a string's length
    public boolean validateStringLength(int maxLen, String input)
    {
        if (input.length() > maxLen)
        {
            return false;
        }

        return true;
    }
}
