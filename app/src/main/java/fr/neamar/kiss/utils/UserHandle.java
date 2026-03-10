package fr.neamar.kiss.utils;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Wrapper class for `android.os.UserHandle` that works with all Android versions
 */
public class UserHandle implements Parcelable, Comparable<UserHandle> {
    private static final String TAG = UserHandle.class.getSimpleName();
    private final long serial;
    private final android.os.UserHandle handle; // android.os.UserHandle on Android 4.2 and newer

    public static final UserHandle OWNER = new UserHandle();

    private UserHandle() {
        this(0, null);
    }

    public UserHandle(long serial, android.os.UserHandle user) {
        if (user != null && Process.myUserHandle().equals(user)) {
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

    public UserHandle(Context context, android.os.UserHandle userHandle) {
        if (userHandle != null && Process.myUserHandle().equals(userHandle)) {
            // For easier processing the current user is also stored as `null`, even
            // if there is multi-user support
            this.serial = 0;
            this.handle = null;
        } else {
            final UserManager manager = ContextCompat.getSystemService(context, UserManager.class);
            assert manager != null;
            // Store the given user handle
            this.serial = manager.getSerialNumberForUser(userHandle);
            this.handle = userHandle;
        }
    }

    protected UserHandle(Parcel in) {
        serial = in.readLong();
        handle = in.readParcelable(android.os.UserHandle.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(serial);
        dest.writeParcelable(handle, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserHandle> CREATOR = new Creator<>() {
        @Override
        public UserHandle createFromParcel(Parcel in) {
            return new UserHandle(in);
        }

        @Override
        public UserHandle[] newArray(int size) {
            return new UserHandle[size];
        }
    };

    @NonNull
    public android.os.UserHandle getRealHandle() {
        if (this.handle != null) {
            return this.handle;
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

        int index = string.lastIndexOf(separator);
        if (index > -1) {
            String serialText = string.substring(index);
            try {
                serial = Long.parseLong(serialText);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Unable to get suffix from string '" + string + "' separated by '" + separator + "'", e);
            }
        }

        return (serial == this.serial);
    }


    @Override
    public int compareTo(UserHandle that) {
        if (this == that)
            return 0;

        return Long.compare(this.serial, that.serial);
    }
}
