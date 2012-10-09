package fr.neamar.summon.toggles;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import fr.neamar.summon.holder.ToggleHolder;

public class TogglesHandler {
	Context context;
	ConnectivityManager connectivityManager;
	BluetoothAdapter bluetoothAdapter;
	LocationManager locationManager;
	
	public TogglesHandler(Context context) {
		this.context = context;
		this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE );
	}
	
	public Boolean getState(ToggleHolder holder)
	{
		if(holder.settingName.equals("wifi"))
			return getWifiState();
		else if(holder.settingName.equals("data"))
			return getDataState();
		else if(holder.settingName.equals("bluetooth"))
			return getBluetoothState();
		else if(holder.settingName.equals("gps"))
			return getGpsState();
		else
		{
			Log.e("wtf", "Unsupported toggle: " + holder.settingName);
			return false;
		}
	}

	protected Boolean getWifiState() {
		
		NetworkInfo ni = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return ni.isConnectedOrConnecting();
	}
	
	protected Boolean getDataState() {
		NetworkInfo ni = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		return ni.isAvailable();
	}
	
	protected Boolean getBluetoothState() {
		return bluetoothAdapter.isEnabled();
	}
	
	protected Boolean getGpsState() {
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}
}
