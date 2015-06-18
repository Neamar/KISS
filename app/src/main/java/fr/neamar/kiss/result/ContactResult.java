package fr.neamar.kiss.result;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.ContactPojo;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.ui.ImprovedQuickContactBadge;

public class ContactResult extends Result {
    private final ContactPojo contactPojo;
    private final QueryInterface queryInterface;

    public ContactResult(QueryInterface queryInterface, ContactPojo contactPojo) {
        super();
        this.pojo = this.contactPojo = contactPojo;
        this.queryInterface = queryInterface;

        if (contactPojo.phone != null) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                contactPojo.phone = PhoneNumberUtils.formatNumber(contactPojo.phone, Locale.getDefault().getCountry());
            } else {
                contactPojo.phone = PhoneNumberUtils.formatNumber(contactPojo.phone);
            }
        }
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
                queryInterface.launchOccurred(-1, ContactResult.this);
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
        } else {
            phoneButton.setVisibility(View.INVISIBLE);
            messageButton.setVisibility(View.INVISIBLE);
        }

        if (contactPojo.homeNumber)
            messageButton.setVisibility(View.INVISIBLE);
        else
            messageButton.setVisibility(View.VISIBLE);

        return v;
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

    @Override
    public void fastLaunch(Context context) {
        launchMessaging(context);
    }

    private void launchMessaging(final Context context) {
        String url = "sms:" + contactPojo.phone;
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                recordLaunch(context);
                queryInterface.launchOccurred(-1, ContactResult.this);
            }
        }, KissApplication.TOUCH_DELAY);

    }

    private void launchCall(final Context context) {
        String url = "tel:" + contactPojo.phone;
        Intent i = new Intent(Intent.ACTION_CALL, Uri.parse(url));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                recordLaunch(context);
                queryInterface.launchOccurred(-1, ContactResult.this);
            }
        }, KissApplication.TOUCH_DELAY);

    }
}
