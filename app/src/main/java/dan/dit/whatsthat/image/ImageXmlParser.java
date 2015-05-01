package dan.dit.whatsthat.image;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dan.dit.whatsthat.R;
import dan.dit.whatsthat.preferences.Tongue;
import dan.dit.whatsthat.riddle.types.RiddleType;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.storage.ImageTable;
import dan.dit.whatsthat.util.BuildException;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.ImageUtil;

/**
 * XML for easily initializing and loading new images into the app. Format:
 * <imagedata>
 *     <bundle version="1">
 *          <image>
 *              <hash></hash>
 *              <resname></resname>
 *              <solutions>
 *                  <solution>
 *                      <tongue></tongue>
 *                      <word></word>
 *                      <word></word>
 *                  </solution>
 *                  <solution>
 *                      <tongue></tongue>
 *                      <word></word>
 *                  </solution>
 *              </solutions>
 *              <author>
 *                  <name></name>
 *                  <source></source>
 *                  <license></license>
 *                  <title></title>
 *                  <extras></extras>
 *              </author>
 *              <riddleprefs>
 *                  <type></type>
 *                  <type></type>
 *              </riddleprefs>
 *              <riddlerefused>
 *                  <type></type>
 *                  <type></type>
 *              </riddlerefused>
 *          </image>
 *          <image>
 *              ...
 *          </image>
 *     </bundle>
 *     <bundle version="2">
 *         ...
 *     </bundle>
 * </imagedata>
 * Created by daniel on 18.04.15.
 */
public class ImageXmlParser {
    // We don't use namespaces
    public static final String NAMESPACE = null;

    private static final String TAG_ALL_BUNDLES_NAME = "imagedata";
    public static final String TAG_BUNDLE_NAME = "bundle";
    public static final String TAG_IMAGE_NAME = "image";
    public static final String BUNDLE_ATTRIBUTE_VERSION_NAME = "version";
    public static final String TAG_SOLUTION_NAME = "solution";
    public static final String TAG_SOLUTION_TONGUE_NAME = "tongue";
    public static final String TAG_SOLUTION_WORD_NAME = "word";
    public static final String TAG_AUTHOR_NAME = "name";
    public static final String TAG_AUTHOR_SOURCE = "source";
    public static final String TAG_AUTHOR_LICENSE = "license";
    public static final String TAG_AUTHOR_TITLE = "title";
    public static final String TAG_AUTHOR_EXTRAS = "extras";
    public static final String TAG_RIDDLE_TYPE_NAME = "type";
    private static final int PARSE_START_PROGRESS = 10;
    private static final int PARSE_END_PROGRESS = 60;

    private Context mContext;
    private int mHighestReadBundleNumber;
    private ImageManager.SynchronizationListener mListener;
    private Map<Integer, List<Image>> mReadBundles = new HashMap<>();

    public int getHighestReadBundleNumber() {
        return mHighestReadBundleNumber;
    }

    public List<Image> getBundle(int bundleNumber) {
        return mReadBundles.get(bundleNumber);
    }

    public Set<Integer> getReadBundleNumbers() {
        return mReadBundles.keySet();
    }

    public List<Image> parseNewBundles(Context context) throws XmlPullParserException, IOException {
        mContext = context;
        InputStream inputStream = context.getResources().openRawResource(R.raw.imagedata_uncompiled);
        List<Image> images = parse (inputStream, 0);
        Log.d("Image", "Parsed new bundles: Loaded images from XML with highest read number= " + mHighestReadBundleNumber + ": " + images);
        return images;
    }


