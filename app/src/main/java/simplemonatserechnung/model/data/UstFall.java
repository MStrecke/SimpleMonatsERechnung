package simplemonatserechnung.model.data;

import java.math.BigDecimal;

public class UstFall extends Kennung {
    private BigDecimal rate;          // Prozent
    private String code;              // TaxCategory (S, Z, G, K)
    private String excemptionreason;  // "Export outside the EU"

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public String getCode() {
        return code;
    }

    public String getExcemptionreason() {
        return excemptionreason;
    }

    public void setExcemptionreason(String excemptioncode) {
        this.excemptionreason = excemptioncode;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
