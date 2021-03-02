package fr.neamar.kiss.customicon;

import androidx.annotation.NonNull;

import java.util.List;

import fr.neamar.kiss.R;
import fr.neamar.kiss.utils.ViewHolderListAdapter;

class IconAdapter extends ViewHolderListAdapter<IconData, IconViewHolder> {

    IconAdapter(@NonNull List<IconData> objects) {
        super(IconViewHolder.class, R.layout.custom_icon_item, objects);
    }
}
