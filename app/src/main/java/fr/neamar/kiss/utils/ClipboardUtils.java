package fr.neamar.kiss.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class ClipboardUtils {

	public static void setClipboard(@NonNull Context context, String text) {
		setClipboard(context, "Copied Text", text);
	}

	public static void setClipboard(@NonNull Context context, String label, String text) {
        ClipboardManager clipboard = ContextCompat.getSystemService(context, ClipboardManager.class);
		ClipData clip = ClipData.newPlainText(label, text);
		clipboard.setPrimaryClip(clip);
	}
}
