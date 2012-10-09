package fr.neamar.summon.record;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;
import fr.neamar.summon.R;
import fr.neamar.summon.holder.ToggleHolder;
import fr.neamar.summon.toggles.TogglesHandler;

public class ToggleRecord extends Record {
	public ToggleHolder toggleHolder;
	public TogglesHandler togglesHandler = null;

	public ToggleRecord(ToggleHolder toggleHolder) {
		super();
		this.holder = this.toggleHolder = toggleHolder;
	}

	@Override
	public View display(Context context, View v) {
		//On first run, initialize handler
		if(togglesHandler == null)
			togglesHandler = new TogglesHandler(context);
		
		if (v == null)
			v = inflateFromId(context, R.layout.item_toggle);

		TextView toggleName = (TextView) v.findViewById(R.id.item_toggle_name);
		toggleName.setText(enrichText(toggleHolder.displayName));

		ImageView toggleIcon = (ImageView) v.findViewById(R.id.item_toggle_icon);
		if(toggleHolder.icon != -1)
			toggleIcon.setImageDrawable(context.getResources().getDrawable(toggleHolder.icon));

		//Use the handler to check or uncheck button
		ToggleButton toggleButton = (ToggleButton) v.findViewById(R.id.item_toggle_action_toggle);
		toggleButton.setChecked(togglesHandler.getState(toggleHolder));
		
		return v;
	}

	@Override
	public void doLaunch(Context context) {

	}
}
