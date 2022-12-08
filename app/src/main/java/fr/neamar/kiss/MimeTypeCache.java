package fr.neamar.kiss;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.XmlResourceParser;
import android.provider.ContactsContract;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.utils.MimeTypeUtils;
import fr.neamar.kiss.utils.PackageManagerUtils;

public class MimeTypeCache {

    private static final String CONTACTS_DATA_KIND = "ContactsDataKind";
    private static final String CONTACT_ATTR_MIME_TYPE = "mimeType";
    private static final String CONTACT_ATTR_DETAIL_COLUMN = "detailColumn";

    private static final String[] METADATA_CONTACTS_NAMES = new String[]{
            "android.provider.ALTERNATE_CONTACTS_STRUCTURE",
            "android.provider.CONTACTS_STRUCTURE"
    };

    // Cached componentName
    private final Map<String, ComponentName> componentNames;
    // Cached label
    private final Map<String, String> labels;
    // Cached detail columns
    private Map<String, String> detailColumns;


    public MimeTypeCache() {
        this.componentNames = new HashMap<>();
        this.labels = new HashMap<>();
        this.detailColumns = null;
    }

    public void clearCache() {
        this.componentNames.clear();
        this.labels.clear();
        this.detailColumns = null;
    }

    /**
     * @param context
     * @param mimeType
     * @return label for best matching app by mimetype
     */
    public String getLabel(Context context, String mimeType) {
        if (labels.containsKey(mimeType)) {
            return labels.get(mimeType);
        }

        final Intent intent = MimeTypeUtils.getIntentByMimeType(mimeType, -1, "");
        String label = PackageManagerUtils.getLabel(context, intent);
        labels.put(mimeType, label);
        return label;
    }

    public ComponentName getComponentName(Context context, String mimeType) {
        if (componentNames.containsKey(mimeType)) {
            return componentNames.get(mimeType);
        }

        final Intent intent = MimeTypeUtils.getIntentByMimeType(mimeType, -1, "");
        ComponentName componentName = PackageManagerUtils.getComponentName(context, intent);
        this.componentNames.put(mimeType, componentName);

        return componentName;
    }

