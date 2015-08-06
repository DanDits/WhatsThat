package dan.dit.whatsthat.system;

import android.app.Application;
import android.content.Intent;

public class MyApplication extends Application {
    private boolean mExceptionHandled = false;

    @Override
    public void onCreate () {
        // Setup handler for uncaught exceptions.
        Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler() {
          @Override
          public void uncaughtException (Thread thread, Throwable e) {
            handleUncaughtException (thread, e);
          }
        });
        super.onCreate();
    }


    private void handleUncaughtException (Thread thread, Throwable e) {
        if (!mExceptionHandled) {
            mExceptionHandled = true;
            e.printStackTrace(); // not all Android versions will print the stack trace automatically

            Intent intent = new Intent();
            intent.setAction("dan.dit.whatsthat.SEND_LOG"); // see step 5.
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // required when starting from Application
            startActivity(intent);
        }
        System.exit(1); // kill off the crashed app
    }
}