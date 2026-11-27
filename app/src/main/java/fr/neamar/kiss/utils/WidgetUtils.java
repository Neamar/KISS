package fr.neamar.kiss.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure helpers for widget line arithmetic and layout.
 *
 * Widgets in minimalistic mode are sized as an integral number of "lines"
 * (see {@code Widgets#getLineHeight()}) and may share a row. These helpers are unit-testable on
 * the JVM and hold no Android framework dependencies.
 */
public final class WidgetUtils {
    private WidgetUtils() {
    }

    /**
     * Convert a pixel height into a number of layout lines, rounding up.
     *
     * @param heightPx     height in pixels
     * @param lineHeightPx height of a single line in pixels
     * @return number of lines, always at least 1
     */
    public static int getLineSize(int heightPx, float lineHeightPx) {
        return Math.max(1, (int) Math.ceil(heightPx / lineHeightPx));
    }

    /**
     * Effective minimum height of a widget, snapped to whole lines.
     *
     * @param minHeightPx          {@link android.appwidget.AppWidgetProviderInfo#minHeight}
     * @param targetCellHeightCells {@link android.appwidget.AppWidgetProviderInfo#targetCellHeight} (0 if unsupported)
     * @param lineHeightPx         height of a single line in pixels
     * @return minimum height in pixels
     */
    public static int getMinHeight(int minHeightPx, int targetCellHeightCells, float lineHeightPx) {
        if (targetCellHeightCells > 0) {
            return (int) (targetCellHeightCells * lineHeightPx);
        } else if (minHeightPx == 0) {
            return 0;
        } else {
            return (int) (getLineSize(minHeightPx, lineHeightPx) * lineHeightPx);
        }
    }

    /**
     * Persisted configuration of a single widget.
     */
    public static final class WidgetConf {
        public final int appWidgetId;
        public final int lineSize;

        public WidgetConf(int appWidgetId, int lineSize) {
            this.appWidgetId = appWidgetId;
            this.lineSize = lineSize;
        }
    }

    /**
     * Parse widget configuration into rows.
     *
     * Entries use the legacy {@code id-lineSize} format. A trailing {@code -1}
     * marks an entry as sharing the previous entry's row.
     */
    public static List<List<WidgetConf>> parseLayout(String conf) {
        List<List<WidgetConf>> rows = new ArrayList<>();
        if (conf == null) {
            return rows;
        }
        for (String entry : conf.split(";")) {
            String[] parts = entry.split("-");
            if (parts.length < 2) {
                continue;
            }
            WidgetConf widgetConf;
            try {
                widgetConf = new WidgetConf(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            } catch (NumberFormatException e) {
                continue;
            }
            if (parts.length > 2 && "1".equals(parts[2]) && !rows.isEmpty()) {
                rows.get(rows.size() - 1).add(widgetConf);
            } else {
                List<WidgetConf> row = new ArrayList<>();
                row.add(widgetConf);
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Serialize widget rows. The legacy format remains readable by older versions.
     */
    public static String serializeLayout(List<List<WidgetConf>> rows) {
        StringBuilder builder = new StringBuilder();
        for (List<WidgetConf> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                WidgetConf widgetConf = row.get(i);
                if (builder.length() > 0) {
                    builder.append(';');
                }
                builder.append(widgetConf.appWidgetId).append('-').append(widgetConf.lineSize);
                if (i > 0) {
                    builder.append("-1");
                }
            }
        }
        return builder.toString();
    }

    public static <T> boolean canMoveUp(List<List<T>> rows, T item) {
        int rowIndex = indexOfRow(rows, item);
        return rowIndex > 0 || (rowIndex == 0 && rows.get(0).size() > 1);
    }

    public static <T> boolean canMoveDown(List<List<T>> rows, T item) {
        int rowIndex = indexOfRow(rows, item);
        return rowIndex >= 0 && (rowIndex < rows.size() - 1 || rows.get(rowIndex).size() > 1);
    }

    public static <T> boolean canMoveLeft(List<List<T>> rows, T item) {
        return canMoveSideways(rows, item, true);
    }

    public static <T> boolean canMoveRight(List<List<T>> rows, T item) {
        return canMoveSideways(rows, item, false);
    }

    public static <T> boolean moveUp(List<List<T>> rows, T item) {
        if (!canMoveUp(rows, item)) {
            return false;
        }
        int rowIndex = indexOfRow(rows, item);
        List<T> row = rows.get(rowIndex);
        if (row.size() > 1) {
            row.remove(item);
            rows.add(rowIndex, newRow(item));
        } else {
            Collections.swap(rows, rowIndex, rowIndex - 1);
        }
        return true;
    }

    public static <T> boolean moveDown(List<List<T>> rows, T item) {
        if (!canMoveDown(rows, item)) {
            return false;
        }
        int rowIndex = indexOfRow(rows, item);
        List<T> row = rows.get(rowIndex);
        if (row.size() > 1) {
            row.remove(item);
            rows.add(rowIndex + 1, newRow(item));
        } else {
            Collections.swap(rows, rowIndex, rowIndex + 1);
        }
        return true;
    }

    public static <T> boolean moveLeft(List<List<T>> rows, T item) {
        return moveSideways(rows, item, true);
    }

    public static <T> boolean moveRight(List<List<T>> rows, T item) {
        return moveSideways(rows, item, false);
    }

    private static <T> boolean canMoveSideways(List<List<T>> rows, T item, boolean left) {
        int rowIndex = indexOfRow(rows, item);
        if (rowIndex < 0) {
            return false;
        }
        List<T> row = rows.get(rowIndex);
        if (row.size() == 1) {
            return rowIndex > 0;
        }
        int column = row.indexOf(item);
        return left ? column > 0 : column < row.size() - 1;
    }

    private static <T> boolean moveSideways(List<List<T>> rows, T item, boolean left) {
        if (!canMoveSideways(rows, item, left)) {
            return false;
        }
        int rowIndex = indexOfRow(rows, item);
        List<T> row = rows.get(rowIndex);
        if (row.size() == 1) {
            rows.remove(rowIndex);
            List<T> rowAbove = rows.get(rowIndex - 1);
            rowAbove.add(left ? 0 : rowAbove.size(), item);
        } else {
            int column = row.indexOf(item);
            Collections.swap(row, column, left ? column - 1 : column + 1);
        }
        return true;
    }

    private static <T> int indexOfRow(List<List<T>> rows, T item) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).contains(item)) {
                return i;
            }
        }
        return -1;
    }

    private static <T> List<T> newRow(T item) {
        List<T> row = new ArrayList<>();
        row.add(item);
        return row;
    }
}
