package fr.neamar.summon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UpdateHandler extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		Log.w("wtf", "received something " + intent.getAction());
		if(intent.getAction().equalsIgnoreCase("android.intent.action.PACKAGE_ADDED") || 
				intent.getAction().equalsIgnoreCase("android.intent.action.PACKAGE_REMOVED")){
			Log.w("wtf", "added or deleted");
			SummonApplication.resetDataHandler(ctx);
		}
	}

}
