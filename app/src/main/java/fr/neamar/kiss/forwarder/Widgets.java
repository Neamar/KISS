package fr.neamar.kiss.forwarder;

import static android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL;
import static android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.text.TextUtils;
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
        for (int i = 0; i < widgetArea.getChildCount(); i++) {
            if (((AppWidgetHostView) widgetArea.getChildAt(i)).getAppWidgetId() == pendingAppWidgetId) {
                widgetArea.removeViewAt(i);
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
        List<String> builder = new ArrayList<>(widgetArea.getChildCount());
        for (int i = 0; i < widgetArea.getChildCount(); i++) {
            AppWidgetHostView view = (AppWidgetHostView) widgetArea.getChildAt(i);
            int appWidgetId = view.getAppWidgetId();
            AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
            if (appWidgetInfo != null) {
                int lineSize = getLineSize(view.getLayoutParams().height);
                builder.add(appWidgetId + "-" + lineSize);
            } else {
                Log.w(TAG, "Unable to retrieve widget by id " + appWidgetId);
            }
        }

        String pref = TextUtils.join(";", builder);
        prefs.edit().putString(WIDGET_PREF_KEY, pref).apply();
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
        String widgetsConfString = prefs.getString(WIDGET_PREF_KEY, "");
        String[] widgetsConf = widgetsConfString.split(";");
        Set<Integer> idsUsed = new HashSet<>();
        for (String widgetConf : widgetsConf) {
            if (widgetConf.isEmpty()) {
                continue;
            }
            String[] conf = widgetConf.split("-");
            int id = Integer.parseInt(conf[0]);
            int lineSize = Integer.parseInt(conf[1]);
            idsUsed.add(id);
            addWidget(id, lineSize);
        }

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
    }

    /**
     * Retrieve a view for specified widget,
     * add context menu to it
     *
     * @param appWidgetId id of widget to add
     * @param lineSize    height of widget given in lines
     */
    private void addWidget(int appWidgetId, int lineSize) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo == null) {
            Log.i(TAG, "Unable to retrieve widget by id " + appWidgetId);
            return;
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

        widgetArea.addView(hostView);
    }

    private void buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, AppWidgetProviderInfo currentAppWidgetInfo, AppWidgetHostView widgetWithMenuCurrentlyDisplayed) {
        final ViewGroup parent = (ViewGroup) widgetWithMenuCurrentlyDisplayed.getParent();

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
        if (parent.indexOfChild(widgetWithMenuCurrentlyDisplayed) != 0) {
            adapter.add(new ListPopup.Item(context, R.string.menu_widget_move_up));
        }
        if (parent.indexOfChild(widgetWithMenuCurrentlyDisplayed) != parent.getChildCount() - 1) {
            adapter.add(new ListPopup.Item(context, R.string.menu_widget_move_down));
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
            mAppWidgetHost.deleteAppWidgetId(widgetWithMenuCurrentlyDisplayed.getAppWidgetId());
            serializeState();
        } else if (stringId == R.string.menu_size_up || stringId == R.string.menu_size_down) {
            int lineSize = getLineSize(widgetWithMenuCurrentlyDisplayed.getLayoutParams().height);
            setWidgetSize(widgetWithMenuCurrentlyDisplayed, stringId == R.string.menu_size_up ? lineSize + 1 : lineSize - 1);
            serializeState();
        } else if (stringId == R.string.menu_widget_move_up) {
            int currentIndex = parent.indexOfChild(widgetWithMenuCurrentlyDisplayed);
            if (currentIndex >= 1) {
                parent.removeViewAt(currentIndex);
                parent.addView(widgetWithMenuCurrentlyDisplayed, currentIndex - 1);
                serializeState();
            }
        } else if (stringId == R.string.menu_widget_move_down) {
            int currentIndex = parent.indexOfChild(widgetWithMenuCurrentlyDisplayed);
            if (currentIndex < parent.getChildCount() - 1) {
                parent.removeViewAt(currentIndex);
                parent.addView(widgetWithMenuCurrentlyDisplayed, currentIndex + 1);
                serializeState();
            }
        }
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        params.bottomMargin = getWidgetSpacingPx();
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

        addWidget(appWidgetId, Math.max(INITIAL_WIDGET_LINE_SIZE, getLineSize(getMinHeight(appWidgetInfo))));
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
            mAppWidgetHost.startAppWidgetConfigureActivityForResult(mainActivity, appWidgetId, 0, requestCode, null);
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
        mAppWidgetHost.stopListening();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            restoreWidgets();
        }
    }
}
