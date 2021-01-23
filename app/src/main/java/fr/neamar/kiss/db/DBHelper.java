package fr.neamar.kiss.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.pojo.ShortcutPojo;

public class DBHelper {
    private static final String TAG = DBHelper.class.getSimpleName();
    private static SQLiteDatabase database = null;

    private DBHelper() {
    }

    private static SQLiteDatabase getDatabase(Context context) {
        if (database == null) {
            database = new DB(context).getReadableDatabase();
        }
        return database;
    }

    private static ArrayList<ValuedHistoryRecord> readCursor(Cursor cursor) {
        cursor.moveToFirst();

        ArrayList<ValuedHistoryRecord> records = new ArrayList<>(cursor.getCount());
        while (!cursor.isAfterLast()) {
            ValuedHistoryRecord entry = new ValuedHistoryRecord();

            entry.record = cursor.getString(0);
            entry.value = cursor.getInt(1);

            records.add(entry);
            cursor.moveToNext();
        }
        cursor.close();

        return records;
    }

    /**
     * Insert new item into history
     *
     * @param context android context
     * @param query   query to insert
     * @param record  record to insert
     */
    public static void insertHistory(Context context, String query, String record) {
        SQLiteDatabase db = getDatabase(context);
        ContentValues values = new ContentValues();
        values.put("query", query);
        values.put("record", record);
        values.put("timeStamp", System.currentTimeMillis());
        db.insert("history", null, values);

        if (Math.random() <= 0.005) {
            // Roughly every 200 inserts, clean up the history of items older than 3 months
            long twoMonthsAgo = 7776000000L; // 1000 * 60 * 60 * 24 * 30 * 3;
            db.delete("history", "timeStamp < ?", new String[]{Long.toString(System.currentTimeMillis() - twoMonthsAgo)});
            // And vacuum the DB for speed
            db.execSQL("VACUUM");
        }
    }

