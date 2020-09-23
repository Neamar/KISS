package fr.neamar.kiss;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.nio.charset.Charset;

public class RootHandler {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private Boolean isRootAvailable = null;
    private Boolean isRootActivated = null;

    RootHandler(Context ctx) {
        resetRootHandler(ctx);
    }

    public boolean isRootActivated() {
        return this.isRootActivated;
    }

    void resetRootHandler(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        isRootActivated = prefs.getBoolean("root-mode", false);
    }

    public boolean isRootAvailable() {

        if (isRootAvailable == null) {
            try {
                isRootAvailable = executeRootShell(null);
            } catch (Exception e) {
                isRootAvailable = false;
            }
        }

        return isRootAvailable;
    }

    public boolean hibernateApp(String packageName) {
        try {
            return executeRootShell("am force-stop " + packageName);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean executeRootShell(String command) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            //put command
            if (command != null && !command.trim().equals("")) {
                p.getOutputStream().write((command + "\n").getBytes(UTF_8));
            }
            //exit from su command
            p.getOutputStream().write("exit\n".getBytes(UTF_8));
            p.getOutputStream().flush();
            p.getOutputStream().close();
            int result = p.waitFor();
            if (result != 0)
                throw new Exception("Command execution failed " + result);
            return true;
        } catch (Exception e) {
            Log.e("RootHandler", " " + e);
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        return false;
    }

}
