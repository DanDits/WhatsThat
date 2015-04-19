package dan.dit.whatsthat.image;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.preferences.Tongue;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.util.BuildException;

/**
 * Manager class for image related things.
 * <ul><li>Syncs new drawables or updated drawable information to the database on startup.
 * <li>Deletes any images from database that are no more accessible due to missing file or being removed from
 * drawables for some reasons.
 * </ul>
 * Created by daniel on 28.03.15.
 */
public class ImageManager {
    public static final int SYNC_VERSION = 1; // version number needs to be increased each time new images are added to the resources to add them to the database

    protected static final String PREFERENCES_KEY_IMAGE_MANAGER_VERSION = "dan.dit.whatsthat.prefkey_imagemanagerversion";

    private static final Set<Image> INVALID_IMAGES = new HashSet<>(); //TODO remove images from database when some async operation is started
    public static final int PROGRESS_COMPLETE = 100;

    private ImageManager() {}

    private static SyncingTask SYNCING_TASK;
    
    public static void sync(Context context, SynchronizationListener listener) {
        cancelSync();
        SYNCING_TASK = new SyncingTask(context, listener);
        SYNCING_TASK.execute();
    }

    public static void registerSynchronizationListener(SynchronizationListener listener) {
        if (SYNCING_TASK != null && listener != null) {
            SYNCING_TASK.mListener.add(listener);
        }
    }

    public static void unregisterSynchronizationListener(SynchronizationListener listener) {
        if (SYNCING_TASK != null && listener != null) {
            SYNCING_TASK.mListener.remove(listener);
        }
    }

    public static void cancelSync() {
        if (SYNCING_TASK != null) {
            SYNCING_TASK.mListener.clear();
            SYNCING_TASK.cancel(true);
            SYNCING_TASK = null;
        }
    }

    public static boolean isSyncing() {
        return SYNCING_TASK != null;
    }

    public static boolean isSynced() {
        return !isSyncing(); //TODO keep track of it after sync was called
    }

    private static class SyncingTask extends AsyncTask<Void, Integer, Void> {

        private List<SynchronizationListener> mListener;
        private Context mContext;
        private int mCurrImageIndex;
        private int mCurrImageCount;

