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
        new AsyncTask<Void, List<?>, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                SharedPreferences prefs = context.getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE);
                int currVersion = prefs.getInt(PREFERENCES_KEY_IMAGE_MANAGER_VERSION, VERSION - 1);
                for (;currVersion < VERSION; currVersion++) {
                    List<Image> newImages = new LinkedList<>();
                    try {
                        switch (currVersion) {
                            case 0:
                                buildVersion1(context, newImages); break;
                            //case 1:
                            //    syncVersion2(context, newImages);
                        }
                    } catch (BuildException be) {
                        Log.e("Image", "Failed synching database from version " + currVersion + " to version " + VERSION + " build exception " + be);
                    }
                    for (Image img : newImages) {
                        img.saveToDatabase(context);
                    }
                    prefs.edit().putInt(PREFERENCES_KEY_IMAGE_MANAGER_VERSION, VERSION).commit();
                    publishProgress(newImages);
                    Log.d("Image", "Published images for version " + currVersion + " : " + newImages);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(List<?>... lists) {
                onImagesAvailable((List<Image>) lists[0]);
            }

            @Override
            public void onPostExecute(Void nothing) {
                if (operationDoneListener != null) {
                    operationDoneListener.operationDone();
                }
            }
        }.execute();
    }


    private static void buildVersion1(Context context, List<Image> newImages) throws BuildException {
        Log.d("Image", "Building images of version 1.");

        //hash esel 1a25d825c94dd0dae1140562439362d0
        ImageAuthor author = new ImageAuthor("Nemo", "pixabay.com/en/donkey-animal-farm-gray-comic-310798", "CC0 Public Domain", "donkey2", null);
        easyBuild(context, author, newImages,
                R.drawable.esel, "ESEL", "DONKEY");

        //hash fisch 6085d77d9198d7fa6070873214baead8
        author = new ImageAuthor("?", "pngimg.com/download/1160", "", "fish", null);
        easyBuild(context, author, newImages,
                R.drawable.fisch, "FISCH", "FISH");

        //hash screwdriver
        author = new ImageAuthor("?", "pixabay.com/en/screwdriver-starhead-star-torx-33634", "CC0 Public Domain", "screwdriver", null);
        easyBuild(context, author, newImages,
                R.drawable.screwdriver, "SCHRAUBENZIEHER", "SCREWDRIVER");

        //hash bunny
        author = new ImageAuthor("Nemo", "pixabay.com/en/bunny-outline-easter-cutout-cookie-306263/", "CC0 Public Domain", "bunny", null);
        easyBuild(context, author, newImages,
                R.drawable.bunny, "HASE", "BUNNY")
                .addSolution(new Solution(Tongue.ENGLISH, "RABBIT"));


        //hash scissor
        author = new ImageAuthor("Nemo", "pixabay.com/en/scissors-shears-cut-tool-equipment-24188/", "CC0 Public Domain", "scissor", null);
        easyBuild(context, author, newImages,
                R.drawable.scissor, "SCHERE", "SCISSOR");

        //hash soccer
        author = new ImageAuthor("OpenClips", "pixabay.com/en/football-ball-sport-soccer-round-157930", "CC0 Public Domain", "soccer", null);
        easyBuild(context, author, newImages,
                R.drawable.football, "FUßBALL", "FOOTBALL");

        //hash penguin
        author = new ImageAuthor("Nemo", "pixabay.com/en/penguin-aquatic-flightless-birds-41066/", "CC0 Public Domain", "penguin", null);
        easyBuild(context, author, newImages,
                R.drawable.penguin, "PINGUIN", "PENGUIN");


        //hash castle
        author = new ImageAuthor("stux", "pixabay.com/en/castle-padlock-shut-off-to-378353", "CC0 Public Domain", "castle", null);
        easyBuild(context, author, newImages,
                R.drawable.castle, "SCHLOSS", "LOCK");


author = new ImageAuthor("jrperes","http://pixabay.com/de/mond-himmel-wolken-tag-v%C3%B6gel-323425/", "CC0 Public Domain","moon", null);easyBuild(context, author, newImages,R.drawable.moon, "MOND", "MOON");
author = new ImageAuthor("catherinemary","http://pixabay.com/de/bank-rot-himmel-blau-natur-185234/", "CC0 Public Domain","bench", null);easyBuild(context, author, newImages,R.drawable.bench, "BANK", "BENCH");
author = new ImageAuthor("MJO","http://pixabay.com/de/rosa-rot-blume-geschenk-143445/", "CC0 Public Domain","pink", null);easyBuild(context, author, newImages,R.drawable.pink, "ROSE", "ROSE");
author = new ImageAuthor("rolypolys","http://pixabay.com/de/d%C3%A4nemark-ostsee-k%C3%BCste-meer-wasser-239992/", "CC0 Public Domain","denmark", null);easyBuild(context, author, newImages,R.drawable.denmark, "MEER", "SEA");
author = new ImageAuthor("PublicDomainPictures","http://pixabay.com/de/hintergrund-blau-sauber-klar-tag-21717/", "CC0 Public Domain","background1", null);easyBuild(context, author, newImages,R.drawable.background1, "WIESE", "GRASSLAND");
author = new ImageAuthor("PublicDomainPictures","http://pixabay.com/de/hintergrund-nahaufnahme-flora-16051/", "CC0 Public Domain","background2", null);easyBuild(context, author, newImages,R.drawable.background2, "GRAS", "GRASS");
author = new ImageAuthor("Maddox74","http://pixabay.com/de/nashorn-safaripark-d%C3%A4nemark-tier-433495/", "CC0 Public Domain","rhino4", null);easyBuild(context, author, newImages,R.drawable.rhino4, "NASHORN", "RHINO");
author = new ImageAuthor("Kaz","http://pixabay.com/de/nashorn-tier-schwarz-silhouette-220220/", "CC0 Public Domain","rhino3", null);easyBuild(context, author, newImages,R.drawable.rhino3, "NASHORN", "RHINO");
author = new ImageAuthor("OpenClips","http://pixabay.com/de/nashorn-afrika-tier-s%C3%BCdafrika-161569/", "CC0 Public Domain","rhino2", null);easyBuild(context, author, newImages,R.drawable.rhino2, "NASHORN", "RHINO");
author = new ImageAuthor("OpenClips","http://pixabay.com/de/nashorn-tier-biologie-s%C3%A4ugetier-153558/", "CC0 Public Domain","rhino1", null);easyBuild(context, author, newImages,R.drawable.rhino1, "NASHORN", "RHINO");
author = new ImageAuthor("OpenClips","http://pixabay.com/de/drache-eidechse-monster-chinesisch-149393/", "CC0 Public Domain","dragon", null);easyBuild(context, author, newImages,R.drawable.dragon, "DRACHE", "DRAGON");
author = new ImageAuthor("OpenClips","http://pixabay.com/de/stier-buffalo-tier-s%C3%A4ugetier-155411/", "CC0 Public Domain","bull", null);easyBuild(context, author, newImages,R.drawable.bull, "STIER", "BULL");
author = new ImageAuthor("Hebi65","http://pixabay.com/de/fledermaus-schwarz-dracula-fl%C3%BCgel-151366/", "CC0 Public Domain","bat", null);easyBuild(context, author, newImages,R.drawable.bat, "FLEDERMAUS", "BAT");
author = new ImageAuthor("Hebi65","http://pixabay.com/de/blume-rose-kontur-umrisse-schwarz-681009/", "CC0 Public Domain","flower", null);easyBuild(context, author, newImages,R.drawable.flower, "BLUME", "FLOWER");
author = new ImageAuthor("Hebi65","http://pixabay.com/de/schmetterling-schwarz-weiss-konturen-658047/", "CC0 Public Domain","butterfly", null);easyBuild(context, author, newImages,R.drawable.butterfly, "SCHMETTERLING", "BUTTERFLY");
author = new ImageAuthor("Hebi65","http://pixabay.com/de/baum-scherenschnitt-natur-657481/", "CC0 Public Domain","tree", null);easyBuild(context, author, newImages,R.drawable.tree, "BAUM", "TREE");
author = new ImageAuthor("OpenClips","http://pixabay.com/de/karte-umrissen-spielen-schwarz-157404/", "CC0 Public Domain","card", null);easyBuild(context, author, newImages,R.drawable.card, "SPIELKARTE", "GAMECARD");
author = new ImageAuthor("traude","http://pixabay.com/de/telefon-handy-telefonieren-558022/", "CC0 Public Domain","phone", null);easyBuild(context, author, newImages,R.drawable.phone, "TELEFON", "PHONE");
author = new ImageAuthor("Nemo","http://pixabay.com/de/flasche-ketchup-leere-geschlossen-306549/", "CC0 Public Domain","bottle", null);easyBuild(context, author, newImages,R.drawable.bottle, "FLASCHE", "BOTTLE");
author = new ImageAuthor("Nemo","http://pixabay.com/de/elefant-profil-rot-gro%C3%9F-306223/", "CC0 Public Domain","elephant", null);easyBuild(context, author, newImages,R.drawable.elephant, "ELEFANT", "ELEPHANT");
author = new ImageAuthor("Nemo","http://pixabay.com/de/brillen-schwarz-silhouette-310516/", "CC0 Public Domain","eyeglasses", null);easyBuild(context, author, newImages,R.drawable.eyeglasses, "BRILLE", "GLASSES");
author = new ImageAuthor("Nemo","http://pixabay.com/de/k%C3%A4nguru-s%C3%A4ugetier-australien-295261/", "CC0 Public Domain","kangaroo", null);easyBuild(context, author, newImages,R.drawable.kangaroo, "KÄNGURU", "KANGAROO");
author = new ImageAuthor("Nemo","http://pixabay.com/de/amsel-stehen-silhouette-rosa-305542/", "CC0 Public Domain","blackbird", null);easyBuild(context, author, newImages,R.drawable.blackbird, "AMSEL", "BLACKBIRD");
author = new ImageAuthor("Nemo","http://pixabay.com/de/au%C3%9Ferirdischer-geste-des-friedens-308429/", "CC0 Public Domain","alien", null);easyBuild(context, author, newImages,R.drawable.alien, "ALIEN", "ALIEN");
author = new ImageAuthor("Nemo","http://pixabay.com/de/teddyb%C3%A4r-b%C3%A4r-pl%C3%BCsch-gef%C3%BCllt-anial-303837/", "CC0 Public Domain","teddy-bear", null);easyBuild(context, author, newImages,R.drawable.teddy_bear, "TEDDYBÄR", "TEDDY");
author = new ImageAuthor("Nemo","http://pixabay.com/de/fox-blau-silhouette-kunst-tierwelt-310123/", "CC0 Public Domain","fox", null);easyBuild(context, author, newImages,R.drawable.fox, "FUCHS", "FOX");
author = new ImageAuthor("Nemo","http://pixabay.com/de/h%C3%A4nde-zwei-offen-silhouette-296850/", "CC0 Public Domain","hands", null);easyBuild(context, author, newImages,R.drawable.hands, "FINGER", "FINGER");
author = new ImageAuthor("Nemo","http://pixabay.com/de/monitor-flatscreen-bildschirm-23269/", "CC0 Public Domain","monitor", null);easyBuild(context, author, newImages,R.drawable.monitor, "BILDSCHIRM", "MONITOR");
author = new ImageAuthor("KTEditor","http://pixabay.com/de/uhr-zeit-stunden-559963/", "CC0 Public Domain","clock", null);easyBuild(context, author, newImages,R.drawable.clock, "UHR", "CLOCK");
author = new ImageAuthor("amandaelizabeth84","http://pixabay.com/de/schnurrbart-lenker-m%C3%A4nnlich-haar-473661/", "CC0 Public Domain","moustache", null);easyBuild(context, author, newImages,R.drawable.moustache, "SCHNURRBART", "MOUSTACHE");

        //cat
        author = new ImageAuthor("Hebi65", "http://pixabay.com/en/animal-cat-contour-outlines-675646", "CC0 Public Domain", "animal", null);
        easyBuild(context, author, newImages,
                R.drawable.cat, "KATZE", "CAT");

    }

    private static Image.Builder easyBuild(Context context, ImageAuthor author, List<Image> newImages,
                                    int resId, String german, String english) throws BuildException {
        Image.Builder builder = new Image.Builder(context, resId, author);
        builder.addSolution(new Solution(Tongue.GERMAN, german));
        builder.addSolution(new Solution(Tongue.ENGLISH, english));
        Image image = builder.build();
        newImages.add(image);
        return builder;
    }

    public static void markInvalidImage(Image image) {
        if (image != null) {
            ALL_IMAGES.remove(image.getHash());
            INVALID_IMAGES.add(image);
        }
    }
}
