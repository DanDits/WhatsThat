package dan.dit.whatsthat.image;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import dan.dit.whatsthat.riddle.types.RiddleType;
import dan.dit.whatsthat.solution.Solution;
import dan.dit.whatsthat.storage.ImageTable;

/**
 * Created by daniel on 18.04.15.
 */
public class ImageXmlWriter {

    public void writeBundle(Context context, List<Image> imageBundle, int bundleNumber) {
        String path = Environment.getExternalStorageDirectory() + "/" + "WhatsThat/build/";
        File dir = new File(path);
        dir.mkdirs();
        String fullName = path + "imagedata" + bundleNumber + ".xml";

        // Write to file.
        File file = new File (fullName);
        FileOutputStream output;
        boolean success = false;
        try {
            output = new FileOutputStream(file);
            success = writeXml(output, imageBundle, bundleNumber);
        } catch (IOException ioe) {
            Log.e("Image", "XMLWRITE: Could not create file or save to file: " + ioe);
        } catch (IllegalStateException state) {
            Log.e("Image", "XMLWRITE: Illegal state for serializer: " + state);
        } catch (IllegalArgumentException arg) {
            Log.e("Image", "XMLWRITE: Illegal argument for serializer: " + arg);
        }
        if (success) {
            Toast.makeText(context, "Bundle created for " + imageBundle.size() + " images, number " + bundleNumber
                    + " at " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean writeXml(FileOutputStream outputStream, List<Image> imageBundle, int bundleNumber) throws IOException{
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag(ImageXmlParser.NAMESPACE, ImageXmlParser.TAG_BUNDLE_NAME);
            serializer.attribute(ImageXmlParser.NAMESPACE, ImageXmlParser.BUNDLE_ATTRIBUTE_VERSION_NAME, String.valueOf(bundleNumber));
            for (Image image: imageBundle){
                serializer.startTag(ImageXmlParser.NAMESPACE, ImageXmlParser.TAG_IMAGE_NAME);
                writeText(serializer, ImageTable.COLUMN_HASH, image.getHash());
                writeText(serializer, ImageTable.COLUMN_RESNAME, image.getName());
                writeSolutions(serializer, ImageTable.COLUMN_SOLUTIONS, image.getSolutions());
                writeAuthor(serializer, ImageTable.COLUMN_AUTHOR, image.getAuthor());
                writeTypes(serializer, ImageTable.COLUMN_RIDDLEPREFTYPES, image.getPreferredRiddleTypes());
                writeTypes(serializer, ImageTable.COLUMN_RIDDLEDISLIKEDTYPES, image.getDislikedRiddleTypes());
                serializer.endTag(ImageXmlParser.NAMESPACE, ImageXmlParser.TAG_IMAGE_NAME);
            }
            serializer.endTag(ImageXmlParser.NAMESPACE, ImageXmlParser.TAG_BUNDLE_NAME);
            serializer.endDocument();
        serializer.flush();
        String dataWrite = writer.toString();
        outputStream.write(dataWrite.getBytes());
        outputStream.close();
        return true;
    }

    private void writeTypes(XmlSerializer serializer, String tag, List<RiddleType> types) throws IOException{
        if (types != null) {
            serializer.startTag(ImageXmlParser.NAMESPACE, tag);
            for (RiddleType type : types) {
                writeText(serializer, ImageXmlParser.TAG_RIDDLE_TYPE_NAME, type.compact());
            }
            serializer.endTag(ImageXmlParser.NAMESPACE, tag);
        }
    }

    private void writeAuthor(XmlSerializer serializer, String tag, ImageAuthor author) throws IOException {
        serializer.startTag(ImageXmlParser.NAMESPACE, tag);
        writeText(serializer, ImageXmlParser.TAG_AUTHOR_NAME, author.getName());
        writeText(serializer, ImageXmlParser.TAG_AUTHOR_SOURCE, author.getSource());
        writeText(serializer, ImageXmlParser.TAG_AUTHOR_LICENSE, author.getLicense());
        writeText(serializer, ImageXmlParser.TAG_AUTHOR_TITLE, author.getTitle());
        writeText(serializer, ImageXmlParser.TAG_AUTHOR_EXTRAS, author.getExtras());
        serializer.endTag(ImageXmlParser.NAMESPACE, tag);
    }

    private void writeSolutions(XmlSerializer serializer, String tag, List<Solution> solutions) throws IOException {
        serializer.startTag(ImageXmlParser.NAMESPACE, tag);
        for (Solution sol : solutions) {
            writeSolution(serializer, ImageXmlParser.TAG_SOLUTION_NAME, sol);
        }
        serializer.endTag(ImageXmlParser.NAMESPACE, tag);
    }

    private void writeSolution(XmlSerializer serializer, String tag, Solution sol) throws IOException {
        serializer.startTag(ImageXmlParser.NAMESPACE, tag);
        writeText(serializer, ImageXmlParser.TAG_SOLUTION_TONGUE_NAME, sol.getTongue().getShortcut());
        for (String word : sol.getWords()) {
            writeText(serializer, ImageXmlParser.TAG_SOLUTION_WORD_NAME, word);
        }
        serializer.endTag(ImageXmlParser.NAMESPACE, tag);
    }

    private void writeText(XmlSerializer serializer, String tag, String text) throws IOException {
        if (text == null) {
            return;
        }
        serializer.startTag(ImageXmlParser.NAMESPACE, tag);
        serializer.text(text);
        serializer.endTag(ImageXmlParser.NAMESPACE, tag);
    }
}
