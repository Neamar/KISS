package fr.neamar.kiss.dataprovider.simpleprovider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.DrawableRes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.neamar.kiss.R;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.SettingsPojo;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.utils.FuzzyScore;

public class SettingsProvider extends SimpleProvider {
    private final static String SCHEME = "setting://";
    private String settingName;
    private List<SettingsPojo> pojos;

    public SettingsProvider(Context context) {
        pojos = new ArrayList<>();

        PackageManager pm = context.getPackageManager();
        pojos.add(createPojo(context.getString(R.string.settings_airplane),
                Settings.ACTION_AIRPLANE_MODE_SETTINGS, R.drawable.setting_airplane));
        pojos.add(createPojo(context.getString(R.string.settings_device_info),
                Settings.ACTION_DEVICE_INFO_SETTINGS, R.drawable.setting_info));
        pojos.add(createPojo(context.getString(R.string.settings_applications),
                Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, R.drawable.setting_apps));
        pojos.add(createPojo(context.getString(R.string.settings_connectivity),
                Settings.ACTION_WIRELESS_SETTINGS, R.drawable.setting_wifi));
        pojos.add(createPojo(context.getString(R.string.settings_storage),
                Settings.ACTION_INTERNAL_STORAGE_SETTINGS, R.drawable.setting_storage));
        pojos.add(createPojo(context.getString(R.string.settings_accessibility),
                Settings.ACTION_ACCESSIBILITY_SETTINGS, R.drawable.setting_accessibility));
        pojos.add(createPojo(context.getString(R.string.settings_battery),
                Intent.ACTION_POWER_USAGE_SUMMARY, R.drawable.setting_battery));
        pojos.add(createPojo(context.getString(R.string.settings_tethering), "com.android.settings",
                "com.android.settings.TetherSettings", R.drawable.setting_tethering));
        pojos.add(createPojo(context.getString(R.string.settings_sound),
                Settings.ACTION_SOUND_SETTINGS, R.drawable.setting_dev));
        pojos.add(createPojo(context.getString(R.string.settings_display),
                Settings.ACTION_DISPLAY_SETTINGS, R.drawable.setting_dev));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            pojos.add(createPojo(context.getString(R.string.settings_nfc),
                    Settings.ACTION_NFC_SETTINGS, R.drawable.setting_nfc));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            pojos.add(createPojo(context.getString(R.string.settings_dev),
                    Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, R.drawable.setting_dev));
        }

        settingName = context.getString(R.string.settings_prefix).toLowerCase(Locale.ROOT);
    }

    private void assignName(SettingsPojo pojo, String name) {
        pojo.setName(name, true);
    }

    private String getId(String settingName) {
        return SCHEME + settingName.toLowerCase(Locale.ENGLISH);
    }

    private SettingsPojo createPojo(String name, String packageName, String settingName,
                                    @DrawableRes int resId) {
        SettingsPojo pojo = new SettingsPojo(getId(settingName), settingName, packageName, resId);
        assignName(pojo, name);
        return pojo;
    }

    private SettingsPojo createPojo(String name, String settingName, @DrawableRes int resId) {
        SettingsPojo pojo = new SettingsPojo(getId(settingName), settingName, resId);
        assignName(pojo, name);
        return pojo;
    }

    @Override
    public void reload() {
        super.reload();

    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo;
        boolean match;

        for (SettingsPojo pojo : pojos) {
            matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
            match = matchInfo.match;
            pojo.relevance = matchInfo.score;

            if (!match) {
                // Match localized setting name
                matchInfo = fuzzyScore.match(settingName);
                match = matchInfo.match;
                pojo.relevance = matchInfo.score;
            }

            if (match && !searcher.addResult(pojo)) {
                return;
            }
        }
    }
}
