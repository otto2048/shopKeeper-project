package uk.ac.abertay.cmp309.shopkeeper.adapters;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

import uk.ac.abertay.cmp309.shopkeeper.List;
import uk.ac.abertay.cmp309.shopkeeper.OnEmptyListView;
import uk.ac.abertay.cmp309.shopkeeper.OnFirestoreEventListener;
import uk.ac.abertay.cmp309.shopkeeper.R;
import uk.ac.abertay.cmp309.shopkeeper.data.ShopRepository;

public class ListAdapter extends ArrayAdapter<List> implements OnFirestoreEventListener<Boolean> {
    private final ArrayList<List> lists;
    private final ShopRepository shopRepository;
    private String shopId;
    private int listToRemoveIndex;

    private OnEmptyListView<Boolean> emptyListView;

    private boolean hasInternetConnection;

    public ListAdapter(Context context, ArrayList<List> lists, String shopId, boolean hasInternetConnection)
    {
        super(context, 0, lists);
        this.lists = lists;

        shopRepository = new ShopRepository();

        this.hasInternetConnection = hasInternetConnection;

        this.shopId = shopId;
    }

    public void registerOnEmptyListViewListener(OnEmptyListView emptyListView)
    {
        this.emptyListView = emptyListView;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        List l = getItem(position);

        /* Check if an existing view is being reused, otherwise inflate the view. */
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.display_list_details_layout, parent, false);
        }

        /* Lookup views. */
        TextView listNameTV = (TextView) convertView.findViewById(R.id.listNameTV);
        TextView listNotesTV = (TextView) convertView.findViewById(R.id.listNotesTV);

        Button deleteBtn = (Button) convertView.findViewById(R.id.deleteListBtn);

        /* Add the data to the template view. */

        if (StringUtils.isNotEmpty(l.getName()))
        {
            listNameTV.setText(l.getName());
            listNameTV.setVisibility(View.VISIBLE);
        }
        else
        {
            listNameTV.setVisibility(View.GONE);
        }

        if (StringUtils.isNotEmpty(l.getNotes()))
        {
            listNotesTV.setText(l.getNotes());
            listNotesTV.setVisibility(View.VISIBLE);
        }
        else
        {
            listNotesTV.setVisibility(View.GONE);
        }

        deleteBtn.setVisibility(View.VISIBLE);

        //set up delete button event listener
        shopRepository.registerOnFirestoreEventListenerForBoolean(this);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listToRemoveIndex = position;
                //remove list
                shopRepository.doDeleteShopList(shopId, l.getId());
            }
        });

        /* Return the completed view to render on screen. */
        return convertView;
    }

    //callback for when firestore operations are done
    @Override
    public void onCallback(Boolean result) {
        if (result)
        {
            lists.remove(listToRemoveIndex);

            if (lists.isEmpty())
            {
                emptyListView.onCallback(true);
            }

            this.notifyDataSetChanged();
        }
        else
        {
            Toast.makeText(getContext(), R.string.failed_to_remove_list_message, Toast.LENGTH_SHORT).show();
        }
    }
}
