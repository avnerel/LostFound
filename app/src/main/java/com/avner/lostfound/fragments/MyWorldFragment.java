package com.avner.lostfound.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.avner.lostfound.Constants;
import com.avner.lostfound.R;
import com.avner.lostfound.activities.MainActivity;
import com.avner.lostfound.activities.ReportFormActivity;
import com.avner.lostfound.activities.ViewLocationActivity;
import com.avner.lostfound.adapters.OpenItemsAdapter;
import com.avner.lostfound.structs.Item;
import com.avner.lostfound.utils.IUIUpdateInterface;
import com.avner.lostfound.utils.ImageUtils;
import com.avner.lostfound.utils.SignalSystem;
import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.software.shell.fab.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyWorldFragment extends Fragment implements IUIUpdateInterface, View.OnClickListener {

    private MainActivity myActivity;
    private View rootView;
    private TextView tv_openListingsNumber;
    private OpenItemsAdapter myOpenListingsAdapter;
    private List<Item> items;
    private ShareActionProvider shareActionProvider;
    private ActionMode actionMode;

    private ListView lv_openListings;
    // item info widgets
    private ImageButton ib_showMap;
    private TextView tv_lossTime;
    private TextView tv_location;
    private TextView tv_descriptionContent;
    private ImageView iv_itemImage;
    private TextView tv_descriptionTitle;
    private int clickedPosition = -1;
    private Dialog clickedDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.myActivity = (MainActivity) getActivity();
        SignalSystem.getInstance().registerUIUpdateChange(this);
        setRetainInstance(true);

    }

    @Override
    public void onDestroy() {
        SignalSystem.getInstance().unRegisterUIUpdateChange(this);
        super.onDestroy();
    }


    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		rootView = inflater.inflate(R.layout.fragment_my_world, container, false);
        tv_openListingsNumber = (TextView) rootView.findViewById(R.id.tv_openListingsNumber);
        initOpenListings();

        initItemInfoWidgets(); // won't do anything if not shown
        FloatingActionButton button = (FloatingActionButton) rootView.findViewById(R.id.b_add_item);
        button.setOnClickListener(this);


        return rootView;
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
        this.ib_showMap = (ImageButton) item_info.findViewById(R.id.ib_showMap);
        this.tv_descriptionTitle = (TextView) item_info.findViewById(R.id.tv_descriptionTitle);
    }

    private void initOpenListings() {

        lv_openListings = (ListView) rootView.findViewById(R.id.lv_openListings);
        setContextualBar(lv_openListings);

        items = new ArrayList<>();
        myOpenListingsAdapter = new OpenItemsAdapter(items, rootView, this);
        lv_openListings.setAdapter(myOpenListingsAdapter);
        lv_openListings.setOnItemClickListener(myOpenListingsAdapter);

        updateMyItems(items);

    }

    private void updateMyItems(final List<Item> items) {
        // get all my lost items
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.ParseObject.PARSE_LOST);
        query.fromLocalDatastore();
        query.orderByDescending(Constants.ParseReport.TIME);
        query.whereEqualTo(Constants.ParseReport.USER_ID, ParseUser.getCurrentUser().getObjectId());
        query.whereEqualTo(Constants.ParseReport.IS_ALIVE, true);

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(final List<ParseObject> itemsList, com.parse.ParseException e) {
                if (e == null) {
                    items.clear();
                    for (int i = 0; i < itemsList.size(); i++) {
                        convertParseListToItemList(itemsList, items);
                    }
                    myOpenListingsAdapter.notifyDataSetChanged();
                    tv_openListingsNumber.setText(String.valueOf(itemsList.size()));

//                    if(clickedPosition != -1) {
//                        lv_openListings.performItemClick(lv_openListings.getAdapter().getView(clickedPosition, null, null), clickedPosition,
//                                lv_openListings.getAdapter().getItemId(clickedPosition));
//                    }
                }
            }

        });

    }

    private void convertParseListToItemList(List<ParseObject> itemsList, List<Item> items) {

        items.clear();
        Item item = null;
        for (ParseObject parseItem : itemsList){
            try {
                if (null != parseItem) {
                    item = new Item(parseItem);
                    items.add(item);
                }
            } catch (NullPointerException e) {
                Log.d(Constants.LOST_FOUND_TAG, "caught NullPointerException when adding an item (item == null?" + (item == null) + ")");
                Log.d(Constants.LOST_FOUND_TAG, "based on parseItem: " + parseItem.toString());
            }
        }
    }

    private void setContextualBar(final ListView usersListView) {
        usersListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        // Capture ListView item click
        usersListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                int selectCount = usersListView.getCheckedItemCount();

                if(selectCount > 1){

                    SparseBooleanArray checkArr = usersListView.getCheckedItemPositions();
                    for(int i=0;i<usersListView.getCount();i++){

                        //check item is checked and not the last item
                        if(checkArr.get(i) && position != i){
                            usersListView.setItemChecked(i, false);
                            break;
                        }
                    }
                }
                myOpenListingsAdapter.setChosenItemPosition(position);
                initShareIntent();
            }
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                final Item item_selected = (Item) myOpenListingsAdapter.getItem(myOpenListingsAdapter.getChosenItemPosition());
                switch (item.getItemId()) {
                    case R.id.delete:

                        AlertDialog alertDialog = new AlertDialog.Builder(rootView.getContext()).create(); //Read Update
                        alertDialog.setTitle("Delete Report");
                        alertDialog.setMessage("Are you sure you want to delete the report?");

                        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,"Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                myOpenListingsAdapter.remove(item_selected);
                            }
                        });
                        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,"No", new DialogInterface.OnClickListener()    {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                        alertDialog.show();  //<-- See This!
                        break;
                    case R.id.edit:
                        myOpenListingsAdapter.edit(item_selected);
                        break;
                    case R.id.get_matches:
                        myOpenListingsAdapter.getMatches(item_selected);
                        break;
                    default:
                }
                // Close CAB
                mode.finish();
                return true;

            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_my_world_contextual, menu);
                ((MainActivity) rootView.getContext()).setActionMode(mode);
                setActionMode(mode);
                initContextMenu(menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                ((MainActivity)rootView.getContext()).setActionMode(null);
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }
        });
    }

    private void initShareIntent() {

        // save image of item to file.
//        ImageView imageView = ((OpenItemsAdapter.ViewHolder) lv_openListings.getChildAt(position).getTag()).itemImage;
//        Bitmap image = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
//        ImageUtils.saveImageToFile(image, "tempImage.png");

        // create intent for sharing, if user chooses to share.
        Item chosenItem = myOpenListingsAdapter.getChosenItem();
        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        File tempImageFile = new File(Environment.getExternalStorageDirectory() + Constants.APP_IMAGE_DIRECTORY_NAME + "/tempImage.png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempImageFile));
        shareIntent.putExtra(Intent.EXTRA_TEXT, chosenItem.getShareDescription());
        shareActionProvider.setShareIntent(shareIntent);

    }

    private void initContextMenu(Menu menu) {

        shareActionProvider = (ShareActionProvider) menu.findItem(R.id.share).getActionProvider();
        shareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {

            @Override
            public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                final String packageName = intent.getComponent().getPackageName();
                Item chosenItem = myOpenListingsAdapter.getChosenItem();
                int chosenItemPosition = myOpenListingsAdapter.getChosenItemPosition();
                // save image of item to file.
                ImageView imageView = ((OpenItemsAdapter.ViewHolder) lv_openListings.getChildAt(chosenItemPosition).getTag()).itemImage;
                Bitmap image = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                ImageUtils.saveImageToFile(image, "tempImage.png");

                if (packageName.equals("com.facebook.katana")) {

                    // save description to clipboard because facebook don't let you upload a status.
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) rootView.getContext()
                            .getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData
                            .newPlainText("share description", chosenItem.getShareDescription());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(rootView.getContext(), "Share description copied to clipboard, paste it in facebook status.", Toast.LENGTH_LONG).show();
                }
                actionMode.finish();
                return false;
            }

        });
    }

    public void updateData() {
        if(items == null){
            return;
        }
        updateMyItems(items);
    }

    public void setActionMode(ActionMode actionMode) {
        this.actionMode = actionMode;
    }

    private boolean showItemInfoWidgets() {
        if (null == this.rootView.findViewById(R.id.item_info)) {
            return false;
        }

        this.iv_itemImage.setVisibility(View.VISIBLE);
        this.ib_showMap.setVisibility(View.VISIBLE);
        this.tv_lossTime.setVisibility(View.VISIBLE);
        this.tv_location.setVisibility(View.VISIBLE);
        this.tv_descriptionContent.setVisibility(View.VISIBLE);
        this.tv_descriptionTitle.setVisibility(View.VISIBLE);

        return true;
    }


    public boolean setDisplayedItem(final Item item) {
        if (!showItemInfoWidgets()) {
            return false;
        }

        Picasso.with(myActivity).load(item.getImageUrl()).placeholder(R.drawable.image_unavailable).into(this.iv_itemImage);
        this.tv_lossTime.setText(item.getTimeAsString());
        this.tv_location.setText(item.getLocationString());
        this.tv_descriptionContent.setText(item.getDescription());

        initMapButton(item);

        return true;
    }

    private void initMapButton(final Item item) {
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

    @Override
    public void onDataChange(Constants.UIActions action, boolean bSuccess, Intent data) {

        switch(action){
            case uiaItemSaved:
                updateData();
                break;
        }
    }

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.b_add_item){

            AlertDialog alertDialog = new AlertDialog.Builder(rootView.getContext()).create(); //Read Update
            alertDialog.setTitle("Report Type");
            alertDialog.setMessage("Choose type of report form");

            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,"Lost", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    openReportForm(true);
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Found", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    openReportForm(false);
                }
            });
            alertDialog.show();  //<-- See This!
        }

    }
    private void openReportForm(boolean isLost){
        Intent newItemIntent = new Intent(rootView.getContext(), ReportFormActivity.class);
        newItemIntent.putExtra(Constants.ReportForm.IS_LOST_FORM, isLost)
                .putExtra(Constants.ReportForm.IS_EDIT_FORM, false);

        ((Activity) rootView.getContext()).startActivityForResult(newItemIntent, Constants.REQUEST_CODE_REPORT_FORM);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.clickedPosition = myOpenListingsAdapter.getClickedPosition();
        if(clickedDialog!=null){
            clickedDialog.dismiss();
        }
    }

    public void setClickedDialog(Dialog clickedDialog) {
        this.clickedDialog = clickedDialog;
    }
}
