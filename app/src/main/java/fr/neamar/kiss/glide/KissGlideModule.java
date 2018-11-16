package fr.neamar.kiss.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import fr.neamar.kiss.glide.appicon.AppIconModel;
import fr.neamar.kiss.glide.appicon.AppIconModelLoaderFactory;
import fr.neamar.kiss.glide.contacts.ContactIconModelLoaderFactory;
import fr.neamar.kiss.pojo.ContactsPojo;

@GlideModule
public final class KissGlideModule extends AppGlideModule {
	@Override
	public void registerComponents(@NonNull Context context, @NonNull Glide glide, Registry registry) {
		registry.prepend(AppIconModel.class, Drawable.class, new AppIconModelLoaderFactory(context));
		registry.prepend(ContactsPojo.class, Drawable.class, new ContactIconModelLoaderFactory(context));
	}
}