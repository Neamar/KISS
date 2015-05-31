package fr.neamar.kiss.pojo;

import android.net.Uri;

public class ContactPojo extends Pojo {
    public String lookupKey = "";

    public String phone = "";
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
