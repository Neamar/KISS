package fr.neamar.kiss;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

public class RootModePreference extends CheckBoxPreference {
	
	public RootModePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	} 	

	@Override
	protected void onClick() {
		if (!isChecked() && !KissApplication.getRootHander(getContext()).isRootAvailable()) { 
    				//show error dialog
    				new AlertDialog.Builder(getContext()).setMessage(R.string.root_mode_error)    													 
    													 .setPositiveButton(android.R.string.ok, new OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// does nothing
						}
					}).show();
    	} else {
    		super.onClick();
    	}
		KissApplication.resetRootHander(getContext());
    }	
}
