package fr.neamar.kiss.result;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.PhonePojo;

public class PhoneResult extends Result {
    private final PhonePojo phonePojo;

    public PhoneResult(PhonePojo phonePojo) {
        super();
        this.pojo = this.phonePojo = phonePojo;
    }

    @Override
    public View display(Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_phone);

        TextView appName = (TextView) v.findViewById(R.id.item_phone_text);
        String text = context.getString(R.string.ui_item_phone);
        appName.setText(enrichText(String.format(text, "{" + phonePojo.phone + "}")));

        ((ImageView) v.findViewById(R.id.item_phone_icon)).setColorFilter(getThemeFillColor(context), PorterDuff.Mode.SRC_IN);

        return v;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected PopupMenu buildPopupMenu(Context context, final RecordAdapter parent, View parentView) {
        return inflatePopupMenu(R.menu.menu_item_phone, context, parentView);
    }

    @Override
    protected Boolean popupMenuClickHandler(Context context, RecordAdapter parent, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_phone_createcontact:
                // Create a new contact with this phone number
                Intent createIntent = new Intent(Intent.ACTION_INSERT);
                createIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                createIntent.putExtra(ContactsContract.Intents.Insert.PHONE, phonePojo.phone);
                createIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(createIntent);
                return true;
            case R.id.item_phone_sendmessage:
                String url = "sms:" + phonePojo.phone;
                Intent messageIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                messageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(messageIntent);
                return true;
        }

        return super.popupMenuClickHandler(context, parent, item);
    }

    @Override
    public void doLaunch(Context context, View v) {
        Intent phone = new Intent(Intent.ACTION_CALL);
        phone.setData(Uri.parse("tel:" + Uri.encode(phonePojo.phone)));

        phone.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(phone);
    }

    @Override
    public Drawable getDrawable(Context context) {
        return context.getResources().getDrawable(android.R.drawable.ic_menu_call);
    }
}
