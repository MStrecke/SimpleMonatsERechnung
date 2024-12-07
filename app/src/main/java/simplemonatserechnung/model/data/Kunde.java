package simplemonatserechnung.model.data;

import org.mustangproject.TradeParty;

public class Kunde extends Kennung {
    // private String kennung;
    private String name; //
    private String adresse1; // LineOne (meist Straße)
    private String adresse2; // LineTwo
    private String adresse3; // LineThree
    private String postleitzahl;
    private String ort;
    private String land; // CountryCode

    private String steuernummer;
    private String umsatzsteueridentifikationsnummer;
    private String steuerfall;
    private String zahlungsbedingungen;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdresse1() {
        return adresse1;
    }

    public void setAdresse1(String adresse1) {
        this.adresse1 = adresse1;
    }

    public String getAdresse2() {
        return adresse2;
    }

    public void setAdresse2(String adresse2) {
        this.adresse2 = adresse2;
    }

    public String getPostleitzahl() {
        return postleitzahl;
    }

    public void setPostleitzahl(String postleitzahl) {
        this.postleitzahl = postleitzahl;
    }

    public String getOrt() {
        return ort;
    }

    public void setOrt(String ort) {
        this.ort = ort;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public String getSteuernummer() {
        return steuernummer;
    }

    public void setSteuernummer(String steuernummer) {
        this.steuernummer = steuernummer;
    }

    public String getUmsatzsteueridentifikationsnummer() {
        return umsatzsteueridentifikationsnummer;
    }

    public void setUmsatzsteueridentifikationsnummer(String umsatzidentifikationsnummer) {
        this.umsatzsteueridentifikationsnummer = umsatzidentifikationsnummer;
    }

    public String getSteuerfall() {
        return steuerfall;
    }

    public void setSteuerfall(String steuerfall) {
        this.steuerfall = steuerfall;
    }

    public String getZahlungsbedingungen() {
        return zahlungsbedingungen;
    }

    public void setZahlungsbedingungen(String zahlungsbedingungen) {
        this.zahlungsbedingungen = zahlungsbedingungen;
    }

    /**
     * Rückgabe der für TradeParty relevanten Daten
     *
     * Wertet auch die Zeilen `adresse2` und `adresse3` aus und fügt
     * sie der TradeParty hinzu.
     *
     * @return TradeParty
     */
    public TradeParty getTradePartyAddress() {
        TradeParty tp = new TradeParty(
                this.name,
                this.adresse1,
                this.postleitzahl,
                this.ort,
                this.land);

        if (this.adresse2 != null) {
            tp.setAdditionalAddress(this.adresse2);
        }
        ;
        if (this.adresse3 != null) {
            tp.setAdditionalAddressExtension(this.adresse3);
        }
        return tp;
    }
}
