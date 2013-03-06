package fr.neamar.summon;

import fr.neamar.summon.lite.SummonApplication;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.Toast;

public class ResetPreference extends DialogPreference {

	public ResetPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		super.onClick(dialog, which);
		if (which == DialogInterface.BUTTON_POSITIVE) {
			getContext().deleteDatabase("summon.s3db");
			new BackupManager(getContext()).dataChanged();
			SummonApplication.resetDataHandler(getContext());
			PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
					.putBoolean("layout-updated", true).commit();

			Toast.makeText(getContext(), "History erased.", Toast.LENGTH_LONG).show();
		}

	}

}
