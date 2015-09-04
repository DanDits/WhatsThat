package dan.dit.whatsthat.image;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.preferences.Language;
import dan.dit.whatsthat.preferences.Tongue;
import dan.dit.whatsthat.preferences.User;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.util.BuildException;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * A class used by the workshop to allow relatively easily building an image
 * bundle of images on the phone by selecting images from default galleries.
 * It requires at least one Solution for each image and author information (at least a name).
 * It offers the possibility to enter multiple solution words and multiple solutions for the supported
 * languages. The interface is not the most pretty one but relatively robust and helpful, so it can be used
 * by advanced users, though the primary use would be developers or for fan :) art.
 * It does not support custom preferred or refused RiddleTypes, though the default ones belonging to the image
 * are being calculated. It fits images inside square of 700x700 pixels, keeping aspect ratio to prevent
 * too big images. It keeps the image's format.
 * A bundle requires an origin which is the bundle's author, the one generating the bundle
 * and a bundle name which is the file name and needs to be relatively unique (at least on the operating device)
 * Created by daniel on 02.09.15.
 */
public class BundleCreator {

    // Hi there. As this view controller offers on the fly validation and fast feedback for given inputs
    // and was written in two days, the code is not really nice, I'm sorry.
    private static final int ACCEPT_RESOURCE = R.drawable.a_green;
    private static final int REFUSE_RESOURCE = R.drawable.x_red;
    private static final int PICK_IMAGES = 1338; // intent to pick images
    private static final int IMAGE_MAX_WIDTH = 700; // max dimensions of images in new bundle
    private static final int IMAGE_MAX_HEIGHT = 700;
    private static final int COMPRESSION = 20; // compression level between 0 and 100 (especially for .jpg images)
    private static final int SNAPSHOT_WIDTH = 100; // displayed snapshot of currently selected bitmap
    private static final int SNAPSHOT_HEIGHT = 100;

    private final View mView;
    private final ImageView mOriginStatus;
    private final EditText mOriginName;
    private final ImageView mBundleNameStatus;
    private final EditText mBundleName;
    private final TextView mProgressText;
    private final Button mSave;
    private final TextView mBitmapsDescr;
    private final Button mBitmapsSelect;
    private final ProgressBar mBitmapsSelectProgress;
    private final ImageView mBitmapsStatus;
    private final Activity mActivity;
    private final ImageView mImageStatus;
    private final TextView mImageDescr;
    private final View mImageInformationContainer;
    private final ImageView mBitmapSnapshot;
    private final ImageButton mNextImage;
    private final ImageButton mPreviousImage;
    private final EditText mSolutionWords;
    private final Spinner mSolutionLanguages;
    private final Tongue[] mTongues;
    private final ImageView mSolutionWordsStatus;
    private final EditText mAuthorExtras;
    private final ImageView mAuthorStatus;
    private final EditText mAuthorName;
    private final EditText mAuthorSource;
    private final Button mAuthorApplyToOthers;

    private int mBitmapsSelectCount;
    private File mTempDirectory;


    private String mBundleNameText;
    private final List<SelectedBitmap> mSelectedBitmaps;
    private int mDisplayedImageIndex;
    private boolean mFillingImageData;
    private BitmapUtil.ByteBufferHolder mBuffer;
    private boolean mSaving;
    private int mSaveResult;
    private boolean mApplyToOthersStateRemoveConnection;

    public BundleCreator(Activity activity) {
        mSaveResult = ImageXmlWriter.RESULT_NONE;
        mActivity = activity;
        mBuffer = new BitmapUtil.ByteBufferHolder();
        mSelectedBitmaps = new ArrayList<>();
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.workshop_bundle_creator, null);