    /**
     * @param context
     * @return all mime types and related data columns from contact sync adapters
     */
    public Map<String, String> fetchDetailColumns(Context context) {
        if (detailColumns == null) {
            long start = System.currentTimeMillis();

            detailColumns = new HashMap<>();

            final Set<String> contactSyncableTypes = new HashSet<>();

            SyncAdapterType[] syncAdapterTypes = ContentResolver.getSyncAdapterTypes();
            for (SyncAdapterType type : syncAdapterTypes) {
                if (type.authority.equals(ContactsContract.AUTHORITY)) {
                    contactSyncableTypes.add(type.accountType);
                }
            }

            AuthenticatorDescription[] authenticatorDescriptions = ((AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE)).getAuthenticatorTypes();
            for (AuthenticatorDescription auth : authenticatorDescriptions) {
                if (contactSyncableTypes.contains(auth.type)) {
                    XmlResourceParser parser = loadContactsXml(context, auth.packageName);
                    if (parser != null) {
                        try {
                            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                                if (CONTACTS_DATA_KIND.equals(parser.getName())) {
                                    String foundMimeType = null;
                                    String foundDetailColumn = null;
                                    int attributeCount = parser.getAttributeCount();
                                    for (int i = 0; i < attributeCount; i++) {
                                        String attr = parser.getAttributeName(i);
                                        String value = parser.getAttributeValue(i);
                                        if (CONTACT_ATTR_MIME_TYPE.equals(attr)) {
                                            foundMimeType = value;
                                        } else if (CONTACT_ATTR_DETAIL_COLUMN.equals(attr)) {
                                            foundDetailColumn = value;
                                        }
                                    }
                                    if (foundMimeType != null) {
                                        detailColumns.put(foundMimeType, foundDetailColumn);
                                    }
                                }
                            }
                        } catch (IOException | XmlPullParserException ignored) {
                        }
                    }
                }
            }

            // Add additional data columns for known mime types
            detailColumns.put(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Email.ADDRESS);
            detailColumns.put(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Phone.NUMBER);

            long end = System.currentTimeMillis();
            Log.i("time", (end - start) + " milliseconds to fetch detail data columns");
        }
        return detailColumns;
    }

    /**
     * Loads contact description from other sync providers, search for ContactsAccountType or ContactsSource
     * detailed description can be found here https://developer.android.com/guide/topics/providers/contacts-provider
     *
     * @param context
     * @param packageName
     * @return XmlResourceParser for contacts.xml, null if nothing found
     */
    @SuppressLint("WrongConstant")
    public XmlResourceParser loadContactsXml(Context context, String packageName) {
        final PackageManager pm = context.getPackageManager();
        final Intent intent = new Intent("android.content.SyncAdapter").setPackage(packageName);
        final List<ResolveInfo> intentServices = pm.queryIntentServices(intent,
                PackageManager.GET_META_DATA | PackageManager.GET_SERVICES);

        if (intentServices != null) {
            for (final ResolveInfo resolveInfo : intentServices) {
                final ServiceInfo serviceInfo = resolveInfo.serviceInfo;
                if (serviceInfo == null) {
                    continue;
                }
                for (String metadataName : METADATA_CONTACTS_NAMES) {
                    final XmlResourceParser parser = serviceInfo.loadXmlMetaData(
                            pm, metadataName);
                    if (parser != null) {
                        return parser;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param context
     * @param mimeType
     * @return related detail data column for mime type
     */
    public String getDetailColumn(Context context, String mimeType) {
        Map<String, String> detailColumns = fetchDetailColumns(context);
        return detailColumns.get(mimeType);
    }

    private static String greatestCommonPrefix(String a, String b) {
        int minLength = Math.min(a.length(), b.length());
        for (int i = 0; i < minLength; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.substring(0, i);
            }
        }
        return a.substring(0, minLength);
    }

    /**
     * Generates unique labels for given mime types, appends mimeType itself if an app supports multiple mime types
     *
     * @param context
     * @param mimeTypes
     * @return labels for given mime types
     */
    public Map<String, String> getUniqueLabels(Context context, Set<String> mimeTypes) {
        Map<String, String> uniqueLabels = new HashMap<>(mimeTypes.size());

        // get labels for mime types
        Map<String, Set<String>> mappedMimeTypes = new HashMap<>();
        for (String mimeType : mimeTypes) {
            String label = getLabel(context, mimeType);
            Set<String> mimeTypesPerLabel = mappedMimeTypes.get(label);
            if (mimeTypesPerLabel == null) {
                mimeTypesPerLabel = new HashSet<>();
                mappedMimeTypes.put(label, mimeTypesPerLabel);
            }
            mimeTypesPerLabel.add(mimeType);
        }

        // check supported mime types and make labels unique
        for (String mimeType : mimeTypes) {
            String label = getLabel(context, mimeType);
            Set<String> mimeTypesPerLabel = mappedMimeTypes.get(label);
            if (mimeTypesPerLabel != null && mimeTypesPerLabel.size() > 1) {
                String prefix = null;
                for (String labelMimeType : mimeTypesPerLabel) {
                    if (labelMimeType != null) {
                        String shortLabel = MimeTypeUtils.getShortMimeType(labelMimeType);
                        if (prefix == null) {
                            prefix = shortLabel;
                        } else {
                            prefix = greatestCommonPrefix(prefix, shortLabel);
                        }
                    }
                }
                if (prefix != null) {
                    // assume dot separated words
                    int pos = prefix.lastIndexOf('.');
                    if (pos == -1) {
                        // no dot found, remove whole prefix
                        pos = prefix.length();
                    } else {
                        // remove words before the dot
                        pos += 1;
                    }
                    label += " (" + MimeTypeUtils.getShortMimeType(mimeType).substring(pos) + ")";
                } else {
                    // no prefix !?
                    label += " (" + MimeTypeUtils.getShortMimeType(mimeType) + ")";
                }
            }
            uniqueLabels.put(mimeType, label);
        }

        return uniqueLabels;
    }
}
