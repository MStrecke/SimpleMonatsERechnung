package simplemonatserechnung.mustang;

import org.mustangproject.Item;
import org.mustangproject.Product;

import java.math.BigDecimal;
import java.util.Date;

public class MyItem extends Item {
    protected String contactPerson = null;
    protected Date bestellDatum = null;
    protected Date lieferDatum = null;
    protected String bestellnummer = null;
    protected BigDecimal itemNetto;
    protected BigDecimal itemTax;
    protected Integer lineID;

    public BigDecimal getItemNetto() {
        return itemNetto;
    }

    public void setItemNetto(BigDecimal itemNetto) {
        this.itemNetto = itemNetto;
    }

    public BigDecimal getItemTax() {
        return itemTax;
    }

    public void setItemTax(BigDecimal itemTax) {
        this.itemTax = itemTax;
    }

    public MyItem(Product product, BigDecimal price, BigDecimal quantity) {
        super(product, price, quantity);
    }

    public String getContactPerson() {
        return contactPerson;
    }
    public MyItem setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
        return this;
    }
    public Date getBestellDatum() {
        return bestellDatum;
    }
    public MyItem setBestellDatum(Date bestellDatum) {
        this.bestellDatum = bestellDatum;
        return this;
    }
    public Date getLieferDatum() {
        return lieferDatum;
    }
    public MyItem setLieferDatum(Date lieferDatum) {
        this.lieferDatum = lieferDatum;
        return this;
    }
    public String getBestellnummer() {
        return bestellnummer;
    }
    public MyItem setBestellnummer(String bestellnummer) {
        this.bestellnummer = bestellnummer;
        return this;
    }

    public Integer getLineID() {
        return lineID;
    }

    public void setLineID(Integer lineID) {
        this.lineID = lineID;
    }


}
