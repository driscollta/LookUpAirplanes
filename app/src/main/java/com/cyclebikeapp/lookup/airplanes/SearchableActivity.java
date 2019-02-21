package com.cyclebikeapp.lookup.airplanes;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import java.util.ArrayList;
import java.util.HashMap;

import static com.cyclebikeapp.lookup.airplanes.AirplaneDialogFragment.SEARCH;
import static com.cyclebikeapp.lookup.airplanes.Constants.ADF_KEY_ICAO24_NUMBER;
import static com.cyclebikeapp.lookup.airplanes.Constants.ADF_KEY_LINK;
import static com.cyclebikeapp.lookup.airplanes.Constants.ADF_KEY_MANUFACTURER;
import static com.cyclebikeapp.lookup.airplanes.Constants.ADF_KEY_MODEL;
import static com.cyclebikeapp.lookup.airplanes.Constants.ADF_KEY_NUM_SEARCH_RESULTS;
import static com.cyclebikeapp.lookup.airplanes.Constants.ADF_KEY_OWNER;
import static com.cyclebikeapp.lookup.airplanes.Constants.ADF_KEY_REGISTRATION;
import static com.cyclebikeapp.lookup.airplanes.Constants.ADF_KEY_TITLE;
import static com.cyclebikeapp.lookup.airplanes.Constants.CC_MAP_KEY_AIRPLANE_NAME;
import static com.cyclebikeapp.lookup.airplanes.Constants.CC_MAP_KEY_ICAO24;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_MANUFACTURER;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_MODEL;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_OWNER;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_REGISTRATION;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_ICAO24;
import static com.cyclebikeapp.lookup.airplanes.Constants.ZERO;
import static com.cyclebikeapp.lookup.airplanes.Utilities.getMoreInfoURI;

