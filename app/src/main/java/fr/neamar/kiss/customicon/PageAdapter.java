package fr.neamar.kiss.customicon;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;

class PageAdapter extends androidx.viewpager.widget.PagerAdapter implements ViewPager.OnPageChangeListener {

    private ArrayList<Page> pageList = new ArrayList<>(0);
    private int mScrollState = ViewPager.SCROLL_STATE_IDLE;

    void addPage(Page page) {
        pageList.add(page);
    }

    Iterable<Page> getPageIterable() {
        return pageList;
    }

    public void setupPageView(Context context, @Nullable Page.OnItemClickListener iconClickListener, @Nullable Page.OnItemClickListener iconLongClickListener) {
        for (Page page : getPageIterable())
            page.setupView(context, iconClickListener, iconLongClickListener);
    }

    public void loadPageData() {
        for (Page page : getPageIterable())
            page.loadData();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        //Log.d("ISDialog", String.format("onPageScrolled %d %.2f", position, positionOffset));
        if (mScrollState != ViewPager.SCROLL_STATE_SETTLING) {
            Page pageLeft = pageList.get(position);
            if (!pageLeft.bDataLoaded)
                pageLeft.loadData();
            if ((position + 1) < pageList.size()) {
                Page pageRight = pageList.get(position + 1);
                if (!pageRight.bDataLoaded)
                    pageRight.loadData();
            }
        }
    }

    @Override
    public void onPageSelected(int position) {
        //Log.d("ISDialog", String.format("onPageSelected %d", position));
        Page page = pageList.get(position);
        if (!page.bDataLoaded)
            page.loadData();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        //Log.d("ISDialog", String.format("onPageScrollStateChanged %d", state));
        mScrollState = state;
    }

    static abstract class Page {
        final CharSequence pageName;
        final View pageView;
        boolean bDataLoaded = false;

        public interface OnItemClickListener {
            void onItemClick(Adapter adapter, View view, int position);
        }

        Page(CharSequence name, View view) {
            pageName = name;
            pageView = view;
        }

        abstract void setupView(@NonNull Context context, @Nullable OnItemClickListener iconClickListener, @Nullable OnItemClickListener iconLongClickListener);

        void loadData() {
            bDataLoaded = true;
        }
    }

    @Override
    public int getCount() {
        return pageList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        if (!(object instanceof Page))
            throw new IllegalStateException("WTF?");
        return ((Page) object).pageView == view;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return pageList.get(position).pageName;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Page page = pageList.get(position);
        container.addView(page.pageView);
        return page;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (!(object instanceof Page))
            throw new IllegalStateException("WTF?");
        Page page = (Page) object;
        container.removeView(page.pageView);
    }
}
