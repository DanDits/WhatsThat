package dan.dit.whatsthat.image;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.obfuscation.ImageObfuscator;
import dan.dit.whatsthat.preferences.Tongue;
import dan.dit.whatsthat.riddle.types.ContentRiddleType;
import dan.dit.whatsthat.riddle.types.FormatRiddleType;
import dan.dit.whatsthat.riddle.types.PracticalRiddleType;
import dan.dit.whatsthat.riddle.types.RiddleType;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.storage.ImageTable;
import dan.dit.whatsthat.storage.ImagesContentProvider;
import dan.dit.whatsthat.util.BuildException;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ColorAnalysisUtil;
import dan.dit.whatsthat.util.image.Dimension;
import dan.dit.whatsthat.util.image.ExternalStorage;
import dan.dit.whatsthat.util.image.ImageUtil;
import dan.dit.whatsthat.util.mosaic.data.MosaicTile;

/**
 * An instance of the Image class references an image which is uniquely identified by its hash.
 * Images are predefined drawables, created by the user or received from other users. Each riddle
 * is created upon an Image object by requesting the corresponding bitmap.
 * Images store metadata like the author, the image name, solution words and preferred and refused
 * riddle types.
 * Created by daniel on 24.03.15.
 */
public class Image implements MosaicTile<String> {
    public static final String SHAREDPREFERENCES_FILENAME ="dan.dit.whatsthat.imagePrefs";
    public static final String ORIGIN_IS_THE_APP = "WhatsThat";
    public static final String IMAGES_DIRECTORY_NAME = "images";
    public static final int NO_AVERAGE_COLOR = 0;

    // instead of building everytime on every device this app runs we built once for every new release of images all essential data
    // that takes long time like hash or preference/refused calculation, save it into a simple text file which we then read on first app
    // launch, create Image objects and save to database like before. This saves alot of initial loading time and does not require
    // shipping database files which make collide with different SQL versions or paths for those files or access violations
    private long mTimestamp;

    // the md5 hash of the image which identifies it
    private String mHash;
    private int mResId; // either resId or resPath is specified to be valid and link to a valid image
    private String mRelativePath;
    private String mName; // a name identifying the image
    private ImageAuthor mAuthor;
    private String mOrigin;
    private int mIsObfuscated; // is obfuscated if != 0
    private List<Solution> mSolutions; // always at least one solution needed
    private List<RiddleType> mPreferredRiddleTypes; // can be null
    private List<RiddleType> mRefusedRiddleTypes; // can be null
    private int mAverageColor = NO_AVERAGE_COLOR; // average color of the bitmap

    private Image() {}

    @Override
    public boolean equals(Object other) {
        if (other instanceof Image) {
            return mHash.equals(((Image) other).mHash);
        } else {
            return super.equals(other);
        }
    }

    @Override
    public int hashCode() {
        return mHash.hashCode();
    }


    /**
     * Deletes the given image from the database. This should not commonly be used! It is required
     * though if the image needed to be removed because of copyright issues or if the custom image
     * got deleted from the hard disk. Even if the image is no longer accessible riddles that used this
     * image can be kept.
     * @param context The application context
     * @param hash The md5 hash of the image. The hash can be obtained by ImageUtil.
     * @return True if successfully deleted from the database.
     */
    protected static boolean deleteFromDatabase(Context context, String hash) {
        return context.getContentResolver().delete(ImagesContentProvider.buildImageUri(hash), null, null) > 0;
    }

    protected boolean deleteFromDatabase(Context context) {
        Log.d("Image", "Deleting " + toString() + " from database.");
        return deleteFromDatabase(context, getHash());
    }

