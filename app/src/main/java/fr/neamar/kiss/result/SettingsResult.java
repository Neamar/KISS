package fr.neamar.kiss.result;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.SettingsPojo;

public class SettingsResult extends Result {
    private final SettingsPojo settingPojo;

    public SettingsResult(SettingsPojo settingPojo) {
        super();
        this.pojo = this.settingPojo = settingPojo;
    }

    @Override
    public View display(Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_setting);

        String settingPrefix = "<small><small>" + context.getString(R.string.settings_prefix) + "</small></small>";
        TextView settingName = (TextView) v.findViewById(R.id.item_setting_name);
        settingName.setText(TextUtils.concat(Html.fromHtml(settingPrefix), enrichText(settingPojo.displayName)));

        ImageView settingIcon = (ImageView) v.findViewById(R.id.item_setting_icon);
        settingIcon.setImageDrawable(getDrawable(context));
        settingIcon.setColorFilter(getThemeFillColor(context), Mode.SRC_IN);

        return v;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected PopupMenu buildPopupMenu(Context context, final RecordAdapter parent, View parentView) {
        PopupMenu menu = new PopupMenu(context, parentView);

        inflateBaseMenu(context, menu);
        return menu;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Drawable getDrawable(Context context) {
        if (settingPojo.icon != -1) {
            return context.getResources().getDrawable(settingPojo.icon);
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
