package fr.neamar.kiss.result;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.TogglePojo;
import fr.neamar.kiss.toggles.TogglesHandler;

public class ToggleResult extends Result {
    private final TogglePojo togglePojo;

    /**
     * Handler for all toggle-related queries
     */
    private TogglesHandler togglesHandler = null;

    public ToggleResult(TogglePojo togglePojo) {
        super();
        this.pojo = this.togglePojo = togglePojo;
    }

    @SuppressWarnings({"ResourceType", "deprecation"})
    @Override
    public View display(Context context, int position, View v) {
        // On first run, initialize handler
        if (togglesHandler == null)
            togglesHandler = new TogglesHandler(context);

        if (v == null)
            v = inflateFromId(context, R.layout.item_toggle);

        TextView toggleName = (TextView) v.findViewById(R.id.item_toggle_name);
        toggleName.setText(enrichText(togglePojo.displayName));

        ImageView toggleIcon = (ImageView) v.findViewById(R.id.item_toggle_icon);
        if (togglePojo.icon != -1) {
            TypedArray a = context.obtainStyledAttributes(R.style.AppTheme,
                    new int[]{togglePojo.icon});
            int attributeResourceId = a.getResourceId(0, -1);
            if (attributeResourceId != -1) {
                toggleIcon
                        .setImageDrawable(context.getResources().getDrawable(attributeResourceId));
            }
            a.recycle();
        }
        // Use the handler to check or un-check button
        final CompoundButton toggleButton = (CompoundButton) v
                .findViewById(R.id.item_toggle_action_toggle);

        Boolean state = togglesHandler.getState(togglePojo);
        if (state != null)
            toggleButton.setChecked(togglesHandler.getState(togglePojo));
        else
            toggleButton.setEnabled(false);

        toggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!togglesHandler.getState(togglePojo).equals(toggleButton.isChecked())) {

                    // record launch manually
                    recordLaunch(buttonView.getContext());

                    togglesHandler.setState(togglePojo, toggleButton.isChecked());

                    toggleButton.setEnabled(false);
                    new AsyncTask<Void, Void, Void>() {

                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                Thread.sleep(1500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void result) {
                            super.onPostExecute(result);
                            toggleButton.setEnabled(true);
                        }

                    }.execute();
                }
            }
        });
        return v;
    }

    @Override
    public void doLaunch(Context context, View v) {
        // Use the handler to check or un-check button
        final CompoundButton toggleButton = (CompoundButton) v
                .findViewById(R.id.item_toggle_action_toggle);
        if (toggleButton.isEnabled())
            toggleButton.performClick();
    }
}
