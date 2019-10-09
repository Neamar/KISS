package fr.neamar.kiss.pojo;

import android.graphics.drawable.Drawable;

import java.util.concurrent.Future;

public class PhonePojo extends Pojo {
    public final String phone;

    public PhonePojo(String id, String phone, Future<Drawable> icon) {
        super(id, icon);

        this.phone = phone;
    }
}
