package dan.dit.whatsthat;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.riddle.PracticalRiddleType;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleManager;
import dan.dit.whatsthat.riddle.RiddleView;
import dan.dit.whatsthat.util.OperationDoneListener;
import dan.dit.whatsthat.util.OperationWaiter;
import dan.dit.whatsthat.util.ui.SystemUiHider;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class HomeActivity extends Activity {
    private Riddle mRiddle;
    private Button mBtnNextRiddle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        testDatabase();
        setContentView(R.layout.activity_home);

        mBtnNextRiddle = (Button) findViewById(R.id.riddle_make_next);
        mBtnNextRiddle.setEnabled(false);
        mBtnNextRiddle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mRiddle != null) {
                    mRiddle.executeSolved(getApplicationContext(), new OperationDoneListener() {
                        @Override
                        public void operationDone() {
                            nextRiddle();
                        }
                    });
                } else {
                    nextRiddle();
                }
            }
        });

    }

    private void nextRiddle() {
        mBtnNextRiddle.setEnabled(false);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        RiddleManager.makeRiddle(getApplicationContext(), PracticalRiddleType.Circle.INSTANCE, displaymetrics, new RiddleManager.RiddleMakerListener() {
            @Override
            public void onProgressUpdate(int progress) {
                Log.d("HomeStuff", "Riddle maker progress: " + progress);
            }

            @Override
            public void onRiddleReady(Riddle riddle) {
                Log.d("HomeStuff", "Riddle ready! " + riddle + " image: " + riddle.getImage());
                RiddleView riddleView = (RiddleView) findViewById(R.id.riddleView);
                Log.d("Riddle", "Init state: " + riddle.getInitializationState());
                mRiddle = riddle;
                if (riddle.getInitializationState() == Riddle.INITIALIZATION_STATE_BITMAP) {
                    riddleView.setRiddle(riddle);
                }
                mBtnNextRiddle.setEnabled(true);
            }

            @Override
            public void onError() {
                Log.d("HomeStuff", "Riddle maker on error.");
                mBtnNextRiddle.setEnabled(true);
            }
        });
    }

    private void testDatabase() {
        Log.d("HomeStuff", "TestDatabase:");
        OperationWaiter waiter = new OperationWaiter(new OperationDoneListener() {

            @Override
            public void operationDone() {
                Log.d("HomeStuff", "Initialization done. Starting a riddle.");
                nextRiddle();
            }
        });
        OperationDoneListener init1 = waiter.makeSubOperation();
        OperationDoneListener init2 = waiter.makeSubOperation();
        ImageManager.init(getApplicationContext(), init1); // loads all images available
        RiddleManager.init(getApplicationContext(), init2); // loads all cores
    }

}
