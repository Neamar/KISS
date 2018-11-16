package fr.neamar.kiss.glide.contacts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.pojo.ContactsPojo;

public class ContactIconModelLoaderFactory implements ModelLoaderFactory<ContactsPojo, Drawable> {

	private final Context context;

	public ContactIconModelLoaderFactory(@NonNull Context context) {
		this.context = context;
	}

	@Override
	public ModelLoader<ContactsPojo, Drawable> build(MultiModelLoaderFactory unused) {
		return new ContactIconModelLoader(context);
	}

	@Override
	public void teardown() {
		// Do nothing.
	}
}