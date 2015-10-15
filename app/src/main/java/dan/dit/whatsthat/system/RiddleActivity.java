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
import android.view.Window;
import android.view.WindowManager;

import java.util.Collection;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.riddle.UnsolvedRiddlesChooser;
import dan.dit.whatsthat.testsubject.TestSubject;


public class RiddleActivity extends Activity implements UnsolvedRiddlesChooser.Callback, NoPanicDialog.Callback {

    private RiddleFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (RiddleInitializer.INSTANCE.isNotInitialized() || !TestSubject.isInitialized()) {
            // app got killed by android and is trying to reconstruct this activity when not initialized
            Log.d("HomeStuff", "App killed and trying to reconstruct non initialized into RiddleActivity.");
            Intent reInit = new Intent(getApplicationContext(), InitActivity.class);
            reInit.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(reInit);
            finish();
            return;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.riddle_activity);
    }

    @Override
    public void onStart() {
        super.onStart();
        mFragment = (RiddleFragment) getFragmentManager().findFragmentById(R.id.riddle_fragment);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mFragment != null) {
            mFragment.onWindowFocusChange(hasFocus);
        }
    }

    @Override
    public void openUnsolvedRiddle(Collection<Long> toOpen) {
        mFragment.openUnsolvedRiddle(toOpen);
    }

    @Override
    public boolean canSkip() {
        return mFragment != null && mFragment.canSkip();
    }

    @Override
    public void onSkip() {
        mFragment.onSkip();
    }

    @Override
    public void onComplain(Image image) {
        mFragment.onComplain(image);
    }
}
