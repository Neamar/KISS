package fr.neamar.kiss.ui;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.res.Resources;

public class WidgetHost extends AppWidgetHost {

    public WidgetHost(Context context, int hostId) {
        super(context, hostId);
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
        } catch (Resources.NotFoundException e) {
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
        } catch (NullPointerException e) {
            // Ignore, happens on some shitty widget down the stack trace.
        }
        clearViews();
    }
}