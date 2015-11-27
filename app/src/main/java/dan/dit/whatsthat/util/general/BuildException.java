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
 * An Exception that can be thrown by builder classes that lack information and are unable to build
 * a valid instance. Usually all required information should be requested in the constructor of the
 * Builder.
 * Created by daniel on 28.03.15.
 */
public class BuildException extends Exception {
    private String mBuilderName;
    private String mMissingDataHint;

    public BuildException(String msg) {
        super(msg);
    }

    public BuildException() {
    }

    /**
     * Sets the missing data that caused this builder to fail building the object instance.
     * @param builderName The name of the builder. Example: "class House"
     * @param missingDataHint The hint to what kind of data was missing or wrong. Example : "missing roof, no door".
     * @return This exception.
     */
    public BuildException setMissingData(String builderName, String missingDataHint) {
        mBuilderName = builderName;
        mMissingDataHint = missingDataHint;
        return this;
    }

    @Override
    public String toString() {
        return super.getMessage() + " Builder for " + mBuilderName + " failed: " + mMissingDataHint;
    }
}
