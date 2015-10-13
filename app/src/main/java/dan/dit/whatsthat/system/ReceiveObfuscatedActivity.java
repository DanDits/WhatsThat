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
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.ImageObfuscator;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("HomeStuff", "Receiveobfuscated activity created.");
        if (getIntent().getAction().equals(Intent.ACTION_VIEW)
                || getIntent().getAction().equals(Intent.ACTION_SEND)) {
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

        boolean hasValidData = (mDataFile != null && mDataFile.exists()) || mDataStream != null;
        super.onCreate(savedInstanceState);
        if (hasValidData) {
            setContentView(R.layout.receive_obfuscated_image);
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
                    Toast.makeText(getApplicationContext(), R.string.obfuscated_not_valid_when_loading, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }
}
