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
import java.lang.ref.WeakReference;
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
import dan.dit.whatsthat.util.image.Dimension;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * An instance of the Image class references an image which is uniquely identified by its hash.
 * Images are predefined drawables, created by the user or received from other users. Each riddle
 * is created upon an Image object by requesting the corresponding bitmap.
 * Images store metadata like the author, the image name, solution words and preferred and refused
 * riddle types.
 * Created by daniel on 24.03.15.
 */
public class Image {
    public static final String SHAREDPREFERENCES_FILENAME ="dan.dit.whatsthat.imagePrefs";
    public static final String ORIGIN_IS_THE_APP = "WhatsThat";

    // instead of building everytime on every device this app runs we built once for every new release of images all essential data
    // that takes long time like hash or preference/refused calculation, save it into a simple text file which we then read on first app
    // launch, create Image objects and save to database like before. This saves alot of initial loading time and does not require
    // shipping database files which make collide with different SQL versions or paths for those files or access violations
    private long mTimestamp;

    // the md5 hash of the image which identifies it
    private String mHash;
    private int mResId; // either resId or resPath is specified to be valid and link to a valid image
    private File mResPath;
    private WeakReference<Bitmap> mImageData; // can hold a reference to the image itself, but is not required
    private String mName; // a name identifying the image
    private ImageAuthor mAuthor;
    private String mOrigin;
    private int mIsObfuscated; // is obfuscated if != 0
    private List<Solution> mSolutions; // always at least one solution needed
    private List<RiddleType> mPreferredRiddleTypes; // can be null
    private List<RiddleType> mRefusedRiddleTypes; // can be null

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

    public boolean isObfuscated() {
        return mIsObfuscated != 0;
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

        // one of mResId or mResPath is valid
        if (mResId != 0) {
            cv.put(ImageTable.COLUMN_RESNAME, ImageUtil.getDrawableNameFromResId(context.getResources(), mResId));
        } else {
            cv.put(ImageTable.COLUMN_RESNAME, 0);
            cv.put(ImageTable.COLUMN_SAVELOC, mResPath.getAbsolutePath());
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

    protected static String[] loadAvailableHashes(Context context) {
        Cursor cursor = context.getContentResolver().query(ImagesContentProvider.CONTENT_URI_IMAGE,
                new String[] {ImageTable.COLUMN_HASH}, null, null, ImageTable.COLUMN_TIMESTAMP + " ASC");

        String[] hashes = new String[cursor.getCount()];
        cursor.moveToFirst();

        int index = 0;
        while (!cursor.isAfterLast()) {
            hashes[index++]=cursor.getString(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_HASH));
            cursor.moveToNext();
        }
        cursor.close();
        return hashes;
    }

    protected static Image loadFromDatabase(Context context, String hash) {
        Cursor cursor = context.getContentResolver().query(ImagesContentProvider.buildImageUri(hash), ImageTable.ALL_COLUMNS, null, null, null);
        cursor.moveToFirst();
        if (cursor.isAfterLast()) {
            Log.e("Image", "Failed loading image with hash "  + hash + " from database. Cursor empty.");
            return null;
        }
        Image curr = loadFromCursor(context, cursor);
        cursor.close();
        return curr;
    }

    public static Image loadFromCursor(Context context, Cursor cursor) {
        String resName = cursor.getString(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_RESNAME));
        int resId = TextUtils.isEmpty(resName) ? 0 : ImageUtil.getDrawableResIdFromName(context, resName);
        String resPathRaw = cursor.getString(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_SAVELOC));
        File resPath = TextUtils.isEmpty(resPathRaw) ? null : new File(resPathRaw);
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
            result = builder.build();
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

    public Bitmap getBitmap() {
        return mImageData == null ? null : mImageData.get();
    }

