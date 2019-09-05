package fr.neamar.kiss.preference;

import android.app.ActionBar;
import android.app.Dialog;
import android.os.Build;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import java.util.ArrayDeque;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public final class PreferenceScreenHelper {
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public static @Nullable Toolbar findToolbar(PreferenceScreen preference) {
		final Dialog dialog = preference.getDialog();
		ViewGroup root = (ViewGroup) dialog.getWindow().getDecorView();

		ArrayDeque<ViewGroup> viewGroups = new ArrayDeque<>();
		viewGroups.push(root);

		while (!viewGroups.isEmpty()) {
			ViewGroup e = viewGroups.removeFirst();

			for (int i = 0; i < e.getChildCount(); i++) {
				View child = e.getChildAt(i);

				if (child instanceof Toolbar) {//Only in LOLIPOP or higher you're going to find a Toolbar
					return (Toolbar) child;
				}

				if (child instanceof ViewGroup) {
					viewGroups.addFirst((ViewGroup) child);
				}
			}
		}

		return null;
	}

}
