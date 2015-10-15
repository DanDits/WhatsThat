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

package dan.dit.whatsthat.util.dependencies;

import android.content.res.Resources;

/**
 * Created by daniel on 10.06.15.
 */
public class MinValueDependency extends Dependency{
    private final Dependable mDependency;
    private final int mMinValue;

    public final int getMinValue() {
        return mMinValue;
    }

    public MinValueDependency(Dependable dependency, int minValue) {
        mDependency = dependency;
        mMinValue = minValue;
        if (mDependency == null) {
            throw new IllegalArgumentException("No dependency given.");
        }
    }

    @Override
    public boolean isNotFulfilled() {
        return mDependency.getValue() < mMinValue;
    }

    @Override
    public CharSequence getName(Resources res) {
        return mDependency.getName(res);
    }

    @Override
    public String toString() {
        return "DEP: value (" + mDependency.getValue() + ") >= " + mMinValue;
    }
}
