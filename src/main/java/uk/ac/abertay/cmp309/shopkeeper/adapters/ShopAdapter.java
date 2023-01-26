package uk.ac.abertay.cmp309.shopkeeper.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import uk.ac.abertay.cmp309.shopkeeper.R;
import uk.ac.abertay.cmp309.shopkeeper.Shop;

public class ShopAdapter extends ArrayAdapter<Shop> {
    private final ArrayList<Shop> shops;

    public ShopAdapter(Context context, ArrayList<Shop> shops)
    {
        super(context, 0, shops);
        this.shops = shops;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        /* Get the item data for this position. */
        Shop i = getItem(position);

        /* Check if an existing view is being reused, otherwise inflate the view. */
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.display_shop_details_layout, parent, false);
        }

        /* Lookup views. */
        TextView shopNameTV = (TextView) convertView.findViewById(R.id.shopNameTV);

        /* Add the data to the template view. */
        shopNameTV.setText(i.getName());

        /* Return the completed view to render on screen. */
        return convertView;
    }
}
