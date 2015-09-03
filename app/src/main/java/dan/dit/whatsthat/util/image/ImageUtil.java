package dan.dit.whatsthat.util.image;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    /**
     * Regularly loads the bitmap so that it fits inside the required
     * dimensions but does not necessarily gets resized to fit one dimension exactly
     */
    public static final int LOAD_MODE_FIT_NO_RESIZE = 0;

    /**
     * Loads the bitmap so that it fits inside the required
     * dimensions and resizes the image so that one dimension fits exactly
     * and the aspect ratio is (almost) the same to the original image.
     */
    public static final int LOAD_MODE_FIT_INSIDE = 1;

    /**
     * Loads the bitmap so that it fits inside the required
     * dimensions and resizes the image so that both dimensions fit exactly.
     */
    public static final int LOAD_MODE_FIT_EXACT = 2;

    private static final String IMAGE_FILE_PREFIX = "WTH_";
    private static final String IMAGE_FILE_EXTENSION = ".png";
    private static final double SIMILARITY_SCALING_THRESHOLD = 0.5; // 0 would mean only exactly the same aspect ratio
    private static final String MEDIA_DIRECTORY_NAME = "Media";
    private static MessageDigest DIGEST;

    private ImageUtil() {
	}

    public static boolean isValidObfuscatedImageName(String imageName) {
        return !TextUtils.isEmpty(imageName) && imageName.endsWith(IMAGE_FILE_EXTENSION) && imageName.startsWith(IMAGE_FILE_PREFIX);
    }

    public static boolean saveToFile(Bitmap image, File target, Bitmap.CompressFormat format, int compression) {
        if (image == null || target == null) {
            return false;
        }
        if (format == null) {
            // try to set format by file extension, else take .png but do not rename target file
            String name = target.getName();
            String pathLowerCase = name.toLowerCase();
            if (pathLowerCase.endsWith(".png")) {
                format = Bitmap.CompressFormat.PNG;
            } else if (pathLowerCase.endsWith(".jpg") || pathLowerCase.endsWith(".jpeg")) {
                format = Bitmap.CompressFormat.JPEG;
            } else if (pathLowerCase.endsWith(".webp")) {
                format = Bitmap.CompressFormat.WEBP;
            } else {
                format = Bitmap.CompressFormat.PNG;
            }
        }
        if (compression < 0) {
            compression = 0;
        }
        if (compression > 100) {
            compression = 100;
        }
        boolean success = false;
        try {
            FileOutputStream fos = new FileOutputStream(target);
            success=image.compress(format, compression, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e("Image", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e("Image", "Error accessing file: " + e.getMessage());
        }
        return success;
    }

	/**
	 * Saves the given image to the given file in png format.
	 * @param image The image to be saved.
	 * @param fileName null-ok; The basic part of the output file name.
	 * @return <code>true</code> if the image was successfully saved,
	 * if context or image parameter is <code>null</code> or there was an error accessing the external storage
     * or saving the file this returns <code>false</code>.
	 */
	public static boolean saveToMediaFile(Bitmap image, String fileName) {
		// create new File for the new Image
        if (image == null) {
            return false;
        }
        File pictureFile = getOutputMediaFile(fileName);
        if (pictureFile == null) {
            Log.e("Image",
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return false;
        }
        return saveToFile(image, pictureFile, Bitmap.CompressFormat.PNG, 100);
    }

    /* Create a File for saving a png image */
    private static File getOutputMediaFile(String pImageName){
        String path = ExternalStorage.getExternalStoragePathIfMounted(MEDIA_DIRECTORY_NAME);
        if (path == null) {
            return null; // external storage not available
        }
        File mediaStorageDir = new File(path);

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.mkdirs() && !mediaStorageDir.isDirectory()){
            return null;
        }
        // Create a media file name
        File mediaFile;
        String suffix = "";
        if (!TextUtils.isEmpty(pImageName) && !pImageName.toLowerCase().endsWith(IMAGE_FILE_EXTENSION)) {
            suffix = IMAGE_FILE_EXTENSION; // a 'valid' name, only missing the extension
        } else if (TextUtils.isEmpty(pImageName)) {
            String timeStamp = SimpleDateFormat.getDateTimeInstance().format(new Date());
            suffix = timeStamp + IMAGE_FILE_EXTENSION; // empty name given
        } // imageName=".png" still possible at this point if given image name was ".png"
        int counter = 0;
        do {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + (counter == 0 ? "" : (counter + "_")) + pImageName + suffix);
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

        if (DIGEST == null) {
            try {
                DIGEST = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                Log.e("Util", "NoSuchAlgorithm HD5!");
                return null;
            }
        }
        DIGEST.reset();
        DIGEST.update(data, 0, data.length);
        return new BigInteger(1, DIGEST.digest()).toString(16); // length 32 in hex format

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

    public static Bitmap loadBitmap(InputStream input, int reqWidth, int reqHeight, int mode) {
        if (reqWidth <= 0 || reqHeight <= 0) {
            // load unscaled image
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeStream(input, null, options);
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        if (input.markSupported()) {
            // First decode with inJustDecodeBounds=true to check dimensions
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            try {
                input.reset();
            } catch (IOException ioe) {
                Log.e("Image", "Error resetting input stream when decoding image: " + ioe);
                return null;
            }
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        }
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap result =  BitmapFactory.decodeStream(input, null, options);
        if (mode == LOAD_MODE_FIT_NO_RESIZE) {
            return result;
        }
        return BitmapUtil.attemptBitmapScaling(result, reqWidth, reqHeight, mode == LOAD_MODE_FIT_EXACT);
    }

    public static Bitmap loadBitmap(Resources res, int resId, int reqWidth, int reqHeight, int mode) {
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
        if (mode == LOAD_MODE_FIT_NO_RESIZE) {
            return result;
        }
        return BitmapUtil.attemptBitmapScaling(result, reqWidth, reqHeight, mode == LOAD_MODE_FIT_EXACT);
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
        return loadBitmap(res, resId, reqWidth, reqHeight, enforceDimension ? LOAD_MODE_FIT_EXACT : LOAD_MODE_FIT_INSIDE);
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
     * @param reqWidth The maximum width.
     * @param reqHeight THe maximum height.
     * @return A bitmap or nul lif no bitmap could be loaded or is not found.
     */
    public static Bitmap loadBitmap(File path, int reqWidth, int reqHeight, boolean enforceDimension) {
        return loadBitmap(path, reqWidth, reqHeight, enforceDimension ? LOAD_MODE_FIT_EXACT : LOAD_MODE_FIT_INSIDE);
    }

    public static Bitmap loadBitmap(File path, int reqWidth, int reqHeight, int mode) {
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
        if (result == null) {
            return null;
        }
        if (mode == LOAD_MODE_FIT_NO_RESIZE) {
            return result;
        }
        return BitmapUtil.attemptBitmapScaling(result, reqWidth, reqHeight, mode == LOAD_MODE_FIT_EXACT);
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

    private static final String[] WRONG_EXTENSIONS = new String[] {".jpeg", ".jpg", ".bmp", ".gif", ".png"};
    public static String ensureFileExtension(String imageName, String ensureExtension) {
        if (TextUtils.isEmpty(imageName)) {
            return ensureExtension;
        }
        String lowercaseName = imageName.toLowerCase();
        if (lowercaseName.endsWith(ensureExtension)) {
            return imageName;
        }
        for (String ext : WRONG_EXTENSIONS) {
            if (lowercaseName.endsWith(ext) && imageName.length() >= ext.length()) {
                return imageName.substring(0, imageName.length() - ext.length()) + ensureExtension;
            }
        }
        // had other extension or none, just add it
        return imageName + ensureExtension;
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

    /* private static class Triangle {
        private static final RectF BOUND = new RectF();
        private static final Path LINES = new Path();
        float x1, x2, x3, y1, y2, y3;
        private int rgb;
        private int childrenStartIndex = -1;

        public Triangle(float x1, float y1, float x2, float y2, float x3, float y3, Bitmap forColor) {
            this.x1 = x1;
            this.x2 = x2;
            this.x3 = x3;
            this.y1 = y1;
            this.y2 = y2;
            this.y3 = y3;
            calculateColor(forColor);
        }

        private void calculateColor(Bitmap forColor) {
            initPath();
            LINES.computeBounds(BOUND, true);
            int red = 0;
            int green = 0;
            int blue = 0;
            int alpha = 0;
            int pixelInTriangle = 0;
            for (int x = (int) BOUND.left; x < BOUND.right; x++) {
                for (int y = (int) BOUND.top; y < BOUND.bottom; y++) {
                    if (x < forColor.getWidth() && y < forColor.getHeight() && isInside(x, y)) {
                        int bitmapRgb = forColor.getPixel(x, y);
                        red += Color.red(bitmapRgb);
                        green += Color.green(bitmapRgb);
                        blue += Color.blue(bitmapRgb);
                        alpha += Color.alpha(bitmapRgb);
                        pixelInTriangle++;
                    }
                }
            }
            if (pixelInTriangle > 0) {
                red /= pixelInTriangle;
                green /= pixelInTriangle;
                blue /= pixelInTriangle;
                alpha /= pixelInTriangle;
                rgb = Color.argb(alpha, red, green, blue);
            } else {
                Log.d("Riddle", "NO pixels in " + BOUND.left + " " + BOUND.right + " " + BOUND.top + " " + BOUND.bottom + " and path " + LINES);
                rgb = Color.BLUE;
            }
        }

        private void initPath() {
            LINES.rewind();
            LINES.moveTo(x1, y1);
            LINES.lineTo(x2, y2);
            LINES.lineTo(x3, y3);
            LINES.close();
        }

        private static float sign(float x1, float y1, float x2, float y2, float x3, float y3) {
            return (x1 - x3) * (y2 - y3) - (x2 - x3) * (y1 - y3);
        }

        private boolean isInside (float x, float y) {
            boolean b1, b2, b3;
            b1 = sign(x, y, x1, y1, x2, y2) < 0.0f;
            b2 = sign(x, y, x2, y2, x3, y3) < 0.0f;
            b3 = sign(x, y, x3, y3, x1, y1) < 0.0f;

            return ((b1 == b2) && (b2 == b3));
        }

        public void draw(Canvas canvas, Paint paint) {
            paint.setColor(rgb);
            initPath();
            canvas.drawPath(LINES, paint);
        }



        private static float calculateDistanceToLine(float x1, float y1, float x2, float y2, float x, float y, float[] coords) {
            float alpha = x2 - x1;
            float beta = y2 - y1;
            float n1, n2;
            if (Math.abs(alpha) >= 1E-4) {
                n2 = 1;
                n1 = -beta*n2 / alpha;
            } else if (Math.abs(beta) >= 1E-4) {
                n1 = 1;
                n2 = -alpha*n1 / beta;
            } else {
                return 0;
            }
            float nNorm = (float) Math.sqrt(n1*n1 + n2*n2);
            if (x1 * n1 + y1 * n2 >= 0) {
                n1 /= nNorm;
                n2 /= nNorm;
            } else {
                n1 /= -nNorm;
                n2 /= -nNorm;
            }
            float d = x1 * n1 + y1 * n2;
            float dist =  x * n1 + y * n2 - d;
            coords[0] = x - n1 * dist;
            coords[1] = y - n2 * dist;
            Log.d("Riddle", "Dist: " + dist + " x/y: " + x + "/" + y + " coords: " + coords[0] + "/" + coords[1]);
            return Math.abs(dist);
        }
    }
    */
	
}
