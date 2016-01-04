package dan.dit.whatsthat.util.general;

import android.app.DialogFragment;
import android.content.ClipData;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

/**
 * Created by daniel on 03.01.16.
 */
public class VersionSafe {
    private VersionSafe() {}

    public static @Nullable
    ClipData getClipData(@NonNull Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return data.getClipData();
        }
        return null;
    }

    public static void setImageAlpha(@NonNull ImageView view, int alpha) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setImageAlpha(alpha);
        } else {
            view.setAlpha(alpha);
        }
    }

    public static boolean isDetached(@NonNull DialogFragment fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            return fragment.isDetached();
        } else {
            return !fragment.isResumed();
        }
    }
}