    public List<Image> parseAndSyncBundles(Context context, ImageManager.SynchronizationListener synchronizationListener) throws XmlPullParserException, IOException {
        mContext = context;
        mListener = synchronizationListener;
        InputStream inputStream = context.getResources().openRawResource(R.raw.imagedata);
        postProgress(PARSE_START_PROGRESS);

        SharedPreferences prefs = context.getSharedPreferences(Image.SHAREDPREFERENCES_FILENAME, Context.MODE_PRIVATE);
        int currBundleNumber = prefs.getInt(ImageManager.PREFERENCES_KEY_IMAGE_MANAGER_VERSION, 0);
        List<Image> images = parse (inputStream, currBundleNumber + 1);
        double progress = PARSE_END_PROGRESS;
        postProgress((int) progress);
        if (images != null && images.size() > 0) {
            double parseProgressPerImage = (PercentProgressListener.PROGRESS_COMPLETE - PARSE_END_PROGRESS) / (double) images.size();
            for (Image img : images) {
                if (mListener != null && mListener.isSyncCancelled()) {
                    break;
                }
                img.saveToDatabase(mContext);
                progress += parseProgressPerImage;
                postProgress((int) progress);
            }
        }
        if (!mListener.isSyncCancelled()) {
            if (mHighestReadBundleNumber > currBundleNumber) {
                prefs.edit().putInt(ImageManager.PREFERENCES_KEY_IMAGE_MANAGER_VERSION, mHighestReadBundleNumber).apply();
            }
            Log.d("Image", "Parsed and synced bundles: Loaded images from XML with highest read number= " + mHighestReadBundleNumber + ": " + images);
            postProgress(PercentProgressListener.PROGRESS_COMPLETE);
        } else {
            Log.d("Image", "Parsing for image sync cancelled.");
        }
        return images;
    }

    private void postProgress(int progress) {
        if (mListener != null) {
            mListener.onSyncProgress(progress);
        }
    }

