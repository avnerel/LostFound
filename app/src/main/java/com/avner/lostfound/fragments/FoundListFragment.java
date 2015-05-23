package com.avner.lostfound.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.avner.lostfound.Constants;
import com.avner.lostfound.R;
import com.avner.lostfound.activities.ReportFormActivity;
import com.avner.lostfound.adapters.LostFoundListAdapter;
import com.avner.lostfound.structs.Item;
import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.software.shell.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class FoundListFragment extends Fragment implements View.OnClickListener {

    private ListView lv_itemList;

    private View rootView;
    private List<Item> items;
    private LostFoundListAdapter adapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_found_list, container, false);

        lv_itemList = (ListView) rootView.findViewById(R.id.lv_myList);

        FloatingActionButton button = (FloatingActionButton) rootView.findViewById(R.id.b_add_item);
        button.setOnClickListener(this);

        initItemsList();


        Spinner timeSpinner = (Spinner) rootView.findViewById(R.id.s_time_filter);
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> timeSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.default_time, android.R.layout.simple_spinner_item);
// Apply the adapter to the spinner
        timeSpinner.setAdapter(timeSpinnerAdapter);

        Spinner locationSpinner = (Spinner) rootView.findViewById(R.id.s_location_filter);
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> locationSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.default_locations, android.R.layout.simple_spinner_item);
// Apply the adapter to the spinner
        locationSpinner.setAdapter(locationSpinnerAdapter);


        return rootView;
    }

    private void initItemsList() {

        items = new ArrayList<>();
        adapter = new LostFoundListAdapter(items,rootView);
        lv_itemList.setClickable(true);
        lv_itemList.setAdapter(adapter);
        lv_itemList.setOnItemClickListener(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        getItemsFromParse();
    }

    private void getItemsFromParse() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.ParseObject.PARSE_FOUND);
        query.orderByAscending(Constants.ParseQuery.CREATED_AT);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> itemsList, com.parse.ParseException e) {
                if (e == null) {
                    for (int i = 0; i < itemsList.size(); i++) {
                        convertParseListToItemList(itemsList,items);
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }


    private void convertParseListToItemList(List<ParseObject> itemsList, List<Item> items) {

        //TODO if not loading all items and just adding so remove this.
        items.clear();

        for(ParseObject parseItem: itemsList){

           Item item = new Item(parseItem);
            items.add(item);
        }

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.b_add_item){

            Intent intent = new Intent(rootView.getContext(),ReportFormActivity.class);
            intent.putExtra(Constants.ReportForm.IS_LOST_FORM, false);
            intent.putExtra(Constants.ReportForm.IS_EDIT_FORM, false);
            startActivity(intent);
        }
    }

}



