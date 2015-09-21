package dan.dit.whatsthat.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import dan.dit.whatsthat.util.image.ExternalStorage;

/**
 * Created by daniel on 03.09.15.
 */
public class User {
    private static final User INSTANCE = new User();
    public static final String TEMP_EXTERNAL_DIRECTORY_NAME = ".temp";
    private static final String PREFERENCES_USER = "dan.dit.whatsthat.user_preferences";
    private static final String KEY_ORIGIN_NAME = "dan.dit.whatsthat.origin_name";
    private static final int MAX_FILE_NAME_LENGTH = 32; // amount of characters
    public static final String PERMISSION_SUPER_USER = "dan.dit.whatsthat.permission_super_user";
    public static final String PERMISSION_BUNDLE_SYNC_ALLOWED = "dan.dit.whatsthat.permission_bundle_sync";

    private SharedPreferences mPreferences;

    public static void makeInstance(Context context) {
        INSTANCE.mPreferences = context.getSharedPreferences(PREFERENCES_USER, Context.MODE_PRIVATE);
    }

    public static User getInstance() {
        if (INSTANCE.mPreferences == null) {
            throw new IllegalStateException("No User initialized.");
        }
        return INSTANCE;
    }

    public void givePermission(String permission) {
        mPreferences.edit().putBoolean(permission, true).apply();
    }

    public void removePermission(String permission) {
        mPreferences.edit().remove(permission).apply();
    }

    public boolean hasPermission(String permission) {
        return mPreferences.getBoolean(permission, false) || mPreferences.getBoolean(PERMISSION_SUPER_USER, false);
    }

    /**
     * We restrict to basic characters since the origin name will be used as file or directory name (_ not allowed)
     */
    private static final char[] ALLOWED_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-.,; ".toCharArray();
    public  boolean saveOriginName(String origin) {
        origin = isValidFileName(origin);
        if (origin == null) {
            return false;
        }
        mPreferences.edit().putString(KEY_ORIGIN_NAME, origin).apply();
        return true;
    }

    public String getOriginName() {
        return mPreferences.getString(KEY_ORIGIN_NAME, null);
    }

    public static String isValidFileName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return null;
        }
        fileName = fileName.trim();
        if (TextUtils.isEmpty(fileName)) {
            return null;
        }
        if (fileName.length() > MAX_FILE_NAME_LENGTH) {
            return null;
        }
        for (int i = 0; i < fileName.length(); i++) {
            char curr = fileName.charAt(i);
            boolean contained = false;
            for (int j = 0; j < ALLOWED_CHARS.length; j++) {
                if (ALLOWED_CHARS[j] == curr) {
                    contained = true;
                }
            }
            if (!contained) {
                return null; // illegal character found
            }
        }
        return fileName;
    }

    public static File getTempDirectory() {
        String tempDirectory = ExternalStorage.getExternalStoragePathIfMounted(TEMP_EXTERNAL_DIRECTORY_NAME);
        if (tempDirectory == null) {
            return null;
        }
        File dir = new File(tempDirectory);
        Log.d("HomeStuff", "getting temp dir: " + dir + " is directory " + dir.isDirectory());
        if (!dir.exists() || !dir.isDirectory()) {
            if (dir.mkdirs() || dir.isDirectory()) {
                try {
                    new File(dir, ".nomedia").createNewFile();
                } catch (IOException ioe) {
                    Log.e("HomeStuff", "Failed creating .nomedia file in temp directory.");
                }
                return dir;
            }
            Log.d("HomeStuff", "MKDIRS FOR TEMP DIRECTORY FAILED");
            return null; // not a directory and failed mkdirs
        }
        return dir;
    }

    public static String extractRelativePathInsideTempDirectory(File pathInTempDirectory) {
        String tempDirectory = ExternalStorage.getExternalStoragePathIfMounted(TEMP_EXTERNAL_DIRECTORY_NAME);
        if (tempDirectory == null) {
            return null;
        }
        String startPath = pathInTempDirectory.getAbsolutePath();
        String result = startPath.replace(tempDirectory + File.separatorChar, "");
        if (result.length() == startPath.length()) {
            return null;
        }
        return result;
    }

    public static File clearTempDirectory() {
        File dir = getTempDirectory();
        if (dir == null) {
            return null;
        }
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                if (!child.getName().equals(".nomedia")) {
                    deleteRecursivly(child);
                }
            }
            return dir;
        } else if (dir.delete() && dir.mkdirs()) {
            Log.e("HomeStuff", "Temp directoy was not a directory, but now it is.");
            return dir;
        }
        return null;
    }

    private static void deleteRecursivly(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursivly(child);
            }
        }
        file.delete();
    }
}
