package fr.neamar.kiss.pojo;

import android.net.Uri;

import fr.neamar.kiss.normalizer.StringNormalizer;

public class ContactsPojo extends Pojo {
    public String lookupKey = "";

    public String phone = "";
    //phone without special characters
    public StringNormalizer.Result normalizedPhone = null;
    public Uri icon = null;

    // Is this a primary phone?
    public Boolean primary = false;

    // How many times did we phone this contact?
    public int timesContacted = 0;

    // Is this contact starred ?
    public Boolean starred = false;

    // Is this number a home (local) number ?
    public final Boolean homeNumber = false;

    public StringNormalizer.Result normalizedNickname = null;

    private String nickname = "";

    public ContactsPojo(String lookupKey, String phone, StringNormalizer.Result normalizedPhone,
                        Uri icon, Boolean primary, int timesContacted, Boolean starred) {
        this.lookupKey = lookupKey;
        this.phone = phone;
        this.normalizedPhone = normalizedPhone;
        this.icon = icon;
        this.primary = primary;
        this.timesContacted = timesContacted;
        this.starred = starred;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        if (nickname != null) {
            // Set the actual user-friendly name
            this.nickname = nickname;
            this.normalizedNickname = StringNormalizer.normalizeWithResult(this.nickname, false);
        } else {
            this.nickname = null;
            this.normalizedNickname = null;
        }
    }
}
