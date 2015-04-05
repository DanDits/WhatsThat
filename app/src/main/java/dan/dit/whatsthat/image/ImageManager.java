package dan.dit.whatsthat.image;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.preferences.Tongue;
import dan.dit.whatsthat.riddle.PracticalRiddleType;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.util.BuildException;
import dan.dit.whatsthat.util.OperationDoneListener;

/**
 * Manager class for image related things.
 * <ul><li>Syncs new drawables or updated drawable information to the database on startup.
 * <li>Adds extern images by some user to database.</li>
 * <li>Deletes any images from database that are no more accessible due to missing file or being removed from
 * drawables for some reasons.
 * <li>Holds a list of all available images, notifies listeners when (new) images are available</li>
 * <li>Performs database actions asynchronous</li>
 * </ul>
 * Created by daniel on 28.03.15.
 */
public class ImageManager {
    private static final int VERSION = 1; // version number needs to be increased each time new images are added to the resources to add them to the database
    private static final int VERSION_START = VERSION; // for debugging use VERSION // forever fixed version number if nothing was previously installed and synched

    private static final String PREFERENCES_KEY_IMAGE_MANAGER_VERSION = "dan.dit.whatsthat.prefkey_imagemanagerversion";

    private static final Map<String, Image> ALL_IMAGES = new HashMap<>();
    private static final Set<Image> INVALID_IMAGES = new HashSet<>(); //TODO remove images from database when some async operation is started

    private ImageManager() {}


    public static Collection<Image> getAvailableImages() {
        return ALL_IMAGES.values();
    }

    public static void init(final Context context, final OperationDoneListener operationDoneListener) {
        // first load the available hashes, then the images for the hashes and finally sync (new) images to database
        new AsyncImageOperator().loadAvailableHashes(context, new AsyncImageOperator.Callback() {
            @Override
            public void onProgressUpdate(String imageHash, int operation, int progress) {
                // ye we got one, only interesting if init wants feedback to a listener
            }

            @Override
            public void onProgressComplete(String[] imageHashes, int operation, Image[] images) {
                // hashes loaded
                Log.d("Image", "Loaded hashes on init: " + Arrays.toString(imageHashes));
                if (imageHashes != null && imageHashes.length > 0) {
                    new AsyncImageOperator().loadImageFromDatabase(context, imageHashes, new AsyncImageOperator.Callback() {

                        @Override
                        public void onProgressUpdate(String imageHash, int operation, int progress) {
                            // see other callback
                        }

                        @Override
                        public void onProgressComplete(String[] imageHashes, int operation, Image[] images) {
                            // images loaded
                            if (images != null && images.length > 0) {
                                List<Image> imagesList = new ArrayList<Image>(images.length);
                                for (Image image : images) {
                                    imagesList.add(image);
                                }
                                onImagesAvailable(imagesList);
                            }
                            syncToDatabase(context, operationDoneListener);
                        }
                    });
                } else {
                    // we cannot load images because we got none in the database, so its even more important to init stuff
                    syncToDatabase(context, operationDoneListener);
                }
            }
        });
    }

    public static void onImagesAvailable(List<Image> images) {
        if (images != null && !images.isEmpty()) {
            for (Image image : images) {
                ALL_IMAGES.put(image.getHash(), image);
            }
            //TODO notify listeners
            Log.d("Image", "Images available: " + images);
        }
    }

    private static void syncToDatabase(final Context context, final OperationDoneListener operationDoneListener) {
        new AsyncTask<Void, Void, List<Image>>() {

            @Override
            protected List<Image> doInBackground(Void... voids) {
                return syncToDatabaseExecute(context);
            }

            @Override
            public void onPostExecute(List<Image> newImages) {
                onImagesAvailable(newImages);
                if (operationDoneListener != null) {
                    operationDoneListener.operationDone();
                }
            }
        }.execute();
    }