    /**
     * Saves this image to the database, overwriting any previous entry with the same image hash.
     * @param context The application context.
     * @return If the image has been saved successfully.
     */
    protected boolean saveToDatabase(Context context) {
        ContentValues cv = new ContentValues();
        cv.put(ImageTable.COLUMN_TIMESTAMP, mTimestamp);
        cv.put(ImageTable.COLUMN_AUTHOR, mAuthor.compact());
        cv.put(ImageTable.COLUMN_HASH, mHash);
        cv.put(ImageTable.COLUMN_NAME, mName);
        cv.put(ImageTable.COLUMN_OBFUSCATION, mIsObfuscated);
        cv.put(ImageTable.COLUMN_ORIGIN, mOrigin);
        cv.put(ImageTable.COLUMN_AVERAGE_COLOR, mAverageColor);

        // one of mResId or mResPath is valid
        if (mResId != 0) {
            cv.put(ImageTable.COLUMN_RESNAME, ImageUtil.getDrawableNameFromResId(context.getResources(), mResId));
        } else {
            cv.put(ImageTable.COLUMN_RESNAME, 0);
            cv.put(ImageTable.COLUMN_SAVELOC, mRelativePath);
        }

        // solutions
        Compacter cmp = new Compacter();
        for (Solution sol : mSolutions) {
            cmp.appendData(sol.compact());
        }
        cv.put(ImageTable.COLUMN_SOLUTIONS, cmp.compact());

        // preferred riddle types
        if (mPreferredRiddleTypes != null) {
            cmp = new Compacter();
            for (RiddleType riddleType : mPreferredRiddleTypes) {
                cmp.appendData(riddleType.getFullName());
            }
            cv.put(ImageTable.COLUMN_RIDDLEPREFTYPES, cmp.compact());
        }

        // refused riddle types
        if (mRefusedRiddleTypes != null) {
            cmp = new Compacter();
            for (RiddleType riddleType : mRefusedRiddleTypes) {
                cmp.appendData(riddleType.getFullName());
            }
            cv.put(ImageTable.COLUMN_RIDDLEREFUSEDTYPES, cmp.compact());
        }

        cv.put(ImagesContentProvider.SQL_INSERT_OR_REPLACE, true);
        return context.getContentResolver().insert(ImagesContentProvider.CONTENT_URI_IMAGE, cv) != null;
    }

