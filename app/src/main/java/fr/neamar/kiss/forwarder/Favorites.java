package fr.neamar.kiss.forwarder;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
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
import fr.neamar.kiss.result.ContactsResult;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.ui.ListPopup;

public class Favorites extends Forwarder implements View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = "FavoriteForwarder";

    // Package used by Android when an Intent can be matched with more than one app
    private static final String DEFAULT_RESOLVER = "com.android.internal.app.ResolverActivity";

    /**
     * IDs for the favorites buttons
     */
    private final static int[] FAV_IDS = new int[]{R.id.favorite0, R.id.favorite1, R.id.favorite2, R.id.favorite3, R.id.favorite4, R.id.favorite5};
    private View[] favoritesViews;

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
        if (isExternalFavoriteBarEnabled()) {
            mainActivity.favoritesBar = mainActivity.findViewById(R.id.externalFavoriteBar);
            // Hide the embedded bar
            mainActivity.findViewById(R.id.embeddedFavoritesBar).setVisibility(View.INVISIBLE);
        } else {
            mainActivity.favoritesBar = mainActivity.findViewById(R.id.embeddedFavoritesBar);
            // Hide the external bar
            mainActivity.findViewById(R.id.externalFavoriteBar).setVisibility(View.GONE);
        }

        favoritesViews = new View[FAV_IDS.length];
        for (int i = 0; i < FAV_IDS.length; i++) {
            favoritesViews[i] = mainActivity.favoritesBar.findViewById(FAV_IDS[i]);
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

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        Bitmap bitmap;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            // Single color bitmap will be created of 1x1 pixel
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    void onFavoriteChange() {
        int[] favoritesIds = FAV_IDS;

        favoritesPojo = KissApplication.getApplication(mainActivity).getDataHandler()
                .getFavorites(TRY_TO_RETRIEVE);

        // Don't look for items after favIds length, we won't be able to display them
        for (int i = 0; i < Math.min(favoritesIds.length, favoritesPojo.size()); i++) {
            Pojo pojo = favoritesPojo.get(i);

            ImageView image = (ImageView) favoritesViews[i];

            Result result = Result.fromPojo(mainActivity, pojo);
            Drawable drawable = result.getDrawable(mainActivity);
            if (drawable != null) {
                if (result instanceof ContactsResult) {
                    Bitmap originalIcon = drawableToBitmap(drawable);
                    // Load the bitmap as a shader to the paint.
                    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    final Shader shader = new BitmapShader(originalIcon, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    paint.setShader(shader);

                    // Create a new image that will be used by the favorites ImageView
                    Bitmap newIcon = Bitmap.createBitmap(originalIcon.getWidth(), originalIcon.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(newIcon);
                    // Draw a circle with the required radius.
                    final float halfWidth = canvas.getWidth() / 2;
                    final float halfHeight = canvas.getHeight() / 2;
                    final float radius = Math.max(halfWidth, halfHeight);
                    canvas.drawCircle(halfWidth, halfHeight, radius, paint);

                    image.setImageBitmap(newIcon);
                } else {
                    image.setImageDrawable(drawable);
                }
            } else {
                // Use a placeholder if no drawable found
                image.setImageResource(R.drawable.ic_launcher_white);
            }

            image.setVisibility(View.VISIBLE);
            image.setContentDescription(pojo.displayName);
        }

        // Hide empty favorites (not enough favorites yet)
        for (int i = favoritesPojo.size(); i < favoritesIds.length; i++) {
            favoritesViews[i].setVisibility(View.GONE);
        }
    }

    void updateSearchRecords(String query) {
        if (query.isEmpty()) {
            mainActivity.favoritesBar.setVisibility(View.VISIBLE);
        } else {
            mainActivity.favoritesBar.setVisibility(View.GONE);
        }
    }

    void onDataSetChanged() {
        // Do not display the external bar when viewing all apps
        if(mainActivity.isViewingAllApps() && isExternalFavoriteBarEnabled()) {
            mainActivity.favoritesBar.setVisibility(View.GONE);
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
                Log.i(TAG, "Dialer resolves to:" + packageName + "/" + resolveInfo.activityInfo.name);

                if ((resolveInfo.activityInfo.name != null) && (!resolveInfo.activityInfo.name.equals(DEFAULT_RESOLVER))) {
                    String activityName = resolveInfo.activityInfo.name;
                    if(packageName.equals("com.google.android.dialer")) {
                        // Default dialer has two different activities, one when calling a phone number and one when opening the app from the launcher.
                        // (com.google.android.apps.dialer.extensions.GoogleDialtactsActivity vs. com.google.android.dialer.extensions.GoogleDialtactsActivity)
                        // (notice the .apps. in the middle)
                        // The phoneIntent above resolve to the former, which isn't exposed as a Launcher activity and thus can't be displayed as a favorite
                        // This hack uses the correct resolver when the application id is the default dialer.
                        // In terms of maintenance, if Android was to change the name of the phone's main resolver, the favorite would simply not appear
                        // and we would have to update the String below to the new default resolver
                        activityName = "com.google.android.dialer.extensions.GoogleDialtactsActivity";
                    }
                    KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites(mainActivity, "app://" + packageName + "/" + activityName);
                }
            }
        }
        {
            //Default contacts app
            Intent contactsIntent = new Intent(Intent.ACTION_DEFAULT, ContactsContract.Contacts.CONTENT_URI);
            ResolveInfo resolveInfo = mainActivity.getPackageManager().resolveActivity(contactsIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null) {
                String packageName = resolveInfo.activityInfo.packageName;
                Log.i(TAG, "Contacts resolves to:" + packageName);
                if ((resolveInfo.activityInfo.name != null) && (!resolveInfo.activityInfo.name.equals(DEFAULT_RESOLVER))) {
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
                Log.i(TAG, "Browser resolves to:" + packageName);

                if ((resolveInfo.activityInfo.name != null) && (!resolveInfo.activityInfo.name.equals(DEFAULT_RESOLVER))) {
                    String activityName = resolveInfo.activityInfo.name;
                    if(packageName.equalsIgnoreCase("com.android.chrome")) {
                        // Chrome has two different activities, one for Launcher and one when opening an URL.
                        // The browserIntent above resolve to the latter, which isn't exposed as a Launcher activity and thus can't be displayed
                        // This hack uses the correct resolver when the application is Chrome.
                        // In terms of maintenance, if Chrome was to change the name of the main resolver, the favorite would simply not appear
                        // and we would have to update the String below to the new default resolver
                        activityName = "com.google.android.apps.chrome.Main";
                    }
                    KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites(mainActivity, "app://" + packageName + "/" + activityName);
                }
            }
        }
    }

    private void registerClickOnFavorites() {
        for (View v : favoritesViews) {
            v.setOnClickListener(this);
        }
    }

    private void registerLongClickOnFavorites() {
        for (View v : favoritesViews) {
            v.setOnLongClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
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

    private boolean isExternalFavoriteBarEnabled() {
        return prefs.getBoolean("enable-favorites-bar", true);
    }
}
