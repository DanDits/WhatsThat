/*
 * Copyright 2015 Daniel Dittmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package dan.dit.whatsthat.system;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import dan.dit.whatsthat.BuildConfig;
import dan.dit.whatsthat.R;
import dan.dit.whatsthat.testsubject.TestSubject;
import dan.dit.whatsthat.util.image.ExternalStorage;

/**
 * Emergency activity when there was an uncaught exception that shut down the app.
 * Enables the user to send me an email containing an attached log extract.
 * Source:
 * http://stackoverflow.com/questions/19897628/need-to-handle-uncaught-exception-and-send-log-file
 */
public class SendLog extends Activity implements View.OnClickListener {

    private static final String LOGS_DIRECTORY_NAME = "logs";
    public static final String KEY_CAUSE = "dan.dit.whatsthat.CRASH_CAUSE";
    private String mCause;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature (Window.FEATURE_NO_TITLE); // make a dialog without a titlebar
        setFinishOnTouchOutside (false); // prevent users from dismissing the dialog by tapping outside
        setContentView (R.layout.send_log);
        findViewById(R.id.send).setOnClickListener(this);
        findViewById(R.id.cancel).setOnClickListener(this);

        Intent intent = getIntent();
        if (intent != null) {
            Bundle data = intent.getExtras();
            if (data != null) {
                mCause = data.getString(KEY_CAUSE, "");
            }
        }
    }

    @Override
    public void onClick (View v) {
        if (v.getId() == R.id.send) {
            sendLogFile();
        } else {
            finish();
        }
    }

    private void sendLogFile () {
        try {
            String fullName = extractLogToFile();
            if (fullName == null) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("plain/text");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{TestSubject.EMAIL_ON_ERROR});
            intent.putExtra(Intent.EXTRA_SUBJECT, "WhatsThat log file");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + fullName));
            intent.putExtra(Intent.EXTRA_TEXT, "Log file attached."); // do this so some email clients don't complain about empty body.
            startActivity(intent);
        } catch (Exception e) {
            // something even worse happened, just finish
        } finally {
            finish(); // no more worries, you are out
        }
    }

    private String extractLogToFile() {
        PackageManager manager = this.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo (this.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e2) {
            // we got no package name, whatever
        }
        String model = Build.MODEL;
        if (!model.startsWith(Build.MANUFACTURER))
            model = Build.MANUFACTURER + " " + model;

        // Make file name - file must be saved to external storage or it wont be readable by
        // the email app.
        String path = ExternalStorage.getExternalStoragePathIfMounted(LOGS_DIRECTORY_NAME);
        if (path == null) {
            return null;
        }
        File dir = new File(path);
        if (!dir.mkdirs() && !dir.isDirectory()) {
            // not created and not a directory
            return null;
        }
        String fullName = path + "/error";

        // Extract to file.
        File file = new File (fullName);

        InputStreamReader reader = null;
        FileWriter writer = null;
        try
        {
            // For Android 4.0 and earlier, you will get all app's log output, so filter it to
            // mostly limit it to your app's output.  In later versions, the filtering isn't needed.
            final String cmd = (Build.VERSION.SDK_INT <= Build.VERSION_CODES
                    .ICE_CREAM_SANDWICH_MR1) ?
                    "logcat -d -v time WhatsThat:v dalvikvm:v System.err:v *:s" :
                    "logcat -d -v time";

            // write output stream
            writer = new FileWriter (file);
            writer.write ("Android version: " +  Build.VERSION.SDK_INT + "\n");
            writer.write ("Device: " + model + "\n");
            writer.write ("App version: " + (info == null ? "(null)" : info.versionCode) + "\n");
            writer.write("Cause: " + (mCause == null ? "(null)" : mCause));

            if (BuildConfig.DEBUG) {
                // get input stream
                try {
                    Process process = Runtime.getRuntime().exec(cmd);
                    reader = new InputStreamReader(process.getInputStream());


                    char[] buffer = new char[10000];
                    do {
                        int n = reader.read(buffer, 0, buffer.length);
                        if (n == -1)
                            break;
                        writer.write(buffer, 0, n);
                    } while (true);

                    reader.close();
                } catch (Exception e) {
                    Log.e("HomeStuff", "Critical, exception during getting error log in debug " +
                            "config: " + e);
                }
            }

            writer.close();
        }
        catch (IOException e)
        {
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e1) {
                    // failed closing after exception, ignore
                }
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e1) {
                    // failed closing after exception, ignore
                }

            Log.e("HomeStuff", "Failed failing. Literally .. could not write log file: " + e);
            return null;
        }

        return fullName;
    }
}