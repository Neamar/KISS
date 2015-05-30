package fr.neamar.kiss.record;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.AppPojo;

public class AppRecord extends Record {
	public AppPojo appPojo;

	protected final ComponentName className;

	public AppRecord(AppPojo appPojo) {
		super();
		this.pojo = this.appPojo = appPojo;

		className = new ComponentName(appPojo.packageName, appPojo.activityName);
	}

	@Override
	public View display(Context context, View v) {
		if (v == null)
			v = inflateFromId(context, R.layout.item_app);

		TextView appName = (TextView) v.findViewById(R.id.item_app_name);
		appName.setText(enrichText(appPojo.displayName));

		ImageView appIcon = (ImageView) v.findViewById(R.id.item_app_icon);
		appIcon.setImageDrawable(this.getDrawable(context));

		return v;
	}
	
	@Override
	public Drawable getDrawable(Context context) {
		try {
			return context.getPackageManager().getActivityIcon(className);
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	@Override
	public void doLaunch(Context context, View v) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setComponent(className);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

		context.startActivity(intent);
	}
}
