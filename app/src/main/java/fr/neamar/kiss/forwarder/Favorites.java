package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.ui.ListPopup;

public class Favorites extends Forwarder implements View.OnClickListener, View.OnLongClickListener {
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
    public final static int TRY_TO_RETRIEVE = FAVORITES_COUNT + 2;

    /**
     * Currently displayed favorites
     */
    private ArrayList<Pojo> favoritesPojo;

    Favorites(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        if(prefs.getBoolean("enable-favorites-bar", true)) {
            mainActivity.favorites = mainActivity.findViewById(R.id.externalFavoriteBar);
            // Hide the embedded bar
            mainActivity.findViewById(R.id.embeddedFavoritesBar).setVisibility(View.INVISIBLE);
        }
        else {
            mainActivity.favorites = mainActivity.findViewById(R.id.embeddedFavoritesBar);
            // Hide the external bar
            mainActivity.findViewById(R.id.externalFavoriteBar).setVisibility(View.GONE);
        }

        registerClickOnFavorites();
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

    void onAllProvidersLoaded() {
        onFavoriteChange();
    }

    void onFavoriteChange() {
        int[] favoritesIds = FAV_IDS;

        favoritesPojo = KissApplication.getApplication(mainActivity).getDataHandler()
                .getFavorites(TRY_TO_RETRIEVE);

        // Don't look for items after favIds length, we won't be able to display them
        for (int i = 0; i < Math.min(favoritesIds.length, favoritesPojo.size()); i++) {
            Pojo pojo = favoritesPojo.get(i);

            ImageView image = (ImageView) mainActivity.favorites.findViewById(favoritesIds[i]);

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
            mainActivity.favorites.findViewById(favoritesIds[i]).setVisibility(View.GONE);
        }
    }

    void updateRecords(String query) {
        if(query.isEmpty()) {
            mainActivity.favorites.setVisibility(View.VISIBLE);
        }
        else {
            mainActivity.favorites.setVisibility(View.GONE);
        }
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
                    KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites(mainActivity, "app://" + packageName + "/" + resolveInfo.activityInfo.name);
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
                    KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites(mainActivity, "app://" + packageName + "/" + resolveInfo.activityInfo.name);
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
                    KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites(mainActivity, "app://" + packageName + "/" + resolveInfo.activityInfo.name);
                }
            }
        }
    }

    private void registerClickOnFavorites() {
        for (int id : FAV_IDS) {
            mainActivity.favorites.findViewById(id).setOnClickListener(this);
        }
    }

    private void registerLongClickOnFavorites() {
        for (int id : FAV_IDS) {
            mainActivity.favorites.findViewById(id).setOnLongClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        // The bar is shown due to dispatchTouchEvent, hide it again to stop the bad ux.
        mainActivity.displayKissBar(false);

        int favNumber = Integer.parseInt((String) v.getTag());
        if (favNumber >= favoritesPojo.size()) {
            // Clicking on a favorite before everything is loaded.
            Log.i(TAG, "Clicking on an unitialized favorite.");
            return;
        }

        // Favorites handling
        Pojo pojo = favoritesPojo.get(favNumber);
        final Result result = Result.fromPojo(mainActivity, pojo);

        result.fastLaunch(mainActivity, v);
    }

    @Override
    public boolean onLongClick(View v) {
        int favNumber = Integer.parseInt((String) v.getTag());
        if (favNumber >= favoritesPojo.size()) {
            // Clicking on a favorite before everything is loaded.
            Log.i(TAG, "Long clicking on an unitialized favorite.");
            return false;
        }
        // Favorites handling
        Pojo pojo = favoritesPojo.get(favNumber);
        final Result result = Result.fromPojo(mainActivity, pojo);
        ListPopup popup = result.getPopupMenu(mainActivity, mainActivity.adapter, v);
        mainActivity.registerPopup(popup);
        popup.show(v);
        return true;
    }
}
