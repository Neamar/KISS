package fr.neamar.kiss.result;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.ImageViewCompat;
import androidx.preference.PreferenceManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.icons.IconPack;
import fr.neamar.kiss.pojo.ContactData;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.ui.ImprovedQuickContactBadge;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.ClipboardUtils;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.MimeTypeUtils;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;

public class ContactsResult extends CallResult<ContactsPojo> {

    private final QueryInterface queryInterface;
    private volatile Drawable icon = null;
    private final Map<String, Drawable> appDrawables = new ConcurrentHashMap<>();
    private static final String TAG = ContactsResult.class.getSimpleName();

    ContactsResult(QueryInterface queryInterface, @NonNull ContactsPojo pojo) {
        super(pojo);
        this.queryInterface = queryInterface;
    }

    @NonNull
    @Override
    public View display(Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null) {
            view = inflateFromId(context, R.layout.item_contact, parent);
            // Keep tint from layout, to restore it after showing an app icon
            ImageButton messageButton = view.findViewById(R.id.item_contact_action_message);
            messageButton.setTag(ImageViewCompat.getImageTintList(messageButton));
            ImageButton phoneButton = view.findViewById(R.id.item_contact_action_phone);
            phoneButton.setTag(ImageViewCompat.getImageTintList(phoneButton));
        }

        // Contact name
        TextView contactName = view.findViewById(R.id.item_contact_name);
        if (!TextUtils.isEmpty(pojo.getName())) {
            displayHighlighted(pojo.normalizedName, pojo.getName(), fuzzyScore, contactName, context);
        }

        // Contact phone or IM identifier
        TextView contactPhone = view.findViewById(R.id.item_contact_phone);
        if (!TextUtils.isEmpty(pojo.phone)) {
            contactPhone.setVisibility(View.VISIBLE);
            displayHighlighted(pojo.normalizedPhone, pojo.phone, fuzzyScore, contactPhone, context);
        } else if (pojo.getContactData() != null && !TextUtils.isEmpty(pojo.getContactData().getIdentifier())) {
            contactPhone.setVisibility(View.VISIBLE);
            displayHighlighted(pojo.getContactData().getNormalizedIdentifier(), pojo.getContactData().getIdentifier(), fuzzyScore, contactPhone, context);
        } else {
            contactPhone.setVisibility(View.GONE);
        }

        // Contact nickname
        TextView contactNickname = view.findViewById(R.id.item_contact_nickname);
        if (TextUtils.isEmpty(pojo.getNickname())) {
            contactNickname.setVisibility(View.GONE);
        } else {
            contactNickname.setVisibility(View.VISIBLE);
            displayHighlighted(pojo.normalizedNickname, pojo.getNickname(), fuzzyScore, contactNickname, context);
        }

        // Contact photo
        ImprovedQuickContactBadge contactIcon = view
                .findViewById(R.id.item_contact_icon);

        boolean hideIcons = isHideIcons(context);
        if (!hideIcons) {
            this.setAsyncDrawable(contactIcon);
        } else {
            contactIcon.setImageDrawable(null);
        }

        Uri contactUri = ContactsContract.Contacts.CONTENT_LOOKUP_URI;
        contactUri = Uri.withAppendedPath(contactUri, String.valueOf(pojo.lookupKey));
        contactUri = Uri.withAppendedPath(contactUri, String.valueOf(pojo.getContactId()));
        contactIcon.assignContactUri(contactUri);

        contactIcon.setExtraOnClickListener(v -> recordLaunch(v.getContext(), queryInterface));

        // Contact actions, the right button never repeats the action of the left one
        String leftAction = getAvailableAction(context, MimeTypeUtils.getContactActions(context, MimeTypeUtils.CONTACT_ACTION_LEFT), null);
        String rightAction = getAvailableAction(context, MimeTypeUtils.getContactActions(context, MimeTypeUtils.CONTACT_ACTION_RIGHT), leftAction);
        displayAction(context, view.findViewById(R.id.item_contact_action_message), leftAction, View.INVISIBLE);
        displayAction(context, view.findViewById(R.id.item_contact_action_phone), rightAction, View.GONE);

