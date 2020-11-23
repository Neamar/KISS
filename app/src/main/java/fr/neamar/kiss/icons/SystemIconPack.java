package fr.neamar.kiss.icons;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.List;

import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.UserHandle;

public class SystemIconPack implements IconPack<Void> {

    private static final String TAG = SystemIconPack.class.getSimpleName();
    private final String packageName;

    public SystemIconPack(String packageName) {
        this.packageName = packageName;
    }

    public SystemIconPack() {
        this("default");
    }

    @NonNull
    @Override
    public String getPackPackageName() {
        return packageName;
    }

    @Override
    public void load(PackageManager packageManager) {
    }

    @Nullable
    @Override
    public Drawable getComponentDrawable(String componentName) {
        return null;
    }

    @Nullable
    @Override
    public Drawable getComponentDrawable(@NonNull Context ctx, @NonNull ComponentName componentName, @NonNull UserHandle userHandle) {
        Drawable drawable = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                List<LauncherActivityInfo> icons = launcher.getActivityList(componentName.getPackageName(), userHandle.getRealHandle());
                for (LauncherActivityInfo info : icons) {
                    if (info.getComponentName().equals(componentName)) {
                        drawable = info.getBadgedIcon(0);
                        break;
                    }
                }

                // This should never happen, let's just return the first icon
                if (drawable == null)
                    drawable = icons.get(0).getBadgedIcon(0);
            } else {
                drawable = ctx.getPackageManager().getActivityIcon(componentName);
            }
        } catch (PackageManager.NameNotFoundException | IndexOutOfBoundsException e) {
            Log.e(TAG, "Unable to find component " + componentName.toString() + e);
        }
        return drawable;
    }

    @NonNull
    @Override
    public BitmapDrawable applyBackgroundAndMask(@NonNull Context ctx, @NonNull Drawable icon) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Bitmap outputBitmap;
            Canvas outputCanvas;
            Paint outputPaint;

            if (icon instanceof AdaptiveIconDrawable) {
                AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) icon;

                int layerSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 108f, ctx.getResources().getDisplayMetrics()));
                int iconSize = Math.round(layerSize / (1 + 2 * AdaptiveIconDrawable.getExtraInsetFraction()));
                int layerOffset = (layerSize - iconSize) / 2;

                // Create a bitmap of the icon to use it as the shader of the outputBitmap
                Bitmap iconBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
                Canvas iconCanvas = new Canvas(iconBitmap);

                // Stretch adaptive layers because they are 108dp and the icon size is 48dp
                {
                    Drawable bgDrawable = adaptiveIcon.getBackground();
                    if (bgDrawable != null) {
                        bgDrawable.setBounds(-layerOffset, -layerOffset, iconSize + layerOffset, iconSize + layerOffset);
                        bgDrawable.draw(iconCanvas);
                    }

                    Drawable fgDrawable = adaptiveIcon.getForeground();
                    if (fgDrawable != null) {
                        fgDrawable.setBounds(-layerOffset, -layerOffset, iconSize + layerOffset, iconSize + layerOffset);
                        fgDrawable.draw(iconCanvas);
                    }
                }

                outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
                outputCanvas = new Canvas(outputBitmap);
                outputPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                outputPaint.setShader(new BitmapShader(iconBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

                DrawableUtils.setIconShape(outputCanvas, outputPaint, getPackPackageName());
            }
            // If icon is not adaptive, put it in a white canvas to make it have a unified shape
            else {
                int iconSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, ctx.getResources().getDisplayMetrics()));

                outputBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
                outputCanvas = new Canvas(outputBitmap);
                outputPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                outputPaint.setColor(Color.WHITE);

                // setBounds for LayerDrawable do not scale it properly and it ends up bigger then the white background shape
                if (icon instanceof LayerDrawable) {
                    Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    icon.draw(canvas);
                    icon = new BitmapDrawable(ctx.getResources(), bitmap);
                }

                // Shrink icon to 70% of its size so that it fits the shape
                int topLeftCorner = Math.round(0.15f * iconSize);
                int bottomRightCorner = Math.round(0.85f * iconSize);
                icon.setBounds(topLeftCorner, topLeftCorner, bottomRightCorner, bottomRightCorner);

                DrawableUtils.setIconShape(outputCanvas, outputPaint, getPackPackageName());
                icon.draw(outputCanvas);
            }
            return new BitmapDrawable(ctx.getResources(), outputBitmap);

        }

        if (icon instanceof BitmapDrawable)
            return (BitmapDrawable) icon;

        return new BitmapDrawable(ctx.getResources(), DrawableUtils.drawableToBitmap(icon));
    }

    @Nullable
    @Override
    public Collection<Void> getDrawableList() {
        return null;
    }

    @Nullable
    @Override
    public Drawable getDrawable(@NonNull Void aVoid) {
        return null;
    }
}
