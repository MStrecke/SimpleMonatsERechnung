package simplemonatserechnung.mustang;

/*
 * This class makes some "protected" methods from the original
 * class "public" because they are needed in MyZUGFeRD2PullProvieder
 */
import java.lang.String;
import java.math.BigDecimal;
import java.util.HashMap;

import org.mustangproject.ZUGFeRD.IExportableTransaction;
import org.mustangproject.ZUGFeRD.TransactionCalculator;
import org.mustangproject.ZUGFeRD.VATAmount;

public class MyTransactionCalculator extends TransactionCalculator {
    /***
     *
     * @param trans the invoice (or IExportableTransaction) to be calculated
     */
    public MyTransactionCalculator(IExportableTransaction trans) {
        super(trans);
    }

    @Override
    public HashMap<BigDecimal, VATAmount> getVATPercentAmountMap() {
        return super.getVATPercentAmountMap();
    };

    @Override
    public BigDecimal getChargesForPercent(BigDecimal percent) {
        return super.getChargesForPercent(percent);
    };

    @Override
    public String getChargeReasonForPercent(BigDecimal percent) {
        return super.getChargeReasonForPercent(percent);
    }

    @Override
    public BigDecimal getAllowancesForPercent(BigDecimal percent) {
        return super.getAllowancesForPercent(percent);
    }

    @Override
    public String getAllowanceReasonForPercent(BigDecimal percent) {
        return super.getAllowanceReasonForPercent(percent);
    }

    @Override
    public BigDecimal getTaxBasis() {
        return super.getTaxBasis();
    }

    @Override
    public BigDecimal getTotalPrepaid() {
        return super.getTotalPrepaid();
    }

    @Override
    public BigDecimal getTotal() {
        return super.getTotal();
    }
}
