package fr.neamar.kiss.forwarder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.DragEvent;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

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
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.UserHandle;

public class Favorites extends Forwarder implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener, View.OnDragListener {
    private static final String TAG = Favorites.class.getSimpleName();

    // Package used by Android when an Intent can be matched with more than one app
    private static final String DEFAULT_RESOLVER = "com.android.internal.app.ResolverActivity";

    /**
     * IDs for the favorites buttons
     */
    private List<ViewHolder> favoritesViews = new ArrayList<>();

    private static class ViewHolder {
        final View view;
        @NonNull
        final Result<?> result;
        @NonNull
        final Pojo pojo;

        ViewHolder(@NonNull Result<?> result, @NonNull Pojo pojo, @NonNull Context context, @NonNull ViewGroup parent) {
            this.result = result;
            this.pojo = pojo;
            view = result.inflateFavorite(context, parent);
            view.setTag(this);
        }

        boolean isReusable() {
            return !result.isDrawableDynamic();
        }
    }

    /**
     * Globals for drag and drop support
     */
    private static long startTime = 0; // Start of the drag and drop, used for long press menu

    // Use so we don't over process on the drag events.
    private boolean mDragEnabled = true;
    private boolean isDragging = false;
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

        notificationPrefs = mainActivity.getSharedPreferences(NotificationListener.NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);

