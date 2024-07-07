package fr.neamar.kiss.pojo;

import android.net.Uri;

import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.normalizer.StringNormalizer;

public final class ContactsPojo extends Pojo {
    public final String lookupKey;

    public String phone;
    //phone without special characters
    public StringNormalizer.Result normalizedPhone;
    // Is this number a home (local) number ?
    private boolean homeNumber;

    public final Uri icon;

    // Is this a primary phone?
    public final boolean primary;

    // Is this contact starred ?
    public final boolean starred;

    private String nickname = null;
    // nickname without special characters
    public StringNormalizer.Result normalizedNickname = null;

    private ContactData contactData;

    public ContactsPojo(String id, String lookupKey, Uri icon, boolean primary, boolean starred) {
        super(id);
        this.lookupKey = lookupKey;
        this.icon = icon;
        this.primary = primary;
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

    public void setPhone(String phone, boolean homeNumber) {
        if (phone != null) {
            this.phone = phone;
            this.normalizedPhone = PhoneNormalizer.simplifyPhoneNumber(phone);
            this.homeNumber = homeNumber;
        } else {
            this.phone = null;
            this.normalizedPhone = null;
            this.homeNumber = false;
        }
    }

    public boolean isHomeNumber() {
        return homeNumber;
    }

    public void setIm(ContactData contactData) {
        this.contactData = contactData;
    }

    public ContactData getContactData() {
        return contactData;
    }

}
