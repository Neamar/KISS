package fr.neamar.summon.record;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public abstract class Record {
	/**
	 * How relevant is this record ? The higher, the most probable it will be displayed
	 */
	public int relevance = 0;
	
	/**
	 * How to display this record ?
	 * @param context
	 * @return a view to display as item
	 */
	public abstract View display(Context context);
	
	/**
	 * How to launch this record ?
	 * Most probably, will fire an intent
	 * @param context
	 */
	public abstract void launch(Context context);
	
	/**
	 * Helper function to get a view
	 * @param context
	 * @param id
	 * @return the view specified by the id
	 */
	protected View inflateFromId(Context context, int id)
	{
		LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return vi.inflate(id, null);
	}
}
