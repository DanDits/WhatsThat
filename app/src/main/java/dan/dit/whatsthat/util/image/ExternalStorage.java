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

package dan.dit.whatsthat.util.image;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by daniel on 19.05.15.
 */
public class ExternalStorage {

    private ExternalStorage() {} // helper class

    /**
     * Returns the path for the application's external storage in the environment's
     * external storage directory if this is mounted.
     * @param directoryName The directory to append to the file path or null for the plain path.
     * @return The external storage path for the application (directory name appended if available)
     * or null if the external storage state is not MEDIA_MOUNTED.
     * Path format is : BasePath/WhatsThat/directoryName or BasePath/WhatsThat or null
     */
    public static String getExternalStoragePathIfMounted(String directoryName) {
        if (!isMounted()) {
            Log.e("HomeStuff", "External storage not available, not retrieving path for directory " + directoryName);
            return null;
        }
        return Environment.getExternalStorageDirectory()
                + "/WhatsThat" + ((TextUtils.isEmpty(directoryName)) ? "" : ("/" + directoryName));
    }

    public static boolean isMounted() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
}
