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
    public static final String getExternalStoragePathIfMounted(String directoryName) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
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
