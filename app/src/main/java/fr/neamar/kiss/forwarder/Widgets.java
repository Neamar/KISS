package fr.neamar.kiss.forwarder;

import static android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL;
import static android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE;

import android.app.Activity;
import android.app.ActivityOptions;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.PickAppWidgetActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.ui.WidgetHost;
import fr.neamar.kiss.ui.WidgetScrollView;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.Log;
import fr.neamar.kiss.utils.WidgetUtils;

class Widgets extends Forwarder {
    private static final String TAG = Widgets.class.getSimpleName();
    private static final int REQUEST_APPWIDGET_CONFIGURED = 5;
    private static final int REQUEST_APPWIDGET_RECONFIGURED = 13;

    private static final int APPWIDGET_HOST_ID = 442;

    private static final String WIDGET_PREF_KEY = "widgets-conf";

    private static final int INITIAL_WIDGET_LINE_SIZE = 2;

    /**
     * Preference key: vertical spacing between widgets, in dp
     */
    private static final String PREF_WIDGET_SPACING = "widget-spacing";

    /**
     * Widgets fields
     */
    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;

    /**
     * View widgets are added to
     */
    private ViewGroup widgetArea;
    /**
     * Scrollable container around {@link #widgetArea}
     */
    private WidgetScrollView widgetScroll;
    /**
     * Id of the widget currently being added, to delete it if the user cancels:
     * a cancelled bind request or configuration may not return the id.
     */
    private int pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    /**
     * All MainActivity instances share the same host id, and a finished one is destroyed
     * after its replacement started listening: only the latest one may stop listening.
     */
    private static WeakReference<Widgets> listeningInstance = new WeakReference<>(null);
    private ActivityResultLauncher<Intent> requestAppWidgetPicked;
    private ActivityResultLauncher<Intent> requestAppWidgetBound;

