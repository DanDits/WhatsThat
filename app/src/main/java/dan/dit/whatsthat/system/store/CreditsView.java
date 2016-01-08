/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dan.dit.whatsthat.system.store;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 11.06.15.
 */
public class CreditsView extends WebView implements StoreContainer {

    public CreditsView(Context context, AttributeSet attributes) {
        super(context, attributes);
    }

    public void refresh(FragmentActivity activity, FrameLayout titleBackContainer) {
        getSettings().setDefaultTextEncodingName("utf-8");
        loadData(getContext().getString(R.string.credits_text), "text/html; charset=utf-8", "UTF-8");
        setBackgroundColor(Color.TRANSPARENT);
        requestLayout();
        invalidate();
    }

    @Override
    public void stop(FragmentActivity activity, boolean pausedOnly) {

    }

    @Override
    public View getView() {
        return getRootView();
    }
}
