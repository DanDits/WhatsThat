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

package dan.dit.whatsthat.system;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.riddle.RiddleInitializer;

/**
 * Created by daniel on 26.04.15.
 */
public class InitActivity extends Activity implements InitializationFragment.OnInitClosingCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("HomeStuff", "onCreate of InitActivity.");
        setContentView(R.layout.init_activity);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("HomeStuff", "onStop of InitActivity, cancel all, init running=" + RiddleInitializer.INSTANCE.isInitializing() + " sync running=" + ImageManager.isSyncing());
        RiddleInitializer.INSTANCE.cancelInit();
        ImageManager.cancelSync();
    }

    @Override
    public void onSkipInit() {
        Intent i = new Intent(this, RiddleActivity.class);
        startActivity(i);
    }
}