        //first obtain all relevant view references and setup listeners
        mProgressText = (TextView) mView.findViewById(R.id.progress_descr);
        mSave = (Button) mView.findViewById(R.id.bundle_save);
        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSaveStarted();
            }
        });
        mOriginStatus = (ImageView) mView.findViewById(R.id.origin_status);
        mOriginName = (EditText) mView.findViewById(R.id.bundle_origin);
        mOriginName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence originText, int start, int before, int count) {
                boolean newOrigin = User.getInstance().saveOriginName(originText.toString());
                if (newOrigin) {
                    applyOrigin();
                }
                updateStatus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mBundleNameStatus = (ImageView) mView.findViewById(R.id.bundle_name_status);
        mBundleName = (EditText) mView.findViewById(R.id.bundle_name);
        mBundleName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                String fileName = User.isValidFileName(text.toString());
                if (fileName != null) {
                    mBundleNameText = fileName;
                } else {
                    mBundleNameText = null;
                }
                updateStatus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mBitmapsDescr = (TextView) mView.findViewById(R.id.bitmaps_descr);
        mBitmapsSelect = (Button) mView.findViewById(R.id.bitmap_open_selection);
        mBitmapsSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSelectBitmaps();
            }
        });
        mBitmapsSelectProgress = (ProgressBar) mView.findViewById(R.id.bitmap_opening_progress);
        mBitmapsSelectProgress.setVisibility(View.INVISIBLE);
        mBitmapsSelectCount = 0;
        mBitmapsSelectProgress.setIndeterminate(true);
        mBitmapsStatus = (ImageView) mView.findViewById(R.id.bitmaps_status);

        mImageInformationContainer = mView.findViewById(R.id.image_information_container);
        mImageInformationContainer.setVisibility(View.INVISIBLE);
        mImageDescr = (TextView) mView.findViewById(R.id.image_descr);
        mImageStatus = (ImageView) mView.findViewById(R.id.image_status);

        mBitmapSnapshot = (ImageView) mView.findViewById(R.id.bitmap_snapshot);
        mNextImage = (ImageButton) mView.findViewById(R.id.next_image);
        mNextImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSaving) {
                    return;
                }
                mDisplayedImageIndex++;
                fillImageData();
                updateStatus();
            }
        });
        mPreviousImage = (ImageButton) mView.findViewById(R.id.previous_image);
        mPreviousImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSaving) {
                    return;
                }
                mDisplayedImageIndex--;
                fillImageData();
                updateStatus();
            }
        });

        mSolutionWordsStatus = (ImageView) mView.findViewById(R.id.solution_words_status);
        mSolutionWords = (EditText) mView.findViewById(R.id.solution_words);
        mSolutionWords.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!mFillingImageData) {
                    if (updateCurrentSolutionWords()) {
                        updateStatus();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mSolutionLanguages = (Spinner) mView.findViewById(R.id.solution_words_languages);
        mTongues = Tongue.values();
        mSolutionLanguages.setAdapter(new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_dropdown_item, mTongues));
        mSolutionLanguages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!mFillingImageData) {
                    fillImageDataSolution();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (!mFillingImageData) {
                    fillImageDataSolution();
                }
            }
        });

        mAuthorStatus = (ImageView) mView.findViewById(R.id.author_status);
        TextWatcher authorInfoListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!mFillingImageData) {
                    if (updateCurrentAuthorData()) {
                        updateStatus();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        mAuthorName = (EditText) mView.findViewById(R.id.author_name);
        mAuthorName.addTextChangedListener(authorInfoListener);
        mAuthorSource = (EditText) mView.findViewById(R.id.author_source);
        mAuthorSource.addTextChangedListener(authorInfoListener);
        mAuthorExtras = (EditText) mView.findViewById(R.id.author_extras);
        mAuthorExtras.addTextChangedListener(authorInfoListener);
        mAuthorApplyToOthers = (Button) mView.findViewById(R.id.apply_author_info_to_others);
        mAuthorApplyToOthers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAuthorInfoToOthers();
            }
        });

        // wipe temp storage
        mTempDirectory = User.clearTempDirectory();

        // fill data into views
        fillData();

        //check for missing or illegal information and show this with status images
        updateStatus();
    }

    private void onSaveStarted() {
        if (mSaving) {
            return;
        }
        mAuthorApplyToOthers.setEnabled(false);
        mSave.setEnabled(false);
        mBitmapsSelect.setEnabled(false);
        mBundleName.setEnabled(false);
        mAuthorExtras.setEnabled(false);
        mAuthorName.setEnabled(false);
        mAuthorSource.setEnabled(false);
        mBundleName.setEnabled(false);
        mOriginName.setEnabled(false);
        mSolutionWords.setEnabled(false);
        mSolutionLanguages.setEnabled(false);
        final List<SelectedBitmap> toSave = new ArrayList<>(mSelectedBitmaps.size());
        synchronized (mSelectedBitmaps) {
            toSave.addAll(mSelectedBitmaps);
        }
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                mSaveResult = ImageXmlWriter.writeBundle(mActivity, toSave, User.getInstance().getOriginName(), mBundleNameText);
                Log.d("Image", "Write bundle, result: " + mSaveResult);
                mSaving = false;
                return null;
            }

            @Override
            public void onPostExecute(Void nothing) {
                if (mSaveResult == ImageXmlWriter.RESULT_SUCCESS) {
                    synchronized (mSelectedBitmaps) {
                        mSelectedBitmaps.clear();
                    }
                    Toast.makeText(mActivity, mActivity.getResources().getString(R.string.bundle_creator_success, mBundleNameText), Toast.LENGTH_LONG).show();

                    mBundleName.setText("");
                } else if (mSaveResult == ImageXmlWriter.RESULT_TARGET_BUNDLE_EXISTS) {

                    Toast.makeText(mActivity, mActivity.getResources().getString(R.string.bundle_creator_failed_name_exists, mBundleNameText), Toast.LENGTH_LONG).show();

                    mBundleName.setText("");
                } else if (mSaveResult == ImageXmlWriter.RESULT_EXTERNAL_STORAGE_PROBLEM || mSaveResult == ImageXmlWriter.RESULT_NO_TEMP_DIRECTORY) {
                    Toast.makeText(mActivity, R.string.bundle_creator_failed_external_storage, Toast.LENGTH_LONG).show();
                }
                reenable();
            }

            @Override
            public void onCancelled(Void obj) {
                reenable();
            }

            private void reenable() {
                mSaving = false;
                mBitmapsSelect.setEnabled(true);
                mBundleName.setEnabled(true);
                mAuthorApplyToOthers.setEnabled(true);
                mAuthorExtras.setEnabled(true);
                mAuthorName.setEnabled(true);
                mAuthorSource.setEnabled(true);
                mBundleName.setEnabled(true);
                mOriginName.setEnabled(true);
                mSolutionWords.setEnabled(true);
                mSolutionLanguages.setEnabled(true);
                fillData();
                updateStatus();

            }
        }.execute();
    }

    private void applyAuthorInfoToOthers() {
        mSaveResult = ImageXmlWriter.RESULT_NONE;
        boolean updateStatus = false;
        boolean updateApplyToOthers = false;
        synchronized (mSelectedBitmaps) {
            SelectedBitmap current = getCurrentBitmap();
            if (current == null) {
                return;
            }
            ImageAuthor author = current.mBuilder.getAuthor();
            if (author == null) {
                return; //should not be clickable if there is not an author for current
            }
            if (mApplyToOthersStateRemoveConnection) {
                ImageAuthor copy = new ImageAuthor(author.getName(), author.getSource(), author.getLicense(), author.getTitle(), author.getExtras());
                current.mBuilder.setAuthor(copy);
                updateApplyToOthers = true;
            } else {
                for (SelectedBitmap curr : mSelectedBitmaps) {
                    if (curr.mBuilder.getAuthor() == null) {
                        curr.mBuilder.setAuthor(author);
                        updateApplyToOthers = true;
                        if (curr.checkedBuild()) {
                            updateStatus = true;
                        }
                    }
                }
            }
            if (updateApplyToOthers) {
                updateApplyToOthersButton(current);
            }
        }
        if (updateStatus) {
            updateStatus();
        }
    }

    private Tongue getCurrentTongue() {
        int selected = mSolutionLanguages.getSelectedItemPosition();
        if (selected == AdapterView.INVALID_POSITION) {
            for (selected = 0; selected < mTongues.length; selected++) {
                if (mTongues[selected] == Language.getInstance().getTongue()) {
                    break;
                }
            }
        }
        if (selected < 0 || selected >= mTongues.length) {
            return null;
        }
        return mTongues[selected];
    }

    private SelectedBitmap getCurrentBitmap() {
        if (mDisplayedImageIndex < 0 || mDisplayedImageIndex >= mSelectedBitmaps.size()) {
            return null;
        }
        SelectedBitmap selected;
        synchronized (mSelectedBitmaps) {
            selected =  mSelectedBitmaps.get(mDisplayedImageIndex);
        }
        return selected;
    }

    private boolean updateCurrentSolutionWords() {
        Tongue currentTongue = getCurrentTongue();
        SelectedBitmap currentBitmap = getCurrentBitmap();
        if (currentBitmap == null || currentTongue == null) {
            return false;
        }
        String solutionWords = mSolutionWords.getText().toString();
        List<Solution> solutions = currentBitmap.mBuilder.getSolutions();
        int currentSolutionIndex;
        Solution currentSolution = null;
        if (solutions == null) {
            currentSolutionIndex = -1;
        } else {
            for (currentSolutionIndex = 0; currentSolutionIndex < solutions.size(); currentSolutionIndex++) {
                currentSolution = solutions.get(currentSolutionIndex);
                if (currentSolution.getTongue().equals(currentTongue)) {
                    break;
                }
                currentSolution = null;
            }
        }
        boolean updateStatus = false;
        if (TextUtils.isEmpty(solutionWords)) {
            if (currentSolution != null) {
                solutions.remove(currentSolutionIndex);
                if (solutions.isEmpty()) {
                    currentBitmap.mBuilder.setSolutions(null);
                    currentBitmap.mImage = null;
                    updateStatus = true;
                }
            }
        } else {
            Solution newSolution = Solution.makeSolution(currentTongue, solutionWords.split(","));
            if (newSolution != null && solutions == null) {
                solutions = new ArrayList<>(mTongues.length);
                updateStatus = true;
            } else if (solutions != null && currentSolution != null) {
                solutions.remove(currentSolutionIndex);
            }
            if (newSolution != null) {
                solutions.add(newSolution);

                currentBitmap.mBuilder.setSolutions(solutions);
                if (currentBitmap.checkedBuild()) {
                    updateStatus = true;
                }
            } else if (solutions != null && solutions.isEmpty()) {
                currentBitmap.mBuilder.setSolutions(null);
                currentBitmap.mImage = null;
                updateStatus = true;
            }
        }
        return updateStatus;
    }

    private boolean updateCurrentAuthorData() {
        boolean updateStatus = false;
        SelectedBitmap current = getCurrentBitmap();
        if (current == null) {
            return false;
        }
        mSaveResult = ImageXmlWriter.RESULT_NONE;
        ImageAuthor author = current.mBuilder.getAuthor();
        String authorNameText = mAuthorName.getText().toString();
        if (author == null) {
            author = new ImageAuthor();
            current.mBuilder.setAuthor(author);
            updateApplyToOthersButton(current);
            updateStatus = true;
        } else {
            if (TextUtils.isEmpty(author.getName()) && !TextUtils.isEmpty(authorNameText)) {
                updateStatus = true;
            }
        }
        author.setName(authorNameText);
        author.setSource(mAuthorSource.getText().toString());
        author.setExtras(mAuthorExtras.getText().toString());
        if (updateStatus) {
            current.checkedBuild();
            synchronized (mSelectedBitmaps) {
                for (SelectedBitmap bitmap : mSelectedBitmaps) {
                    if (bitmap.mBuilder.getAuthor() == author) {
                        bitmap.checkedBuild();
                    }
                }
            }
        }
        if (TextUtils.isEmpty(author.getName())) {
            boolean authorCleared = TextUtils.isEmpty(author.getSource()) && TextUtils.isEmpty(author.getExtras());
            // if author data cleared, remove author from this and connected bitmaps
            // in any case clear image as name is required
            synchronized (mSelectedBitmaps) {
                for (SelectedBitmap bitmap : mSelectedBitmaps) {
                    if (bitmap.mBuilder.getAuthor() == author) {
                        if (authorCleared) {
                            bitmap.mBuilder.setAuthor(null);
                        }
                        bitmap.mImage = null;
                    }
                }
            }
            updateApplyToOthersButton(current);

            updateStatus = true;
        }
        return updateStatus;
    }

    private void updateApplyToOthersButton(SelectedBitmap current) {
        mAuthorApplyToOthers.setEnabled(false);
        if (current == null) {
            return;
        }
        mSaveResult = ImageXmlWriter.RESULT_NONE;
        boolean allHaveAuthor = true;
        boolean hasSharedAuthor = false;
        synchronized (mSelectedBitmaps) {
            for (SelectedBitmap bitmap : mSelectedBitmaps) {
                if (bitmap.mBuilder.getAuthor() == null) {
                    allHaveAuthor = false;
                    break;
                } else if (current != bitmap && bitmap.mBuilder.getAuthor().equals(current.mBuilder.getAuthor())) {
                    hasSharedAuthor = true;
                }
            }
        };

        mApplyToOthersStateRemoveConnection = allHaveAuthor && hasSharedAuthor;
        mAuthorApplyToOthers.setEnabled(current.mBuilder.getAuthor() != null && (mApplyToOthersStateRemoveConnection || !allHaveAuthor));
        mAuthorApplyToOthers.setText(mApplyToOthersStateRemoveConnection ? R.string.author_info_apply_to_others_remove_connection : R.string.author_info_apply_to_others);
    }

    private void openSelectBitmaps() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            getIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");


        Intent chooserIntent = Intent.createChooser(getIntent, mActivity.getResources().getString(R.string.select_images_from_gallery));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});

        mActivity.startActivityForResult(chooserIntent, PICK_IMAGES);
    }

    /**
     * Checks and updates all status indicators, updates current image's status indicators.
     * If all checks are passed, the bundle is allowed to save.
     */
    private void updateStatus() {
        String userOrigin = User.getInstance().getOriginName();
        boolean originOk = userOrigin != null && userOrigin.equals(mOriginName.getText().toString().trim());
        applyStatus(mOriginStatus, originOk);
        boolean bundleNameOk = mBundleNameText != null && mBundleNameText.equals(mBundleName.getText().toString().trim());
        applyStatus(mBundleNameStatus, bundleNameOk);
        boolean bitmapsOk = mSelectedBitmaps.size() > 0 && mBitmapsSelectCount == 0;
        applyStatus(mBitmapsStatus, bitmapsOk);
        boolean imageInformationOk = getCompletedImageDataCount() == mSelectedBitmaps.size() && mSelectedBitmaps.size() > 0;
        applyStatus(mImageStatus, imageInformationOk);

        boolean allOk = originOk && bundleNameOk && bitmapsOk && imageInformationOk;
        mSave.setEnabled(allOk);
        if (allOk) {
            if (mSaveResult == ImageXmlWriter.RESULT_SUCCESS) {
                mProgressText.setText(R.string.bundle_creator_progress_saved);
            } else if (mSaveResult == ImageXmlWriter.RESULT_NONE) {
                mProgressText.setText(R.string.bundle_creator_progress_ready_to_save);
            } else {
                mProgressText.setText(mProgressText.getResources().getString(R.string.bundle_creator_progress_error_result, mSaveResult));
            }
            mSave.setCompoundDrawablesWithIntrinsicBounds(0, 0, ACCEPT_RESOURCE, 0);
        } else {
            mSaving = false;
            mSave.setCompoundDrawablesWithIntrinsicBounds(0, 0, REFUSE_RESOURCE, 0);
            mProgressText.setText(R.string.bundle_creator_progress_info_missing);
        }
        updateCurrentImageStatus();
        fillImageHeaderData();
    }

    private void updateCurrentImageStatus() {
        if (mImageInformationContainer.getVisibility() != View.VISIBLE) {
            return;
        }
        SelectedBitmap current = getCurrentBitmap();
        if (current == null) {
            return;
        }
        boolean solutionWordsOk = current.mBuilder.getSolutions() != null && !current.mBuilder.getSolutions().isEmpty();
        applyStatus(mSolutionWordsStatus, solutionWordsOk);

        boolean authorOk = current.mBuilder.getAuthor() != null && !TextUtils.isEmpty(current.mBuilder.getAuthor().getName());
        applyStatus(mAuthorStatus, authorOk);

    }

    private static void applyStatus(ImageView statusView, boolean accept) {
        statusView.setImageResource(accept ? ACCEPT_RESOURCE : REFUSE_RESOURCE);
    }

    /**
     * Fills the data currently available into the views. Fills selected bitmaps data.
     */
    private void fillData() {
        String origin = User.getInstance().getOriginName();
        if (origin != null) {
            mOriginName.setText(origin);
        } else {
            mOriginName.setText("");
        }

        if (mBundleNameText != null) {
            mBundleName.setText(mBundleNameText);
        } else {
            mBundleName.setText("");
        }
        fillSelectedBitmapsData();
    }

    /**
     * Fills selected bitmaps data, showing amount of loaded bitmaps
     * and setting image data container visibility accordingly.
     * Updates image data header.
     */
    private void fillSelectedBitmapsData() {
        int count;
        int sizeMB;
        synchronized (mSelectedBitmaps) {
            count = mSelectedBitmaps.size();
            sizeMB = 0;
            for (SelectedBitmap bitmap : mSelectedBitmaps) {
                sizeMB += bitmap.mEstimatedSizeKB;
            }
        }
        sizeMB /= 1024;
        if (count > 0) {
            fillImageData();
            if (mImageInformationContainer.getVisibility() != View.VISIBLE) {
                mImageInformationContainer.setVisibility(View.VISIBLE);
            }
            // would be strange to have 0MB even though there are images selected
            sizeMB = Math.max(1, sizeMB);
        } else {
            mImageInformationContainer.setVisibility(View.GONE);
        }
        mBitmapsDescr.setText(mBitmapsDescr.getResources().getString(R.string.bundle_creator_bitmaps_descr, count, sizeMB));
        mBitmapsSelect.setEnabled(true);

        fillImageHeaderData();
    }

    /**
     * Fills all image data of the currently selected bitmap if any.
     */
    private void fillImageData() {
        boolean wasFilling = mFillingImageData;
        mFillingImageData = true;
        mDisplayedImageIndex = Math.max(mDisplayedImageIndex, 0);
        mDisplayedImageIndex = Math.min(mDisplayedImageIndex, mSelectedBitmaps.size() - 1);
        if (mDisplayedImageIndex < 0) {
            return;
        }
        final SelectedBitmap selected = getCurrentBitmap();
        if (selected == null) {
            return;
        }
        mNextImage.setVisibility(mDisplayedImageIndex < mSelectedBitmaps.size() - 1 ? View.VISIBLE : View.INVISIBLE);
        mPreviousImage.setVisibility(mDisplayedImageIndex > 0 ? View.VISIBLE : View.INVISIBLE);

        mSaveResult = ImageXmlWriter.RESULT_NONE;
        if (selected.mSnapshot != null) {
            mBitmapSnapshot.setImageBitmap(selected.mSnapshot);
            selected.mLoadSnapshotTask = null;
        } else {
            mBitmapSnapshot.setImageResource(REFUSE_RESOURCE);
            selected.mLoadSnapshotTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    selected.mSnapshot = ImageUtil.loadBitmap(selected.mPathInTemp, SNAPSHOT_WIDTH, SNAPSHOT_HEIGHT, BitmapUtil.MODE_FIT_INSIDE);
                    return null;
                }
                @Override
                public void onPostExecute(Void result) {
                    fillImageData();
                }
            }.execute();
        }
        fillImageDataSolution();
        fillImageDataAuthor();
        mFillingImageData = wasFilling;
        updateCurrentImageStatus();
    }

    private void fillImageDataSolution() {
        boolean wasFilling = mFillingImageData;
        mFillingImageData = true;
        Tongue currentTongue = getCurrentTongue();
        if (currentTongue == null || mDisplayedImageIndex < 0) {
            return;
        }
        SelectedBitmap bitmap = getCurrentBitmap();
        List<Solution> solutions = bitmap.mBuilder.getSolutions();
        mSolutionWords.setText("");
        if (solutions != null) {
            Solution curr = null;
            for (Solution sol : solutions) {
                if (sol.getTongue().equals(currentTongue)) {
                    curr = sol;
                    break;
                }
            }
            if (curr != null) {
                StringBuilder words = new StringBuilder();
                List<String> wordsList = curr.getWords();
                for (int i = 0; i < wordsList.size(); i++) {
                    words.append(wordsList.get(i));
                    if (i < wordsList.size() - 1) {
                        words.append(",");
                    }
                }
                mSolutionWords.setText(words.toString());
            }
        }
        mFillingImageData = wasFilling;
    }

    private void fillImageDataAuthor() {
        boolean wasFilling = mFillingImageData;
        mFillingImageData = true;
        SelectedBitmap current = getCurrentBitmap();
        mAuthorName.setText("");
        mAuthorSource.setText("");
        mAuthorExtras.setText("");
        if (current != null && current.mBuilder.getAuthor() != null) {
            ImageAuthor author = current.mBuilder.getAuthor();
            mAuthorName.setText(author.getName());
            mAuthorSource.setText(author.getSource());
            mAuthorExtras.setText(author.getExtras());
        }
        updateApplyToOthersButton(current);
        mFillingImageData = wasFilling;
    }

    private int getCompletedImageDataCount() {
        int count = 0;
        synchronized (mSelectedBitmaps) {
            for (SelectedBitmap bitmap : mSelectedBitmaps) {
                if (bitmap.mImage != null) {
                    count++;
                }
            }
        }
        return count;
    }

    private void fillImageHeaderData() {
        int completedCount = getCompletedImageDataCount();
        mImageDescr.setText(mImageDescr.getResources().getString(R.string.bundle_creator_image_descr, completedCount, mSelectedBitmaps.size()));
    }

    /**
     * Required for reacting to selected bitmaps from galleries. Loads and prepares
     * bitmaps asynchronously, showing progress on the fly and an indeterminate progress bar.
     * @param requestCode The request code of the activity result started for intent.
     * @param resultCode The result code. Does nothing if not ok.
     * @param data The returned data.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || requestCode != PICK_IMAGES || data == null) {
            return;
        }
        ClipData clipData = data.getClipData();
        Uri[] selectedImageUris;
        if (clipData == null) {
            selectedImageUris = new Uri[] {data.getData()};
        } else {
            selectedImageUris = new Uri[clipData.getItemCount()];
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item curr = clipData.getItemAt(i);
                selectedImageUris[i] = curr.getUri();
            }
        }
        new AsyncTask<Uri, SelectedBitmap, Void>() {
            @Override
            public void onPreExecute() {
                ensureTempDirectory();
                mBitmapsSelectCount++;
                if (mBitmapsSelectCount > 0) {
                    mBitmapsSelectProgress.setVisibility(View.VISIBLE);
                }
            }

            @Override
            protected Void doInBackground(Uri... params) {
                String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DISPLAY_NAME };
                for (Uri selectedImages : params) {
                    if (selectedImages == null) {
                        continue;
                    }
                    Cursor cursor = null;
                    try {
                        cursor = mActivity.getContentResolver().query(selectedImages,
                                columns, null, null, null);
                        cursor.moveToFirst();
                        if (cursor.isAfterLast()) {
                            continue; // empty cursor
                        }

                        int pathIndex = cursor.getColumnIndexOrThrow(columns[0]);
                        int sizeIndex = cursor.getColumnIndexOrThrow(columns[1]);
                        int nameIndex = cursor.getColumnIndex(columns[2]);
                        while (!cursor.isAfterLast()) {
                            String picturePath = cursor.getString(pathIndex);
                            int sizeKB = cursor.getInt(sizeIndex) / (1024);
                            if (picturePath == null) {
                                InputStream input = null;
                                try {
                                    input = mActivity.getContentResolver().openInputStream(selectedImages);
                                } catch (FileNotFoundException e1) {
                                    Log.e("HomeStuff", "File not found when trying to decode bitmap from stream: " + e1);
                                }
                                String bitmapName = null;
                                if (nameIndex != -1) {
                                    bitmapName = cursor.getString(nameIndex);
                                }
                                if (TextUtils.isEmpty(bitmapName)) {
                                    bitmapName = String.valueOf(System.currentTimeMillis());
                                }
                                File bitmapFileInTemp = new File(mTempDirectory, bitmapName);

                                Bitmap bitmap = ImageUtil.loadBitmap(input, IMAGE_MAX_WIDTH, IMAGE_MAX_HEIGHT, BitmapUtil.MODE_FIT_NO_GROW);
                                if (ImageUtil.saveToFile(bitmap, bitmapFileInTemp, null, COMPRESSION)) {
                                    publishProgress(new SelectedBitmap(bitmap, bitmapFileInTemp, sizeKB, mBuffer));
                                }
                            } else {
                                File path = new File(picturePath);
                                String bitmapName = path.getName();
                                if (TextUtils.isEmpty(bitmapName) && nameIndex != -1) {
                                    bitmapName = cursor.getString(nameIndex);
                                }
                                File targetPath = new File(mTempDirectory, bitmapName);
                                Bitmap bitmap = ImageUtil.loadBitmap(path, IMAGE_MAX_WIDTH, IMAGE_MAX_HEIGHT, BitmapUtil.MODE_FIT_NO_GROW);
                                if (ImageUtil.saveToFile(bitmap, targetPath, null, COMPRESSION)) {
                                    publishProgress(new SelectedBitmap(bitmap, targetPath, sizeKB, mBuffer));
                                }
                            }
                            cursor.moveToNext();
                        }
                    } catch (Exception e) {
                        Log.e("HomeStuff", "Error retrieving images from cursor " + cursor + ": " + e);
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
                return null;
            }

            @Override
            public void onProgressUpdate(SelectedBitmap... bitmap) {
                if (!isImageSelected(bitmap[0].mPathInTemp.getAbsolutePath())) {
                    synchronized (mSelectedBitmaps) {
                        mSelectedBitmaps.add(bitmap[0]);
                    }
                }
                mSaveResult = ImageXmlWriter.RESULT_NONE;
                fillSelectedBitmapsData();
                updateStatus();
            }

            @Override
            public void onPostExecute(Void nothing) {
                mSaveResult = ImageXmlWriter.RESULT_NONE;
                Log.d("HomeStuff", "Finished loading bitmaps (Processes still loading: " + (mBitmapsSelectCount - 1) + ").");
                mBitmapsSelectCount--;
                if (mBitmapsSelectCount <= 0) {
                    mBitmapsSelectProgress.setVisibility(View.INVISIBLE);
                }
                applyOrigin();
                fillSelectedBitmapsData();
                updateStatus();
            }
        }.execute(selectedImageUris);

    }

    /**
     * Applies origin to all selected bitmaps.
     */
    private void applyOrigin() {
        String origin = User.getInstance().getOriginName();
        if (origin == null) {
            return;
        }
        synchronized (mSelectedBitmaps) {
            for (SelectedBitmap bitmap : mSelectedBitmaps) {
                bitmap.mBuilder.setOrigin(origin);
            }
        }
    }

    private boolean isImageSelected(String pathInTemp) {
        synchronized (mSelectedBitmaps) {
            for (SelectedBitmap bitmap : mSelectedBitmaps) {
                if (bitmap.mPathInTemp.getAbsolutePath().equalsIgnoreCase(pathInTemp)) {
                    return true;
                }
            }
        }
        return false;
    }

    private synchronized void ensureTempDirectory() {
        if (mTempDirectory == null) {
            mTempDirectory = User.getTempDirectory();
            if (mTempDirectory == null) {
                throw new IllegalStateException("No temp directory available");
            }
        }
    }

    public static class SelectedBitmap {
        public final File mPathInTemp;
        private Image.Builder mBuilder;
        public Image mImage;
        private int mEstimatedSizeKB;
        private AsyncTask<Void, Void, Void> mLoadSnapshotTask;
        public Bitmap mSnapshot;

        public SelectedBitmap(Bitmap bitmap, File imagePath, int sizeKB, BitmapUtil.ByteBufferHolder buffer) {
            mBuilder = new Image.Builder();
            mBuilder.calculateHashAndPreferences(buffer, bitmap);
            mBuilder.setRelativeImagePath(User.extractRelativePathInsideTempDirectory(imagePath));
            mPathInTemp = imagePath;
            mEstimatedSizeKB = sizeKB;
        }

        @Override
        public String toString() {
            return mImage != null ? mImage.toString() : "Unprepared SelectedBitmap: " + mPathInTemp + " (" + mEstimatedSizeKB + ")";
        }

        public boolean checkedBuild() {
            if (mBuilder.getOrigin() != null && !mBuilder.getOrigin().equals(Image.ORIGIN_IS_THE_APP)
                    && mBuilder.getSolutions() != null && !mBuilder.getSolutions().isEmpty()
                    && mBuilder.getAuthor() != null && !TextUtils.isEmpty(mBuilder.getAuthor().getName()) ) {
                try {
                    mImage = mBuilder.build();
                } catch (BuildException be) {
                    Log.e("Image", "Failed building image: " + be);
                    mImage = null;
                }
                return true;
            }
            return false;
        }
    }

    public View getView() {
        return mView;
    }
}
