package fr.neamar.kiss.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class ExtendedRecyclerView extends RecyclerView {

    private int mFixedColumnWidth = -1;
    private int mFixedNumColumns = -1;

    private int mOrigSpanCount = -1;

    public ExtendedRecyclerView(Context context) {
        super(context);
    }

    public ExtendedRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public ExtendedRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (context != null && attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.numColumns});
            mFixedNumColumns = array.getInteger(0, -1);
            array.recycle();

            array = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.columnWidth});
            mFixedColumnWidth = array.getDimensionPixelSize(0, -1);
            array.recycle();
        }
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        super.setLayoutManager(layout);

        if (layout instanceof GridLayoutManager) {
            mOrigSpanCount = ((GridLayoutManager) layout).getSpanCount();
        } else if (layout instanceof StaggeredGridLayoutManager) {
            mOrigSpanCount = ((StaggeredGridLayoutManager) layout).getSpanCount();
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);

        if (mFixedNumColumns > 0) {
            if (getLayoutManager() instanceof GridLayoutManager) {
                ((GridLayoutManager) getLayoutManager()).setSpanCount(checkSpanCount(mFixedNumColumns));
            } else if (getLayoutManager() instanceof StaggeredGridLayoutManager) {
                ((StaggeredGridLayoutManager) getLayoutManager()).setSpanCount(checkSpanCount(mFixedNumColumns));
            }
        } else if (mFixedColumnWidth > 0) {
            int spanCount = mOrigSpanCount;

            if (getLayoutManager() instanceof GridLayoutManager) {
                GridLayoutManager mGridLM = (GridLayoutManager) getLayoutManager();

                switch (mGridLM.getOrientation()) {
                    case RecyclerView.VERTICAL: {
                        spanCount = getMeasuredWidth() / mFixedColumnWidth;
                        break;
                    }
                    case RecyclerView.HORIZONTAL: {
                        spanCount = getMeasuredHeight() / mFixedColumnWidth;
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("wrong orientation");
                }
                mGridLM.setSpanCount(checkSpanCount(spanCount));
            } else if (getLayoutManager() instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager mStaggeredGridLM = (StaggeredGridLayoutManager) getLayoutManager();

                switch (mStaggeredGridLM.getOrientation()) {
                    case StaggeredGridLayoutManager.VERTICAL: {
                        spanCount = getMeasuredWidth() / mFixedColumnWidth;
                        break;
                    }
                    case StaggeredGridLayoutManager.HORIZONTAL: {
                        spanCount = getMeasuredHeight() / mFixedColumnWidth;
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("wrong orientation");
                }
                mStaggeredGridLM.setSpanCount(checkSpanCount(spanCount));
            }
        }
    }

    private int checkSpanCount(int spanCount) {
        if (spanCount < 1) return 1;
        //if (spanCount > mOrigSpanCount) return mOrigSpanCount;
        return spanCount;
    }
}
