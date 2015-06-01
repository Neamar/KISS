package fr.neamar.kiss.result;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.PhonePojo;

public class PhoneResult extends Result {
    private final PhonePojo phonePojo;

    public PhoneResult(PhonePojo phonePojo) {
        super();
        this.pojo = this.phonePojo = phonePojo;
    }

    @Override
    public View display(Context context, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_phone);

        TextView appName = (TextView) v.findViewById(R.id.item_phone_text);
        appName.setText(enrichText(context.getString(R.string.ui_item_phone) + " \"{"
                + phonePojo.phone + "}\""));

        return v;
    }

    @Override
    public void doLaunch(Context context, View v) {
        Intent phone = new Intent(Intent.ACTION_CALL);
        phone.setData(Uri.parse("tel:" + phonePojo.phone));

        phone.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(phone);
    }

}
