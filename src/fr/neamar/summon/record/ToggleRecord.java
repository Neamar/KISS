package fr.neamar.summon.record;

import android.content.Context;
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

	/**
	 * Handler for all toggle-related queries
	 */
	protected TogglesHandler togglesHandler = null;

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
				if (!togglesHandler.getState(toggleHolder).equals(
						toggleButton.isChecked())) {

					// record launch manually
					recordLaunch(v.getContext());

					togglesHandler.setState(toggleHolder,
							toggleButton.isChecked());
				}

			}
		});
		return v;
	}

	@Override
	public void doLaunch(Context context, View v) {
		// Use the handler to check or uncheck button
		final ToggleButton toggleButton = (ToggleButton) v
				.findViewById(R.id.item_toggle_action_toggle);
		toggleButton.performClick();
	}
}
