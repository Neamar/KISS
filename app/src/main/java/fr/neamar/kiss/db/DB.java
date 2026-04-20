package fr.neamar.kiss.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.neamar.kiss.utils.Log;
import fr.neamar.kiss.utils.ShortcutUtil;
import fr.neamar.kiss.utils.UserHandle;

class DB extends SQLiteOpenHelper {

    private final static String DB_NAME = "kiss.s3db";
    private final static int DB_VERSION = 11;
    private static final String TAG = DB.class.getSimpleName();

    private final Context mContext;

    DB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        // `query` is a keyword so we escape it. See: https://www.sqlite.org/lang_keywords.html
        database.execSQL("CREATE TABLE history ( _id INTEGER PRIMARY KEY AUTOINCREMENT, \"query\" TEXT, record TEXT NOT NULL)");
        database.execSQL("CREATE TABLE shortcuts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, package TEXT,"
                + "icon TEXT, intent_uri TEXT NOT NULL, icon_blob BLOB)");
        createTags(database);
        addTimeStamps(database);
        addAppsTable(database);
        addCustomComponentsTable(database);
    }

    private void createTags(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE tags ( _id INTEGER PRIMARY KEY AUTOINCREMENT, tag TEXT NOT NULL, record TEXT NOT NULL)");
        database.execSQL("CREATE INDEX idx_tags_record ON tags(record);");
    }

    private void addTimeStamps(SQLiteDatabase database) {
        database.execSQL("ALTER TABLE history ADD COLUMN timeStamp INTEGER DEFAULT 0  NOT NULL");
    }

    private void addAppsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS custom_apps ( _id INTEGER PRIMARY KEY AUTOINCREMENT, custom_flags INTEGER DEFAULT 0, component_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL DEFAULT '' )");
        db.execSQL("CREATE INDEX IF NOT EXISTS index_component ON custom_apps(component_name);");
    }

    private void addCustomComponentsTable(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE custom_components ( _id INTEGER PRIMARY KEY AUTOINCREMENT, id TEXT NOT NULL UNIQUE, package TEXT NOT NULL, class TEXT NOT NULL)");
        database.execSQL("CREATE INDEX idx_custom_components_id ON custom_components(id);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w("onUpgrade", "Updating database from version " + oldVersion + " to version " + newVersion);
        // See
        // http://www.drdobbs.com/database/using-sqlite-on-android/232900584
        if (oldVersion < newVersion) {
            switch (oldVersion) {
                case 1:
                case 2:
                case 3:
                    database.execSQL("CREATE TABLE shortcuts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, package TEXT,"
                            + "icon TEXT, intent_uri TEXT NOT NULL, icon_blob BLOB)");
                    // fall through
                case 4:
                    createTags(database);
                    // fall through
                case 5:
                    addTimeStamps(database);
                    // fall through
                case 6:
                case 7:
                    addAppsTable(database);
                    // fall through
                case 8:
                    convertShortcutIds(database);
                    // fall through
                case 9:
                    convertTheme();
                    // fall through
                case 10:
                    addCustomComponentsTable(database);
                    // fall through
                default:
                    break;
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w("onDowngrade", "Updating database from version " + oldVersion + " to version " + newVersion);

        if (newVersion < oldVersion) {
            switch (newVersion) {
                case 10:
                    database.execSQL("DROP INDEX idx_custom_components_id");
                    database.execSQL("DROP TABLE custom_components");
                    break;
                case 9:
                case 8:
                    throw new UnsupportedOperationException("Can't downgrade app below DB level " + (newVersion + 1));
                case 7:
                case 6:
                    database.execSQL("DROP INDEX index_component");
                    database.execSQL("DROP TABLE custom_apps");
                    break;
                case 5:
                    throw new UnsupportedOperationException("Can't downgrade app below DB level " + (newVersion + 1));
                default:
                    break;
            }
        }
    }

    private void convertShortcutIds(SQLiteDatabase database) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            List<ShortcutInfo> shortcutInfos = ShortcutUtil.getAllShortcuts(mContext);
            List<ConvertShortcutInfo> shortcuts = new ArrayList<>();
            shortcutInfos.forEach(shortcutInfo -> {
                ShortcutRecord shortcutRecordWithName = ShortcutUtil.createShortcutRecord(mContext, shortcutInfo, true);
                if (shortcutRecordWithName != null) {
                    shortcuts.add(new ConvertShortcutInfo(UserHandle.OWNER, shortcutRecordWithName));
                }
                ShortcutRecord shortcutRecordWithoutName = ShortcutUtil.createShortcutRecord(mContext, shortcutInfo, false);
                if (shortcutRecordWithoutName != null) {
                    shortcuts.add(new ConvertShortcutInfo(UserHandle.OWNER, shortcutRecordWithoutName));
                }
            });
            // convert all tags
            shortcuts.forEach((shortcut) -> {
                ContentValues values = new ContentValues(1);
                values.put("record", shortcut.newId);
                int updated = database.update("tags", values, "record=?", new String[]{shortcut.oldId});
                if (updated > 0) {
                    Log.v(TAG, "Updated tags: " + shortcut.oldId + " > " + shortcut.newId);
                }
            });
            // convert history
            shortcuts.forEach((shortcut) -> {
                ContentValues values = new ContentValues(1);
                values.put("record", shortcut.newId);
                int updated = database.update("history", values, "record=?", new String[]{shortcut.oldId});
                if (updated > 0) {
                    Log.v(TAG, "Updated history: " + shortcut.oldId + " > " + shortcut.newId);
                }
            });
            // convert favorites
            String[] favoriteAppsList = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getString("favorite-apps-list", "").split(";");
            List<String> favorites = new ArrayList<>();
            Collections.addAll(favorites, favoriteAppsList);
            shortcuts.forEach((shortcut) -> {
                int index = favorites.indexOf(shortcut.oldId);
                if (index >= 0) {
                    favorites.set(index, shortcut.newId);
                    Log.v(TAG, "Updated favorite: " + shortcut.oldId + " > " + shortcut.newId);
                }
            });

            SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                    .putString("favorite-apps-list", TextUtils.join(";", favorites));
            commit(prefsEditor, "favorite-apps-list");
        }
    }

    private void convertTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String oldTheme = prefs.getString("theme", "transparent");
        SharedPreferences.Editor prefsEditor = prefs.edit();
        switch (oldTheme) {
            case "dark":
                prefsEditor.putString("theme", "opaque");
                prefsEditor.putString("night-mode", "yes");
                break;
            case "semi-transparent-dark":
                prefsEditor.putString("theme", "semi-transparent");
                prefsEditor.putString("night-mode", "yes");
                break;
            case "transparent-dark":
                prefsEditor.putString("theme", "transparent");
                prefsEditor.putString("night-mode", "yes");
                break;
            case "amoled-dark":
                prefsEditor.putString("theme", "amoled-dark");
                prefsEditor.putString("night-mode", "yes");
                break;
            case "light":
                prefsEditor.putString("theme", "opaque");
                prefsEditor.putString("night-mode", "no");
                break;
            case "semi-transparent":
                prefsEditor.putString("theme", "semi-transparent");
                prefsEditor.putString("night-mode", "no");
                break;
            case "transparent":
                prefsEditor.putString("theme", "transparent");
                prefsEditor.putString("night-mode", "no");
                break;
        }
        commit(prefsEditor, "theme");
    }

    private void commit(SharedPreferences.Editor prefsEditor, String key) {
        boolean commited = prefsEditor.commit();
        if (!commited) {
            throw new UnsupportedOperationException("Can't upgrade preference: " + key);
        }
    }

    private static class ConvertShortcutInfo {

        final String oldId;
        final String newId;

        ConvertShortcutInfo(UserHandle userHandle, @NonNull ShortcutRecord shortcutRecord) {
            this.oldId = ShortcutUtil.generateShortcutId(null, shortcutRecord);
            this.newId = ShortcutUtil.generateShortcutId(userHandle, shortcutRecord);
        }
    }
}
