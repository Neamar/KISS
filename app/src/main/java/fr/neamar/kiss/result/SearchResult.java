package fr.neamar.kiss.result;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.AlarmClock;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.icons.IconPack;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.pojo.SearchPojoType;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.ClipboardUtils;
import fr.neamar.kiss.utils.Log;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;

public class SearchResult extends Result<SearchPojo> {

    private static final String TAG = SearchResult.class.getSimpleName();

    private volatile Drawable icon = null;

    SearchResult(@NonNull SearchPojo pojo) {
        super(pojo);
    }

    @NonNull
    @Override
    public View display(Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null)
            view = inflateFromId(context, R.layout.item_search, parent);

        TextView searchText = view.findViewById(R.id.item_search_text);
        ImageView image = view.findViewById(R.id.item_search_icon);

        boolean hideIcons = isHideIcons(context);
        if (hideIcons) {
            image.setImageDrawable(null);
        } else {
            this.setAsyncDrawable(image);
        }

        String text;
        int pos;
        int len;
        switch (pojo.type) {
            case URL_QUERY:
                text = context.getString(R.string.ui_item_visit, this.pojo.getName());
                pos = text.indexOf(this.pojo.getName());
                len = this.pojo.getName().length();
                break;
            case SEARCH_QUERY:
                text = context.getString(R.string.ui_item_search, this.pojo.getName(), pojo.query);
                pos = text.indexOf(pojo.query);
                len = pojo.query.length();
                break;
            case CALCULATOR_QUERY:
                text = pojo.query;
                pos = text.indexOf("=");
                len = text.length() - pos;
                break;
            case TIMER_QUERY:
                String elapsedTime = DateUtils.formatElapsedTime(parseSeconds(pojo.url));
                text = context.getString(R.string.ui_item_timer, elapsedTime);
                pos = text.indexOf(elapsedTime);
                len = elapsedTime.length();
                break;
            case URI_QUERY:
                text = context.getString(R.string.ui_item_open, this.pojo.query);
                pos = text.indexOf(pojo.query);
                len = pojo.query.length();
                break;
            default:
                throw new IllegalArgumentException("Following type isn't supported: " + pojo.type);
        }

        displayHighlighted(text, Collections.singletonList(new Pair<>(pos, pos + len)), searchText, context);

        return view;
    }

    @Override
    boolean isDrawableCached() {
        return icon != null;
    }

    @Override
    void setDrawableCache(Drawable drawable) {
        icon = drawable;
    }

    @Override
    public Drawable getDrawable(Context context) {
        if (icon == null) {
            synchronized (this) {
                if (icon == null) {
                    Intent intent = getLaunchIntent();
                    icon = getIconByIntent(context, intent);
                    if (icon == null) {
                        icon = getThemedDrawable(context, pojo, pojo.type.getIconId());
                    }
                }
            }
        }
        return icon;
    }

    /**
     * Creates intent to start activity for given uri query.
     *
     * @return intent
     */
    private Intent createUriQueryIntent() {
        Uri uri = Uri.parse(pojo.query);
        return PackageManagerUtils.createUriIntent(uri);
    }

    /**
     * Creates intent to start activity for given search query.
     *
     * @return intent
     */
    private Intent createSearchQueryIntent() {
        if (TextUtils.isEmpty(pojo.url) || "%s".equals(pojo.url)) {
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(SearchManager.QUERY, pojo.query); // query contains search string
            return intent;
        } else {
            String query;
            try {
                query = URLEncoder.encode(pojo.query, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                query = URLEncoder.encode(pojo.query);
            }
            String urlWithQuery = pojo.url.replaceAll("%s|\\{q\\}", query);
            Uri uri = Uri.parse(urlWithQuery);
            return PackageManagerUtils.createUriIntent(uri);
        }
    }

    /**
     * @param context
     * @param intent
     * @return icon, of best matching app for given intent
     */
    @Nullable
    private Drawable getIconByIntent(@NonNull Context context, @Nullable Intent intent) {
        ComponentName componentName = PackageManagerUtils.getComponentName(context, intent);
        if (componentName != null) {
            IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();
            return iconsHandler.getDrawableIconForPackage(PackageManagerUtils.getLaunchingComponent(context, componentName, pojo.getUserHandle()), pojo.getUserHandle());
        }
        return null;
    }

    @Override
    public void doLaunch(Context context, View v) {
        switch (pojo.type) {
            case URL_QUERY:
            case SEARCH_QUERY:
            case TIMER_QUERY:
            case URI_QUERY:
                Intent intent = getLaunchIntent();
                if (intent != null) {
                    setSourceBounds(intent, v);
                    try {
                        context.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.w(TAG, "Unable to launch activity for " + pojo.type + "(query=" + pojo.query + ", url=" + pojo.url + ")");
                    }
                }
                break;
            case CALCULATOR_QUERY:
                ClipboardUtils.setClipboard(context, pojo.query.substring(pojo.query.indexOf("=") + 2));
                Toast.makeText(context, R.string.copy_confirmation, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private Intent getLaunchIntent() {
        switch (pojo.type) {
            case URL_QUERY:
            case SEARCH_QUERY:
                return createSearchQueryIntent();
            case TIMER_QUERY:
                int seconds = parseSeconds(pojo.url);
                Intent timerIntent = new Intent(AlarmClock.ACTION_SET_TIMER);
                timerIntent.putExtra(AlarmClock.EXTRA_LENGTH, seconds);
                timerIntent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                String elapsedTime = DateUtils.formatElapsedTime(seconds);
                timerIntent.putExtra(AlarmClock.EXTRA_MESSAGE, elapsedTime);
                timerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return timerIntent;
            case URI_QUERY:
                return createUriQueryIntent();
            case CALCULATOR_QUERY:
            default:
                return null;
        }
    }

    private int parseSeconds(@NonNull String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    protected void buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter) {
        super.buildPopupMenu(context, adapter);

        adapter.add(new ListPopup.Item(context, R.string.share));
    }

    @Override
    protected boolean popupMenuClickHandler(Context context, RecordAdapter parent, int stringId, View parentView) {
        if (stringId == R.string.share) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, pojo.query);
            shareIntent.setType("text/plain");
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(shareIntent);
            return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }

    @Override
    protected boolean isAllowedAsFavorite() {
        return false;
    }

    @Override
    protected boolean canRemoveFromHistory(Context context) {
        return false;
    }

    @Override
    protected boolean canHaveCustomIcon(Context context, IconPack iconPack) {
        return pojo.type == SearchPojoType.CALCULATOR_QUERY;
    }
}
