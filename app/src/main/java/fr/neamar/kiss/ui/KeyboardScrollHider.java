package fr.neamar.kiss.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;

import androidx.annotation.NonNull;

/**
 * Utility class for automatically hiding the keyboard when scrolling down a {@link android.widget.ListView},
 * keeping the position of the finger on the list stable
 */
public class KeyboardScrollHider implements View.OnTouchListener {
    private final static int THRESHOLD = 24;

    private final KeyboardHandler handler;
    private final BlockableListView list;
    private final View listParent;
    private final BottomPullEffectView pullEffect;
    private int listHeightInitial = 0;

    private float offsetYStart = 0;
    private float offsetYCurrent = 0;
    private int offsetYDiff = 0;

    private MotionEvent lastMotionEvent;
    private int initialWindowPadding = 0;
    private boolean resizeDone = false;

    private boolean scrollBarEnabled = true;

    public KeyboardScrollHider(KeyboardHandler handler, BlockableListView list, BottomPullEffectView pullEffect) {
        this.handler = handler;
        this.list = list;
        this.listParent = (View) list.getParent();
        this.pullEffect = pullEffect;
    }

    /**
     * Start monitoring and intercepting touch events of the target list view and providing our
     * transformations
     */
    public void start() {
        this.list.setOnTouchListener(this);
    }

    /**
     *
     */
    @SuppressWarnings("unused")
    public void stop() {
        this.list.setOnTouchListener(null);
        this.handleResizeDone();
    }

    private int getWindowPadding() {
        ViewGroup rootView = (ViewGroup) this.list.getRootView();
        return rootView.getChildAt(0).getPaddingBottom();
    }

    private int getWindowWidth() {
        ViewGroup rootView = (ViewGroup) this.list.getRootView();
        return rootView.getChildAt(0).getWidth();
    }

    private void setListLayoutHeight(int height) {
        final ViewGroup.LayoutParams params = this.list.getLayoutParams();
        params.height = height;
        this.list.setLayoutParams(params);
        this.list.forceLayout();
    }

    protected void handleResizeDone() {
        if (this.resizeDone) {
            return;
        }

        // Give the list view the control over it's input back
        this.list.unblockTouchEvents();

        // Quickly fade out edge pull effect
        this.pullEffect.releasePull();

        // Make sure list uses the height of it's parent
        this.list.setVerticalScrollBarEnabled(this.scrollBarEnabled);
        this.setListLayoutHeight(ViewGroup.LayoutParams.MATCH_PARENT);

        this.resizeDone = true;
    }

    private void updateListViewHeight() {
        // Don't do anything if the window hasn't resized yet or if we're already done
        if (this.getWindowPadding() >= this.initialWindowPadding || this.resizeDone) {
            return;
        }

        // Resize in progress - prevent the view from responding to touch events directly
        this.list.blockTouchEvents();
        this.list.setVerticalScrollBarEnabled(false);

        int heightContainer = this.listParent.getHeight();
        int offsetYDiff = (int) (this.offsetYCurrent - this.offsetYStart);
        if (offsetYDiff < (this.offsetYDiff - THRESHOLD)) {
            double pullFeedback = Math.sqrt((double) (this.offsetYDiff - offsetYDiff) / THRESHOLD);
            offsetYDiff = this.offsetYDiff - (int) (THRESHOLD * pullFeedback);
        }

        // Determine new size of list view widget within its container
        int listLayoutHeight = ViewGroup.LayoutParams.MATCH_PARENT;
        if ((this.listHeightInitial + offsetYDiff) < heightContainer) {
            listLayoutHeight = this.listHeightInitial + offsetYDiff;
        }
        this.setListLayoutHeight(listLayoutHeight);
        if (offsetYDiff > this.offsetYDiff) {
            this.offsetYDiff = offsetYDiff;
        }

        if (this.getWindowPadding() < this.initialWindowPadding
                && listLayoutHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
            // Window size has increased and view has reached it's new maximum size - we're done
            this.handleResizeDone();
            return;
        }

        // Display edge pulling effect while list view is detached from the bottom of its
        // container
        float distance = ((float) (heightContainer - listLayoutHeight)) / heightContainer;
        float displacement = 1 - this.lastMotionEvent.getX() / getWindowWidth();
        this.pullEffect.setPull(distance, displacement, false);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        this.scrollBarEnabled = this.list.isVerticalScrollBarEnabled();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.offsetYStart = event.getY();
                this.offsetYCurrent = event.getY();
                this.offsetYDiff = 0;

                this.lastMotionEvent = event;
                this.resizeDone = false;
                this.initialWindowPadding = this.getWindowPadding();

                // Lock list view height to its current value
                this.listHeightInitial = this.list.getHeight();
                this.setListLayoutHeight(this.listHeightInitial);
                break;

            case MotionEvent.ACTION_MOVE:
                this.offsetYCurrent = event.getY();
                this.lastMotionEvent = event;

                this.updateListViewHeight();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                this.lastMotionEvent = null;

                if (!this.resizeDone) {
                    ValueAnimator animator = ValueAnimator.ofInt(
                            this.list.getHeight(),
                            this.listParent.getHeight()
                    );
                    animator.setDuration(250);
                    animator.setInterpolator(new AccelerateInterpolator());
                    animator.addUpdateListener(animation -> {
                        int height = (int) animation.getAnimatedValue();
                        KeyboardScrollHider.this.setListLayoutHeight(height);
                    });
                    animator.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(@NonNull Animator animation) {
                            // Give the list view the control over it's input back
                            KeyboardScrollHider.this.list.unblockTouchEvents();

                            // Quickly fade out edge pull effect
                            KeyboardScrollHider.this.pullEffect.releasePull();
                        }

                        @Override
                        public void onAnimationEnd(@NonNull Animator animation) {
                            KeyboardScrollHider.this.handleResizeDone();
                        }

                        @Override
                        public void onAnimationCancel(@NonNull Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(@NonNull Animator animation) {
                        }
                    });
                    animator.start();
                } else {
                    this.handleResizeDone();
                }

                break;
        }

        // Hide the keyboard if the user has scrolled down by about half a result item
        if (isScrolled()) {
            this.handler.hideKeyboard();
            this.handler.applyScrollSystemUi();
        }

        return false;
    }

    public void fixScroll() {
        this.list.post(() -> {
            resizeDone = false;
            handleResizeDone();
        });
    }

    public boolean isScrolled() {
        return (this.offsetYCurrent - this.offsetYStart) > THRESHOLD;
    }

    public interface KeyboardHandler {
        void showKeyboard();

        void hideKeyboard();

        void applyScrollSystemUi();
    }
}
