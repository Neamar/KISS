package fr.neamar.summon;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import fr.neamar.summon.R;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;


public class Widget extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		RemoteViews remoteViews;
		ComponentName watchWidget;
		DateFormat format = SimpleDateFormat.getTimeInstance(
				SimpleDateFormat.MEDIUM, Locale.getDefault());

		remoteViews = new RemoteViews(context.getPackageName(), R.layout.main);
		watchWidget = new ComponentName(context, Widget.class);
		remoteViews.setTextViewText(R.id.widget_textview,
				"Time = " + format.format(new Date()));
		appWidgetManager.updateAppWidget(watchWidget, remoteViews);
	}
}
