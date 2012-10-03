package fr.neamar.summon.record;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import fr.neamar.summon.holder.Holder;

public abstract class Record {
	/**
	 * How relevant is this record ? The higher, the most probable it will be displayed
	 */
	public int relevance = 0;
	
	/**
	 * Current information holder
	 */
	public Holder holder = null;
	
	/**
	 * How to display this record ?
	 * @param context
	 * @param convertView a view to be recycled
	 * @return a view to display as item
	 */
	public abstract View display(Context context, View convertView);
	
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
	
	/**
	 * Enrich text for display.
	 * Put text requiring highlighting between {}
	 * @param text
	 * @return text displayable on a textview
	 */
	protected Spanned enrichText(String text)
	{
		return Html.fromHtml(text.replaceAll("\\{(.+)\\}", "<font color=#6e73e5>$1</font>"));
	}
}
