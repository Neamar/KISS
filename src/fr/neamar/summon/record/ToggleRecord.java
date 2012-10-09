package fr.neamar.summon.record;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import fr.neamar.summon.R;
import fr.neamar.summon.holder.AppHolder;
import fr.neamar.summon.holder.ToggleHolder;

public class ToggleRecord extends Record {
	public ToggleHolder toggleHolder;

	public ToggleRecord(ToggleHolder toggleHolder) {
		super();
		this.holder = this.toggleHolder = toggleHolder;
	}

	@Override
	public View display(Context context, View v) {
		if (v == null)
			v = inflateFromId(context, R.layout.item_toggle);

		TextView toggleName = (TextView) v.findViewById(R.id.item_toggle_name);
		toggleName.setText(enrichText(toggleHolder.displayName));

		ImageView toggleIcon = (ImageView) v.findViewById(R.id.item_toggle_icon);
		if(toggleHolder.icon != -1)
			toggleIcon.setImageDrawable(context.getResources().getDrawable(toggleHolder.icon));

		return v;
	}

	@Override
	public void doLaunch(Context context) {

	}

}
