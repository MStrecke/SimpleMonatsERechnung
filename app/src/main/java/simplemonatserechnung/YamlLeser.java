package simplemonatserechnung;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class YamlLeser {
    private static final Logger LOGGER = LogManager.getRootLogger();

    /**
     * Lese eine Yaml-Datei mit nur einem Datensatz in eine Variable
     *
     * @param <T>       Typ der Variable
     * @param cls       Class der Variable
     * @param filename  Dateipfad aus dem gelesen werden soll
     * @param errorcode Fehlercode für den Abbruch
     * @return
     */
    public static <T> T leseSingle(Class<T> cls, String filename, int errorcode) {
        LOGGER.debug("YAML Single: {}", filename);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();

        try {
            T obj = mapper.readValue(new File(filename), cls);
            return obj;
        } catch (Exception e) {
            System.out.println("Exception:" + e.toString());
            System.exit(errorcode);
        }
        return null;
    }

    /***
     * Einlesen einer Yaml-Datei mit mehreren Abschnitten
     *
     * @param <T>          Klasse, in die eingelesen werden soll
     * @param cls          klasse.class
     * @param filename     Pfad zur Datei, die eingelesen werden soll
     * @param beschreibung Teil der Beschreibung bei einem Fehler
     * @param error        Fehlercode für Abbruch
     * @return
     */
    public static <T> List<T> leseMulti(Class<T> cls, String filename, String beschreibung, Integer error) {
        LOGGER.debug("YAML Multi: {}", filename);
        try {
            InputStream inputStream = new FileInputStream(new File(filename));
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

            JsonParser yamlParser = mapper.getFactory().createParser(inputStream);
            List<T> docs = mapper.readValues(
                    yamlParser,
                    cls)
                    .readAll();
            return docs;
        } catch (Exception e) {
            System.out.println("Fehler beim Einlesen der " + beschreibung + ": " + e.toString());
            System.exit(error);
        }
        return null;
    };
}