    private List<Image> parse(InputStream in, int startBundleNumber) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readImageBundles(parser, startBundleNumber);
        } finally {
            in.close();
        }
    }


    private List<Image> readImageBundles(XmlPullParser parser, int startBundleNumber)  throws XmlPullParserException, IOException {
        List<Image> images = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_ALL_BUNDLES_NAME);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(TAG_BUNDLE_NAME)) {
                String bundleNumberRaw = parser.getAttributeValue(null, BUNDLE_ATTRIBUTE_VERSION_NAME);
                int bundleNumber;
                try {
                    bundleNumber = Integer.parseInt(bundleNumberRaw);
                } catch (NumberFormatException nfe) {
                    throw new XmlPullParserException("Bundle version number not a number: " + bundleNumberRaw);
                }
                if (bundleNumber >= startBundleNumber) {
                    List<Image> bundleImages = readBundle(parser);
                    mReadBundles.put(bundleNumber, bundleImages);
                    images.addAll(bundleImages);
                    postProgress((int) (PARSE_START_PROGRESS + (PARSE_END_PROGRESS - PARSE_START_PROGRESS) * bundleNumber / (double) ImageManager.ESTIMATED_BUNDLE_COUNT));
                    mHighestReadBundleNumber = Math.max(mHighestReadBundleNumber, bundleNumber); // so the bundles should be in ascending order in case of exceptions
                } else {
                    skip(parser);
                }
            } else {
                skip(parser);
            }
        }
        return images;
    }

    private List<Image> readBundle(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_BUNDLE_NAME);
        List<Image> bundleImages = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(TAG_IMAGE_NAME)) {
                bundleImages.add(readImage(parser));
            } else {
                skip(parser);
            }
        }
        return bundleImages;
    }

    private Image readImage(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_IMAGE_NAME);
        Image.Builder builder = new Image.Builder();
        boolean newImage = true;
        String resName = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(ImageTable.COLUMN_HASH)) {
                String hash = readTextChecked(parser, ImageTable.COLUMN_HASH);
                if (!TextUtils.isEmpty(hash)) {
                    builder.setHash(hash);
                    newImage = false;
                }
            } else if (name.equals(ImageTable.COLUMN_RESNAME)) {
                resName = readTextChecked(parser, ImageTable.COLUMN_RESNAME);
                builder.setResourceName(mContext, resName);
            } else if (name.equals(ImageTable.COLUMN_SOLUTIONS)) {
                builder.setSolutions(readSolutions(parser));
            } else if (name.equals(ImageTable.COLUMN_AUTHOR)) {
                builder.setAuthor(readAuthor(parser));
            } else if (name.equals(ImageTable.COLUMN_RIDDLEPREFTYPES)) {
                builder.setPreferredRiddleTypes(readPreferredRiddleTypes(parser));
            } else if (name.equals(ImageTable.COLUMN_RIDDLEREFUSEDTYPES)) {
                builder.setRefusedRiddleTypes(readRefusedRiddleTypes(parser));
            } else {
                skip(parser);
            }
        }
        if (newImage && !TextUtils.isEmpty(resName)) {
            Log.d("Image", "Calculating for image: " + resName);
            int resId = ImageUtil.getDrawableResIdFromName(mContext, resName);
            if (resId == 0) {
                throw new XmlPullParserException("No resource for image with name " + resName + " found. Renamed file?");
            }
            builder.calculateHashAndPreferences(ImageUtil.loadBitmap(mContext.getResources(), resId, 0, 0, true));
        }
        try {
            return builder.build();
        } catch (BuildException be) {
            throw new XmlPullParserException("Could not parse image: " + be.getMessage());
        }
    }

    private List<Solution> readSolutions(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, ImageTable.COLUMN_SOLUTIONS);
        List<Solution> solutions = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(TAG_SOLUTION_NAME)) {
                solutions.add(readSolution(parser));
            } else {
                skip(parser);
            }
        }
        return solutions;
    }

    private Solution readSolution(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_SOLUTION_NAME);
        Tongue tongue = null;
        List<String> solutionWords = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            if (parser.getName().equals(TAG_SOLUTION_TONGUE_NAME)) {
                tongue = Tongue.getByShortcut(readTextChecked(parser, TAG_SOLUTION_TONGUE_NAME));
            } else if (parser.getName().equals(TAG_SOLUTION_WORD_NAME)) {
                String solWord = readTextChecked(parser, TAG_SOLUTION_WORD_NAME);
                if (!TextUtils.isEmpty(solWord)) {
                    solutionWords.add(solWord);
                }
            } else {
                skip(parser);
            }
        }
        if (solutionWords.isEmpty() || tongue == null) {
            throw new XmlPullParserException("No tongue or solution words.");
        }
        return new Solution(tongue, solutionWords);
    }

    private ImageAuthor readAuthor(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, ImageTable.COLUMN_AUTHOR);
        String name = null;
        String source = null;
        String license = null;
        String title = null;
        String extras = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            if (parser.getName().equals(TAG_AUTHOR_NAME)) {
                name = readTextChecked(parser, TAG_AUTHOR_NAME);
            } else if (parser.getName().equals(TAG_AUTHOR_SOURCE)) {
                source = readTextChecked(parser, TAG_AUTHOR_SOURCE);
            } else if (parser.getName().equals(TAG_AUTHOR_LICENSE)) {
                license = readTextChecked(parser, TAG_AUTHOR_LICENSE);
            } else if (parser.getName().equals(TAG_AUTHOR_TITLE)) {
                title = readTextChecked(parser, TAG_AUTHOR_TITLE);
            } else if (parser.getName().equals(TAG_AUTHOR_EXTRAS)) {
                extras = readTextChecked(parser, TAG_AUTHOR_EXTRAS);
            } else {
                skip(parser);
            }
        }
        if (TextUtils.isEmpty(name)) {
            throw new XmlPullParserException("Missing ImageAuthor");
        }
        return new ImageAuthor(name, source, license, title, extras);
    }

    private List<RiddleType> readPreferredRiddleTypes(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, ImageTable.COLUMN_RIDDLEPREFTYPES);
        List<RiddleType> types = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            if (parser.getName().equals(TAG_RIDDLE_TYPE_NAME)) {
                String data = readTextChecked(parser, TAG_RIDDLE_TYPE_NAME);
                if (!TextUtils.isEmpty(data)) {
                    RiddleType type = RiddleType.getInstance(data);
                    if (type != null) {
                        types.add(type);
                    }
                }
            } else {
                skip(parser);
            }
        }
        return types;
    }

    private List<RiddleType> readRefusedRiddleTypes(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, ImageTable.COLUMN_RIDDLEREFUSEDTYPES);
        List<RiddleType> types = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            if (parser.getName().equals(TAG_RIDDLE_TYPE_NAME)) {
                String data = readTextChecked(parser, TAG_RIDDLE_TYPE_NAME);
                if (!TextUtils.isEmpty(data)) {
                    RiddleType type = RiddleType.getInstance(data);
                    if (type != null) {
                        types.add(type);
                    }
                }
            } else {
                skip(parser);
            }
        }
        return types;
    }

    private String readTextChecked(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, tag);
        String text = readText(parser);
        parser.require(XmlPullParser.END_TAG, NAMESPACE, tag);
        return text;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = null;
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
