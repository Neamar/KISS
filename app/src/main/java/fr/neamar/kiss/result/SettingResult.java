package fr.neamar.kiss.result;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.SettingPojo;

public class SettingResult extends Result {
    private final SettingPojo settingPojo;

    public SettingResult(SettingPojo settingPojo) {
        super();
        this.pojo = this.settingPojo = settingPojo;
    }

    @Override
    public View display(Context context, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_setting);

        TextView settingName = (TextView) v.findViewById(R.id.item_setting_name);
        settingName.setText(enrichText(settingPojo.displayName));

        ImageView settingIcon = (ImageView) v.findViewById(R.id.item_setting_icon);
        settingIcon.setImageDrawable(getDrawable(context));

        return v;
    }

    @SuppressWarnings({"ResourceType", "deprecation"})
    @Override
    public Drawable getDrawable(Context context) {
        if (settingPojo.icon != -1) {
            TypedArray a = context.obtainStyledAttributes(R.style.AppTheme,
                    new int[]{settingPojo.icon});
            int attributeResourceId = a.getResourceId(0, -1);
            if (attributeResourceId != -1) {
                a.recycle();
                return context.getResources().getDrawable(attributeResourceId);
            }
            a.recycle();
        }

        return null;
    }

    @Override
    public void doLaunch(Context context, View v) {
        Intent intent = new Intent(settingPojo.settingName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
