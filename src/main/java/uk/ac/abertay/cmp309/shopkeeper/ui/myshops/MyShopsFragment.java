package uk.ac.abertay.cmp309.shopkeeper.ui.myshops;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;

import uk.ac.abertay.cmp309.shopkeeper.R;
import uk.ac.abertay.cmp309.shopkeeper.Shop;
import uk.ac.abertay.cmp309.shopkeeper.adapters.ShopAdapter;
import uk.ac.abertay.cmp309.shopkeeper.databinding.MyShopsFragmentBinding;

public class MyShopsFragment extends Fragment {

    private MyShopsViewModel mViewModel;

    private MyShopsFragmentBinding binding;

    private ShopAdapter shopAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(MyShopsViewModel.class);

        binding = MyShopsFragmentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //monitor changes on shops
        mViewModel.getLoadShops().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean)
                {
                    //hide no shop message
                    TextView noShopMessage = getActivity().findViewById(R.id.noShopsTV);
                    noShopMessage.setVisibility(View.GONE);

                    shopAdapter.notifyDataSetChanged();
                }
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //set up shop adapter
        shopAdapter = new ShopAdapter(getActivity(), mViewModel.getShops());
        ListView shopLV = binding.shopLV;

        //add header
        View shopTitle = getLayoutInflater().inflate(R.layout.shops_title_layout, null);
        shopLV.addHeaderView(shopTitle);

        shopLV.setAdapter(shopAdapter);

        Fragment fragment = this;

        //set click listener for shops to link to lists page for that shop
        shopLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //handle headers
                if (position >= shopLV.getHeaderViewsCount())
                {
                    Shop shop = (Shop) parent.getItemAtPosition(position);

                    //set up extras - shop details
                    Gson gson = new Gson();
                    String shopData = gson.toJson(shop);

                    Bundle shopDataBundle = new Bundle();
                    shopDataBundle.putString("shopDetails", shopData);

                    NavHostFragment.findNavController(fragment).navigate(R.id.action_navigation_my_shops_to_myLists, shopDataBundle);
                }
            }
        });

        //get shops
        mViewModel.getShopData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}