@SuppressWarnings("ConstantConditions")
public class SearchableActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private static final int TOO_MANY_SEARCH_RESULTS = 100;
    // all the database functions
    private AirplaneDBAdapter dataBaseAdapter = null;
    private PopupMenu popup;
    private View popupAnchor;

    public SearchableActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchable);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        dataBaseAdapter = new AirplaneDBAdapter(getApplicationContext());
    }
    @Override
    protected void onResume() {
        if (MainActivity.DEBUG) {
            Log.w(this.getClass().getName(), "searchable-onResume()");
        }
        // create readable satDB
        try {
            if (dataBaseAdapter != null && dataBaseAdapter.isClosed()) {
                dataBaseAdapter.openRead();
            }
        } catch (SQLException ignored) {
        }
        popupAnchor = findViewById(R.id.search_popup_anchor);
        popup = new PopupMenu(SearchableActivity.this, popupAnchor);
        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        setResult(RESULT_OK, intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doMySearch(query);
        }
        super.onResume();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (MainActivity.DEBUG) {
            Log.w(this.getClass().getName(), "onSaveInstanceState()");
        }
        super.onSaveInstanceState(outState);
    }

    private void doMySearch(String query) {
        // 1) if query is a hex number search by icao24
        // 2) if query is a registration, search reg column in DB
        // 3) assemble the query for "starts with"
        // wildcard '_' looks for a single character; '%' looks for any number of characters
        // search term is case insensitive
        if (queryIsHex(query)) {
            Log.w(this.getClass().getName(), "queryIsHex " + query);

            Cursor searchResult = searchByHexNumber(query);
            int cursorSize = getCursorSize(searchResult);
            if (cursorSize == 0) {
                Log.w(this.getClass().getName(), "didn't get anything from icao24 " + query);
                // didn't get anything from icao24
                // pop-up satellite Dialog with text = "No Airplanes found matching query"
                String link = "";
                String icao24 = ZERO;
                showAirplaneDialogFragment(getString(R.string.no_search_results), null, link, 0, icao24);
            }
            // if cursorSize = 1, retrieve data via icao24, pop-up Airplane Dialog
            if (cursorSize == 1 && searchResult.moveToFirst()) {
                String icao24 = Integer.toString(searchResult.getInt(searchResult.getColumnIndexOrThrow(DB_KEY_ICAO24)), 16);
                Log.w(this.getClass().getName(), String.format("got one result from icao24: %s query: %s ", icao24, query));
                HashMap<String, String> data = dataBaseAdapter.fetchAirplaneData(icao24);
                String link = composeLinkFromRegistration(data);
                showAirplaneDialogFragment(getString(R.string.search_results), data, link, 1, icao24);
            }
        } else {
            Cursor searchResult = searchByRegistration(query);
            int cursorSize = getCursorSize(searchResult);
            if (cursorSize == 0) {
                Log.w(this.getClass().getName(), "didn't get anything from registration " + query);
                // didn't get anything from registartion
                searchResult = searchByOwner(query);
                cursorSize = getCursorSize(searchResult);
            }
            if (cursorSize == 0) {
                Log.w(this.getClass().getName(), "didn't get anything from owner " + query);
                // didn't get anything from owner either
                // pop-up Airplane Dialog with text = "No Airplanes found matching query"
                String link = "";
                String icao24 = ZERO;
                showAirplaneDialogFragment(getString(R.string.no_search_results), null, link, 0, icao24);
            }
            // if cursorSize = 1, retrieve data via icao24, pop-up Airplane Dialog
            if (cursorSize == 1 && searchResult.moveToFirst()) {
                String icao24 = Integer.toString(searchResult.getInt(searchResult.getColumnIndexOrThrow(DB_KEY_ICAO24)), 16);
                Log.w(this.getClass().getName(), String.format("got one result from reg or owner icao24: %s query: %s ", icao24, query));
                HashMap<String, String> data = dataBaseAdapter.fetchAirplaneData(icao24);
                String link = composeLinkFromRegistration(data);
                showAirplaneDialogFragment(getString(R.string.search_results), data, link, 1, icao24);
            } else if (cursorSize > TOO_MANY_SEARCH_RESULTS){
                String link = "";
                showAirplaneDialogFragment(getString(R.string.too_many_search_results), null, link, 101, ZERO);
            } else if (cursorSize > 1) {
                // if search result != null and has more than one entry, populate the List, wait for itemClick
                // has to be 'final' since it's in the run()
                final ArrayList<HashMap<String, String>> searchAirplaneList = makeAirplaneList(searchResult);
                // how many satellites did I find?  print Name, Norad num
                //situate a View in the Layout to anchor this pop-up
                popupAnchor.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showPopupMenu(searchAirplaneList);
                    }
                }, 50);
            }
        }
    }

    private boolean queryIsHex(String query) {
        return query.matches("-?[0-9a-fA-F]+");
    }

    private boolean queryStartsWithNumber(String query) {
        return Character.isDigit(query.charAt(0));
    }

    private int getCursorSize(Cursor searchResult) {
        int cursorSize = 0;
        if (searchResult != null){
            cursorSize = searchResult.getCount();
        }
        return cursorSize;
    }

    private Cursor searchByHexNumber(String query) {
// try matching icao24 exactly
        if (dataBaseAdapter == null) {
            return  null;
        }
        return dataBaseAdapter.searchDataBaseByICAO24Number(query);
    }
    private Cursor searchByOwner(String query) {
        if (dataBaseAdapter == null) {
            return  null;
        }
        String containsQuery = "%" + query + "%";
        return dataBaseAdapter.searchDataBaseByOwner(containsQuery);
    }

    private Cursor searchByRegistration(String query) {
        if (dataBaseAdapter == null) {
            return  null;
        }
        // try matching registration starting with query
        String startsWithQuery = query + "%";
        Cursor searchResult = dataBaseAdapter.searchDataBaseByRegistration(startsWithQuery);
        if (searchResult == null) {
            //no result, try searching by owner
            searchResult = searchByOwner(query);
        }
        return searchResult;
    }
    /**
     * Convert the Cursor into a Hashmap used for the pop-up menu
     * @param searchResult is a Cursor containing registrations from the match to the search
     * @return is a Hashmap of the icao24 and names to populate the list
     */
    private ArrayList<HashMap<String, String>> makeAirplaneList(Cursor searchResult) {
        ArrayList<HashMap<String, String>> returnData = new ArrayList<>();
        if (searchResult != null && searchResult.moveToFirst()) {
            do {
                HashMap<String, String> hmItem = new HashMap<>();
                hmItem.put(CC_MAP_KEY_AIRPLANE_NAME, searchResult.getString(searchResult.getColumnIndexOrThrow(DB_KEY_AIR_REGISTRATION)));
                hmItem.put(CC_MAP_KEY_ICAO24, searchResult.getString(searchResult.getColumnIndexOrThrow(DB_KEY_ICAO24)));
                returnData.add(hmItem);
            } while (searchResult.moveToNext());
        }
        return returnData;
    }

    @Override
    protected void onStop() {
        if (MainActivity.DEBUG) {
            Log.w(this.getClass().getName(), "searchable-onStop()");
        }
        super.onStop();
        if (dataBaseAdapter != null) {
            dataBaseAdapter.close();
        }
    popup.dismiss();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
            NavUtils.navigateUpFromSameTask(SearchableActivity.this);
    }

    private void showPopupMenu(ArrayList<HashMap<String, String>> clickSatList) {
        // Don't leave a pop-up on screen if we're leaving Activity
        if (SearchableActivity.this.isFinishing()) {
            return;
        }
        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(SearchableActivity.this);
        //popup.setOnDismissListener(SearchableActivity.this);
        //add items to pop-up depending on # satellites
        int order = 1;
        // only 1 group
        int groupId = 0;
        for (HashMap<String, String> hm : clickSatList) {
            // use "itemId" entry to store icao24 number.
            // When user clicks on List we're given itemId and we can then retrieve icao24
            int itemId = Integer.parseInt(hm.get(CC_MAP_KEY_ICAO24));
            popup.getMenu().add(groupId, itemId, order, hm.get(CC_MAP_KEY_AIRPLANE_NAME));
            order++;
        }
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.actions, popup.getMenu());
        popup.show();
    }

    /**
     * This method will be invoked when a menu item is clicked if the item itself did
     * not already handle the event.
     *
     * @param item {@link MenuItem} that was clicked
     * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        String icao24 = Integer.toString(item.getItemId(), 16);
        Log.w(this.getClass().getName(), String.format("onMenuItemClick icao24: %s", icao24));

        HashMap<String, String> data = dataBaseAdapter.fetchAirplaneData(icao24);
        String link = composeLinkFromRegistration(data);
        showAirplaneDialogFragment(getString(R.string.search_results), data, link, popup.getMenu().size(), icao24);
        return true;
    }

    private String composeLinkFromRegistration(HashMap<String, String> data) {
        return getMoreInfoURI(data.get(DB_KEY_AIR_REGISTRATION));
    }

    @SuppressLint("DefaultLocale")
    private void showAirplaneDialogFragment(
            String dialog_title,
            HashMap<String, String> dialog_message,
            final String link,
            final int numSearchResults,
            final String icao24) {

        Bundle dialogBundle = new Bundle();
        dialogBundle.putCharSequence(ADF_KEY_TITLE, dialog_title);
        dialogBundle.putCharSequence(ADF_KEY_LINK, link);
        dialogBundle.putInt(ADF_KEY_NUM_SEARCH_RESULTS, numSearchResults);
        if (numSearchResults >= 1 && numSearchResults < 101) {
            dialogBundle.putCharSequence(ADF_KEY_ICAO24_NUMBER, icao24);
            dialogBundle.putCharSequence(ADF_KEY_REGISTRATION, dialog_message.get(DB_KEY_AIR_REGISTRATION));
            dialogBundle.putCharSequence(ADF_KEY_MANUFACTURER, dialog_message.get(DB_KEY_AIR_MANUFACTURER));
            dialogBundle.putCharSequence(ADF_KEY_MODEL, dialog_message.get(DB_KEY_AIR_MODEL));
            dialogBundle.putCharSequence(ADF_KEY_OWNER, dialog_message.get(DB_KEY_AIR_OWNER));
        }
        AirplaneDialogFragment newFragment = AirplaneDialogFragment.newInstance(dialogBundle);
        newFragment.show(getFragmentManager(), SEARCH);
    }

    public void doAirplaneDialogPostiveClick(Bundle aBundle) {
        // User clicked OK button, just exit dialog
        int numSearchResults = aBundle.getInt(ADF_KEY_NUM_SEARCH_RESULTS);
        if (numSearchResults > 1) {
            // go back to showing the popup from search
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.actions, popup.getMenu());
            popup.show();
        } else {
            NavUtils.navigateUpFromSameTask(SearchableActivity.this);
        }
    }

    public void doAirplaneDialogNegativeClick(Bundle aBundle) {
        String link = aBundle.getString(ADF_KEY_LINK);
        // User clicked More Info button; start Intent to navigate to provided link
        String url = link.trim();
        // if !hasWiFiInternetConnection && !hasMobileInternetPermission ask for mobileInternetPermission
        // else must be okay to send browseIntent. If no internet connection, browser will complain
        final Intent browseIntent = new Intent(Intent.ACTION_VIEW);
        browseIntent.setData(Uri.parse(url));
        startActivity(browseIntent);
    }
}
