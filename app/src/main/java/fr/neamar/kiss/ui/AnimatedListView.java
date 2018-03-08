package fr.neamar.kiss.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.HashMap;

public class AnimatedListView extends BlockableListView {
    private final static int MOVE_DURATION = 100;
    private final HashMap<Long, ItemInfo> mItemMap = new HashMap<>();

    public AnimatedListView(Context context) {
        super(context);
    }

    public AnimatedListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimatedListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void prepareChangeAnim() {
        mItemMap.clear();

        // store positions before the update
        int firstVisiblePosition = this.getFirstVisiblePosition();
        int nCount = Math.min(this.getChildCount(), getAdapter().getCount() - firstVisiblePosition);
        for (int i = 0; i < nCount; i += 1) {
            View child = this.getChildAt(i);
            child.clearAnimation();
            int position = firstVisiblePosition + i;
            long itemId = getAdapter().getItemId(position);
            mItemMap.put(itemId, new ItemInfo(i, child.getTop()));
        }
    }

    public void animateChange() {
        if (mItemMap.isEmpty())
            return;

        // check if we can use the ViewTreeObserver for animations
        final ViewTreeObserver observer = this.getViewTreeObserver();
        if (!observer.isAlive())
            return;

        // postpone animation to after the layout is computed and views are rebound
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (!observer.isAlive())
                    return true;
                observer.removeOnPreDrawListener(this);
                AnimatedListView listView = AnimatedListView.this;

                // this is called after the layout is updated to the new list
                int firstVisiblePosition = listView.getFirstVisiblePosition();
                int nCount = Math.min(listView.getChildCount(), getAdapter().getCount() - firstVisiblePosition);
                for (int i = 0; i < nCount; i += 1) {
                    int position = firstVisiblePosition + i;
                    long itemId = getAdapter().getItemId(position);
                    View child = listView.getChildAt(i);
                    int topAfterLayout = child.getTop();
                    int delta;
                    if (mItemMap.containsKey(itemId)) {
                        int topBeforeLayout = mItemMap.get(itemId).top;
                        // this view may have moved
                        delta = topBeforeLayout - topAfterLayout;
                    } else {
                        // this is a new view
                        if (i == 0) {
                            // the first visible position can slide from the top
                            delta = -child.getHeight() - listView.getDividerHeight();
                        } else {
                            delta = 0;

                            // animate new views
                            child.setScaleY(0.f);
                            child.animate()
                                    .setDuration(MOVE_DURATION)
                                    .scaleY(1.f);
                        }
                    }
                    if (delta != 0) {
                        child.setTranslationY(delta);
                        child.animate()
                                .setDuration(MOVE_DURATION)
                                .translationY(0);
                    }
                }

                return false;
            }
        });
    }

    static class ItemInfo {
        final int top;
        final int viewIndex;
        final boolean validated;

        ItemInfo(int index, int top) {
            this.viewIndex = index;
            this.top = top;
            this.validated = false;
        }
    }
}
