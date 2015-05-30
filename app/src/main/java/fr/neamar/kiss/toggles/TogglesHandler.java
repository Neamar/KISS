package fr.neamar.kiss.toggles;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import fr.neamar.kiss.pojo.TogglePojo;

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
		this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		this.audioManager = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
	}

	/**
	 * Return the state for the specified pojo
	 * 
	 * @param pojo
	 * @return
	 */
	public Boolean getState(TogglePojo pojo) {
		try {
			if (pojo.settingName.equals("wifi"))
				return getWifiState();
			else if (pojo.settingName.equals("data"))
				return getDataState();
			else if (pojo.settingName.equals("bluetooth"))
				return getBluetoothState();
			else if (pojo.settingName.equals("gps"))
				return getGpsState();
			else if (pojo.settingName.equals("silent"))
				return getSilentState();
			else {
				Log.e("wtf", "Unsupported toggle for reading: " + pojo.settingName);
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.w("log", "Unsupported toggle for device: " + pojo.settingName);
			return null;
		}
	}

	public void setState(TogglePojo pojo, Boolean state) {
		try {
			if (pojo.settingName.equals("wifi"))
				setWifiState(state);
			else if (pojo.settingName.equals("data"))
				setDataState(state);
			else if (pojo.settingName.equals("bluetooth"))
				setBluetoothState(state);
			else if (pojo.settingName.equals("gps"))
				setGpsState(state);
			else if (pojo.settingName.equals("silent"))
				setSilentState(state);
			else {
				Log.e("wtf", "Unsupported toggle for update: " + pojo.settingName);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.w("log", "Unsupported toggle for device: " + pojo.settingName);
		}
	}

	protected Boolean getWifiState() {
		return wifiManager.isWifiEnabled();
	}

	protected void setWifiState(Boolean state) {
		wifiManager.setWifiEnabled(state);
	}

	protected Boolean getDataState() {
		Method dataMtd = null;
		try {
			dataMtd = ConnectivityManager.class.getDeclaredMethod("getMobileDataEnabled");
			dataMtd.setAccessible(true);
			return (Boolean) dataMtd.invoke(connectivityManager);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	protected void setDataState(Boolean state) {
		Method dataMtd = null;
		try {
			dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled",
					boolean.class);
			dataMtd.setAccessible(true);
			dataMtd.invoke(connectivityManager, state);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		if (state == AudioManager.RINGER_MODE_SILENT || state == AudioManager.RINGER_MODE_VIBRATE)
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
			audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_VIBRATE);
		}
	}
}
