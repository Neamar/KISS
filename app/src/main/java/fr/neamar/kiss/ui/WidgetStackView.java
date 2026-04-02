package fr.neamar.kiss.ui;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.preference.PreferenceManager;

import fr.neamar.kiss.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Container that holds multiple widgets in a stack.
 * Only one widget is visible at a time, users can swipe or tap to switch between widgets.
 */
public class WidgetStackView extends FrameLayout {
    private static final String PREF_WIDGET_STACK_SHOW_DOT_INDICATOR = "widget-stack-show-dot-indicator";

    private final List<AppWidgetHostView> widgets = new ArrayList<>();
    private int currentIndex = 0;
    private GestureDetector gestureDetector;
    private OnStackChangedListener stackChangedListener;
    private LinearLayout dotIndicatorContainer;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;

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
        setBackgroundResource(R.drawable.widget_stack_container);
        setElevation(getResources().getDimensionPixelSize(R.dimen.widget_stack_elevation));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setClipToOutline(true);
        }
        dotIndicatorContainer = (LinearLayout) inflate(getContext(), R.layout.widget_stack_indicator, null);
        FrameLayout.LayoutParams indicatorParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        indicatorParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        addView(dotIndicatorContainer, indicatorParams);
        dotIndicatorContainer.setVisibility(View.GONE);
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
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        prefListener = (prefs, key) -> {
            if (PREF_WIDGET_STACK_SHOW_DOT_INDICATOR.equals(key)) {
                updateStackIndicator();
            }
        };
        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(prefListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (prefListener != null) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(prefListener);
            prefListener = null;
        }
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
        int count = widgets.size();
        if (count == 0) {
            updateStackContentDescription();
            updateStackIndicator();
            return;
        }
        int nextIndex = (currentIndex + 1) % count;
        for (int i = 0; i < count; i++) {
            AppWidgetHostView widget = widgets.get(i);
            widget.setTranslationX(0);
            if (i == currentIndex) {
                widget.setVisibility(View.VISIBLE);
                widget.bringToFront();
            } else if (count > 1 && i == nextIndex) {
                widget.setVisibility(View.VISIBLE);
            } else {
                widget.setVisibility(View.GONE);
            }
        }
        updateStackContentDescription();
    }

    private void notifyStackChanged() {
        updateStackContentDescription();
        updateStackIndicator();
        if (stackChangedListener != null) {
            stackChangedListener.onStackChanged(currentIndex, widgets.size());
        }
    }

    private void updateStackIndicator() {
        int count = widgets.size();
        boolean showIndicator = count > 1 && PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean(PREF_WIDGET_STACK_SHOW_DOT_INDICATOR, true);
        if (!showIndicator) {
            dotIndicatorContainer.setVisibility(View.GONE);
            return;
        }
        dotIndicatorContainer.setVisibility(View.VISIBLE);
        int dotSizePx = getResources().getDimensionPixelSize(R.dimen.widget_stack_dot_size);
        int spacingPx = getResources().getDimensionPixelSize(R.dimen.widget_stack_dot_spacing);
        dotIndicatorContainer.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSizePx, dotSizePx);
            if (i > 0) {
                params.setMarginStart(spacingPx);
            }
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == currentIndex ? R.drawable.widget_stack_dot_active : R.drawable.widget_stack_dot_inactive);
            dotIndicatorContainer.addView(dot);
        }
        dotIndicatorContainer.setContentDescription(getContext().getString(R.string.widget_stack_content_description,
                currentIndex + 1, count));
        dotIndicatorContainer.bringToFront();
    }

    private void updateStackContentDescription() {
        if (widgets.size() > 1) {
            setContentDescription(getContext().getString(R.string.widget_stack_content_description,
                    currentIndex + 1, widgets.size()));
        } else {
            setContentDescription(null);
        }
    }

    public void setOnStackChangedListener(OnStackChangedListener listener) {
        this.stackChangedListener = listener;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int width = right - left;
        int count = widgets.size();
        int peekPx = getResources().getDimensionPixelSize(R.dimen.widget_stack_peek_width);
        for (int i = 0; i < count; i++) {
            AppWidgetHostView widget = widgets.get(i);
            ViewGroup.LayoutParams params = widget.getLayoutParams();
            if (params != null) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                if (params.height == ViewGroup.LayoutParams.MATCH_PARENT || params.height <= 0) {
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
            }
            if (count > 1 && width > 0) {
                int nextIndex = (currentIndex + 1) % count;
                if (i == nextIndex) {
                    widget.setTranslationX(-(width - peekPx));
                }
            }
        }
    }
}
