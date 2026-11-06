package fr.neamar.kiss.utils;

import static java.util.Collections.emptySet;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String TAG = MimeTypeUtils.class.getSimpleName();

    // Contact buttons, and actions that can be assigned to them besides mime types
    public static final String CONTACT_ACTION_LEFT = "left";
    public static final String CONTACT_ACTION_RIGHT = "right";
    private static final String[] CONTACT_ACTION_SIDES = {CONTACT_ACTION_LEFT, CONTACT_ACTION_RIGHT};
    public static final String CONTACT_ACTION_NONE = "none";
    public static final String CONTACT_ACTION_SMS = "sms";
    public static final String CONTACT_ACTION_CALL = "call";

    // Phone number at the end of a contact data detail, like "Voice call +1 (234) 567-890"
    private static final Pattern TRAILING_PHONE_NUMBER = Pattern.compile("[\\s+0-9().-]*[0-9][\\s+0-9().-]*$");

    private MimeTypeUtils() {
    }

    /**
     * @param context
     * @return a set of all supported mime types from existing contacts
     */
    public static Set<String> getSupportedMimeTypes(Context context) {
        if (!Permission.checkPermission(context, Permission.PERMISSION_READ_CONTACTS)) {
            return emptySet();
        }

        long start = System.currentTimeMillis();

        Set<String> mimeTypes = new HashSet<>();

        try (Cursor cursor = context.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.Data.MIMETYPE}, null, null, null)) {
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
        }

        // always add classic phone contacts
        mimeTypes.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

        long end = System.currentTimeMillis();
        Log.i(TAG, (end - start) + " milliseconds to load " + mimeTypes.size() + " supported mime types");

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
     * @return a set of all supported mime types that are used by a contact action
     */
    public static Set<String> getActiveMimeTypes(Context context) {
        long start = System.currentTimeMillis();

        Set<String> selectedMimeTypes = new HashSet<>();
        for (String side : CONTACT_ACTION_SIDES) {
            selectedMimeTypes.addAll(getContactActions(context, side));
        }
        Set<String> supportedMimeTypes = getSupportedMimeTypes(context);

        supportedMimeTypes.retainAll(selectedMimeTypes);
        supportedMimeTypes.remove(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

        long end = System.currentTimeMillis();
        Log.i(TAG, (end - start) + " milliseconds to load " + supportedMimeTypes.size() + " active mime types");

        return supportedMimeTypes;
    }

    /**
     * @param context
     * @param side    {@link #CONTACT_ACTION_LEFT} or {@link #CONTACT_ACTION_RIGHT}
     * @return configured actions for given contact button, ordered from main action to last fallback
     */
    public static List<String> getContactActions(Context context, String side) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultAction = CONTACT_ACTION_LEFT.equals(side) ? CONTACT_ACTION_SMS : CONTACT_ACTION_CALL;
        String key = "contact-action-" + side;
        return Arrays.asList(
                prefs.getString(key, defaultAction),
                prefs.getString(key + "-fallback", defaultAction),
                prefs.getString(key + "-fallback-2", defaultAction)
        );
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
        return !receiverList.isEmpty();
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
     * @param text text that may end with a phone number
     * @return phone number at the end of text, null if none
     */
    @Nullable
    public static String getTrailingPhoneNumber(@Nullable String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = TRAILING_PHONE_NUMBER.matcher(text);
        return matcher.find() ? matcher.group().trim() : null;
    }
}