        return view;
    }

    /**
     * @param context context
     * @param actions configured actions, in order of preference
     * @param exclude action that must not be used
     * @return first action available for this contact, null if none
     */
    private String getAvailableAction(Context context, List<String> actions, String exclude) {
        boolean hasPhone = pojo.phone != null && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        for (String action : actions) {
            if (action.equals(exclude)) {
                continue;
            }
            boolean available;
            if (MimeTypeUtils.CONTACT_ACTION_SMS.equals(action)) {
                available = hasPhone && !pojo.isHomeNumber();
            } else if (MimeTypeUtils.CONTACT_ACTION_CALL.equals(action)) {
                available = hasPhone;
            } else {
                available = pojo.getContactData(action) != null;
            }
            if (available) {
                return action;
            }
        }
        return null;
    }

    private void displayAction(Context context, ImageButton button, String action, int hiddenVisibility) {
        if (action == null) {
            button.setVisibility(hiddenVisibility);
            button.setOnClickListener(null);
            return;
        }

        button.setVisibility(View.VISIBLE);
        if (MimeTypeUtils.CONTACT_ACTION_SMS.equals(action) || MimeTypeUtils.CONTACT_ACTION_CALL.equals(action)) {
            boolean sms = MimeTypeUtils.CONTACT_ACTION_SMS.equals(action);
            ImageViewCompat.setImageTintList(button, (ColorStateList) button.getTag());
            button.setContentDescription(context.getString(sms ? R.string.ui_item_contact_hint_message : R.string.ui_item_contact_hint_call));
            int drawableId = sms ? R.drawable.ic_message : R.drawable.ic_phone;
            setAsyncDrawable(button, 0, false, () -> true, ctx -> AppCompatResources.getDrawable(ctx, drawableId), drawable -> {
            });
            button.setOnClickListener(v -> {
                if (sms) {
                    launchMessaging(v.getContext(), v, pojo.phone);
                } else {
                    launchCall(v.getContext(), v, pojo.phone);
                }
                recordLaunch(context, queryInterface);
            });
        } else {
            ImageViewCompat.setImageTintList(button, null);
            button.setContentDescription(KissApplication.getMimeTypeCache(context).getLabel(context, action));
            setAsyncDrawable(button, 0, false, () -> appDrawables.containsKey(action), ctx -> getAppDrawable(ctx, action), drawable -> appDrawables.put(action, drawable));
            button.setOnClickListener(v -> {
                launchIm(v.getContext(), v, pojo.getContactData(action));
                recordLaunch(context, queryInterface);
            });
        }
    }

    private Drawable getAppDrawable(Context context, String mimeType) {
        Drawable appDrawable = appDrawables.get(mimeType);
        if (appDrawable == null) {
            ComponentName componentName = KissApplication.getMimeTypeCache(context).getComponentName(context, mimeType);
            if (componentName != null) {
                IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();
                appDrawable = iconsHandler.getDrawableIconForPackage(PackageManagerUtils.getLaunchingComponent(context, componentName, pojo.getUserHandle()), pojo.getUserHandle());
            }
            if (appDrawable == null) {
                // This should never happen, let's just return the generic activity icon
                appDrawable = PackageManagerUtils.getDefaultActivityIcon(context);
            }
            // Same size as SMS and call icons
            int size = DrawableUtils.dpToPx(context, 24);
            appDrawable = new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(DrawableUtils.drawableToBitmap(appDrawable), size, size, true));
        }
        return appDrawable;
    }

    @Override
    protected void buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter) {
        super.buildPopupMenu(context, adapter);

        if (!TextUtils.isEmpty(pojo.phone)) {
            adapter.add(new ListPopup.Item(context, R.string.menu_contact_copy_phone));
        }
    }

    @Override
    protected boolean popupMenuClickHandler(Context context, RecordAdapter parent, int stringId, View parentView) {
        if (stringId == R.string.menu_contact_copy_phone) {
            copyPhone(context, pojo);
            return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }

    private void copyPhone(Context context, ContactsPojo pojo) {
        ClipboardUtils.setClipboard(context, "Phone number for " + pojo.getName(), pojo.phone);
    }

    @Override
    boolean isDrawableCached() {
        return icon != null;
    }

    @Override
    void setDrawableCache(Drawable drawable) {
        icon = drawable;
    }

    @Override
    public Drawable getDrawable(Context context) {
        if (icon == null) {
            synchronized (this) {
                if (icon == null) {
                    IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();
                    icon = iconsHandler.getDrawableForContact(pojo, pojo.icon, getBackgroundColor(context), getTextColor(context), getThemeFillColor(context));
                }
            }
        }
        return icon;
    }

    @Override
    public void inflateFavorite(@NonNull Context context, @NonNull View favoriteView) {
        Drawable drawable = getDrawable(context);
        ImageView favoriteImage = favoriteView.findViewById(R.id.favorite);
        favoriteImage.setImageDrawable(drawable);
        favoriteView.setContentDescription(pojo.getName());
    }

    private void launchContactView(Context context, View v) {
        Uri contactUri = ContactsContract.Contacts.getLookupUri(pojo.getContactId(), pojo.lookupKey);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pojo.getContactData() != null) {
            ContactsContract.QuickContact.showQuickContact(
                    context,
                    getViewBounds(v),
                    contactUri,
                    null,
                    pojo.getContactData().getMimeType());
        } else {
            ContactsContract.QuickContact.showQuickContact(
                    context,
                    getViewBounds(v),
                    contactUri,
                    ContactsContract.QuickContact.MODE_LARGE,
                    null);
        }
    }

    @Override
    public void doLaunch(Context context, View v) {
        SharedPreferences settingPrefs = PreferenceManager.getDefaultSharedPreferences(v.getContext());
        boolean callContactOnClick = settingPrefs.getBoolean("call-contact-on-click", false);

        if (callContactOnClick) {
            launchCall(context, v, pojo.phone);
        } else {
            launchContactView(context, v);
        }
    }

    private void launchIm(final Context context, final View view, final ContactData contactData) {
        Intent intent = MimeTypeUtils.getRegisteredIntentByMimeType(context, contactData.getMimeType(), contactData.getId(), contactData.getIdentifier());
        if (intent != null) {
            setSourceBounds(intent, view);
            context.startActivity(intent);
        }
    }

    @Override
    protected boolean isAllowedAsFavorite() {
        return true;
    }

    @Override
    protected boolean canRemoveFromHistory(Context context) {
        return true;
    }

    /**
     *
     * @param context
     * @param iconPack
     * @return true, if contact has no icon set
     */
    @Override
    protected boolean canHaveCustomIcon(Context context, IconPack iconPack) {
        return pojo.icon == null;
    }
}
