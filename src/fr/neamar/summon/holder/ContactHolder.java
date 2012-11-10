package fr.neamar.summon.holder;

import android.net.Uri;

public class ContactHolder extends Holder {
	public String phone = "";
	public String mail = "";
	public Uri icon = null;

	public int timesContacted = 0;
	public String lookupKey = "";

	// Is this contact starred ?
	public Boolean starred = false;

	// Is this number a home (local) number ? If yes, messaging icon won't be
	// displayed
	public Boolean homeNumber = false;
}
