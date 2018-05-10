package fr.neamar.kiss.forwarder;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.ui.WidgetLayout;

class Widget extends Forwarder {
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
    private boolean widgetUsed = false;

    /**
     * View widgets are added to
     */
    private ViewGroup widgetArea;

    Widget(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        // Initialize widget manager and host, restore widgets
        widgetPrefs = mainActivity.getSharedPreferences(WIDGET_PREFERENCE_ID, Context.MODE_PRIVATE);

        mAppWidgetManager = AppWidgetManager.getInstance(mainActivity);
        mAppWidgetHost = new AppWidgetHost(mainActivity, APPWIDGET_HOST_ID);
        widgetArea = mainActivity.findViewById(R.id.widgetLayout);

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
            if (!widgetUsed) {
                // request widget picker, a selection will lead to a call of onActivityResult
                int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
                Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
                pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                mainActivity.startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
            } else {
                // if we already have a widget we remove it
                removeAllWidgets();
            }
            return true;
        }

        return false;
    }

    void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (prefs.getBoolean("history-hide", true)) {
            if (widgetUsed) {
                menu.findItem(R.id.widget).setTitle(R.string.menu_widget_remove);
            } else {
                menu.findItem(R.id.widget).setTitle(R.string.menu_widget_add);
            }
        } else {
            menu.findItem(R.id.widget).setVisible(false);
        }
    }

    void onDataSetChanged() {
        if (widgetUsed && mainActivity.adapter.isEmpty()) {
            // when a widget is displayed the empty list would prevent touches on the widget
            mainActivity.emptyListView.setVisibility(View.GONE);
        }
    }

    /**
     * Restores all previously added widgets
     */
    private void restoreWidgets() {
        HashMap<String, Integer> widgetIds = (HashMap<String, Integer>) widgetPrefs.getAll();
        for (int appWidgetId : widgetIds.values()) {
            addWidgetToLauncher(appWidgetId);
        }
    }

    /**
     * Adds a widget to the widget area on the MainActivity
     *
     * @param appWidgetId id of widget to add
     */
    private void addWidgetToLauncher(int appWidgetId) {
        // only add widgets if in minimal mode (may need launcher restart when turned on)
        if (prefs.getBoolean("history-hide", true)) {
            // remove empty list view when using widgets, this would block touches on the widget
            mainActivity.emptyListView.setVisibility(View.GONE);
            //add widget to view
            AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
            if (appWidgetInfo == null) {
                removeAllWidgets();
                return;
            }
            AppWidgetHostView hostView = mAppWidgetHost.createView(mainActivity, appWidgetId, appWidgetInfo);
            hostView.setMinimumHeight(appWidgetInfo.minHeight);
            hostView.setAppWidget(appWidgetId, appWidgetInfo);
            if (Build.VERSION.SDK_INT > 15) {
                hostView.updateAppWidgetSize(null, appWidgetInfo.minWidth, appWidgetInfo.minHeight, appWidgetInfo.minWidth, appWidgetInfo.minHeight);
            }
            addWidgetHostView(hostView, appWidgetInfo);
        }
        // only one widget allowed so widgetUsed is true now, even if not added to view
        widgetUsed = true;
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
        layoutParams.position = WidgetLayout.LayoutParams.POSITION_LEFT;

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
        // only one widget allowed so widgetUsed is false now
        widgetUsed = false;
    }

    /**
     * Adds widget to Activity and persists it in prefs to be able to restore it
     *
     * @param data Intent holding widget id to add
     */
    private void addAppWidget(Intent data) {
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        //add widget
        addWidgetToLauncher(appWidgetId);
        // Save widget in preferences
        SharedPreferences.Editor widgetPrefsEditor = widgetPrefs.edit();
        widgetPrefsEditor.putInt(String.valueOf(appWidgetId), appWidgetId);
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
}
