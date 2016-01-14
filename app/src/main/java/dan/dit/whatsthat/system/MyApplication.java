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

import android.app.Application;
import android.content.Intent;

import java.io.PrintWriter;
import java.io.StringWriter;

import dan.dit.whatsthat.preferences.User;

public class MyApplication extends Application {
    private boolean mExceptionHandled = false;
    private boolean mApplicationCreated;

    @Override
    public void onCreate () {
        // Setup handler for uncaught exceptions.
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                handleUncaughtException(thread, e);
            }
        });
        super.onCreate();
        User.makeInstance(this);
        mApplicationCreated = true;
        // 50331648 bytes max memory by default on my phone (48 mb)
        // 134217728 bytes max memory if largeHeap = true set in manifest (128 mb)!!!
    }


    private void handleUncaughtException (Thread thread, Throwable e) {
        // only handle exception if application was successfully created and the exception did not occur during creation
        // else this could result in an endless loop of recreating the application
        if (!mExceptionHandled && mApplicationCreated) {
            mExceptionHandled = true;
            e.printStackTrace(); // not all Android versions will print the stack trace automatically

            Intent intent = new Intent();
            intent.putExtra(SendLog.KEY_CAUSE, convertStacktraceToString(e));
            intent.setAction("dan.dit.whatsthat.SEND_LOG"); // see step 5.
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // required when starting from Application
            startActivity(intent);
        }
        System.exit(1); // kill off the crashed app
    }

    private String convertStacktraceToString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}