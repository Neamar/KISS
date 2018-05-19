package fr.neamar.kiss.loader;

import android.content.Context;

import java.util.ArrayList;

import fr.neamar.kiss.pojo.CommandsPojo;

public class LoadCommandsPojos extends LoadPojos<CommandsPojo> {

    public LoadCommandsPojos(Context context) {
        super(context, "command://");
    }

    @Override
    protected ArrayList<CommandsPojo> doInBackground(Void... params) {
        ArrayList<CommandsPojo> settings = new ArrayList<>();

        if(context.get() == null) {
            return settings;
        }
        settings.add(new CommandsPojo("kiss_history_clear", "kiss history clear"));
        settings.add(new CommandsPojo("kiss_ui_theme_light", "kiss theme light"));

        return settings;
    }
}
