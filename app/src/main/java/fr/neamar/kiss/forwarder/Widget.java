package fr.neamar.kiss.forwarder;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.Map;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.ui.WidgetLayout;
import fr.neamar.kiss.ui.WidgetPreferences;

public class Widget extends Forwarder implements ListPopup.OnItemClickListener {
    public static final int REQUEST_REFRESH_APPWIDGET = 10;
    private static final int REQUEST_PICK_APPWIDGET = 9;
    private static final int REQUEST_CREATE_APPWIDGET = 5;

    private static final int APPWIDGET_HOST_ID = 442;
    private static final String WIDGET_PREFERENCE_ID = "fr.neamar.kiss.widgetprefs";

    private SharedPreferences widgetPrefs;

    /**
     * Widget fields
     */
    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;

    /**
     * View widgets are added to
     */
    private WidgetLayout widgetArea;

    Widget(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        // Initialize widget manager and host, restore widgets
        widgetPrefs = mainActivity.getSharedPreferences(WIDGET_PREFERENCE_ID, Context.MODE_PRIVATE);

        mAppWidgetManager = AppWidgetManager.getInstance(mainActivity);
        mAppWidgetHost = new AppWidgetHost(mainActivity, APPWIDGET_HOST_ID);
        widgetArea = mainActivity.findViewById(R.id.widgetLayout);
        widgetArea.setWidgetForwarder(this);

        restoreWidgets();
    }

    void onStart() {
        // Start listening for widget update
        mAppWidgetHost.startListening();
    }

