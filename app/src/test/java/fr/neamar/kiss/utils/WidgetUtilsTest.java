package fr.neamar.kiss.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for widget sizing helpers.
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
    void initialSizeUsesMinHeight() {
        // 3-line minimum widget starts at 3 lines even though default is smaller
        assertThat(WidgetUtils.getInitialLineSize(450, LINE_HEIGHT_PX, true, 2), equalTo(3));
        assertThat(WidgetUtils.getInitialLineSize(450, LINE_HEIGHT_PX, false, 2), equalTo(3));
    }

    @Test
    void initialSizeUpsizedToDefaultWhenAllowed() {
        // tiny 1-line widget is upsized to the 2-line default if resizing allows it
        assertThat(WidgetUtils.getInitialLineSize(150, LINE_HEIGHT_PX, true, 2), equalTo(2));
    }

    @Test
    void initialSizeKeepsSmallSizeWhenUpsizeForbidden() {
        assertThat(WidgetUtils.getInitialLineSize(150, LINE_HEIGHT_PX, false, 2), equalTo(1));
    }
}
