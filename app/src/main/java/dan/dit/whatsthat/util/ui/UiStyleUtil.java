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

package dan.dit.whatsthat.util.ui;

import android.app.Dialog;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

/**
 * Created by daniel on 13.08.15.
 */
public class UiStyleUtil {
    private UiStyleUtil() {}

    public static void setDialogDividerColor(Dialog dialog, Resources res, int color) {
        if (dialog == null || res == null) {
            Log.e("HomeStuff", "No title divider set. No dialog or resources given.");
            return;
        }
        int titleDividerId = res.getIdentifier("titleDivider", "id", "android");
        if (titleDividerId == 0) {
            Log.e("HomeStuff", "No title divider id found for setting dialog divider color.");
            return;
        }
        View titleDivider = dialog.findViewById(titleDividerId);
        if (titleDivider == null) {
            Log.e("HomeStuff", "No title divider view found for setting dialog divider color.");
            return;
        }
        titleDivider.setBackgroundColor(color);
    }

    public static void setTabHostSelector(TabHost host, int selector) {
        if (host == null) {
            return;
        }
        TabWidget widget = host.getTabWidget();
        if (widget == null) {
            return;
        }
        for(int i = 0; i < widget.getChildCount(); i++) {
            View v = widget.getChildAt(i);
            if (v == null) {
                continue;
            }
            TextView tv = (TextView) v.findViewById(android.R.id.title);
            if (tv == null) {
                continue;
            }
            v.setBackgroundResource(selector);
        }
    }
}
