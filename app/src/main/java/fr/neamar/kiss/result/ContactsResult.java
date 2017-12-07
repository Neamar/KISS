package fr.neamar.kiss.result;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UiTweaks;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.ContactsPojo;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.ui.ImprovedQuickContactBadge;
import fr.neamar.kiss.ui.ListPopup;

public class ContactsResult extends Result {
    private final ContactsPojo contactPojo;
    private final QueryInterface queryInterface;
    private Drawable icon = null;

    class AsyncSetImage extends AsyncTask<Void, Void, Drawable>
    {
        final private View      view;
        final private ImageView image;
        AsyncSetImage( View view, ImageView image )
        {
            super();
            this.view = view;
            this.image = image;
        }

        @Override
        protected Drawable doInBackground( Void... voids )
        {
            if ( isCancelled() || view.getTag() != this )
                return null;
            return getDrawable( view.getContext() );
        }

        @Override
        protected void onPostExecute( Drawable drawable )
        {
            if ( isCancelled() || drawable == null )
                return;
            image.setImageDrawable( drawable );
            view.setTag( null );
        }
    }

    public ContactsResult(QueryInterface queryInterface, ContactsPojo contactPojo) {
        super();
        this.pojo = this.contactPojo = contactPojo;
        this.queryInterface = queryInterface;
    }

    @Override
    public View display(Context context, int position, View convertView) {
        View view = convertView;
        if (convertView == null)
            view = inflateFromId(context, R.layout.item_contact);

        // Contact name
        TextView contactName = (TextView) view.findViewById(R.id.item_contact_name);
        contactName.setText(enrichText(contactPojo.displayName, context));

        // Contact phone
        TextView contactPhone = (TextView) view.findViewById(R.id.item_contact_phone);
        contactPhone.setText(contactPojo.phone);

        // Contact photo
        ImprovedQuickContactBadge contactIcon = (ImprovedQuickContactBadge) view
                .findViewById(R.id.item_contact_icon);

        //contactIcon.setImageDrawable(getDrawable(context));
        if ( view.getTag() instanceof AsyncSetImage )
        {
            ((AsyncSetImage)view.getTag()).cancel( true );
            view.setTag( null );
        }
        if( isDrawableCached() )
        {
            contactIcon.setImageDrawable(getDrawable(contactIcon.getContext()));
        }
        else
        {
            view.setTag( new AsyncSetImage( view, contactIcon ).execute() );
        }

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

        int primaryColor = Color.parseColor(UiTweaks.getPrimaryColor(context));
        // Phone action
        ImageButton phoneButton = (ImageButton) view.findViewById(R.id.item_contact_action_phone);
        phoneButton.setColorFilter(primaryColor);
        // Message action
        ImageButton messageButton = (ImageButton) view.findViewById(R.id.item_contact_action_message);
        messageButton.setColorFilter(primaryColor);

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

            if (contactPojo.homeNumber)
                messageButton.setVisibility(View.INVISIBLE);
            else
                messageButton.setVisibility(View.VISIBLE);

        } else {
            phoneButton.setVisibility(View.INVISIBLE);
            messageButton.setVisibility(View.INVISIBLE);
        }

        return view;
    }

    @Override
    protected ListPopup buildPopupMenu( Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView ) {
        adapter.add( new ListPopup.Item( context, R.string.menu_remove ) );
        adapter.add( new ListPopup.Item( context, R.string.menu_contact_copy_phone ) );
        adapter.add( new ListPopup.Item( context, R.string.menu_favorites_add ) );
        adapter.add( new ListPopup.Item( context, R.string.menu_favorites_remove ) );

        return inflatePopupMenu(adapter, context );
    }

    @Override
    protected Boolean popupMenuClickHandler( Context context, RecordAdapter parent, int stringId ) {
        switch ( stringId ) {
            case R.string.menu_contact_copy_phone:
                copyPhone(context, contactPojo);
                return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId );
    }

    @SuppressWarnings("deprecation")
    private void copyPhone(Context context, ContactsPojo contactPojo) {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(
                "Phone number for " + contactPojo.displayName,
                contactPojo.phone);
        clipboard.setPrimaryClip(clip);
    }

    boolean isDrawableCached()
    {
        return icon != null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Drawable getDrawable(Context context) {
        if ( isDrawableCached() )
            return icon;
        if (contactPojo.icon != null) {
            InputStream inputStream = null;
            try {
                inputStream = context.getContentResolver().openInputStream(contactPojo.icon);
                return icon = Drawable.createFromStream(inputStream, null);
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
        return icon = context.getResources().getDrawable(R.drawable.ic_contact);
    }

    @Override
    public void doLaunch(Context context, View v) {
        Intent viewContact = new Intent(Intent.ACTION_VIEW);

        viewContact.setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                String.valueOf(contactPojo.lookupKey)));
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewContact.setSourceBounds(v.getClipBounds());
        }

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
