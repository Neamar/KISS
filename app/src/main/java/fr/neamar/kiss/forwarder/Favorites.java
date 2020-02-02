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
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.notification.NotificationListener;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.Pojo;
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
        @NonNull
        final View view;
        @NonNull
        final Result result;
        @NonNull
        final Pojo pojo;

        ViewHolder(@NonNull Result result, @NonNull Pojo pojo, @NonNull Context context, @NonNull ViewGroup parent) {
            this.result = result;
            this.pojo = pojo;
            view = result.inflateFavorite(context, null, parent);
            view.setTag(this);
        }
    }

    /**
     * Globals for drag and drop support
     */
    private static long startTime = 0; // Start of the drag and drop, used for long press menu

    // Use so we don't over process on the drag events.
    private boolean mDragEnabled = true;
    private boolean isDragging = false;
    private boolean contextMenuShown = false;
    private int potentialNewIndex = -1;
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

        if (prefs.getBoolean("first-run-favorites", true)) {
            // It is the first run. Make sure this is not an update by checking if history is empty
            if (DBHelper.getHistoryLength(mainActivity) == 0) {
                addDefaultAppsToFavs();
            }
            // set flag to false
            prefs.edit().putBoolean("first-run-favorites", false).apply();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            notificationPrefs = mainActivity.getSharedPreferences(NotificationListener.NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);
        }

    }

    private ViewHolder findViewHolder(@NonNull Pojo pojo) {
        for (ViewHolder vh : favoritesViews) {
            if (vh.pojo == pojo) {
                return vh;
            }
        }
        return null;
    }

    void onFavoriteChange() {
        ArrayList<Pojo> favoritesPojo = KissApplication.getApplication(mainActivity).getDataHandler().getFavorites();

        favCount = favoritesPojo.size();

        ArrayList<ViewHolder> holders = new ArrayList<>(favCount);

        ViewGroup favoritesBar = mainActivity.favoritesBar;

        // Don't look for items after favIds length, we won't be able to display them
        for (int i = 0; i < favCount; i++) {
            Pojo favoritePojo = favoritesPojo.get(i);
            // Is there already a ViewHolder for this Pojo?
            ViewHolder viewHolder = findViewHolder(favoritePojo);
            if (viewHolder == null) {
                // If not, build a new one
                viewHolder = new ViewHolder(Result.fromPojo(mainActivity, favoritePojo), favoritePojo, mainActivity, mainActivity.favoritesBar);
                viewHolder.view.setOnDragListener(this);
                viewHolder.view.setOnTouchListener(this);
            }
            holders.add(viewHolder);

            // Check if view is different (we get null if beyond bounds, for instance when a new favorite was added)
            View currentView = favoritesBar.getChildAt(i);
            if (currentView != viewHolder.view) {
                if (viewHolder.view.getParent() != null) {
                    // We need to remove the view from its parent first
                    ((ViewGroup) viewHolder.view.getParent()).removeView(viewHolder.view);
                }
                favoritesBar.addView(viewHolder.view, i);
            }

            if (notificationPrefs != null) {
                int dotColor = isExternalFavoriteBarEnabled() ? UIColors.getPrimaryColor(mainActivity) : Color.WHITE;

                ImageView notificationDot = viewHolder.view.findViewById(R.id.item_notification_dot);
                if (notificationDot == null)
                    // Notification-less favorites don't have a dot
                    continue;
                notificationDot.setColorFilter(dotColor);

                if (favoritePojo instanceof AppPojo) {
                    String packageName = ((AppPojo) favoritePojo).packageName;
                    notificationDot.setTag(packageName);
                    notificationDot.setVisibility(notificationPrefs.contains(packageName) ? View.VISIBLE : View.GONE);
                }
            }
        }

        // Remove any leftover views from previous renders
        while (favoritesBar.getChildCount() > favCount) {
            View toBeDisposed = favoritesBar.getChildAt(favCount);
            toBeDisposed.setOnDragListener(null);
            toBeDisposed.setOnTouchListener(null);
            favoritesBar.removeViewAt(favCount);
        }

        // kepp viewholders in memory for future recycling
        favoritesViews = holders;
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
        // How long to hold your finger in place to trigger the app menu.
        int LONG_PRESS_DELAY = 250;
        if (holdTime < LONG_PRESS_DELAY && motionEvent.getAction() == MotionEvent.ACTION_UP) {
            this.onClick(view);
            view.performClick();
            return true;
        }

        if (holdTime > LONG_PRESS_DELAY) {
            // Long press, either drag or context menu

            // Drag handlers
            int intCurrentY = Math.round(motionEvent.getY());
            int intCurrentX = Math.round(motionEvent.getX());
            int intStartY = motionEvent.getHistorySize() > 0 ? Math.round(motionEvent.getHistoricalY(0)) : intCurrentY;
            int intStartX = motionEvent.getHistorySize() > 0 ? Math.round(motionEvent.getHistoricalX(0)) : intCurrentX;

            // How much you need to move your finger to be considered "moving"
            int MOVE_SENSITIVITY = 8;
            boolean hasMoved = (Math.abs(intCurrentX - intStartX) > MOVE_SENSITIVITY) || (Math.abs(intCurrentY - intStartY) > MOVE_SENSITIVITY);

            if (hasMoved && mDragEnabled && !isDragging) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

                if (contextMenuShown) {
                    mainActivity.dismissPopup();
                }

                mDragEnabled = false;
                mainActivity.dismissPopup();
                mainActivity.closeContextMenu();

                mainActivity.favoritesBar.setOnDragListener(this);
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.setVisibility(View.INVISIBLE);
                isDragging = true;
                view.startDrag(null, shadowBuilder, view, 0);
                return true;
            } else if (!contextMenuShown && !isDragging) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

                contextMenuShown = true;
                this.onLongClick(view);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onDrag(View targetView, final DragEvent event) {
        final View draggedView = (View) event.getLocalState();

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return targetView instanceof LinearLayout;
            case DragEvent.ACTION_DRAG_ENTERED:
                return isDragging;
            case DragEvent.ACTION_DRAG_LOCATION:
                ViewGroup bar = ((ViewGroup) targetView);
                float x = event.getX();
                int width = targetView.getWidth();

                int currentPos = (int) (favCount * x / width);

                View currentChildAtPos = bar.getChildAt(currentPos);
                if (currentChildAtPos != draggedView) {
                    bar.removeView(draggedView);
                    try {
                        bar.addView(draggedView, currentPos);
                    } catch (IllegalStateException e) {
                        // In some situations,
                        // removeView() somehow fails (this especially happens if you start the drag and immediately moves to the left or right)
                        // and we can't add the children back, because it still has a parent. In this case, do nothing, this should fix itself on the next iteration.
                        potentialNewIndex = -1;
                        return false;
                    }
                }

                potentialNewIndex = currentPos;

                return true;
            case DragEvent.ACTION_DROP:
                // Accept the drop, will be followed by ACTION_DRAG_ENDED
                return isDragging;
            case DragEvent.ACTION_DRAG_ENDED:
                // Sometimes we don't trigger onDrag over another app, in which case just drop.
                if (potentialNewIndex == -1) {
                    Log.w(TAG, "Wasn't dragged over a favorite, returning app to starting position");
                } else {
                    final ViewHolder draggedApp = (ViewHolder) draggedView.getTag();
                    int newIndex = potentialNewIndex;

                    draggedView.post(() -> {
                        // Signals to a View that the drag and drop operation has concluded.
                        // If event result is set, this means the dragged view was dropped in target
                        if (event.getResult()) {
                            KissApplication.getApplication(mainActivity).getDataHandler().setFavoritePosition(mainActivity, draggedApp.result.getPojoId(), newIndex);
                            mainActivity.onFavoriteChange();
                        }
                    });
                }

                // Reset dragging to what it should be
                draggedView.setVisibility(View.VISIBLE);
                mDragEnabled = favCount > 1;
                potentialNewIndex = -1;
                isDragging = false;
                return true;
            default:
                break;
        }
        return isDragging;
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
                    KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites("app://" + packageName + "/" + activityName);
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
                    KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites("app://" + packageName + "/" + resolveInfo.activityInfo.name);
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
                    KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites("app://" + packageName + "/" + activityName);
                }
            }
        }
        mainActivity.onFavoriteChange();
    }
}

