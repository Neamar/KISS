package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.ui.ListPopup;

public class FavoriteForwarder extends Forwarder {
    private static final String TAG = "FavoriteForwarder";

    /**
     * IDs for the favorites buttons
     */
    private final static int[] FAV_IDS = new int[]{R.id.favorite0, R.id.favorite1, R.id.favorite2, R.id.favorite3, R.id.favorite4, R.id.favorite5};

    /**
     * Number of favorites that can be displayed
     */
    public final static int FAVORITES_COUNT = FAV_IDS.length;

    /**
     * Number of favorites to retrieve.
     * We need to pad this number to account for removed items still in history
     */
    private final int tryToRetrieve = FAVORITES_COUNT + 2;

    /**
     * IDs for the favorites buttons on the quickbar
     */
    private final int[] favBarIds = new int[]{R.id.favoriteBar0, R.id.favoriteBar1, R.id.favoriteBar2, R.id.favoriteBar3, R.id.favoriteBar4, R.id.favoriteBar5};

    /**
     * Favorites bar, in the KISS bar, next to KISS logo
     */
    private final View favoritesInKissBar;


    FavoriteForwarder(MainActivity mainActivity, SharedPreferences prefs) {
        super(mainActivity, prefs);

        favoritesInKissBar = mainActivity.findViewById(R.id.favoritesKissBar);
    }

    @Override
    public void onCreate() {
        registerLongClickOnFavorites();

        if (prefs.getBoolean("firstRun", true)) {
            // It is the first run. Make sure this is not an update by checking if history is empty
            if (DBHelper.getHistoryLength(mainActivity) == 0) {
                addDefaultAppsToFavs();
            }
            // set flag to false
            prefs.edit().putBoolean("firstRun", false).apply();
        }
    }

    @Override
    public void onResume() {
        //Show favorites above search field ONLY if AppProvider is already loaded
        //Otherwise this will get triggered by the broadcastreceiver in the onCreate
        if (mainActivity.allProvidersHaveLoaded) {
            // Favorites needs to be displayed again if the quickfavorite bar is active,
            // Not sure why exactly, but without the "true" the favorites drawable will disappear
            // (not their intent) after moving to another activity and switching back to KISS.
            displayExternalFavoritesBar(true, mainActivity.searchEditText.getText().toString().length() > 0);
        }
    }

    @Override
    public void allProvidersHaveLoaded() {
        displayExternalFavoritesBar(true, false);
    }

    private void displayExternalFavoritesBar(boolean initialize, boolean touched) {
        View quickFavoritesBar = mainActivity.findViewById(R.id.favoritesBar);
        if (mainActivity.searchEditText.getText().toString().isEmpty()
                && prefs.getBoolean("enable-favorites-bar", true)) {
            if ((!prefs.getBoolean("favorites-hide", false) || touched)) {
                quickFavoritesBar.setVisibility(View.VISIBLE);
            }

            if (initialize) {
                Log.i(TAG, "Using quick favorites bar, filling content.");
                favoritesInKissBar.setVisibility(View.INVISIBLE);
                displayFavorites();
            }
        } else {
            quickFavoritesBar.setVisibility(View.GONE);
        }
    }