    /**
     * Syncs available drawable data to database if any new data is available. This should not be invoked on the
     * ui thread.
     * @param context The application context.
     */
    protected static List<Image> syncToDatabaseExecute(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE);
        int currVersion = prefs.getInt(PREFERENCES_KEY_IMAGE_MANAGER_VERSION, VERSION_START);
        // no breaks on the sync train!
        List<Image> newImages = new LinkedList<Image>();
        try {
            switch (currVersion) {
                case VERSION_START:
                    syncVersion1(context, newImages);
                //case 1:
                //    syncVersion2(context, newImages);
            }
        } catch (BuildException be) {
            Log.e("Image", "Failed synching database from version " + currVersion + " to version " + VERSION + " build exception " + be);
        }
        prefs.edit().putInt(PREFERENCES_KEY_IMAGE_MANAGER_VERSION, VERSION).commit();
        return newImages;
    }

    private static void syncVersion1(Context context, List<Image> newImages) throws BuildException {
        Log.d("Image", "Synching version 1 to image database.");

        //hash esel 1a25d825c94dd0dae1140562439362d0
        ImageAuthor author = new ImageAuthor("Nemo", "pixabay.com/en/donkey-animal-farm-gray-comic-310798", "CC0 Public Domain", "donkey2", null);
        Image.Builder builder = new Image.Builder(context, R.drawable.esel, author);
        builder.addSolution(new Solution(Tongue.GERMAN, "ESEL"));
        builder.addSolution(new Solution(Tongue.ENGLISH, "DONKEY"));
        Image image = builder.build();
        Log.d("Image", "Created esel, prefs: " + image.getPreferredRiddleTypes());
        boolean success = image.saveToDatabase(context);
        if (success) { newImages.add(image); }

        //hash fisch 6085d77d9198d7fa6070873214baead8
        author = new ImageAuthor("?", "pngimg.com/download/1160", "", "fish", null);
        builder = new Image.Builder(context, R.drawable.fisch, author);
        builder.addSolution(new Solution(Tongue.GERMAN, "FISCH"));
        builder.addSolution(new Solution(Tongue.ENGLISH, "FISH"));
        builder.addDislikedRiddleType(PracticalRiddleType.Circle.INSTANCE);
        image = builder.build();
        Log.d("Image", "Created fisch, prefs: " + image.getPreferredRiddleTypes());
        success = image.saveToDatabase(context);
        if (success) { newImages.add(image); }

        //hash screwdriver

        author = new ImageAuthor("?", "pixabay.com/en/screwdriver-starhead-star-torx-33634", "CC0 Public Domain", "screwdriver", null);
        builder = new Image.Builder(context, R.drawable.screwdriver, author);
        builder.addSolution(new Solution(Tongue.GERMAN, "SCHRAUBENZIEHER"));
        builder.addSolution(new Solution(Tongue.ENGLISH, "SCREWDRIVER"));
        image = builder.build();
        Log.d("Image", "Created screwdriver, prefs: " + image.getPreferredRiddleTypes());
        success = image.saveToDatabase(context);
        if (success) { newImages.add(image); }

        //hash bunny
        author = new ImageAuthor("Nemo", "pixabay.com/en/bunny-outline-easter-cutout-cookie-306263/", "CC0 Public Domain", "bunny", null);
        builder = new Image.Builder(context, R.drawable.bunny, author);
        builder.addSolution(new Solution(Tongue.GERMAN, "HASE"));
        builder.addSolution(new Solution(Tongue.ENGLISH, "BUNNY", "RABBIT"));
        builder.addPreferredRiddleType(PracticalRiddleType.Circle.INSTANCE);
        image = builder.build();
        Log.d("Image", "Created rabbit, prefs: " + image.getPreferredRiddleTypes());
        success = image.saveToDatabase(context);
        if (success) { newImages.add(image); }

        //hash scissor
        author = new ImageAuthor("Nemo", "pixabay.com/en/scissors-shears-cut-tool-equipment-24188/", "CC0 Public Domain", "scissor", null);
        builder = new Image.Builder(context, R.drawable.scissor, author);
        builder.addSolution(new Solution(Tongue.GERMAN, "SCHERE"));
        builder.addSolution(new Solution(Tongue.ENGLISH, "SCISSOR"));
        builder.addPreferredRiddleType(PracticalRiddleType.Circle.INSTANCE);
        builder.addPreferredRiddleType(PracticalRiddleType.Circle.INSTANCE);
        image = builder.build();
        Log.d("Image", "Created scissor, prefs: " + image.getPreferredRiddleTypes());
        success = image.saveToDatabase(context);
        if (success) { newImages.add(image); }

        //hash soccer
        author = new ImageAuthor("OpenClips", "pixabay.com/en/football-ball-sport-soccer-round-157930", "CC0 Public Domain", "soccer", null);
        builder = new Image.Builder(context, R.drawable.football, author);
        builder.addSolution(new Solution(Tongue.GERMAN, "FUßBALL"));
        builder.addSolution(new Solution(Tongue.ENGLISH, "FOOTBALL"));
        builder.addPreferredRiddleType(PracticalRiddleType.Circle.INSTANCE);
        image = builder.build();
        Log.d("Image", "Created soccer, prefs: " + image.getPreferredRiddleTypes());
        success = image.saveToDatabase(context);
        if (success) { newImages.add(image); }

        //hash penguin
        author = new ImageAuthor("Nemo", "pixabay.com/en/penguin-aquatic-flightless-birds-41066/", "CC0 Public Domain", "penguin", null);
        builder = new Image.Builder(context, R.drawable.penguin, author);
        builder.addSolution(new Solution(Tongue.GERMAN, "PINGUIN"));
        builder.addSolution(new Solution(Tongue.ENGLISH, "PENGUIN"));
        builder.addPreferredRiddleType(PracticalRiddleType.Circle.INSTANCE);
        image = builder.build();
        Log.d("Image", "Created penguin, prefs: " + image.getPreferredRiddleTypes());
        success = image.saveToDatabase(context);
        if (success) { newImages.add(image); }


        //hash castle
        author = new ImageAuthor("stux", "pixabay.com/en/castle-padlock-shut-off-to-378353", "CC0 Public Domain", "castle", null);
        builder = new Image.Builder(context, R.drawable.castle, author);
        builder.addSolution(new Solution(Tongue.GERMAN, "SCHLOSS"));
        builder.addSolution(new Solution(Tongue.ENGLISH, "CASTLE"));
        builder.addPreferredRiddleType(PracticalRiddleType.Circle.INSTANCE);
        image = builder.build();
        Log.d("Image", "Created castle, prefs: " + image.getPreferredRiddleTypes());
        success = image.saveToDatabase(context);
        if (success) { newImages.add(image); }
    }

    public static void markInvalidImage(Image image) {
        if (image != null) {
            ALL_IMAGES.remove(image.getHash());
            INVALID_IMAGES.add(image);
        }
    }
}
