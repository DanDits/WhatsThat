package dan.dit.whatsthat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.riddle.PracticalRiddleType;
import dan.dit.whatsthat.riddle.Riddle;
import dan.dit.whatsthat.riddle.RiddleManager;
import dan.dit.whatsthat.riddle.RiddleView;
import dan.dit.whatsthat.util.OperationDoneListener;
import dan.dit.whatsthat.util.OperationWaiter;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ImageUtil;
import dan.dit.whatsthat.util.ui.SystemUiHider;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class HomeActivity extends Activity {
    private RiddleView mRiddleView;
    private Button mBtnNextRiddle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_home);
        testDatabase();
        //testImageStuff();
        mRiddleView = (RiddleView) findViewById(R.id.riddleView);

        mBtnNextRiddle = (Button) findViewById(R.id.riddle_make_next);
        mBtnNextRiddle.setEnabled(false);
        mBtnNextRiddle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mRiddleView.hasController()) {
                    mRiddleView.removeController();
                }
                nextRiddle();
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
                mRiddleView.setController(riddle.getController());
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

    private void testImageStuff() {
        ImageView img1 = (ImageView) findViewById(R.id.dummyImage1);
        ImageView img2 = (ImageView) findViewById(R.id.dummyImage2);

        Bitmap bmp = ImageUtil.loadBitmap(getResources(), R.drawable.rhino4, 0, 0);
        //img1.setImageBitmap(bmp);
        Bitmap bmp2 = BitmapUtil.improveContrast(bmp);
        img2.setImageBitmap(bmp2);
    }
}
