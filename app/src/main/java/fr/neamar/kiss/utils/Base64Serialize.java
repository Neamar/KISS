package fr.neamar.kiss.utils;

import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Base64Serialize {
    public static String encode(Object... args) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(args);
            so.flush();
            return new String(Base64.encode(bo.toByteArray(), Base64.NO_WRAP));
        } catch (Exception e) {
            Log.e("Serialize", "encode", e);
        }
        return null;
    }

    public static Object[] decode(String text) {
        try {
            byte b[] = Base64.decode(text.getBytes(), Base64.NO_WRAP);
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            return (Object[])si.readObject();
        } catch (Exception e) {
            Log.e("Serialize", "decode", e);
        }
        return null;
    }
}
