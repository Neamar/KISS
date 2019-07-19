package fr.neamar.kiss.forwarder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.DragEvent;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.notification.NotificationListener;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.result.AppResult;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.ui.ListPopup;

public class Favorites extends Forwarder implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener, View.OnDragListener {
    private static final String TAG = "FavoriteForwarder";

    // Package used by Android when an Intent can be matched with more than one app
    private static final String DEFAULT_RESOLVER = "com.android.internal.app.ResolverActivity";

    /**
     * IDs for the favorites buttons
     */
    private ArrayList<ViewHolder> favoritesViews = new ArrayList<>();

    private static class ViewHolder {
        @Nullable
        View view;
        @NonNull
        Result result;

        ViewHolder(@NonNull Result result) {
            this.result = result;
            view = null;
        }

        void initView(@NonNull Context context, @NonNull ViewGroup parent) {
            // try to recycle old view (inflate is expensive)
            view = result.inflateFavorite(context, view, parent);
            // from this point forward view is not null
            view.setTag(this);
        }
    }

    /**
     * Currently displayed favorites
     */
    private ArrayList<Pojo> favoritesPojo = new ArrayList<>();

    /**
     * Globals for drag and drop support
     */
    private static long startTime = 0; // Start of the drag and drop, used for long press menu
    private float currentX = 0.0f; // Current X position of the drag op, this is 0 on DRAG END so we keep a copy here
    private ViewHolder overApp; // the view for the DRAG_END event is typically wrong, so we store a reference of the last dragged over app.

    /**
     * Configuration for drag and drop
     */
    private final int MOVE_SENSITIVITY = 8; // How much you need to move your finger to be considered "moving"
    private final int LONG_PRESS_DELAY = 250; // How long to hold your finger in place to trigger the app menu.

    // Use so we don't over process on the drag events.
    private boolean mDragEnabled = true;
    private boolean isDragging = false;
    private boolean contextMenuShown = false;
    private int favCount = -1;

    private SharedPreferences notificationPrefs = null;

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