        onFavoriteChange(false);
    }

    private ViewHolder findViewHolder(@NonNull Pojo pojo) {
        for (ViewHolder vh : favoritesViews) {
            if (vh.pojo == pojo && vh.isReusable()) {
                return vh;
            }
        }
        return null;
    }

    public void onFavoriteChange() {
        onFavoriteChange(false);
    }

    private void onFavoriteChange(boolean isRefresh) {
        List<Pojo> favoritesPojo = KissApplication.getApplication(mainActivity).getDataHandler().getFavorites();
        favCount = favoritesPojo.size();

        List<ViewHolder> holders = new ArrayList<>(favCount);
        ViewGroup favoritesBar = mainActivity.favoritesBar;
        if (isRefresh) {
            removeViews(favoritesBar, 0);
            favoritesViews.clear();
        }

        // Don't look for items after favIds length, we won't be able to display them
        for (int i = 0; i < favCount; i++) {
            Pojo favoritePojo = favoritesPojo.get(i);
            // Is there already a ViewHolder for this Pojo?
            ViewHolder viewHolder = findViewHolder(favoritePojo);
            if (viewHolder == null) {
                // If not, build a new one
                viewHolder = new ViewHolder(Result.fromPojo(mainActivity, favoritePojo), favoritePojo, mainActivity, mainActivity.favoritesBar);
            }
            viewHolder.view.setOnClickListener(this);
            viewHolder.view.setOnLongClickListener(this);
            viewHolder.view.setOnTouchListener(this);

            holders.add(viewHolder);

            // Check if view is different (we get null if beyond bounds, for instance when a new favorite was added)
            View currentView = favoritesBar.getChildAt(i);
            if (currentView != viewHolder.view) {
                if (viewHolder.view.getParent() != null) {
                    // We need to remove the view from its parent first
                    ((ViewGroup) viewHolder.view.getParent()).removeView(viewHolder.view);
                }
                favoritesBar.addView(viewHolder.view, i);
                // Remove old current view, which is now out of date. Useful for dynamic icons to ensure only the dynamic icon is updated, and everything else remains the same
                disposeOf(currentView);
                favoritesBar.removeView(currentView);
            }

            if (notificationPrefs != null) {
                int dotColor = isExternalFavoriteBarEnabled() ? UIColors.getNotificationDotColor(mainActivity) : Color.WHITE;

                ImageView notificationDot = viewHolder.view.findViewById(R.id.item_notification_dot);
                if (notificationDot == null)
                    // Notification-less favorites don't have a dot
                    continue;
                notificationDot.setColorFilter(dotColor);

                if (favoritePojo instanceof AppPojo) {
                    String packageKey = ((AppPojo) favoritePojo).getPackageKey();
                    notificationDot.setTag(packageKey);
                    notificationDot.setVisibility(notificationPrefs.contains(packageKey) ? View.VISIBLE : View.GONE);
                }
            }
        }

        removeViews(favoritesBar, favCount);

        // kepp viewholders in memory for future recycling
        favoritesViews = holders;
        mDragEnabled = favCount > 1;
    }

    private void removeViews(ViewGroup favoritesBar, int favCount) {
        // Remove any leftover views from previous renders
        while (favoritesBar.getChildCount() > favCount) {
            Log.d(TAG, "Disposing view");
            View toBeDisposed = favoritesBar.getChildAt(favCount);
            disposeOf(toBeDisposed);
            favoritesBar.removeViewAt(favCount);
        }
    }

    void disposeOf(@Nullable View toBeDisposed) {
        if (toBeDisposed == null) {
            return;
        }
        toBeDisposed.setOnClickListener(null);
        toBeDisposed.setOnLongClickListener(null);
        toBeDisposed.setOnTouchListener(null);
    }

    void updateSearchRecords(String query) {
        if (TextUtils.isEmpty(query)) {
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
        // How long to hold your finger in place to trigger the app menu.
        final int LONG_PRESS_DELAY = ViewConfiguration.getLongPressTimeout();

        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            startTime = motionEvent.getEventTime();
            return false;
        }
        // No need to do any extra work while dragging
        if (isDragging) {
            return false;
        }

        long holdTime = motionEvent.getEventTime() - startTime;

        if (holdTime > LONG_PRESS_DELAY) {
            // Long press, either drag or context menu
            int intCurrentY = Math.round(motionEvent.getY());
            int intCurrentX = Math.round(motionEvent.getX());
            int intStartY = motionEvent.getHistorySize() > 0 ? Math.round(motionEvent.getHistoricalY(0)) : intCurrentY;
            int intStartX = motionEvent.getHistorySize() > 0 ? Math.round(motionEvent.getHistoricalX(0)) : intCurrentX;

            // How much you need to move your finger to be considered "moving"
            int MOVE_SENSITIVITY = 8;
            boolean hasMoved = (Math.abs(intCurrentX - intStartX) > MOVE_SENSITIVITY) || (Math.abs(intCurrentY - intStartY) > MOVE_SENSITIVITY);

            if (hasMoved && mDragEnabled && !isDragging) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

                mDragEnabled = false;
                mainActivity.dismissPopup();
                mainActivity.closeContextMenu();

                mainActivity.favoritesBar.setOnDragListener(this);
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.setVisibility(View.INVISIBLE);
                isDragging = true;
                view.cancelLongPress();
                view.startDrag(null, shadowBuilder, view, 0);
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
                    // Do not update the ViewGroup during the event dispatch, older Androids will crash
                    // See #1419
                    bar.removeView(draggedView);
                    try {
                        bar.addView(draggedView, currentPos);
                    } catch (IllegalStateException e) {
                        // In some situations,
                        // removeView() somehow fails (this especially happens if you start the drag and immediately moves to the left or right)
                        // and we can't add the children back, because it still has a parent. In this case, do nothing, this should fix itself on the next iteration.
                        // This is very likely caused by animateLayoutChange, since disabling the property fixes the issue... but also removes all animation.
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
                final ViewHolder draggedApp = (ViewHolder) draggedView.getTag();
                int newIndex = potentialNewIndex;

                draggedView.post(() -> {
                    // Reset dragging to what it should be
                    // Need to be posted to avoid updating the ViewGroup during the event dispatch, older Androids will crash otherwise
                    // See #1419
                    draggedView.setVisibility(View.VISIBLE);

                    try {
                        if (newIndex == -1) {
                            // Sometimes we don't trigger onDrag over another app, in which case just drop.
                            Log.w(TAG, "Wasn't dragged over a favorite, returning app to starting position");
                            // We still need to refresh our favorites, in case one was removed and never added back (see IllegalStateException above)
                            KissApplication.getApplication(mainActivity).getDataHandler().refreshFavorites();
                        } else {
                            // Signals to a View that the drag and drop operation has concluded.
                            // If event result is set, this means the dragged view was dropped in target
                            if (event.getResult()) {
                                KissApplication.getApplication(mainActivity).getDataHandler().setFavoritePosition(mainActivity, draggedApp.result.getPojoId(), newIndex);
                            }
                        }
                    } catch (IllegalStateException e) {
                        // An animation was running. Retry later
                        // (to trigger: long press a favorite while already moving your finger a little, release as soon as you get haptic feedback)
                        draggedView.postDelayed(() -> KissApplication.getApplication(mainActivity).getDataHandler().refreshFavorites(), 300);
                    }
                });

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
            ComponentName componentName = getLaunchingComponent(phoneIntent);
            if (componentName != null) {
                Log.i(TAG, "Dialer resolves to: " + componentName);
                KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites("app://" + componentName.getPackageName() + "/" + componentName.getClassName());
            }
        }
        {
            // Default contacts app
            Intent contactsIntent = new Intent(Intent.ACTION_DEFAULT, ContactsContract.Contacts.CONTENT_URI);
            ComponentName componentName = getLaunchingComponent(contactsIntent);
            if (componentName != null) {
                Log.i(TAG, "Contacts resolves to: " + componentName);
                KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites("app://" + componentName.getPackageName() + "/" + componentName.getClassName());
            }
        }
        {
            // Default browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://"));
            ComponentName componentName = getLaunchingComponent(browserIntent);
            if (componentName != null) {
                Log.i(TAG, "Browser resolves to: " + componentName);
                KissApplication.getApplication(mainActivity).getDataHandler().addToFavorites("app://" + componentName.getPackageName() + "/" + componentName.getClassName());
            }
        }
    }

    /**
     * @param intent intent
     * @return launching component for given intent
     */
    @Nullable
    private ComponentName getLaunchingComponent(Intent intent) {
        ComponentName componentName = PackageManagerUtils.getComponentName(mainActivity, intent);
        ComponentName launchingComponent = PackageManagerUtils.getLaunchingComponent(mainActivity, componentName, UserHandle.OWNER);
        if (launchingComponent != null && !launchingComponent.getClassName().equals(DEFAULT_RESOLVER)) {
            return launchingComponent;
        }
        return null;
    }

    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        onFavoriteChange(true);
    }

}

