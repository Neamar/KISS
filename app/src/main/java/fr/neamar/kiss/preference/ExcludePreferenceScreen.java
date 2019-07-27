package fr.neamar.kiss.preference;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.List;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.PojoComparator;

/**
 * Normally this would be a subclass of PreferenceScreen but PreferenceScreen is final.
 */
public class ExcludePreferenceScreen {

	public static PreferenceScreen getInstance(
			PreferenceActivity preferenceActivity,
			IsExcludedCallback isExcludedCallback,
			OnExcludedListener onExcludedListener,
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
		Arrays.sort(apps, new PojoComparator());

		final PreferenceScreen excludedAppsScreen = preferenceActivity.getPreferenceManager().createPreferenceScreen(preferenceActivity);
		excludedAppsScreen.setTitle(preferenceTitleResId);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			excludedAppsScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Toolbar toolbar = PreferenceScreenHelper.findToolbar(excludedAppsScreen);
					if(toolbar != null) {
						toolbar.setTitle(preferenceScreenTitleResId);
					}
					return false;
				}
			});
		}

		for (AppPojo app : apps) {
			final ComponentName componentName = new ComponentName(app.packageName, app.activityName);

			final Drawable icon = iconsHandler.getDrawableIconForPackage(componentName, app.userHandle);

			final boolean showSummary = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
					|| preferenceActivity.getResources().getConfiguration().screenWidthDp > 420;

			SwitchPreference pref = createExcludeAppSwitch(preferenceActivity, icon, app.getName(),
					app.getComponentName(), isExcludedCallback.isExcluded(app), app, showSummary, onExcludedListener);

			excludedAppsScreen.addPreference(pref);
		}

		return excludedAppsScreen;
	}

	private static SwitchPreference createExcludeAppSwitch(
			@NonNull Context context,
			@NonNull Drawable icon,
			String appName,
			String mainActivityName,
			boolean isExcluded,
			final @NonNull AppPojo app,
			boolean showSummary,
			final OnExcludedListener onExcludedListener
	) {
		final SwitchPreference switchPreference = new SwitchPreference(context);
		switchPreference.setIcon(icon);
		switchPreference.setTitle(appName);
		if (showSummary) {
			switchPreference.setSummary(mainActivityName);
		}
		switchPreference.setChecked(isExcluded);
		switchPreference.setOnPreferenceChangeListener(
				new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						boolean becameExcluded = newValue != null && (boolean) newValue;

						if(becameExcluded) {
							onExcludedListener.onExcluded(app);
						} else {
							onExcludedListener.onIncluded(app);
						}

						return true;
					}
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
