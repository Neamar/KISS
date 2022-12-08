package fr.neamar.kiss.utils;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Collections.emptySet;

public class MimeTypeUtils {

    // Known android mime types that are not supported by KISS
    private static final Set<String> UNSUPPORTED_MIME_TYPES = new HashSet<>(Arrays.asList(
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Identity.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE
    ));

    private MimeTypeUtils() {
    }

    /**
     * @param context
     * @return a list of all supported mime types from existing contacts
     */
    public static Set<String> getSupportedMimeTypes(Context context) {
        if (!Permission.checkPermission(context, Permission.PERMISSION_READ_CONTACTS)) {
            return emptySet();
        }

        long start = System.currentTimeMillis();

        Set<String> mimeTypes = new HashSet<>();

        Cursor cursor = context.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.Data.MIMETYPE}, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                int mimeTypeIndex = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
                while (cursor.moveToNext()) {
                    String mimeType = cursor.getString(mimeTypeIndex);
                    if (isSupportedMimeType(context, mimeType)) {
                        mimeTypes.add(mimeType);
                    }
                }
            }
        }
        cursor.close();

        // always add classic phone contacts
        mimeTypes.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

        long end = System.currentTimeMillis();
        Log.i("time", (end - start) + " milliseconds to load " + mimeTypes.size() + " supported mime types");

        return mimeTypes;
    }

    private static boolean isSupportedMimeType(Context context, String mimeType) {
        if (mimeType == null) {
            return false;
        }
        if (UNSUPPORTED_MIME_TYPES.contains(mimeType)) {
            return false;
        }
        // check if intent for custom mime type is registered
        Intent intent = getRegisteredIntentByMimeType(context, mimeType, -1, "");
        return intent != null;
    }

    /**
     * @param context
     * @return a list of all mime types that should be shown
     */
    public static Set<String> getActiveMimeTypes(Context context) {
        long start = System.currentTimeMillis();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> selectedMimeTypes = prefs.getStringSet("selected-contact-mime-types", getDefaultMimeTypes());
        Set<String> supportedMimeTypes = getSupportedMimeTypes(context);

        supportedMimeTypes.retainAll(selectedMimeTypes);

        long end = System.currentTimeMillis();
        Log.i("time", (end - start) + " milliseconds to load " + supportedMimeTypes.size() + " active mime types");

        return supportedMimeTypes;
    }

    /**
     * Create a new intent to view given row of contact data.
     *
     * @param mimeType           mimetype of contact data row
     * @param id                 id of contact data row
     * @param schemeSpecificPart
     * @return intent to view contact by mime type and id, null if no activity is registered for intent
     */
    public static Intent getRegisteredIntentByMimeType(Context context, String mimeType, long id, String schemeSpecificPart) {
        final Intent intent = getIntentByMimeType(mimeType, id, schemeSpecificPart);

        if (isIntentRegistered(context, intent)) {
            return intent;
        } else {
            return null;
        }
    }

    /**
     * create a new intent to view given row of contact data
     *
     * @param mimeType           mime type of contact data row
     * @param id                 id of contact data row
     * @param schemeSpecificPart
     * @return intent to view contact by mime type and id
     */
    public static Intent getIntentByMimeType(String mimeType, long id, String schemeSpecificPart) {
        Intent intent;
        if (ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
            final Uri phoneUri = Uri.fromParts("tel", Uri.encode(schemeSpecificPart), null);
            intent = new Intent(Intent.ACTION_CALL, phoneUri);
        } else if (ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
            final Uri mailUri = Uri.fromParts("mailto", schemeSpecificPart, null);
            intent = new Intent(Intent.ACTION_SENDTO, mailUri);
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
            final Uri uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id);
            intent.setDataAndType(uri, mimeType);

        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * @param context
     * @param intent
     * @return true if any activity is registered for given intent
     */
    private static boolean isIntentRegistered(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> receiverList = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return receiverList.size() > 0;
    }

    /**
     * strip common vnd.android.cursor.item/ from mimeType
     *
     * @param mimeType
     * @return shortened version of mime type
     */
    public static String getShortMimeType(String mimeType) {
        return mimeType.replaceFirst("vnd\\.android\\.cursor\\.item/", "");
    }

    /**
     * @return mimeTypes that are shown by default
     */
    public static Set<String> getDefaultMimeTypes() {
        return new TreeSet<>(Collections.singletonList(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE));
    }
}
