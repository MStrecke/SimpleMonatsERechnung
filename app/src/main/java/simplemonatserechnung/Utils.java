package simplemonatserechnung;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;

public class Utils {
    /**
     * Convert XML string to document
     *
     * @param xmlStr string to convert
     * @return XML document
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @note Source:
     *       https://www.digitalocean.com/community/tutorials/java-convert-string-to-xml-document-and-xml-document-to-string
     */
    public static Document convertXMLStringToDocument(String xmlStr)
            throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
        return doc;
    }

    /**
     * convert Document to String
     *
     * @param doc XML document to convert
     * @return String
     * @throws TransformerException
     * @note Source:
     *       https://www.digitalocean.com/community/tutorials/java-convert-string-to-xml-document-and-xml-document-to-string
     */
    public static String convertDocumentToXMLString(Document doc) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;

        transformer = tf.newTransformer();
        // below code to remove XML declaration
        // transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        String output = writer.getBuffer().toString();
        return output;
    }

    /**
     * Return "" if string is null, otherwise return string
     */
    public static String null2String(@Nullable String s) {
        if (s == null) {
            return "";
        }
        ;
        return s;
    };

    /**
     * XML-String to pretty-formatted XML-String
     *
     * @param input  String to format
     * @param indent number of spaces for one indent
     * @return formated String, noll on error
     */
    public static String prettyFormatXML(String input, int indent) {
        Source xmlInput = new StreamSource(new StringReader(input));
        StringWriter stringWriter = new StringWriter();
        StreamResult xmlOutput = new StreamResult(stringWriter);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("indent-number", indent);
        // transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        // transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Adds `addme` before extension
     * @param old old filename
     * @param addme string to add
     * @return new filename
     */
    public static String extendFilenameBeforExtension(String old, String addme) {
        if (old == null) {
            return null;
        };
        if (addme == null) {
            return old;
        }
        String ext = FilenameUtils.getExtension(old);
        String base = FilenameUtils.removeExtension(old);
        return base + addme + FilenameUtils.EXTENSION_SEPARATOR_STR + ext;
    }
}
