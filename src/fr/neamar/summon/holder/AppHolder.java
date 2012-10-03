package fr.neamar.summon.holder;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;

public class AppHolder extends Holder {	
	public String name = "";
	public String nameLowerCased = "";
	public String displayName = ""; //App name with markers to put query text in bold
	public Intent intent;
	public Drawable icon;

	public final void setActivity(ComponentName className, int launchFlags) {
		intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setComponent(className);
		intent.setFlags(launchFlags);
	}
}
