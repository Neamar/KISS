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
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;

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
    // Setting from before contact actions, when contacts from other apps were shown as separate results
    public static final String SELECTED_MIME_TYPES_KEY = "selected-contact-mime-types";

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

        // Let the contacts provider skip the rows we don't want: they are most of the table, and it has to go through all of them otherwise
        List<String> ignoredMimeTypes = new ArrayList<>(UNSUPPORTED_MIME_TYPES);
        ignoredMimeTypes.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        String selection = ContactsContract.Data.MIMETYPE + " NOT IN (" + TextUtils.join(",", Collections.nCopies(ignoredMimeTypes.size(), "?")) + ")";

        try (Cursor cursor = context.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.Data.MIMETYPE}, selection, ignoredMimeTypes.toArray(new String[0]), null)) {
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    int mimeTypeIndex = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
                    while (cursor.moveToNext()) {
                        mimeTypes.add(cursor.getString(mimeTypeIndex));
                    }
                }
            }
        }
        mimeTypes.removeIf(mimeType -> !isSupportedMimeType(context, mimeType));

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
     * @return a set of all mime types selected in settings for a contact button
     */
    public static Set<String> getSelectedMimeTypes(Context context) {
        Set<String> selectedMimeTypes = new HashSet<>();
        for (String side : CONTACT_ACTION_SIDES) {
            selectedMimeTypes.add(getContactAction(context, side));
        }
        selectedMimeTypes.remove(CONTACT_ACTION_NONE);
        selectedMimeTypes.remove(CONTACT_ACTION_SMS);
        selectedMimeTypes.remove(CONTACT_ACTION_CALL);
        return selectedMimeTypes;
    }

    /**
     * @param side {@link #CONTACT_ACTION_LEFT} or {@link #CONTACT_ACTION_RIGHT}
     * @return action used when the configured one isn't available for a contact
     */
    public static String getDefaultContactAction(String side) {
        return CONTACT_ACTION_LEFT.equals(side) ? CONTACT_ACTION_SMS : CONTACT_ACTION_CALL;
    }

    /**
     * @param context
     * @param side    {@link #CONTACT_ACTION_LEFT} or {@link #CONTACT_ACTION_RIGHT}
     * @return action configured in settings for given contact button
     */
    public static String getContactAction(Context context, String side) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("contact-action-" + side, getDefaultContactAction(side));
    }

    /**
     * @param context
     * @param side      {@link #CONTACT_ACTION_LEFT} or {@link #CONTACT_ACTION_RIGHT}
     * @param lookupKey lookup key of the contact
     * @return actions for given contact button, in order of preference: contact override, settings, default
     */
    public static List<String> getContactActions(Context context, String side, String lookupKey) {
        List<String> actions = new ArrayList<>(3);
        String override = getContactActionOverride(context, side, lookupKey);
        if (override != null) {
            actions.add(override);
        }
        actions.add(getContactAction(context, side));
        actions.add(getDefaultContactAction(side));
        return actions;
    }

    /**
     * @param context
     * @param mimeTypes mime types of contact data, phone is ignored as it is already handled by SMS and call
     * @return contact actions with their labels: SMS, call, then given mime types sorted by label
     */
    public static Map<String, String> getContactActionLabels(Context context, Set<String> mimeTypes) {
        Set<String> appMimeTypes = new HashSet<>(mimeTypes);
        appMimeTypes.remove(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        Map<String, String> appLabels = KissApplication.getMimeTypeCache(context).getActionLabels(context, appMimeTypes);
        List<String> sortedMimeTypes = new ArrayList<>(appMimeTypes);
        Collections.sort(sortedMimeTypes, (a, b) -> appLabels.get(a).compareToIgnoreCase(appLabels.get(b)));

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(CONTACT_ACTION_SMS, context.getString(R.string.contact_action_sms));
        labels.put(CONTACT_ACTION_CALL, context.getString(R.string.ui_item_contact_hint_call));
        for (String mimeType : sortedMimeTypes) {
            labels.put(mimeType, appLabels.get(mimeType));
        }
        return labels;
    }

    /**
     * Contacts from other apps (WhatsApp, Signal, ...) used to be shown as separate results.
     * They are now actions of the phone contact: use the first one for the left button,
     * and the second one for the right button on devices that can't call.
     *
     * @param context
     * @param prefs   settings to convert
     * @param editor  editor of these settings, to commit afterwards
     */
    public static void convertSelectedMimeTypes(Context context, SharedPreferences prefs, SharedPreferences.Editor editor) {
        List<String> mimeTypes = new ArrayList<>(prefs.getStringSet(SELECTED_MIME_TYPES_KEY, emptySet()));
        mimeTypes.remove(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        // Prefer a messaging app over email
        if (mimeTypes.size() > 1) {
            mimeTypes.remove(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        }
        Collections.sort(mimeTypes);
        if (!mimeTypes.isEmpty()) {
            editor.putString("contact-action-" + CONTACT_ACTION_LEFT, mimeTypes.get(0));
        }
        if (mimeTypes.size() > 1 && !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            editor.putString("contact-action-" + CONTACT_ACTION_RIGHT, mimeTypes.get(1));
        }
        editor.remove(SELECTED_MIME_TYPES_KEY);
    }

    // Overrides are stored as "<action> <lookup key>", actions never contain spaces
    private static String getContactActionOverridesKey(String side) {
        return "contact-action-" + side + "-overrides";
    }

    public static String getContactActionOverride(String action, String lookupKey) {
        return action + ' ' + lookupKey;
    }

    /**
     * @param context
     * @return overrides of both contact buttons, see {@link #getContactActionOverride(String, String)}
     */
    public static Set<String> getContactActionOverrides(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> overrides = new HashSet<>();
        for (String side : CONTACT_ACTION_SIDES) {
            overrides.addAll(prefs.getStringSet(getContactActionOverridesKey(side), emptySet()));
        }
        return overrides;
    }

    /**
     * @param context
     * @param side      {@link #CONTACT_ACTION_LEFT} or {@link #CONTACT_ACTION_RIGHT}
     * @param lookupKey lookup key of the contact
     * @return action chosen for given contact button of this contact only, null if none
     */
    @Nullable
    public static String getContactActionOverride(Context context, String side, String lookupKey) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (String override : prefs.getStringSet(getContactActionOverridesKey(side), emptySet())) {
            int separator = override.indexOf(' ');
            if (separator > 0 && override.substring(separator + 1).equals(lookupKey)) {
                return override.substring(0, separator);
            }
        }
        return null;
    }

    /**
     * @param context
     * @param side      {@link #CONTACT_ACTION_LEFT} or {@link #CONTACT_ACTION_RIGHT}
     * @param lookupKey lookup key of the contact
     * @param action    action for given contact button of this contact only, null to use settings
     */
    public static void setContactActionOverride(Context context, String side, String lookupKey, @Nullable String action) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = getContactActionOverridesKey(side);
        Set<String> overrides = new HashSet<>(prefs.getStringSet(key, emptySet()));
        overrides.removeIf(override -> override.substring(override.indexOf(' ') + 1).equals(lookupKey));
        if (action != null) {
            overrides.add(getContactActionOverride(action, lookupKey));
        }
        prefs.edit().putStringSet(key, overrides).apply();
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
}
