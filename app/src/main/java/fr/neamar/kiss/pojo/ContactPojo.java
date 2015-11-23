package fr.neamar.kiss.pojo;

import android.net.Uri;

public class ContactPojo extends Pojo {
    public String lookupKey = "";

    public String phone = "";
    //phone without special characters
    public String phoneSimplified = "";
    //the phone number to display (might contain styling)
    public String displayPhone ="";
    public Uri icon = null;

    // Is this a primary phone?
    public Boolean primary = false;

    // How many times did we phone this contact?
    public int timesContacted = 0;

    // Is this contact starred ?
    public Boolean starred = false;

    // Is this number a home (local) number ?
    public Boolean homeNumber = false;


    public void setDisplayPhoneHighlightRegion(int positionNormalizedStart, int positionNormalizedEnd) {

        this.displayPhone = this.phoneSimplified.substring(0, positionNormalizedStart)
                + '{' + this.phoneSimplified.substring(positionNormalizedStart, positionNormalizedEnd) + '}'
                + this.phoneSimplified.substring(positionNormalizedEnd);
    }
}
