package fr.neamar.kiss.pojo;

import android.net.Uri;

import fr.neamar.kiss.normalizer.StringNormalizer;

public class ContactsPojo extends Pojo {
    public String lookupKey = "";

    public String phone = "";
    //phone without special characters
    public String phoneSimplified = "";
    public Uri icon = null;

    // Is this a primary phone?
    public Boolean primary = false;

    // How many times did we phone this contact?
    public int timesContacted = 0;

    // Is this contact starred ?
    public Boolean starred = false;

    // Is this number a home (local) number ?
    public final Boolean homeNumber = false;

    public String nickname = "";

    public void setNickname(String nickname) {
        this.nickname = StringNormalizer.normalize(nickname);
    }
}
