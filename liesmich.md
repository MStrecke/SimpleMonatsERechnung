# Einführung

Dieser Text befasst sich mit den technischen Details dieses Programms (`SimpleMonatsERechnung`).

# Haftungsauschluss

Ich bin **KEIN** Steuerberater und **KEIN** Rechtsanwalt.
Die Annahmen, die ich in diesem Programm mache, sowie das Programm selbst, können fehlerhaft sein. Für Hinweise bin ich jederzeit dankbar.

# Grund für dieses Programm

Ab 2025 und spätestens ab 2028 ist **jeder** Gewerbetreibende und Freiberufler, der Rechnungen mit Umsatzsteuer schreibt, gesetzlich verpflichtet, eine e-Rechnung zu erstellen. Kurz gesagt: Die Papierrechnung wird abgeschafft (ok, ok... so gut wie abgeschafft).

Die e-Rechnung ist im Prinzip eine XML-Datei in einem vom Gesetz vorgeschriebenen Format, die alle Rechnungsinformationen enthält und maschinenlesbar ist.

Die e-Rechnung kommt in zwei Ausführungen: die *nackte* XML-Datei (XRechnung) oder ein PDF, in die die XML-Datei integriert ist (ZUGFeRD).
Die ZUGFeRD-Variante hat den Vorteil, dass sie von jedermann mit jedem beliebigen PDF-Viewer ohne weitere Software angezeigt werden kann.
Falls man diese Ausführung (PDF+XML) wählt, ist gesetzlich vorgeschrieben, dass beide Ausführungen dasselbe wiedergeben müssen. Das betrifft auch Floskeln wie "erlauben wir uns, Ihnen zu berechnen", "bedanken wir uns für Ihren Einkauf" usw. Steuerrechtlich bindend ist allerdings der XML-Teil.

Die hier vorgestellte Software zeigt, wie man mit einem Minimum an Daten diese Datei erzeugen und in eine ZUGFeRD-PDF-Datei integrieren kann. Sie baut auf den Bedürfnissen eines Übersetzers auf, lässt sich aber auf viele andere Bereiche übertragen.

Die Daten selber sind in YAML-Dateien abgelegt. Das Format lässt sich mit einem normalen Texteditor bearbeiten und ist noch halbwegs menschenlesbar.

Das Programm setzt die Bibliothek des Mustang-Projekts ein. Diese kann:
 - die gewünschte XML-Datei erzeugen
 - diese XML-Datei in eine PDF-Datei integrieren
 - und die so entstandene Datei validieren.

Sie hat aber auch Nachteile:
 - Sie gibt die Art und Weise der Ermittlung der Umsatzsteuer vor.
 - Sie reicht nur für *einfache* Fälle (DL und EU) - die aber für die meisten Anwender ausreichen.
 - Weitergehende Fälle sind im XML-Format zwar vorgesehen, werden aber zurzeit von Mustang nicht umgesetzt:
   - Insbesondere fehlt die Möglichkeit zusätzliche Daten (z. B. unterschiedliche Bestellungen) den Rechnungspositionen zuzuordnen.
   - Auch deckt sie viele Fälle außerhalb der EU nicht ab.
   - Der vorliegende Quellcode zeigt, wie man diese Funktionen trotzdem implementieren kann.

Eine Validierung ist nützlich, um mögliche Formfehler zu erkennen und zu beheben, die als Grund für eine Zurückweisung der Rechnung gelten können.

# Meine "Sonderanforderungen"

## Monatsrechnungen

Der "Normalfall" ist, dass EINE Rechnung für genau EINE Bestellung geschrieben wird, d.h. die Angaben zur Bestellung (Bestellnummer, Bestelldatum usw.) beziehen sich auf die gesamte Rechnung.

Das ist bei mir nicht der Fall. Ich schreibe Monatsrechnungen, bei denen die Bestellungen der Kunden über den Monat gesammelt und am Monatsende zusammen berechnet werden.

In diesem Fall bezieht sich EINE Rechnung auf MEHRERE Bestellungen.

