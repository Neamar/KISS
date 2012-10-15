package fr.neamar.summon;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;


public class Widget extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		RemoteViews remoteViews=  new RemoteViews(context.getPackageName(), R.layout.widget);
		ComponentName watchWidget= new ComponentName(context, Widget.class);

		appWidgetManager.updateAppWidget(watchWidget, remoteViews);
		
		Log.e("wtf", "onUpdate");
	}
}
