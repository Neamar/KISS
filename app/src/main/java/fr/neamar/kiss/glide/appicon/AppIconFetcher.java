package fr.neamar.kiss.glide.appicon;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.utils.UserHandle;

public class AppIconFetcher implements DataFetcher<Drawable> {

	private final ComponentName componentName;
	private final UserHandle userHandle;
	private final IconsHandler iconsHandler;

	public AppIconFetcher(AppPojo model, IconsHandler iconsHandler) {
		this.componentName = new ComponentName(model.packageName, model.activityName);
		this.userHandle = model.userHandle;
		this.iconsHandler = iconsHandler;
	}

	@Override
	public void loadData(@NonNull Priority priority, @NonNull DataFetcher.DataCallback<? super Drawable> callback) {
		callback.onDataReady(iconsHandler.getDrawableIconForPackage(componentName, userHandle));
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