Informationen wie Bestellnummer, Bestell- und Lieferdatum wandern dann vom "Kopf" der Rechnung in die einzelnen Rechnungspositionen. Diese ist im ZUGFeRD-Format vorgesehen (aber nur im Profil "Extended") und wird zurzeit von Mustang leider nur rudimentär unterstützt. Der Quellcode zeigt entsprechende Erweiterungen.


## Unterschiedliche Ansprechpartner in einer Firma

Ich habe teilweise mehrere Ansprechpartner bei einem Kunden und führe den Besteller bisher in meiner Rechnung auf.

Auch dieser Umstand ist im ZUGFeRD-Format vorgesehen. Er ist allerdings auch (noch?) nicht in der Mustang-Bibliothek implementiert. Der Quellcode zeigt, wie man dies trotzdem erreichen kann.

## Kunden im Vereinigten Königreich

Seit dem Brexit ist GB nicht mehr Mitglied der EU, was jede Menge Änderungen im XML nach sich zieht. Das einfachste ist noch die Umstellung des Tax-Codes. Aber je nachdem, durch welchen Validator man das XML dann laufen lässt, bemängeln diese dann das Vorhandensein oder Nichtvorhandensein weiterer Felder - leider sind sie sich dabei nicht einig, welche Felder.

# Validator(en)

Es gibt im Format Hunderte Wenns und Abers. Diese von Hand zu überprüfen wäre sehr zeitaufwendig. Daher verwendet man dazu Programme: Validatoren.
Sie überprüfen sowohl die syntaktische Richtigkeit der XML-Datei, als auch die Beziehungen der Felder untereinander sowie das Format der PDF-Datei.
Die Prüfung ist deshalb wichtig, da Formfehler zur Zurückweisung der Rechnung führen können.

Eine 100%ige Sicherheit ist diese Prüfung aber auch nicht, da es unterschiedliche Validatoren gibt. Außerdem werden die Validatoren ebenfalls weiterentwickelt, sodass sich das Ergebnis von einer zur anderen Version verändern kann.

Das kann zu überraschenden Anforderungen führen: So besteht der Validator nur bei EU-Rechnungen darauf, dass im Header wahlweise ein Lieferdatum (schwierig bei mehreren unterschiedlichen Lieferzeitpunkten) oder ein Lieferzeitraum angegeben werden muss. Dabei ist es ihm egal, dass diese bei den Rechnungspositionen angegeben sind.

Hin und wieder widersprechen sich Validatoren aber auch:
So fordert z. B. ein Validator, dass bei einer Rechnung für England (d.h. außerhalb Europa mit "Not subject to VAT") weder für den Käufer noch für den Verkäufer die USt-ID angegeben werden darf - eigentlich logisch.
Entfernt man diese, beschwert sich der nächste Validator, dass beim Verkäufer die USt-ID fehlt. Es ist also nicht klar, wem man glauben soll.

# Vereinfachungen

Die ZUGFeRD-Spezifikation sieht sehr viele Sonderfälle vor, die - falls sie nicht vorliegen - auch nicht in der XML-Datei angegeben werden müssen. Viele dieser "Vereinfachungen" treffen auf mich - und wahrscheinlich viele Andere - zu:

 * Alle Rechnungen werden in EUR ausgestellt.
 * Die einzige *Steuer*, die in der Rechnung angegeben wird, ist die *Umsatzsteuer*.
 * Ich bin selbst der Zahlungsempfänger und kein Dritter (z. B. eine Factoring-Firma).
 * Der Rechnungsempfänger ist die Firma, die die Rechnung erhält (und z. B. keine Holding, die für ihre Tochtergesellschaft bezahlt).
 * Alle Kunden haben ihren Sitz im SEPA-Raum (können also eine SEPA-Überweisung durchführen).
 * Bei mir gibt es keine Auf- und Abschläge auf den Originalpreis, keine Skonti, Strafzinsen oder Vorauszahlungen.
 * Es gibt keine Lieferscheine oder Wareneingangsbelege.
 * Das Rechnungsdatum ist das Datum der Steuerfälligkeit.
 * Es werden nur Dienstleistungen berechnet, keine Waren (=> Auswirkung auf TaxExcemptionCode/Reason).
 * Behörden sind nicht unter den Kunden (diese verlangen das Format `XRechnung` und nicht `ZUGFeRD`.)

