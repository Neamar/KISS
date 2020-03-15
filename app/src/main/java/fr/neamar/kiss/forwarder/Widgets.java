package fr.neamar.kiss.forwarder;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.ui.WidgetHost;

class Widgets extends Forwarder {
    private static final int REQUEST_PICK_APPWIDGET = 9;
    private static final int REQUEST_CREATE_APPWIDGET = 5;

    private static final int APPWIDGET_HOST_ID = 442;

    private static final String WIDGET_PREF_KEY = "widgets-conf";

    private AppWidgetHostView widgetWithMenuCurrentlyDisplayed;

    /**
     * Widgets fields
     */
    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;

    /**
     * View widgets are added to
     */
    private ViewGroup widgetArea;

    Widgets(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        // Initialize widget manager and host, restore widgets
        mAppWidgetManager = AppWidgetManager.getInstance(mainActivity);
        mAppWidgetHost = new WidgetHost(mainActivity, APPWIDGET_HOST_ID);
        widgetArea = mainActivity.findViewById(R.id.widgetLayout);

        restoreWidgets();
    }

    void onStart() {
        // Start listening for widget update
        try {
            mAppWidgetHost.startListening();
        } catch (Resources.NotFoundException e) {
            // Widgets app was just updated?
            // See https://github.com/Neamar/KISS/issues/959
        }
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
        } else if (resultCode == Activity.RESULT_CANCELED && data != null && (requestCode == REQUEST_CREATE_APPWIDGET || requestCode == REQUEST_PICK_APPWIDGET)) {
            // if widget was not selected, delete id
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_widget) {
            // request widget picker, a selection will lead to a call of onActivityResult
            int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
            Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
            pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            mainActivity.startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
            return true;
        } else if (item.getItemId() == R.id.remove_widget && widgetWithMenuCurrentlyDisplayed != null) {
            ((ViewGroup) widgetWithMenuCurrentlyDisplayed.getParent()).removeView(widgetWithMenuCurrentlyDisplayed);
            widgetWithMenuCurrentlyDisplayed = null;
            serializeState();
            return true;
        } else if (item.getItemId() == R.id.increase_size && widgetWithMenuCurrentlyDisplayed != null) {
            int lineSize = Math.round(widgetWithMenuCurrentlyDisplayed.getLayoutParams().height / getLineHeight());
            lineSize++;
            ViewGroup.LayoutParams params = widgetWithMenuCurrentlyDisplayed.getLayoutParams();
            params.height = (int) (lineSize * getLineHeight());
            widgetWithMenuCurrentlyDisplayed.setLayoutParams(params);
            serializeState();
        } else if (item.getItemId() == R.id.decrease_size && widgetWithMenuCurrentlyDisplayed != null) {
            int lineSize = Math.round(widgetWithMenuCurrentlyDisplayed.getLayoutParams().height / getLineHeight());
            lineSize--;
            AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(widgetWithMenuCurrentlyDisplayed.getAppWidgetId());

            if(lineSize == 0 || (lineSize * getLineHeight() < appWidgetInfo.minHeight)) {
                return true;
            }
            ViewGroup.LayoutParams params = widgetWithMenuCurrentlyDisplayed.getLayoutParams();
            params.height = (int) (lineSize * getLineHeight());
            widgetWithMenuCurrentlyDisplayed.setLayoutParams(params);
            serializeState();
        }
        else if(item.getItemId() == R.id.move_up && widgetWithMenuCurrentlyDisplayed != null) {
            ViewGroup parent = (ViewGroup) widgetWithMenuCurrentlyDisplayed.getParent();
            int currentIndex = parent.indexOfChild(widgetWithMenuCurrentlyDisplayed);
            if(currentIndex >= 1) {
                parent.removeViewAt(currentIndex);
                parent.addView(widgetWithMenuCurrentlyDisplayed, currentIndex - 1);
                serializeState();
            }
        }
        else if(item.getItemId() == R.id.move_down && widgetWithMenuCurrentlyDisplayed != null) {
            ViewGroup parent = (ViewGroup) widgetWithMenuCurrentlyDisplayed.getParent();
            int currentIndex = parent.indexOfChild(widgetWithMenuCurrentlyDisplayed);
            if(currentIndex < parent.getChildCount() - 1) {
                parent.removeViewAt(currentIndex);
                parent.addView(widgetWithMenuCurrentlyDisplayed, currentIndex + 1);
                serializeState();
            }
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
        ArrayList<String> builder = new ArrayList<>(widgetArea.getChildCount());
        for (int i = 0; i < widgetArea.getChildCount(); i++) {
            AppWidgetHostView view = (AppWidgetHostView) widgetArea.getChildAt(i);
            int appWidgetId = view.getAppWidgetId();
            int lineSize = Math.round(view.getLayoutParams().height / getLineHeight());
            builder.add(appWidgetId + "-" + lineSize);
        }

        String pref = TextUtils.join(";", builder);
        prefs.edit().putString(WIDGET_PREF_KEY, pref).apply();
    }

