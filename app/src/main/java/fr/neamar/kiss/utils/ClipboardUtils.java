package fr.neamar.kiss.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.annotation.NonNull;

public class ClipboardUtils {

	public static void setClipboard(@NonNull Context context, String text) {
		setClipboard(context, "Copied Text", text);
	}

	public static void setClipboard(@NonNull Context context, String label, String text) {
		ClipboardManager clipboard;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			clipboard = context.getSystemService(ClipboardManager.class);
		} else {
			clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		}

		ClipData clip = ClipData.newPlainText(label, text);
		clipboard.setPrimaryClip(clip);
	}
}
