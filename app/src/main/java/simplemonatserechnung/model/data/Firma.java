package simplemonatserechnung.model.data;

import org.mustangproject.TradeParty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Firma {
    private String name; // Name der Firma
    private String adresse1; // Adresse, Zeile 1, normalerweise: Straße
    private String adresse2; // optional, Adresse Zeile 2
    private String adresse3; // optional, Adresse Zeile 3
    private String postleitzahl; // Postleitzahl
    private String ort; // Ort
    private String land; // Land (EN ISO 3166-1, 2er) DE, BE, FR, GB, US
    private String steuernummer; // nationale Steuernummer
    private String umsatzsteueridentifikationsnummer; // internationale Steuernumer
    private String iban; // IBAN der Kontoverbindung
    private String bic; // BIC der Kontoverbindung

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdresse1() {
        return adresse1;
    }

    public void setLand(String land) {
        this.land = land;
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

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

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
