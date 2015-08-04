package dan.dit.whatsthat.system;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.spec.X509EncodedKeySpec;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.ImageManager;
import dan.dit.whatsthat.riddle.RiddleInitializer;
import dan.dit.whatsthat.testsubject.SimpleCrypto;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.image.ExternalStorage;

/**
 * Created by daniel on 26.04.15.
 */
public class InitActivity extends Activity implements InitializationFragment.OnInitClosingCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("HomeStuff", "onCreate of InitActivity.");
        //ImageManager.calculateImagedataDeveloper(this);
        setContentView(R.layout.init_activity);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("HomeStuff", "onStop of InitActivity, cancel all, init running=" + RiddleInitializer.INSTANCE.isInitializing() + " sync running=" + ImageManager.isSyncing());
        RiddleInitializer.INSTANCE.cancelInit();
        ImageManager.cancelSync();
    }

    @Override
    public void onSkipInit() {
        Intent i = new Intent(this, RiddleActivity.class);
        startActivity(i);
    }
}
