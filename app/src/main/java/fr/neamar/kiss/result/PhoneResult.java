package fr.neamar.kiss.result;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Collections;

import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.forwarder.Permission;
import fr.neamar.kiss.pojo.PhonePojo;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.FuzzyScore;

public class PhoneResult extends Result {
    private final PhonePojo phonePojo;

    PhoneResult(PhonePojo phonePojo) {
        super(phonePojo);
        this.phonePojo = phonePojo;
    }

    @NonNull
    @Override
    public View display(Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null)
            view = inflateFromId(context, R.layout.item_phone, parent);

        TextView phoneText = view.findViewById(R.id.item_phone_text);
        String text = String.format(context.getString(R.string.ui_item_phone), phonePojo.phone);
        int pos = text.indexOf(phonePojo.phone);
        int len = phonePojo.phone.length();
        displayHighlighted(text, Collections.singletonList(new Pair<Integer, Integer>(pos, pos + len)), phoneText, context);

        ((ImageView) view.findViewById(R.id.item_phone_icon)).setColorFilter(getThemeFillColor(context), PorterDuff.Mode.SRC_IN);

        return view;
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView) {
        adapter.add(new ListPopup.Item(context, R.string.menu_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_add));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_phone_create));
        adapter.add(new ListPopup.Item(context, R.string.ui_item_contact_hint_message));

        return inflatePopupMenu(adapter, context);
    }

    @Override
    protected boolean popupMenuClickHandler(Context context, RecordAdapter parent, int stringId, View parentView) {
        switch (stringId) {
            case R.string.menu_phone_create:
                // Create a new contact with this phone number
                Intent createIntent = new Intent(Intent.ACTION_INSERT);
                createIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                createIntent.putExtra(ContactsContract.Intents.Insert.PHONE, phonePojo.phone);
                createIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(createIntent);
                return true;
            case R.string.ui_item_contact_hint_message:
                String url = "sms:" + phonePojo.phone;
                Intent messageIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                messageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(messageIntent);
                return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void doLaunch(Context context, View v) {
        Intent phone = new Intent(Intent.ACTION_CALL);
        phone.setData(Uri.parse("tel:" + Uri.encode(phonePojo.phone)));
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            phone.setSourceBounds(v.getClipBounds());
        }

        phone.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Make sure we have permission to call someone as this is considered a dangerous permission
        if (Permission.ensureCallPhonePermission(phone)) {
            context.startActivity(phone);
        }
    }

    @Override
    public Drawable getDrawable(Context context) {
        //noinspection: getDrawable(int, Theme) requires SDK 21+
        return context.getResources().getDrawable(android.R.drawable.ic_menu_call);
    }


}
