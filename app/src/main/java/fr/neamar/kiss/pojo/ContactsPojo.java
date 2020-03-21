package fr.neamar.kiss.pojo;

import android.net.Uri;

import fr.neamar.kiss.normalizer.StringNormalizer;

public final class ContactsPojo extends Pojo {
    public final String lookupKey;

    public final String phone;
    //phone without special characters
    public final StringNormalizer.Result normalizedPhone;
    public final Uri icon;

    // Is this a primary phone?
    public final boolean primary;

    // Is this contact starred ?
    public final boolean starred;

    // Is this number a home (local) number ?
    public final boolean homeNumber;

    public StringNormalizer.Result normalizedNickname = null;

    private String nickname = "";

    public ContactsPojo(String id, String lookupKey, String phone, StringNormalizer.Result normalizedPhone,
                        Uri icon, boolean primary, boolean starred,
                        boolean homeNumber) {
        super(id);
        this.lookupKey = lookupKey;
        this.phone = phone;
        this.normalizedPhone = normalizedPhone;
        this.icon = icon;
        this.primary = primary;
        this.starred = starred;
        this.homeNumber = homeNumber;
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
