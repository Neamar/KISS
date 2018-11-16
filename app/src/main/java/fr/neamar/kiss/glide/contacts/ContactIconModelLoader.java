package fr.neamar.kiss.glide.contacts;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.pojo.ContactsPojo;

public final class ContactIconModelLoader implements ModelLoader<ContactsPojo, Drawable> {

	private final Context context;

	public ContactIconModelLoader(Context context) {
		this.context = context;
	}

	@Nullable
	@Override
	public LoadData<Drawable> buildLoadData(ContactsPojo model, int width, int height, @NonNull Options options) {
		return new LoadData<>(new ObjectKey(model.icon), new ContactIconFetcher(context, model));
	}

	@Override
	public boolean handles(@NonNull ContactsPojo model) {
		return true;
	}
}