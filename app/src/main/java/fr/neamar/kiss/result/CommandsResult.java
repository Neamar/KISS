package fr.neamar.kiss.result;

import android.content.Context;
import android.graphics.PorterDuff;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.CommandsPojo;
import fr.neamar.kiss.ui.ListPopup;

public class CommandsResult extends Result {
    private final CommandsPojo commandPojo;

    CommandsResult(CommandsPojo commandPojo) {
        super(commandPojo);
        this.commandPojo = commandPojo;
    }

    @Override
    public View display(Context context, int position, View v) {
        if (v == null)
            v = inflateFromId(context, R.layout.item_command);

        TextView appName = v.findViewById(R.id.item_command_text);
        ImageView image = v.findViewById(R.id.item_command_icon);

        String text = this.pojo.getName();
        int len = this.pojo.getName().length();

        appName.setText(enrichText(text, Collections.singletonList(new Pair<Integer, Integer>(0, len)), context));

        image.setColorFilter(getThemeFillColor(context), PorterDuff.Mode.SRC_IN);
        return v;
    }

    @Override
    public void doLaunch(Context context, View v) {
        switch (commandPojo.id) {
            case "kiss_history_clear" : KissApplication.getApplication(context).getDataHandler().clearHistory(); break;
            case "kiss_ui_theme_next" :
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString("theme", "light").apply();
                context.setTheme(R.style.AppThemeLight);
                ((MainActivity)context).recreate();
                break;

        }
//        String urlWithQuery = commandPojo.url.replace("{q}", commandPojo.query);
//        Uri uri = Uri.parse(urlWithQuery);
//        Intent search = new Intent(Intent.ACTION_VIEW, uri);
//        search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        try {
//            context.startActivity(search);
//        } catch (android.content.ActivityNotFoundException e) {
//            Log.w("CommandsResult", "Unable to run search for url: " + commandPojo.url);
//        }
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, final RecordAdapter parent, View parentView) {
        adapter.add(new ListPopup.Item(context, R.string.menu_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_add));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_remove));

        return inflatePopupMenu(adapter, context);
    }

    @Override
    protected Boolean popupMenuClickHandler(Context context, RecordAdapter parent, int stringId) {
        //switch (stringId) {

        //}

        return super.popupMenuClickHandler(context, parent, stringId);
    }
}
