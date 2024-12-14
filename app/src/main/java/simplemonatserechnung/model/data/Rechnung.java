package simplemonatserechnung.model.data;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonFormat;

public class Rechnung {
    private String kunde;
    private String rechnungsnummer;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    private Date rechnungsdatum;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    private Date periode_start;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    private Date periode_ende;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    private Date faelligkeitsdatum;

    public Date getFaelligkeitsdatum() {
        return faelligkeitsdatum;
    }

    public void setFaelligkeitsdatum(Date faelligkeitsdatum) {
        this.faelligkeitsdatum = faelligkeitsdatum;
    }

    private String waehrung;
    private List<Freitext> freitexte = new ArrayList<Freitext>();
    private List<RechnungsPosition> positionen;

    public String getKunde() {
        return kunde;
    }

    public void setKunde(String kunde) {
        this.kunde = kunde;
    }

    public String getRechnungsnummer() {
        return rechnungsnummer;
    }

    public void setRechnungsnummer(String rechnungsnummer) {
        this.rechnungsnummer = rechnungsnummer;
    }

    public Date getRechnungsdatum() {
        return rechnungsdatum;
    }

    public void setRechnungsdatum(Date rechnungsdatum) {
        this.rechnungsdatum = rechnungsdatum;
    }

    public String getWaehrung() {
        return waehrung;
    }

    public void setWaehrung(String waehrung) {
        this.waehrung = waehrung;
    }

    public List<Freitext> getFreitexte() {
        return freitexte;
    }

    public void setFreitexte(List<Freitext> freitexte) {
        this.freitexte = freitexte;
    }

    public List<RechnungsPosition> getPositionen() {
        return positionen;
    }

    public void setPositionen(List<RechnungsPosition> positionen) {
        this.positionen = positionen;
    }

    public Date getPeriode_start() {
        return periode_start;
    }

    public void setPeriode_start(Date periode_start) {
        this.periode_start = periode_start;
    }

    public Date getPeriode_ende() {
        return periode_ende;
    }

    public void setPeriode_ende(Date periode_ende) {
        this.periode_ende = periode_ende;
    }
}
