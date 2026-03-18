package fr.neamar.kiss.utils;

import static android.util.Log.DEBUG;
import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import fr.neamar.kiss.BuildConfig;

public class Log {
    private Log() {
    }

    public static int v(@Nullable String tag, @NonNull String msg) {
        tag = sanitizeTag(tag);
        if (BuildConfig.DEBUG || android.util.Log.isLoggable(tag, VERBOSE)) {
            return android.util.Log.v(tag, msg);
        } else {
            return -1;
        }
    }

    public static int d(@Nullable String tag, @NonNull String msg) {
        tag = sanitizeTag(tag);
        if (BuildConfig.DEBUG || android.util.Log.isLoggable(tag, DEBUG)) {
            return android.util.Log.d(tag, msg);
        } else {
            return -1;
        }
    }

    public static int d(@Nullable String tag, @Nullable String msg, @Nullable Throwable tr) {
        tag = sanitizeTag(tag);
        if (BuildConfig.DEBUG || android.util.Log.isLoggable(tag, VERBOSE)) {
            return android.util.Log.d(tag, msg, tr);
        } else {
            return -1;
        }
    }

    public static int i(@Nullable String tag, @NonNull String msg) {
        tag = sanitizeTag(tag);
        if (BuildConfig.DEBUG || android.util.Log.isLoggable(tag, INFO)) {
            return android.util.Log.i(tag, msg);
        } else {
            return -1;
        }
    }

    public static int w(@Nullable String tag, @NonNull String msg) {
        tag = sanitizeTag(tag);
        return android.util.Log.w(tag, msg);
    }

    public static int w(@Nullable String tag, @Nullable String msg, @Nullable Throwable tr) {
        tag = sanitizeTag(tag);
        return android.util.Log.w(tag, msg, tr);
    }

    public static int e(@Nullable String tag, @NonNull String msg) {
        tag = sanitizeTag(tag);
        return android.util.Log.e(tag, msg);
    }

    public static int e(@Nullable String tag, @Nullable String msg, @Nullable Throwable tr) {
        tag = sanitizeTag(tag);
        return android.util.Log.e(tag, msg, tr);
    }

    private static String sanitizeTag(@Nullable String tag) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N && tag != null && tag.length() > 23) {
            return tag.substring(0, 23);
        }
        return tag;
    }

}