    public static void removeFromHistory(Context context, String record) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("history", "record = ?", new String[]{record});
    }

    public static void clearHistory(Context context) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("history", "", null);
    }

    private static Cursor getHistoryByFrecency(SQLiteDatabase db, int limit) {
        // Since smart history sql uses a group by we don't use the whole history but a limit of recent apps
        int historyWindowSize = limit * 30;

        // order history based on frequency * recency
        // frequency = #launches_for_app / #all_launches
        // recency = 1 / position_of_app_in_normal_history
        String sql = "SELECT record, count(*) FROM " +
                " (" +
                "   SELECT * FROM history ORDER BY _id DESC " +
                "   LIMIT " + historyWindowSize + "" +
                " ) small_history " +
                " GROUP BY record " +
                " ORDER BY " +
                "   count(*) * 1.0 / (select count(*) from history LIMIT " + historyWindowSize + ") / ((SELECT _id FROM history ORDER BY _id DESC LIMIT 1) - max(_id) + 0.001) " +
                " DESC " +
                " LIMIT " + limit;
        return db.rawQuery(sql, null);
    }

    private static Cursor getHistoryByFrequency(SQLiteDatabase db, int limit) {
        // order history based on frequency
        String sql = "SELECT record, count(*) FROM history" +
                " GROUP BY record " +
                " ORDER BY count(*) DESC " +
                " LIMIT " + limit;
        return db.rawQuery(sql, null);
    }

    private static Cursor getHistoryByRecency(SQLiteDatabase db, int limit) {
        return db.query(true, "history", new String[]{"record", "1"}, null, null,
                null, null, "_id DESC", Integer.toString(limit));
    }

    /**
     * Get the most used history items adaptively based on a set period of time
     *
     * @param db    The SQL db
     * @param hours How many hours back we want to test frequency against
     * @param limit Maximum result size
     * @return Cursor
     */
    private static Cursor getHistoryByAdaptive(SQLiteDatabase db, int hours, int limit) {
        // order history based on frequency
        String sql = "SELECT record, count(*) FROM history " +
                "WHERE timeStamp >= 0 " +
                "AND timeStamp >" + (System.currentTimeMillis() - (hours * 3600000)) +
                " GROUP BY record " +
                " ORDER BY count(*) DESC " +
                " LIMIT " + limit;
        return db.rawQuery(sql, null);
    }

    /**
     * Retrieve previous query history
     *
     * @param context android context
     * @param limit   max number of items to retrieve
     * @return records with number of use
     */
    public static ArrayList<ValuedHistoryRecord> getHistory(Context context, int limit, String historyMode) {
        ArrayList<ValuedHistoryRecord> records;

        SQLiteDatabase db = getDatabase(context);

        Cursor cursor;
        switch (historyMode) {
            case "frecency":
                cursor = getHistoryByFrecency(db, limit);
                break;
            case "frequency":
                cursor = getHistoryByFrequency(db, limit);
                break;
            case "adaptive":
                cursor = getHistoryByAdaptive(db, 36, limit);
                break;
            default:
                cursor = getHistoryByRecency(db, limit);
                break;
        }

        records = readCursor(cursor);
        cursor.close();

        // sort history entries alphabetically
        if (historyMode.equals("alphabetically")) {
            DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();

            for (ValuedHistoryRecord entry : records) {
                entry.name = dataHandler.getItemName(entry.record);
            }

            Collections.sort(records, (a, b) -> a.name.compareToIgnoreCase(b.name));
        }

        return records;
    }


    /**
     * Retrieve history size
     *
     * @param context android context
     * @return total number of use for the application
     */
    public static int getHistoryLength(Context context) {
        SQLiteDatabase db = getDatabase(context);

        // Cursor query (boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy, String limit)
        Cursor cursor = db.query(false, "history", new String[]{"COUNT(*)"}, null, null,
                null, null, null, null);

        cursor.moveToFirst();
        int historyLength = cursor.getInt(0);
        cursor.close();
        return historyLength;
    }

    /**
     * Retrieve previously selected items for the query
     *
     * @param context android context
     * @param query   query to run
     * @return records with number of use
     */
    public static ArrayList<ValuedHistoryRecord> getPreviousResultsForQuery(Context context,
                                                                            String query) {
        ArrayList<ValuedHistoryRecord> records;
        SQLiteDatabase db = getDatabase(context);

        // Cursor query (String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String
        // orderBy)
        Cursor cursor = db.query("history", new String[]{"record", "COUNT(*) AS count"},
                "query LIKE ?", new String[]{query + "%"}, "record", null, "COUNT(*) DESC", "10");
        records = readCursor(cursor);
        cursor.close();
        return records;
    }

    public static boolean insertShortcut(Context context, ShortcutRecord shortcut) {
        SQLiteDatabase db = getDatabase(context);
        // Do not add duplicate shortcuts
        Cursor cursor = db.query("shortcuts", new String[]{"package", "intent_uri"},
                "package = ? AND intent_uri = ?", new String[]{shortcut.packageName, shortcut.intentUri}, null, null, null, null);
        if (cursor.moveToFirst()) {
            return false;
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put("name", shortcut.name);
        values.put("package", shortcut.packageName);
        values.put("icon", (String) null); // Legacy field (for shortcuts before Oreo), not used anymore
        values.put("icon_blob", (String) null); // Another legacy field (icon is dynamically retrieved)
        values.put("intent_uri", shortcut.intentUri);

        db.insert("shortcuts", null, values);
        return true;
    }

    public static void removeShortcut(Context context, ShortcutPojo shortcut) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("shortcuts", "package = ? AND intent_uri = ?", new String[]{shortcut.packageName, shortcut.intentUri});
    }

    public static void addCustomAppName(Context context, String componentName, String newName) {
        SQLiteDatabase db = getDatabase(context);

        long id;
        String sql = "INSERT OR ABORT INTO custom_apps(\"name\", \"component_name\", \"custom_flags\") VALUES (?,?,?)";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindString(1, newName);
            statement.bindString(2, componentName);
            statement.bindLong(3, AppRecord.FLAG_CUSTOM_NAME);
            id = statement.executeInsert();
            statement.close();
        } catch (Exception ignored) {
            id = -1;
        }
        if (id == -1) {
            sql = "UPDATE custom_apps SET name=?,custom_flags=custom_flags|? WHERE component_name=?";
            try {
                SQLiteStatement statement = db.compileStatement(sql);
                statement.bindString(1, newName);
                statement.bindLong(2, AppRecord.FLAG_CUSTOM_NAME);
                statement.bindString(3, componentName);
                int count = statement.executeUpdateDelete();
                if (count != 1) {
                    Log.e(TAG, "Update name count = " + count);
                }
                statement.close();
            } catch (Exception e) {
                Log.e(TAG, "Insert or Update custom app name", e);
            }
        }
    }


    @Nullable
    private static AppRecord getAppRecord(SQLiteDatabase db, String componentName) {
        String[] selArgs = new String[]{componentName};
        String[] columns = new String[]{"_id", "name", "component_name", "custom_flags"};
        try (Cursor cursor = db.query("custom_apps", columns,
                "component_name=?", selArgs, null, null, null)) {
            if (cursor.moveToNext()) {
                AppRecord entry = new AppRecord();

                entry.dbId = cursor.getLong(0);
                entry.name = cursor.getString(1);
                entry.componentName = cursor.getString(2);
                entry.flags = cursor.getInt(3);

                return entry;
            }
        }
        return null;
    }

    public static long addCustomAppIcon(Context context, String componentName) {
        SQLiteDatabase db = getDatabase(context);

        long id;
        String sql = "INSERT OR ABORT INTO custom_apps(\"component_name\", \"custom_flags\") VALUES (?,?)";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindString(1, componentName);
            statement.bindLong(2, AppRecord.FLAG_CUSTOM_ICON);
            id = statement.executeInsert();
            statement.close();
        } catch (Exception ignored) {
            id = -1;
        }
        if (id == -1) {
            sql = "UPDATE custom_apps SET custom_flags=custom_flags|? WHERE component_name=?";
            try {
                SQLiteStatement statement = db.compileStatement(sql);
                statement.bindLong(1, AppRecord.FLAG_CUSTOM_ICON);
                statement.bindString(2, componentName);
                int count = statement.executeUpdateDelete();
                if (count != 1) {
                    Log.e(TAG, "Update `custom_flags` returned count=" + count);
                }
                statement.close();
            } catch (Exception e) {
                Log.e(TAG, "Update custom app icon", e);
            }
            AppRecord appRecord = getAppRecord(db, componentName);
            id = appRecord != null ? appRecord.dbId : 0;
        }
        return id;
    }

    public static long removeCustomAppIcon(Context context, String componentName) {
        SQLiteDatabase db = getDatabase(context);
        AppRecord app = getAppRecord(db, componentName);
        if (app == null)
            return 0;

        if (app.hasCustomName()) {
            // app has a custom name, just remove the custom icon
            String sql = "UPDATE custom_apps SET custom_flags=custom_flags&~? WHERE component_name=?";
            try {
                SQLiteStatement statement = db.compileStatement(sql);
                statement.bindLong(1, AppRecord.FLAG_CUSTOM_ICON);
                statement.bindString(2, componentName);
                int count = statement.executeUpdateDelete();
                if (count != 1) {
                    Log.e(TAG, "Update `custom_flags` returned count=" + count);
                }
                statement.close();
            } catch (Exception e) {
                Log.e(TAG, "remove custom app icon", e);
            }
        } else {
            // nothing custom about this app anymore, remove entry
            db.delete("custom_apps", "_id=?", new String[]{String.valueOf(app.dbId)});
        }

        return app.dbId;
    }

    public static void removeCustomAppName(Context context, String componentName) {
        SQLiteDatabase db = getDatabase(context);
        AppRecord app = getAppRecord(db, componentName);
        if (app == null)
            return;

        if (app.hasCustomIcon()) {
            // app has a custom icon, just remove the custom name
            String sql = "UPDATE custom_apps SET custom_flags=custom_flags&~? WHERE component_name=?";
            try {
                SQLiteStatement statement = db.compileStatement(sql);
                statement.bindLong(1, AppRecord.FLAG_CUSTOM_NAME);
                statement.bindString(2, componentName);
                int count = statement.executeUpdateDelete();
                if (count != 1) {
                    Log.e(TAG, "Update `custom_flags` returned count=" + count);
                }
                statement.close();
            } catch (Exception e) {
                Log.e(TAG, "remove custom app icon", e);
            }
        } else {
            // nothing custom about this app anymore, remove entry
            db.delete("custom_apps", "_id=?", new String[]{String.valueOf(app.dbId)});
        }
    }

    public static HashMap<String, AppRecord> getCustomAppData(Context context) {
        HashMap<String, AppRecord> records;
        SQLiteDatabase db = getDatabase(context);
        try (Cursor cursor = db.query("custom_apps", new String[]{"_id", "name", "component_name", "custom_flags"},
                null, null, null, null, null)) {
            records = new HashMap<>(cursor.getCount());
            while (cursor.moveToNext()) {
                AppRecord entry = new AppRecord();

                entry.dbId = cursor.getInt(0);
                entry.name = cursor.getString(1);
                entry.componentName = cursor.getString(2);
                entry.flags = cursor.getInt(3);

                records.put(entry.componentName, entry);
            }
        }

        return records;
    }

    /**
     * Retrieve a list of all shortcuts for current package name, without icons.
     */
    public static ArrayList<ShortcutRecord> getShortcuts(Context context, String packageName) {
        SQLiteDatabase db = getDatabase(context);

        // Cursor query (String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String
        // orderBy)
        Cursor cursor = db.query("shortcuts", new String[]{"name", "package", "intent_uri"},
                "package = ?", new String[]{packageName}, null, null, null);
        cursor.moveToFirst();

        ArrayList<ShortcutRecord> records = new ArrayList<>();
        while (!cursor.isAfterLast()) {
            ShortcutRecord entry = new ShortcutRecord();

            entry.name = cursor.getString(0);
            entry.packageName = cursor.getString(1);
            entry.intentUri = cursor.getString(2);

            records.add(entry);
            cursor.moveToNext();
        }
        cursor.close();

        return records;
    }

    /**
     * Retrieve a list of all shortcuts, without icons.
     */
    public static ArrayList<ShortcutRecord> getShortcuts(Context context) {
        SQLiteDatabase db = getDatabase(context);

        // Cursor query (String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String
        // orderBy)
        Cursor cursor = db.query("shortcuts", new String[]{"_id", "name", "package", "intent_uri"},
                null, null, null, null, null);
        cursor.moveToFirst();

        ArrayList<ShortcutRecord> records = new ArrayList<>(cursor.getCount());
        while (!cursor.isAfterLast()) {
            ShortcutRecord entry = new ShortcutRecord();

            entry.dbId = cursor.getInt(0);
            entry.name = cursor.getString(1);
            entry.packageName = cursor.getString(2);
            entry.intentUri = cursor.getString(3);

            records.add(entry);
            cursor.moveToNext();
        }
        cursor.close();

        return records;
    }

    /**
     * Remove shortcuts for a given package name
     */
    public static void removeShortcuts(Context context, String packageName) {
        SQLiteDatabase db = getDatabase(context);

        // remove shortcuts
        db.delete("shortcuts", "package LIKE ?", new String[]{"%" + packageName + "%"});
    }

    public static void removeAllShortcuts(Context context) {
        SQLiteDatabase db = getDatabase(context);
        // delete whole table
        db.delete("shortcuts", null, null);
    }

    /**
     * Insert new tags for given id
     *
     * @param context android context
     * @param tag     tag to insert
     * @param record  record to insert
     */
    public static void insertTagsForId(Context context, String tag, String record) {
        SQLiteDatabase db = getDatabase(context);
        ContentValues values = new ContentValues();
        values.put("tag", tag);
        values.put("record", record);
        db.insert("tags", null, values);
    }


    /* Delete
     *
     * @param context android context
     * @param tag   query to insert
     * @param record  record to insert
     */
    public static void deleteTagsForId(Context context, String record) {
        SQLiteDatabase db = getDatabase(context);

        db.delete("tags", "record = ?", new String[]{record});
    }

    public static Map<String, String> loadTags(Context context) {
        Map<String, String> records = new HashMap<>();
        SQLiteDatabase db = getDatabase(context);

        Cursor cursor = db.query("tags", new String[]{"record", "tag"}, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String id = cursor.getString(0);
            String tags = cursor.getString(1);
            records.put(id, tags);
            cursor.moveToNext();
        }
        cursor.close();
        return records;

    }

}
