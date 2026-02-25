package fr.neamar.kiss.result;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.PojoWithTags;
import fr.neamar.kiss.utils.SpaceTokenizer;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;

public abstract class ResultWithTags<T extends PojoWithTags> extends Result<T> {

    protected ResultWithTags(@NonNull T pojo) {
        super(pojo);
    }

    @Override
    boolean popupMenuClickHandler(Context context, RecordAdapter parent, @StringRes int stringId, View parentView) {
        if (stringId == R.string.menu_tags_edit) {
            launchEditTagsDialog(context, parent, pojo);
            return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }

    private void launchEditTagsDialog(final Context context, RecordAdapter parent, final T pojo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.tags_add_title));

        // Create the tag dialog
        final View v = View.inflate(context, R.layout.tags_dialog, null);
        final MultiAutoCompleteTextView tagInput = v.findViewById(R.id.tag_input);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, KissApplication.getApplication(context).getDataHandler().getTagsHandler().getAllTagsAsArray());
        tagInput.setTokenizer(new SpaceTokenizer());
        tagInput.setText(pojo.getTags());

        tagInput.setAdapter(adapter);
        builder.setView(v);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            dialog.dismiss();
            // Refresh tags for given app
            pojo.setTags(tagInput.getText().toString());
            KissApplication.getApplication(context).getDataHandler().getTagsHandler().setTags(pojo.id, pojo.getTags());
            // Show toast message
            String msg = context.getResources().getString(R.string.tags_confirmation_added);
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            setTranscriptModeAlwaysScroll(parent);
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            dialog.cancel();
            setTranscriptModeAlwaysScroll(parent);
        });
        setTranscriptModeDisabled(parent);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected void displayTags(Context context, FuzzyScore fuzzyScore, TextView tagsView) {
        // Hide tags view if tags are empty
        if (TextUtils.isEmpty(pojo.getTags())) {
            tagsView.setVisibility(View.GONE);
        } else if (displayHighlighted(pojo.getNormalizedTags(), pojo.getTags(),
                fuzzyScore, tagsView, context) || isTagsVisible(context)) {
            tagsView.setVisibility(View.VISIBLE);
        } else {
            tagsView.setVisibility(View.GONE);
        }

    }
}
