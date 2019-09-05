package fr.neamar.kiss.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Process;


/**
 * Wrapper class for `android.os.UserHandle` that works with all Android versions
 */
public class UserHandle {
    private final long serial;
    private final Object handle; // android.os.UserHandle on Android 4.2 and newer

    public UserHandle() {
        this(0, null);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public UserHandle(long serial, android.os.UserHandle user) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // OS does not provide any APIs for multi-user support
            this.serial = 0;
            this.handle = null;
        } else if (user != null && Process.myUserHandle().equals(user)) {
            // For easier processing the current user is also stored as `null`, even
            // if there is multi-user support
            this.serial = 0;
            this.handle = null;
        } else {
            // Store the given user handle
            this.serial = serial;
            this.handle = user;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public android.os.UserHandle getRealHandle() {
        if (this.handle != null) {
            return (android.os.UserHandle) this.handle;
        } else {
            return Process.myUserHandle();
        }
    }


    public boolean isCurrentUser() {
        return (this.handle == null);
    }


    public String addUserSuffixToString(String base, char separator) {
        if (this.handle == null) {
            return base;
        } else {
            return base + separator + this.serial;
        }
    }

    @SuppressWarnings("CatchAndPrintStackTrace")
    public boolean hasStringUserSuffix(String string, char separator) {
        long serial = 0;

        int index = string.lastIndexOf((int) separator);
        if (index > -1) {
            String serialText = string.substring(index);
            try {
                serial = Long.parseLong(serialText);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return (serial == this.serial);
    }
}
