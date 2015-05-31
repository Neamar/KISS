package fr.neamar.kiss.ui;

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.QuickContactBadge;

/**
 * A {@link QuickContactBadge} that allows setting a click listener. The base
 * class may use {@link View#setOnClickListener} internally, so this class adds
 * a separate click listener field.
 */
public class ImprovedQuickContactBadge extends RoundedQuickContactBadge {

    private View.OnClickListener mExtraOnClickListener;

    public ImprovedQuickContactBadge(Context context) {
        super(context);
    }

    public ImprovedQuickContactBadge(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImprovedQuickContactBadge(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        if (mExtraOnClickListener != null) {
            mExtraOnClickListener.onClick(v);
        }
    }

    public void setExtraOnClickListener(View.OnClickListener extraOnClickListener) {
        mExtraOnClickListener = extraOnClickListener;
    }

}
