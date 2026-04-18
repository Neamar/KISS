package fr.neamar.kiss.icons;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.ui.GoogleCalendarIcon;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.IconShape;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.UserHandle;

public class SystemIconPack implements IconPack {

    private final String packageName;
    private final Context ctx;
    private IconShape mAdaptiveShape = IconShape.SHAPE_SYSTEM;

    public SystemIconPack(Context ctx) {
        this.packageName = "default";
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public String getPackPackageName() {
        return packageName;
    }

    @NonNull
    public IconShape getAdaptiveShape() {
        return mAdaptiveShape;
    }

    public void setAdaptiveShape(@NonNull IconShape shape) {
        mAdaptiveShape = shape;
    }

    @Nullable
    @Override
    public Drawable getComponentDrawable(@NonNull Context ctx, @NonNull ComponentName componentName, @NonNull UserHandle userHandle) {
        if (componentName.getPackageName().equals(GoogleCalendarIcon.GOOGLE_CALENDAR)) {
            Drawable drawable = GoogleCalendarIcon.getDrawable(ctx, componentName.getClassName());
            if (drawable != null)
                return drawable;
        }

        return PackageManagerUtils.getActivityIcon(ctx, componentName, userHandle);
    }

    @NonNull
    @Override
    public Drawable applyBackgroundAndMask(@NonNull Context ctx, @NonNull Drawable icon, boolean fitInside, @ColorInt int backgroundColor) {
        return DrawableUtils.applyIconMaskShape(ctx, icon, mAdaptiveShape, fitInside, backgroundColor);
    }

    @Override
    public void loadDrawables(@NonNull Context ctx) {
    }

    @Nullable
    @Override
    public Map<ComponentName, Set<DrawableInfo>> getDrawablesByComponent() {
        Map<ComponentName, Set<DrawableInfo>> drawablesByComponent = new HashMap<>(0);
        List<AppPojo> apps = getDataHandler().getApplications();
        if (apps != null) {
            apps.forEach(app -> {
                if (app.userHandle.isCurrentUser()) {
                    ComponentName componentName = new ComponentName(app.packageName, app.activityName);
                    drawablesByComponent.put(componentName, Collections.singleton(new SystemDrawableInfo(componentName)));
                }
            });
        }

        return Collections.unmodifiableMap(drawablesByComponent);
    }

    @Nullable
    @Override
    public Drawable getDrawable(@NonNull DrawableInfo drawableInfo) {
        return drawableInfo.getDrawable(ctx, ctx.getResources(), getPackPackageName());
    }

    @Override
    public boolean allowForCustomIcons() {
        return false;
    }

    @Override
    public boolean isSystemIconPack() {
        return true;
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public boolean hasMask() {
        return false;
    }

    private DataHandler getDataHandler() {
        return KissApplication.getApplication(ctx).getDataHandler();
    }

    public static class SystemDrawableInfo implements DrawableInfo {

        private final ComponentName componentName;

        public SystemDrawableInfo(ComponentName componentName) {
            this.componentName = componentName;
        }

        @Nullable
        @Override
        public Drawable getDrawable(Context context, @NonNull Resources resources, @NonNull String iconPackPackageName) {
            return PackageManagerUtils.getActivityIcon(context, componentName, UserHandle.OWNER);
        }

        @Nullable
        @Override
        public String getTextForSearch() {
            return componentName.flattenToString();
        }
    }

}
