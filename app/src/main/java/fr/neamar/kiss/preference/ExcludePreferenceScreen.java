package fr.neamar.kiss.preference;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.NameComparator;
import fr.neamar.kiss.utils.Utilities;

/**
 * Normally this would be a subclass of PreferenceScreen but PreferenceScreen is final.
 */
public class ExcludePreferenceScreen {

	public static PreferenceScreen getInstance(
			@NonNull PreferenceActivity preferenceActivity,
			@NonNull IsExcludedCallback isExcludedCallback,
			@NonNull OnExcludedListener onExcludedListener,
			@StringRes int preferenceTitleResId,
			final @StringRes int preferenceScreenTitleResId
	) {
		List<AppPojo> appList = KissApplication.getApplication(preferenceActivity).getDataHandler().getApplications();
		IconsHandler iconsHandler = KissApplication.getApplication(preferenceActivity).getIconsHandler();

		AppPojo[] apps;
		if(appList != null) {
			apps = appList.toArray(new AppPojo[0]);
		}
		else {
			apps = new AppPojo[0];
		}
		Arrays.sort(apps, new NameComparator());

		final PreferenceScreen excludedAppsScreen = preferenceActivity.getPreferenceManager().createPreferenceScreen(preferenceActivity);
		excludedAppsScreen.setTitle(preferenceTitleResId);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			excludedAppsScreen.setOnPreferenceClickListener(preference -> {
				Toolbar toolbar = PreferenceScreenHelper.findToolbar(excludedAppsScreen);
				if (toolbar != null) {
					toolbar.setTitle(preferenceScreenTitleResId);
				}
				return false;
			});
		}

		final boolean showSummary = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
				|| preferenceActivity.getResources().getConfiguration().screenWidthDp > 420;

		for (AppPojo app : apps) {
			SwitchPreference pref = createExcludeAppSwitch(preferenceActivity, iconsHandler, isExcludedCallback, app, showSummary, onExcludedListener);

			excludedAppsScreen.addPreference(pref);
		}

		return excludedAppsScreen;
	}

	private static SwitchPreference createExcludeAppSwitch(
			@NonNull Context context,
			@NonNull IconsHandler iconsHandler,
			@NonNull IsExcludedCallback isExcludedCallback,
			final @NonNull AppPojo app,
			boolean showSummary,
			@NonNull final OnExcludedListener onExcludedListener
	) {
		final SwitchPreference switchPreference = new SwitchPreference(context);

		AtomicReference<Drawable> icon = new AtomicReference<>(null);
		switchPreference.setIcon(R.drawable.ic_launcher_white);
		Utilities.runAsync((task) -> {
			final ComponentName componentName = new ComponentName(app.packageName, app.activityName);
			icon.set(iconsHandler.getDrawableIconForPackage(componentName, app.userHandle));
		}, (task) -> {
			if (!task.isCancelled()) {
				switchPreference.setIcon(icon.get());
			}
		});

		switchPreference.setTitle(app.getName());
		if (showSummary) {
			switchPreference.setSummary(app.getComponentName());
		}
		switchPreference.setChecked(isExcludedCallback.isExcluded(app));
		switchPreference.setOnPreferenceChangeListener(
				(preference, newValue) -> {
					boolean becameExcluded = newValue != null && (boolean) newValue;

					if (becameExcluded) {
						onExcludedListener.onExcluded(app);
					} else {
						onExcludedListener.onIncluded(app);
					}

					return true;
				}
		);
		return switchPreference;
	}

	/**
	 * Use {@link #getInstance}
	 */
	private ExcludePreferenceScreen() {}

	public interface IsExcludedCallback {
		boolean isExcluded(final @NonNull AppPojo app);
	}

	public interface OnExcludedListener {
		void onExcluded(final @NonNull AppPojo app);
		void onIncluded(final @NonNull AppPojo app);

	}
}
