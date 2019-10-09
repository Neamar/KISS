package fr.neamar.kiss.pojo;

import android.graphics.drawable.Drawable;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class SettingsPojo extends Pojo {
    public final String settingName;
    public final String packageName;

    public SettingsPojo(String id, String settingName, Future<Drawable> icon) {
    	super(id, icon);

        this.settingName = settingName;
        this.packageName = "";
    }

    public SettingsPojo(String id, String settingName, String packageName, Future<Drawable> icon) {
	    super(id, icon);

        this.settingName = settingName;
        this.packageName = packageName;
    }
}
