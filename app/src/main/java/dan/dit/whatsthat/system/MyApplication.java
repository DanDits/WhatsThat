package dan.dit.whatsthat.system;

import android.app.Application;
import android.content.Intent;

import dan.dit.whatsthat.testsubject.TestSubject;

public class MyApplication extends Application
{
  @Override
  public void onCreate ()
  {
      super.onCreate();
      initSingletons();

    // Setup handler for uncaught exceptions.
    Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler()
    {
      @Override
      public void uncaughtException (Thread thread, Throwable e)
      {
        handleUncaughtException (thread, e);
      }
    });
  }

    private void initSingletons() {
        TestSubject.loadInstance(this);
    }

    private void handleUncaughtException (Thread thread, Throwable e) {
        e.printStackTrace(); // not all Android versions will print the stack trace automatically

        Intent intent = new Intent ();
        intent.setAction ("com.mydomain.SEND_LOG"); // see step 5.
        intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK); // required when starting from Application
        startActivity (intent);

        System.exit(1); // kill off the crashed app
    }
}