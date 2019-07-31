package fr.neamar.kiss.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.PopupWindow;

import androidx.annotation.StringRes;
import fr.neamar.kiss.R;
import fr.neamar.kiss.utils.SystemUiVisibilityHelper;

public class ListPopup extends PopupWindow {
    private final View.OnClickListener mClickListener;
    private final View.OnLongClickListener mLongClickListener;
    private OnItemLongClickListener mItemLongClickListener;
    private OnItemClickListener mItemClickListener;
    private DataSetObserver mObserver;
    private ListAdapter mAdapter;
    private SystemUiVisibilityHelper mSystemUiVisibilityHelper;
    private boolean dismissOnClick = true;

    public ListPopup(Context context) {
        super(context, null, android.R.attr.popupMenuStyle);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layout);
        setContentView(scrollView);
        setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        mItemClickListener = null;
        mClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( dismissOnClick )
                    dismiss();
                if (mItemClickListener != null) {
                    LinearLayout layout = getLinearLayout();
                    int position = layout.indexOfChild(v);
                    mItemClickListener.onItemClick(mAdapter, v, position);
                }
            }
        };
        mLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mItemLongClickListener == null)
                    return false;
                LinearLayout layout = getLinearLayout();
                int position = layout.indexOfChild(v);
                return mItemLongClickListener.onItemLongClick(mAdapter, v, position);
            }
        };
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mItemClickListener = onItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        mItemLongClickListener = onItemLongClickListener;
    }

    public void setVisibilityHelper(SystemUiVisibilityHelper systemUiVisibility) {
        mSystemUiVisibilityHelper = systemUiVisibility;
        mSystemUiVisibilityHelper.addPopup();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if ( mSystemUiVisibilityHelper != null )
            mSystemUiVisibilityHelper.popPopup();
    }

    public ListAdapter getAdapter() {
        return mAdapter;
    }

    public void setDismissOnItemClick(boolean dismissOnClick )
    {
        this.dismissOnClick = dismissOnClick;
    }

    /**
     * Sets the adapter that provides the data and the views to represent the data
     * in this popup window.
     *
     * @param adapter The adapter to use to create this window's content.
     */
    public void setAdapter(ListAdapter adapter) {
        if (mObserver == null) {
            mObserver = new PopupDataSetObserver();
        } else if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mObserver);
        }
        mAdapter = adapter;
        if (mAdapter != null) {
            adapter.registerDataSetObserver(mObserver);
        }
    }

    private LinearLayout getLinearLayout() {
        return (LinearLayout) ((ScrollView) getContentView()).getChildAt(0);
    }

    private void updateItems() {
        LinearLayout layout = getLinearLayout();
        layout.removeAllViews();
        int adapterCount = mAdapter.getCount();
        for (int i = 0; i < adapterCount; i += 1) {
            View view = mAdapter.getView(i, null, layout);
            layout.addView(view);
            if (mAdapter.isEnabled(i)) {
                view.setOnClickListener(mClickListener);
                if (mItemLongClickListener == null) {
                    view.setLongClickable(false);
                } else {
                    view.setOnLongClickListener(mLongClickListener);
                }
            }
        }
    }

    public void show(View anchor) {
        show(anchor, .5f);
    }

    public void show(View anchor, float anchorOverlap) {
        updateItems();

        if (mSystemUiVisibilityHelper != null)
            mSystemUiVisibilityHelper.copyVisibility(getContentView());

        // don't steal the focus, this will prevent the keyboard from changing
        setFocusable(false);
        // draw over stuff if needed
        setClippingEnabled(false);

        final Rect displayFrame = new Rect();
        anchor.getWindowVisibleDisplayFrame(displayFrame);

        final int[] anchorPos = new int[2];
        anchor.getLocationOnScreen(anchorPos);

        final int distanceToBottom = displayFrame.bottom - (anchorPos[1] + anchor.getHeight());
        final int distanceToTop = anchorPos[1] - displayFrame.top;

        LinearLayout linearLayout = getLinearLayout();

        linearLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        setWidth(linearLayout.getMeasuredWidth());

        int xOffset = anchorPos[0] + anchor.getPaddingLeft();
        if (xOffset + linearLayout.getMeasuredWidth() > displayFrame.right)
            xOffset = displayFrame.right - linearLayout.getMeasuredWidth();

        int overlapAmount = (int) (anchor.getHeight() * anchorOverlap);
        int yOffset;
        if (distanceToBottom > linearLayout.getMeasuredHeight()) {
            // show below anchor
            yOffset = anchorPos[1] + overlapAmount;
            setAnimationStyle(R.style.PopupAnimationTop);
        } else if (distanceToTop > distanceToBottom) {
            // show above anchor
            yOffset = anchorPos[1] + overlapAmount - linearLayout.getMeasuredHeight();
            setAnimationStyle(R.style.PopupAnimationBottom);
            if (distanceToTop < linearLayout.getMeasuredHeight()) {
                // enable scroll
                setHeight(distanceToTop + overlapAmount);
                yOffset += linearLayout.getMeasuredHeight() - distanceToTop - overlapAmount;
            }
        } else {
            // show below anchor with scroll
            yOffset = anchorPos[1] + overlapAmount;
            setAnimationStyle(R.style.PopupAnimationTop);
            setHeight(distanceToBottom + overlapAmount);
        }

        showAtLocation(anchor, Gravity.START | Gravity.TOP, xOffset, yOffset);
    }

    public interface OnItemClickListener {
        void onItemClick(ListAdapter adapter, View view, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(ListAdapter adapter, View view, int position);
    }

    public static class Item {
        @StringRes
        public final int stringId;
        final String string;

        public Item(Context context, @StringRes int stringId) {
            super();
            this.stringId = stringId;
            this.string = context.getResources()
                    .getString(stringId);
        }

        public Item(String string) {
            super();
            this.stringId = 0;
            this.string = string;
        }

        @Override
        public String toString() {
            return this.string;
        }
    }

    protected class ScrollView extends android.widget.ScrollView {
        public ScrollView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            // act as a modal, if we click outside dismiss the popup
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            if ((event.getAction() == MotionEvent.ACTION_DOWN)
                    && ((x < 0) || (x >= getWidth()) || (y < 0) || (y >= getHeight()))) {
                dismiss();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                dismiss();
                return true;
            }
            return super.dispatchTouchEvent(event);
        }
    }

    private class PopupDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            if (isShowing()) {
                // Resize the popup to fit new content
                updateItems();
                update();
            }
        }

        @Override
        public void onInvalidated() {
            dismiss();
        }
    }
}
