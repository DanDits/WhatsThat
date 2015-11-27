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

package dan.dit.whatsthat.util.general;

/**
 * A generic interface for any progress that is expressed in percent
 * from 0% to 100% represented by integers from 0 to 100.
 * Created by daniel on 30.04.15.
 */
public interface PercentProgressListener {
    /**
     * The constant for completion, 100%.
     */
    int PROGRESS_COMPLETE = 100;

    /**
     * Hint that progress was done.
     * @param progress The new total progress in percent.
     */
    void onProgressUpdate(int progress);
}
