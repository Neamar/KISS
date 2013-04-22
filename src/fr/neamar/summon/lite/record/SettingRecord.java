package fr.neamar.summon.lite.record;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import fr.neamar.summon.lite.R;
import fr.neamar.summon.lite.holder.SettingHolder;

public class SettingRecord extends Record {
	public final SettingHolder settingHolder;

	public SettingRecord(SettingHolder settingHolder) {
		super();
		this.holder = this.settingHolder = settingHolder;
	}

	@Override
	public View display(Context context, View v) {
		if (v == null)
			v = inflateFromId(context, R.layout.item_setting);

		TextView settingName = (TextView) v.findViewById(R.id.item_setting_name);
		settingName.setText(enrichText(settingHolder.displayName));

		ImageView settingIcon = (ImageView) v.findViewById(R.id.item_setting_icon);
		settingIcon.setImageDrawable(getDrawable(context));

		return v;
	}

	@Override
	public Drawable getDrawable(Context context) {
		if (settingHolder.icon != -1) {
			TypedArray a = context.obtainStyledAttributes(R.style.SummonTheme,
					new int[] { settingHolder.icon });
			int attributeResourceId = a.getResourceId(0, -1);
			if (attributeResourceId != -1) {
				a.recycle();
				return context.getResources().getDrawable(attributeResourceId);
			}
			a.recycle();
		}

		return null;
	}

	@Override
	public void doLaunch(Context context, View v) {
		Intent intent = new Intent(settingHolder.settingName);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
