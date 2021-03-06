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
import android.content.Context;
import android.view.View;
import android.view.Window;

/**
 * Created by daniel on 23.08.15.
 */
public class GlasDialog extends Dialog {
    public GlasDialog(Context context, View view) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(view);
        getWindow().getDecorView().setBackgroundResource(android.R.color.transparent);
    }
}