    void onStop() {
        // Stop listening for widget update
        mAppWidgetHost.stopListening();
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CREATE_APPWIDGET:
                    addAppWidget(data);
                    break;
                case REQUEST_PICK_APPWIDGET:
                    configureAppWidget(data);
                    break;
                case REQUEST_REFRESH_APPWIDGET:
                    refreshAppWidget(data);
                    break;
            }
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            //if widget was not selected, delete id
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.widget) {

            if (getWidgetHostViewCount() == 0) {
                if (canAddWidget()) {
                    // request widget picker, a selection will lead to a call of onActivityResult
                    int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
                    Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
                    pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    mainActivity.startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
                } else {
                    // if we already have a widget we remove it
                    removeAllWidgets();
                }
            } else {
                ListPopup menu = new ListPopup(mainActivity);
                ArrayAdapter<WidgetMenuItem> adapter = new ArrayAdapter<>(mainActivity, R.layout.popup_list_item);
                menu.setAdapter(adapter);
                if (canAddWidget())
                    adapter.add(new WidgetMenuItem(mainActivity.getResources().getString(R.string.menu_widget_add), -1, R.string.menu_widget_add));

                for (int i = 0; i < getWidgetHostViewCount(); i += 1) {
                    AppWidgetHostView hostView = getWidgetHostView(i);
                    if (hostView != null) {
                        AppWidgetProviderInfo info = hostView.getAppWidgetInfo();
                        adapter.add(new WidgetMenuItem("Edit widget " + info.provider.flattenToShortString(), hostView.getAppWidgetId(), R.string.menu_widget_settings));
                        adapter.add(new WidgetMenuItem("Remove widget " + info.provider.flattenToShortString(), hostView.getAppWidgetId(), R.string.menu_widget_remove));
                    }
                }

                menu.setOnItemClickListener( this );

                menu.showCentered(widgetArea);
                mainActivity.registerPopup(menu);
            }
            return true;
        }

        return false;
    }

    void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (prefs.getBoolean("history-hide", true)) {
            if (getWidgetHostViewCount() == 0)
                menu.findItem(R.id.widget).setTitle(R.string.menu_widget_add);
        } else {
            menu.findItem(R.id.widget).setVisible(false);
        }
    }

    void onDataSetChanged() {
        if ((getWidgetHostViewCount() > 0) && mainActivity.adapter.isEmpty()) {
            // when a widget is displayed the empty list would prevent touches on the widget
            mainActivity.emptyListView.setVisibility(View.GONE);
        }
    }

    /**
     * Restores all previously added widgets
     */
    private void restoreWidgets() {
        Map<String, ?> widgetIds = widgetPrefs.getAll();
        for (String appWidgetId : widgetIds.keySet()) {
            addWidgetToLauncher(Integer.parseInt(appWidgetId));
        }
    }

    /**
     * Adds a widget to the widget area on the MainActivity
     *
     * @param appWidgetId id of widget to add
     */
    private WidgetPreferences addWidgetToLauncher(int appWidgetId) {
        // only add widgets if in minimal mode (may need launcher restart when turned on)
        if (prefs.getBoolean("history-hide", true)) {
            // remove empty list view when using widgets, this would block touches on the widget
            mainActivity.emptyListView.setVisibility(View.GONE);
            //add widget to view
            AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
            if (appWidgetInfo == null) {
                removeAllWidgets();
                return null;
            }
            AppWidgetHostView hostView = mAppWidgetHost.createView(mainActivity, appWidgetId, appWidgetInfo);
            hostView.setMinimumHeight(appWidgetInfo.minHeight);
            hostView.setAppWidget(appWidgetId, appWidgetInfo);
            if (Build.VERSION.SDK_INT > 15) {
                hostView.updateAppWidgetSize(null, appWidgetInfo.minWidth, appWidgetInfo.minHeight, appWidgetInfo.minWidth, appWidgetInfo.minHeight);
            }
            addWidgetHostView(hostView, appWidgetInfo);
            WidgetLayout.LayoutParams layoutParams = (WidgetLayout.LayoutParams) hostView.getLayoutParams();

            WidgetPreferences wp = new WidgetPreferences();
            wp.width = layoutParams.width;
            wp.height = layoutParams.height;
            wp.offsetTop = layoutParams.topMargin;
            wp.position = layoutParams.position;
            return wp;
        }
        return null;
    }

    private void addWidgetHostView(AppWidgetHostView hostView, AppWidgetProviderInfo appWidgetInfo) {
        widgetArea.addView(hostView);

        int w = ViewGroup.LayoutParams.WRAP_CONTENT;
        int h = ViewGroup.LayoutParams.WRAP_CONTENT;
//        int w = appWidgetInfo.minWidth;
//        int h = appWidgetInfo.minHeight;

//        switch (appWidgetInfo.resizeMode)
//        {
//            case AppWidgetProviderInfo.RESIZE_HORIZONTAL:
//                w = ViewGroup.LayoutParams.MATCH_PARENT;
//                break;
//            case AppWidgetProviderInfo.RESIZE_VERTICAL:
//                h = ViewGroup.LayoutParams.MATCH_PARENT;
//                break;
//            case AppWidgetProviderInfo.RESIZE_BOTH:
//                w = ViewGroup.LayoutParams.MATCH_PARENT;
//                h = ViewGroup.LayoutParams.MATCH_PARENT;
//                break;
//        }

        //TODO: widgetArea needs to be a custom layout so I can be able to position and resize views as I please

        WidgetLayout.LayoutParams layoutParams = new WidgetLayout.LayoutParams(w, h);
        //layoutParams.position = WidgetLayout.LayoutParams.POSITION_MIDDLE;
        layoutParams.position = getWidgetHostViewCount() - 1;

        hostView.setBackgroundColor(0x7Fffd700);
        hostView.setLayoutParams(layoutParams);
    }

    private void removeWidgetHostView(AppWidgetHostView hostView) {
        int childCount = widgetArea.getChildCount();
        for (int i = 0; i < childCount; i += 1) {
            if (widgetArea.getChildAt(i) == hostView) {
                widgetArea.removeViewAt(i);
                return;
            }
        }
    }

    private AppWidgetHostView getWidgetHostView(int index) {
        return (AppWidgetHostView) widgetArea.getChildAt(index);
    }

    private AppWidgetHostView getWidgetHostView(View view) {
        return (AppWidgetHostView) view;
    }

    private int getWidgetHostViewCount() {
        return widgetArea.getChildCount();
    }

    /**
     * Removes all widgets from the launcher
     */
    private void removeAllWidgets() {
        while (getWidgetHostViewCount() > 0) {
            AppWidgetHostView widget = getWidgetHostView(0);
            removeAppWidget(widget);
        }
    }

    /**
     * Removes a single widget and deletes it from persistent prefs
     *
     * @param hostView instance of a displayed widget
     */
    private void removeAppWidget(AppWidgetHostView hostView) {
        // remove widget from view
        int appWidgetId = hostView.getAppWidgetId();
        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        removeWidgetHostView(hostView);
        // remove widget id from persistent prefs
        SharedPreferences.Editor widgetPrefsEditor = widgetPrefs.edit();
        widgetPrefsEditor.remove(String.valueOf(appWidgetId));
        widgetPrefsEditor.apply();
    }

    private boolean canAddWidget() {
        return getWidgetHostViewCount() < 3;
    }

    /**
     * Adds widget to Activity and persists it in prefs to be able to restore it
     *
     * @param data Intent holding widget id to add
     */
    private void addAppWidget(Intent data) {
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        //add widget
        WidgetPreferences wp = addWidgetToLauncher(appWidgetId);

        // Save widget in preferences
        SharedPreferences.Editor widgetPrefsEditor = widgetPrefs.edit();
        widgetPrefsEditor.putString(String.valueOf(appWidgetId), WidgetPreferences.serialize(wp));
        widgetPrefsEditor.apply();
    }

    /**
     * Check if widget needs configuration and display configuration view if necessary,
     * otherwise just add the widget
     *
     * @param data Intent holding widget id to configure
     */
    private void configureAppWidget(Intent data) {
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        AppWidgetProviderInfo appWidget =
                mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        if (appWidget.configure != null) {
            // Launch over to configure widget, if needed.
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidget.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            mainActivity.startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            // Otherwise, finish adding the widget.
            addAppWidget(data);
        }
    }

    private void refreshAppWidget(Intent intent) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        String data = widgetPrefs.getString(String.valueOf(appWidgetId), null);
        WidgetPreferences wp = WidgetPreferences.unserialize(data);
        if (wp == null)
            return;
        for ( int i = 0; i < getWidgetHostViewCount(); i+= 1 ) {
            AppWidgetHostView hostView = getWidgetHostView(i);
            if ( hostView.getAppWidgetId() == appWidgetId ) {
                WidgetLayout.LayoutParams layoutParams = (WidgetLayout.LayoutParams)hostView.getLayoutParams();

                layoutParams.width = wp.width;
                layoutParams.height = wp.height;
                layoutParams.topMargin = wp.offsetTop;

                hostView.setLayoutParams(layoutParams);
                break;
            }
        }
    }

    public void onWidgetLayout(View child, boolean changed, Rect childRect) {
        if (!changed)
            return;
        AppWidgetHostView hostView = getWidgetHostView(child);
        // remove widget from view
        int appWidgetId = hostView.getAppWidgetId();

        String data = widgetPrefs.getString(String.valueOf(appWidgetId), null);
        WidgetPreferences wp = WidgetPreferences.unserialize(data);
        if (wp == null)
            wp = new WidgetPreferences();
        wp.width = childRect.width();
        wp.height = childRect.height();
        SharedPreferences.Editor widgetPrefsEditor = widgetPrefs.edit();
        widgetPrefsEditor.putString(String.valueOf(appWidgetId), WidgetPreferences.serialize(wp));
        widgetPrefsEditor.apply();
    }

    @Override
    public void onItemClick(ListAdapter adapter, View view, int position) {
        WidgetMenuItem menuItem = (WidgetMenuItem) adapter.getItem(position);
        switch (menuItem.action)
        {
            case R.string.menu_widget_remove:
                for ( int i = 0; i < getWidgetHostViewCount(); i+= 1 ) {
                    AppWidgetHostView hostView = getWidgetHostView(i);
                    if ( hostView.getAppWidgetId() == menuItem.appWidgetId ) {
                        removeAppWidget(hostView);
                        break;
                    }
                }
                break;
            case R.string.menu_widget_settings:
                for ( int i = 0; i < getWidgetHostViewCount(); i+= 1 ) {
                    AppWidgetHostView hostView = getWidgetHostView(i);
                    if ( hostView.getAppWidgetId() == menuItem.appWidgetId ) {
                        String data = widgetPrefs.getString(String.valueOf(menuItem.appWidgetId), null);
                        WidgetPreferences wp = WidgetPreferences.unserialize(data);
                        if (wp == null)
                            wp = new WidgetPreferences();
                        wp.showEditMenu(mainActivity, widgetPrefs, hostView);
                        break;
                    }
                }
                break;
        }
    }

    static class WidgetMenuItem {
        final String string;
        final int appWidgetId;
        final int action;

        WidgetMenuItem(String string, int appWidgetId, int action) {
            this.string = string;
            this.appWidgetId = appWidgetId;
            this.action = action;
        }

        @Override
        public String toString() {
            return this.string;
        }
    }
}
