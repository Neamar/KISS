package fr.neamar.summon.toggles;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import fr.neamar.summon.holder.ToggleHolder;

public class TogglesHandler {
	Context context;
	ConnectivityManager connectivityManager;
	WifiManager wifiManager;
	BluetoothAdapter bluetoothAdapter;
	LocationManager locationManager;

	/**
	 * Initialize managers
	 * 
	 * @param context
	 */
	public TogglesHandler(Context context) {
		this.context = context;
		this.connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		this.wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		this.locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
	}

	/**
	 * Return the state for the specified holder
	 * 
	 * @param holder
	 * @return
	 */
	public Boolean getState(ToggleHolder holder) {
		if (holder.settingName.equals("wifi"))
			return getWifiState();
		else if (holder.settingName.equals("data"))
			return getDataState();
		else if (holder.settingName.equals("bluetooth"))
			return getBluetoothState();
		else if (holder.settingName.equals("gps"))
			return getGpsState();
		else {
			Log.e("wtf", "Unsupported toggle for reading: "
					+ holder.settingName);
			return false;
		}
	}

	public void setState(ToggleHolder holder, Boolean state) {
		if (holder.settingName.equals("wifi"))
			setWifiState(state);
		else if (holder.settingName.equals("data"))
			setDataState(state);
		else if (holder.settingName.equals("bluetooth"))
			setBluetoothState(state);
		else if (holder.settingName.equals("gps"))
			setGpsState(state);
		else {
			Log.e("wtf", "Unsupported toggle for update: " + holder.settingName);
		}
	}

	protected Boolean getWifiState() {
		return wifiManager.isWifiEnabled();
	}

	protected void setWifiState(Boolean state) {
		wifiManager.setWifiEnabled(state);
	}

	protected Boolean getDataState() {
		NetworkInfo ni = connectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		return ni.isAvailable();
	}

	protected void setDataState(Boolean state) {
		// http://stackoverflow.com/questions/3644144/how-to-disable-mobile-data-on-android
		Toast.makeText(context, "Data toggle not working yet. Soon ;)",
				Toast.LENGTH_SHORT).show();
	}

	protected Boolean getBluetoothState() {
		return bluetoothAdapter.isEnabled();
	}

	protected void setBluetoothState(Boolean state) {
		if (state)
			bluetoothAdapter.enable();
		else
			bluetoothAdapter.disable();
	}

	protected Boolean getGpsState() {
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	protected void setGpsState(Boolean state) {
		Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(myIntent);
	}
}
