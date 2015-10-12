package dan.dit.whatsthat.util.mosaic;

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
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.image.Image;
import dan.dit.whatsthat.image.ImageBitmapSource;
import dan.dit.whatsthat.system.RiddleFragment;
import dan.dit.whatsthat.system.store.WorkshopView;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ColorMetric;
import dan.dit.whatsthat.util.image.Dimension;
import dan.dit.whatsthat.util.image.ImageCache;
import dan.dit.whatsthat.util.image.ImageUtil;
import dan.dit.whatsthat.util.mosaic.data.MosaicMaker;
import dan.dit.whatsthat.util.mosaic.matching.SimpleLinearTileMatcher;
import dan.dit.whatsthat.util.mosaic.matching.TileMatcher;

/**
 * Created by daniel on 07.10.15.
 */
public class MosaicGeneratorUi {
    private static final double DEFAULT_ROWS_COLUMNS = 30;
    private static final double MAX_ROWS_COLUMNS = 100;
    private static final ColorMetric DEFAULT_COLOR_METRIC = ColorMetric.Euclid2.INSTANCE;
    private static final boolean DEFAULT_USE_ALPHA = true;
    private static final int MAX_IMAGE_WIDTH_HEIGHT = 1024; // else we run into out of memory errors really quick since most cameras produce high resolution images and we only get 50mb ram from JVM
    private final Activity mActivity;
    private final View mView;
    private final Spinner mMosaicTypes;
    private final List<ColorMetric> mMetrics;
    private final ImageView mSelectedBitmapState;
    private final ImageView mMosaicImageView;
    private final View mWorkingIndicator;
    private final PercentProgressListener mProgress;
    private File mMediaDirectory;
    private String mSelectedBitmapName;
    private String mMosaicBitmapName;
    private Bitmap mSelectedBitmap;
    private List<MosaicType> mTypes;
    private List<TextView> mParameterName;
    private List<SeekBar> mParameterValue;
    private List<View> mParameterContainer;
    private CheckBox mParameterUseAlpha;
    private Spinner mParameterColorMetric;

    private MosaicMaker<String> mMosaicMaker;
    private int mIgnoreParameterChange;
    private AsyncTask<Void, Integer, Bitmap> mMosaicTask;
    private Bitmap mMosaicBitmap;
    private File mMosaicFile;
    private View mShare;

    public MosaicGeneratorUi(Activity activity) {
        mActivity = activity;

        Map<String, Image> images = RiddleFragment.ALL_IMAGES;
        ImageBitmapSource source = new ImageBitmapSource(mActivity.getResources(), images);
        TileMatcher<String> matcher = new SimpleLinearTileMatcher<>(images.values(), DEFAULT_USE_ALPHA, DEFAULT_COLOR_METRIC);
        mMosaicMaker = new MosaicMaker<>(matcher, source, DEFAULT_USE_ALPHA, DEFAULT_COLOR_METRIC);

        mTypes = new ArrayList<>(3);
        mTypes.add(new MosaicType(MosaicType.RECT, R.string.mosaic_generator_mosaic_type_rect)
                    .addParameter(R.string.mosaic_generator_param_rows, 1, MAX_ROWS_COLUMNS, DEFAULT_ROWS_COLUMNS, true)
                    .addParameter(R.string.mosaic_generator_param_columns, 1, MAX_ROWS_COLUMNS, DEFAULT_ROWS_COLUMNS, true));
        mTypes.add(new MosaicType(MosaicType.MULTI_RECT, R.string.mosaic_generator_mosaic_type_multirect)
                .addParameter(R.string.mosaic_generator_param_rows, 1, MAX_ROWS_COLUMNS, DEFAULT_ROWS_COLUMNS, true)
                .addParameter(R.string.mosaic_generator_param_columns, 1, MAX_ROWS_COLUMNS, DEFAULT_ROWS_COLUMNS, true)
                .addParameter(R.string.mosaic_generator_param_merge_factor, 0., 1., 0.5, false));
        mTypes.add(new MosaicType(MosaicType.FIXED_LAYER, R.string.mosaic_generator_mosaic_type_fixedlayer)
                .addParameter(R.string.mosaic_generator_param_layer_count, 1, 10, 3, true));

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.workshop_mosaic_generator, null);

