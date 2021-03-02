package fr.neamar.kiss.customicon;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.icons.IconPackXML;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.Utilities;

class IconPackPage extends PageAdapter.Page {
    final ArrayList<IconData> iconDataList = new ArrayList<>();
    final String packageName;
    private ProgressBar mIconLoadingBar;
    private GridView mGridView;
    private TextView mSearch;
    private IconPackXML mIconPack = null;

    IconPackPage(CharSequence name, String packPackageName, View view) {
        super(name, view);
        packageName = packPackageName;
    }

    @Override
    void setupView(@NotNull Context context, @Nullable OnItemClickListener iconClickListener, @Nullable OnItemClickListener iconLongClickListener) {
        mIconLoadingBar = pageView.findViewById(R.id.iconLoadingBar);

        Drawable packIcon = null;
        // set page title
        TextView textView = pageView.findViewById(android.R.id.text1);
        textView.setText(context.getResources().getString(R.string.icon_pack_content_list, packageName));
        try {
            packIcon = context.getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        if (packIcon != null) {
            int size = UISizes.getResultIconSize(context);
            packIcon.setBounds(0, 0, size, size);
            textView.setCompoundDrawables(packIcon, null, null, null);
        }

        // set page icon grid
        mGridView = pageView.findViewById(R.id.iconGrid);
        IconAdapter iconAdapter = new IconAdapter(iconDataList);
        mGridView.setAdapter(iconAdapter);
        if (iconClickListener != null) {
            mGridView.setOnItemClickListener((parent, view, position, id) -> iconClickListener.onItemClick(parent.getAdapter(), view, position));
        }
        if (iconLongClickListener != null) {
            mGridView.setOnItemLongClickListener((parent, view, position, id) -> {
                iconLongClickListener.onItemClick(parent.getAdapter(), view, position);
                return true;
            });
        }
        TBApplication.ui(context).setResultListPref(mGridView);

        // set page search bar
        mSearch = pageView.findViewById(R.id.search);
        mSearch.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                // Auto left-trim text.
                if (s.length() > 0 && s.charAt(0) == ' ')
                    s.delete(0, 1);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSearch.post(() -> refreshList());
            }
        });
        mSearch.requestFocus();


        // show it's loading while we parse the icon pack XML
        mIconLoadingBar.setVisibility(View.VISIBLE);
        mGridView.setVisibility(View.GONE);
    }

    @Override
    void loadData() {
        super.loadData();

        // load the new pack
        final IconPackXML pack = TBApplication.iconPackCache(pageView.getContext()).getIconPack(packageName);
        Utilities.runAsync((t) -> {
            Activity activity = Utilities.getActivity(pageView);
            if (activity != null)
                pack.loadDrawables(activity.getPackageManager());
        }, (t) -> {
            Activity activity = Utilities.getActivity(pageView);
            if (activity != null) {
                mIconPack = pack;
                refreshList();
            } else
                mIconPack = null;
        });
    }

    private void refreshList() {
        iconDataList.clear();
        if (mIconPack != null) {
            Collection<IconPackXML.DrawableInfo> drawables = mIconPack.getDrawableList();
            StringNormalizer.Result normalized = StringNormalizer.normalizeWithResult(mSearch.getText(), true);
            FuzzyScore fuzzyScore = new FuzzyScore(normalized.codePoints);
            for (IconPackXML.DrawableInfo info : drawables) {
                if (fuzzyScore.match(info.getDrawableName()).match)
                    iconDataList.add(new IconData(mIconPack, info));
            }
        }
        mIconLoadingBar.setVisibility(View.GONE);
        boolean showGridAndSearch = !iconDataList.isEmpty() || (mSearch.length() > 0);
        mSearch.setVisibility(showGridAndSearch ? View.VISIBLE : View.GONE);
        mGridView.setVisibility(showGridAndSearch ? View.VISIBLE : View.GONE);
        ((BaseAdapter) mGridView.getAdapter()).notifyDataSetChanged();
    }
}
