package fr.neamar.kiss.result;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.ui.ImprovedQuickContactBadge;

public class ContactsResult extends Result {
    private final ContactsPojo contactPojo;
    private final QueryInterface queryInterface;

    public ContactsResult(QueryInterface queryInterface, ContactsPojo contactPojo) {
        super();
        this.pojo = this.contactPojo = contactPojo;
        this.queryInterface = queryInterface;
    }

    @Override
    public View display(Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_contact);

        // Contact name
        TextView contactName = (TextView) v.findViewById(R.id.item_contact_name);
        contactName.setText(enrichText(contactPojo.displayName));

        // Contact phone
        TextView contactPhone = (TextView) v.findViewById(R.id.item_contact_phone);
        contactPhone.setText(contactPojo.phone);

        // Contact photo
        ImprovedQuickContactBadge contactIcon = (ImprovedQuickContactBadge) v
                .findViewById(R.id.item_contact_icon);
        contactIcon.setImageDrawable(getDrawable(context));

        contactIcon.assignContactUri(Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                String.valueOf(contactPojo.lookupKey)));
        contactIcon.setExtraOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                recordLaunch(v.getContext());
                queryInterface.launchOccurred(-1, ContactsResult.this);
            }
        });

        // Phone action
        ImageButton phoneButton = (ImageButton) v.findViewById(R.id.item_contact_action_phone);
        // Message action
        ImageButton messageButton = (ImageButton) v.findViewById(R.id.item_contact_action_message);

        PackageManager pm = context.getPackageManager();

        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            phoneButton.setVisibility(View.VISIBLE);
            messageButton.setVisibility(View.VISIBLE);
            phoneButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchCall(v.getContext());
                }
            });

            messageButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    launchMessaging(v.getContext());
                }
            });

            /*
            Temporary disabled, see https://github.com/Neamar/KISS/issues/540
            if (contactPojo.homeNumber)
                messageButton.setVisibility(View.INVISIBLE);
            else
            */
                messageButton.setVisibility(View.VISIBLE);

        } else {
            phoneButton.setVisibility(View.INVISIBLE);
            messageButton.setVisibility(View.INVISIBLE);
        }

        return v;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected PopupMenu buildPopupMenu(Context context, final RecordAdapter parent, View parentView) {
        return inflatePopupMenu(R.menu.menu_item_contact, context, parentView);
    }

    @Override
    protected Boolean popupMenuClickHandler(Context context, RecordAdapter parent, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_contact_copy_phone:
                copyPhone(context, contactPojo);
                return true;
        }

        return super.popupMenuClickHandler(context, parent, item);
    }

    @SuppressWarnings("deprecation")
    private void copyPhone(Context context, ContactsPojo contactPojo) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard =
                    (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(contactPojo.phone);
        } else {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(
                    "Phone number for " + contactPojo.displayName,
                    contactPojo.phone);
            clipboard.setPrimaryClip(clip);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Drawable getDrawable(Context context) {
        if (contactPojo.icon != null) {
            InputStream inputStream = null;
            try {
                inputStream = context.getContentResolver().openInputStream(contactPojo.icon);
                return Drawable.createFromStream(inputStream, null);
            } catch (FileNotFoundException ignored) {
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        // Default icon
        return context.getResources().getDrawable(R.drawable.ic_contact);
    }

    @Override
    public void doLaunch(Context context, View v) {
        Intent viewContact = new Intent(Intent.ACTION_VIEW);

        viewContact.setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                String.valueOf(contactPojo.lookupKey)));
        viewContact.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        viewContact.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(viewContact);
    }

    private void launchMessaging(final Context context) {
        String url = "sms:" + Uri.encode(contactPojo.phone);
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                recordLaunch(context);
                queryInterface.launchOccurred(-1, ContactsResult.this);
            }
        }, KissApplication.TOUCH_DELAY);

    }

    private void launchCall(final Context context) {
        String url = "tel:" + Uri.encode(contactPojo.phone);
        Intent i = new Intent(Intent.ACTION_CALL, Uri.parse(url));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                recordLaunch(context);
                queryInterface.launchOccurred(-1, ContactsResult.this);
            }
        }, KissApplication.TOUCH_DELAY);

    }
}