    public static Image loadFromCursor(Context context, Cursor cursor) {
        String resName = cursor.getString(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_RESNAME));
        int resId = TextUtils.isEmpty(resName) ? 0 : ImageUtil.getDrawableResIdFromName(context, resName);
        String resPathRaw = cursor.getString(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_SAVELOC));
        String resPath = TextUtils.isEmpty(resPathRaw) ? null : resPathRaw;
        String hash = cursor.getString(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_HASH));
        ImageAuthor author;
        try {
            author = new ImageAuthor(new Compacter(cursor.getString(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_AUTHOR))));
        } catch (CompactedDataCorruptException exp) {
            Log.e("Image", "Failed loading image with hash "  + hash + " from database. ImageAuthor data corrupt.");
            return null;
        }
        String name = cursor.getString(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_NAME));
        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_TIMESTAMP));
        Image.Builder builder = new Image.Builder(resId, resPath, name, author, timestamp, hash);
        builder.setOrigin(cursor.getString(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_ORIGIN)));
        builder.setObfuscation(cursor.getInt(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_OBFUSCATION)));
        builder.setAverageColor(cursor.getInt(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_AVERAGE_COLOR)));

        // solutions
        for (String sol : new Compacter(cursor.getString(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_SOLUTIONS)))) {
            try {
                builder.addSolution(new Solution(new Compacter(sol)));
            } catch (CompactedDataCorruptException exp) {
                Log.e("Image", "Problem loading image with hash "  + hash + " from database. Failed to load solution " + sol);
                return null;
            }
        }

        // preferred riddle types
        String riddleData = cursor.getString(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_RIDDLEPREFTYPES));
        if (!TextUtils.isEmpty(riddleData)) {
            for (String prefRiddleType : new Compacter(riddleData)) {
                builder.addPreferredRiddleType(RiddleType.getInstance(prefRiddleType));
            }
        }

        // refused riddle types
        riddleData = cursor.getString(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_RIDDLEREFUSEDTYPES));
        if (!TextUtils.isEmpty(riddleData)) {
            for (String prefRiddleType : new Compacter(riddleData)) {
                builder.addRefusedRiddleType(PracticalRiddleType.getInstance(prefRiddleType));
            }
        }

        Image result = null;
        try {
            result = builder.build(context);
        } catch (BuildException be) {
            Log.e("Image", "Failed loading image with hash "  + hash + " from database. Building failed.");
        }
        return result;
    }

    public ImageAuthor getAuthor() {
        return mAuthor;
    }

    public String getHash() {
        return mHash;
    }

    public List<Solution> getSolutions() {
        return mSolutions;
    }

    public List<RiddleType> getPreferredRiddleTypes() {
        return mPreferredRiddleTypes;
    }

    public List<RiddleType> getRefusedRiddleTypes() {
        return mRefusedRiddleTypes;
    }

    public String getName() {
        return mName;
    }

    public String getOrigin() {
        return mOrigin;
    }

    private Bitmap loadBitmapExecute(Resources res, int reqWidth, int reqHeight, boolean enforceDimension) {
        Bitmap result = null;
        if (mRelativePath != null) {
            String path = ExternalStorage.getExternalStoragePathIfMounted(IMAGES_DIRECTORY_NAME);
            if (path != null) {
                File imagePath = new File(path + "/" + mOrigin + "/" + mRelativePath);
                result = ImageUtil.loadBitmap(imagePath, reqWidth, reqHeight, enforceDimension);
            }
        } else if (mResId != 0) {
            result = ImageUtil.loadBitmap(res, mResId, reqWidth, reqHeight, enforceDimension);
        } else {
            Log.e("Image", "Trying to load bitmap without relative path or res id! Use other method!");
        }
        return result;
    }

    // will fail when loading images only built by being parsed as they don't have a valid resource id or name
    public Bitmap loadBitmap(Resources res, Dimension reqDimension, boolean enforceDimension) {
        int reqWidth = reqDimension.getWidth();
        int reqHeight = reqDimension.getHeight();
        Bitmap result;
        if (mIsObfuscated == 0) {
            // not obfuscated, we can optimize the loading
            return loadBitmapExecute(res, reqWidth, reqHeight, enforceDimension);
        } else {
            // we first need to deobfuscate the image
            result = loadBitmapExecute(res, 0, 0, false);
            if (result != null) {
                result = ImageObfuscator.restoreImage(result);
                if (result != null) {
                    result = BitmapUtil.attemptBitmapScaling(result, reqWidth, reqHeight, enforceDimension);
                }
            }
        }
        return result;
    }

    public Bitmap loadBitmap(Context context, Dimension reqDimension, boolean enforceDimension) {
        if (mResId == 0 && mRelativePath == null) {
            // try to use name to get a image resource id
            Log.d("Image", "No res path and no res id for name " + mName);
            mResId = ImageUtil.getDrawableResIdFromName(context, mName);
            if (mResId != 0) {
                return ImageUtil.loadBitmap(context.getResources(), mResId, reqDimension.getWidth(), reqDimension.getHeight(), enforceDimension);
            } else {
                Log.e("Image", "Failed retrieving res id for image " + mName + ": did not load bitmap.");
                return null;
            }
        } else {
            return loadBitmap(context.getResources(), reqDimension, enforceDimension);
        }
    }

    @Override
    public String toString() {
        return mName + ":" + mHash;
    }

    public @NonNull
    Solution getSolution(Tongue tongue) {
        for (Solution sol : mSolutions) {
            if (sol.getTongue().equals(tongue)) {
                return sol;
            }
        }
        // didn't find a solution in the wanted tongue, check the tongue's parent
        Tongue parent = tongue.getParentTongue();
        if (parent != null) {
            for (Solution sol : mSolutions) {
                if (sol.getTongue().equals(parent)) {
                    return sol;
                }
            }
        }
        // still didn't find a solution, if we wanted something else than english lets try to get the english language else just take any solution
        if (!Tongue.ENGLISH.equals(tongue)) {
            return getSolution(Tongue.ENGLISH); // will either be the english solution or the first solution in the list
        }
        return mSolutions.get(0);
    }

    public String getRelativePath() {
        return mRelativePath;
    }

    public int getObfuscation() {
        return mIsObfuscated;
    }

    public int getAverageColor() {
        return mAverageColor;
    }

    @Override
    @NonNull
    public String getSource() {
        return mHash;
    }

    @Override
    public int getAverageARGB() {
        return mAverageColor;
    }

    /**
     * A builder for the Image class that allows recreation from the database or fresh creation
     * of a new image object.
     */
    protected static class Builder {
        private Image mImage = new Image();

        public Builder() {
            mImage.mTimestamp = System.currentTimeMillis();
        }

        public Builder(int resId, String relativePath, String name, ImageAuthor author, long timestamp, String hash) {
            mImage.mResId = resId;
            mImage.mRelativePath = relativePath;
            mImage.mName = name;
            mImage.mAuthor = author;
            mImage.mTimestamp = timestamp;
            mImage.mHash = hash;
        }

        private static final Dimension EMPTY_DIMENSION = new Dimension(0, 0);
        private void calculateHashAndPreferences(Context context) {
            Bitmap image = mImage.loadBitmap(context, EMPTY_DIMENSION, false);
            if (image != null) {
                mImage.mHash = ImageUtil.getHash(BitmapUtil.extractDataFromBitmap(image));
                calculateAverageColor(image);
                addOwnFormatAsPreference(image);
                addOwnContrastAsPreference(image);
                addOwnGreynessAsPreference(image);
            }
        }

        private void calculateAverageColor(Bitmap image) {
            mImage.mAverageColor = ColorAnalysisUtil.getAverageColor(image);
        }

        private void addOwnGreynessAsPreference(Bitmap image) {
            double greyness = BitmapUtil.calculateGreyness(image);
            if (greyness <= BitmapUtil.GREYNESS_STRONG_THRESHOLD) {
                addPreferredRiddleType(ContentRiddleType.GREY_VERY_INSTANCE);
            } else if (greyness > BitmapUtil.GREYNESS_MEDIUM_THRESHOLD) {
                addPreferredRiddleType(ContentRiddleType.GREY_LITTLE_INSTANCE);
            } else {
                addPreferredRiddleType(ContentRiddleType.GREY_MEDIUM_INSTANCE);
            }
        }

        public void setResourceName(Context context, String resourceName) {
            mImage.mName = resourceName;
            mImage.mResId = ImageUtil.getDrawableResIdFromName(context, resourceName);
        }

        public Builder setRelativeImagePath(String relativePath) {
            if (TextUtils.isEmpty(relativePath)) {
                return this;
            }
            mImage.mName = relativePath;
            mImage.mRelativePath = relativePath;
            return this;
        }

        public void setAuthor(ImageAuthor author) {
            mImage.mAuthor = author;
        }

        public Builder setHash(String hash) {
            if (TextUtils.isEmpty(hash)) {
                return this;
            }
            mImage.mHash = hash;
            return this;
        }

        private void addOwnFormatAsPreference(Bitmap bitmap) {
            boolean almostASquare = ImageUtil.isAspectRatioSquareSimilar(bitmap.getWidth(), bitmap.getHeight());
            if (almostASquare) {
                addPreferredRiddleType(FormatRiddleType.SQUARE_INSTANCE);
            } else if (bitmap.getWidth() > bitmap.getHeight()) {
                addPreferredRiddleType(FormatRiddleType.LANDSCAPE_INSTANCE);
            } else {
                addPreferredRiddleType(FormatRiddleType.PORTRAIT_INSTANCE);
            }
        }

        private void addOwnContrastAsPreference(Bitmap bitmap) {
            double contrast = BitmapUtil.calculateContrast(bitmap);
            if (BitmapUtil.CONTRAST_STRONG_THRESHOLD > contrast && contrast >= BitmapUtil.CONTRAST_WEAK_THRESHOLD) {
                addPreferredRiddleType(ContentRiddleType.CONTRAST_MEDIUM_INSTANCE);
            } else if (BitmapUtil.CONTRAST_STRONG_THRESHOLD <= contrast) {
                addPreferredRiddleType(ContentRiddleType.CONTRAST_STRONG_INSTANCE);
            } else {
                addPreferredRiddleType(ContentRiddleType.CONTRAST_WEAK_INSTANCE);
            }
        }


        public Builder setOrigin(String origin) {
            mImage.mOrigin = origin;
            return this;
        }

        public Builder setObfuscation(int obfuscation) {
            mImage.mIsObfuscated = obfuscation;
            return this;
        }

        public Builder setObfuscation(String obfuscation) {
            if (!TextUtils.isEmpty(obfuscation)) {
                try {
                    mImage.mIsObfuscated = Integer.parseInt(obfuscation);
                } catch (NumberFormatException nfe) {
                    mImage.mIsObfuscated = ImageObfuscator.IS_OBFUSCATED_HINT;
                }
            }
            return this;
        }

        public Builder setAverageColor(String averageColor) {
            if (!TextUtils.isEmpty(averageColor)) {
                try {
                    setAverageColor(Integer.parseInt(averageColor));
                } catch (NumberFormatException nfe) {
                    setAverageColor(NO_AVERAGE_COLOR);
                }
            }
            return this;
        }

        public Builder addSolution(Solution solution) {
            if (mImage.mSolutions == null) {
                mImage.mSolutions = new LinkedList<>();
            }
            if (solution != null) {
                mImage.mSolutions.add(solution);
            }
            return this;
        }

        public Builder setSolutions(List<Solution> solutions) {
            mImage.mSolutions = solutions;
            return this;
        }

        public Builder addPreferredRiddleType(RiddleType type) {
            if (mImage.mPreferredRiddleTypes == null) {
                mImage.mPreferredRiddleTypes = new LinkedList<>();
            }
            if (type != null) {
                mImage.mPreferredRiddleTypes.add(type);
            }
            return this;
        }

        public Builder setPreferredRiddleTypes(List<RiddleType> types) {
            mImage.mPreferredRiddleTypes = types;
            return this;
        }

        public Builder addRefusedRiddleType(PracticalRiddleType type) {
            if (mImage.mRefusedRiddleTypes == null) {
                mImage.mRefusedRiddleTypes = new LinkedList<>();
            }
            if (type != null) {
                mImage.mRefusedRiddleTypes.add(type);
            }
            return this;
        }

        public Builder setRefusedRiddleTypes(List<RiddleType> types) {
            mImage.mRefusedRiddleTypes = types;
            return this;
        }

        public Image build(Context context) throws BuildException {
            if (mImage.mAuthor == null) {
                throw new BuildException("Source: " + mImage.mName).setMissingData("Image", "Author");
            }
            if (TextUtils.isEmpty(mImage.mOrigin)) {
                mImage.mOrigin = ORIGIN_IS_THE_APP;
            }
            if (TextUtils.isEmpty(mImage.mRelativePath) && mImage.mResId == 0) {
                throw new BuildException("Source: " + mImage.mName).setMissingData("Image","ResPath or resId");
            }
            if (TextUtils.isEmpty(mImage.mHash)) {
                Log.d("Image", "Building image with no hash yet: " + mImage.mName);
                calculateHashAndPreferences(context);
            }
            if (mImage.mSolutions == null || mImage.mSolutions.isEmpty()) {
                throw new BuildException("Source: " + mImage.mName).setMissingData("Image", "Solutions");
            }
            if (TextUtils.isEmpty(mImage.mHash)) {
                throw new BuildException("Source: " + mImage.mName).setMissingData("Image", "Hash");
            }
            return mImage;
        }

        public void setAverageColor(int averageColor) {
            mImage.mAverageColor = averageColor;
        }
    }
}