    public Bitmap getOrLoadBitmap(Context context, Dimension reqDimension, boolean enforceDimension) {
        int reqWidth = reqDimension.getWidth();
        int reqHeight = reqDimension.getHeight();
        Bitmap result = mImageData == null ? null : mImageData.get();
        if (result != null) {
            if (reqWidth <= 0 || reqHeight <= 0 || (result.getWidth() == reqWidth && result.getHeight() == reqHeight)) {
                return result; // we got a valid bitmap with required dimensions
            } else if (result.getWidth() >= reqWidth && result.getHeight() >= reqHeight) {
                return BitmapUtil.attemptBitmapScaling(result, reqWidth, reqHeight, enforceDimension); // we are bigger than required, scale down
            }
            // else we are smaller than required, try fresh loading and then scaling
        }
        // we need to reload the image
        if (mResPath != null) {
            result = ImageUtil.loadBitmap(mResPath, reqWidth, reqHeight, enforceDimension);
        } else if (mResId != 0) {
            result = ImageUtil.loadBitmap(context.getResources(), mResId, reqWidth, reqHeight, enforceDimension);
        } else {
            // try to use name to get a image resource id
            Log.d("Image", "No res path and no res id for name " + mName);
            mResId = ImageUtil.getDrawableResIdFromName(context, mName);
            if (mResId != 0) {
                result = ImageUtil.loadBitmap(context.getResources(), mResId, reqWidth, reqHeight, enforceDimension);
            }
        }
        mImageData = new WeakReference<>(result);
        return result;
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
        if (!tongue.equals(Tongue.ENGLISH)) {
            Solution lastTry = getSolution(Tongue.ENGLISH);
            if (lastTry != null) {
                return lastTry;
            }
        }
        return mSolutions.get(0);
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

        public Builder(int resId, File filePath, String name, ImageAuthor author, long timestamp, String hash) {
            mImage.mResId = resId;
            mImage.mResPath = filePath;
            mImage.mName = name;
            mImage.mAuthor = author;
            mImage.mTimestamp = timestamp;
            mImage.mHash = hash;
        }

        public Builder(Context context, int resId, ImageAuthor author) throws BuildException {
            Bitmap image = ImageUtil.loadBitmap(context.getResources(), resId, 0, 0, true);
            mImage.mResId = resId;
            mImage.mName = ImageUtil.getDrawableNameFromResId(context.getResources(), resId);
            buildBasic(image, author);
        }

        public Builder(File imagePath, ImageAuthor author) throws BuildException {
            Bitmap image = ImageUtil.loadBitmap(imagePath, 0, 0, true);
            mImage.mResPath = imagePath;
            mImage.mName = imagePath.getName();
            buildBasic(image, author);
        }

        public Builder(Resources res, int resId, Bitmap image, ImageAuthor author) throws BuildException {
            mImage.mResId = resId;
            if (resId == 0) {
                throw new BuildException().setMissingData("Image", "Valid resource id.");
            }
            mImage.mName = ImageUtil.getDrawableNameFromResId(res, resId);

            buildBasic(image, author);
        }

        public Builder(File imagePath, Bitmap image, ImageAuthor author) throws BuildException {
            mImage.mResPath = imagePath;
            if (imagePath == null) {
                throw new BuildException().setMissingData("Image", "Path to image.");
            }
            mImage.mName = imagePath.getName();
            buildBasic(image, author);
        }

        private void buildBasic(Bitmap image, ImageAuthor author) throws BuildException {
            if (image == null) {
                throw new BuildException().setMissingData("Image", "Bitmap image");
            }
            // init timestamp, hash, weak ref, author
            // calculating hash, loading image, and calculations with the bitmap take quite some time
            mImage.mTimestamp = System.currentTimeMillis();
            mImage.mAuthor = author;
            mImage.mImageData = new WeakReference<>(image);
            calculateHashAndPreferences(image);
        }

        public void calculateHashAndPreferences(Bitmap image) {
            mImage.mHash = ImageUtil.getHash(BitmapUtil.extractDataFromBitmap(image));
            addOwnFormatAsPreference(image);
            addOwnContrastAsPreference(image);
            addOwnGreynessAsPreference(image);
        }

        private void addOwnGreynessAsPreference(Bitmap image) {
            double greyness = BitmapUtil.calculateGreyness(image);
            Log.d("Image", "Image greyness : " + greyness + " for image " + mImage.mName);
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

        public void setAuthor(ImageAuthor author) {
            mImage.mAuthor = author;
        }

        public void setHash(String hash) {
            mImage.mHash = hash;
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

        public Builder setIsObfuscated() {
            mImage.mIsObfuscated = ImageObfuscator.IS_OBFUSCATED_HINT;
            return this;
        }

        public Builder setObfuscation(int obfuscation) {
            mImage.mIsObfuscated = obfuscation;
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

        public Image build() throws BuildException {
            if (mImage.mAuthor == null) {
                throw new BuildException().setMissingData("Image", "Author");
            }
            if (mImage.mOrigin == null) {
                mImage.mOrigin = ORIGIN_IS_THE_APP;
            }
            if (mImage.mSolutions == null || mImage.mSolutions.isEmpty()) {
                throw new BuildException().setMissingData("Image", "Solutions");
            }
            if (TextUtils.isEmpty(mImage.mHash)) {
                throw new BuildException().setMissingData("Image", "Hash");
            }
            return mImage;
        }
    }
}
