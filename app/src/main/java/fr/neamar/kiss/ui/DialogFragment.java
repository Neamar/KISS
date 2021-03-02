package fr.neamar.kiss.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import fr.neamar.kiss.R;

public abstract class DialogFragment<Output> extends android.app.DialogFragment {
    private OnDismissListener<Output> mOnDismissListener = null;
    private OnConfirmListener<Output> mOnConfirmListener = null;

    @LayoutRes
    protected abstract int layoutRes();

    public interface OnDismissListener<T> {
        void onDismiss(@NonNull DialogFragment<T> dialog);
    }

    public interface OnConfirmListener<T> {
        void onConfirm(@Nullable T output);
    }

    public void setOnDismissListener(OnDismissListener<Output> listener) {
        mOnDismissListener = listener;
    }

    public void setOnConfirmListener(OnConfirmListener<Output> listener) {
        mOnConfirmListener = listener;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (mOnDismissListener != null)
            mOnDismissListener.onDismiss(this);
        super.onDismiss(dialog);
    }

    public void onConfirm(@Nullable Output output) {
        if (mOnConfirmListener != null)
            mOnConfirmListener.onConfirm(output);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int style = UITheme.getDialogTheme(requireContext());
        if (style == UITheme.ID_NULL)
            style = R.style.NoTitleDialogTheme;
        setStyle(DialogFragment.STYLE_NO_FRAME, style);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context themeWrapper = UITheme.getDialogThemedContext(requireContext());
        TypedValue outValue = new TypedValue();
        themeWrapper.getTheme().resolveAttribute(R.attr.alertDialogTheme, outValue, true);
        int dialogStyle = outValue.resourceId;
        return new Dialog(themeWrapper, dialogStyle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Dialog dialog = requireDialog();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setDimAmount(0.7f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dialog.setCanceledOnTouchOutside(true);

        View view = inflater.inflate(layoutRes(), container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setClipToOutline(true);
        }

        return view;
    }
}
