/*
 * This source file was generated by the Gradle 'init' task
 */
package simplemonatserechnung;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.logging.log4j.Logger;
import org.mustangproject.BankDetails;
import org.mustangproject.Invoice;
import org.mustangproject.Product;
import org.mustangproject.ZUGFeRD.IZUGFeRDExportableItem;
import org.mustangproject.ZUGFeRD.Profiles;
import org.mustangproject.ZUGFeRD.ZUGFeRDExporterFromA1;
import org.mustangproject.validator.ZUGFeRDValidator;
import org.xml.sax.SAXException;

import simplemonatserechnung.model.data.Firma;
import simplemonatserechnung.model.data.Freitext;
import simplemonatserechnung.model.data.KennungsHandler;
import simplemonatserechnung.model.data.Kunde;
import simplemonatserechnung.model.data.Rechnung;
import simplemonatserechnung.model.data.RechnungsPosition;
import simplemonatserechnung.model.data.UstFall;
import simplemonatserechnung.model.data.Ware;
import simplemonatserechnung.mustang.MyItem;
import simplemonatserechnung.mustang.MyTransactionCalculator;
import simplemonatserechnung.mustang.MyZUGFeRD2PullProvider;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.logging.log4j.LogManager;

public class App {
    private static final Logger LOGGER = LogManager.getRootLogger();

        // Konstanten mit Dateinamen der YAML-Dateien
    public static final String FIRMA_YAML_FILE = "firma.yaml";
    public static final String UST_FAELLE_YAML = "ust-faelle.yaml";
    public static final String KUNDEN_YAML = "kunden.yaml";
    public static final String WAREN_YAML = "waren.yaml";
    public static final String RECHNUNG_YAML = "rechnung.yaml";

    // null zum Disablen
    public static final String XML_DUMP = "xrechnung.xml";

    static KennungsHandler<Ware> kh_ware = null;
    static KennungsHandler<UstFall> kh_ustfall = null;
    static KennungsHandler<Kunde> kh_kunde = null;
    static Firma firma = null;
    static Rechnung rechnung = null;

    static Path pdfpath = null;
    static String rgYamlFilename = null;

    public static void leseYamlDateien() {
        LOGGER.info("Lese Yaml-Dateien");

        // Kennungshandler kh_XXXXX sind global

        List<UstFall> ustfaelle = YamlLeser.leseMulti(UstFall.class, UST_FAELLE_YAML, "Ust-Fälle", 1);
        kh_ustfall = new KennungsHandler<UstFall>(ustfaelle);
        LOGGER.debug(kh_ustfall.printlist(" Ustfall"));

        List<Kunde> kunden = YamlLeser.leseMulti(Kunde.class, KUNDEN_YAML, "Kunden", 2);
        kh_kunde = new KennungsHandler<Kunde>(kunden);
        LOGGER.debug(kh_kunde.printlist(" Kunde"));

        List<Ware> waren = YamlLeser.leseMulti(Ware.class, WAREN_YAML, "Waren", 3);
        kh_ware = new KennungsHandler<Ware>(waren);
        LOGGER.debug(kh_ware.printlist(" Ware"));

        // firma: global
        firma = YamlLeser.leseSingle(Firma.class, FIRMA_YAML_FILE, 10);
        System.out.println("Rechnungsersteller: " + firma.getName());
        System.out.println("Steuernummer: " + Utils.null2String(firma.getSteuernummer()));
        System.out.println("USt-ID: " + Utils.null2String(firma.getUmsatzsteueridentifikationsnummer()));

        // rechnung: global
        rechnung = YamlLeser.leseSingle(Rechnung.class, rgYamlFilename, 11);
        System.out.println("RgNr: " + rechnung.getRechnungsnummer());
    }

