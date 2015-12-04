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

import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.R;

/**
 * Created by daniel on 30.09.15.
 */
public class OrDependency extends Dependency {
    private List<Dependency> mCandidates = new ArrayList<>(4);
    public OrDependency(Dependency first, Dependency second) {
        add(first);
        add(second);
    }

    public OrDependency() {}

    public OrDependency add(Dependency dep) {
        if (dep != null) {
            mCandidates.add(dep);
        }
        return this;
    }

    @Override
    public boolean isNotFulfilled() {
        boolean anyFulfilled = false;
        for (Dependency dep : mCandidates) {
            if (!dep.isNotFulfilled()) {
                anyFulfilled = true;
                break;
            }
        }
        return !anyFulfilled;
    }

    @Override
    public CharSequence getName(Resources res) {
        if (mCandidates.size() == 0) {
            return "";
        } else if (mCandidates.size() == 1) {
            return mCandidates.get(0).getName(res);
        } else {
            StringBuilder builder = new StringBuilder();
            boolean separateSymbol = false;
            for (Dependency dep : mCandidates) {
                if (separateSymbol) {
                    builder.append(' ')
                            .append(res.getString(R.string.dependency_or))
                            .append(' ');
                }
                builder.append(dep.getName(res));
                separateSymbol = true;
            }
            return builder.toString();
        }
    }
}