Falls dies bei Ihnen auch der Fall ist, reicht es aus, die Stammdaten anzupassen, um das Programm zu verwenden.

# Stammdaten

Um Daten nicht immer neu eingeben zu müssen, werden diese in YAML-Dateien gespeichert. Nach dem Einrichten muss nur noch `rechnung.yaml` an die jeweilige Rechnung angepasst werden.

Bei allen YAML-Dateien gilt: Dezimalzahlen werden mit einem Dezimalpunkt (!) geschrieben und enthalten kein Tausender-Trennzeichen.

Ein Kalenderdatum wird hingegen in der deutschen Schreibweise `TT.MM.JJJJ` erwartet.

Die Namen in den Feldern der YAML-Dateien sind weitestgehend selbsterklärend.

Kommentare haben ein `#` am Anfang der Zeile.

Dem Programm liegen Beispieldateien bei, die durch die Endung `-example.yaml` zu erkennen sind. Kopieren Sie sie in die entsprechende Datei ohne `-example` und passen Sie sie an Ihre Bedürfnisse an.

## firma.yaml

Diese Datei enthält nur einen Datensatz: die Stammdaten des Rechnungserstellers.

Hinweis: Normalerweise wird der Straßenname in das Feld `adresse1` eingetragen.

## ust-faelle.yaml

Diese Datei enthält drei weit verbreitete Umsatzsteuerfälle, kann ggf. aber an andere Bedürfnisse angepasst werden.