    public static Invoice fuelleInvoice(Firma firma, Rechnung rechnung) {
        String kunde_s = rechnung.getKunde();
        Kunde kunde = kh_kunde.getEintrag(kunde_s);
        if (kunde == null) {
            LOGGER.error("Kunde {} nicht in Kundenstammdaten", kunde_s);
            return null;
        }
        UstFall ustfall = kh_ustfall.getEintrag(kunde.getSteuerfall());
        if (ustfall == null) {
            LOGGER.error("Kein Steuerfall für Kunde {} definiert", kunde_s);
            return null;
        }
        LOGGER.debug("Kunde Steuerfall: {}", ustfall);
        LOGGER.debug("Kunde Land: {}", kunde.getLand());

        Invoice i = new Invoice()
            .setDueDate(rechnung.getFaelligkeitsdatum())
            .setIssueDate(rechnung.getRechnungsdatum())
            .setCurrency(rechnung.getWaehrung())
            // .setDeliveryDate(new Date())
            .setDetailedDeliveryPeriod(rechnung.getPeriode_start(), rechnung.getPeriode_ende())
            .setSender(
                firma.getTradePartyAddress()
                    .addTaxID(firma.getSteuernummer())
                    .addVATID(firma.getUmsatzsteueridentifikationsnummer())
                    .addBankDetails(
                        new BankDetails(firma.getIban(), firma.getBic()))
            )
            .setRecipient(
                kunde.getTradePartyAddress()
                    .addTaxID(kunde.getSteuernummer())
                    .addVATID(kunde.getUmsatzsteueridentifikationsnummer())
            )
            // Notwendig bei EU-Rechnungen [ID FX-SCH-A-000145]
            .setDeliveryAddress(kunde.getTradePartyAddress())
            // .setReferenceNumber("991-01484-64") // leitweg-id
            .setNumber(rechnung.getRechnungsnummer());

        LOGGER.debug("Invoice Ship Land: {}", i.getShipToCountry());

        for (Freitext ftext : rechnung.getFreitexte()) {
            /*
             * The mustang library has no general public constructor for IncludeNodes.
             * Only specialized constructors can be called.
             * When Mustang expands list in `SubjectCode.java` the switch command will
             * not compile because ALL Enum members have to be defined.
             * When reading the YAML file only defined Enum members can be used.
             */
            String text = ftext.getText();
            if (text != null) {
                if (ftext.getSubjectCode() == null) {
                    i.addNote(text);
                } else {
                    switch (ftext.getSubjectCode()) {
                        case AAI:
                            i.addGeneralNote(text);
                            break;
                        case REG:
                            i.addRegulatoryNote(text);
                            break;
                        case ABL:
                            i.addLegalNote(text);
                            break;
                        case CUS:
                            i.addCustomsNote(text);
                            break;
                        case SUR:
                            i.addSellerNote(text);
                            break;
                        case TXD:
                            i.addTaxNote(text);
                            break;
                        case ACY:
                            i.addIntroductionNote(text);
                            break;
                        case AAK:
                            i.addDiscountBonusNote(text);
                            break;
                    }
                }
            }
        }

        for (RechnungsPosition item : rechnung.getPositionen()) {
            Ware ware = kh_ware.getEintrag(item.getProdukt());

            BigDecimal preis = ware.getPreis();
            if (item.getPreis() != null) {
                preis = item.getPreis();
            }

            LOGGER.debug("Ware {}", ware.getBezeichnung());
            LOGGER.debug("Preis {}", preis.toString());

            Product newProduct = new Product( // name, description, unit, VATPercent
                ware.getBezeichnung(),
                Utils.null2String(item.getBeschreibung()),
                ware.getEinheit(),
                ustfall.getRate()
            );

            newProduct.setTaxCategoryCode(ustfall.getCode());
            newProduct.setTaxExemptionReason(ustfall.getExcemptionreason());

            MyItem newItem = new MyItem(
                newProduct,
                preis,
                item.getAnzahl()
            );
            newItem = newItem
                .setContactPerson(item.getAnsprechpartner())
                .setBestellDatum(item.getBestelldatum())
                .setBestellnummer(item.getBestellnummer())
                .setLieferDatum(item.getLieferdatum());

            i.addItem(newItem);
        }
        return i;
    }

    public static String addXMLtoPDF(String pdffilename, String theXML) {
        String ext_pdffilename = Utils.extendFilenameBeforExtension(pdffilename.toString(), "_zugferd");
        LOGGER.debug("Lade PDF-Rechnung: {}", pdffilename);

        try {
            @SuppressWarnings("resource")
            ZUGFeRDExporterFromA1 ze = new ZUGFeRDExporterFromA1().load(pdffilename);

            // Unser erzeugtes XML nehmen und einbauen
            ze.setXML(theXML.getBytes());
            LOGGER.info("Writing ZUGFeRD-PDF: {}", ext_pdffilename);
            ze.export(ext_pdffilename);
            LOGGER.debug("ZUGFeRD-PDF wurde geschrieben: {}", ext_pdffilename);
            return ext_pdffilename;

        } catch (IOException e) {
            LOGGER.fatal("PDF-Rechnung {} nicht ok", pdffilename);
            System.err.println("PDF-Rechnung " + pdffilename + " nicht ok.");
            System.err.println(e.getMessage());
            return null;
        }
    }

