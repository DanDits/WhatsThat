package dan.dit.whatsthat.image;

import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import dan.dit.whatsthat.storage.ImageTable;

/**
 * Created by daniel on 18.04.15.
 */
public class ImageXmlWriter {
    private String writeXml(List<Image> imageBundle, int bundleNumber) throws IOException{
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter(); // TODO save to file named by bundle number
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag(ImageXmlParser.NAMESPACE, ImageXmlParser.TAG_BUNDLE_NAME);
            serializer.attribute(ImageXmlParser.NAMESPACE, ImageXmlParser.BUNDLE_ATTRIBUTE_VERSION_NAME, String.valueOf(bundleNumber));
            for (Image image: imageBundle){
                //TODO save image

                serializer.startTag(ImageXmlParser.NAMESPACE, ImageTable.COLUMN_HASH);
                serializer.attribute("", "date", msg.getDate());
                serializer.startTag("", "title");
                serializer.text(msg.getTitle());
                serializer.endTag("", "title");
                serializer.startTag("", "url");
                serializer.text(msg.getLink().toExternalForm());
                serializer.endTag("", "url");
                serializer.startTag("", "body");
                serializer.text(msg.getDescription());
                serializer.endTag("", "body");
                serializer.endTag("", "message");
            }
            serializer.endTag(ImageXmlParser.NAMESPACE, ImageXmlParser.TAG_BUNDLE_NAME);
            serializer.endDocument();
            return writer.toString();
    }
}
