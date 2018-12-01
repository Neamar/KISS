package fr.neamar.kiss.kustomsupport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;

public class AutoFinishTransparentActivity extends Activity {

    public static void start(@Nullable Context context) {
        if (context != null)
            context.startActivity(new Intent(context, AutoFinishTransparentActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }
}