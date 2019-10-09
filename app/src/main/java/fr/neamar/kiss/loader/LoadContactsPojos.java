package fr.neamar.kiss.loader;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import androidx.annotation.Nullable;
import fr.neamar.kiss.R;
import fr.neamar.kiss.forwarder.Permission;
import fr.neamar.kiss.normalizer.PhoneNormalizer;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.ui.RoundedQuickContactBadge;

import static fr.neamar.kiss.utils.DrawableToBitmap.drawableToBitmap;

public class LoadContactsPojos extends LoadPojos<ContactsPojo> {

    public LoadContactsPojos(Context context) {
        super(context, "contact://");
    }

    @Override
    protected ArrayList<ContactsPojo> doInBackground(Void... params) {
        long start = System.nanoTime();

        ArrayList<ContactsPojo> contacts = new ArrayList<>();
        Context c = context.get();
        if(c == null) {
            return contacts;
        }

        // Skip if we don't have permission to list contacts yet:(
        if(!Permission.checkContactPermission(c)) {
            Permission.askContactPermission();
            return contacts;
        }

        // Run query
        Cursor cur = context.get().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.STARRED,
                        ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                        ContactsContract.Contacts.PHOTO_ID,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID}, null, null, ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED + " DESC");

        // Prevent duplicates by keeping in memory encountered contacts.
        Map<String, Set<ContactsPojo>> mapContacts = new HashMap<>();

        if (cur != null) {
            if (cur.getCount() > 0) {
                int lookupIndex = cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                int timesContactedIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED);
                int displayNameIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int starredIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED);
                int isPrimaryIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY);
                int photoIdIndex = cur.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);

                while (cur.moveToNext()) {
                    String lookupKey = cur.getString(lookupIndex);
                    Integer timesContacted = cur.getInt(timesContactedIndex);
                    String name = cur.getString(displayNameIndex);

                    String phone = cur.getString(numberIndex);
                    if (phone == null) {
                        phone = "";
                    }

                    StringNormalizer.Result normalizedPhone = PhoneNormalizer.simplifyPhoneNumber(phone);
                    boolean starred = cur.getInt(starredIndex) != 0;
                    boolean primary = cur.getInt(isPrimaryIndex) != 0;
                    String photoId = cur.getString(photoIdIndex);
                    Uri iconUri = null;
                    if (photoId != null) {
                        iconUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI,
                                Long.parseLong(photoId));
                    }

                    Future<Drawable> icon = getIcon(iconUri);

                    ContactsPojo contact = new ContactsPojo(pojoScheme + lookupKey + phone,
                            lookupKey, phone, normalizedPhone, icon, getRoundedIcon(icon),
                            primary, timesContacted, starred, false);

                    contact.setName(name);

                    if (contact.getName() != null) {
                        //TBog: contact should have the normalized name already
                        //contact.setName( contact.getName(), true );

                        if (mapContacts.containsKey(contact.lookupKey))
                            mapContacts.get(contact.lookupKey).add(contact);
                        else {
                            Set<ContactsPojo> phones = new HashSet<>();
                            phones.add(contact);
                            mapContacts.put(contact.lookupKey, phones);
                        }
                    }
                }
            }
            cur.close();
        }

        // Retrieve contacts' nicknames
        Cursor nickCursor = context.get().getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Nickname.NAME,
                        ContactsContract.Data.LOOKUP_KEY},
                ContactsContract.Data.MIMETYPE + "= ?",
                new String[]{ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE},
                null);

        if (nickCursor != null) {
            if (nickCursor.getCount() > 0) {
                int lookupKeyIndex = nickCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                int nickNameIndex = nickCursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME);
                while (nickCursor.moveToNext()) {
                    String lookupKey = nickCursor.getString(lookupKeyIndex);
                    String nick = nickCursor.getString(nickNameIndex);

                    if (nick != null && lookupKey != null && mapContacts.containsKey(lookupKey)) {
                        for (ContactsPojo contact : mapContacts.get(lookupKey)) {
                            contact.setNickname(nick);
                        }
                    }
                }
            }
            nickCursor.close();
        }

        for (Set<ContactsPojo> phones : mapContacts.values()) {
            // Find primary phone and add this one.
            Boolean hasPrimary = false;
            for (ContactsPojo contact : phones) {
                if (contact.primary) {
                    contacts.add(contact);
                    hasPrimary = true;
                    break;
                }
            }

            // If no primary available, add all (excluding duplicates).
            if (!hasPrimary) {
                HashSet<String> added = new HashSet<>(phones.size());
                for (ContactsPojo contact : phones) {
                    if (!added.contains(contact.normalizedPhone.toString())) {
                        added.add(contact.normalizedPhone.toString());
                        contacts.add(contact);
                    }
                }
            }
        }
        long end = System.nanoTime();
        Log.i("time", Long.toString((end - start) / 1000000) + " milliseconds to list contacts");
        return contacts;
    }

    private Future<Drawable> getIcon(Uri iconUri) {
        FutureTask<Drawable> futureIcon = new FutureTask<>(new IconCallable(context, iconUri));
        IMAGE_EXCECUTOR.execute(futureIcon);
        return futureIcon;
    }

    private Future<Drawable> getRoundedIcon(Future<Drawable> icon) {
        return new FutureTask<>(new RoundedIconCallable(icon));
    }

    private static final class IconCallable implements Callable<Drawable> {
        private final WeakReference<Context> context;
        @Nullable
        private final Uri iconUri;

        private IconCallable(WeakReference<Context> context, @Nullable Uri iconUri) {
            this.context = context;
            this.iconUri = iconUri;
        }


        @Override
        public Drawable call() throws IOException {
            final Context context = this.context.get();
            if(context == null) {
                return null;
            }

            if(iconUri == null) {
                return defaultIcon(context);
            }

            InputStream inputStream = null;
            try {
                inputStream = context.getContentResolver().openInputStream(iconUri);
                return Drawable.createFromStream(inputStream, null);
            } catch (FileNotFoundException e) {
                return defaultIcon(context);
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }

        private Drawable defaultIcon(Context context) {
            return context.getResources().getDrawable(R.drawable.ic_contact);
        }
    }

    private static final class RoundedIconCallable implements Callable<Drawable> {
        private final Future<Drawable> futureIcon;

        private RoundedIconCallable(Future<Drawable> futureIcon) {
            this.futureIcon = futureIcon;
        }

        @Override
        public Drawable call() throws ExecutionException, InterruptedException {
            Bitmap iconBitmap = drawableToBitmap(futureIcon.get());
            return new RoundedQuickContactBadge.RoundedDrawable(iconBitmap);
        }
    }
}
