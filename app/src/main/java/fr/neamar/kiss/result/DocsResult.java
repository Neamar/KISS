package fr.neamar.kiss.result;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.DocsPojo;

public class DocsResult extends Result {
    private final DocsPojo docsPojo;


    public DocsResult(DocsPojo docsPojo) {
        super();
        this.pojo = this.docsPojo = docsPojo;
    }

    @SuppressWarnings({"ResourceType", "deprecation"})
    @Override
    public View display(Context context, int position, View v) {

        if (v == null)
            v = inflateFromId(context, R.layout.item_doc);

        String docPrefix = "<small><small>" + context.getString(R.string.docs_prefix) + "</small></small>";

        TextView docName = (TextView) v.findViewById(R.id.item_doc_name);
        docName.setText(TextUtils.concat(Html.fromHtml(docPrefix), enrichText(docsPojo.displayName)));


        ImageView docIcon = (ImageView) v.findViewById(R.id.item_doc_icon);
        docIcon.setImageDrawable(context.getResources().getDrawable(docsPojo.icon));
        docIcon.setColorFilter(getThemeFillColor(context), Mode.SRC_IN);


        return v;
    }

    @Override
    public void doLaunch(Context context, View v) {
        if (v == null) {

            //show toast to inform user what the state is
            Toast.makeText(context, ("MSG: " + this.pojo.displayName), Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(context, "Uh-oh something went wrong!!" + this.pojo.displayName, Toast.LENGTH_SHORT).show();

        }
    }

}