        public SyncingTask(Context context, SynchronizationListener listener) {
            mContext = context;
            mListener = new LinkedList<>();
            if (listener != null) {
                mListener.add(listener);
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ImageXmlParser parser = new ImageXmlParser();
            try {
                parser.parseAndSyncBundles(mContext);
            } catch (IOException e) {
                Log.e("Image", "IO Error parsing bundles: " + e);
            } catch (XmlPullParserException e) {
                Log.e("Image", "XML Error parsing bundles: " + e);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (progress.length == 1) {
                for (SynchronizationListener listener : mListener) {
                    listener.onSyncComplete(progress[0]);
                }
            } else if (progress.length == 3) {
                for (SynchronizationListener listener : mListener) {
                    listener.onSyncProgress(progress[0], (int) (PROGRESS_COMPLETE * progress[1] / ((double) progress[2])));
                }
            }
        }

        @Override
        protected void onPostExecute(Void nothing) {
            SYNCING_TASK = null;
            for (SynchronizationListener listener : mListener) {
                listener.onSyncComplete(SYNC_VERSION);
            }
        }

        private void buildVersion1() throws BuildException {
            mCurrImageCount = 39;
            mCurrImageIndex = 0;
            Log.d("Image", "Building " + mCurrImageCount + " images.");
            Image.Builder builder;

            //hash esel 1a25d825c94dd0dae1140562439362d0
            ImageAuthor author = new ImageAuthor("Nemo", "pixabay.com/en/donkey-animal-farm-gray-comic-310798", "CC0 Public Domain", "donkey2", null);
            builder=easyBuild(author,R.drawable.esel, "ESEL", "DONKEY");
            if (isCancelled()) {return;} else {easySave(builder);}

            //hash fisch 6085d77d9198d7fa6070873214baead8
            author = new ImageAuthor("?", "pngimg.com/download/1160", "", "fish", null);
            builder=easyBuild(author,R.drawable.fisch, "FISCH", "FISH");
            if (isCancelled()) {return;} else {easySave(builder);}

            //hash screwdriver
            author = new ImageAuthor("?", "pixabay.com/en/screwdriver-starhead-star-torx-33634", "CC0 Public Domain", "screwdriver", null);
            builder=easyBuild(author, R.drawable.screwdriver, "SCHRAUBENZIEHER", "SCREWDRIVER");
            if (isCancelled()) {return;} else {easySave(builder);}

            //hash bunny
            author = new ImageAuthor("Nemo", "pixabay.com/en/bunny-outline-easter-cutout-cookie-306263/", "CC0 Public Domain", "bunny", null);
            builder=easyBuild(author,R.drawable.bunny, "HASE", "BUNNY")
                    .addSolution(new Solution(Tongue.ENGLISH, "RABBIT"));
            if (isCancelled()) {return;} else {easySave(builder);}


            //hash scissor
            author = new ImageAuthor("Nemo", "pixabay.com/en/scissors-shears-cut-tool-equipment-24188/", "CC0 Public Domain", "scissor", null);
            builder=easyBuild(author,R.drawable.scissor, "SCHERE", "SCISSOR");
            if (isCancelled()) {return;} else {easySave(builder);}

            //hash soccer
            author = new ImageAuthor("OpenClips", "pixabay.com/en/football-ball-sport-soccer-round-157930", "CC0 Public Domain", "soccer", null);
            builder=easyBuild(author, R.drawable.football, "FUßBALL", "FOOTBALL");
            if (isCancelled()) {return;} else {easySave(builder);}

            //hash penguin
            author = new ImageAuthor("Nemo", "pixabay.com/en/penguin-aquatic-flightless-birds-41066/", "CC0 Public Domain", "penguin", null);
            builder=easyBuild(author,R.drawable.penguin, "PINGUIN", "PENGUIN");
            if (isCancelled()) {return;} else {easySave(builder);}


            //hash castle
            author = new ImageAuthor("stux", "pixabay.com/en/castle-padlock-shut-off-to-378353", "CC0 Public Domain", "castle", null);
            builder=easyBuild(author,
                    R.drawable.castle, "SCHLOSS", "LOCK");
            if (isCancelled()) {return;} else {easySave(builder);}


            author = new ImageAuthor("jrperes","http://pixabay.com/de/mond-himmel-wolken-tag-v%C3%B6gel-323425/", "CC0 Public Domain","moon", null);builder=easyBuild(author, R.drawable.moon, "MOND", "MOON");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("catherinemary","http://pixabay.com/de/bank-rot-himmel-blau-natur-185234/", "CC0 Public Domain","bench", null);builder=easyBuild(author, R.drawable.bench, "BANK", "BENCH");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("MJO","http://pixabay.com/de/rosa-rot-blume-geschenk-143445/", "CC0 Public Domain","pink", null);builder=easyBuild(author, R.drawable.pink, "ROSE", "ROSE");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("rolypolys","http://pixabay.com/de/d%C3%A4nemark-ostsee-k%C3%BCste-meer-wasser-239992/", "CC0 Public Domain","denmark", null);builder=easyBuild(author, R.drawable.denmark, "MEER", "SEA");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("PublicDomainPictures","http://pixabay.com/de/hintergrund-blau-sauber-klar-tag-21717/", "CC0 Public Domain","background1", null);builder=easyBuild(author, R.drawable.background1, "WIESE", "GRASSLAND");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("PublicDomainPictures","http://pixabay.com/de/hintergrund-nahaufnahme-flora-16051/", "CC0 Public Domain","background2", null);builder=easyBuild(author, R.drawable.background2, "GRAS", "GRASS");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Maddox74","http://pixabay.com/de/nashorn-safaripark-d%C3%A4nemark-tier-433495/", "CC0 Public Domain","rhino4", null);builder=easyBuild(author, R.drawable.rhino4, "NASHORN", "RHINO");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Kaz","http://pixabay.com/de/nashorn-tier-schwarz-silhouette-220220/", "CC0 Public Domain","rhino3", null);builder=easyBuild(author, R.drawable.rhino3, "NASHORN", "RHINO");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("OpenClips","http://pixabay.com/de/nashorn-afrika-tier-s%C3%BCdafrika-161569/", "CC0 Public Domain","rhino2", null);builder=easyBuild(author, R.drawable.rhino2, "NASHORN", "RHINO");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("OpenClips","http://pixabay.com/de/nashorn-tier-biologie-s%C3%A4ugetier-153558/", "CC0 Public Domain","rhino1", null);builder=easyBuild(author, R.drawable.rhino1, "NASHORN", "RHINO");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("OpenClips","http://pixabay.com/de/drache-eidechse-monster-chinesisch-149393/", "CC0 Public Domain","dragon", null);builder=easyBuild(author, R.drawable.dragon, "DRACHE", "DRAGON");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("OpenClips","http://pixabay.com/de/stier-buffalo-tier-s%C3%A4ugetier-155411/", "CC0 Public Domain","bull", null);builder=easyBuild(author, R.drawable.bull, "STIER", "BULL");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Hebi65","http://pixabay.com/de/fledermaus-schwarz-dracula-fl%C3%BCgel-151366/", "CC0 Public Domain","bat", null);builder=easyBuild(author, R.drawable.bat, "BATMAN", "BATMAN");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Hebi65","http://pixabay.com/de/blume-rose-kontur-umrisse-schwarz-681009/", "CC0 Public Domain","flower", null);builder=easyBuild(author, R.drawable.flower, "BLUME", "FLOWER");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Hebi65","http://pixabay.com/de/schmetterling-schwarz-weiss-konturen-658047/", "CC0 Public Domain","butterfly", null);builder=easyBuild(author, R.drawable.butterfly, "SCHMETTERLING", "BUTTERFLY");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Hebi65","http://pixabay.com/de/baum-scherenschnitt-natur-657481/", "CC0 Public Domain","tree", null);builder=easyBuild(author, R.drawable.tree, "BAUM", "TREE");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("OpenClips","http://pixabay.com/de/karte-umrissen-spielen-schwarz-157404/", "CC0 Public Domain","card", null);builder=easyBuild(author, R.drawable.card, "PIK", "PIK");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("traude","http://pixabay.com/de/telefon-handy-telefonieren-558022/", "CC0 Public Domain","phone", null);builder=easyBuild(author, R.drawable.phone, "TELEFON", "PHONE");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo","http://pixabay.com/de/flasche-ketchup-leere-geschlossen-306549/", "CC0 Public Domain","bottle", null);builder=easyBuild(author, R.drawable.bottle, "FLASCHE", "BOTTLE");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo","http://pixabay.com/de/elefant-profil-rot-gro%C3%9F-306223/", "CC0 Public Domain","elephant", null);builder=easyBuild(author, R.drawable.elephant, "ELEFANT", "ELEPHANT");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo","http://pixabay.com/de/brillen-schwarz-silhouette-310516/", "CC0 Public Domain","eyeglasses", null);builder=easyBuild(author, R.drawable.eyeglasses, "BRILLE", "GLASSES");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo","http://pixabay.com/de/k%C3%A4nguru-s%C3%A4ugetier-australien-295261/", "CC0 Public Domain","kangaroo", null);builder=easyBuild(author, R.drawable.kangaroo, "KÄNGURU", "KANGAROO");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo","http://pixabay.com/de/amsel-stehen-silhouette-rosa-305542/", "CC0 Public Domain","blackbird", null);builder=easyBuild(author, R.drawable.blackbird, "AMSEL", "BLACKBIRD");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo","http://pixabay.com/de/au%C3%9Ferirdischer-geste-des-friedens-308429/", "CC0 Public Domain","alien", null);builder=easyBuild(author, R.drawable.alien, "ALIEN", "ALIEN");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo","http://pixabay.com/de/teddyb%C3%A4r-b%C3%A4r-pl%C3%BCsch-gef%C3%BCllt-anial-303837/", "CC0 Public Domain","teddy-bear", null);builder=easyBuild(author, R.drawable.teddybear, "TEDDYBÄR", "TEDDY");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo","http://pixabay.com/de/fox-blau-silhouette-kunst-tierwelt-310123/", "CC0 Public Domain","fox", null);builder=easyBuild(author, R.drawable.fox, "FUCHS", "FOX");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo","http://pixabay.com/de/h%C3%A4nde-zwei-offen-silhouette-296850/", "CC0 Public Domain","hands", null);builder=easyBuild(author, R.drawable.hands, "HÄNDE", "HANDS");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo","http://pixabay.com/de/monitor-flatscreen-bildschirm-23269/", "CC0 Public Domain","monitor", null);builder=easyBuild(author, R.drawable.monitor, "BILDSCHIRM", "MONITOR");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("KTEditor","http://pixabay.com/de/uhr-zeit-stunden-559963/", "CC0 Public Domain","clock", null);builder=easyBuild(author, R.drawable.clock, "UHR", "CLOCK");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("amandaelizabeth84","http://pixabay.com/de/schnurrbart-lenker-m%C3%A4nnlich-haar-473661/", "CC0 Public Domain","moustache", null);builder=easyBuild(author, R.drawable.moustache, "SCHNURRBART", "MOUSTACHE");if (isCancelled()) {return;} else {easySave(builder);}

            author = new ImageAuthor("Hebi65", "http://pixabay.com/en/animal-cat-contour-outlines-675646", "CC0 Public Domain", "animal", null);builder=easyBuild(author,R.drawable.cat, "KATZE", "CAT");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("OpenClips", "http://pixabay.com/en/bomb-explosive-detonation-fuze-154456", "CC0 Public Domain", "bomb", null);builder=easyBuild(author,R.drawable.bomb, "BOMBE", "BOMB");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo", "http://pixabay.com/en/bones-dog-chicken-comic-307870/", "CC0 Public Domain", "bones", null);builder=easyBuild(author,R.drawable.bones, "KNOCHEN", "BONES");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo", "http://pixabay.com/en/mouse-rodent-animal-small-pet-311207/", "CC0 Public Domain", "mouse", null);builder=easyBuild(author,R.drawable.mouse, "MAUS", "MOUSE");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("OpenClips", "http://pixabay.com/en/rocket-spaceship-space-shuttle-nasa-147466/", "CC0 Public Domain", "rocket", null);builder=easyBuild(author,R.drawable.rocket, "RAKETE", "ROCKET");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo", "http://pixabay.com/en/worm-cartoon-character-cute-funny-309559/", "CC0 Public Domain", "worm", null);builder=easyBuild(author,R.drawable.worm, "WURM", "WORM");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("Nemo", "http://pixabay.com/en/turtle-carapace-tortoise-green-303732/", "CC0 Public Domain", "turtle", null);builder=easyBuild(author,R.drawable.turtle, "SCHILDKRÖTE", "TURTLE");if (isCancelled()) {return;} else {easySave(builder);}
            author = new ImageAuthor("OpenClips", "http://pixabay.com/en/santa-claus-st-nicholas-funny-x-mas-153309/", "CC0 Public Domain", "santa", null);builder=easyBuild(author,R.drawable.santa, "NIKOLAUS", "SANTA");if (isCancelled()) {return;} else {easySave(builder);}

        }

        private Image.Builder easyBuild(ImageAuthor author,
                                               int resId, String german, String english) throws BuildException {
            Image.Builder builder = new Image.Builder(mContext, resId, author);
            builder.addSolution(new Solution(Tongue.GERMAN, german));
            builder.addSolution(new Solution(Tongue.ENGLISH, english));
            return builder;
        }
        
        private void easySave(Image.Builder builder) throws BuildException {
            Image image = builder.build();
            image.saveToDatabase(mContext);
            mCurrImageIndex++;
            publishProgress(-1, mCurrImageIndex, mCurrImageCount);
        }

    }

    public interface SynchronizationListener {
        void onSyncProgress(int syncingAtVersion, int imageProgress); // image progress in percent
        void onSyncComplete(int syncedToVersion);
    }


    public static void markInvalidImage(Image image) {
        if (image != null) {
            INVALID_IMAGES.add(image);
        }
    }
}
