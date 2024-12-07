package simplemonatserechnung.model.data;

import java.math.BigDecimal;

public class Ware extends Kennung {
    private String bezeichnung;     // Sattel, Übersetzung
    private String einheit;         // UnitOfMeasurement
    private BigDecimal preis;

    public String getBezeichnung() {
        return bezeichnung;
    }
    public void setBezeichnung(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }
    public String getEinheit() {
        return einheit;
    }
    public void setEinheit(String einheit) {
        this.einheit = einheit;
    }
    public BigDecimal getPreis() {
        return preis;
    }
    public void setPreis(BigDecimal preis) {
        this.preis = preis;
    }
}