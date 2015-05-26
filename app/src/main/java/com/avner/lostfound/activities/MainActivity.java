package com.avner.lostfound.activities;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.avner.lostfound.Constants;
import com.avner.lostfound.R;
import com.avner.lostfound.adapters.TabsPagerAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends FragmentActivity implements
        ActionBar.TabListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MenuItem.OnMenuItemClickListener {

    private Location lastKnownLocation = null;
    private GoogleApiClient googleApiClient;

    private ViewPager viewPager;
    private ActionBar actionBar;
    private SearchView sv_search;
    private MenuItem mi_search_menu_item;

    private int selectedTabIndex = 0;

    // Tab titles
    private String[] tabsStrings = {
            Constants.TabTexts.MY_WORLD,
            Constants.TabTexts.LOST,
            Constants.TabTexts.FOUND,
            Constants.TabTexts.STATS
    };

    // Tab icons
    private int[] tabsIcons = {
            R.drawable.earth,
            R.drawable.question_mark_red1,
            R.drawable.chequered_flags,
            R.drawable.graph,
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialization
        viewPager = (ViewPager) findViewById(R.id.pager);
        actionBar = getActionBar();
        TabsPagerAdapter mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(mAdapter);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setHomeButtonEnabled(false);

        // Adding Tabs with icons
        for (int i = 0; i < tabsStrings.length; ++i) {
            Tab tab = actionBar.newTab()
                    .setCustomView(R.layout.action_bar_tab_layout)
                    .setTabListener(this);

            ImageView imageView = (ImageView) tab.getCustomView().findViewById(R.id.iv_tabIcon);
            imageView.setImageResource(tabsIcons[i]);

            TextView textView = (TextView) tab.getCustomView().findViewById(R.id.tv_tabText);
            textView.setText(tabsStrings[i]);

            actionBar.addTab(tab);
        }

        /**
         * on swiping the viewpager make respective tab selected
         * */
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                // on changing the page make respected tab selected
                actionBar.setSelectedNavigationItem(position);

                if (isListingFragment(position)) {
                    // switched to a listing tab - enable search view
                    Log.d(Constants.LOST_FOUND_TAG, "changed to a listing tab - enabling search view");
                    showSearchView();
                }
                else {
                    // switched to a non-listing tab - disable search view
                    Log.d(Constants.LOST_FOUND_TAG, "changed to a non-listing tab - disabling search view");
                    hideSearchView();
                }
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {}

            @Override
            public void onPageScrollStateChanged(int arg0) {}
        });

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void showSearchView() {
        sv_search.setVisibility(View.VISIBLE);
        sv_search.setEnabled(true);
        mi_search_menu_item.setVisible(true);
        mi_search_menu_item.setEnabled(true);
    }

    private void hideSearchView() {
        sv_search.setVisibility(View.INVISIBLE);
        sv_search.setEnabled(false);
        mi_search_menu_item.setVisible(false);
        mi_search_menu_item.setEnabled(false);
        mi_search_menu_item.collapseActionView();
    }

    private boolean isListingFragment(int position) {
        final TextView tv = (TextView) actionBar.getTabAt(position)
                .getCustomView()
                .findViewById(R.id.tv_tabText);

        return (tv.getText().equals(Constants.TabTexts.FOUND) || tv.getText().equals(Constants.TabTexts.LOST));
    }

    public Location getLastKnownLocation() {
        return new Location(this.lastKnownLocation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        this.mi_search_menu_item = menu.findItem(R.id.search);
        this.sv_search = (SearchView) menu.findItem(R.id.search).getActionView();

        if (!isListingFragment(actionBar.getSelectedNavigationIndex())) {
            hideSearchView();
        }
        MenuItem settings = menu.findItem(R.id.action_settings);
        settings.setOnMenuItemClickListener(this);

        return true;
    }

    public SearchView getSearchView() {
        return this.sv_search;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        // on tab selected
        // show respected fragment view
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.googleApiClient.connect();
        setInitLocation();

    }

    private void setInitLocation() {
        if (null == this.lastKnownLocation) {
            Location mockLocation = new Location(LocationManager.NETWORK_PROVIDER);
            mockLocation.setLatitude(90.0);
            mockLocation.setLongitude(0.0);
            mockLocation.setAltitude(0.0);
            mockLocation.setAccuracy(50.0f);

            this.lastKnownLocation = mockLocation;
            Log.d(Constants.LOST_FOUND_TAG, "set init mock location: " + mockLocation.toString());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        updateCurrentLocation();
    }

    private void updateCurrentLocation() {
        Log.d(Constants.LOST_FOUND_TAG, "Updating lastKnownLocation...");
        Location location = LocationServices.FusedLocationApi.getLastLocation(this.googleApiClient);

        if (null != location) { // got a location
            lastKnownLocation = location;
            Log.d(Constants.LOST_FOUND_TAG, "got last location! " + location.toString());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(item.getItemId() == R.id.action_settings){
            Intent intent = new Intent(this,SettingsActivity.class);
            startActivity(intent);
        }
        return false;
    }
}
