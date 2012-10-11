package fr.neamar.summon.record;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import fr.neamar.summon.holder.AppHolder;
import fr.neamar.summon.holder.ContactHolder;
import fr.neamar.summon.holder.Holder;
import fr.neamar.summon.holder.SearchHolder;
import fr.neamar.summon.holder.SettingHolder;
import fr.neamar.summon.holder.ToggleHolder;

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
	
	public final void launch(Context context, View v)
	{
		Log.i("log", "Launching " + holder.id);
		
		recordLaunch(context);
		
		//Launch
		doLaunch(context, v);
	}
	
	/**
	 * How to launch this record ?
	 * Most probably, will fire an intent.
	 * This function needs to call recordLaunch()
	 * @param context
	 */
	public abstract void doLaunch(Context context, View v);
	
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
	
	/**
	 * Put this item in application history
	 * @param context
	 */
	protected void recordLaunch(Context context)
	{
		// Save in history
		// Move every item one step down
		SharedPreferences prefs = context.getSharedPreferences("history",
				Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		for (int k = 50; k >= 0; k--) {
			String id = prefs.getString(Integer.toString(k), "(none)");
			if (!id.equals("(none)"))
				ed.putString(Integer.toString(k + 1), id);
		}
		//Store current item
		ed.putString("0", holder.id);
		//Remember result for this query
		ed.putString("query://" + prefs.getString("currentQuery", ""), holder.id);
		ed.commit();
	}
	
	public static Record fromHolder(Holder holder)
	{
		if(holder instanceof AppHolder)
			return new AppRecord((AppHolder) holder);
		else if(holder instanceof ContactHolder)
			return new ContactRecord((ContactHolder) holder);
		else if(holder instanceof SearchHolder)
			return new SearchRecord((SearchHolder) holder);
		else if(holder instanceof SettingHolder)
			return new SettingRecord((SettingHolder) holder);
		else if(holder instanceof ToggleHolder)
			return new ToggleRecord((ToggleHolder) holder);
		
		Log.e("log", "Unable to create record for specified holder.");
		return null;
	}
}
