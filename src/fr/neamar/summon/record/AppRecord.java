package fr.neamar.summon.record;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import fr.neamar.summon.R;
import fr.neamar.summon.holder.AppHolder;

public class AppRecord extends Record {
	public AppHolder appHolder;
	
	public AppRecord(AppHolder appHolder) {
		super();
		this.appHolder = appHolder;
	}

	@Override
	public View display(Context context) {
		View v = inflateFromId(context, R.layout.item_app);
		TextView appName = (TextView) v.findViewById(R.id.item_app_name);
		appName.setText(appHolder.appName);
		
		ImageView appIcon = (ImageView) v.findViewById(R.id.item_app_icon);
		appIcon.setImageDrawable(appHolder.icon);
		
		return v;
	}

}
