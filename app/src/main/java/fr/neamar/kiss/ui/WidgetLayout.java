package fr.neamar.kiss.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;

import fr.neamar.kiss.R;
import fr.neamar.kiss.forwarder.Widget;

/**
 * Example of writing a custom layout manager.  This is a fairly full-featured
 * layout manager that is relatively general, handling all layout cases.  You
 * can simplify it for more specific cases.
 * https://developer.android.com/reference/android/view/ViewGroup
 */
@RemoteViews.RemoteView
public class WidgetLayout extends ViewGroup {
    /**
     * The amount of space used by children in the left gutter.
     */
    private int mLeftWidth;

    /**
     * The amount of space used by children in the right gutter.
     */
    private int mRightWidth;

    /**
     * These are used for computing child frames based on their gravity.
     */
    private final Rect mTmpContainerRect = new Rect();
    private final Rect mTmpChildRect = new Rect();

    private Widget mWidget = null;

    public WidgetLayout(Context context) {
        super(context);
    }

    public WidgetLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Any layout manager that doesn't scroll will want this.
     */
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    /**
     * Ask all children to measure themselves and compute the measurement of this
     * layout based on the children.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        // These keep track of the space we are using on the left and right for
        // views positioned there; we need member variables so we can also use
        // these for layout later.
        mLeftWidth = 0;
        mRightWidth = 0;

        // Measurement will ultimately be computing these values.
        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;

        // Iterate through all children, measuring them and computing our dimensions
        // from their size.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE)
                continue;
            // Measure the child.
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);

            // Update our size information based on the layout params.  Children
            // that asked to be positioned on the left or right go in those gutters.
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.position == LayoutParams.POSITION_LEFT) {
                mLeftWidth += Math.max(maxWidth,
                        child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            } else if (lp.position == LayoutParams.POSITION_RIGHT) {
                mRightWidth += Math.max(maxWidth,
                        child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            } else {
                maxWidth = Math.max(maxWidth,
                        child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            }
            maxHeight = Math.max(maxHeight,
                    child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
            childState = combineMeasuredStates(childState, child.getMeasuredState());
        }

        // Total width is the maximum width of all inner children plus the gutters.
        maxWidth += mLeftWidth + mRightWidth;

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        // Report our final dimensions.
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    /**
     * Position all children within this layout.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int count = getChildCount();

        // These are the far left and right edges in which we are performing layout.
        int leftPos = getPaddingLeft();
        int rightPos = getLayoutParams().width - getPaddingRight();

        // This is the middle region inside of the gutter.
        final int middleLeft = leftPos + mLeftWidth;
        final int middleRight = rightPos - mRightWidth;

        // These are the top and bottom edges in which we are performing layout.
        final int parentTop = getPaddingTop();
        final int parentBottom = bottom - top - getPaddingBottom();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE)
                continue;
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            final int width = child.getMeasuredWidth();
            final int height = child.getMeasuredHeight();

            // Compute the frame in which we are placing this child.
            if (lp.position == LayoutParams.POSITION_LEFT) {
                mTmpContainerRect.left = leftPos + lp.leftMargin;
                mTmpContainerRect.right = leftPos + width + lp.rightMargin;
                leftPos = mTmpContainerRect.right;
            } else if (lp.position == LayoutParams.POSITION_RIGHT) {
                mTmpContainerRect.right = rightPos - lp.rightMargin;
                mTmpContainerRect.left = rightPos - width - lp.leftMargin;
                rightPos = mTmpContainerRect.left;
            } else {
                mTmpContainerRect.left = middleLeft + lp.leftMargin;
                mTmpContainerRect.right = middleRight - lp.rightMargin;
            }
            mTmpContainerRect.top = parentTop + lp.topMargin;
            mTmpContainerRect.bottom = parentBottom - lp.bottomMargin;

            // Use the child's gravity and size to determine its final frame within its container.
            Gravity.apply(lp.gravity, width, height, mTmpContainerRect, mTmpChildRect);

            // Place the child.
            child.layout(mTmpChildRect.left, mTmpChildRect.top,
                    mTmpChildRect.right, mTmpChildRect.bottom);

            if (mWidget != null)
                mWidget.onWidgetLayout(child, changed, mTmpChildRect);
        }
    }

    // ----------------------------------------------------------------------
    // The rest of the implementation is for custom per-child layout parameters.
    // If you do not need these (for example you are writing a layout manager
    // that does fixed positioning of its children), you can drop all of this.

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new WidgetLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public void setWidgetForwarder(Widget widget) {
        mWidget = widget;
    }

    public void scrollWidgets(float fCurrent) {
        //TODO: Fix this! We assume the widget area size is 3x screen size
        int screenSize = getLayoutParams().width / 3;
        int scrollX = (int) (screenSize * 2.f * fCurrent);
        setScrollX(scrollX);
    }

    /**
     * Custom per-child layout information.
     */
    public static class LayoutParams extends MarginLayoutParams {
        /**
         * The gravity to apply with the View to which these layout parameters
         * are associated.
         */
        public int gravity = Gravity.TOP;

        public static int POSITION_MIDDLE = 0;
        public static int POSITION_LEFT = 1;
        public static int POSITION_RIGHT = 2;

        public int position = POSITION_MIDDLE;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            // Pull the layout param values from the layout XML during
            // inflation.  This is not needed if you don't care about
            // changing the layout behavior in XML.
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.WidgetLayoutLP);
            gravity = a.getInt(R.styleable.WidgetLayoutLP_android_layout_gravity, gravity);
            position = a.getInt(R.styleable.WidgetLayoutLP_layout_position, position);
            a.recycle();
            /* Put this in attrs.xml
            <declare-styleable name="WidgetLayoutLP">
                <attr name="android:layout_gravity" />
                <attr name="layout_position">
                    <enum name="middle" value="0" />
                    <enum name="left" value="1" />
                    <enum name="right" value="2" />
                </attr>
            </declare-styleable>
             */
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}