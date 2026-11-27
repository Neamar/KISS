package fr.neamar.kiss.broadcast;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import fr.neamar.kiss.R;

public class SpacerWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_spacer);
        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }
}
