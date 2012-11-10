package fr.neamar.summon.toggles;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import fr.neamar.summon.holder.ToggleHolder;

public class TogglesHandler {
	protected Context context;
	protected ConnectivityManager connectivityManager;
	protected WifiManager wifiManager;
	protected BluetoothAdapter bluetoothAdapter;
	protected LocationManager locationManager;
	protected AudioManager audioManager;

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
		this.audioManager = ((AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE));
	}

	/**
	 * Return the state for the specified holder
	 * 
	 * @param holder
	 * @return
	 */
	public Boolean getState(ToggleHolder holder) {
		try {
			if (holder.settingName.equals("wifi"))
				return getWifiState();
			else if (holder.settingName.equals("data"))
				return getDataState();
			else if (holder.settingName.equals("bluetooth"))
				return getBluetoothState();
			else if (holder.settingName.equals("gps"))
				return getGpsState();
			else if (holder.settingName.equals("silent"))
				return getSilentState();
			else {
				Log.e("wtf", "Unsupported toggle for reading: "
						+ holder.settingName);
				return false;
			}
		} catch (Exception e) {
			Log.w("log", "Unsupported toggle for device: " + holder.settingName);
			return null;
		}
	}

	public void setState(ToggleHolder holder, Boolean state) {
		try {
			if (holder.settingName.equals("wifi"))
				setWifiState(state);
			else if (holder.settingName.equals("data"))
				setDataState(state);
			else if (holder.settingName.equals("bluetooth"))
				setBluetoothState(state);
			else if (holder.settingName.equals("gps"))
				setGpsState(state);
			else if (holder.settingName.equals("silent"))
				setSilentState(state);
			else {
				Log.e("wtf", "Unsupported toggle for update: "
						+ holder.settingName);
			}
		} catch (Exception e) {
			Log.w("log", "Unsupported toggle for device: " + holder.settingName);
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

	protected Boolean getSilentState() {
		int state = audioManager.getRingerMode();
		if (state == AudioManager.RINGER_MODE_SILENT
				|| state == AudioManager.RINGER_MODE_VIBRATE)
			return true;
		else
			return false;
	}

	protected void setSilentState(Boolean state) {

		if (!state) {
			audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
			audioManager.setStreamVolume(AudioManager.STREAM_RING,
					audioManager.getStreamVolume(AudioManager.STREAM_RING),
					AudioManager.FLAG_PLAY_SOUND);
		} else {
			audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
			audioManager.setStreamVolume(AudioManager.STREAM_RING, 0,
					AudioManager.FLAG_VIBRATE);
		}
	}
}
