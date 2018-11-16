package fr.neamar.kiss.glide.appicon;

import android.content.ComponentName;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Key;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

import fr.neamar.kiss.utils.UserHandle;

public class AppIconModel {
	public final ComponentName componentName;
	public final UserHandle userHandle;

	public AppIconModel(ComponentName componentName, UserHandle userHandle) {
		this.componentName = componentName;
		this.userHandle = userHandle;
	}
}
