package fr.neamar.kiss.glide.appicon;


import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.pojo.AppPojo;

public final class AppIconModelLoader implements ModelLoader<AppPojo, Drawable> {

	private final IconsHandler iconsHandler;

	public AppIconModelLoader(IconsHandler iconsHandler) {
		this.iconsHandler = iconsHandler;
	}

	@Nullable
	@Override
	public ModelLoader.LoadData<Drawable> buildLoadData(AppPojo model, int width, int height, @NonNull Options options) {
		return new LoadData<>(new ObjectKey(model.getComponentName().hashCode()), new AppIconFetcher(model, iconsHandler));
	}

	@Override
	public boolean handles(@NonNull AppPojo model) {
		return true;
	}
}