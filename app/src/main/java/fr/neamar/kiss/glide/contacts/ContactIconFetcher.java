package fr.neamar.kiss.glide.contacts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.pojo.ContactsPojo;

public class ContactIconFetcher implements DataFetcher<Drawable> {

	private final Context context;
	private final ContactsPojo model;

	private InputStream inputStream = null;

	public ContactIconFetcher(Context context, ContactsPojo model) {
		this.context = context;
		this.model = model;
	}

	@Override
	public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Drawable> callback) {
		if (model.icon != null) {
			try {
				inputStream = context.getContentResolver().openInputStream(model.icon);
				callback.onDataReady(Drawable.createFromStream(inputStream, null));
			} catch (FileNotFoundException e) {
				callback.onLoadFailed(e);
			}
		} else {
			callback.onLoadFailed(new NullPointerException());
		}
	}

	@Override
	public void cleanup() {
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException ignored) { }
		}
	}

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