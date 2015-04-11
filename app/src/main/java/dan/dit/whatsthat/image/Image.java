package dan.dit.whatsthat.image;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import dan.dit.whatsthat.obfuscation.ImageObfuscator;
import dan.dit.whatsthat.riddle.ContentRiddleType;
import dan.dit.whatsthat.riddle.FormatRiddleType;
import dan.dit.whatsthat.riddle.RiddleType;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.storage.ImageTable;
import dan.dit.whatsthat.storage.ImagesContentProvider;
import dan.dit.whatsthat.util.BuildException;
import dan.dit.whatsthat.util.compaction.CompactedDataCorruptException;
import dan.dit.whatsthat.util.compaction.Compacter;
import dan.dit.whatsthat.util.image.BitmapUtil;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * An instance of the Image class references an image which is uniquely identified by its hash.
 * Images are predefined drawables, created by the user or received from other users. Each riddle
 * is created upon an Image object by requesting the corresponding bitmap.
 * Images store metadata like the author, the image name, solution words and preferred and disliked
 * riddle types.
 * Created by daniel on 24.03.15.
 */
public class Image {
    public static final String SHAREDPREFERENCES_FILENAME ="dan.dit.whatsthat.imagePrefs";
    public static final String ORIGIN_IS_THE_APP = "WhatsThat";

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
    private List<RiddleType> mDislikedRiddleTypes; // can be null

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
                cmp.appendData(riddleType.compact());
            }
            cv.put(ImageTable.COLUMN_RIDDLEPREFTYPES, cmp.compact());
        }

        // disliked riddle types
        if (mDislikedRiddleTypes != null) {
            cmp = new Compacter();
            for (RiddleType riddleType : mDislikedRiddleTypes) {
                cmp.appendData(riddleType.compact());
            }
            cv.put(ImageTable.COLUMN_RIDDLEDISLIKEDTYPES, cmp.compact());
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
                builder.addPreferredRiddleType(RiddleType.reconstruct(new Compacter(prefRiddleType)));
            }
        }

        // disliked riddle types
        riddleData = cursor.getString(cursor.getColumnIndexOrThrow(ImageTable.COLUMN_RIDDLEDISLIKEDTYPES));
        if (!TextUtils.isEmpty(riddleData)) {
            for (String prefRiddleType : new Compacter(riddleData)) {
                builder.addDislikedRiddleType(RiddleType.reconstruct(new Compacter(prefRiddleType)));
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

    public int getResourceId() {
        return mResId;
    }

    public File getResourcePath() {
        return mResPath;
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

    public List<RiddleType> getDislikedRiddleTypes() {
        return mDislikedRiddleTypes;
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

    protected Bitmap getOrLoadBitmap(Resources res, int reqWidth, int reqHeight) {
        Bitmap result = mImageData == null ? null : mImageData.get();
        if (result != null) {
            if (reqWidth <= 0 || reqHeight <= 0 || (result.getWidth() == reqWidth && result.getHeight() == reqHeight)) {
                return result; // we got a valid bitmap with required dimensions
            } else if (result.getWidth() >= reqWidth && result.getHeight() >= reqHeight) {
                return BitmapUtil.attemptBitmapScaling(result, reqWidth, reqHeight); // we are bigger than required, scale down
            }
            // else we are smaller than required, try fresh loading and then scaling
        }
        // we need to reload the image
        if (mResId != 0) {
            result = ImageUtil.loadBitmap(res, mResId, reqWidth, reqHeight);
        } else {
            result = ImageUtil.loadBitmap(mResPath, reqWidth, reqHeight);
        }
        mImageData = new WeakReference<>(result);
        return result;
    }

    @Override
    public String toString() {
        return mName + ":" + mHash;
    }

    /**
     * A builder for the Image class that allows recreation from the database or fresh creation
     * of a new image object.
     */
    public static class Builder {
        private Image mImage = new Image();

        public Builder(int resId, File filePath, String name, ImageAuthor author, long timestamp, String hash) {
            mImage.mResId = resId;
            mImage.mResPath = filePath;
            mImage.mName = name;
            mImage.mAuthor = author;
            mImage.mTimestamp = timestamp;
            mImage.mHash = hash;
        }

        public Builder(Context context, int resId, ImageAuthor author) throws BuildException {
            Bitmap image = ImageUtil.loadBitmap(context.getResources(), resId, 0, 0);
            mImage.mResId = resId;
            mImage.mName = ImageUtil.getDrawableNameFromResId(context.getResources(), resId);
            buildBasic(image, author);
        }

        public Builder(File imagePath, ImageAuthor author) throws BuildException {
            Bitmap image = ImageUtil.loadBitmap(imagePath, 0, 0);
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
            mImage.mHash = ImageUtil.getHash(BitmapUtil.extractDataFromBitmap(image));
            addOwnFormatAsPreference(image);
            addOwnContrastAsPreference(image);
        }

        private void addOwnFormatAsPreference(Bitmap bitmap) {
            boolean almostASquare = ImageUtil.isAspectRatioSquareSimilar(bitmap.getWidth(), bitmap.getHeight());
            if (almostASquare) {
                addPreferredRiddleType(FormatRiddleType.FormatSquare.INSTANCE);
            } else if (bitmap.getWidth() > bitmap.getHeight()) {
                addPreferredRiddleType(FormatRiddleType.FormatLandscape.INSTANCE);
            } else {
                addPreferredRiddleType(FormatRiddleType.FormatPortrait.INSTANCE);
            }
        }

        private void addOwnContrastAsPreference(Bitmap bitmap) {
            double contrast = BitmapUtil.calculateContrast(bitmap);
            Log.d("Riddle", "Contrast for " + mImage.mName + ": " + contrast);
            if (BitmapUtil.CONTRAST_STRONG_THRESHOLD > contrast && contrast >= BitmapUtil.CONTRAST_WEAK_THRESHOLD) {
                addPreferredRiddleType(ContentRiddleType.ContentMediumContrast.INSTANCE);
            } else if (BitmapUtil.CONTRAST_STRONG_THRESHOLD <= contrast) {
                addPreferredRiddleType(ContentRiddleType.ContentStrongContrast.INSTANCE);
            } else {
                addDislikedRiddleType(ContentRiddleType.ContentStrongContrast.INSTANCE);
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

        public Builder addDislikedRiddleType(RiddleType type) {
            if (mImage.mDislikedRiddleTypes == null) {
                mImage.mDislikedRiddleTypes = new LinkedList<>();
            }
            if (type != null) {
                mImage.mDislikedRiddleTypes.add(type);
            }
            return this;
        }

        public Builder setDislikedRiddleTypes(List<RiddleType> types) {
            mImage.mDislikedRiddleTypes = types;
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
