package fr.neamar.kiss.toggles;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.pojo.TogglesPojo;

public class TogglesHandler {
    private final ConnectivityManager connectivityManager;
    private final WifiManager wifiManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final AudioManager audioManager;
    private final ContentResolver contentResolver;

    /**
     * Initialize managers
     *
     * @param context android context
     */
    public TogglesHandler(Context context) {
        this.connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.audioManager = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
        this.contentResolver = context.getContentResolver();
    }

    /**
     * Return the state for the specified pojo
     *
     * @param pojo item to look for
     * @return item state
     */
    public Boolean getState(TogglesPojo pojo) {
        try {
            switch (pojo.settingName) {
                case "wifi":
                    return getWifiState();
                case "data":
                    return getDataState();
                case "bluetooth":
                    return getBluetoothState();
                case "silent":
                    return getSilentState();
                case "torch":
                    return getTorchState();
                case "sync":
                    return getSyncState();
                case "autorotate":
                    return getAutorotationState();
                default:
                    Log.e("wtf", "Unsupported toggle for reading: " + pojo.settingName);
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("log", "Unsupported toggle for device: " + pojo.settingName);
            return null;
        }
    }

    public void setState(TogglesPojo pojo, Boolean state) {
        try {
            switch (pojo.settingName) {
                case "wifi":
                    setWifiState(state);
                    break;
                case "data":
                    setDataState(state);
                    break;
                case "bluetooth":
                    setBluetoothState(state);
                    break;
                case "silent":
                    setSilentState(state);
                    break;
                case "torch":
                    setTorchState(state);
                    break;
                case "sync":
                    setSyncState(state);
                case "autorotate":
                    setAutorotationState(state);
                default:
                    Log.e("wtf", "Unsupported toggle for update: " + pojo.settingName);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("log", "Unsupported toggle for device: " + pojo.settingName);
        }
    }

    private Boolean getWifiState() {
        return wifiManager.isWifiEnabled();
    }

    private void setWifiState(Boolean state) {
        wifiManager.setWifiEnabled(state);
    }

    private Boolean getDataState() {
        Method dataMtd;
        try {
            dataMtd = ConnectivityManager.class.getDeclaredMethod("getMobileDataEnabled");
            dataMtd.setAccessible(true);
            return (Boolean) dataMtd.invoke(connectivityManager);
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void setDataState(Boolean state) {
        Method dataMtd;
        try {
            dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled",
                    boolean.class);
            dataMtd.setAccessible(true);
            dataMtd.invoke(connectivityManager, state);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private Boolean getBluetoothState() {
        return bluetoothAdapter.isEnabled();
    }

    private void setBluetoothState(Boolean state) {
        if (state)
            bluetoothAdapter.enable();
        else
            bluetoothAdapter.disable();
    }

    private Boolean getTorchState() {
        return KissApplication.getCameraHandler().isTorchAvailable() && KissApplication.getCameraHandler().getTorchState();
    }

    private void setTorchState(Boolean state) {
        if(KissApplication.getCameraHandler().isTorchAvailable()) {
            KissApplication.getCameraHandler().setTorchState(state);
        }
    }


    private Boolean getSilentState() {
        int state = audioManager.getRingerMode();
        return state == AudioManager.RINGER_MODE_SILENT || state == AudioManager.RINGER_MODE_VIBRATE;
    }

    private void setSilentState(Boolean state) {
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

    private Boolean getSyncState() {
        return ContentResolver.getMasterSyncAutomatically();
    }

    private void setSyncState(Boolean state) {
        ContentResolver.setMasterSyncAutomatically(state);
    }
    
    private Boolean getAutorotationState() {
        return android.provider.Settings.System.getInt(this.contentResolver,Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
    }

    private void setAutorotationState(Boolean state) {
        android.provider.Settings.System.putInt(this.contentResolver,Settings.System.ACCELEROMETER_ROTATION, (state) ? 1 : 0 );
    }
}
