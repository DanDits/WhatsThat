package dan.dit.whatsthat.util.image;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * An image utiliy class that can save an image to a file and 
 * do other helpful stuff for image reconstruction.
 * @author Daniel
 *
 */
public final class ImageUtil {
    private static final String IMAGE_FILE_PREFIX = "WTH_";
    private static final String IMAGE_FILE_EXTENSION = ".png";
	private static final String TAG = "ImageUtil";
    private static final double SIMILARITY_SCALING_THRESHOLD = 0.5; // 0 would mean only exactly the same aspect ratio

    private ImageUtil() {
	}

    public static boolean isValidObfuscatedImageName(String imageName) {
        return !TextUtils.isEmpty(imageName) && imageName.endsWith(IMAGE_FILE_EXTENSION) && imageName.startsWith(IMAGE_FILE_PREFIX);
    }

	/**
	 * Saves the given image to the given file in png format.
     * @param context Any application context.
	 * @param image The image to be saved.
	 * @param fileName null-ok; The basic part of the output file name.
	 * @return <code>true</code> if the image was successfully saved,
	 * if context or image parameter is <code>null</code> or there was an error accessing the external storage
     * or saving the file this returns <code>false</code>.
	 */
	public static boolean saveToFile(Context context, Bitmap image, String fileName) {
		// create new File for the new Image
        if (image == null || context == null) {
            return false;
        }
        File pictureFile = getOutputMediaFile(context, fileName);
        if (pictureFile == null) {
            Log.e(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return false;
        }
        boolean success = false;
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            success=image.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Error accessing file: " + e.getMessage());
        }
        return success;
    }