    Widgets(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        // Initialize widget manager and host, restore widgets
        mAppWidgetManager = AppWidgetManager.getInstance(mainActivity);
        mAppWidgetHost = new WidgetHost(mainActivity, APPWIDGET_HOST_ID, this::onAppWidgetRemoved);
        widgetArea = mainActivity.findViewById(R.id.widgetLayout);
        widgetScroll = mainActivity.findViewById(R.id.widgetScroll);

        requestAppWidgetPicked = mainActivity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::appWidgetPicked);
        requestAppWidgetBound = mainActivity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::appWidgetBound);

        restoreWidgets();
    }

    /**
     * Forward gestures performed on empty areas of the widget scroll area to
     * the given listener, so they behave exactly as on a non-widget area.
     */
    void setEmptyAreaTouchListener(View.OnTouchListener listener) {
        widgetScroll.setEmptyAreaTouchListener(listener);
    }

    /**
     * Effective spacing between widgets in pixels.
     */
    private int getWidgetSpacingPx() {
        try {
            return DrawableUtils.dpToPx(mainActivity, Integer.parseInt(prefs.getString(PREF_WIDGET_SPACING, "0")));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void onAppWidgetRemoved() {
        restoreWidgets();
        serializeState();
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_APPWIDGET_CONFIGURED && resultCode == Activity.RESULT_CANCELED) {
            removePendingWidget();
        }
    }

    private void appWidgetPicked(ActivityResult result) {
        Intent data = result.getData();
        if (result.getResultCode() != Activity.RESULT_OK || data == null) {
            return;
        }
        ComponentName provider = data.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER);
        UserHandle profile = data.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE);
        // allocate the id only once a widget is picked:
        // an id without provider breaks accessibility services on Android 10
        pendingAppWidgetId = mAppWidgetHost.allocateAppWidgetId();
        data.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingAppWidgetId);
        if (mAppWidgetManager.bindAppWidgetIdIfAllowed(pendingAppWidgetId, profile, provider, null)) {
            addAppWidget(data);
        } else {
            Log.w(TAG, "Widget bind not allowed");
            requestBindWidget(data);
        }
    }

    private void appWidgetBound(ActivityResult result) {
        Intent data = result.getData();
        if (result.getResultCode() == Activity.RESULT_OK && data != null) {
            addAppWidget(data);
        } else {
            // if bind was not allowed, delete the widget
            removePendingWidget();
        }
    }

    /**
     * Delete the widget currently being added, and its view if it was already displayed
     */
    private void removePendingWidget() {
        boolean removed = false;
        for (int i = 0; i < widgetArea.getChildCount(); i++) {
            ViewGroup row = (ViewGroup) widgetArea.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                if (((AppWidgetHostView) row.getChildAt(j)).getAppWidgetId() == pendingAppWidgetId) {
                    row.removeViewAt(j);
                    if (row.getChildCount() == 0) {
                        widgetArea.removeViewAt(i);
                    }
                    removed = true;
                    break;
                }
            }
            if (removed) {
                break;
            }
        }
        mAppWidgetHost.deleteAppWidgetId(pendingAppWidgetId);
        serializeState();
    }

    boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_widget) {
            // request widget picker, a selection will lead to a call of appWidgetPicked
            requestAppWidgetPicked.launch(new Intent(mainActivity, PickAppWidgetActivity.class));
            return true;
        }

        return false;
    }

    void onCreateContextMenu(ContextMenu menu) {
        if (!prefs.getBoolean("history-hide", false)) {
            menu.findItem(R.id.add_widget).setVisible(false);
        }
    }

    void onDataSetChanged() {
        if (widgetArea.getChildCount() > 0 && mainActivity.adapter.isEmpty()) {
            // when a widget is displayed the empty list would prevent touches on the widget
            mainActivity.emptyListView.setVisibility(View.GONE);
        }
    }

    private void serializeState() {
        List<List<WidgetUtils.WidgetConf>> layout = new ArrayList<>();
        for (List<AppWidgetHostView> row : getWidgetRows()) {
            List<WidgetUtils.WidgetConf> rowConfig = new ArrayList<>(row.size());
            for (AppWidgetHostView view : row) {
                int appWidgetId = view.getAppWidgetId();
                AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
                if (appWidgetInfo != null) {
                    rowConfig.add(new WidgetUtils.WidgetConf(appWidgetId, getLineSize(view.getLayoutParams().height)));
                } else {
                    Log.w(TAG, "Unable to retrieve widget by id " + appWidgetId);
                }
            }
            if (!rowConfig.isEmpty()) {
                layout.add(rowConfig);
            }
        }

        prefs.edit().putString(WIDGET_PREF_KEY, WidgetUtils.serializeLayout(layout)).apply();
    }

    /**
     * Display all widgets based on state
     */
    private void restoreWidgets() {
        // only add widgets if in minimal mode
        if (!prefs.getBoolean("history-hide", false)) {
            return;
        }

        mAppWidgetHost.stopListening();

        // remove empty list view when using widgets, this would block touches on the widget
        mainActivity.emptyListView.setVisibility(View.GONE);
        widgetArea.removeAllViews();
        List<List<WidgetUtils.WidgetConf>> layout = WidgetUtils.parseLayout(prefs.getString(WIDGET_PREF_KEY, ""));
        List<List<AppWidgetHostView>> rows = new ArrayList<>(layout.size());
        Set<Integer> idsUsed = new HashSet<>();
        for (List<WidgetUtils.WidgetConf> rowConfig : layout) {
            List<AppWidgetHostView> row = new ArrayList<>(rowConfig.size());
            for (WidgetUtils.WidgetConf widgetConf : rowConfig) {
                idsUsed.add(widgetConf.appWidgetId);
                AppWidgetHostView hostView = createWidgetView(widgetConf.appWidgetId, widgetConf.lineSize);
                if (hostView != null) {
                    row.add(hostView);
                }
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        applyRows(rows);

        // kill zombie widgets
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int[] hostWidgetIds = mAppWidgetHost.getAppWidgetIds();
            for (int hostWidgetId : hostWidgetIds) {
                if (!idsUsed.contains(hostWidgetId)) {
                    mAppWidgetHost.deleteAppWidgetId(hostWidgetId);
                }
            }
        }

        // Start listening for widget update
        mAppWidgetHost.startListening();
        listeningInstance = new WeakReference<>(this);
    }

    private List<List<AppWidgetHostView>> getWidgetRows() {
        List<List<AppWidgetHostView>> rows = new ArrayList<>(widgetArea.getChildCount());
        for (int i = 0; i < widgetArea.getChildCount(); i++) {
            ViewGroup rowView = (ViewGroup) widgetArea.getChildAt(i);
            List<AppWidgetHostView> row = new ArrayList<>(rowView.getChildCount());
            for (int j = 0; j < rowView.getChildCount(); j++) {
                row.add((AppWidgetHostView) rowView.getChildAt(j));
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        return rows;
    }

    private void applyRows(List<List<AppWidgetHostView>> rows) {
        for (int i = 0; i < widgetArea.getChildCount(); i++) {
            ((ViewGroup) widgetArea.getChildAt(i)).removeAllViews();
        }
        widgetArea.removeAllViews();

        for (List<AppWidgetHostView> row : rows) {
            if (row.isEmpty()) {
                continue;
            }
            LinearLayout rowView = createWidgetRow();
            for (AppWidgetHostView widgetView : row) {
                rowView.addView(widgetView);
            }
            widgetArea.addView(rowView);
        }
    }

    private LinearLayout createWidgetRow() {
        LinearLayout row = new LinearLayout(mainActivity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = getWidgetSpacingPx();
        row.setLayoutParams(params);
        return row;
    }

    /**
     * Retrieve a view for specified widget,
     * add context menu to it
     *
     * @param appWidgetId id of widget to add
     * @param lineSize    height of widget given in lines
     */
    @Nullable
    private AppWidgetHostView createWidgetView(int appWidgetId, int lineSize) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo == null) {
            Log.i(TAG, "Unable to retrieve widget by id " + appWidgetId);
            return null;
        }

        AppWidgetHostView hostView = mAppWidgetHost.createView(mainActivity.getApplicationContext(), appWidgetId, appWidgetInfo);

        hostView.setMinimumWidth(Math.min(appWidgetInfo.minWidth, appWidgetInfo.minResizeWidth));
        setWidgetSize(hostView, lineSize);

        hostView.setLongClickable(true);
        hostView.setOnLongClickListener(v -> {
            final AppWidgetHostView widgetWithMenuCurrentlyDisplayed = (AppWidgetHostView) v;
            final AppWidgetProviderInfo currentAppWidgetInfo = mAppWidgetManager.getAppWidgetInfo(widgetWithMenuCurrentlyDisplayed.getAppWidgetId());

            ArrayAdapter<ListPopup.Item> popupMenuAdapter = new ArrayAdapter<>(mainActivity, R.layout.popup_list_item);
            buildPopupMenu(mainActivity, popupMenuAdapter, currentAppWidgetInfo, widgetWithMenuCurrentlyDisplayed);
            ListPopup popupMenu = new ListPopup(mainActivity);
            popupMenu.setAdapter(popupMenuAdapter);
            popupMenu.setOnItemClickListener((adapter, view, position) -> {
                @StringRes int stringId = ((ListPopup.Item) adapter.getItem(position)).stringId;
                popupMenuClickHandler(stringId, widgetWithMenuCurrentlyDisplayed);
            });
            mainActivity.registerPopup(popupMenu);
            popupMenu.show(hostView);
            return true;
        });

        return hostView;
    }

    private void buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, AppWidgetProviderInfo currentAppWidgetInfo, AppWidgetHostView widgetWithMenuCurrentlyDisplayed) {
        List<List<AppWidgetHostView>> rows = getWidgetRows();

        if (isReconfigurable(currentAppWidgetInfo)) {
            adapter.add(new ListPopup.Item(context, R.string.menu_widget_settings));
        }
        int lineSize = getLineSize(widgetWithMenuCurrentlyDisplayed.getLayoutParams().height);
        if (canGrowTo(lineSize + 1, currentAppWidgetInfo)) {
            adapter.add(new ListPopup.Item(context, R.string.menu_size_up));
        }
        if (canShrinkTo(lineSize - 1, currentAppWidgetInfo)) {
            adapter.add(new ListPopup.Item(context, R.string.menu_size_down));
        }
        if (WidgetUtils.canMoveUp(rows, widgetWithMenuCurrentlyDisplayed)) {
            adapter.add(new ListPopup.Item(context, R.string.menu_widget_move_up));
        }
        if (WidgetUtils.canMoveDown(rows, widgetWithMenuCurrentlyDisplayed)) {
            adapter.add(new ListPopup.Item(context, R.string.menu_widget_move_down));
        }
        boolean rtl = isRtl();
        if (rtl ? WidgetUtils.canMoveRight(rows, widgetWithMenuCurrentlyDisplayed) : WidgetUtils.canMoveLeft(rows, widgetWithMenuCurrentlyDisplayed)) {
            adapter.add(new ListPopup.Item(context, R.string.menu_widget_move_left));
        }
        if (rtl ? WidgetUtils.canMoveLeft(rows, widgetWithMenuCurrentlyDisplayed) : WidgetUtils.canMoveRight(rows, widgetWithMenuCurrentlyDisplayed)) {
            adapter.add(new ListPopup.Item(context, R.string.menu_widget_move_right));
        }
        adapter.add(new ListPopup.Item(context, R.string.menu_widget_remove));
    }

    private void popupMenuClickHandler(@StringRes int stringId, AppWidgetHostView widgetWithMenuCurrentlyDisplayed) {
        final ViewGroup parent = (ViewGroup) widgetWithMenuCurrentlyDisplayed.getParent();
        if (parent == null) {
            // widget was detached between long-press and menu-item click
            return;
        }
        if (stringId == R.string.menu_widget_settings) {
            reConfigureAppWidget(widgetWithMenuCurrentlyDisplayed.getAppWidgetId());
        } else if (stringId == R.string.menu_widget_remove) {
            parent.removeView(widgetWithMenuCurrentlyDisplayed);
            if (parent.getChildCount() == 0) {
                widgetArea.removeView(parent);
            }
            mAppWidgetHost.deleteAppWidgetId(widgetWithMenuCurrentlyDisplayed.getAppWidgetId());
            serializeState();
        } else if (stringId == R.string.menu_size_up || stringId == R.string.menu_size_down) {
            int lineSize = getLineSize(widgetWithMenuCurrentlyDisplayed.getLayoutParams().height);
            setWidgetSize(widgetWithMenuCurrentlyDisplayed, stringId == R.string.menu_size_up ? lineSize + 1 : lineSize - 1);
            serializeState();
        } else if (stringId == R.string.menu_widget_move_up) {
            moveWidget(widgetWithMenuCurrentlyDisplayed, WidgetUtils::moveUp);
        } else if (stringId == R.string.menu_widget_move_down) {
            moveWidget(widgetWithMenuCurrentlyDisplayed, WidgetUtils::moveDown);
        } else if (stringId == R.string.menu_widget_move_left) {
            moveWidget(widgetWithMenuCurrentlyDisplayed, isRtl() ? WidgetUtils::moveRight : WidgetUtils::moveLeft);
        } else if (stringId == R.string.menu_widget_move_right) {
            moveWidget(widgetWithMenuCurrentlyDisplayed, isRtl() ? WidgetUtils::moveLeft : WidgetUtils::moveRight);
        }
    }

    private void moveWidget(AppWidgetHostView widget, WidgetMover mover) {
        List<List<AppWidgetHostView>> rows = getWidgetRows();
        if (mover.move(rows, widget)) {
            applyRows(rows);
            serializeState();
        }
    }

    private boolean isRtl() {
        return mainActivity.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    @FunctionalInterface
    private interface WidgetMover {
        boolean move(List<List<AppWidgetHostView>> rows, AppWidgetHostView widget);
    }

    /**
     * Set new height to host view of widget.
     *
     * @param hostView host view for widget
     * @param lineSize height of widget given in lines
     */
    private void setWidgetSize(AppWidgetHostView hostView, int lineSize) {
        int height = (int) (lineSize * getLineHeight());
        hostView.setMinimumHeight(height);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, height, 1f);
        hostView.setLayoutParams(params);
    }

    /**
     * @return true, if widget can be shrunk to given number of lines
     */
    private boolean canShrinkTo(int lineSize, AppWidgetProviderInfo appWidgetInfo) {
        return lineSize > 0 && appWidgetInfo != null && (int) (lineSize * getLineHeight()) >= Math.min(getMinHeight(appWidgetInfo), appWidgetInfo.minResizeHeight);
    }

    /**
     * @return true, if widget can be grown to given number of lines
     */
    private boolean canGrowTo(int lineSize, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return appWidgetInfo.maxResizeHeight < appWidgetInfo.minHeight || lineSize <= getLineSize(appWidgetInfo.maxResizeHeight);
        }
        return true;
    }

    /**
     * Scroll the given widget into view, centered vertically.
     *
     * Uses a one-shot layout listener instead of {@link View#post(Runnable)} to
     * guarantee the child has been measured and positioned before computing the
     * scroll target ({@code view.getBottom()} is stale before layout).
     */
    private void scrollIntoView(View view) {
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                v.removeOnLayoutChangeListener(this);
                widgetScroll.smoothScrollTo(0,
                        v.getBottom() - (widgetScroll.getHeight() - v.getHeight()) / 2);
            }
        });
    }

    private void requestBindWidget(@NonNull Intent data) {
        final int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        final ComponentName provider = data.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER);
        final UserHandle profile = data.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "asking for permission");

            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, profile);
            requestAppWidgetBound.launch(intent);
        }, 500);
    }

    /**
     * Adds widget to Activity and persists it in prefs to be able to restore it,
     * then display its configuration view if necessary
     *
     * @param data Intent holding widget id to add
     */
    private void addAppWidget(Intent data) {
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo == null) {
            Log.w(TAG, "Add widget not possible");
            return;
        }

        AppWidgetHostView hostView = createWidgetView(appWidgetId, Math.max(INITIAL_WIDGET_LINE_SIZE, getLineSize(getMinHeight(appWidgetInfo))));
        if (hostView == null) {
            removePendingWidget();
            return;
        }
        List<List<AppWidgetHostView>> rows = getWidgetRows();
        List<AppWidgetHostView> row = new ArrayList<>();
        row.add(hostView);
        rows.add(row);
        applyRows(rows);
        serializeState();

        // Scroll the new widget into view.  When content fits the viewport
        // this is a no-op; when it overflows the scroll view shows the widget.
        scrollIntoView(widgetArea.getChildAt(widgetArea.getChildCount() - 1));

        if (!isConfigurationOptional(appWidgetInfo)) {
            configureAppWidget(appWidgetId, appWidgetInfo, REQUEST_APPWIDGET_CONFIGURED);
        }
    }

    private void reConfigureAppWidget(int appWidgetId) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        configureAppWidget(appWidgetId, appWidgetInfo, REQUEST_APPWIDGET_RECONFIGURED);
    }

    private void configureAppWidget(int appWidgetId, AppWidgetProviderInfo appWidgetInfo, int requestCode) {
        if (appWidgetInfo != null && appWidgetInfo.configure != null) {
            Bundle options = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // the configure activity is started through a system PendingIntent, which needs our permission to start activities
                options = ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                        .toBundle();
            }
            mAppWidgetHost.startAppWidgetConfigureActivityForResult(mainActivity, appWidgetId, 0, requestCode, options);
        }
    }

    /**
     * A widget's configuration is optional only if it's configuration is marked as optional AND
     * it can be reconfigured later.
     *
     * @param appWidgetInfo
     * @return true, if configuration is optional
     */
    private boolean isConfigurationOptional(@NonNull AppWidgetProviderInfo appWidgetInfo) {
        return isReconfigurable(appWidgetInfo) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && (appWidgetInfo.widgetFeatures & WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0;
    }

    /**
     * @param appWidgetInfo
     * @return true, if widget can be reconfigured
     */
    private boolean isReconfigurable(@NonNull AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo.configure != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int featureFlags = appWidgetInfo.widgetFeatures;
            return (featureFlags & WIDGET_FEATURE_RECONFIGURABLE) != 0;
        } else {
            return false;
        }
    }

    /**
     * @param height
     * @return calculated line size of given height
     */
    private int getLineSize(int height) {
        return WidgetUtils.getLineSize(height, getLineHeight());
    }

    /**
     * @return line height in pixel
     */
    private float getLineHeight() {
        return DrawableUtils.dpToPx(mainActivity, 50);
    }

    private int getMinHeight(AppWidgetProviderInfo appWidgetInfo) {
        int targetCellHeight = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            targetCellHeight = appWidgetInfo.targetCellHeight;
        }
        return WidgetUtils.getMinHeight(appWidgetInfo.minHeight, targetCellHeight, getLineHeight());
    }

    public void onDestroy() {
        if (listeningInstance.get() == this) {
            listeningInstance.clear();
            mAppWidgetHost.stopListening();
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            restoreWidgets();
        }
    }
}
