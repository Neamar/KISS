package fr.neamar.kiss.result;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.utils.Permission;

public abstract class CallResult extends Result {
    CallResult(@NonNull Pojo pojo) {
        super(pojo);
    }


    @SuppressLint("MissingPermission")
    public void launchCall(Context context, View v, String phone) {
        Intent phoneIntent = new Intent(Intent.ACTION_CALL);
        phoneIntent.setData(Uri.parse("tel:" + Uri.encode(phone)));
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            phoneIntent.setSourceBounds(v.getClipBounds());
        }

        phoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


        // Make sure we have permission to call someone as this is considered a dangerous permission
        if (!Permission.checkPermission(context, Permission.PERMISSION_CALL_PHONE)) {
            Permission.askPermission(Permission.PERMISSION_CALL_PHONE, new Permission.PermissionResultListener() {
                @Override
                public void onGranted() {
                    // Great! Start the intent we stored for later use.
                    context.startActivity(phoneIntent);
                }

                @Override
                public void onDenied() {
                    Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        // Pre-android 23, or we already have permission

        context.startActivity(phoneIntent);
    }
}
