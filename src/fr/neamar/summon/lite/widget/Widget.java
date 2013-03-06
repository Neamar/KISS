package fr.neamar.summon.lite.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import fr.neamar.summon.lite.R;
import fr.neamar.summon.lite.SummonActivity;

public class Widget extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
		ComponentName watchWidget = new ComponentName(context, Widget.class);

		Intent intent = new Intent(context, SummonActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		remoteViews.setOnClickPendingIntent(R.id.widget_view, pIntent);

		appWidgetManager.updateAppWidget(watchWidget, remoteViews);

		Log.i("wtf", "onUpdate");
	}
}
