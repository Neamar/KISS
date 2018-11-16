package fr.neamar.kiss.glide.appicon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;

public class AppIconModelLoaderFactory implements ModelLoaderFactory<AppIconModel, Drawable> {

	private final IconsHandler iconsHandler;

	public AppIconModelLoaderFactory(@NonNull Context context) {
		this.iconsHandler = KissApplication.getApplication(context).getIconsHandler();;
	}

	@Override
	public ModelLoader<AppIconModel, Drawable> build(MultiModelLoaderFactory unused) {
		return new AppIconModelLoader(iconsHandler);
	}

	@Override
	public void teardown() {
		// Do nothing.
	}
}