        mMosaicTypes = (Spinner) mView.findViewById(R.id.mosaic_types);
        mMosaicTypes.setAdapter(new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_dropdown_item, mTypes));
        mMosaicTypes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MosaicType type = mTypes.get(position);
                type.applyUi();
                type.makeMosaic();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mView.findViewById(R.id.parameter_title).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIgnoreParameterChange++;
                View container = mView.findViewById(R.id.parameter_inner_container);
                if (container.getVisibility() == View.VISIBLE) {
                    container.setVisibility(View.GONE);
                    ((TextView) v).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.achievement_list_group_indicator_down, 0);
                } else {
                    container.setVisibility(View.VISIBLE);
                    ((TextView) v).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.achievement_list_group_indicator_up, 0);
                }
                mIgnoreParameterChange--;
            }
        });

        mParameterName = new ArrayList<>(3);
        mParameterValue = new ArrayList<>(3);
        mParameterContainer = new ArrayList<>(3);
        mParameterName.add((TextView) mView.findViewById(R.id.parameter1_name));
        mParameterName.add((TextView) mView.findViewById(R.id.parameter2_name));
        mParameterName.add((TextView) mView.findViewById(R.id.parameter3_name));
        mParameterValue.add((SeekBar) mView.findViewById(R.id.parameter1_value));
        mParameterValue.add((SeekBar) mView.findViewById(R.id.parameter2_value));
        mParameterValue.add((SeekBar) mView.findViewById(R.id.parameter3_value));
        SeekBar.OnSeekBarChangeListener seekChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int index = mParameterValue.indexOf(seekBar);
                    if (index >= 0) {
                        MosaicType type = getCurrentMosaicType();
                        if (type != null) {
                            type.barToValue(seekBar, index);
                        }
                    }
                    onParameterChanged(false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onParameterChanged(true);
            }
        };
        for (int i = 0; i < mParameterValue.size(); i++) {
            mParameterValue.get(i).setOnSeekBarChangeListener(seekChangeListener);
        }
        mParameterContainer.add(mView.findViewById(R.id.parameter1));
        mParameterContainer.add(mView.findViewById(R.id.parameter2));
        mParameterContainer.add(mView.findViewById(R.id.parameter3));
        mParameterUseAlpha = (CheckBox) mView.findViewById(R.id.parameterUseAlpha_value);
        mParameterColorMetric = (Spinner) mView.findViewById(R.id.parameterColorMetric_value);

        mMetrics = ColorMetric.makeAll();
        mParameterColorMetric.setAdapter(new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_dropdown_item, mMetrics));

        mParameterUseAlpha.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mMosaicMaker.setUseAlpha(isChecked);
                onParameterChanged(true);
            }
        });
        mParameterColorMetric.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mMetrics.get(position).equals(mMosaicMaker.getColorMetric())) {
                    return;
                }
                mMosaicMaker.setColorMetric(mMetrics.get(position));
                onParameterChanged(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mSelectedBitmapState = (ImageView) mView.findViewById(R.id.select_image_state);
        applySelectImageState();
        mView.findViewById(R.id.select_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSelectBitmap();
            }
        });
        mMosaicImageView = (ImageView) mView.findViewById(R.id.mosaic_image);
        mWorkingIndicator = mView.findViewById(R.id.progress_is_working);
        mProgress = (PercentProgressListener) mView.findViewById(R.id.progress_bar);

        mView.findViewById(R.id.save_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = mMosaicBitmap;
                if (bitmap != null) {
                    ensureMediaDirectory();
                    mMosaicFile = ImageUtil.saveToMediaFile(mActivity, bitmap, mMosaicBitmapName);
                    applyShare();
                    if (mMosaicFile != null) {
                        Toast.makeText(mActivity, R.string.mosaic_generator_image_save_success, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mActivity, R.string.mosaic_generator_image_save_failed, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        mShare = mView.findViewById(R.id.share_image);
        mShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareMosaic();
            }
        });
        applyShare();
    }

    private void applyShare() {
        mShare.setEnabled(mMosaicFile != null);
    }

    private void shareMosaic() {
        if (mMosaicFile != null) {

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/*");
            share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mMosaicFile));
            mActivity.startActivity(Intent.createChooser(share, mActivity.getResources().getString(R.string.mosaic_generator_share_mosaic)));
        }
    }
    private void onBitmapSelected() {
        applySelectImageState();
        Log.d("HomeStuff", "Bitmap selected: " + mSelectedBitmap.getWidth() + "/" + mSelectedBitmap.getHeight());
        mMosaicImageView.setImageBitmap(mSelectedBitmap);
        MosaicType curr = getCurrentMosaicType();
        if (curr != null) {
            curr.makeMosaic();
        }
    }

    private void onMosaicDone(@NonNull Bitmap mosaic) {
        mMosaicBitmap = mosaic;
        mMosaicImageView.setImageBitmap(mMosaicBitmap);
    }

    private void applySelectImageState() {
        if (mSelectedBitmap == null) {
            mSelectedBitmapState.setImageResource(R.drawable.x_red);
        } else {
            mSelectedBitmapState.setImageResource(R.drawable.a_green);
        }
    }

    private MosaicType getCurrentMosaicType() {
        if (mMosaicTypes == null || mMosaicTypes.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
            return null;
        }
        return mTypes.get(mMosaicTypes.getSelectedItemPosition());
    }

    private void onParameterChanged(boolean updateMosaic) {
        if (mIgnoreParameterChange > 0) {
            return;
        }
        Log.d("Riddle", "On Parameter changed: " + mIgnoreParameterChange);
        MosaicType type = getCurrentMosaicType();
        if (type != null) {
            type.applyUi();
            if (updateMosaic) {
                type.makeMosaic();
            }
        }
    }

    public View getView() {
        return mView;
    }

    public void clear() {
        // to release memory
        mSelectedBitmapName = null;
        if (mSelectedBitmap != null) {
            mSelectedBitmap.recycle();
        }
        mSelectedBitmap = null;
        mMosaicImageView.setImageResource(0);
        if (mMosaicBitmap != null) {
            mMosaicBitmap.recycle();
        }
        mMosaicBitmap = null;
    }

    private class MosaicType {
        public static final int RECT = 0;
        public static final int MULTI_RECT = 1;
        public static final int FIXED_LAYER = 2;
        private final int mNameResId;
        private final List<Double> mMinValues;
        private final List<Double> mMaxValues;
        private final List<Double> mValues;
        private final List<Integer> mParameterNameResIds;
        private final int mType;
        private List<Boolean> mOnlyIntegers;

        public MosaicType(int type, int nameResId) {
            mType = type;
            mNameResId = nameResId;
            mMinValues = new LinkedList<>();
            mMaxValues = new LinkedList<>();
            mValues = new LinkedList<>();
            mOnlyIntegers = new LinkedList<>();
            mParameterNameResIds = new LinkedList<>();
        }

        public MosaicType addParameter(int nameResId, double minValue, double maxValue, double defaultValue, boolean onlyIntegers) {
            mParameterNameResIds.add(nameResId);
            mMinValues.add(minValue);
            mMaxValues.add(maxValue);
            mValues.add(defaultValue);
            mOnlyIntegers.add(onlyIntegers);
            return this;
        }

        private void valueToBar(SeekBar bar, double minValue, double maxValue, double value) {
            // interval [minValue, maxValue] mapped to [0, bar.max()]
            int progress = (int) ((value - minValue) / (maxValue - minValue) * bar.getMax());
            bar.setProgress(progress);
        }

        private void barToValue(SeekBar bar, int index) {
            double value = bar.getProgress() / (double) bar.getMax() * (mMaxValues.get(index) - mMinValues.get(index)) + mMinValues.get(index);
            if (mOnlyIntegers.get(index)) {
                value = Math.round(value);
            }
            mValues.set(index, value);
        }

        public void applyUi() {
            mIgnoreParameterChange++;
            mParameterUseAlpha.setChecked(mMosaicMaker.usesAlpha());
            mParameterColorMetric.setSelection(mMetrics.indexOf(mMosaicMaker.getColorMetric()));
            for (int i = 0; i < mParameterContainer.size(); i++) {
                if (i < mMinValues.size()) {
                    // got that parameter
                    mParameterContainer.get(i).setVisibility(View.VISIBLE);
                    SeekBar bar = mParameterValue.get(i);
                    double max = mMaxValues.get(i);
                    double min = mMinValues.get(i);
                    int steps;
                    if (mOnlyIntegers.get(i)) {
                        steps = (int) (max - min + 1);
                    } else {
                        steps = 100;
                    }
                    bar.setMax(steps);
                    valueToBar(bar, min, max, mValues.get(i));
                    mParameterName.get(i).setText(mActivity.getResources().getString(mParameterNameResIds.get(i), mValues.get(i)));
                } else {
                    mParameterContainer.get(i).setVisibility(View.GONE);
                }
            }
            mIgnoreParameterChange--;
        }

        public void makeMosaic() {
            Log.d("Riddle", "Start making mosaic " + mType + " task was: " + mMosaicTask);
            if (mMosaicTask != null) {
                mMosaicTask.cancel(true);
                mMosaicTask = null;
            }
            mMosaicTask = new AsyncTask<Void, Integer, Bitmap>() {
                @Override
                public void onPreExecute() {
                    mWorkingIndicator.setVisibility(View.VISIBLE);
                    mProgress.onProgressUpdate(0);
                    ImageUtil.CACHE.makeReusable(mMosaicBitmap);
                    mMosaicBitmap = null;
                    mMosaicBitmapName = null;
                    mMosaicFile = null;
                    applyShare();
                }

                @Override
                public void onProgressUpdate(Integer... progress) {
                    mProgress.onProgressUpdate(progress[0]);
                }

                public AsyncTask<Void, Integer, Bitmap> getTask() {
                    return this; // to be accessible in the inner nested anonymous class
                }

                @Override
                protected Bitmap doInBackground(Void... params) {
                    Bitmap base = mSelectedBitmap;
                    if (base == null) {
                        return null;
                    }
                    int rows = 0, columns = 0;
                    if (mType == RECT || mType == MULTI_RECT) {
                        //make sure that image dimensions are dividable by the given columns/rows values by resizing
                        rows = (int) Math.round(mValues.get(0));
                        columns = (int) Math.round(mValues.get(1));
                        Dimension targetDim = new Dimension(base.getWidth(), base.getHeight());
                        targetDim.ensureDivisibleBy(columns, rows, true);
                        if (base.getWidth() != targetDim.getWidth() || base.getHeight() != targetDim.getHeight()) {
                            Log.d("Riddle", "Need to resize base image before doing rect or multirect mosaic: rows=" + rows + ", columns=" + columns + " and bitmap was "  + base.getWidth() + "/" + base.getHeight() + " and now is " + targetDim);
                            base = BitmapUtil.resize(base, targetDim.getWidth(), targetDim.getHeight());
                        }
                    }
                    if (isCancelled()) {
                        return null;
                    }
                    Bitmap result = null;
                    Log.d("Riddle", "Really starting to make mosaic " + mType + " task was: " + mMosaicTask + " for bitmap dimension " + base.getWidth() + "/" + base.getHeight());
                    MosaicMaker.ProgressCallback callback = new MosaicMaker.ProgressCallback() {
                        @Override
                        public void onProgressUpdate(int progress) {
                            publishProgress(progress);
                        }

                        @Override
                        public boolean isCancelled() {
                            return getTask().isCancelled();
                        }
                    };
                    try {
                        switch (mType) {
                            case RECT:
                                result = mMosaicMaker.makeRect(base, rows, columns, callback);
                                break;
                            case MULTI_RECT:
                                result = mMosaicMaker.makeMultiRect(base, rows, columns, mValues.get(2), callback);
                                break;
                            case FIXED_LAYER:
                                result = mMosaicMaker.makeFixedLayer(base, (int) Math.round(mValues.get(0)), callback);
                                break;
                        }
                    } catch (OutOfMemoryError error) {
                        // well I know I am catching an error, but since there is this stupid (50mb) restriction we move at the edge of what is possible
                        clear();
                        Toast.makeText(mActivity, R.string.mosaic_generator_memory_error, Toast.LENGTH_LONG).show();
                    }
                    return result;
                }
                @Override
                public void onPostExecute(Bitmap result) {
                    mMosaicTask = null;
                    mWorkingIndicator.setVisibility(View.INVISIBLE);
                    mProgress.onProgressUpdate(0);
                    if (result != null) {
                        StringBuilder name = new StringBuilder();
                        MosaicType.this.addConfigPrefix(name);
                        name.append(mSelectedBitmapName);
                        mMosaicBitmapName = name.toString();
                        onMosaicDone(result);
                    }
                }
            }.execute();
        }

        @Override
        public String toString() {
            return mActivity.getResources().getString(mNameResId);
        }

        public void addConfigPrefix(StringBuilder builder) {
            builder.append(toString());
            for (int i = 0; i < mMinValues.size(); i++) {
                builder.append('_');
                if (mOnlyIntegers.get(i)) {
                    builder.append((int) Math.round(mValues.get(i)));
                } else {
                    builder.append(NumberFormat.getInstance().format(mValues.get(i)));
                }
            }
        }
    }

    private void openSelectBitmap() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");


        Intent chooserIntent = Intent.createChooser(getIntent, mActivity.getResources().getString(R.string.select_mosaic_image_from_gallery));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});

        mActivity.startActivityForResult(chooserIntent, WorkshopView.PICK_IMAGE_FOR_MOSAIC);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || requestCode != WorkshopView.PICK_IMAGE_FOR_MOSAIC || data == null) {
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
        new AsyncTask<Uri, Bitmap, Void>() {

            @Override
            public void onPreExecute() {
                mWorkingIndicator.setVisibility(View.VISIBLE);
                clear();
            }

            @Override
            protected Void doInBackground(Uri... params) {
                String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME};
                for (Uri selectedImages : params) {
                    if (selectedImages == null) {
                        continue;
                    }
                    Cursor cursor = null;
                    try {
                        cursor = mActivity.getContentResolver().query(selectedImages,
                                columns, null, null, null);
                        if (cursor == null) {
                            continue;
                        }
                        cursor.moveToFirst();
                        if (cursor.isAfterLast()) {
                            continue; // empty cursor
                        }

                        int pathIndex = cursor.getColumnIndexOrThrow(columns[0]);
                        int nameIndex = cursor.getColumnIndex(columns[1]);
                        do {
                            String picturePath = cursor.getString(pathIndex);
                            if (picturePath == null) {
                                InputStream input = null;
                                try {
                                    input = mActivity.getContentResolver().openInputStream(selectedImages);
                                } catch (FileNotFoundException e1) {
                                    Log.e("HomeStuff", "File not found when trying to decode bitmap from stream: " + e1);
                                }
                                mSelectedBitmapName = null;
                                if (nameIndex != -1) {
                                    mSelectedBitmapName = cursor.getString(nameIndex);
                                }
                                if (TextUtils.isEmpty(mSelectedBitmapName)) {
                                    mSelectedBitmapName = String.valueOf(System.currentTimeMillis());
                                }

                                publishProgress(ImageUtil.loadBitmap(input, MAX_IMAGE_WIDTH_HEIGHT, MAX_IMAGE_WIDTH_HEIGHT, BitmapUtil.MODE_FIT_NO_GROW));

                            } else {
                                File path = new File(picturePath);
                                mSelectedBitmapName = path.getName();
                                if (TextUtils.isEmpty(mSelectedBitmapName) && nameIndex != -1) {
                                    mSelectedBitmapName = cursor.getString(nameIndex);
                                } else if (TextUtils.isEmpty(mSelectedBitmapName)) {
                                    mSelectedBitmapName = String.valueOf(System.currentTimeMillis());
                                }
                                publishProgress(ImageUtil.loadBitmap(path, MAX_IMAGE_WIDTH_HEIGHT, MAX_IMAGE_WIDTH_HEIGHT, BitmapUtil.MODE_FIT_NO_GROW));

                            }
                            cursor.moveToNext();
                        } while (!cursor.isAfterLast());
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
            public void onProgressUpdate(Bitmap... bitmap) {
                ImageUtil.CACHE.makeReusable(mSelectedBitmap);
                mSelectedBitmap = bitmap[0];
                mWorkingIndicator.setVisibility(View.INVISIBLE);
                onBitmapSelected();
            }

        }.execute(selectedImageUris);

    }

    private void ensureMediaDirectory() {
        if (mMediaDirectory == null) {
            mMediaDirectory = ImageUtil.getMediaDirectory();
            if (mMediaDirectory == null) {
                throw new IllegalStateException("No media directory available");
            }
        }
    }
}
