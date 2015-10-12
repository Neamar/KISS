package fr.neamar.kiss.loader;

import java.util.ArrayList;

import android.content.Context;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.pojo.ShortcutPojo;

public class LoadShortcutPojos extends LoadPojos<ShortcutPojo> {

    public LoadShortcutPojos(Context context) {
        super(context, ShortcutPojo.SCHEME);
    }

    @Override
    protected ArrayList<ShortcutPojo> doInBackground(Void... arg0) {
        return KissApplication.getDataHandler(this.context).getShortcuts(this.context);
    }

}
