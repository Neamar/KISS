package fr.neamar.kiss.glide.appicon;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import fr.neamar.kiss.IconsHandler;

public class AppIconFetcher implements DataFetcher<Drawable> {

	private final AppIconModel model;
	private final IconsHandler iconsHandler;

	public AppIconFetcher(AppIconModel model, IconsHandler iconsHandler) {
		this.model = model;
		this.iconsHandler = iconsHandler;
	}

	@Override
	public void loadData(@NonNull Priority priority, @NonNull DataFetcher.DataCallback<? super Drawable> callback) {
		callback.onDataReady(iconsHandler.getDrawableIconForPackage(model.componentName, model.userHandle));
	}

	@Override
	public void cleanup() {}

	@Override
	public void cancel() {}

	@NonNull
	@Override
	public Class<Drawable> getDataClass() {
		return Drawable.class;
	}

	@NonNull
	@Override
	public DataSource getDataSource() {
		return DataSource.LOCAL;
	}
}