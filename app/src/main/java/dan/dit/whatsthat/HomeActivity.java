package dan.dit.whatsthat;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.intro.InitializationFragment;
import dan.dit.whatsthat.riddle.RiddleManager;
import dan.dit.whatsthat.util.ui.SystemUiHider;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class HomeActivity extends Activity implements InitializationFragment.OnInitClosingCallback {

    private boolean mStateRiddleFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_home);
        getFragmentManager().beginTransaction().add(R.id.home_fragment_container, new InitializationFragment()).commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("HomeStuff", "onDestroy of HomeActivity, cancel all, init running=" + RiddleManager.isInitializing() + " sync running=" + ImageManager.isSyncing());
        RiddleManager.cancelInit();
        ImageManager.cancelSync();
    }

    @Override
    public void onSkipInit() {
        if (!mStateRiddleFragment) {
            mStateRiddleFragment = true;
            getFragmentManager().beginTransaction().replace(R.id.home_fragment_container, new RiddleFragment()).commit();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//to clear: getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
