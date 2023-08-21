package fr.neamar.kiss.utils;

import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

/**
 * Wrapper class for `android.os.UserHandle` that works with all Android versions
 */
public class UserHandle implements Parcelable {
    private static final String TAG = UserHandle.class.getSimpleName();
    private final long serial;
    private final Parcelable handle; // android.os.UserHandle on Android 4.2 and newer

    public UserHandle() {
        this(0, null);
    }

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

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public UserHandle(Context context, android.os.UserHandle userHandle) {
        final UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        assert manager != null;
        serial = manager.getSerialNumberForUser(userHandle);
        handle = userHandle;
    }


    protected UserHandle(Parcel in) {
        serial = in.readLong();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            handle = in.readParcelable(android.os.UserHandle.class.getClassLoader());
        } else {
            handle = null;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(serial);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            dest.writeParcelable(handle, flags);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserHandle> CREATOR = new Creator<UserHandle>() {
        @Override
        public UserHandle createFromParcel(Parcel in) {
            return new UserHandle(in);
        }

        @Override
        public UserHandle[] newArray(int size) {
            return new UserHandle[size];
        }
    };

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
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

    public boolean hasStringUserSuffix(String string, char separator) {
        long serial = 0;

        int index = string.lastIndexOf((int) separator);
        if (index > -1) {
            String serialText = string.substring(index);
            try {
                serial = Long.parseLong(serialText);
            } catch (NumberFormatException e) {
                Log.e(TAG, String.format("Unable to get suffix from string '%s' separated by '%s'", string, separator), e);
            }
        }

        return (serial == this.serial);
    }
}