    public static File getMediaDir(Context context) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + context.getApplicationContext().getPackageName()
                + "/Files");
        return mediaStorageDir;
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(Context context, String pImageName){
        File mediaStorageDir = getMediaDir(context);
        if (mediaStorageDir == null) {
            return null; // external storage not available
        }
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        File mediaFile = null;
        String imageName = pImageName;
        String suffix = "";
        if (!TextUtils.isEmpty(imageName) && !imageName.toLowerCase().endsWith(".png")) {
            suffix = IMAGE_FILE_EXTENSION; // a 'valid' name, only missing the extension
        } else if (TextUtils.isEmpty(imageName)) {
            String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
            suffix = timeStamp + IMAGE_FILE_EXTENSION; // empty name given
        } // imageName=".png" still possible at this point if given image name was ".png"
        int counter = 0;
        do {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + IMAGE_FILE_PREFIX + (counter == 0 ? "" : (counter + "_")) + imageName + suffix);
            counter++;
        } while (mediaFile.exists() && counter < Integer.MAX_VALUE);
        return mediaFile;
    }


    /**
     * Returns a MD5 hash of the given data.
     * @param data Data to hash, not null.
     * @return The MD5 hash or null on error.
     */
    public static String getHash(byte[] data) {
        if (data == null) {
            return null;
        }
        MessageDigest m = null;

        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e("Util", "NoSuchAlgorithm HD5!");
            return null;
        }
        m.reset();
        m.update(data, 0, data.length);
        return new BigInteger(1, m.digest()).toString(16); // length 32 in hex format

    }


    // convertDpToPixel(25f, metrics) -> (25dp converted to pixels)
    public static float convertDpToPixel(float dp, DisplayMetrics metrics){
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    public static float convertDpToPixel(float dp, int screenDensity) {
        return dp * screenDensity / 160.f;
    }

    /*public static float convertPixelsToDp(float px, DisplayMetrics metrics){
        float dp = px / (metrics.densityDpi / 160.f);
        return dp;
    }*/

    // Code from http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static boolean areAspectRatiosSimilar(int width1, int height1, int width2, int height2) {
        // using absolute differences of the aspect ratios where height and width are interchangable
        // similarity of 0 means that the aspect ratios are equal
        // examples: 9:16 to 3:4 => similarity = 0.583 , ok'ish
        //           5:3 to 2:4 => similarity = 3.033 , bad
        //           16:9 to 15:10 => similarity = 0.341 , good
        double similarity = Math.abs(height1 / ((double) width1) * width2 / ((double) height2) - 1.0)
                + Math.abs(width1 / ((double) height1) * height2 / ((double) width2) - 1.0);
        return similarity <= SIMILARITY_SCALING_THRESHOLD;
    }

    public static boolean isAspectRatioSquareSimilar(int width, int height) {
        // If r = width/height, T = SIMILARITY_SCALING_THRESHOLD this is true if (sqrt(T*T+4)/2 - T/2) <= r <= (T/2 + sqrt(T*T+4)/2)
        return areAspectRatiosSimilar(width, height, 1, 1);
    }

    /**
     * Loads the bitmap specified by the given resource id. A negative value or zero for the required
     * height or width will result in loading the unscaled original image.
     * @param res Resources of the context.
     * @param resId The resource id of the bitmap.
     * @param reqWidth The required width of the image to load.
     * @param reqHeight The required height of the image to load.
     * @return A bitmap that will approximate the given dimensions at its best or null if no bitmap could be loaded.
     */
    public static Bitmap loadBitmap(Resources res, int resId, int reqWidth, int reqHeight, boolean enforceDimension) {
        if (reqWidth <= 0 || reqHeight <= 0) {
            // load unscaled image
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeResource(res, resId, options);
        }
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap result = BitmapFactory.decodeResource(res, resId, options);
        return BitmapUtil.attemptBitmapScaling(result, reqWidth, reqHeight, enforceDimension);
    }

    public static Bitmap loadBitmapStrict(Resources res, int resId, int width, int height) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, width, height);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap result = BitmapFactory.decodeResource(res, resId, options);
        return Bitmap.createScaledBitmap(result, width, height, true);
    }

    /**
     * Loads the bitmap specified by the given path.
     * @param path The path to the image.
     * @param reqWidth
     *@param reqHeight @return A bitmap or nul lif no bitmap could be loaded or is not found.
     */
    public static Bitmap loadBitmap(File path, int reqWidth, int reqHeight, boolean enforceDimension) {
        if (reqWidth <= 0 || reqHeight <= 0) {
            // load unscaled image
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(path.getAbsolutePath(), options);
        }
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path.getAbsolutePath(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap result = BitmapFactory.decodeFile(path.getAbsolutePath(), options);
        return BitmapUtil.attemptBitmapScaling(result, reqWidth, reqHeight, enforceDimension);
    }

    /**
     * Gets the resource id corresponding to the drawable with the given name.
     * @param context A context.
     * @param drawableName The drawable resource name.
     * @return The resource id to the drawable with the given name.
     */
    public static int getDrawableResIdFromName(Context context, String drawableName) {
        return context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
    }

    /**
     * Gets the resource name corresponding to the drawable with the given resource id. See
     * getDrawableResIdFromName(Context,String).
     * @param res A resource.
     * @param resId The resource id of the drawable.
     * @return The entry name of the resource.
     */
    public static String getDrawableNameFromResId(Resources res, int resId) {
        return res.getResourceEntryName(resId);
    }
	
	//Library: https://code.google.com/p/pngj/wiki/Overview
	//OR WITH STANDARD JAVA FOR WRITING PNG METADATA
	/*RenderedImage image = getMyImage();         
	Iterator<ImageWriter> iterator = ImageIO.getImageWritersBySuffix( "png" );

	if(!iterator.hasNext()) throw new Error( "No image writer for PNG" );

	ImageWriter imagewriter = iterator.next();
	ByteArrayOutputStream bytes = new ByteArrayOutputStream();
	imagewriter.setOutput( ImageIO.createImageOutputStream( bytes ) ); 

	// Create & populate metadata
	PNGMetadata metadata = new PNGMetadata();
	// see http://www.w3.org/TR/PNG-Chunks.html#C.tEXt for standardized keywords
	metadata.tEXt_keyword.add( "Title" );
	metadata.tEXt_text.add( "Mandelbrot" );
	metadata.tEXt_keyword.add( "Comment" );
	metadata.tEXt_text.add( "..." );
	metadata.tEXt_keyword.add( "MandelbrotCoords" ); // custom keyword
	metadata.tEXt_text.add( fractal.getCoords().toString() );           

	// Render the PNG to memory
	IIOImage iioImage = new IIOImage( image, null, null );
	iioImage.setMetadata( metadata ); // Attach the metadata
	imagewriter.write( null, iioImage, null );
	 writer.dispose();*/
	
}
