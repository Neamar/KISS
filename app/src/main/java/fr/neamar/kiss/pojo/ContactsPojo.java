package fr.neamar.kiss.pojo;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.utils.PhoneUtils;
import fr.neamar.kiss.utils.UserHandle;

public final class ContactsPojo extends Pojo {
    public final String lookupKey;
    private final long contactId;

    public String phone;
    //phone without special characters
    public StringNormalizer.Result normalizedPhone;
    private Set<StringNormalizer.Result> additionalNormalizedPhoneNumbers;
    // Is this number a home (local) number ?
    private boolean homeNumber;

    public final Uri icon;

    // Is this a primary phone?
    public final boolean primary;

    // Is this contact starred ?
    public final boolean starred;

    private final UserHandle userHandle;

    private String nickname = null;
    // nickname without special characters
    public StringNormalizer.Result normalizedNickname = null;

    public StringNormalizer.Result normalizedNameAlternative = null;
    public StringNormalizer.Result normalizedPhoneticName = null;

    // Contact data from other apps (WhatsApp, Signal, ...), by mime type
    private Map<String, ContactData> contactData = null;

    public ContactsPojo(String id, String lookupKey, long contactId, Uri icon, boolean primary, boolean starred, @NonNull UserHandle userHandle) {
        super(id);
        this.lookupKey = lookupKey;
        this.contactId = contactId;
        this.icon = icon;
        this.primary = primary;
        this.starred = starred;
        this.userHandle = userHandle;
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

    /**
     * Add contact data from another app, only the first one for each mime type is kept
     *
     * @param data contact data to add
     */
    public void addContactData(ContactData data) {
        if (contactData == null) {
            contactData = new LinkedHashMap<>(1);
        }
        if (!contactData.containsKey(data.getMimeType())) {
            contactData.put(data.getMimeType(), data);
        }
    }

    /**
     * @param mimeType mime type of contact data
     * @return contact data for given mime type, null if not available for this contact
     */
    public ContactData getContactData(String mimeType) {
        return contactData == null ? null : contactData.get(mimeType);
    }

    /**
     * @return first contact data added, used for contacts without phone number
     */
    public ContactData getContactData() {
        return contactData == null ? null : contactData.values().iterator().next();
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

    /**
     * @return id if icon is set and some default string if no icon is set
     */
    @Override
    public String getCustomIconId() {
        if (icon != null) {
            return id;
        } else {
            return "contact://default";
        }
    }

    public void addAdditionalNormalizedPhoneNumber(StringNormalizer.Result phoneNumber) {
        if (phoneNumber == null || Objects.equals(phoneNumber, normalizedPhone)) {
            return;
        }
        if (additionalNormalizedPhoneNumbers == null) {
            additionalNormalizedPhoneNumbers = new HashSet<>();
        }
        additionalNormalizedPhoneNumbers.add(phoneNumber);
    }

    public Set<StringNormalizer.Result> getAllNormalizedPhoneNumbers() {
        Set<StringNormalizer.Result> result = new HashSet<>();
        if (normalizedPhone != null) {
            result.add(normalizedPhone);
        }
        if (additionalNormalizedPhoneNumbers != null) {
            result.addAll(additionalNormalizedPhoneNumbers);
        }
        return result;
    }

    @Override
    public UserHandle getUserHandle() {
        return userHandle;
    }
}
