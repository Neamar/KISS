package fr.neamar.kiss.result;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.ContactPojo;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.pojo.SettingPojo;
import fr.neamar.kiss.pojo.TogglePojo;
import fr.neamar.kiss.pojo.PhonePojo;

public abstract class Result {
	/**
	 * How relevant is this record ? The higher, the most probable it will be
	 * displayed
	 */
	public int relevance = 0;

	/**
	 * Current information pojo
	 */
	public Pojo pojo = null;

	/**
	 * How to display this record ?
	 *
	 * @param context android context
	 * @param convertView
	 *            a view to be recycled
	 * @return a view to display as item
	 */
	public abstract View display(Context context, View convertView);

	public final void launch(Context context, View v) {
		Log.i("log", "Launching " + pojo.id);

		recordLaunch(context);

		// Launch
		doLaunch(context, v);
	}

	/**
	 * How to launch this record ? Most probably, will fire an intent. This
	 * function must call recordLaunch()
	 *
	 * @param context android context
	 */
	public abstract void doLaunch(Context context, View v);

	/**
	 * How to launch this record "quickly" ? Most probably, same as doLaunch().
	 * Override to define another behavior.
	 *
	 * @param context android context
	 */
	public void fastLaunch(Context context)
	{
		this.launch(context, null);
	}

	/**
	 * Return the icon for this Result, or null if non existing.
	 *
	 * @param context android context
	 */
	public Drawable getDrawable(Context context)
	{
		return null;
	}

	/**
	 * Helper function to get a view
	 *
	 * @param context android context
	 * @param id id to inflate
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
	 * @param text to highlight
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
		DBHelper.insertHistory(context, KissApplication.getDataHandler(context).currentQuery,
				pojo.id);
	}

	public void deleteRecord(Context context) {
		DBHelper.removeFromHistory(context, pojo.id);
	}

	public static Result fromPojo(QueryInterface parent, Pojo pojo) {
		if (pojo instanceof AppPojo)
			return new AppResult((AppPojo) pojo);
		else if (pojo instanceof ContactPojo)
			return new ContactResult(parent, (ContactPojo) pojo);
		else if (pojo instanceof SearchPojo)
			return new SearchResult((SearchPojo) pojo);
		else if (pojo instanceof SettingPojo)
			return new SettingResult((SettingPojo) pojo);
		else if (pojo instanceof TogglePojo)
			return new ToggleResult((TogglePojo) pojo);
		else if (pojo instanceof PhonePojo)
			return new PhoneResult((PhonePojo) pojo);

		Log.e("log", "Unable to create record for specified pojo.");
		return null;
	}
}
