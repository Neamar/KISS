package fr.neamar.kiss.loader;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.DocsPojo;

import static android.provider.MediaStore.MediaColumns.DATA;

public class LoadDocsPojos extends LoadPojos<DocsPojo> {

    public LoadDocsPojos(Context context) {
        super(context, "file://");
    }

    @Override
    protected ArrayList<DocsPojo> doInBackground(Void... params) {
        ArrayList<DocsPojo> docs = new ArrayList<>();

        final String[] DOC_PROJECTION = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Files.FileColumns.TITLE
        };

        String[] selectionArgs = new String[]{".pdf", ".ppt", ".pptx", ".xlsx", ".xls", ".doc", ".docx", ".txt"};
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
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE));
                        if (path != null && contains(selectionArgs, path)) {
                            if (path.lastIndexOf(".") != -1) {
                                switch (path.substring(path.lastIndexOf(".") + 1, path.length())) {
                                    case "pdf":
                                        docs.add(createPojo(title, path, R.drawable.ic_pdf));
                                        break;
                                    case "ppt":
                                        docs.add(createPojo(title, path, R.drawable.ic_ppt));
                                        break;
                                    case "pptx":
                                        docs.add(createPojo(title, path, R.drawable.ic_ppt));
                                        break;
                                    case "xls":
                                        docs.add(createPojo(title, path, R.drawable.ic_xls));
                                        break;
                                    case "xlsx":
                                        docs.add(createPojo(title, path, R.drawable.ic_xls));
                                        break;
                                    case "doc":
                                        docs.add(createPojo(title, path, R.drawable.ic_doc));
                                        break;
                                    case "docx":
                                        docs.add(createPojo(title, path, R.drawable.ic_doc));
                                        break;
                                    case "txt":
                                        docs.add(createPojo(title, path, R.drawable.ic_txt));
                                        break;
                                }


                            }
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


        return docs;
    }

    private DocsPojo createPojo(String name, String path, int resId) {
        DocsPojo pojo = new DocsPojo();
        pojo.id = pojoScheme + path.toLowerCase();
        pojo.name = name;
        pojo.nameNormalized = pojo.name.toLowerCase();
        pojo.docPath = path;
        pojo.icon = resId;

        return pojo;
    }

    boolean contains(String[] types, String path) {
        for (String string : types) {
            if (path.endsWith(string)) return true;
        }
        return false;
    }
}
