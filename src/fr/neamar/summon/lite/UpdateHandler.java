package fr.neamar.summon.lite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This class gets called when an application is created or removed on the
 * system
 * 
 * We then recreate our data set.
 * 
 * @author dorvaryn
 * 
 */
public class UpdateHandler extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		SummonApplication.resetDataHandler(ctx);
	}

}
