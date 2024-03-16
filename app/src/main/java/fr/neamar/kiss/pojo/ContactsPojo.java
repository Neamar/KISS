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

    private ImData imData;

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

    public void setIm(ImData imData) {
        this.imData = imData;
    }

    public ImData getImData() {
        return imData;
    }

    public static class ImData {
        private final long id;
        private final String mimeType;

        private String identifier;
        // IM name without special characters
        private StringNormalizer.Result normalizedIdentifier;

        public ImData(String mimeType, long id) {
            this.mimeType = mimeType;
            this.id = id;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            if (identifier != null) {
                // Set the actual user-friendly name
                this.identifier = identifier;
                this.normalizedIdentifier = StringNormalizer.normalizeWithResult(this.identifier, false);
            } else {
                this.identifier = null;
                this.normalizedIdentifier = null;
            }
        }

        public String getMimeType() {
            return mimeType;
        }

        public long getId() {
            return id;
        }

        public StringNormalizer.Result getNormalizedIdentifier() {
            return normalizedIdentifier;
        }
    }
}
