package dan.dit.whatsthat.image;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dan.dit.whatsthat.preferences.Tongue;
import dan.dit.whatsthat.riddle.types.RiddleType;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.storage.ImageTable;
import dan.dit.whatsthat.util.BuildException;
import dan.dit.whatsthat.util.PercentProgressListener;
import dan.dit.whatsthat.util.image.BitmapUtil;

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

    public static final String TAG_ALL_BUNDLES_NAME = "imagedata";
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
    private static final String BUNDLE_ATTRIBUTE_ORIGIN = "origin";

    private Context mContext;
    private int mHighestReadBundleNumber = Integer.MIN_VALUE;
    private SparseArray<List<Image>> mReadBundles = new SparseArray<>();
    private SparseArray<String> mReadBundlesOrigin = new SparseArray<>();
    private boolean mModeAbortOnImageBuildFailure;
    private BitmapUtil.ByteBufferHolder mBuffer = new BitmapUtil.ByteBufferHolder();
    private String mCurrOrigin;

    public List<Image> getBundle(int bundleNumber) {
        return mReadBundles.get(bundleNumber);
    }

    public Set<Integer> getReadBundleNumbers() {
        Set<Integer> keys = new HashSet<>(mReadBundles.size());
        for (int i = 0; i < mReadBundles.size(); i++) {
            keys.add(mReadBundles.keyAt(i));
        }
        return keys;
    }

    private ImageXmlParser() {}

    /**
     * Parses all new bundles found in the given stream (xml file).
     * @param context The context.
     * @param inputStream The input to parse.
     * @param startBundleNumber The bundle version number to start parsing (useful to skip old bundles when updating).
     * @param abortOnFailure If true the parser will stop and abort with an Exception if there was an error building an image.
     * @return The parser or null if the inputStream or context was null.
     * @throws XmlPullParserException There was an XML error parsing the data.
     * @throws IOException There was an IO error reading the data.
     */
    public static ImageXmlParser parseInput(Context context, InputStream inputStream, int startBundleNumber, boolean abortOnFailure) throws XmlPullParserException, IOException {
        if (inputStream == null || context == null) {
            return null;
        }
        ImageXmlParser parser = new ImageXmlParser();
        parser.mContext = context;
        parser.mModeAbortOnImageBuildFailure = abortOnFailure;
        parser.parse(inputStream, startBundleNumber);
        Log.d("Image", "Parsed new bundles: Loaded images from XML with highest read number= " + parser.mHighestReadBundleNumber);
        return parser;
    }

    public boolean syncToDatabase(ImageManager.SynchronizationListener listener) {
        if (mReadBundles == null || mReadBundles.size() == 0) {
            return false;
        }
        List<Integer> keyList = new ArrayList<>(getReadBundleNumbers());
        Collections.sort(keyList); // ascending order so that higher version bundles can overwrite older images
        int imageCount = 0;
        for (Integer k : keyList) {
            imageCount += mReadBundles.get(k).size();
        }
        if (imageCount > 0) {
            double progress = 0;
            double parseProgressPerImage = PercentProgressListener.PROGRESS_COMPLETE / (double) imageCount;
            for (Integer version : keyList) {
                for (Image img : mReadBundles.get(version)) {
                    if (listener != null && listener.isSyncCancelled()) {
                        return false;
                    }
                    img.saveToDatabase(mContext);
                    progress += parseProgressPerImage;
                    postProgress((int) progress, listener);
                }
            }
            Log.d("Image", "Synced " + imageCount + " images to database from " + getReadBundlesCount() + " read bundles.");
            return true;
        }
        return false;
    }

    private void postProgress(int progress, ImageManager.SynchronizationListener listener) {
        if (listener != null) {
            listener.onSyncProgress(progress);
        }
    }

    private void parse(InputStream in, int startBundleNumber) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            readImageBundles(parser, startBundleNumber);
        } finally {
            in.close();
        }
    }


    private void readImageBundles(XmlPullParser parser, int startBundleNumber)  throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_ALL_BUNDLES_NAME);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(TAG_BUNDLE_NAME)) {
                String bundleNumberRaw = parser.getAttributeValue(NAMESPACE, BUNDLE_ATTRIBUTE_VERSION_NAME);
                int bundleNumber;
                try {
                    bundleNumber = Integer.parseInt(bundleNumberRaw);
                } catch (NumberFormatException nfe) {
                    throw new XmlPullParserException("Bundle version number not a number: " + bundleNumberRaw);
                }
                String origin = parser.getAttributeValue(NAMESPACE, BUNDLE_ATTRIBUTE_ORIGIN);
                mCurrOrigin = origin;
                if (bundleNumber >= startBundleNumber) {
                    List<Image> bundleImages = readBundle(parser);
                    mReadBundles.put(bundleNumber, bundleImages);
                    mReadBundlesOrigin.put(bundleNumber, origin);
                    mHighestReadBundleNumber = Math.max(mHighestReadBundleNumber, bundleNumber); // so the bundles should be in ascending order in case of exceptions
                } else {
                    skip(parser);
                }
            } else {
                skip(parser);
            }
        }
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
                Image readImage = readImage(parser);
                if (readImage != null) {
                    bundleImages.add(readImage);
                }
            } else {
                skip(parser);
            }
        }
        return bundleImages;
    }

    private Image readImage(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, TAG_IMAGE_NAME);
        Image.Builder builder = new Image.Builder();
        builder.setOrigin(mCurrOrigin);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case ImageTable.COLUMN_HASH:
                    builder.setHash(readTextChecked(parser, ImageTable.COLUMN_HASH));
                    break;
                case ImageTable.COLUMN_RESNAME:
                    builder.setResourceName(mContext, readTextChecked(parser, ImageTable.COLUMN_RESNAME));
                    break;
                case ImageTable.COLUMN_SOLUTIONS:
                    builder.setSolutions(readSolutions(parser));
                    break;
                case ImageTable.COLUMN_AUTHOR:
                    builder.setAuthor(readAuthor(parser));
                    break;
                case ImageTable.COLUMN_RIDDLEPREFTYPES:
                    builder.setPreferredRiddleTypes(readPreferredRiddleTypes(parser));
                    break;
                case ImageTable.COLUMN_RIDDLEREFUSEDTYPES:
                    builder.setRefusedRiddleTypes(readRefusedRiddleTypes(parser));
                    break;
                case ImageTable.COLUMN_ORIGIN:
                    builder.setOrigin(readTextChecked(parser, ImageTable.COLUMN_ORIGIN));
                    break;
                case ImageTable.COLUMN_SAVELOC:
                    builder.setRelativeImagePath(readTextChecked(parser, ImageTable.COLUMN_SAVELOC));
                    break;
                case ImageTable.COLUMN_OBFUSCATION:
                    builder.setObfuscation(readTextChecked(parser, ImageTable.COLUMN_OBFUSCATION));
                    break;
                case ImageTable.COLUMN_AVERAGE_COLOR:
                    builder.setAverageColor(readTextChecked(parser, ImageTable.COLUMN_AVERAGE_COLOR));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        try {
            return builder.build(mContext, mBuffer);
        } catch (BuildException be) {
            if (mModeAbortOnImageBuildFailure) {
                throw new XmlPullParserException("Could not parse image: " + be);
            }
            Log.e("Image", "Failed parsing image, but not aborting: " + be);
            return null; // failure but do not abort
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

    public int getReadBundlesCount() {
        return mReadBundles == null ? 0 : mReadBundles.size();
    }

    public int getHighestReadBundleNumber() {
        return mHighestReadBundleNumber;
    }

    public String getOrigin(Integer bundleNumber) {
        return mReadBundlesOrigin.get(bundleNumber);
    }
}
