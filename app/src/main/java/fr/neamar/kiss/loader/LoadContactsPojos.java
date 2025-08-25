package fr.neamar.kiss.loader;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MimeTypeCache;
import fr.neamar.kiss.pojo.ContactData;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.utils.MimeTypeUtils;
import fr.neamar.kiss.utils.Permission;

public class LoadContactsPojos extends LoadPojos<ContactsPojo> {

    private static final String TAG = LoadContactsPojos.class.getSimpleName();

    public LoadContactsPojos(Context context) {
        super(context, "contact://");
    }

    @Override
    protected List<ContactsPojo> doInBackground(Void... params) {
        long start = System.currentTimeMillis();

        List<ContactsPojo> contacts = new ArrayList<>();
        Context ctx = context.get();
        if (ctx == null) {
            return contacts;
        }

        // Skip if we don't have permission to list contacts yet:(
        if (!Permission.checkPermission(ctx, Permission.PERMISSION_READ_CONTACTS)) {
            return contacts;
        }

        // Skip if we don't have any mime types to be shown
        Set<String> mimeTypes = MimeTypeUtils.getActiveMimeTypes(ctx);
        if (mimeTypes.isEmpty()) {
            return contacts;
        }

        // Query basic contact information and keep in memory to prevent duplicates
        Map<String, BasicContact> basicContacts = new HashMap<>();
        long startBasicContacts = System.currentTimeMillis();
        try (Cursor contactCursor = ctx.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                        ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE,
                        ContactsContract.Contacts.PHONETIC_NAME,
                        ContactsContract.Contacts.PHOTO_ID,
                        ContactsContract.Contacts.PHOTO_URI}, null, null, null)) {
            if (contactCursor != null) {
                if (contactCursor.getCount() > 0) {
                    int lookupIndex = contactCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                    int contactIdIndex = contactCursor.getColumnIndex(ContactsContract.Contacts._ID);
                    int displayNameIndex = contactCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
                    int displayNameAlternativeIndex = contactCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE);
                    int phoneticNameIndex = contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHONETIC_NAME);
                    int photoIdIndex = contactCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
                    int photoUriIndex = contactCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI);
                    while (contactCursor.moveToNext() && !isCancelled()) {
                        BasicContact basicContact = new BasicContact(
                                contactCursor.getString(lookupIndex),
                                contactCursor.getLong(contactIdIndex),
                                contactCursor.getString(displayNameIndex),
                                contactCursor.getString(displayNameAlternativeIndex),
                                contactCursor.getString(phoneticNameIndex),
                                contactCursor.getString(photoIdIndex),
                                contactCursor.getString(photoUriIndex)
                        );
                        basicContacts.put(basicContact.getLookupKey(), basicContact);
                    }
                }
            }
        }
        long endBasicContacts = System.currentTimeMillis();
        Log.i(TAG, (endBasicContacts - startBasicContacts) + " milliseconds to load " + basicContacts.size() + " basic contacts");

        // Query raw contact information and keep in memory to prevent duplicates
        Map<Long, BasicRawContact> basicRawContacts = new HashMap<>();
        long startRawContacts = System.currentTimeMillis();
        try (Cursor rawContactCursor = ctx.getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{ContactsContract.RawContacts._ID,
                        ContactsContract.RawContacts.ACCOUNT_TYPE,
                        ContactsContract.RawContacts.STARRED}, null, null, null)) {
            if (rawContactCursor != null) {
                if (rawContactCursor.getCount() > 0) {
                    int rawContactIdIndex = rawContactCursor.getColumnIndex(ContactsContract.RawContacts._ID);
                    int starredIndex = rawContactCursor.getColumnIndex(ContactsContract.RawContacts.STARRED);
                    while (rawContactCursor.moveToNext() && !isCancelled()) {
                        BasicRawContact basicRawContact = new BasicRawContact(
                                rawContactCursor.getLong(rawContactIdIndex),
                                rawContactCursor.getInt(starredIndex) != 0
                        );
                        basicRawContacts.put(basicRawContact.getId(), basicRawContact);
                    }
                }
            }
        }
        long endRawContacts = System.currentTimeMillis();
        Log.i(TAG, (endRawContacts - startRawContacts) + " milliseconds to load " + basicRawContacts.size() + " raw contacts");

        // Retrieve contacts' nicknames
        long startNicks = System.currentTimeMillis();
        try (Cursor nickCursor = ctx.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Nickname.NAME,
                        ContactsContract.Data.LOOKUP_KEY},
                ContactsContract.Data.MIMETYPE + "= ?",
                new String[]{ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE},
                null)) {
            if (nickCursor != null) {
                if (nickCursor.getCount() > 0) {
                    int lookupKeyIndex = nickCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                    int nickNameIndex = nickCursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME);
                    while (nickCursor.moveToNext() && !isCancelled()) {
                        String lookupKey = nickCursor.getString(lookupKeyIndex);
                        String nick = nickCursor.getString(nickNameIndex);

                        if (nick != null && lookupKey != null) {
                            BasicContact basicContact = basicContacts.get(lookupKey);
                            if (basicContact != null) {
                                basicContact.setNickName(nick);
                            }
                        }
                    }
                }
            }
        }
        long endNicks = System.currentTimeMillis();
        Log.i(TAG, (endNicks - startNicks) + " milliseconds to load nicknames");

        // Query all mime types
        for (String mimeType : mimeTypes) {
            long startMimeType = System.currentTimeMillis();
            if (ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                contacts.addAll(createPhoneContacts(ctx, basicContacts, basicRawContacts));
            } else {
                contacts.addAll(createGenericContacts(ctx, mimeType, basicContacts, basicRawContacts));
            }
            long endMimeType = System.currentTimeMillis();
            Log.i(TAG, (endMimeType - startMimeType) + " milliseconds to list contacts for " + mimeType);
        }

        long end = System.currentTimeMillis();
        Log.i(TAG, (end - start) + " milliseconds to list all " + contacts.size() + " contacts");
        return contacts;
    }

    private List<ContactsPojo> createPhoneContacts(@NonNull Context ctx, Map<String, BasicContact> basicContacts, Map<Long, BasicRawContact> basicRawContacts) {

        // Prevent duplicates by keeping in memory encountered contacts.
        Map<String, Set<ContactsPojo>> mapContacts = new HashMap<>();

        // Query all phone numbers
        try (Cursor phoneCursor = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.IS_PRIMARY}, null, null, null)) {
            if (phoneCursor != null) {
                if (phoneCursor.getCount() > 0) {
                    int lookupIndex = phoneCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                    int rawContactIdIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID);
                    int numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int isPrimaryIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY);

                    while (phoneCursor.moveToNext() && !isCancelled()) {
                        String lookupKey = phoneCursor.getString(lookupIndex);
                        BasicContact basicContact = basicContacts.get(lookupKey);
                        long rawContactId = phoneCursor.getLong(rawContactIdIndex);
                        BasicRawContact basicRawContact = basicRawContacts.get(rawContactId);

                        if (basicContact != null && basicRawContact != null) {
                            long contactId = basicContact.getContactId();

                            String phone = phoneCursor.getString(numberIndex);
                            if (phone == null) {
                                phone = "";
                            }

                            boolean starred = basicRawContact.isStarred();
                            boolean primary = phoneCursor.getInt(isPrimaryIndex) != 0;
                            Uri icon = basicContact.getIcon();

                            ContactsPojo contact = new ContactsPojo(pojoScheme + contactId + '/' + phone, lookupKey, contactId, icon, primary, starred);
                            setNames(contact, basicContact);

                            contact.setPhone(phone, false);

                            addContactToMap(contact, mapContacts);
                        }
                    }
                }
            }
        }

        return getFilteredContacts(mapContacts, contact -> contact.normalizedPhone == null ? null : contact.normalizedPhone.toString());
    }

    private List<ContactsPojo> createGenericContacts(@NonNull Context ctx, String mimeType, Map<String, BasicContact> basicContacts, Map<Long, BasicRawContact> basicRawContacts) {
        final MimeTypeCache mimeTypeCache = KissApplication.getMimeTypeCache(ctx);
        // Prevent duplicates by keeping in memory encountered contacts.
        Map<String, Set<ContactsPojo>> mapContacts = new HashMap<>();

        List<String> columns = new ArrayList<>();
        columns.add(ContactsContract.Data.LOOKUP_KEY);
        columns.add(ContactsContract.Data.RAW_CONTACT_ID);
        columns.add(ContactsContract.Data._ID);
        columns.add(ContactsContract.Data.IS_PRIMARY);

        String detailColumn = mimeTypeCache.getDetailColumn(ctx, mimeType);
        if (detailColumn != null && !columns.contains(detailColumn)) {
            columns.add(detailColumn);
        }

        // Query all entries by mimeType
        try (Cursor mimeTypeCursor = ctx.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                columns.toArray(new String[]{}),
                ContactsContract.Data.MIMETYPE + "= ?",
                new String[]{mimeType}, null)) {
            if (mimeTypeCursor != null) {
                if (mimeTypeCursor.getCount() > 0) {
                    int lookupIndex = mimeTypeCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                    int rawContactIdIndex = mimeTypeCursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID);
                    int idIndex = mimeTypeCursor.getColumnIndex(ContactsContract.Data._ID);
                    int isPrimaryIndex = mimeTypeCursor.getColumnIndex(ContactsContract.Data.IS_PRIMARY);
                    int detailColumnIndex = -1;
                    if (detailColumn != null) {
                        detailColumnIndex = mimeTypeCursor.getColumnIndex(detailColumn);
                    }
                    while (mimeTypeCursor.moveToNext() && !isCancelled()) {
                        String lookupKey = mimeTypeCursor.getString(lookupIndex);
                        BasicContact basicContact = basicContacts.get(lookupKey);
                        long rawContactId = mimeTypeCursor.getLong(rawContactIdIndex);
                        BasicRawContact basicRawContact = basicRawContacts.get(rawContactId);

                        if (basicContact != null && basicRawContact != null) {
                            long contactId = basicContact.getContactId();
                            long id = mimeTypeCursor.getLong(idIndex);
                            boolean primary = mimeTypeCursor.getInt(isPrimaryIndex) != 0;
                            String label = null;
                            if (detailColumnIndex >= 0) {
                                label = mimeTypeCursor.getString(detailColumnIndex);
                            }
                            if (TextUtils.isEmpty(label)) {
                                label = mimeTypeCache.getLabel(ctx, mimeType);
                            }
                            Uri icon = basicContact.getIcon();

                            ContactsPojo contact = new ContactsPojo(pojoScheme + contactId + '/' + MimeTypeUtils.getShortMimeType(mimeType) + '/' + id, lookupKey, contactId, icon, primary, basicRawContact.isStarred());
                            setNames(contact, basicContact);

                            ContactData contactData = new ContactData(mimeType, id);
                            contactData.setIdentifier(label);
                            contact.setIm(contactData);

                            addContactToMap(contact, mapContacts);
                        }
                    }
                }
            }
        }

        return getFilteredContacts(mapContacts, contact -> contact.getContactData().getIdentifier());
    }

    /**
     * set all available names to contact
     *
     * @param contact      the contact
     * @param basicContact basic contact information to get names from
     */
    private void setNames(ContactsPojo contact, BasicContact basicContact) {
        contact.setName(basicContact.getDisplayName());
        if (!Objects.equals(basicContact.getDisplayName(), basicContact.getDisplayNameAlternative())) {
            // see https://developer.android.com/reference/android/provider/ContactsContract.ContactNameColumns#DISPLAY_NAME_ALTERNATIVE
            contact.setNameAlternative(basicContact.getDisplayNameAlternative());
        }
        contact.setPhoneticName(basicContact.getPhoneticName());
        contact.setNickname(basicContact.getNickName());
    }

    /**
     * add contact to mapContacts, grouped by lookup key
     *
     * @param contact     contact to add
     * @param mapContacts all contacts grouped by lookup key
     */
    private void addContactToMap(ContactsPojo contact, Map<String, Set<ContactsPojo>> mapContacts) {
        if (contact.getName() != null) {
            Set<ContactsPojo> contacts = mapContacts.get(contact.lookupKey);
            if (contacts == null) {
                contacts = new HashSet<>(1);
                mapContacts.put(contact.lookupKey, contacts);
            }
            contacts.add(contact);
        }
    }

    /**
     * Filter all contacts dependent of fields.
     * Return primary contacts if available.
     * If no primary contacts are available all contacts are returned.
     *
     * @param mapContacts all contacts grouped by lookup key
     * @param idSupplier  id supplier for identifying duplicates
     * @return filtered contacts
     */
    private List<ContactsPojo> getFilteredContacts(Map<String, Set<ContactsPojo>> mapContacts, IdSupplier idSupplier) {
        List<ContactsPojo> contacts = new ArrayList<>();
        // Add phone numbers
        for (Set<ContactsPojo> mappedContacts : mapContacts.values()) {
            // Find primary phone and add this one.
            boolean hasPrimary = false;
            for (ContactsPojo contact : mappedContacts) {
                if (contact.primary) {
                    contacts.add(contact);
                    hasPrimary = true;
                    break;
                }
            }

            // If no primary available, add all (excluding duplicates).
            if (!hasPrimary) {
                Set<String> added = new HashSet<>(mappedContacts.size());
                for (ContactsPojo contact : mappedContacts) {
                    String id = idSupplier.getId(contact);
                    if (id == null) {
                        contacts.add(contact);
                    } else if (!added.contains(id)) {
                        added.add(id);
                        contacts.add(contact);
                    }
                }
            }
        }
        return contacts;
    }

    @FunctionalInterface
    private interface IdSupplier {
        String getId(ContactsPojo contact);
    }

    /**
     * Holds data from {@link ContactsContract.Contacts}
     */
    private static class BasicContact {
        private final String lookupKey;
        private final long contactId;
        private final String displayName;
        private final String displayNameAlternative;
        private final String phoneticName;
        private final String photoId;
        private final String photoUri;
        private String nickName;

        protected BasicContact(String lookupKey, long contactId, String displayName, String displayNameAlternative, String phoneticName, String photoId, String photoUri) {
            this.lookupKey = lookupKey;
            this.contactId = contactId;
            this.displayName = displayName;
            this.displayNameAlternative = displayNameAlternative;
            this.phoneticName = phoneticName;
            this.photoId = photoId;
            this.photoUri = photoUri;
        }

        public String getLookupKey() {
            return lookupKey;
        }

        public long getContactId() {
            return contactId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDisplayNameAlternative() {
            return displayNameAlternative;
        }

        public String getPhoneticName() {
            return phoneticName;
        }

        public String getNickName() {
            return nickName;
        }

        public void setNickName(String nickName) {
            this.nickName = nickName;
        }

        public Uri getIcon() {
            if (photoUri != null) {
                return Uri.parse(photoUri);
            }
            if (photoId != null) {
                return ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI,
                        Long.parseLong(photoId));

            }
            return null;
        }
    }

    /**
     * Holds data from {@link ContactsContract.RawContacts}
     */
    private static class BasicRawContact {
        private final long id;
        private final boolean starred;

        protected BasicRawContact(long id, boolean starred) {
            this.id = id;
            this.starred = starred;
        }

        public long getId() {
            return id;
        }

        public boolean isStarred() {
            return starred;
        }
    }
}
