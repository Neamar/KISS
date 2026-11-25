package fr.neamar.kiss.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for widget sizing and layout helpers.
 *
 * Widgets are laid out in "lines" of a fixed pixel height
 * ({@code Widgets#getLineHeight()}); these tests verify the pure
 * line-arithmetic used to size newly added widgets.
 */
class WidgetUtilsTest {
    private static final float LINE_HEIGHT_PX = 150f; // 50dp @ 3x density

    @Test
    void zeroHeightSnapsToOneLine() {
        assertThat(WidgetUtils.getLineSize(0, LINE_HEIGHT_PX), equalTo(1));
    }

    @Test
    void partialLineRoundsUp() {
        assertThat(WidgetUtils.getLineSize(1, LINE_HEIGHT_PX), equalTo(1));
        assertThat(WidgetUtils.getLineSize(151, LINE_HEIGHT_PX), equalTo(2));
        assertThat(WidgetUtils.getLineSize(299, LINE_HEIGHT_PX), equalTo(2));
    }

    @Test
    void exactMultipleOfLineHeight() {
        assertThat(WidgetUtils.getLineSize(300, LINE_HEIGHT_PX), equalTo(2));
        assertThat(WidgetUtils.getLineSize(600, LINE_HEIGHT_PX), equalTo(4));
    }

    @Test
    void minHeightFromTargetCellHeight() {
        // API S+ widgets declare their preferred size in cells
        assertThat(WidgetUtils.getMinHeight(1000, 3, LINE_HEIGHT_PX), equalTo(450));
    }

    @Test
    void minHeightZeroStaysZero() {
        assertThat(WidgetUtils.getMinHeight(0, 0, LINE_HEIGHT_PX), equalTo(0));
    }

    @Test
    void minHeightSnapsToWholeLines() {
        // minHeight of 1.4 lines snaps to 2 lines
        assertThat(WidgetUtils.getMinHeight(210, 0, LINE_HEIGHT_PX), equalTo(300));
    }

    @Test
    void parseAndSerializeWidgetRows() {
        String config = "12-3;13-2;14-4-1;15-1";
        List<List<WidgetUtils.WidgetConf>> rows = WidgetUtils.parseLayout(config);

        assertThat(rows.size(), equalTo(3));
        assertThat(rows.get(1).size(), equalTo(2));
        assertThat(rows.get(1).get(1).appWidgetId, equalTo(14));
        assertThat(WidgetUtils.serializeLayout(rows), equalTo(config));
    }

    @Test
    void parseLegacyWidgetLayoutAndIgnoreMalformedEntries() {
        List<List<WidgetUtils.WidgetConf>> rows = WidgetUtils.parseLayout("12-3;bad;13-2");

        assertThat(rows.size(), equalTo(2));
        assertThat(rows.get(0).get(0).appWidgetId, equalTo(12));
        assertThat(rows.get(1).get(0).lineSize, equalTo(2));
        assertThat(WidgetUtils.parseLayout(null).isEmpty(), equalTo(true));
    }

    @Test
    void horizontalMovesJoinRowsOrSwapNeighbours() {
        List<List<String>> rows = rows("calendar", "clock", "timer");
        assertThat(WidgetUtils.moveRight(rows, "timer"), equalTo(true));
        assertThat(rows, equalTo(rows("calendar", "clock|timer")));

        rows = rows("clock", "timer");
        assertThat(WidgetUtils.moveLeft(rows, "timer"), equalTo(true));
        assertThat(rows, equalTo(rows("timer|clock")));

        rows = rows("a|b|c");
        assertThat(WidgetUtils.moveRight(rows, "a"), equalTo(true));
        assertThat(rows, equalTo(rows("b|a|c")));
        assertThat(WidgetUtils.moveLeft(rows, "c"), equalTo(true));
        assertThat(rows, equalTo(rows("b|c|a")));
    }

    @Test
    void horizontalMoveCannotLeaveFirstRowOrCrossRowEdge() {
        List<List<String>> rows = rows("a|b", "c");
        assertThat(WidgetUtils.canMoveLeft(rows, "a"), equalTo(false));
        assertThat(WidgetUtils.canMoveRight(rows, "b"), equalTo(false));
        assertThat(WidgetUtils.moveLeft(rows, "missing"), equalTo(false));
        assertThat(WidgetUtils.canMoveLeft(rows, "c"), equalTo(true));
    }

    @Test
    void verticalMovesLeaveSharedRow() {
        List<List<String>> rows = rows("top", "a|b", "bottom");
        assertThat(WidgetUtils.moveUp(rows, "b"), equalTo(true));
        assertThat(rows, equalTo(rows("top", "b", "a", "bottom")));

        rows = rows("top", "a|b", "bottom");
        assertThat(WidgetUtils.moveDown(rows, "a"), equalTo(true));
        assertThat(rows, equalTo(rows("top", "b", "a", "bottom")));

        rows = rows("top", "middle", "bottom");
        assertThat(WidgetUtils.moveUp(rows, "bottom"), equalTo(true));
        assertThat(rows, equalTo(rows("top", "bottom", "middle")));
        assertThat(WidgetUtils.moveDown(rows, "top"), equalTo(true));
        assertThat(rows, equalTo(rows("bottom", "top", "middle")));
    }

    private static List<List<String>> rows(String... rowContents) {
        List<List<String>> rows = new ArrayList<>();
        for (String rowContent : rowContents) {
            rows.add(new ArrayList<>(Arrays.asList(rowContent.split("\\|"))));
        }
        return rows;
    }
}
