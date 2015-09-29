package fr.neamar.kiss.result;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.ShortcutPojo;

public class ShortcutResult extends Result {
    private final ShortcutPojo shortcutPojo;

    public ShortcutResult(ShortcutPojo shortcutPojo) {
        super();
        this.pojo = this.shortcutPojo = shortcutPojo;
    }

    @Override
    public View display(final Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_app);

        TextView appName = (TextView) v.findViewById(R.id.item_app_name);
        appName.setText(enrichText(shortcutPojo.displayName));

        final ImageView appIcon = (ImageView) v.findViewById(R.id.item_app_icon);
        if (position < 15) {
            appIcon.setImageDrawable(this.getDrawable(context));
        } else {
            // Do actions on a message queue to avoid performance issues on main
            // thread
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    appIcon.setImageDrawable(getDrawable(context));
                }
            });
        }

        return v;
    }

    @Override
    protected void doLaunch(Context context, View v) {

        try {
            Intent intent = Intent.parseUri(shortcutPojo.intentUri, 0);
            context.startActivity(intent);
        } catch (Exception e) {
            // Application was just removed?
            Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public Drawable getDrawable(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        Resources resources;
        try {
            resources = packageManager.getResourcesForApplication(shortcutPojo.packageName);
            final int id = resources.getIdentifier(shortcutPojo.resourceName, null, null);
            return resources.getDrawable(id);
        } catch (NameNotFoundException e) {

        }
        return null;
    }

}
