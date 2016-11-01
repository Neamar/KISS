package fr.neamar.kiss.loader;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.DocsPojo;

import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.MIME_TYPE;
import static android.provider.MediaStore.MediaColumns.TITLE;

public class LoadDocsPojos extends LoadPojos<DocsPojo> {
    private HashMap<String, String> mimeTypeMap;

    public LoadDocsPojos(Context context) {
        super(context, "file://");
    }

    @Override
    protected ArrayList<DocsPojo> doInBackground(Void... params) {
        ArrayList<DocsPojo> docs = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            mimeTypeMap = getMimeTypeMap();

            final String[] DOC_PROJECTION = {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Files.FileColumns.TITLE
            };

            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"),
                        DOC_PROJECTION,
                        null,
                        null,
                        MediaStore.Files.FileColumns.DATE_ADDED + " DESC");

                if (cursor != null) {
                    if (cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            String path = cursor.getString(cursor.getColumnIndexOrThrow(DATA));
                            String title = cursor.getString(cursor.getColumnIndexOrThrow(TITLE));
                            String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MIME_TYPE));
                            if (isKnownMimeType(mimeType)) {
                                docs.add(createPojo(title, path, mimeType, R.drawable.ic_doc));
                            } else {
                                String extension = path.substring(path.lastIndexOf(".") + 1, path.length());//.doc .pdf
                                String extractedMimeType = getMimeType(extension);
                                if (extractedMimeType != null)
                                    docs.add(createPojo(title, path, extractedMimeType, R.drawable.ic_doc));
                            }

                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }

        return docs;
    }

    private String getMimeType(String extension) {
        if (mimeTypeMap != null) {
            for (String key : mimeTypeMap.keySet()) {
                if (key.equals(extension))
                    return mimeTypeMap.get(extension);
            }
        }
        return null;
    }

    private boolean isKnownMimeType(String mimeType) {

        return (mimeTypeMap != null && mimeTypeMap.values().contains(mimeType));

    }

    private HashMap<String, String> getMimeTypeMap() {
        if (context == null)
            return null;
        HashMap<String, String> map = new HashMap<>();
        BufferedReader reader;
        try {

            InputStream is = context.getAssets().open("general_doc_types.txt");

            reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            while (line != null) {
                String[] words = line.split(" ");
                for (String part : words) {
                    if (!part.equals(words[0]))
                        map.put(part, words[0]);
                }
                line = reader.readLine();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return map;
    }

    private DocsPojo createPojo(String name, String path, String mimeType, int resId) {
        DocsPojo pojo = new DocsPojo();
        pojo.id = pojoScheme + path.toLowerCase();
        pojo.name = name;
        pojo.nameNormalized = pojo.name.toLowerCase();
        pojo.docPath = path;
        pojo.mimeType = mimeType;
        pojo.icon = resId;

        return pojo;
    }

}
