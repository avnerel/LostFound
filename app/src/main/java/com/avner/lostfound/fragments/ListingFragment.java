package com.avner.lostfound.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.avner.lostfound.Constants;
import com.avner.lostfound.R;
import com.avner.lostfound.activities.MainActivity;
import com.avner.lostfound.activities.MessagingActivity;
import com.avner.lostfound.activities.PossibleMatchesActivity;
import com.avner.lostfound.activities.ReportFormActivity;
import com.avner.lostfound.activities.ViewLocationActivity;
import com.avner.lostfound.adapters.LostFoundListAdapter;
import com.avner.lostfound.structs.Item;
import com.avner.lostfound.structs.ListFilter;
import com.avner.lostfound.utils.IUIUpdateInterface;
import com.avner.lostfound.utils.ListFilterUtils;
import com.avner.lostfound.utils.SignalSystem;
import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.software.shell.fab.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ListingFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener, IUIUpdateInterface {

    // WIDGETS
    private ListView lv_itemList;
    private Spinner sp_locationSpinner;
    private Spinner sp_timeSpinner;

    // INSTANCE VARIABLES
    private View rootView;
    private List<Item> allItems;
    private LostFoundListAdapter adapter;
    private boolean isLostFragment;
    private int myLayoutId;
    private String parseClassName; // used as parse queries identifier
    private ListFilter filters = new ListFilter();
    private MainActivity myMainActivity;

    // item info widgets
    private ImageButton ib_sendMessage;
    private ImageButton ib_showMap;
    private TextView tv_lossTime;
    private TextView tv_location;
    private TextView tv_descriptionContent;
    private ImageView iv_itemImage;
    private TextView tv_descriptionTitle;
    private boolean isPossibleMatchesFragment;
    private PossibleMatchesActivity myPossibleMatchesActivity;
    private ArrayList<String> possibleMatchesIds;
    private TextView tv_no_items_available;
    private int clickedPosition = -1;
    private Dialog clickedDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMembers();
        SignalSystem.getInstance().registerUIUpdateChange(this);
        setRetainInstance(true);
    }


    @Override
    public void onDestroy() {
        SignalSystem.getInstance().unRegisterUIUpdateChange(this);
        super.onDestroy();
    }

    /**
     * Initialize this fragment's instance variables, based on its configuration - Lost of Found listing.
     */
    private void initMembers() {
        if(getArguments() == null){
            this.isPossibleMatchesFragment = true;
            this.myPossibleMatchesActivity = (PossibleMatchesActivity) getActivity();
            this.possibleMatchesIds = (ArrayList<String>) myPossibleMatchesActivity.getIntent().getExtras().get(Constants.POSSIBLE_MATCHES);
        }else{

            this.isLostFragment = getArguments().getBoolean("isLostFragment");
            this.myMainActivity = (MainActivity) getActivity();
        }
        this.parseClassName = Constants.ParseObject.PARSE_LOST ;//this.isLostFragment ? Constants.ParseObject.PARSE_LOST : Constants.ParseObject.PARSE_FOUND;
        this.myLayoutId = R.layout.fragment_item_listing;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        initWidgets(inflater, container);
        initItemsList();
        initItemInfoWidgets(); // won't do anything if not shown
        getItemsFromParse();

        return rootView;
    }

    /**
     * Initialize visible widgets in fragment (add report button, filter spinners, listview).
     *
     * @param inflater
     * @param container
     */
    private void initWidgets(LayoutInflater inflater, ViewGroup container) {
        rootView = inflater.inflate(this.myLayoutId, container, false);
        lv_itemList = (ListView) rootView.findViewById(R.id.lv_myList);
        tv_no_items_available = (TextView) rootView.findViewById(R.id.tv_no_items_available);

        FloatingActionButton button = (FloatingActionButton) rootView.findViewById(R.id.b_add_item);

        if (!isPossibleMatchesFragment) {
            initTimeSpinner();
            initLocationSpinner();
            button.setOnClickListener(this);

            //remove filters and button for possible matches fragment.
        } else {
            button.setVisibility(View.INVISIBLE);
            this.sp_timeSpinner = (Spinner) rootView.findViewById(R.id.s_time_filter);
            this.sp_locationSpinner = (Spinner) rootView.findViewById(R.id.s_location_filter);
            TextView spinnerText = (TextView) rootView.findViewById(R.id.tv_filter_text);

            ViewGroup spinnersLayout = (ViewGroup) this.sp_timeSpinner.getParent();
            spinnersLayout.removeView(sp_timeSpinner);
            spinnersLayout.removeView(sp_locationSpinner);
            spinnersLayout.removeView(spinnerText);
        }
    }

    private void initItemInfoWidgets() {
        View item_info = this.rootView.findViewById(R.id.item_info);

        if (null == item_info) { // probably not in land + xlarge config
            return;
        }

        this.iv_itemImage = (ImageView) item_info.findViewById(R.id.iv_itemImage);
        this.tv_lossTime = (TextView) item_info.findViewById(R.id.tv_lossTime);
        this.tv_location = (TextView) item_info.findViewById(R.id.tv_location);
        this.tv_descriptionContent = (TextView) item_info.findViewById(R.id.tv_descriptionContent);
        this.ib_sendMessage = (ImageButton) item_info.findViewById(R.id.ib_sendMessage);
        this.ib_showMap = (ImageButton) item_info.findViewById(R.id.ib_showMap);
        this.tv_descriptionTitle = (TextView) item_info.findViewById(R.id.tv_descriptionTitle);
    }

    public void searchPhrase(Context ctx, String phrase, boolean forceUpdate) {
        if (this.filters.updateContentFilter(ctx, phrase, forceUpdate)) {
            ListFilterUtils.applyListFilters(allItems, adapter, filters, myMainActivity.getLastKnownLocation());
            Log.d(Constants.LOST_FOUND_TAG, "phrase filter updated to \"" + this.filters.getContentFilter() + "\"");
        }

        adapter.notifyDataSetInvalidated();
    }

    public void searchPhrase(Context ctx, String phrase) {
        searchPhrase(ctx, phrase, false);
    }

    /**
     * Initialize the location spinner widget, used for filtering the listings by location.
     */
    private void initLocationSpinner() {
        this.sp_locationSpinner = (Spinner) rootView.findViewById(R.id.s_location_filter);
        ArrayAdapter<CharSequence> locationSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.default_locations, android.R.layout.simple_spinner_item);

        this.sp_locationSpinner.setAdapter(locationSpinnerAdapter);
        this.sp_locationSpinner.setOnItemSelectedListener(this);
    }

    /**
     * Initialize the time spinner widget, used for filtering the listings by time.
     */
    private void initTimeSpinner() {
        this.sp_timeSpinner = (Spinner) rootView.findViewById(R.id.s_time_filter);
        ArrayAdapter<CharSequence> timeSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.default_time, android.R.layout.simple_spinner_item);

        this.sp_timeSpinner.setAdapter(timeSpinnerAdapter);
        this.sp_timeSpinner.setOnItemSelectedListener(this);
    }

    /**
     * Initialize the list-view widget holding all listings for display.
     */
    private void initItemsList() {
        this.allItems = new ArrayList<>();

        this.adapter = new LostFoundListAdapter(this.allItems, rootView, this);
//        Log.d(Constants.LOST_FOUND_TAG, "raw items list contains " + this.allItems.size() + " items. filtered items: " + this.itemsToDisplay.size());

        this.lv_itemList.setClickable(true);
        this.lv_itemList.setAdapter(this.adapter);
        this.lv_itemList.setOnItemClickListener(this.adapter);
    }

    /**
     * Get all raw listings for display from Parse. List received is un-filtered, sorted by time of
     * listing creation, and must be filtered using the {@link ListFilterUtils#applyListFilters(List, LostFoundListAdapter, ListFilter, android.location.Location)} method
     */
    private void getItemsFromParse() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(parseClassName);
        query.fromLocalDatastore();
        query.whereEqualTo(Constants.ParseReport.IS_ALIVE, true);
        query.orderByDescending(Constants.ParseReport.TIME);

        if (isPossibleMatchesFragment) {
            query.whereContainedIn(Constants.ParseQuery.OBJECT_ID, possibleMatchesIds);
        } else {
            query.whereEqualTo(Constants.ParseReport.IS_LOST, isLostFragment);
        }

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(final List<ParseObject> itemsList, com.parse.ParseException e) {
                if (e == null) {
                    allItems.clear();
                    for (int i = 0; i < itemsList.size(); i++) {
                        convertParseListToItemList(itemsList);
                    }
                    if (!isPossibleMatchesFragment) {
                        ListFilterUtils.applyListFilters(allItems, adapter, filters, ((MainActivity) getActivity()).getLastKnownLocation());
                    } else {
                        adapter.notifyDataSetInvalidated();
                    }

                    if (itemsList.isEmpty()) {
                        tv_no_items_available.setVisibility(View.VISIBLE);
                    } else {
                        tv_no_items_available.setVisibility(View.INVISIBLE);
                    }

//                    if(clickedPosition != -1) {
//                        lv_itemList.performItemClick(
//                                lv_itemList.getAdapter().getView(clickedPosition, null, null),
//                                clickedPosition,
//                                lv_itemList.getAdapter().getItemId(clickedPosition));
//                    }
                    Log.d(Constants.LOST_FOUND_TAG, "Fetched " + allItems.size() + " items from Parse");
                }
            }
        });

    }

    private void convertParseListToItemList(List<ParseObject> itemsList) {
        allItems.clear();

        for (ParseObject parseItem : itemsList){
            try {
                if (null != parseItem) {
                    allItems.add(new Item(parseItem));
                }
                else {
                    Log.d(Constants.LOST_FOUND_TAG, "parseItem was null");
                }
            } catch (NullPointerException e) {
                Log.d(Constants.LOST_FOUND_TAG, "caught NullPointerException when adding an item");
            }
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b_add_item:
                Intent intent = new Intent(rootView.getContext(), ReportFormActivity.class);
                intent.putExtra(Constants.ReportForm.IS_LOST_FORM, isLostFragment);
                intent.putExtra(Constants.ReportForm.IS_EDIT_FORM, false);
                startActivityForResult(intent,Constants.REQUEST_CODE_REPORT_FORM);
                break;

            default:
                Log.d(Constants.LOST_FOUND_TAG, "WTF?! some unknown onClick...");
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.s_location_filter:
                if (this.filters.updateDistanceFilter((String) parent.getItemAtPosition(position))) {
                    ListFilterUtils.applyListFilters(allItems, adapter, filters, ((MainActivity) getActivity()).getLastKnownLocation());
                    Log.d(Constants.LOST_FOUND_TAG, "distance filter updated to " + this.filters.getDistFilter() + " meter radius");
                }

                adapter.notifyDataSetInvalidated();
                break;

            case R.id.s_time_filter:
                if (this.filters.updateTimeFilter((String) parent.getItemAtPosition(position))) {
                    ListFilterUtils.applyListFilters(allItems, adapter, filters, ((MainActivity) getActivity()).getLastKnownLocation());
                    Log.d(Constants.LOST_FOUND_TAG, "time filter updated to " + (this.filters.getTimeFilter() / Constants.MILLI_SECONDS_PER_DAY) + " last days");
                }

                adapter.notifyDataSetInvalidated();
                break;

            default:
                Log.d(Constants.LOST_FOUND_TAG, "WTF?! some unknown selection...");
        }
    }


    public boolean setDisplayedItem(final Item item) {
        if (!showItemInfoWidgets()) {
            return false;
        }

        Picasso.with(myMainActivity).load(item.getImageUrl()).placeholder(R.drawable.image_unavailable).into(this.iv_itemImage);
        this.tv_lossTime.setText(item.getTimeAsString());
        this.tv_location.setText(item.getLocationString());
        this.tv_descriptionContent.setText(item.getDescription());

        initMapButton(item);
        initConversationButton(item);

        return true;
    }

    private void initConversationButton(final Item item) {
        // can't message myself.
        if (item.getUserId().equals(ParseUser.getCurrentUser().getObjectId())){
            ib_sendMessage.setVisibility(Button.INVISIBLE);
            return;
        }

        ib_sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(rootView.getContext(), MessagingActivity.class);
                intent.putExtra(Constants.Conversation.RECIPIENT_ID, item.getUserId());
                intent.putExtra(Constants.Conversation.RECIPIENT_NAME, item.getUserDisplayName());
                intent.putExtra(Constants.Conversation.ITEM_ID, item.getId());
                rootView.getContext().startActivity(intent);
            }
        });
    }

    private void initMapButton(Item item) {
        final Location location = item.getLocation();

        // no location specified.
        if (location == null) {
            ib_showMap.setVisibility(ImageButton.INVISIBLE);
            return;
        }

        this.ib_showMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(rootView.getContext(), ViewLocationActivity.class);
                intent.putExtra(Constants.LATITUDE, location.getLatitude());
                intent.putExtra(Constants.LONGITUDE, location.getLongitude());
                rootView.getContext().startActivity(intent);
            }
        });
    }

    private boolean showItemInfoWidgets() {
        if (null == this.rootView.findViewById(R.id.item_info)) {
            return false;
        }

        this.iv_itemImage.setVisibility(View.VISIBLE);
        this.ib_sendMessage.setVisibility(View.VISIBLE);
        this.ib_showMap.setVisibility(View.VISIBLE);
        this.tv_lossTime.setVisibility(View.VISIBLE);
        this.tv_location.setVisibility(View.VISIBLE);
        this.tv_descriptionContent.setVisibility(View.VISIBLE);
        this.tv_descriptionTitle.setVisibility(View.VISIBLE);

        return true;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    public void updateData() {
        getItemsFromParse();
    }

    @Override
    public void onDataChange(Constants.UIActions action, boolean bSuccess, Intent data) {
        switch (action){
            case uiaItemSaved:
                updateData();
                break;
        }
    }

    public String getSearchPhrase() {
        if (null != filters) {
            return filters.getContentFilter();
        }

        return null;
    }

    public void clearFilters() {
        filters = new ListFilter();
        this.sp_locationSpinner.setSelection(0);
        this.sp_timeSpinner.setSelection(0);
        ListFilterUtils.applyListFilters(allItems, adapter, filters, myMainActivity.getLastKnownLocation());
    }

    @Override
    public void onDestroyView() {
//        clearFilters();
        super.onDestroyView();
        this.clickedPosition = adapter.getClickedPosition();
        if(clickedDialog!=null){
            clickedDialog.dismiss();
        }
    }

    public void setClickedDialog(Dialog clickedDialog) {
        this.clickedDialog = clickedDialog;
    }

}
