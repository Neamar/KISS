package fr.neamar.summon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UpdateHandler extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		Log.w("wtf", "received something " + intent.getAction());
		SummonApplication.resetDataHandler(ctx);
	}

}
