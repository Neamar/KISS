package fr.neamar.kiss.record;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import fr.neamar.kiss.QueryInterface;
import fr.neamar.kiss.SummonApplication;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.holder.AppHolder;
import fr.neamar.kiss.holder.ContactHolder;
import fr.neamar.kiss.holder.Holder;
import fr.neamar.kiss.holder.SearchHolder;
import fr.neamar.kiss.holder.SettingHolder;
import fr.neamar.kiss.holder.ToggleHolder;
import fr.neamar.kiss.holder.PhoneHolder;

public abstract class Record {
	/**
	 * How relevant is this record ? The higher, the most probable it will be
	 * displayed
	 */
	public int relevance = 0;

	/**
	 * Current information holder
	 */
	public Holder holder = null;

	/**
	 * How to display this record ?
	 *
	 * @param context
	 * @param convertView
	 *            a view to be recycled
	 * @return a view to display as item
	 */
	public abstract View display(Context context, View convertView);

	public final void launch(Context context, View v) {
		Log.i("log", "Launching " + holder.id);

		recordLaunch(context);

		// Launch
		doLaunch(context, v);
	}

	/**
	 * How to launch this record ? Most probably, will fire an intent. This
	 * function must call recordLaunch()
	 *
	 * @param context
	 */
	public abstract void doLaunch(Context context, View v);

	/**
	 * How to launch this record "quickly" ? Most probably, same as doLaunch().
	 * Override to define another behavior.
	 *
	 * @param context
	 */
	public void fastLaunch(Context context)
	{
		this.launch(context, null);
	}

	/**
	 * Return the icon for this Record, or null if non existing.
	 *
	 * @param context
	 */
	public Drawable getDrawable(Context context)
	{
		return null;
	}

	/**
	 * Helper function to get a view
	 *
	 * @param context
	 * @param id
	 * @return the view specified by the id
	 */
	protected View inflateFromId(Context context, int id) {
		LayoutInflater vi = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return vi.inflate(id, null);
	}

	/**
	 * Enrich text for display. Put text requiring highlighting between {}
	 *
	 * @param text
	 * @return text displayable on a textview
	 */
	protected Spanned enrichText(String text) {
		return Html.fromHtml(text.replaceAll("\\{(.+)\\}", "<font color=#4caf50>$1</font>"));
	}

	/**
	 * Put this item in application history
	 *
	 * @param context
	 */
	protected void recordLaunch(Context context) {
		// Save in history
		// TODO: move to datahandler
		DBHelper.insertHistory(context, SummonApplication.getDataHandler(context).currentQuery,
				holder.id);
	}

	public void deleteRecord(Context context) {
		DBHelper.removeFromHistory(context, holder.id);
	}

	public static Record fromHolder(QueryInterface parent, Holder holder) {
		if (holder instanceof AppHolder)
			return new AppRecord((AppHolder) holder);
		else if (holder instanceof ContactHolder)
			return new ContactRecord(parent, (ContactHolder) holder);
		else if (holder instanceof SearchHolder)
			return new SearchRecord((SearchHolder) holder);
		else if (holder instanceof SettingHolder)
			return new SettingRecord((SettingHolder) holder);
		else if (holder instanceof ToggleHolder)
			return new ToggleRecord((ToggleHolder) holder);
		else if (holder instanceof PhoneHolder)
			return new PhoneRecord((PhoneHolder) holder);

		Log.e("log", "Unable to create record for specified holder.");
		return null;
	}
}
