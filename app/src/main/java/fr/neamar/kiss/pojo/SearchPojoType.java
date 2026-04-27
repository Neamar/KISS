package fr.neamar.kiss.pojo;

import androidx.annotation.DrawableRes;

import fr.neamar.kiss.R;

public enum SearchPojoType {
    SEARCH_QUERY(R.drawable.ic_search),
    URL_QUERY(R.drawable.ic_public),
    CALCULATOR_QUERY(R.drawable.ic_functions),
    URI_QUERY(R.drawable.ic_public),
    TIMER_QUERY(R.drawable.ic_timer);

    @DrawableRes
    private final int iconId;

    SearchPojoType(@DrawableRes int iconId) {
        this.iconId = iconId;
    }

    @DrawableRes
    public int getIconId() {
        return iconId;
    }
}
