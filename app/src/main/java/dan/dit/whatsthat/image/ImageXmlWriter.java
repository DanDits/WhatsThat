package dan.dit.whatsthat.image;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import dan.dit.whatsthat.preferences.User;
import dan.dit.whatsthat.riddle.types.RiddleType;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.storage.ImageTable;
import dan.dit.whatsthat.util.IOUtil;
import dan.dit.whatsthat.util.image.ExternalStorage;

/**
 * I contrast to the {@see ImageXmlParser} this is the counter part class
 * that writes a list of images to a bundle that can be read the parser.
 * Created by daniel on 18.04.15.
 */
class ImageXmlWriter {
    public static final int RESULT_SUCCESS = -1;
    public static final int RESULT_NONE = 0;
    public static final int RESULT_ILLEGAL_ARGUMENT = 1;
    public static final int RESULT_EXTERNAL_STORAGE_PROBLEM = 2;
    public static final int RESULT_TARGET_BUNDLE_EXISTS = 3;
    public static final int RESULT_NO_TEMP_DIRECTORY = 4;
    public static final int RESULT_XML_WRITE_FAILED = 5;
    public static final int RESULT_XML_WRITE_SUCCESS = 6;
    public static final int RESULT_ZIP_FAILED = 7;

    private static final String BUILD_DIRECTORY_NAME = ".build";

    private ImageXmlWriter() {}

