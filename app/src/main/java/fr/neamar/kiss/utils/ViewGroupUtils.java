package fr.neamar.kiss.utils;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class ViewGroupUtils {

    public static List<View> removeAndGetDirectChildren(ViewGroup viewGroup) {
        List<View> result = new ArrayList<>();
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            result.add(viewGroup.getChildAt(i));
        }
        viewGroup.removeAllViews();
        return result;
    }

    public static void addAllViews(ViewGroup viewGroup, List<View> views) {
        for (View view : views) {
            viewGroup.addView(view);
        }
    }

}
