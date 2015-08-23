package dan.dit.whatsthat.util.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;

/**
 * Created by daniel on 23.08.15.
 */
public class GlasDialog extends Dialog {
    public GlasDialog(Context context, View view) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(view);
        getWindow().getDecorView().setBackgroundResource(android.R.color.transparent);
    }
}