        if (prefs.getBoolean("firstRun", true)) {
            // It is the first run. Make sure this is not an update by checking if history is empty
            if (DBHelper.getHistoryLength(mainActivity) == 0) {
                addDefaultAppsToFavs();
            }
            // set flag to false
            prefs.edit().putBoolean("firstRun", false).apply();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            notificationPrefs = mainActivity.getSharedPreferences(NotificationListener.NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);
        }

    }

    ViewHolder popFavViewHolder(@NonNull Result result) {
        for (Iterator<ViewHolder> iterator = favoritesViews.iterator(); iterator.hasNext(); ) {
            ViewHolder viewHolder = iterator.next();
            if (result.getClass() == viewHolder.result.getClass()) {
                iterator.remove();
                viewHolder.result = result;
                return viewHolder;
            }
        }
        return new ViewHolder(result);
    }

    void onFavoriteChange() {
        favoritesPojo = KissApplication.getApplication(mainActivity).getDataHandler()
                .getFavorites();

        favCount = favoritesPojo.size();

        ArrayList<ViewHolder> holders = new ArrayList<>(favCount);

        // Don't look for items after favIds length, we won't be able to display them
        for (int i = 0; i < favCount; i++) {
            ViewHolder viewHolder = popFavViewHolder(Result.fromPojo(mainActivity, favoritesPojo.get(i)));
            viewHolder.initView(mainActivity, mainActivity.favoritesBar);
            holders.add(viewHolder);
        }

        // release unused view holders and keep the used ones for recycle
        favoritesViews.clear();
        favoritesViews.addAll(holders);

        // clear the view hierarchy
        mainActivity.favoritesBar.removeAllViews();

        // add views to view hierarchy
        for (ViewHolder viewHolder : favoritesViews) {
            assert viewHolder.view != null;
            mainActivity.favoritesBar.addView(viewHolder.view);
            viewHolder.view.setOnDragListener(this);
            viewHolder.view.setOnTouchListener(this);
            viewHolder.view.setVisibility(View.VISIBLE);
        }

        if (notificationPrefs != null) {
            int dotColor;
            if (isExternalFavoriteBarEnabled()) {
                dotColor = UIColors.getPrimaryColor(mainActivity);
            }
            else {
                dotColor = Color.WHITE;
            }

            for (ViewHolder viewHolder : favoritesViews) {
                assert viewHolder.view != null;
                ImageView notificationDot = viewHolder.view.findViewById(R.id.item_notification_dot);
                if (notificationDot == null)
                    continue;
                notificationDot.setColorFilter(dotColor);

                if (viewHolder.result instanceof AppResult) {
                    String packageName = ((AppResult) viewHolder.result).getPackageName();
                    notificationDot.setTag(packageName);
                    notificationDot.setVisibility(notificationPrefs.contains(packageName) ? View.VISIBLE : View.GONE);
                } else {
                    // Ensure view is clean (might have been recycled after a drag and drop)
                    notificationDot.setTag(null);
                    notificationDot.setVisibility(View.GONE);
                }
            }
        }

        mDragEnabled = favCount > 1;
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
        if (mainActivity.isViewingAllApps() && isExternalFavoriteBarEnabled()) {
            mainActivity.favoritesBar.setVisibility(View.GONE);
        }
    }

    /**
     * On first run, fill the favorite bar with sensible defaults
     */
    private void addDefaultAppsToFavs() {
        {
            // Default phone call app
            Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
            phoneIntent.setData(Uri.parse("tel:0000"));
            ResolveInfo resolveInfo = mainActivity.getPackageManager().resolveActivity(phoneIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null) {
                String packageName = resolveInfo.activityInfo.packageName;
                Log.i(TAG, "Dialer resolves to:" + packageName + "/" + resolveInfo.activityInfo.name);

                if (resolveInfo.activityInfo.name != null && !resolveInfo.activityInfo.name.equals(DEFAULT_RESOLVER)) {
                    String activityName = resolveInfo.activityInfo.name;
                    if (packageName.equals("com.google.android.dialer")) {
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
            // Default contacts app
            Intent contactsIntent = new Intent(Intent.ACTION_DEFAULT, ContactsContract.Contacts.CONTENT_URI);
            ResolveInfo resolveInfo = mainActivity.getPackageManager().resolveActivity(contactsIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null) {
                String packageName = resolveInfo.activityInfo.packageName;
                Log.i(TAG, "Contacts resolves to:" + packageName);
                if (resolveInfo.activityInfo.name != null && !resolveInfo.activityInfo.name.equals(DEFAULT_RESOLVER)) {
                    KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites(mainActivity, "app://" + packageName + "/" + resolveInfo.activityInfo.name);
                }
            }

        }
        {
            // Default browser
            Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://"));
            ResolveInfo resolveInfo = mainActivity.getPackageManager().resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null) {
                String packageName = resolveInfo.activityInfo.packageName;
                Log.i(TAG, "Browser resolves to:" + packageName);

                if (resolveInfo.activityInfo.name != null && !resolveInfo.activityInfo.name.equals(DEFAULT_RESOLVER)) {
                    String activityName = resolveInfo.activityInfo.name;
                    if (packageName.equalsIgnoreCase("com.android.chrome")) {
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

    @Override
    public void onClick(View v) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();
        viewHolder.result.fastLaunch(mainActivity, v);
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

    }

    @Override
    public boolean onLongClick(View v) {
        ViewHolder viewHolder = (ViewHolder) v.getTag();
        ListPopup popup = viewHolder.result.getPopupMenu(mainActivity, mainActivity.adapter, v);
        mainActivity.registerPopup(popup);
        popup.show(v);
        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        return true;
    }

    private boolean isExternalFavoriteBarEnabled() {
        return prefs.getBoolean("enable-favorites-bar", true);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            startTime = motionEvent.getEventTime();
            contextMenuShown = false;
            return true;
        }
        // No need to do the extra work
        if (isDragging) {
            return true;
        }

        // Click handlers first
        long holdTime = motionEvent.getEventTime() - startTime;
        if (holdTime < LONG_PRESS_DELAY && motionEvent.getAction() == MotionEvent.ACTION_UP) {
            this.onClick(view);
            return true;
        }
        if (!contextMenuShown && holdTime > LONG_PRESS_DELAY) {
            contextMenuShown = true;
            this.onLongClick(view);
            return true;
        }

        // Drag handlers
        int intCurrentY = Math.round(motionEvent.getY());
        int intCurrentX = Math.round(motionEvent.getX());
        int intStartY = motionEvent.getHistorySize() > 0 ? Math.round(motionEvent.getHistoricalY(0)) : intCurrentY;
        int intStartX = motionEvent.getHistorySize() > 0 ? Math.round(motionEvent.getHistoricalX(0)) : intCurrentX;
        boolean hasMoved = (Math.abs(intCurrentX - intStartX) > MOVE_SENSITIVITY) || (Math.abs(intCurrentY - intStartY) > MOVE_SENSITIVITY);

        if (hasMoved && mDragEnabled) {
            mDragEnabled = false;
            mainActivity.dismissPopup();
            mainActivity.closeContextMenu();
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            view.startDrag(null, shadowBuilder, view, 0);
            view.setVisibility(View.INVISIBLE);
            return true;
        }

        return false;
    }

    @Override
    public boolean onDrag(View v, final DragEvent event) {

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                isDragging = true;
                break;

            case DragEvent.ACTION_DRAG_ENTERED:
            case DragEvent.ACTION_DRAG_EXITED:
            case DragEvent.ACTION_DROP:
                if (!isDragging) {
                    return true;
                }
                overApp = (ViewHolder) v.getTag();
                currentX = (event.getX() != 0.0f) ? event.getX() : currentX;
                break;

            case DragEvent.ACTION_DRAG_ENDED:
                // Only need to handle this action once.
                if (!isDragging) {
                    return true;
                }
                isDragging = false;

                // Reset dragging to what it should be
                mDragEnabled = favCount > 1;

                final View draggedView = (View) event.getLocalState();

                // Sometimes we don't trigger onDrag over another app, in which case just drop.
                if (overApp == null) {
                    Log.w(TAG, "Wasn't dragged over an app, returning app to starting position");
                    draggedView.post(new Runnable() {
                        @Override
                        public void run() {
                            draggedView.setVisibility(View.VISIBLE);
                        }
                    });
                    break;
                }

                final ViewHolder draggedApp = (ViewHolder) draggedView.getTag();

                int left = v.getLeft();
                int right = v.getRight();
                int width = right - left;

                // currentX is relative to the view not the screen, so add the current X of the view.
                final boolean leftSide = (left + currentX < left + (width / 2));

                final int pos = KissApplication.getApplication(mainActivity).getDataHandler().getFavoritePosition(overApp.result.getPojoId());

                draggedView.post(() -> {
                    // Signals to a View that the drag and drop operation has concluded.
                    // If event result is set, this means the dragged view was dropped in target
                    if (event.getResult()) {
                        KissApplication.getApplication(mainActivity).getDataHandler().setFavoritePosition(mainActivity, draggedApp.result.getPojoId(), leftSide ? pos - 1 : pos);
                        mainActivity.onFavoriteChange();
                    } else {
                        draggedView.setVisibility(View.VISIBLE);
                    }
                });

                break;
            default:
                break;
        }
        return true;
    }
}

