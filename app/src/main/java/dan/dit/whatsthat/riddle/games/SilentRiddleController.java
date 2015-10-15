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

package dan.dit.whatsthat.riddle.games;

import android.content.Context;
import android.support.annotation.NonNull;

import dan.dit.whatsthat.riddle.Riddle;

/**
 * Created by daniel on 08.05.15.
 */
public class SilentRiddleController extends RiddleController {
    SilentRiddleController(@NonNull RiddleGame riddleGame, @NonNull Riddle riddle) {
        super(riddleGame, riddle);
    }

    protected void onPreRiddleClose() {
        // do nothing
    }

    protected void onRiddleClosed(final Context context) {
        // do nothing
    }

    protected void onRiddleGotVisible() {
        // do nothing
    }
}
