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
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.ImageObfuscator;
import dan.dit.whatsthat.preferences.User;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * Created by daniel on 13.10.15.
 */
public class ReceiveObfuscatedActivity extends Activity {
    private File mDataFile;
    private InputStream mDataStream;
    private String mName;
    private Button mAccept;
    private Bitmap mObfuscated;
    private AsyncTask<Void, Integer, Object> mPrepareTask;
    private Button mRefuse;
    private ProgressBar mProgressWorking;
    private View mFailExplanation;
    private URL mDataDownloadLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("HomeStuff", "Receiveobfuscated activity created.");
        if (getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            Uri uri = getIntent().getData();
            if (uri != null) {
                attemptExtractFromUri(uri);
            } else {
                ClipData data = getIntent().getClipData();
                if (data != null && data.getItemCount() > 0) {
                    ClipData.Item item = data.getItemAt(0);
                    attemptExtractFromUri(item.getUri());
                }
            }
        }

        boolean hasValidData = (mDataFile != null && mDataFile.exists()) || mDataStream != null
                || mDataDownloadLink != null;
        super.onCreate(savedInstanceState);
        if (hasValidData) {
            setContentView(R.layout.receive_obfuscated_image);
            mFailExplanation = findViewById(R.id.obfuscated_fail_explanation);
            mFailExplanation.setVisibility(View.GONE);
            mRefuse = (Button) findViewById(R.id.obfuscated_refuse);
            mRefuse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            mProgressWorking = (ProgressBar) findViewById(R.id.obfuscated_progress);
            mAccept = (Button) findViewById(R.id.obfuscated_accept);
            mAccept.setEnabled(false);
            mAccept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mProgressWorking.setVisibility(View.VISIBLE);
                    mAccept.setEnabled(false);
                    mPrepareTask = new AsyncTask<Void, Integer, Object>() {
                        @Override
                        protected Object doInBackground(Void... params) {
                            int result = ImageObfuscator.registerObfuscated(getApplicationContext(), mObfuscated, mName);

                            Log.d("HomeStuff", "Result for registering obfuscation: " + result);
                            return result;
                        }

                        @Override
                        public void onCancelled() {
                            mProgressWorking.setVisibility(View.GONE);
                        }

                        @Override
                        public void onPostExecute(Object result) {
                            Integer resultValue = result != null ? (Integer) result : null;
                            if (resultValue != null && resultValue == ImageObfuscator.RESULT_REGISTRATION_SUCCESS_NO_RIDDLE) {
                                Toast.makeText(getApplicationContext(), R.string.obfuscated_loading_success_no_riddle, Toast.LENGTH_LONG).show();
                            } else if (resultValue != null && resultValue == ImageObfuscator.RESULT_REGISTRATION_SUCCESS_WITH_RIDDLE) {
                                Toast.makeText(getApplicationContext(), R.string.obfuscated_loading_success_with_riddle, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getApplicationContext(), R.string.obfuscated_loading_failed, Toast.LENGTH_SHORT).show();
                            }

                            mProgressWorking.setVisibility(View.GONE);
                            finish();
                            if (result != null) {
                                Intent intent = new Intent(getApplicationContext(), RiddleActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                            }
                        }
                    }.execute();
                }
            });

            prepareData();
        } else {
            Log.d("HomeStuff", "No valid data found in intent " + getIntent());
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void attemptExtractFromUri(Uri uri) {
        Log.d("HomeStuff", "Found uri for obfuscated activity: " + uri);
        if (uri.getScheme().equals("file")) {
            String path = uri.getPath();
            if (path != null) {
                mDataFile = new File(path);
                mName = mDataFile.getName();
            }
        } else if (uri.getScheme().equals("content")) {
            try {
                mDataStream = getContentResolver().openInputStream(uri);
                mName = uri.getHost();
                if (mName == null) {
                    mName = uri.getLastPathSegment();
                }
                if (mName == null) {
                    mName = uri.getUserInfo();
                }
            } catch (FileNotFoundException fnf) {
                //bad luck
            }

        } else if (uri.getScheme().equals("https")) {
            // expected uri: https://experiment.whatsthat/photohash/photoname
            List<String> segments = uri.getPathSegments();
            Log.d("HomeStuff", "Got https scheme: " + uri + " with segments: " + segments);
            if (segments.size() > 1) {
                mName = segments.get(segments.size() - 1);
                try {
                    mDataDownloadLink = new URL(User.getInstance().getWebPhotoStorage()
                            .makeDownloadLink
                                    (segments.get(segments.size() - 2)
                                            + "/"
                                            + segments.get(segments.size() - 1)));
                } catch (MalformedURLException e) {
                    Log.e("HomeStuff", "Illegal uri: " + e);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPrepareTask != null) {
            mPrepareTask.cancel(true);
        }
    }

    private void prepareData() {
        mProgressWorking.setVisibility(View.VISIBLE);
        mPrepareTask = new AsyncTask<Void, Integer, Object>() {

            @Override
            protected Object doInBackground(Void... params) {
                //load bitmap and check if it is a valid obfuscated image
                if (mDataFile != null) {
                    mObfuscated = ImageUtil.loadBitmap(mDataFile, 0, 0, true);
                } else if (mDataStream != null) {
                    mObfuscated = ImageUtil.loadBitmap(mDataStream, 0, 0, BitmapUtil.MODE_FIT_EXACT);
                } else if (mDataDownloadLink != null) {
                    File tempFile = User.getInstance().getWebPhotoStorage().download
                            (mDataDownloadLink,
                            null);
                    if (tempFile != null) {
                        mObfuscated = ImageUtil.loadBitmap(tempFile, 0, 0, true);
                    }
                }
                if (!ImageObfuscator.checkIfValidObfuscatedImage(mObfuscated)) {
                    mObfuscated = null;
                }
                return null;
            }

            @Override
            public void onCancelled() {
                mProgressWorking.setVisibility(View.GONE);
            }

            @Override
            public void onPostExecute(Object nothing) {
                mProgressWorking.setVisibility(View.GONE);
                if (mObfuscated != null) {
                    mAccept.setEnabled(true);
                } else {
                    if (mDataDownloadLink == null) {
                        mFailExplanation.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(getApplicationContext(), R.string.obfuscated_not_valid_when_loading, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    public static URL makeDownloadLink(String photoLink) {
        // expected photoLink = photohash/photoname
        try {
            return new URL("https://experiment.whatsthat/" + (photoLink.startsWith("/") ?
                    photoLink
                    .substring(1)
                    : photoLink));
        } catch (MalformedURLException e) {
            Log.e("HomeStuff", "Failed creating URL for photolink: " + photoLink);
            return null;
        }
    }
}
