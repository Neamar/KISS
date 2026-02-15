package fr.neamar.kiss.ui;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Container that holds multiple widgets in a stack.
 * Only one widget is visible at a time, users can swipe or tap to switch between widgets.
 */
public class WidgetStackView extends FrameLayout {
    private final List<AppWidgetHostView> widgets = new ArrayList<>();
    private int currentIndex = 0;
    private GestureDetector gestureDetector;
    private OnStackChangedListener stackChangedListener;

    public interface OnStackChangedListener {
        void onStackChanged(int currentIndex, int totalCount);
    }

    public WidgetStackView(Context context) {
        super(context);
        init();
    }

    public WidgetStackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WidgetStackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // Swipe left to go to next widget
                if (e1.getX() - e2.getX() > 100 && Math.abs(velocityX) > 500) {
                    showNextWidget();
                    return true;
                }
                // Swipe right to go to previous widget
                if (e2.getX() - e1.getX() > 100 && Math.abs(velocityX) > 500) {
                    showPreviousWidget();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // Double tap to cycle through widgets (less intrusive than single tap)
                if (widgets.size() > 1) {
                    showNextWidget();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Only intercept if we have multiple widgets and it's a gesture we can handle
        if (widgets.size() > 1) {
            return gestureDetector.onTouchEvent(ev);
        }
        return false;
    }

    /**
     * Add a widget to the stack
     */
    public void addWidget(AppWidgetHostView widget) {
        widgets.add(widget);
        
        // Preserve the widget's layout params when adding to stack
        ViewGroup.LayoutParams originalParams = widget.getLayoutParams();
        if (originalParams != null && originalParams.height > 0 && 
            originalParams.height != ViewGroup.LayoutParams.WRAP_CONTENT &&
            originalParams.height != ViewGroup.LayoutParams.MATCH_PARENT) {
            // Widget has a specific height, preserve it
            ViewGroup.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                originalParams.height
            );
            widget.setLayoutParams(params);
        } else {
            // Use match parent for width, wrap content for height
            ViewGroup.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            widget.setLayoutParams(params);
        }
        
        if (widgets.size() == 1) {
            // First widget, show it
            addView(widget);
        } else {
            // Additional widget, hide it initially
            widget.setVisibility(View.GONE);
            addView(widget);
        }
        notifyStackChanged();
    }

    /**
     * Remove a widget from the stack
     */
    public void removeWidget(AppWidgetHostView widget) {
        int index = widgets.indexOf(widget);
        if (index == -1) {
            return;
        }

        widgets.remove(widget);
        removeView(widget);

        // Adjust current index if needed
        if (currentIndex >= widgets.size() && widgets.size() > 0) {
            currentIndex = widgets.size() - 1;
        }
        if (currentIndex < 0) {
            currentIndex = 0;
        }

        // Show the current widget
        updateVisibleWidget();
        notifyStackChanged();
    }

    /**
     * Get all widgets in the stack
     */
    public List<AppWidgetHostView> getWidgets() {
        return new ArrayList<>(widgets);
    }

    /**
     * Get the number of widgets in the stack
     */
    public int getWidgetCount() {
        return widgets.size();
    }

    /**
     * Get the currently visible widget
     */
    public AppWidgetHostView getCurrentWidget() {
        if (widgets.isEmpty()) {
            return null;
        }
        return widgets.get(currentIndex);
    }

    /**
     * Show the next widget in the stack
     */
    public void showNextWidget() {
        if (widgets.size() <= 1) {
            return;
        }
        currentIndex = (currentIndex + 1) % widgets.size();
        updateVisibleWidget();
        notifyStackChanged();
    }

    /**
     * Show the previous widget in the stack
     */
    public void showPreviousWidget() {
        if (widgets.size() <= 1) {
            return;
        }
        currentIndex = (currentIndex - 1 + widgets.size()) % widgets.size();
        updateVisibleWidget();
        notifyStackChanged();
    }

    /**
     * Set the currently visible widget by index
     */
    public void setCurrentWidget(int index) {
        if (index >= 0 && index < widgets.size()) {
            currentIndex = index;
            updateVisibleWidget();
            notifyStackChanged();
        }
    }

    /**
     * Check if this stack contains a specific widget
     */
    public boolean containsWidget(AppWidgetHostView widget) {
        return widgets.contains(widget);
    }

    /**
     * Get the index of a widget in the stack
     */
    public int getWidgetIndex(AppWidgetHostView widget) {
        return widgets.indexOf(widget);
    }

    private void updateVisibleWidget() {
        for (int i = 0; i < widgets.size(); i++) {
            AppWidgetHostView widget = widgets.get(i);
            if (i == currentIndex) {
                widget.setVisibility(View.VISIBLE);
                widget.bringToFront();
            } else {
                widget.setVisibility(View.GONE);
            }
        }
    }

    private void notifyStackChanged() {
        if (stackChangedListener != null) {
            stackChangedListener.onStackChanged(currentIndex, widgets.size());
        }
    }

    public void setOnStackChangedListener(OnStackChangedListener listener) {
        this.stackChangedListener = listener;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // Ensure all widgets have proper width, but preserve their height
        for (AppWidgetHostView widget : widgets) {
            ViewGroup.LayoutParams params = widget.getLayoutParams();
            if (params != null) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                // Don't override height - preserve the height set by setWidgetSize()
                // Only set to WRAP_CONTENT if height is not already set to a specific value
                if (params.height == ViewGroup.LayoutParams.MATCH_PARENT || params.height <= 0) {
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
            }
        }
    }
}
