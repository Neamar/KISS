package fr.neamar.kiss.utils;

/**
 * Pure helpers for widget line arithmetic.
 *
 * Widgets in minimalistic mode are sized as an integral number of "lines"
 * (see {@code Widgets#getLineHeight()}). These helpers are unit-testable on
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
}