    public static int writeBundle(Context context, List<BundleCreator.SelectedBitmap> imageBundle, String bundleOrigin, String bundleName) {
        if (context == null || imageBundle == null || TextUtils.isEmpty(bundleName)) {
            return RESULT_ILLEGAL_ARGUMENT;
        }
        File dir = BundleManager.ensureBundleDirectory();
        if (dir == null) {
            return RESULT_EXTERNAL_STORAGE_PROBLEM;
        }

        File targetZip = BundleManager.makeBundleFile(dir, bundleOrigin, bundleName);
        if (targetZip.exists()) {
            return RESULT_TARGET_BUNDLE_EXISTS;
        }
        File tempDir = User.getTempDirectory();
        if (tempDir == null) {
            return RESULT_NO_TEMP_DIRECTORY;
        }
        File targetDataXml = new File(tempDir, bundleName + ".xml");
        FileOutputStream output = null;
        boolean success = false;
        List<Image> images = new ArrayList<>(imageBundle.size());
        for (BundleCreator.SelectedBitmap bitmap : imageBundle) {
            images.add(bitmap.mImage);
        }
        try {
            output = new FileOutputStream(targetDataXml);
            success = writeXml(output, images, bundleOrigin, 0);
        } catch (IOException ioe) {
            Log.e("Image", "Bundle XMLWRITE: Could not create file or save to file: " + ioe);
        } catch (IllegalStateException state) {
            Log.e("Image", "Bundle XMLWRITE: Illegal state for serializer: " + state);
        } catch (IllegalArgumentException arg) {
            Log.e("Image", "Bundle XMLWRITE: Illegal argument for serializer: " + arg);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ioe) {
                    // final failure, ignore
                }
            }
        }
        int result = success ? RESULT_XML_WRITE_SUCCESS : RESULT_XML_WRITE_FAILED;
        if (success) {
            // now zip everything together
            List<File> toZip = new ArrayList<>(1 + imageBundle.size());
            for (BundleCreator.SelectedBitmap bitmap : imageBundle) {
                toZip.add(bitmap.mPathInTemp);
            }
            toZip.add(targetDataXml);
            result = RESULT_ZIP_FAILED;
            try {
                if (IOUtil.zip(toZip, targetZip)) {
                    result = RESULT_SUCCESS;
                    User.clearTempDirectory();
                }
            } catch (IOException e) {
                Log.e("Image", "Failed zipping files " + toZip + " into " + targetZip);
            }
        }
        return result;
    }

    /**
     * Writes a list of images to an xml file contained in the external storage directory under
     * WhatsThat/build, suffixed with the given bundle number. Does nothing if given bundle number is null
     * or empty or if there is no context given.
     * @param context The context.
     * @param imageBundle The list of images to write to xml.
     * @param bundleNumber The number of the bundle, suffix of the file name.
     */
    public static void writeBundle(Context context, List<Image> imageBundle, int bundleNumber) {
        if (context == null || imageBundle == null || imageBundle.isEmpty()) {
            return;
        }
        String path = ExternalStorage.getExternalStoragePathIfMounted(BUILD_DIRECTORY_NAME);
        if (path == null) {
            return;
        }
        File dir = new File(path);
        if (dir.mkdirs() || dir.isDirectory()) {
            String fullName = path + "/imagedata" + bundleNumber + ".xml";

            // Write to file.
            File file = new File(fullName);
            FileOutputStream output = null;
            boolean success = false;
            try {
                output = new FileOutputStream(file);
                success = writeXml(output, imageBundle, null, bundleNumber);
            } catch (IOException ioe) {
                Log.e("Image", "XMLWRITE: Could not create file or save to file: " + ioe);
            } catch (IllegalStateException state) {
                Log.e("Image", "XMLWRITE: Illegal state for serializer: " + state);
            } catch (IllegalArgumentException arg) {
                Log.e("Image", "XMLWRITE: Illegal argument for serializer: " + arg);
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException ioe) {
                        // final failure, ignore
                    }
                }
            }
            if (success) {
                Log.d("Image", "Bundle created for " + imageBundle.size() + " images, number " + bundleNumber
                        + " at " + file.getAbsolutePath());
            }
        } else {
            Log.e("Image", "Failed writing bundle " + bundleNumber + " dir not available: " + dir);
        }
    }

    private static boolean writeXml(FileOutputStream outputStream, List<Image> imageBundle, String bundleOrigin, int bundleNumber) throws IOException{
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        serializer.setOutput(writer);
        serializer.startDocument("UTF-8", true);

        serializer.startTag(ImageXmlParser.NAMESPACE, ImageXmlParser.TAG_ALL_BUNDLES_NAME);

        serializer.startTag(ImageXmlParser.NAMESPACE, ImageXmlParser.TAG_BUNDLE_NAME);
        if (bundleOrigin != null) {
            serializer.attribute(ImageXmlParser.NAMESPACE, ImageTable.COLUMN_ORIGIN, bundleOrigin);
        }
        serializer.attribute(ImageXmlParser.NAMESPACE, ImageXmlParser.BUNDLE_ATTRIBUTE_VERSION_NAME, String.valueOf(bundleNumber));
        for (Image image: imageBundle){
            serializer.startTag(ImageXmlParser.NAMESPACE, ImageXmlParser.TAG_IMAGE_NAME);
            writeText(serializer, ImageTable.COLUMN_RESNAME, image.getName());
            writeText(serializer, ImageTable.COLUMN_ORIGIN, image.getOrigin());
            writeText(serializer, ImageTable.COLUMN_HASH, image.getHash());
            writeText(serializer, ImageTable.COLUMN_OBFUSCATION, String.valueOf(image.getObfuscation()));
            writeText(serializer, ImageTable.COLUMN_AVERAGE_COLOR, String.valueOf(image.getAverageColor()));
            writeText(serializer, ImageTable.COLUMN_SAVELOC, image.getRelativePath());
            writeSolutions(serializer, ImageTable.COLUMN_SOLUTIONS, image.getSolutions());
            writeAuthor(serializer, ImageTable.COLUMN_AUTHOR, image.getAuthor());
            writeTypes(serializer, ImageTable.COLUMN_RIDDLEPREFTYPES, image.getPreferredRiddleTypes());
            writeTypes(serializer, ImageTable.COLUMN_RIDDLEREFUSEDTYPES, image.getRefusedRiddleTypes());
            serializer.endTag(ImageXmlParser.NAMESPACE, ImageXmlParser.TAG_IMAGE_NAME);
        }
        serializer.endTag(ImageXmlParser.NAMESPACE, ImageXmlParser.TAG_BUNDLE_NAME);
        serializer.endTag(ImageXmlParser.NAMESPACE, ImageXmlParser.TAG_ALL_BUNDLES_NAME);
        serializer.endDocument();

        serializer.flush();
        String dataWrite = writer.toString();
        outputStream.write(dataWrite.getBytes());
        outputStream.close();
        return true;
    }

    private static void writeTypes(XmlSerializer serializer, String tag, List<RiddleType> types) throws IOException{
        if (types != null) {
            serializer.startTag(ImageXmlParser.NAMESPACE, tag);
            for (RiddleType type : types) {
                writeText(serializer, ImageXmlParser.TAG_RIDDLE_TYPE_NAME, type.getFullName());
            }
            serializer.endTag(ImageXmlParser.NAMESPACE, tag);
        }
    }

    private static void writeAuthor(XmlSerializer serializer, String tag, ImageAuthor author) throws IOException {
        serializer.startTag(ImageXmlParser.NAMESPACE, tag);
        writeText(serializer, ImageXmlParser.TAG_AUTHOR_NAME, author.getName());
        writeText(serializer, ImageXmlParser.TAG_AUTHOR_SOURCE, author.getSource());
        writeText(serializer, ImageXmlParser.TAG_AUTHOR_LICENSE, author.getLicense());
        writeText(serializer, ImageXmlParser.TAG_AUTHOR_TITLE, author.getTitle());
        writeText(serializer, ImageXmlParser.TAG_AUTHOR_EXTRAS, author.getExtras());
        serializer.endTag(ImageXmlParser.NAMESPACE, tag);
    }

    private static void writeSolutions(XmlSerializer serializer, String tag, List<Solution> solutions) throws IOException {
        serializer.startTag(ImageXmlParser.NAMESPACE, tag);
        for (Solution sol : solutions) {
            writeSolution(serializer, ImageXmlParser.TAG_SOLUTION_NAME, sol);
        }
        serializer.endTag(ImageXmlParser.NAMESPACE, tag);
    }

    private static void writeSolution(XmlSerializer serializer, String tag, Solution sol) throws IOException {
        serializer.startTag(ImageXmlParser.NAMESPACE, tag);
        writeText(serializer, ImageXmlParser.TAG_SOLUTION_TONGUE_NAME, sol.getTongue().getShortcut());
        for (String word : sol.getWords()) {
            writeText(serializer, ImageXmlParser.TAG_SOLUTION_WORD_NAME, word);
        }
        serializer.endTag(ImageXmlParser.NAMESPACE, tag);
    }

    private static void writeText(XmlSerializer serializer, String tag, String text) throws IOException {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        serializer.startTag(ImageXmlParser.NAMESPACE, tag);
        serializer.text(text);
        serializer.endTag(ImageXmlParser.NAMESPACE, tag);
    }
}