    /**
     * Display all widgets based on state
     */
    @SuppressWarnings("StringSplitter")
    private void restoreWidgets() {
        // only add widgets if in minimal mode
        if (!prefs.getBoolean("history-hide", false)) {
            return;
        }

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
            AppWidgetHostView widget = getWidget(id, lineSize);
            if (widget != null) {
                // Add the widget into the linearlayout. LayoutParams as specified in getWidget() will be erased, so specify them again
                widgetArea.addView(widget, LinearLayout.LayoutParams.MATCH_PARENT, widget.getLayoutParams().height);
            }
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
    }

    /**
     * Retrieve a view for specified widget,
     * add context menu to it
     *
     * @param appWidgetId id of widget to add
     */
    private AppWidgetHostView getWidget(int appWidgetId, int lineSize) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo == null) {
            Log.i("Widget", "Unable to retrieve widget by id " + appWidgetId);
            return null;
        }

        AppWidgetHostView hostView = mAppWidgetHost.createView(mainActivity, appWidgetId, appWidgetInfo);

        int height = (int) (lineSize * getLineHeight());
        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height);
        hostView.setLayoutParams(params);

        hostView.setMinimumHeight(height);

        hostView.setAppWidget(appWidgetId, appWidgetInfo);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            hostView.updateAppWidgetSize(null, appWidgetInfo.minWidth, height, appWidgetInfo.minWidth, height);
        }
        hostView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            MenuInflater inflater = mainActivity.getMenuInflater();
            inflater.inflate(R.menu.menu_widget, menu);

            // Disable items that can't be triggered
            ViewGroup parent = (ViewGroup) hostView.getParent();
            if(parent.indexOfChild(hostView) == 0) {
                menu.findItem(R.id.move_up).setVisible(false);
            }
            if(parent.indexOfChild(hostView) == parent.getChildCount() - 1) {
                menu.findItem(R.id.move_down).setVisible(false);
            }

            int newLineSize = Math.round(hostView.getLayoutParams().height / getLineHeight()) - 1;
            if(newLineSize == 0 || (newLineSize * getLineHeight() < appWidgetInfo.minHeight)) {
                menu.findItem(R.id.decrease_size).setVisible(false);
            }
        });
        hostView.setLongClickable(true);
        hostView.setOnLongClickListener(v -> {
            mainActivity.openContextMenu(hostView);
            widgetWithMenuCurrentlyDisplayed = hostView;
            return true;
        });

        return hostView;
    }

    /**
     * Adds widget to Activity and persists it in prefs to be able to restore it
     *
     * @param data Intent holding widget id to add
     */
    private void addAppWidget(Intent data) {
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        float minWidgetHeight = appWidgetInfo.minHeight;
        float lineHeight = getLineHeight();
        int lineSize = (int) Math.ceil(minWidgetHeight / lineHeight);
        AppWidgetHostView widget = getWidget(appWidgetId, lineSize);
        if (widget != null) {
            widgetArea.addView(widget, LinearLayout.LayoutParams.MATCH_PARENT, widget.getLayoutParams().height);
        }

        serializeState();
    }

    /**
     * Check if widget needs configuration and display configuration view if necessary,
     * otherwise just add the widget
     *
     * @param data Intent holding widget id to configure
     */
    private void configureAppWidget(Intent data) {
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        if (appWidgetInfo.configure != null) {
            // Launch over to configure widget, if needed.
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            mainActivity.startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            // Otherwise, finish adding the widget.
            addAppWidget(data);
        }
    }

    private float getLineHeight() {
        float dip = 50f;
        Resources r = mainActivity.getResources();

        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }
}
