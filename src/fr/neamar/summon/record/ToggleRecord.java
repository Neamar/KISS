package fr.neamar.summon.record;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;
import fr.neamar.summon.R;
import fr.neamar.summon.holder.ToggleHolder;
import fr.neamar.summon.toggles.TogglesHandler;

public class ToggleRecord extends Record {
	public final ToggleHolder toggleHolder;
	public TogglesHandler togglesHandler = null;

	public ToggleRecord(ToggleHolder toggleHolder) {
		super();
		this.holder = this.toggleHolder = toggleHolder;
	}

	@Override
	public View display(Context context, View v) {
		// On first run, initialize handler
		if (togglesHandler == null)
			togglesHandler = new TogglesHandler(context);

		if (v == null)
			v = inflateFromId(context, R.layout.item_toggle);

		TextView toggleName = (TextView) v.findViewById(R.id.item_toggle_name);
		toggleName.setText(enrichText(toggleHolder.displayName));

		ImageView toggleIcon = (ImageView) v
				.findViewById(R.id.item_toggle_icon);
		if (toggleHolder.icon != -1)
			toggleIcon.setImageDrawable(context.getResources().getDrawable(
					toggleHolder.icon));

		// Use the handler to check or uncheck button
		final ToggleButton toggleButton = (ToggleButton) v
				.findViewById(R.id.item_toggle_action_toggle);
		toggleButton.setChecked(togglesHandler.getState(toggleHolder));
		// And wait for changes
		toggleButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				recordLaunch(v.getContext());
				Log.i("toggle",
						toggleHolder.name + " state: "
								+ Boolean.toString(toggleButton.isChecked()));
				if (!togglesHandler.getState(toggleHolder).equals(
						toggleButton.isChecked()))
					togglesHandler.setState(toggleHolder,
							toggleButton.isChecked());
			}
		});

		v.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				toggleButton.performClick();
			}
		});

		return v;
	}

	@Override
	public void doLaunch(Context context) {
		// Emulated with v.setOnClickListener
		// Allows access to toggleButton on the View
	}
}
