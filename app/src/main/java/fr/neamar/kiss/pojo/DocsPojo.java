package fr.neamar.kiss.pojo;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

public class DocsPojo extends Pojo {
    public String docPath;
    public String mimeType;
    public int icon = -1;

    public void doLaunch(Context context){
        File file = new File(this.docPath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), this.mimeType);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        Intent target = Intent.createChooser(intent, "Open File");
        try {
            context.startActivity(target);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }
}
