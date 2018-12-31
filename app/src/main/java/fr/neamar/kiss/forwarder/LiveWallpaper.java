package fr.neamar.kiss.forwarder;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import fr.neamar.kiss.MainActivity;

class LiveWallpaper extends Forwarder {
    private final boolean wallpaperIsVisible;

    private WallpaperManager mWallpaperManager;
    private Point mWindowSize;
    private android.os.IBinder mWindowToken;
    private View mContentView;
    private float mLastTouchPos;
    private float mWallpaperOffset;
    private LiveWallpaper.Anim mAnimation;
    private VelocityTracker mVelocityTracker;

    LiveWallpaper(MainActivity mainActivity) {
        super(mainActivity);
        TypedValue typedValue = new TypedValue();
        mainActivity.getTheme().resolveAttribute(android.R.attr.windowShowWallpaper, typedValue, true);
        TypedArray a = mainActivity.obtainStyledAttributes(typedValue.resourceId, new int[]{android.R.attr.windowShowWallpaper});
        wallpaperIsVisible = a.getBoolean(0, true);
        a.recycle();

        if(!wallpaperIsVisible) {
            return;
        }

        mWallpaperManager = (WallpaperManager) mainActivity.getSystemService(Context.WALLPAPER_SERVICE);
        assert mWallpaperManager != null;

        mContentView = mainActivity.findViewById(android.R.id.content);
        mWallpaperManager.setWallpaperOffsetSteps(.5f, 0.f);
        mWallpaperOffset = 0.5f; // this is the center
        mAnimation = new LiveWallpaper.Anim();
        mVelocityTracker = null;
        mWindowSize = new Point(1, 1);
    }

    boolean onTouch(View view, MotionEvent event) {
        if(!wallpaperIsVisible) {
            return false;
        }

        int actionMasked = event.getActionMasked();
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                if (isPreferenceWPDragAnimate()) {
                    mContentView.clearAnimation();

                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(event);

                    mLastTouchPos = event.getRawX();
                    mainActivity.getWindowManager()
                            .getDefaultDisplay()
                            .getSize(mWindowSize);
                }
                //send touch event to the LWP
                if (isPreferenceLWPTouch())
                    sendTouchEvent(view, event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mVelocityTracker != null) {
                    mVelocityTracker.addMovement(event);

                    float fTouchPos = event.getRawX();
                    float fOffset = (mLastTouchPos - fTouchPos) * 1.01f / mWindowSize.x;
                    fOffset += mWallpaperOffset;
                    updateWallpaperOffset(fOffset);
                    mLastTouchPos = fTouchPos;
                }

                //send move/drag event to the LWP
                if (isPreferenceLWPDrag())
                    sendTouchEvent(view, event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mVelocityTracker != null) {
                    mVelocityTracker.addMovement(event);

                    if (mAnimation.init())
                        mContentView.startAnimation(mAnimation);

                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
        }

        // do not consume the event
        return false;
    }

    private boolean isPreferenceLWPTouch() {
        return prefs.getBoolean("lwp-touch", true);
    }

    private boolean isPreferenceLWPDrag() {
        return prefs.getBoolean("lwp-drag", false);
    }

    private boolean isPreferenceWPDragAnimate() {
        return prefs.getBoolean("wp-drag-animate", false);
    }

    private boolean isPreferenceWPReturnCenter() {
        return prefs.getBoolean("wp-animate-center", true);
    }

    private boolean isPreferenceWPStickToSides() {
        return prefs.getBoolean("wp-animate-sides", false);
    }

    private android.os.IBinder getWindowToken() {
        return mWindowToken != null ? mWindowToken : (mWindowToken = mContentView.getWindowToken());
    }

    private void updateWallpaperOffset(float offset) {
        android.os.IBinder iBinder = getWindowToken();
        if (iBinder != null) {
            offset = Math.max(0.f, Math.min(1.f, offset));
            mWallpaperOffset = offset;
            mWallpaperManager.setWallpaperOffsets(iBinder, mWallpaperOffset, 0.f);
        }
    }

    private void sendTouchEvent(int x, int y, int index) {
        android.os.IBinder iBinder = getWindowToken();
        if (iBinder != null) {
            String command = index == 0 ? WallpaperManager.COMMAND_TAP : WallpaperManager.COMMAND_SECONDARY_TAP;
            mWallpaperManager.sendWallpaperCommand(iBinder, command, x, y, 0, null);
        }
    }

    private void sendTouchEvent(View view, MotionEvent event) {
        int pointerCount = event.getPointerCount();
        int viewOffset[] = {0, 0};
        // this will not account for a rotated view
        view.getLocationOnScreen(viewOffset);

        // get index of first finger
        int pointerIndex = event.findPointerIndex(0);
        if (pointerIndex >= 0 && pointerIndex < pointerCount) {
            sendTouchEvent((int) event.getX(pointerIndex) + viewOffset[0], (int) event.getY(pointerIndex) + viewOffset[1], pointerIndex);
        }

        // get index of second finger
        pointerIndex = event.findPointerIndex(1);
        if (pointerIndex >= 0 && pointerIndex < pointerCount) {
            sendTouchEvent((int) event.getX(pointerIndex) + viewOffset[0], (int) event.getY(pointerIndex) + viewOffset[1], pointerIndex);
        }
    }

    class Anim extends Animation {
        float mStartOffset;
        float mDeltaOffset;
        float mVelocity;

        Anim() {
            super();
            setDuration(1000);
        }

        boolean init() {
            mVelocityTracker.computeCurrentVelocity(1000 / 30);
            mVelocity = mVelocityTracker.getXVelocity();
            //Log.d("LWP", "mVelocity=" + String.format(Locale.US, "%.2f", mVelocity));

            mStartOffset = mWallpaperOffset;
            //Log.d("LWP", "mStartOffset=" + String.format(Locale.US, "%.2f", mStartOffset));

            boolean stickToSides = isPreferenceWPStickToSides();
            boolean stickToCenter = isPreferenceWPReturnCenter();
            float expectedPos = -Math.min(Math.max(mVelocity / mWindowSize.x, -.5f), .5f) + mStartOffset;
            //Log.d("LWP", "expectedPos=" + String.format(Locale.US, "%.2f", expectedPos));

            // if we stick only to the center
            float leftStickPercent = -1.f;
            float rightStickPercent = 2.f;

            if (stickToSides && stickToCenter) {
                // if we stick to the left, right and center
                leftStickPercent = .2f;
                rightStickPercent = .8f;
            } else if (stickToSides) {
                // if we stick only to the center
                leftStickPercent = .5f;
                rightStickPercent = .5f;
            }

            if (expectedPos <= leftStickPercent)
                mDeltaOffset = 0.f - mStartOffset;
            else if (expectedPos >= rightStickPercent)
                mDeltaOffset = 1.f - mStartOffset;
            else if (stickToCenter)
                mDeltaOffset = .5f - mStartOffset;
            else
                return false;
            return true;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float fOffset = mStartOffset + mDeltaOffset * interpolatedTime;
            float velocityInterpolator = (float) Math.sqrt(interpolatedTime) * 3.f;
            if (velocityInterpolator < 1.f)
                fOffset -= mVelocity / mWindowSize.x * velocityInterpolator;
            else
                fOffset -= mVelocity / mWindowSize.x * (1.f - 0.5f * (velocityInterpolator - 1.f));
            updateWallpaperOffset(fOffset);
        }
    }
}