    public static int checkNodeStatus(Document doc, String xpath) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        Node nodeX;
        try {
            nodeX = (Node) xPath.compile(xpath).evaluate(doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            LOGGER.error("xpath '{}' nicht ok.", xpath);
            return -1;
        }
        if (nodeX == null) {
            // Node nicht gefunden
            LOGGER.error("Ergebnis-Node '{}' nicht gefunden.", xpath);
            return -1;
        }

        // Ergebnis steht im Attribut "status"
        // <summary status="valid"/>
        String valres = nodeX.getAttributes().getNamedItem("status").getTextContent();
        // "valid"
        LOGGER.debug("Validierungsergebnis ist: {}", valres);
        if (valres.equals("valid")) {
            return 1;
        }
        if (valres.equals("invalid")) {
            return 0;
        }
        return -1;
    }

    /**
     * Validiere zusammengesetztes PDF
     *
     * @param filename Dateiname des PDFs
     * @return null, falls ok, sonst Grund des Fehlers
     */
    public static String validatePDF(String filename) {
        // Validierung durchführen
        // Ergebnis im String validation_result_xml
        LOGGER.info("Validierung {}", filename);
        ZUGFeRDValidator zfv=new ZUGFeRDValidator();
        String validation_result_xml = zfv.validate(filename);

        StringBuilder result = new StringBuilder();

        // Ergebnis nach Document wandeln und Ergebnis auslesen
        Document validation_doc;
        try {
            validation_doc = Utils.convertXMLStringToDocument(validation_result_xml);
            if (validation_doc == null) {
                LOGGER.error("Validierung erzeugte kein valides XML");
                result.append("Ergebnis des Validierers nicht in erwarteter Form");
            } else {
                // Prüfe Nodes im XML:
                // PDF: /validation/pdf/summary (status)
                // XML: /validation/xml/summary (status)
                // Gesamt: /validation/summary (status)

                switch (checkNodeStatus(validation_doc, "/validation/pdf/summary")) {
                    case 0:
                        result.append("PDF-Teil: nicht valide\n");
                        LOGGER.error("PDF-Teil: nicht valide");
                        break;
                    case 1:
                        result.append("PDF-Teil: valide\n");
                        break;
                    case -1:
                        result.append("keine Bewertung für PDF-Teil gefunden\n");
                }
                switch (checkNodeStatus(validation_doc, "/validation/xml/summary")) {
                    case 0:
                        result.append("XML-Teil: nicht valide\n");
                        LOGGER.error("XML-Teil: nicht valide");
                        break;
                    case 1:
                        result.append("XML-Teil: valide\n");
                        break;
                    case -1:
                        result.append("keine Bewertung für XML-Teil gefunden\n");
                }
                switch (checkNodeStatus(validation_doc, "/validation/summary")) {
                    case 0:
                        result.append("Gesamt-Datei: nicht valide\n");
                        LOGGER.error("Gesamt-Datei: nicht valide");
                        break;
                    case 1:
                        // Gesamtvalidierung bestanden
                        return null;
                }
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            LOGGER.error("Validierer erzeugt kein valides XML");
            result.append("Validierer erzeugt kein valides XML");
        }

        System.out.println("Validierungs-Ergebnis (XML:)");
        System.out.println(validation_result_xml);

        return result.toString();
    }

    public static String currencyFormat(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    };

    public static void simpleConsoleInvoice(Invoice rg, MyTransactionCalculator mytc) {
        final SimpleDateFormat germanDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        System.out.printf("Rechnungsnummer: %s%n", rg.getNumber());
        System.out.printf("Rechnungsdatum.: %s%n", germanDateFormat.format(rg.getIssueDate()));
        System.out.printf("Kunde..........: %s%n", rg.getRecipient().getName());
        System.out.printf("Währung........: %s%n%n", rg.getCurrency());

        for (final IZUGFeRDExportableItem currentItem : rg.getZFItems()) {
            MyItem item = (MyItem) currentItem;
            String description = item.getProduct().getName();
            if (!item.getProduct().getDescription().isEmpty()) {
                description += " - " + item.getProduct().getDescription();
            }
            System.out.printf("%3d %8s %5s%% %8s %8s - %s%n",
                    item.getLineID(),
                    currencyFormat(item.getItemNetto(), 2),
                    item.getProduct().getVATPercent(),
                    currencyFormat(item.getItemTax(), 2),
                    currencyFormat(item.getItemNetto()
                            .add(item.getItemTax()), 2),
                    description);
        }
        System.out.printf("\ntotal.......: %9s%n", currencyFormat(mytc.getTotal(), 2));
        System.out.printf("taxBasis....: %9s%n", currencyFormat(mytc.getTaxBasis(), 2));
        System.out.printf("taxTotal....: %9s%n", currencyFormat(mytc.getGrandTotal().subtract(mytc.getTotal()), 2));
        System.out.printf("grandTotal..: %9s%n", currencyFormat(mytc.getGrandTotal(), 2));
        System.out.printf("totalPrepaid: %9s%n", currencyFormat(mytc.getTotalPrepaid(), 2));
        System.out.printf("duePayable..: %9s%n", currencyFormat(mytc.getGrandTotal().subtract(mytc.getTotalPrepaid()), 2));
    }

    static void kommandozeileAuswerten(String[] args) {
        /*
         * erster Parameter: Name des zu ergänzenden PDFs
         * -> pdfpath (globale Variable)
         * zweiter Parameter: (optional) Name der Rechnungs-YAML-Datei
         * -> rgYamlFilename (default = RECHNUNG_YAML)
         */
        LOGGER.info("Auswertung Kommandozeile");

        if (args.length == 0) {
            System.err.println("Bitte PDF-Datei angeben");
            System.exit(1);
        };
        if (args.length >= 1) {
            String fname = args[0];
            pdfpath = Paths.get(fname).normalize();
            if (Files.notExists(pdfpath)) {
                System.err.println("PDF-Datei '" + fname + "' existiert nicht");
                System.exit(2);
            }
        }
        if (args.length >= 2) {
            rgYamlFilename = args[1];
            Path rgYamlPath = Paths.get(rgYamlFilename).normalize();
            if (Files.notExists(rgYamlPath)) {
                System.err.println("Datei mit Rechnungsdaten '" + rgYamlFilename + "' existiert nicht");
                System.exit(3);
            }
            LOGGER.info("Verwende angegebene Rg-YAML-Datei: " + rgYamlFilename);
        };
        if (args.length >= 3) {
            System.err.println("Zu viele Parameter angegeben");
            System.exit(3);
        }

        if (rgYamlFilename == null) {
            rgYamlFilename = RECHNUNG_YAML;
            Path rgYamlPath = Paths.get(RECHNUNG_YAML).normalize();
            if (Files.notExists(rgYamlPath)) {
                System.err.println(rgYamlFilename + " existiert nicht");
                System.exit(4);
            }
            LOGGER.info("Verwende Standard-Rg-YAML-Datei: " + rgYamlFilename);
        }
    }

    public static void main(String[] args) {
        System.out.println("Arbeitsverzeichnis = " + System.getProperty("user.dir"));

        kommandozeileAuswerten(args);

        leseYamlDateien();

        Invoice rg = fuelleInvoice(firma, rechnung);
        if (rg == null) {
            System.err.println("Fehler beim Zusammentragen der Rechnungsdaten");
            System.exit(10);
        };

        LOGGER.info("Erstelle XML");
        MyZUGFeRD2PullProvider zf2p = new MyZUGFeRD2PullProvider();
        // zf2p.setTest();     // nicht implementiert
        // Basic, Extended
        zf2p.setProfile(Profiles.getByName("Extended"));
        zf2p.generateXML(rg);
        String theXML = new String(zf2p.getXML());

        // vom PullProvider erzeugte Zwischenergebnisse, die später
        // woanders auch ausgegeben werden
        MyTransactionCalculator mytc = zf2p.getMyTransactionCalculator();

        if (XML_DUMP != null) {
            // Schreibe XML als eigene Datei
            try {
                LOGGER.info("Schreibe XML: {}", XML_DUMP);
                BufferedWriter writer = new BufferedWriter(new FileWriter(XML_DUMP));
                writer.write(theXML);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String newpdf_filename = addXMLtoPDF(pdfpath.toString(), theXML);
        if(newpdf_filename == null) {
            System.exit(12);
        }

        String errorString = validatePDF(newpdf_filename);
        if (errorString == null) {
            System.out.printf("Validierung von '%s' erfolgreich.%nBitte Zahlen bitte mit sichtbarem PDF-Inhalt vergleichen.%n%n",
                    newpdf_filename);

            // Gebe abgespeckte Rechnungsdaten auf der Conole erst aus,
            // wenn die Validierung bestanden wurde
            simpleConsoleInvoice(
                rg,
                mytc
            );
        } else {
            System.out.println(errorString);
            LOGGER.error("Validierung der ZUGFeRD-Datei '{}' fehlgeschlagen", newpdf_filename);
            Path zpdf = Paths.get(newpdf_filename);
            try {
                Files.deleteIfExists(zpdf);
                System.out.println("ZUGFeRD-Datei wird gelöscht");
            } catch (IOException e) {
                System.out.println(zpdf + " konnte nicht gelöscht werden");
                LOGGER.error("ZUGFeRD-Datei konnte nicht gelöscht werden.");
            }
        }
    }
}