    public void displayFavorites() {
        int[] favoritesIds = favoritesInKissBar.getVisibility() == View.VISIBLE ? FAV_IDS : favBarIds;

        ArrayList<Pojo> favoritesPojo = KissApplication.getDataHandler(mainActivity)
                .getFavorites(tryToRetrieve);

        if (favoritesPojo.size() == 0) {
            int noFavCnt = prefs.getInt("no-favorites-tip", 0);
            if (noFavCnt < 3 && !prefs.getBoolean("enable-favorites-bar", true)) {
                Toast toast = Toast.makeText(mainActivity, mainActivity.getString(R.string.no_favorites), Toast.LENGTH_SHORT);
                toast.show();
                prefs.edit().putInt("no-favorites-tip", ++noFavCnt).apply();

            }
        }

        // Don't look for items after favIds length, we won't be able to display them
        for (int i = 0; i < Math.min(favoritesIds.length, favoritesPojo.size()); i++) {
            Pojo pojo = favoritesPojo.get(i);

            ImageView image = (ImageView) mainActivity.findViewById(favoritesIds[i]);

            Result result = Result.fromPojo(mainActivity, pojo);
            Drawable drawable = result.getDrawable(mainActivity);
            if (drawable != null) {
                image.setImageDrawable(drawable);
            } else {
                Log.e(TAG, "Falling back to default image for favorite.");
                // Use the default contact image otherwise
                image.setImageResource(R.drawable.ic_contact);
            }

            image.setVisibility(View.VISIBLE);
            image.setContentDescription(pojo.displayName);
        }

        // Hide empty favorites (not enough favorites yet)
        for (int i = favoritesPojo.size(); i < favoritesIds.length; i++) {
            mainActivity.findViewById(favoritesIds[i]).setVisibility(View.GONE);
        }
    }

    @Override
    public void updateRecords(String query) {
        displayExternalFavoritesBar(false, false);
    }

    /**
     * On first run, fill the favorite bar with sensible defaults
     */
    private void addDefaultAppsToFavs() {
        {
            //Default phone call app
            Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
            phoneIntent.setData(Uri.parse("tel:0000"));
            ResolveInfo resolveInfo = mainActivity.getPackageManager().resolveActivity(phoneIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null) {
                String packageName = resolveInfo.activityInfo.packageName;
                if ((resolveInfo.activityInfo.name != null) && (!resolveInfo.activityInfo.name.equals("com.android.internal.app.ResolverActivity"))) {
                    KissApplication.getDataHandler(mainActivity).addToFavorites(mainActivity, "app://" + packageName + "/" + resolveInfo.activityInfo.name);
                }
            }
        }
        {
            //Default contacts app
            Intent contactsIntent = new Intent(Intent.ACTION_DEFAULT, ContactsContract.Contacts.CONTENT_URI);
            ResolveInfo resolveInfo = mainActivity.getPackageManager().resolveActivity(contactsIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null) {
                String packageName = resolveInfo.activityInfo.packageName;
                if ((resolveInfo.activityInfo.name != null) && (!resolveInfo.activityInfo.name.equals("com.android.internal.app.ResolverActivity"))) {
                    KissApplication.getDataHandler(mainActivity).addToFavorites(mainActivity, "app://" + packageName + "/" + resolveInfo.activityInfo.name);
                }
            }

        }
        {
            //Default browser
            Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://"));
            ResolveInfo resolveInfo = mainActivity.getPackageManager().resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null) {
                String packageName = resolveInfo.activityInfo.packageName;

                if ((resolveInfo.activityInfo.name != null) && (!resolveInfo.activityInfo.name.equals("com.android.internal.app.ResolverActivity"))) {
                    KissApplication.getDataHandler(mainActivity).addToFavorites(mainActivity, "app://" + packageName + "/" + resolveInfo.activityInfo.name);
                }
            }
        }
    }

    private void registerLongClickOnFavorites() {
        View.OnLongClickListener listener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                int favNumber = Integer.parseInt((String) view.getTag());
                ArrayList<Pojo> favorites = KissApplication.getDataHandler(mainActivity).getFavorites(tryToRetrieve);
                if (favNumber >= favorites.size()) {
                    // Clicking on a favorite before everything is loaded.
                    Log.i(TAG, "Long clicking on an unitialized favorite.");
                    return false;
                }
                // Favorites handling
                Pojo pojo = favorites.get(favNumber);
                final Result result = Result.fromPojo(mainActivity, pojo);
                ListPopup popup = result.getPopupMenu(mainActivity, mainActivity.adapter, view);
                mainActivity.registerPopup(popup);
                popup.show(view);
                return true;
            }
        };
        for (int id : favBarIds) {
            mainActivity.findViewById(id).setOnLongClickListener(listener);
        }
        for (int id : FAV_IDS) {
            mainActivity.findViewById(id).setOnLongClickListener(listener);
        }
    }
}
