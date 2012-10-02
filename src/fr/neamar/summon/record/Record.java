package fr.neamar.summon.record;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public abstract class Record {
	/**
	 * How relevant is this record ? The higher, the most probable it will be displayed
	 */
	public int relevance = 0;

	public Record() {
	}
	
	public abstract View display(Context context);
	
	protected View inflateFromId(Context context, int id)
	{
		LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return vi.inflate(id, null);
	}
}
