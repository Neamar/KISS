package fr.neamar.kiss;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.icons.IconPackXML;
import fr.neamar.kiss.icons.SystemIconPack;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.TrimmingTextChangedListener;
import fr.neamar.kiss.utils.UserHandle;
import fr.neamar.kiss.utils.Utilities;
import fr.neamar.kiss.utils.fuzzy.FuzzyFactory;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;

public class CustomIconDialog extends DialogFragment {
    private final List<IconData> mIconData = new ArrayList<>();
    private Drawable mSelectedDrawable = null;
    private RecyclerView mIconView;
    private TextView mSearch;
    private ImageView mPreview;
    private OnDismissListener mOnDismissListener = null;
    private OnConfirmListener mOnConfirmListener = null;
    private Utilities.AsyncRun<Void> mLoadIconsPackTask = null;

    public interface OnDismissListener {
        void onDismiss(@NonNull CustomIconDialog dialog);
    }

    public interface OnConfirmListener {
        void onConfirm(@Nullable Drawable icon);
    }

    public void setOnDismissListener(OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    public void setOnConfirmListener(OnConfirmListener listener) {
        mOnConfirmListener = listener;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        cancelLoadIconsPackTask();
        if (mOnDismissListener != null)
            mOnDismissListener.onDismiss(this);
        super.onDismiss(dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.custom_icon_dialog, container, false);
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        WindowManager.LayoutParams lp = getDialog().getWindow().getAttributes();
        lp.dimAmount = 0.7f;
        getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getDialog().setCanceledOnTouchOutside(true);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = getDialog().getContext();

        view.setClipToOutline(true);

        mIconView = view.findViewById(R.id.iconView);
        GridLayoutManager layoutManager = new GridLayoutManager(context, 4);
        IconAdapter iconAdapter = new IconAdapter(mIconData);
        mIconView.setLayoutManager(layoutManager);
        mIconView.setAdapter(iconAdapter);

        iconAdapter.setOnItemClickListener((iconData) -> {
            mSelectedDrawable = iconData.getIcon();
            mPreview.setImageDrawable(mSelectedDrawable);
        });

        mSearch = view.findViewById(R.id.search);
        mSearch.addTextChangedListener(new TrimmingTextChangedListener(false, changedText -> mSearch.post(this::refreshList)));

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        @Nullable
        ComponentName cn = ComponentName.unflattenFromString(args.getString("className", ""));
        UserHandle userHandle = args.getParcelable("userHandle");
        String name = args.getString("componentName", "");
        long customIcon = args.getLong("customIcon", 0);

        IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();

        // Preview
        {
            mPreview = view.findViewById(R.id.preview);
            Drawable drawable = customIcon != 0 ? iconsHandler.getCustomIcon(name, customIcon) : null;
            if (drawable == null)
                drawable = iconsHandler.getDrawableIconForPackage(cn, userHandle, false, false);
            mPreview.setImageDrawable(drawable);
        }

        // OK button
        {
            View button = view.findViewById(android.R.id.button1);
            button.setOnClickListener(v -> {
                cancelLoadIconsPackTask();
                if (mOnConfirmListener != null) {
                    mOnConfirmListener.onConfirm(mSelectedDrawable);
                }
                dismiss();
            });
        }

        // CANCEL button
        {
            View button = view.findViewById(android.R.id.button2);
            button.setOnClickListener(v -> dismiss());
        }

        ViewGroup quickList = view.findViewById(R.id.quickList);

        // add default icon
        {
            Drawable drawable = iconsHandler.getDrawableIconForPackage(cn, userHandle, false, false);

            ImageView icon = quickList.findViewById(android.R.id.icon);
            icon.setImageDrawable(drawable);
            icon.setOnClickListener(v -> {
                mSelectedDrawable = null;
                mPreview.setImageDrawable(((ImageView) v).getDrawable());
            });
            ((TextView) quickList.findViewById(android.R.id.text1)).setText(R.string.default_icon);
        }

        IconPackXML iconPack = iconsHandler.getCustomIconPack();
        if (iconPack != null) {
            cancelLoadIconsPackTask();
            mLoadIconsPackTask = Utilities.runAsync((task) -> {
                if (!task.isCancelled() && task == mLoadIconsPackTask) {
                    iconPack.loadDrawables(context.getPackageManager());
                }
                return null;
            }, (task, result) -> {
                if (!task.isCancelled() && task == mLoadIconsPackTask) {
                    Activity activity = Utilities.getActivity(context);
                    if (activity != null)
                        refreshList();
                }
            });
        }

        SystemIconPack systemPack = iconsHandler.getSystemIconPack();

        Set<Bitmap> dSet = new HashSet<>(6);

        // add getActivityIcon(componentName)
        {
            Drawable drawable = null;
            try {
                drawable = context.getPackageManager().getActivityIcon(cn);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (drawable != null && checkDuplicateDrawable(dSet, drawable)) {
                addQuickOption(R.string.custom_icon_activity, drawable, quickList);
                if (iconPack != null && iconPack.hasMask())
                    addQuickOption(R.string.custom_icon_activity_with_pack, iconPack.applyBackgroundAndMask(context, drawable, true, Color.WHITE), quickList);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    addQuickOption(R.string.custom_icon_activity_adaptive, systemPack.applyBackgroundAndMask(context, drawable, true, Color.WHITE), quickList);
                if (!DrawableUtils.isAdaptiveIconDrawable(drawable))
                    addQuickOption(R.string.custom_icon_activity_adaptive_fill, systemPack.applyBackgroundAndMask(context, drawable, false, Color.TRANSPARENT), quickList);
            }
        }

        // add getApplicationIcon(packageName)
        {
            Drawable drawable = null;
            try {
                drawable = context.getPackageManager().getApplicationIcon(cn.getPackageName());
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (drawable != null && checkDuplicateDrawable(dSet, drawable)) {
                addQuickOption(R.string.custom_icon_application, drawable, quickList);
                if (iconPack != null && iconPack.hasMask())
                    addQuickOption(R.string.custom_icon_application_with_pack, iconPack.applyBackgroundAndMask(context, drawable, true, Color.WHITE), quickList);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    addQuickOption(R.string.custom_icon_application_adaptive, systemPack.applyBackgroundAndMask(context, drawable, true, Color.WHITE), quickList);
                if (!DrawableUtils.isAdaptiveIconDrawable(drawable))
                    addQuickOption(R.string.custom_icon_application_adaptive_fill, systemPack.applyBackgroundAndMask(context, drawable, false, Color.TRANSPARENT), quickList);
            }
        }

        // add Activity BadgedIcon
        LauncherApps launcher = ContextCompat.getSystemService(context, LauncherApps.class);
        List<LauncherActivityInfo> icons = launcher.getActivityList(cn.getPackageName(), userHandle.getRealHandle());
        for (LauncherActivityInfo info : icons) {
            Drawable drawable = info.getBadgedIcon(0);
            if (drawable != null && checkDuplicateDrawable(dSet, drawable)) {
                addQuickOption(R.string.custom_icon_badged, drawable, quickList);
                if (iconPack != null && iconPack.hasMask())
                    addQuickOption(R.string.custom_icon_badged_with_pack, iconPack.applyBackgroundAndMask(context, drawable, true, Color.WHITE), quickList);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    addQuickOption(R.string.custom_icon_badged_adaptive, systemPack.applyBackgroundAndMask(context, drawable, true, Color.WHITE), quickList);
                if (!DrawableUtils.isAdaptiveIconDrawable(drawable))
                    addQuickOption(R.string.custom_icon_badged_adaptive_fill, systemPack.applyBackgroundAndMask(context, drawable, false, Color.TRANSPARENT), quickList);
            }
        }
    }

    private boolean checkDuplicateDrawable(Set<Bitmap> set, Drawable drawable) {
        Bitmap b = null;
        if (drawable instanceof BitmapDrawable)
            b = ((BitmapDrawable) drawable).getBitmap();

        if (set.contains(b))
            return false;

        set.add(b);
        return true;
    }

    private void addQuickOption(@StringRes int textId, Drawable drawable, ViewGroup parent) {
        if (!(drawable instanceof BitmapDrawable))
            return;

        ViewGroup layout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_icon_quick, parent, false);
        ImageView icon = layout.findViewById(android.R.id.icon);
        TextView text = layout.findViewById(android.R.id.text1);

        icon.setImageDrawable(drawable);
        icon.setOnClickListener(v -> {
            mSelectedDrawable = ((ImageView) v).getDrawable();
            mPreview.setImageDrawable(mSelectedDrawable);
        });

        text.setText(textId);

        parent.addView(layout);
    }

    protected void refreshList() {
        mIconData.clear();
        IconsHandler iconsHandler = KissApplication.getApplication(requireContext()).getIconsHandler();
        IconPackXML iconPack = iconsHandler.getCustomIconPack();
        if (iconPack != null) {
            Collection<IconPackXML.DrawableInfo> drawables = iconPack.getDrawableList();
            if (drawables != null) {
                CharSequence searchText = mSearch.getText();
                StringNormalizer.Result normalized = StringNormalizer.normalizeWithResult(searchText, true);
                FuzzyScore fuzzyScore = FuzzyFactory.createFuzzyScore(requireContext(), normalized.codePoints);
                for (IconPackXML.DrawableInfo info : drawables) {
                    if (TextUtils.isEmpty(searchText) || fuzzyScore.match(info.getDrawableName()).match)
                        mIconData.add(new IconData(iconPack, info));
                }
            }
        }
        mIconData.sort(Comparator.comparing(iconData -> iconData.drawableInfo.getDrawableName()));
        mIconView.getAdapter().notifyDataSetChanged();
        mSearch.setVisibility(mIconData.isEmpty() ? View.GONE : View.VISIBLE);
        mIconView.setVisibility(mIconData.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private static class IconData {
        final IconPackXML.DrawableInfo drawableInfo;
        final IconPackXML iconPack;

        IconData(IconPackXML iconPack, IconPackXML.DrawableInfo drawableInfo) {
            this.iconPack = iconPack;
            this.drawableInfo = drawableInfo;
        }

        Drawable getIcon() {
            return iconPack.getDrawable(drawableInfo);
        }
    }

    private static class IconAdapter extends RecyclerView.Adapter<IconAdapter.ViewHolder> {
        private final List<IconData> mIcons;
        private OnItemClickListener mOnItemClickListener = null;

        public interface OnItemClickListener {
            void onItemClick(IconData iconData);
        }

        IconAdapter(@NonNull List<IconData> objects) {
            mIcons = objects;
        }

        void setOnItemClickListener(OnItemClickListener listener) {
            mOnItemClickListener = listener;
        }

        private IconData getItem(int position) {
            return mIcons.get(position);
        }


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_icon_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            IconData content = getItem(position);
            holder.setContent(content);

            holder.icon.setOnClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(content);
                }
            });
            holder.icon.setOnLongClickListener(v -> {
                displayToast(v, content.drawableInfo.getDrawableName());
                return true;
            });
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public int getItemCount() {
            return mIcons.size();
        }

        /**
         * @param v       is the Button view that you want the Toast to appear above
         * @param message is the string for the message
         */

        private void displayToast(View v, CharSequence message) {
            int xOffset = 0;
            int yOffset = 0;
            Rect gvr = new Rect();

            if (v.getGlobalVisibleRect(gvr)) {
                View root = v.getRootView();

                int halfWidth = root.getRight() / 2;
                int halfHeight = root.getBottom() / 2;

                int parentCenterX = (gvr.width() / 2) + gvr.left;

                int parentCenterY = (gvr.height() / 2) + gvr.top;

                if (parentCenterY <= halfHeight) {
                    yOffset = -(halfHeight - parentCenterY);
                } else {
                    yOffset = (parentCenterY - halfHeight);
                }

                if (parentCenterX < halfWidth) {
                    xOffset = -(halfWidth - parentCenterX);
                }

                if (parentCenterX >= halfWidth) {
                    xOffset = parentCenterX - halfWidth;
                }
            }

            Toast toast = Toast.makeText(v.getContext(), message, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, xOffset, yOffset + v.getHeight());
            toast.show();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            protected final ImageView icon;
            protected Utilities.AsyncRun<Drawable> loader = null;

            ViewHolder(View itemView) {
                super(itemView);
                itemView.setTag(this);
                icon = itemView.findViewById(android.R.id.icon);
            }

            public void setContent(IconData content) {
                if (loader != null) {
                    loader.cancel(true);
                    loader = null;
                }

                this.icon.setImageDrawable(null);
                loader = Utilities.runAsync((task) -> {
                    if (task.isCancelled()) {
                        return null;
                    }
                    return content.getIcon();
                }, (task, drawable) -> {
                    if (!task.isCancelled() && task == loader) {
                        icon.setImageDrawable(drawable);
                    }
                });
            }
        }
    }

    /**
     * Cancel running {@link CustomIconDialog#mLoadIconsPackTask} and set to null.
     */
    private void cancelLoadIconsPackTask() {
        if (mLoadIconsPackTask != null) {
            mLoadIconsPackTask.cancel();
            mLoadIconsPackTask = null;
        }
    }

}
