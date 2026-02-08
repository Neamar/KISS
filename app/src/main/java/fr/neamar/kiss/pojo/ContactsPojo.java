package fr.neamar.kiss.pojo;

import android.net.Uri;
import android.text.TextUtils;

import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.utils.PhoneUtils;

public final class ContactsPojo extends Pojo {
    public final String lookupKey;
    private final long contactId;

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

    public StringNormalizer.Result normalizedNameAlternative = null;
    public StringNormalizer.Result normalizedPhoneticName = null;

    private ContactData contactData;

    public ContactsPojo(String id, String lookupKey, long contactId, Uri icon, boolean primary, boolean starred) {
        super(id);
        this.lookupKey = lookupKey;
        this.contactId = contactId;
        this.icon = icon;
        this.primary = primary;
        this.starred = starred;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        if (!TextUtils.isEmpty(nickname)) {
            // Set the actual user-friendly name
            this.nickname = nickname;
            this.normalizedNickname = StringNormalizer.normalizeWithResult(this.nickname, false);
        } else {
            this.nickname = null;
            this.normalizedNickname = null;
        }
    }

    public void setPhone(String phone, boolean homeNumber) {
        if (!TextUtils.isEmpty(phone)) {
            this.phone = PhoneUtils.convertKeypadLettersToDigits(phone);
            this.normalizedPhone = PhoneNormalizer.normalizeWithResult(phone);
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

    public long getContactId() {
        return contactId;
    }

    public void setNameAlternative(String nameAlternative) {
        if (!TextUtils.isEmpty(nameAlternative)) {
            this.normalizedNameAlternative = StringNormalizer.normalizeWithResult(nameAlternative, false);
        } else {
            this.normalizedNameAlternative = null;
        }
    }

    public void setPhoneticName(String phoneticName) {
        if (!TextUtils.isEmpty(phoneticName)) {
            this.normalizedPhoneticName = StringNormalizer.normalizeWithResult(phoneticName, false);
        } else {
            this.normalizedPhoneticName = null;
        }
    }
}
