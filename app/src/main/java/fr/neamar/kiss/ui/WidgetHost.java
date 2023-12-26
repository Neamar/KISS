package fr.neamar.kiss.ui;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

public class WidgetHost extends AppWidgetHost {

    final static private String TAG = WidgetHost.class.getSimpleName();

    private final WidgetProvidersUpdateCallback mWidgetsUpdateCallback;

    public WidgetHost(Context context, int hostId, WidgetProvidersUpdateCallback widgetProvidersUpdateCallback) {
        super(context, hostId);
        this.mWidgetsUpdateCallback = widgetProvidersUpdateCallback;
    }

    @Override
    protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
        // We need to create a custom view to handle long click events
        return new WidgetView(context);
    }

    @Override
    public void startListening() {
        try {
            super.startListening();
            Log.d(TAG, "Start listening");
        } catch (Resources.NotFoundException e) {
            Log.d(TAG, "Start listening failed", e);
            // Widgets app was just updated?
            // See https://github.com/Neamar/KISS/issues/959
        }
    }

    @Override
    public void stopListening() {
        // If stopListening is called during onDestroy the workaround for https://github.com/Neamar/KISS/issues/744 is not needed any more.
        // This was necessary because stopListening cleared remote views until https://android.googlesource.com/platform/frameworks/base/+/2857f1c783e69461735a51159f9abdb85378e210
        // which is ok for calls during onDestroy()
        try {
            super.stopListening();
            Log.d(TAG, "Stop listening");
        } catch (NullPointerException e) {
            // Ignore, happens on some shitty widget down the stack trace.
            Log.d(TAG, "Stop listening failed", e);
        }
        clearViews();
    }

    @Override
    protected void onProvidersChanged() {
        super.onProvidersChanged();
        Log.d(TAG, "Providers changed");
        if (mWidgetsUpdateCallback != null) {
            mWidgetsUpdateCallback.onProvidersUpdated();
        }
    }

    /**
     * Callback interface for packages list update.
     */
    @FunctionalInterface
    public interface WidgetProvidersUpdateCallback {
        /**
         * Gets called when widget providers list changes
         */
        void onProvidersUpdated();
    }

}