* `kennung` enthält den frei wählbaren internen Namen (hier `standard`, `europa` und `welt`).
* `rate`: Prozentsatz der Steuer (Achtung: Dezimalpunkt!)
* `code`: Code entsprechend [UNTDID 5305](https://www.xrepository.de/details/urn:xoev-de:kosit:codeliste:untdid.5305_1)
* `excemptionreason` ist ein Freitext und nur dann notwendig, wenn 0% Umsatzsteuer berechnet werden (Kleinunternehmer, EU-Rechnung, Welt-Rechnung).

Hinweis: Bei Ländern außerhalb der EU wird steuerrechtlich zwischen Waren und Dienstleistungen unterschieden. `code` und `excemptionreason` müssen dann angepasst werden. Die Beispieldatei enthält nur die Einträge für *Dienstleistungen*.

## kunden.yaml

Diese Datei enthält die Stammdaten mehrerer Kunden, jeweils getrennt durch das YAML-typische `---`.

Die `kennung` enthält wieder eine (frei wählbare) interne Bezeichnung, mit der die Daten später in der Rechnung aufgerufen werden können.
Die Angaben zur Adresse und Steuernummer entsprechen `firma.yaml`.
Hinzu kommt die Art des `steuerfall`s aus `ust-faelle.yaml`, d.h. welche Art von Umsatzsteuer zur Anwendung kommt.

## waren.yaml

Auch diese Datei enthält mehrere Einträge getrennt durch `---`.

* Die `kennung` enthält wieder eine interne Bezeichnung, mit der die Daten später in der Rechnung aufgerufen werden können.
* `bezeichnung` ist eine Langbeschreibung des Artikels für die Rechnung
* `einheit` bezeichnet die Einheit der Leistung nach [UN/ECE Recommendation N°20](https://www.xrepository.de/details/urn:xoev-de:kosit:codeliste:rec20_1) (häufig verwendet: C62 = Stück, LS = Lumb Sum = Pauschal, HUR = Stunden, DAY = Tage, KGM = Kilogramm, H87 = piece, AE = each)
* Die Angabe des `preis` ist optional und kann in der Rechnung überschrieben werden.

## rechnung.yaml

Diese Datei enthält nur einen Datensatz. Dessen Daten bestimmen die Werte im XML.

| Schlüssel         | Wert                                                          |
| ----------------- | ------------------------------------------------------------- |
| kunde             | Kennung des Kunden aus `kunden.yaml`                          |
| rechnungsnummer   | Rechnungsnummer (Pflichtangabe)                               |
| rechnungsdatum    | Datum der Rechnung (Pflichtangabe)                            |
| faelligkeitsdatum | Datum der Fälligkeit (Pflichtangabe)                          |
| periode_start     | Abrechnungsperiode Startdatum (nur Pflicht bei EU-Rechnungen) |
| periode_ende      | Abrechnungsperiode Enddatum (nur Pflicht bei EU-Rechnungen)   |
| waehrung          | sollte EUR sein                                               |
| freitexte         | optional, verwendete Freitexte (s.u.)                         |
| positionen        | die Rechnungspositionen (s.u.)                                |

## Freitexte

Wie oben erläutert, muss der Inhalt von XML und PDF identisch sein.
Floskeln aus dem PDF müssen auch im XML auftauchen.
Falls die Texte in bestimmte Kategorien passen, sollte ein `subjectCode` angegeben werden.

**Mustang** implementiert zurzeit (Ende 2024, v2.15.0) nur die folgenden Codes:

| Code | Erläuterung                  |
| ---- | ---------------------------- |
| AAI  | Allgemeine Informationen     |
| SUR  | Anmerkungen des Verkäufers   |
| REG  | Regulatorische Informationen |
| ABL  | Rechtliche Informationen     |
| TXD  | Informationen zur Steuer     |
| CUS  | Zollinformationen            |
| ACY  | Introduction                 |
| AAK  | Text zu Entgeltminderungen   |

Die Beispieldatei zeigt:
- eine Zeile mit Text in Anführungsstrichen
- eine Zeile mit Text ohne Anführungsstriche
- einen Eintrag mit `text` und `subjectCode`
  - Der `subjectCode` stammt aus der Codeliste [UNTDID 4451](https://www.xrepository.de/details/urn:xoev-de:kosit:codeliste:untdid.4451_4). Zurzeit können nur die o.g. Codes verwendet werden.

## Rechnungspositionen

Es können ein oder mehrere Positionen angegeben werden.
Angaben pro Position:

| Schlüssel       | Wert                                                      |
| --------------- | --------------------------------------------------------- |
| produkt         | Pflichtangabe: Kennung aus `waren.yaml`                   |
| anzahl          | Pflichtangabe: Anzahl                                     |
| preis           | optional, überschreibt den Preis aus `waren.yaml`         |
| bestellnummer   | Pflichtangabe: Bestellnummer des Kunden                   |
| bestelldatum    | Pflichtangabe: Bestelldatum des Kunden                    |
| lieferdatum     | Pflichtangabe: Datum der Lieferung                        |
| ansprechpartner | optional: Name des Ansprechpartners bei dieser Bestellung |
| beschreibung    | optional: weitere Beschreibung des Artikels, die zusätzlich zum Feld `bezeichnung` in `waren.yaml` ausgegeben wird. |

Beispiel zur vorgesehenen Verwendung von `bezeichnung` und `beschreibung`:

 * `bezeichnung` (waren.yaml) - allgemeine Bezeichnung der Dienstleistung: Übersetzung Englisch -> Deutsch
 * `beschreibung` (rechnung.yaml) - konkretes Projekt bei der Bestellung: Wartungshandbuch Testfirma

# Hinweis zu Rundungsfehlern

Es gibt in Deutschland zwei Methoden zur Berechnung der Umsatzsteuer:
- man summiert alle Nettobeträge auf und wendet den Steuersatz auf die Summe an,
- oder man berechnet die Steuer pro Rechnungsposition, rundet diese und addiert die gerundeten Werte auf.

Je nach verwendeter Methode kann es bei der Berechnung der Umsatzsteuer zu Unterschieden von einigen Cent kommen (Rundungsfehler).

Mustang berechnet die Steuer intern mit 4 Nachkommastellen und addiert diese auf. Das Ergebnis entspricht in den meisten Fällen der ersten Variante.

Der ZUGFeRD-Validator kann ab Fassung 2.3 mit Rundungsfehlern [umgehen](https://www.awv-net.de/aktuelles/meldungen/neue-zugferd-version-2.3-veroeffentlicht.html) - allerdings nur im Profil `Extended`. (Dies ist ein Beispiel, wo sich das Ergebnis eines Validators in einer neuen Version von dem seines Vorläufers unterscheiden kann.)

Beachten Sie deshalb auf jeden Fall die Zusammenfassung der Zahlen, die das Program am Ende ausgibt, und stellen Sie sicher, dass die Zahlen mit denen übereinstimmen, die im "sichtbaren" PDF angezeigt werden.)

# Aufruf des Programms

Neben dem Quellcode steht das Programm steht als sog. "Fat-Jar" oder "Uber-Jar" zur Verfügung. Dieses Paket bringt alle Abhängigkeiten mit - insbesondere die MB-großen Tabellen zur Validierung. Es sollte ohne weitere Zusätze mit einem installierten Java (ab Version 17) laufen.

Das Java-Programm wird (falls es nicht umbenannt wurde) gestartet mit:

```
java -jar smer-X.X-all.jar name_der_pdf_datei.pdf [name_der_rechnungsdaten.yaml]
```
* `name_der_pdf_datei.pdf` ist der Name der PDF-Datei ohne XML
* `name_der_rechnungsdaten.yaml` (optional) ist der Name der YAML-Datei mit den Rechnungsdaten. Wird er nicht angegeben, wird `rechnung.yaml` verwendet.

Die Daten werden aus den Yaml-Dateien zusammengetragen, in ein XML verpackt und dieses in eine neue PDF-Datei geschrieben.

Diese neue PDF-Datei hat im Namen den Zusatz `_zugferd`. War der `name_der_datei.pdf` beispielsweise `rechnung123.pdf`, heißt die erstellte Datei, die dann das XML enthält, `rechnung123_zugferd.pdf`.

Nur wenn auch die abschließende Validierung dieser neuen PDF-Datei erfolgreich war, erscheint eine kurze Zusammenfassung der Zahlen, die mit dem sichtbaren Inhalt des PDFs verglichen werden müssen.

Der normale Befehl
```
./gradlew build
```
erzeugt das normale, wie auch das Fat-Jar. Letzters kann auch mit

```
./gradlew shadowJar
```
separat erstellt werden.

Es wird unter `./app/build/libs/smer-X.X-all.jar` gespeichert.

# Grundlegender Ablauf

* Zunächst überprüft das Programm, ob beim Aufruf eine PDF-Datei angegeben wurde, und ob diese existiert.
* Dann liest es die Daten aus den YAML-Dateien ein (`leseYamlDateien`).
* Mit diesen Daten wird die Struktur `Invoice` befüllt (`fuelleInvoice`).
* Dann wird mit `generateXML` von `MyZUGFeRD2PullProvider` der XML-Teil erzeugt.
  * Diese Methode speichert Einträge in `MyItem` und erhält mit der Funktion `getMyTransactionCalculator` Informationen zu den berechneten Summen.
* Falls im Quelltext ein Dateiname für `XML_DUMP` definiert wurde, wird die XML-Datei zum Debugging dort zusätzlich gespeichert.
* In `addXMLtoPDF` wird die beim Aufruf angegebene PDF-Datei mit dem XML verbunden und als ZUGFeRD-Datei ausgegeben.
* `validatePDF` führt eine abschließende Validierung dieser neuen PDF-Datei durch.
  * Nur wenn diese Validierung erfolgreich wird, wird eine kurze Zusammenfassung der Rechnungspositionen ausgegeben, deren Inhalt mit dem *sichtbaren* Teil des PDF verglichen werden muss.
  * Falls die Validierung fehlschlägt, wird die ZUGFeRD-Datei wieder gelöscht.

# Logging

Die Log4j-Konfigurationsdatei in `app/source/main/resources/log4j2.xml` erzeugt neben der Ausgabe der Meldungen auf dem Bildschirm noch eine weitere Protokolldatei `log.json` im aktuellen Verzeichnis.

# Erweiterungen

Mustang deckt nicht alle Fälle ab. So sind Bitten an dessen Autor, bestimmte Erweiterungen vorzunehmen, sehr häufig.

Das Programm verwendet diverse Optionen, die (bisher) in Mustang nicht vorgesehen sind.

Um Wartezeiten zu vermeiden, definiert das Programm deshalb Unterklassen der bestehenden Mustang-Klassen und erweitert diese entsprechend den eigenen Bedürfnissen.

## Erweiterung von Mustang-Klassen

### MyItem

Dies ist eine Erweiterung der Mustang-Klasse `Item`. Sie nimmt die Informationen pro Rechnungsposition auf, die ich zusätzliche ausgeben möchte:

 * `contactPerson`: den Ansprechpartner beim Kunden
 * `bestellDatum`: das Datum der Bestellung
 * `bestellnummer`: die Bestellnummer beim Kunden
 * `lieferDatum`: das Lieferdatum

Außerdem werden beim Generieren des XML der Gesamtbetrag der jeweiligen Position und dessen Umsatzsteuer berechnet. `MyItem` wird ebenfalls verwendet, um diese Werte zwischenzuspeichern und in der vom Programm erzeugten Kurzfassung der Rechnung zur Überprüfung wieder auszugeben. Künftige Erweiterungen dieses Programms könnten sie aber auch direkt zum Erstellen des Rechnungs-PDFs verwenden:

 * `lineID`: die Zeilennummer in der Rechnung
 * `itemNetto`: der Betrag ohne Umsatzsteuer für diese Position
 * `itemTax`: der Umsatzsteuerbetrag für diese Position

### MyTransactionCalculator

Die zugrunde liegende Klasse `TransactionCalculator` wird im `ZUGFeRD2PullProvider` gebraucht. Allerdings definiert sie ihre internen Methoden als `protected`. Sie sind daher in *meiner* abgeleiteten Klasse des PullProviders - `MyZUGFeRD2PullProvider` - nicht zugänglich.

Deshalb habe ich `MyTransactionCalculator` von der Klasse `TransactionCalculator` abgeleitet und die `protected` Methoden als `public` definiert. So kann ich sie in `MyZUGFeRD2PullProvider` aufrufen. Der Code selbst wurde nicht geändert.

### MyZUGFeRD2PullProvider

`ZUGFeRD2PullProvider` ist besonders wichtig, da dessen Methode `generateXML` dafür zuständig ist, den ZUGFeRD-XML-Code zu erzeugen.

Meine abgeleitete Klasse `MyZUGFeRD2PullProvider` überschreibt diese Methode, übernimmt weitestgehend den Code aus der ursprünglichen Klasse und fügt an bestimmten Stellen eigenen Code ein - hauptsächlich zur Ausgabe der Informationen, die in den anderen `MyXXXX`-Klassen zusätzlich erfasst wurden.

Ähnliches gilt für die Methode `getMyTradePartyAsXML`. Auch sie entspricht fast der ursprünglichen Mustang-Variante `getTradePartyAsXML`, mit der Ausnahme, dass die Ausgabe der VAT-ID unterdrückt werden kann - was für Rechnungen außerhalb der EU notwendig ist.

Bei einer neuen Version von Mustang, müssen diese Änderungen von Hand wieder einpflegen werden, bis sie in die 'offizielle' Version übernommen werden.

Die Änderungen sind aber leicht zu finden, da ich sie mit dem Kürzel `MS` markiert habe.

# Aussichten

Diese Programm zeigt, welche Daten mindestens notwendig sind, um eine elektronische Rechnung zu erstellen. Es kann somit als Ausgangspunkt für eigene Projekte dienen.

So wäre die Anbindung an eine "richtige" Datenbank oder die Erzeugung des PDF-Teils der Rechnung denkbar.

Für das gelegentliche "Aufwerten" einer selbst geschriebenen PDF-Rechnung, die im PDF/A-1b-Format gespeichert wurde, ist es aber ausreichend.

Es zeigt aber auch, wie viele ungelöste Probleme, Ungenauigkeiten und Widersprüche noch im gesamten System stecken - die wir mit der Papierrechnung alle nicht hatten: Vielleicht holt der Rechnungsempfänger nur den Gesamtbetrag aus dem XML, vielleicht zusätzlich auch die Rechnungspositionen - und steht die Bestellnummer tatsächlich an der Stelle, an der **sein** Auswerteprogramm es erwartet? All dies wird sich künftig erst zeigen müssen.

# Links
* https://www.mustangproject.org
* https://validator.invoice-portal.de/index.php