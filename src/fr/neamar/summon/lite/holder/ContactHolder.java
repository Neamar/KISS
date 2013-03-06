package fr.neamar.summon.lite.holder;

import android.net.Uri;

public class ContactHolder extends Holder {
	public String lookupKey = "";

	public String phone = "";
	public String mail = "";
	public Uri icon = null;

	// Is this a primary phone?
	public Boolean primary = false;

	// How many times did we phone this contact?
	public int timesContacted = 0;

	// Is this contact starred ?
	public Boolean starred = false;

	// Is this number a home (local) number ?
	public Boolean homeNumber = false;
}
