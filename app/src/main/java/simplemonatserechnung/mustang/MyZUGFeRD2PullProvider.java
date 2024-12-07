package simplemonatserechnung.mustang;

/*
 * This is a nearly verbatim copy of ZUGFeRD2PullProvider
 *
 * It adds the (casting) and handling of MyItem which is an
 * extended version of Item.
 *
 * Access to the protected methods of TransactionCalculator is
 * done via the intermediate class MyTransactionCalculator
 */

import static org.mustangproject.ZUGFeRD.ZUGFeRDDateFormat.DATE;
import static org.mustangproject.ZUGFeRD.model.TaxCategoryCodeTypeConstants.CATEGORY_CODES_WITH_EXEMPTION_REASON;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.mustangproject.FileAttachment;
import org.mustangproject.IncludedNote;
import org.mustangproject.Product;
import org.mustangproject.XMLTools;
import org.mustangproject.ZUGFeRD.IDesignatedProductClassification;
import org.mustangproject.ZUGFeRD.IExportableTransaction;
import org.mustangproject.ZUGFeRD.IReferencedDocument;
import org.mustangproject.ZUGFeRD.IZUGFeRDAllowanceCharge;
import org.mustangproject.ZUGFeRD.IZUGFeRDCashDiscount;
import org.mustangproject.ZUGFeRD.IZUGFeRDExportableItem;
import org.mustangproject.ZUGFeRD.IZUGFeRDExportableTradeParty;
import org.mustangproject.ZUGFeRD.IZUGFeRDPaymentDiscountTerms;
import org.mustangproject.ZUGFeRD.IZUGFeRDPaymentTerms;
import org.mustangproject.ZUGFeRD.IZUGFeRDTradeSettlement;
import org.mustangproject.ZUGFeRD.IZUGFeRDTradeSettlementDebit;
import org.mustangproject.ZUGFeRD.IZUGFeRDTradeSettlementPayment;
import org.mustangproject.ZUGFeRD.LineCalculator;
import org.mustangproject.ZUGFeRD.Profile;
import org.mustangproject.ZUGFeRD.Profiles;
import org.mustangproject.ZUGFeRD.VATAmount;
import org.mustangproject.ZUGFeRD.ZUGFeRD2PullProvider;
import org.mustangproject.ZUGFeRD.model.DocumentCodeTypeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyZUGFeRD2PullProvider extends ZUGFeRD2PullProvider {
	/* MS: copied from super, because it's private there */
	private String paymentTermsDescription;
	/* MS: more "public" TransactionCalculator */
	protected MyTransactionCalculator calc;

	/* MS */
	public MyTransactionCalculator getMyTransactionCalculator() {
		return calc;
	}

	/*
	 * MS
	 * This is a verbatim copy of `getTradePartyAsXML`, except:
	 * - it skips the output of the VAT-ID if taxCaseO is used
	 */
	protected String getMyTradePartyAsXML(IZUGFeRDExportableTradeParty party, boolean isSender, boolean isShipToTradeParty, boolean needsVATID) {
		String xml = "";
		// According EN16931 either GlobalID or seller assigned ID might be present for BuyerTradeParty
		// and ShipToTradeParty, but not both. Prefer seller assigned ID for now.
		if (party.getID() != null) {
			xml += "<ram:ID>" + XMLTools.encodeXML(party.getID()) + "</ram:ID>";
		}
		if ((party.getGlobalIDScheme() != null) && (party.getGlobalID() != null)) {
			xml += "<ram:GlobalID schemeID=\"" + XMLTools.encodeXML(party.getGlobalIDScheme()) + "\">"
				+ XMLTools.encodeXML(party.getGlobalID()) + "</ram:GlobalID>";
		}
		xml += "<ram:Name>" + XMLTools.encodeXML(party.getName()) + "</ram:Name>";
		if (party.getDescription() != null) {
			xml += "<ram:Description>" + XMLTools.encodeXML(party.getDescription()) + "</ram:Description>";
		}
		if (party.getLegalOrganisation() != null) {
			xml += "<ram:SpecifiedLegalOrganization> ";
			if (party.getLegalOrganisation().getSchemedID() != null) {
				if (profile == Profiles.getByName("Minimum")) {
					xml += "<ram:ID>" + XMLTools.encodeXML(party.getLegalOrganisation().getSchemedID().getID()) + "</ram:ID>";
				} else {
					xml += "<ram:ID schemeID=\"" + XMLTools.encodeXML(party.getLegalOrganisation().getSchemedID().getScheme()) + "\">" + XMLTools.encodeXML(party.getLegalOrganisation().getSchemedID().getID()) + "</ram:ID>";
				}
			}
			if (party.getLegalOrganisation().getTradingBusinessName() != null) {
				xml += "<ram:TradingBusinessName>" + XMLTools.encodeXML(party.getLegalOrganisation().getTradingBusinessName()) + "</ram:TradingBusinessName>";
			}
			xml += "</ram:SpecifiedLegalOrganization>";
		}

		if ((party.getContact() != null) && (isSender || profile == Profiles.getByName("EN16931") || profile == Profiles.getByName("Extended") || profile == Profiles.getByName("XRechnung"))) {
			xml += "<ram:DefinedTradeContact>";
			if (party.getContact().getName() != null) {
				xml += "<ram:PersonName>"
					+ XMLTools.encodeXML(party.getContact().getName())
					+ "</ram:PersonName>";
			}
			if (party.getContact().getPhone() != null) {
				xml += "<ram:TelephoneUniversalCommunication><ram:CompleteNumber>"
					+ XMLTools.encodeXML(party.getContact().getPhone()) + "</ram:CompleteNumber>"
					+ "</ram:TelephoneUniversalCommunication>";
			}

			if ((party.getContact().getFax() != null) && (profile == Profiles.getByName("Extended"))) {
				xml += "<ram:FaxUniversalCommunication><ram:CompleteNumber>"
					+ XMLTools.encodeXML(party.getContact().getFax()) + "</ram:CompleteNumber>"
					+ "</ram:FaxUniversalCommunication>";
			}
			if (party.getContact().getEMail() != null) {
				xml += "<ram:EmailURIUniversalCommunication><ram:URIID>"
					+ XMLTools.encodeXML(party.getContact().getEMail()) + "</ram:URIID>"
					+ "</ram:EmailURIUniversalCommunication>";
			}
			xml += "</ram:DefinedTradeContact>";
		}

		xml += "<ram:PostalTradeAddress>";
		if (party.getZIP() != null) {
			xml += "<ram:PostcodeCode>" + XMLTools.encodeXML(party.getZIP())
				+ "</ram:PostcodeCode>";
		}
		if (party.getStreet() != null) {
			xml += "<ram:LineOne>" + XMLTools.encodeXML(party.getStreet())
				+ "</ram:LineOne>";
		}
		if (party.getAdditionalAddress() != null) {
			xml += "<ram:LineTwo>" + XMLTools.encodeXML(party.getAdditionalAddress())
				+ "</ram:LineTwo>";
		}
		if (party.getAdditionalAddressExtension() != null) {
			xml += "<ram:LineThree>" + XMLTools.encodeXML(party.getAdditionalAddressExtension())
				+ "</ram:LineThree>";
		}
		if (party.getLocation() != null) {
			xml += "<ram:CityName>" + XMLTools.encodeXML(party.getLocation())
				+ "</ram:CityName>";
		}

		//country IS mandatory
		xml += "<ram:CountryID>" + XMLTools.encodeXML(party.getCountry())
			+ "</ram:CountryID>"
			+ "</ram:PostalTradeAddress>";
		if (party.getUriUniversalCommunicationID() != null && party.getUriUniversalCommunicationIDScheme() != null) {
			xml += "<ram:URIUniversalCommunication>" +
				"<ram:URIID schemeID=\"" + party.getUriUniversalCommunicationIDScheme() + "\">" +
				XMLTools.encodeXML(party.getUriUniversalCommunicationID())
				+ "</ram:URIID></ram:URIUniversalCommunication>";
		}

		if ((party.getVATID() != null) && (!isShipToTradeParty) && needsVATID) {
			xml += "<ram:SpecifiedTaxRegistration>"
				+ "<ram:ID schemeID=\"VA\">" + XMLTools.encodeXML(party.getVATID())
				+ "</ram:ID>"
				+ "</ram:SpecifiedTaxRegistration>";
		}
		if ((party.getTaxID() != null) && (!isShipToTradeParty) && needsVATID) {
			xml += "<ram:SpecifiedTaxRegistration>"
				+ "<ram:ID schemeID=\"FC\">" + XMLTools.encodeXML(party.getTaxID())
				+ "</ram:ID>"
				+ "</ram:SpecifiedTaxRegistration>";

		}
		return xml;

	}


	/* MS
	 * This is a verbatim copy of ZUGFeRD2PullProvider 2.15.0 with own additions
	 */
	@Override
	public void generateXML(IExportableTransaction trans) {
		this.trans = trans;
		/* MS - MyTransactionCalculator */
		this.calc = new MyTransactionCalculator(trans);
		/* MS - we need this info often */
		Boolean isProfileExtended = (getProfile() == Profiles.getByName("Extended"));
		/* MS
		 * If taxCode "O" (Not subject to VAT) some fields must be skipped
		 * - the tax number in both "sender" and "recipient".
		 * This flag will be determined when iterating over the invoice items.
		 * Fortunately the addresses of sender and recipient will be printed
		 * AFTER that.
		*/
		Boolean taxCodeOwasUsed = false;

		boolean hasDueDate = trans.getDueDate() != null;
		final SimpleDateFormat germanDateFormat = new SimpleDateFormat("dd.MM.yyyy");

		String exemptionReason = "";

		if (trans.getPaymentTermDescription() != null) {
			paymentTermsDescription = XMLTools.encodeXML(trans.getPaymentTermDescription());
		}


		if ((profile == Profiles.getByName("XRechnung")) && (trans.getCashDiscounts() != null) && (trans.getCashDiscounts().length > 0)) {
			for (IZUGFeRDCashDiscount discount : trans.getCashDiscounts()
			) {
				if (paymentTermsDescription == null) {
					paymentTermsDescription = "";
				}
				paymentTermsDescription += discount.getAsXRechnung();
			}
		} else if ((paymentTermsDescription == null) && (trans.getDocumentCode() != DocumentCodeTypeConstants.CORRECTEDINVOICE) && (trans.getDocumentCode() != DocumentCodeTypeConstants.CREDITNOTE)) {
			if (trans.getDueDate() != null) {
				paymentTermsDescription = "Please remit until " + germanDateFormat.format(trans.getDueDate());
			}
		}


		String typecode = "380";
		if (trans.getDocumentCode() != null) {
			typecode = trans.getDocumentCode();
		}

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<rsm:CrossIndustryInvoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:rsm=\"urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100\""
			// + "
			// xsi:schemaLocation=\"urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100
			// ../Schema/ZUGFeRD1p0.xsd\""
			+ " xmlns:ram=\"urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100\""
			+ " xmlns:udt=\"urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100\""
			+ " xmlns:qdt=\"urn:un:unece:uncefact:data:standard:QualifiedDataType:100\">"
				/*
				 * MS
				 * Modify "generated by:" to indicate that this is NOT the *official*
				 * PullProvider
				 * "My"ZUGFeRD2PullProvider + ".ms"
				 */
			+ "<!-- generated by: mustangproject.org v" + MyZUGFeRD2PullProvider.class.getPackage().getImplementationVersion() + ".ms -->"
			+ "<rsm:ExchangedDocumentContext>\n";
		// + "
		// <ram:TestIndicator><udt:Indicator>"+testBooleanStr+"</udt:Indicator></ram:TestIndicator>"
		//

		if (getProfile() == Profiles.getByName("XRechnung")) {
			xml += "<ram:BusinessProcessSpecifiedDocumentContextParameter>\n"
				+ "<ram:ID>urn:fdc:peppol.eu:2017:poacc:billing:01:1.0</ram:ID>\n"
				+ "</ram:BusinessProcessSpecifiedDocumentContextParameter>\n";
		}
		xml +=
			"<ram:GuidelineSpecifiedDocumentContextParameter>"
				+ "<ram:ID>" + getProfile().getID() + "</ram:ID>"
				+ "</ram:GuidelineSpecifiedDocumentContextParameter>"
				+ "</rsm:ExchangedDocumentContext>"
				+ "<rsm:ExchangedDocument>"
				+ "<ram:ID>" + XMLTools.encodeXML(trans.getNumber()) + "</ram:ID>"
				// + "<ram:Name>RECHNUNG</ram:Name>"
				// + "<ram:TypeCode>380</ram:TypeCode>"
				+ "<ram:TypeCode>" + typecode + "</ram:TypeCode>"
				+ "<ram:IssueDateTime>"
				+ DATE.udtFormat(trans.getIssueDate()) + "</ram:IssueDateTime>" // date
				+ buildNotes(trans)

				+ "</rsm:ExchangedDocument>"
				+ "<rsm:SupplyChainTradeTransaction>";
		int lineID = 0;
		for (final IZUGFeRDExportableItem currentItem : trans.getZFItems()) {
			lineID++;

			MyItem myCurrentItem = (MyItem) currentItem;

			if (currentItem.getProduct().getTaxExemptionReason() != null) {
				exemptionReason = "<ram:ExemptionReason>" + XMLTools.encodeXML(currentItem.getProduct().getTaxExemptionReason()) + "</ram:ExemptionReason>";
			}
			final LineCalculator lc = new LineCalculator(currentItem);
			if ((getProfile() != Profiles.getByName("Minimum")) && (getProfile() != Profiles.getByName("BasicWL"))) {
				xml += "<ram:IncludedSupplyChainTradeLineItem>" +
					"<ram:AssociatedDocumentLineDocument>"
					+ "<ram:LineID>" + lineID + "</ram:LineID>"
					+ buildItemNotes(currentItem)
					+ "</ram:AssociatedDocumentLineDocument>"

					+ "<ram:SpecifiedTradeProduct>";
				if ((currentItem.getProduct().getGlobalIDScheme() != null) && (currentItem.getProduct().getGlobalID() != null)) {
					xml += "<ram:GlobalID schemeID=\"" + XMLTools.encodeXML(currentItem.getProduct().getGlobalIDScheme()) + "\">" + XMLTools.encodeXML(currentItem.getProduct().getGlobalID()) + "</ram:GlobalID>";
				}

				if (currentItem.getProduct().getSellerAssignedID() != null) {
					xml += "<ram:SellerAssignedID>"
						+ XMLTools.encodeXML(currentItem.getProduct().getSellerAssignedID()) + "</ram:SellerAssignedID>";
				}
				if (currentItem.getProduct().getBuyerAssignedID() != null) {
					xml += "<ram:BuyerAssignedID>"
						+ XMLTools.encodeXML(currentItem.getProduct().getBuyerAssignedID()) + "</ram:BuyerAssignedID>";
				}
				String allowanceChargeStr = "";
				if (currentItem.getItemAllowances() != null && currentItem.getItemAllowances().length > 0) {
					for (final IZUGFeRDAllowanceCharge allowance : currentItem.getItemAllowances()) {
						allowanceChargeStr += getAllowanceChargeStr(allowance, currentItem);
					}
				}
				if (currentItem.getItemCharges() != null && currentItem.getItemCharges().length > 0) {
					for (final IZUGFeRDAllowanceCharge charge : currentItem.getItemCharges()) {
						allowanceChargeStr += getAllowanceChargeStr(charge, currentItem);

					}
				}

				String itemTotalAllowanceChargeStr = "";
				if (currentItem.getItemTotalAllowances() != null && currentItem.getItemTotalAllowances().length > 0) {
					for (final IZUGFeRDAllowanceCharge itemTotalAllowance : currentItem.getItemTotalAllowances()) {
						itemTotalAllowanceChargeStr += getItemTotalAllowanceChargeStr(itemTotalAllowance, currentItem);
					}
				}

				/* MS
				 * Store current values for later output
				 */
				myCurrentItem.setLineID(lineID);
				myCurrentItem.setItemNetto(lc.getItemTotalNetAmount());
				myCurrentItem.setItemTax(lc.getItemTotalVATAmount());

				xml += "<ram:Name>" + XMLTools.encodeXML(currentItem.getProduct().getName()) + "</ram:Name>";
				if (currentItem.getProduct().getDescription().length() > 0) {
					xml += "<ram:Description>" +
						XMLTools.encodeXML(currentItem.getProduct().getDescription()) +
						"</ram:Description>";
				}
				if (currentItem.getProduct().getClassifications() != null && currentItem.getProduct().getClassifications().length > 0) {
					for (IDesignatedProductClassification classification : currentItem.getProduct().getClassifications()) {
						xml += "<ram:DesignatedProductClassification>"
							+ "<ram:ClassCode listId=\"" + XMLTools.encodeXML(classification.getClassCode().getListID()) + "\"";
						if (classification.getClassCode().getListVersionID() != null) {
							xml += " listVersionID=\"" + XMLTools.encodeXML(classification.getClassCode().getListVersionID()) + "\"";
						}
						xml += ">" + classification.getClassCode().getCode() + "</ram:ClassCode>";
						if (classification.getClassName() != null) {
							xml += "<ram:ClassName>" + XMLTools.encodeXML(classification.getClassName()) + "</ram:ClassName>";
						}
						xml += "</ram:DesignatedProductClassification>";
					}
				}
				if (currentItem.getProduct().getAttributes() != null) {
					for (Entry<String, String> entry : currentItem.getProduct().getAttributes().entrySet()) {
						xml += "<ram:ApplicableProductCharacteristic>" +
							"<ram:Description>" + XMLTools.encodeXML(entry.getKey()) + "</ram:Description>" +
							"<ram:Value>" + XMLTools.encodeXML(entry.getValue()) + "</ram:Value>" +
							"</ram:ApplicableProductCharacteristic>";
					}
				}
				if (currentItem.getProduct().getCountryOfOrigin() != null) {
					xml += "<ram:OriginTradeCountry><ram:ID>" +
						XMLTools.encodeXML(currentItem.getProduct().getCountryOfOrigin()) +
						"</ram:ID></ram:OriginTradeCountry>";
				}
				xml += "</ram:SpecifiedTradeProduct>"
					+ "<ram:SpecifiedLineTradeAgreement>";
				if (currentItem.getReferencedDocuments() != null) {
					for (final IReferencedDocument currentReferencedDocument : currentItem.getReferencedDocuments()) {
						xml += "<ram:AdditionalReferencedDocument>" +
							"<ram:IssuerAssignedID>" + XMLTools.encodeXML(currentReferencedDocument.getIssuerAssignedID()) + "</ram:IssuerAssignedID>" +
							"<ram:TypeCode>" + XMLTools.encodeXML(currentReferencedDocument.getTypeCode()) + "</ram:TypeCode>" +
							"<ram:ReferenceTypeCode>" + XMLTools.encodeXML(currentReferencedDocument.getReferenceTypeCode()) + "</ram:ReferenceTypeCode>" +
							"</ram:AdditionalReferencedDocument>";
					}
				}

				/* MS - Replaces original source code block
				 *
				 * The section `BuyerOrderReferencedDocument` is only shown, if
				 * - `getBuyerOrderReferencedDocumentLineID` (all profiles) (original source)
				 * - OR Bestellnummer and/or Bestelldatum is set (only in profile Extended)
				 */
				if (
					(myCurrentItem.getBuyerOrderReferencedDocumentLineID() != null)
					||
					( isProfileExtended &&
						(
							(myCurrentItem.getBestellDatum() != null) ||
							(myCurrentItem.getBestellnummer() != null)
						)
					)
				) {
					xml += "<ram:BuyerOrderReferencedDocument> ";
					if (myCurrentItem.getBestellnummer() != null) {
						xml += "<ram:IssuerAssignedID>" + XMLTools.encodeXML(myCurrentItem.getBestellnummer()) + "</ram:IssuerAssignedID>";
					}
					if (currentItem.getBuyerOrderReferencedDocumentLineID() != null) {
						xml += "<ram:LineID>" + XMLTools.encodeXML(myCurrentItem.getBuyerOrderReferencedDocumentLineID()) + "</ram:LineID>";
					}
					if (myCurrentItem.getBestellDatum() != null) {
						xml += "<ram:FormattedIssueDateTime>" + DATE.qdtFormat(myCurrentItem.getBestellDatum()) + "</ram:FormattedIssueDateTime>";
					}
					xml += "</ram:BuyerOrderReferencedDocument> ";
				}

				if (!allowanceChargeStr.isEmpty()) {
					xml += "<ram:GrossPriceProductTradePrice>"
						+ "<ram:ChargeAmount>" + priceFormat(lc.getPriceGross())
						+ "</ram:ChargeAmount>" //currencyID=\"EUR\"
						+ "<ram:BasisQuantity unitCode=\"" + XMLTools.encodeXML(currentItem.getProduct().getUnit())
						+ "\">" + quantityFormat(currentItem.getBasisQuantity()) + "</ram:BasisQuantity>"
						+ allowanceChargeStr
						// + "<AppliedTradeAllowanceCharge>"
						// + "<ChargeIndicator>false</ChargeIndicator>"
						// + "<ActualAmount currencyID=\"EUR\">0.6667</ActualAmount>"
						// + "<Reason>Rabatt</Reason>"
						// + "</AppliedTradeAllowanceCharge>"
						+ "</ram:GrossPriceProductTradePrice>";
				}

				xml += "<ram:NetPriceProductTradePrice>"
					+ "<ram:ChargeAmount>" + priceFormat(lc.getPrice())
					+ "</ram:ChargeAmount>" // currencyID=\"EUR\"
					+ "<ram:BasisQuantity unitCode=\"" + XMLTools.encodeXML(currentItem.getProduct().getUnit())
					+ "\">" + quantityFormat(currentItem.getBasisQuantity()) + "</ram:BasisQuantity>"
					+ "</ram:NetPriceProductTradePrice>"
					+ "</ram:SpecifiedLineTradeAgreement>"
					+ "<ram:SpecifiedLineTradeDelivery>"
					+ "<ram:BilledQuantity unitCode=\"" + XMLTools.encodeXML(currentItem.getProduct().getUnit()) + "\">"
					+ quantityFormat(currentItem.getQuantity()) + "</ram:BilledQuantity>";

				/* MS
				 * Addition to original code
				 * must be directly after ram:BilledQuantity
				 *
				 * The following entries are only available in the profile Extended
				 */

                if (isProfileExtended) {
					/* MS
					 * not using `getTradePartyAsXML` here, because it will output
					 * `ram:Name`, `ram:PostalTradeAddress` and `ram:CountryID`
					 * which don't seem necessary
					 */
                    String contactperson = myCurrentItem.getContactPerson();

                    if (contactperson != null) {
                        xml += "<ram:ShipToTradeParty>"
                                + "<ram:DefinedTradeContact>"
                                + "<ram:PersonName>"
                                + XMLTools.encodeXML(contactperson)
                                + "</ram:PersonName>"
                                + "</ram:DefinedTradeContact>"
                                + "</ram:ShipToTradeParty>";
                    }

					/* MS
					 * output delivery date for this item
					 * (only available in Extended)
					 */
                    if (myCurrentItem.getLieferDatum() != null) {
                        xml += "<ram:ActualDeliverySupplyChainEvent>"
                                + "<ram:OccurrenceDateTime>"
								   + DATE.udtFormat(myCurrentItem.getLieferDatum())
                                + "</ram:OccurrenceDateTime>"
                                + "</ram:ActualDeliverySupplyChainEvent>";
                    }
                }

				xml += "</ram:SpecifiedLineTradeDelivery>"
					+ "<ram:SpecifiedLineTradeSettlement>"
					+ "<ram:ApplicableTradeTax>"
					+ "<ram:TypeCode>VAT</ram:TypeCode>"
					+ exemptionReason
					+ "<ram:CategoryCode>" + currentItem.getProduct().getTaxCategoryCode() + "</ram:CategoryCode>";

                /* MS
				 * skip `RateApplicablePercent` if tax category is "O"
                 * change of original source
				 * must be behind CategoryCode
                 * due to FX-SCH-A-000248: [BR-O-05]-An Invoice line (BG-25) where the VAT category code (BT-151) is "Not subject to VAT" shall not contain an Invoiced item VAT rate (BT-152).
				 */
                if (! currentItem.getProduct().getTaxCategoryCode().equals("O")) {
                    xml += "<ram:RateApplicablePercent>" +
                     vatFormat(currentItem.getProduct().getVATPercent()) +
                     "</ram:RateApplicablePercent>";
                } else {
					taxCodeOwasUsed = true;
				};
				xml += "</ram:ApplicableTradeTax>";

				if ((currentItem.getDetailedDeliveryPeriodFrom() != null) || (currentItem.getDetailedDeliveryPeriodTo() != null)) {
					xml += "<ram:BillingSpecifiedPeriod>";
					if (currentItem.getDetailedDeliveryPeriodFrom() != null) {
						xml += "<ram:StartDateTime>" + DATE.udtFormat(currentItem.getDetailedDeliveryPeriodFrom()) + "</ram:StartDateTime>";
					}
					if (currentItem.getDetailedDeliveryPeriodTo() != null) {
						xml += "<ram:EndDateTime>" + DATE.udtFormat(currentItem.getDetailedDeliveryPeriodTo()) + "</ram:EndDateTime>";
					}
					xml += "</ram:BillingSpecifiedPeriod>";
				}

				xml += itemTotalAllowanceChargeStr;

				xml += "<ram:SpecifiedTradeSettlementLineMonetarySummation>"
					+ "<ram:LineTotalAmount>" + currencyFormat(lc.getItemTotalNetAmount())
					+ "</ram:LineTotalAmount>" // currencyID=\"EUR\"
					+ "</ram:SpecifiedTradeSettlementLineMonetarySummation>";
				if (currentItem.getAdditionalReferences() != null) {
					for (final IReferencedDocument currentReference : currentItem.getAdditionalReferences()) {
						xml += "<ram:AdditionalReferencedDocument>" +
							"<ram:IssuerAssignedID>" + XMLTools.encodeXML(currentReference.getIssuerAssignedID()) + "</ram:IssuerAssignedID>" +
							"<ram:TypeCode>130</ram:TypeCode>" +
							"<ram:ReferenceTypeCode>" + XMLTools.encodeXML(currentReference.getReferenceTypeCode()) + "</ram:ReferenceTypeCode>" +
							"</ram:AdditionalReferencedDocument>";
					}
				} else if (currentItem.getAdditionalReferencedDocumentID() != null) {
					xml += "<ram:AdditionalReferencedDocument><ram:IssuerAssignedID>" + currentItem.getAdditionalReferencedDocumentID() + "</ram:IssuerAssignedID><ram:TypeCode>130</ram:TypeCode></ram:AdditionalReferencedDocument>";
				}
				xml += "</ram:SpecifiedLineTradeSettlement>"
					+ "</ram:IncludedSupplyChainTradeLineItem>";
			}

		}

		xml += "<ram:ApplicableHeaderTradeAgreement>";
		if (trans.getReferenceNumber() != null) {
			xml += "<ram:BuyerReference>" + XMLTools.encodeXML(trans.getReferenceNumber()) + "</ram:BuyerReference>";

		}
		/* MS
		 *
		 * Depending on the validator a VATID has to be present
		 * in case of taxCode "O" (Not subject to VAT) or not.
		 * The built-in validator thinks it has to be there, but
		 * tat may change in the future.
		*/
		xml += "<ram:SellerTradeParty>"
			+ getMyTradePartyAsXML(trans.getSender(), true, false, true)
			+ "</ram:SellerTradeParty>"
			+ "<ram:BuyerTradeParty>";
		// + "<ID>GE2020211</ID>"
		// + "<GlobalID schemeID=\"0088\">4000001987658</GlobalID>"

		/* MS - getMyTradePartyAsXML: taxCodeOwasUsed */
		xml += getMyTradePartyAsXML(trans.getRecipient(), false, false, !taxCodeOwasUsed);
		xml += "</ram:BuyerTradeParty>";

		if (trans.getSellerOrderReferencedDocumentID() != null) {
			xml += "<ram:SellerOrderReferencedDocument>"
				+ "<ram:IssuerAssignedID>"
				+ XMLTools.encodeXML(trans.getSellerOrderReferencedDocumentID()) + "</ram:IssuerAssignedID>"
				+ "</ram:SellerOrderReferencedDocument>";
		}
		if (trans.getBuyerOrderReferencedDocumentID() != null) {
			xml += "<ram:BuyerOrderReferencedDocument>"
				+ "<ram:IssuerAssignedID>"
				+ XMLTools.encodeXML(trans.getBuyerOrderReferencedDocumentID()) + "</ram:IssuerAssignedID>"
				+ "</ram:BuyerOrderReferencedDocument>";
		}
		if (trans.getContractReferencedDocument() != null) {
			xml += "<ram:ContractReferencedDocument>"
				+ "<ram:IssuerAssignedID>"
				+ XMLTools.encodeXML(trans.getContractReferencedDocument()) + "</ram:IssuerAssignedID>"
				+ "</ram:ContractReferencedDocument>";
		}

		// Additional Documents of XRechnung (Rechnungsbegruendende Unterlagen - BG-24 XRechnung)
		if (trans.getAdditionalReferencedDocuments() != null) {
			for (final FileAttachment f : trans.getAdditionalReferencedDocuments()) {
				final String documentContent = new String(Base64.getEncoder().encodeToString(f.getData()));
				xml += "<ram:AdditionalReferencedDocument>"
					+ "<ram:IssuerAssignedID>" + f.getFilename() + "</ram:IssuerAssignedID>"
					+ "<ram:TypeCode>916</ram:TypeCode>"
					+ "<ram:Name>" + f.getDescription() + "</ram:Name>"
					+ "<ram:AttachmentBinaryObject mimeCode=\"" + f.getMimetype() + "\"\n"
					+ "filename=\"" + f.getFilename() + "\">" + documentContent + "</ram:AttachmentBinaryObject>"
					+ "</ram:AdditionalReferencedDocument>";
			}
		}

		if (trans.getSpecifiedProcuringProjectID() != null) {
			xml += "<ram:SpecifiedProcuringProject>"
				+ "<ram:ID>"
				+ XMLTools.encodeXML(trans.getSpecifiedProcuringProjectID()) + "</ram:ID>";
			if (trans.getSpecifiedProcuringProjectName() != null) {
				xml += "<ram:Name>" + XMLTools.encodeXML(trans.getSpecifiedProcuringProjectName()) + "</ram:Name>";
			}
			xml += "</ram:SpecifiedProcuringProject>";
		}
		xml += "</ram:ApplicableHeaderTradeAgreement>";
		xml += "<ram:ApplicableHeaderTradeDelivery>";

		if (this.trans.getDeliveryAddress() != null) {
			/* MS getMyTradePartyAsXML:  */
			xml += "<ram:ShipToTradeParty>" +
				getMyTradePartyAsXML(this.trans.getDeliveryAddress(), false, true, !taxCodeOwasUsed) +
				"</ram:ShipToTradeParty>";
		}


		if (trans.getDeliveryDate() != null) {
			xml += "<ram:ActualDeliverySupplyChainEvent>"
				+ "<ram:OccurrenceDateTime>";
			xml += DATE.udtFormat(trans.getDeliveryDate());
			xml += "</ram:OccurrenceDateTime>";
			xml += "</ram:ActualDeliverySupplyChainEvent>";

		}
		/*
		 * + "<DeliveryNoteReferencedDocument>" +
		 * "<IssueDateTime format=\"102\">20130603</IssueDateTime>" +
		 * "<ID>2013-51112</ID>" +
		 * "</DeliveryNoteReferencedDocument>"
		 */
		if (trans.getDespatchAdviceReferencedDocumentID() != null) {
			xml += "<ram:DespatchAdviceReferencedDocument>";
			xml += "<ram:IssuerAssignedID>" + XMLTools.encodeXML(trans.getDespatchAdviceReferencedDocumentID()) + "</ram:IssuerAssignedID>";
			xml += "</ram:DespatchAdviceReferencedDocument>";

		}

		xml += "</ram:ApplicableHeaderTradeDelivery>";
		xml += "<ram:ApplicableHeaderTradeSettlement>";

		if ((trans.getCreditorReferenceID() != null) && (getProfile() != Profiles.getByName("Minimum"))) {
			xml += "<ram:CreditorReferenceID>" + XMLTools.encodeXML(trans.getCreditorReferenceID()) + "</ram:CreditorReferenceID>";
		}
		if ((trans.getNumber() != null) && (getProfile() != Profiles.getByName("Minimum"))) {
			xml += "<ram:PaymentReference>" + XMLTools.encodeXML(trans.getNumber()) + "</ram:PaymentReference>";
		}
		xml += "<ram:InvoiceCurrencyCode>" + trans.getCurrency() + "</ram:InvoiceCurrencyCode>";
		if (this.trans.getPayee() != null) {
			xml += "<ram:PayeeTradeParty>" +
				getTradePartyPayeeAsXML(this.trans.getPayee()) +
				"</ram:PayeeTradeParty>";
		}

		if (trans.getTradeSettlementPayment() != null) {
			for (final IZUGFeRDTradeSettlementPayment payment : trans.getTradeSettlementPayment()) {
				if (payment != null) {
					hasDueDate = true;
					if (getProfile() != Profiles.getByName("Minimum")) {
						xml += payment.getSettlementXML();
					}
				}
			}
		}
		if (trans.getTradeSettlement() != null) {
			for (final IZUGFeRDTradeSettlement payment : trans.getTradeSettlement()) {
				if (payment != null) {
					if (payment instanceof IZUGFeRDTradeSettlementPayment) {
						hasDueDate = true;
					}
					if (getProfile() != Profiles.getByName("Minimum")) {
						xml += payment.getSettlementXML();
					}
				}
			}
		}
		if ((trans.getDocumentCode() == DocumentCodeTypeConstants.CORRECTEDINVOICE) || (trans.getDocumentCode() == DocumentCodeTypeConstants.CREDITNOTE)) {
			hasDueDate = false;
		}

		final Map<BigDecimal, VATAmount> VATPercentAmountMap = calc.getVATPercentAmountMap();
		for (final BigDecimal currentTaxPercent : VATPercentAmountMap.keySet()) {
			final VATAmount amount = VATPercentAmountMap.get(currentTaxPercent);
			if (amount != null) {
				final String amountCategoryCode = amount.getCategoryCode();
				final String amountDueDateTypeCode = amount.getDueDateTypeCode();
				/* MS
				 * category "O" MUST also display an ExcemptionReason
				 */
				final boolean displayExemptionReason = CATEGORY_CODES_WITH_EXEMPTION_REASON.contains(amountCategoryCode) || amountCategoryCode.equals("O");

				if (getProfile() != Profiles.getByName("Minimum")) {
					String exemptionReasonTextXML = "";
					if ((displayExemptionReason) && (amount.getVatExemptionReasonText() != null)) {
						exemptionReasonTextXML = "<ram:ExemptionReason>" + XMLTools.encodeXML(amount.getVatExemptionReasonText()) + "</ram:ExemptionReason>";

					}

					xml += "<ram:ApplicableTradeTax>"
						+ "<ram:CalculatedAmount>" + currencyFormat(amount.getCalculated())
						+ "</ram:CalculatedAmount>" //currencyID=\"EUR\"
						+ "<ram:TypeCode>VAT</ram:TypeCode>"
						+ exemptionReasonTextXML
						+ "<ram:BasisAmount>" + currencyFormat(amount.getBasis()) + "</ram:BasisAmount>" // currencyID=\"EUR\"
						+ "<ram:CategoryCode>" + amountCategoryCode + "</ram:CategoryCode>"
						+ (amountDueDateTypeCode != null ? "<ram:DueDateTypeCode>" + amountDueDateTypeCode + "</ram:DueDateTypeCode>" : "")
                            /* MS
							 * Skip `RateApplicablePercent` if category is "O"
							 * inline change of original source
							 */
                            + (amountCategoryCode.equals("O") ?
                                "" :
                                "<ram:RateApplicablePercent>" + vatFormat(currentTaxPercent) + "</ram:RateApplicablePercent>")
                            /* MS ^^^
							*/
					    + "</ram:ApplicableTradeTax>";
				}
			}
		}
		if ((trans.getDetailedDeliveryPeriodFrom() != null) || (trans.getDetailedDeliveryPeriodTo() != null)) {
			xml += "<ram:BillingSpecifiedPeriod>";
			if (trans.getDetailedDeliveryPeriodFrom() != null) {
				xml += "<ram:StartDateTime>" + DATE.udtFormat(trans.getDetailedDeliveryPeriodFrom()) + "</ram:StartDateTime>";
			}
			if (trans.getDetailedDeliveryPeriodTo() != null) {
				xml += "<ram:EndDateTime>" + DATE.udtFormat(trans.getDetailedDeliveryPeriodTo()) + "</ram:EndDateTime>";
			}
			xml += "</ram:BillingSpecifiedPeriod>";
		}

		if ((trans.getZFCharges() != null) && (trans.getZFCharges().length > 0)) {
			if (profile == Profiles.getByName("XRechnung")) {
				for (IZUGFeRDAllowanceCharge charge : trans.getZFCharges()) {
					xml += "<ram:SpecifiedTradeAllowanceCharge>" +
						"<ram:ChargeIndicator>" +
						"<udt:Indicator>true</udt:Indicator>" +
						"</ram:ChargeIndicator>" +
						"<ram:ActualAmount>" + currencyFormat(charge.getTotalAmount(calc)) + "</ram:ActualAmount>";
					if (charge.getReason() != null) {
						xml += "<ram:Reason>" + XMLTools.encodeXML(charge.getReason()) + "</ram:Reason>";
					}
					if (charge.getReasonCode() != null) {
						xml += "<ram:ReasonCode>" + charge.getReasonCode() + "</ram:ReasonCode>";
					}
					xml += "<ram:CategoryTradeTax>" +
						"<ram:TypeCode>VAT</ram:TypeCode>" +
						"<ram:CategoryCode>" + charge.getCategoryCode() + "</ram:CategoryCode>";

					/* MS
					 * skip `RateApplicablePercent` if category is "O"
					 * change of original source
					 */
					if ((! charge.getCategoryCode().equals("O")) && (charge.getTaxPercent() != null)) {
						xml += "<ram:RateApplicablePercent>" + vatFormat(charge.getTaxPercent()) + "</ram:RateApplicablePercent>";
					}
					xml += "</ram:CategoryTradeTax>" +
						"</ram:SpecifiedTradeAllowanceCharge>";
				}
			} else {
				for (final BigDecimal currentTaxPercent : VATPercentAmountMap.keySet()) {
					if (calc.getChargesForPercent(currentTaxPercent).compareTo(BigDecimal.ZERO) != 0) {
						xml += "<ram:SpecifiedTradeAllowanceCharge>" +
							"<ram:ChargeIndicator>" +
							"<udt:Indicator>true</udt:Indicator>" +
							"</ram:ChargeIndicator>" +
							"<ram:ActualAmount>" + currencyFormat(calc.getChargesForPercent(currentTaxPercent)) + "</ram:ActualAmount>" +
							"<ram:Reason>" + XMLTools.encodeXML(calc.getChargeReasonForPercent(currentTaxPercent)) + "</ram:Reason>" +
							"<ram:CategoryTradeTax>" +
							"<ram:TypeCode>VAT</ram:TypeCode>" +
							"<ram:CategoryCode>" +
 VATPercentAmountMap.get(currentTaxPercent).getCategoryCode() + "</ram:CategoryCode>";

                    		/* MS
							 * skip `RateApplicablePercent` if category is "O"
							 * change of original source
                             * must be after `CategoryCode`
							 */
                            if (! VATPercentAmountMap.get(currentTaxPercent).getCategoryCode().equals("O")) {
                                xml += "<ram:RateApplicablePercent>" + vatFormat(currentTaxPercent)
                                + "</ram:RateApplicablePercent>";
                            };

						xml += "</ram:CategoryTradeTax>" +
							"</ram:SpecifiedTradeAllowanceCharge>";
					}
				}
			}
		}

		if ((trans.getZFAllowances() != null) && (trans.getZFAllowances().length > 0)) {
			if (profile == Profiles.getByName("XRechnung")) {
				for (IZUGFeRDAllowanceCharge allowance : trans.getZFAllowances()) {
					xml += "<ram:SpecifiedTradeAllowanceCharge>" +
						"<ram:ChargeIndicator>" +
						"<udt:Indicator>false</udt:Indicator>" +
						"</ram:ChargeIndicator>" +
						"<ram:ActualAmount>" + currencyFormat(allowance.getTotalAmount(calc)) + "</ram:ActualAmount>";
					if (allowance.getReason() != null) {
						xml += "<ram:Reason>" + XMLTools.encodeXML(allowance.getReason()) + "</ram:Reason>";
					}
					if (allowance.getReasonCode() != null) {
						xml += "<ram:ReasonCode>" + allowance.getReasonCode() + "</ram:ReasonCode>";
					}
					xml += "<ram:CategoryTradeTax>" +
						"<ram:TypeCode>VAT</ram:TypeCode>" +
						"<ram:CategoryCode>" + allowance.getCategoryCode() + "</ram:CategoryCode>";
					if (allowance.getTaxPercent() != null) {
						xml += "<ram:RateApplicablePercent>" + vatFormat(allowance.getTaxPercent()) + "</ram:RateApplicablePercent>";
					}
					xml += "</ram:CategoryTradeTax>" +
						"</ram:SpecifiedTradeAllowanceCharge>";
				}
			} else {
				for (final BigDecimal currentTaxPercent : VATPercentAmountMap.keySet()) {
					if (calc.getAllowancesForPercent(currentTaxPercent).compareTo(BigDecimal.ZERO) != 0) {
						xml += "<ram:SpecifiedTradeAllowanceCharge>" +
							"<ram:ChargeIndicator>" +
							"<udt:Indicator>false</udt:Indicator>" +
							"</ram:ChargeIndicator>" +
							"<ram:ActualAmount>" + currencyFormat(calc.getAllowancesForPercent(currentTaxPercent)) + "</ram:ActualAmount>" +
							"<ram:Reason>" + XMLTools.encodeXML(calc.getAllowanceReasonForPercent(currentTaxPercent)) + "</ram:Reason>" +
							"<ram:CategoryTradeTax>" +
							"<ram:TypeCode>VAT</ram:TypeCode>" +
							"<ram:CategoryCode>" + VATPercentAmountMap.get(currentTaxPercent).getCategoryCode() + "</ram:CategoryCode>" +
							"<ram:RateApplicablePercent>" + vatFormat(currentTaxPercent) + "</ram:RateApplicablePercent>" +
							"</ram:CategoryTradeTax>" +
							"</ram:SpecifiedTradeAllowanceCharge>";
					}
				}
			}
		}

		if ((trans.getPaymentTerms() == null) && (getProfile() != Profiles.getByName("Minimum")) && ((paymentTermsDescription != null) || (trans.getTradeSettlement() != null) || (hasDueDate))) {
			xml += "<ram:SpecifiedTradePaymentTerms>";

			if (paymentTermsDescription != null) {
				xml += "<ram:Description>" + paymentTermsDescription + "</ram:Description>";
			}

			if (trans.getTradeSettlement() != null) {
				for (final IZUGFeRDTradeSettlement payment : trans.getTradeSettlement()) {
					if ((payment != null) && (payment instanceof IZUGFeRDTradeSettlementDebit)) {
						xml += payment.getPaymentXML();
					}
				}
			}

			if (trans.getDueDate() != null) {
				xml += "<ram:DueDateDateTime>" // $NON-NLS-2$
					+ DATE.udtFormat(trans.getDueDate())
					+ "</ram:DueDateDateTime>";// 20130704

			}
			xml += "</ram:SpecifiedTradePaymentTerms>";
		} else {
			xml += buildPaymentTermsXml();
		}
		if ((profile == Profiles.getByName("Extended")) && (trans.getCashDiscounts() != null) && (trans.getCashDiscounts().length > 0)) {
			for (IZUGFeRDCashDiscount discount : trans.getCashDiscounts()
			) {
				xml += discount.getAsCII();
			}
		}


		final String allowanceTotalLine = "<ram:AllowanceTotalAmount>" + currencyFormat(calc.getAllowancesForPercent(null)) + "</ram:AllowanceTotalAmount>";

		final String chargesTotalLine = "<ram:ChargeTotalAmount>" + currencyFormat(calc.getChargesForPercent(null)) + "</ram:ChargeTotalAmount>";

		xml += "<ram:SpecifiedTradeSettlementHeaderMonetarySummation>";
		if ((getProfile() != Profiles.getByName("Minimum")) && (getProfile() != Profiles.getByName("BASICWL"))) {
			xml += "<ram:LineTotalAmount>" + currencyFormat(calc.getTotal()) + "</ram:LineTotalAmount>";
			xml += chargesTotalLine
				+ allowanceTotalLine;
		}
		xml += "<ram:TaxBasisTotalAmount>" + currencyFormat(calc.getTaxBasis()) + "</ram:TaxBasisTotalAmount>"
			// //
			// currencyID=\"EUR\"
			+ "<ram:TaxTotalAmount currencyID=\"" + trans.getCurrency() + "\">"
			+ currencyFormat(calc.getGrandTotal().subtract(calc.getTaxBasis())) + "</ram:TaxTotalAmount>";
		if (trans.getRoundingAmount() != null) {
			xml += "<ram:RoundingAmount>" + currencyFormat(trans.getRoundingAmount()) + "</ram:RoundingAmount>";
		}

		xml += "<ram:GrandTotalAmount>" + currencyFormat(calc.getGrandTotal()) + "</ram:GrandTotalAmount>";
		// //
		// currencyID=\"EUR\"
		if (getProfile() != Profiles.getByName("Minimum")) {
			xml += "<ram:TotalPrepaidAmount>" + currencyFormat(calc.getTotalPrepaid()) + "</ram:TotalPrepaidAmount>";
		}
		xml += "<ram:DuePayableAmount>" + currencyFormat(calc.getDuePayable()) + "</ram:DuePayableAmount>"
			+ "</ram:SpecifiedTradeSettlementHeaderMonetarySummation>";
		if (trans.getInvoiceReferencedDocumentID() != null) {
			xml += "<ram:InvoiceReferencedDocument>"
				+ "<ram:IssuerAssignedID>"
				+ XMLTools.encodeXML(trans.getInvoiceReferencedDocumentID()) + "</ram:IssuerAssignedID>";
			if (trans.getInvoiceReferencedIssueDate() != null) {
				xml += "<ram:FormattedIssueDateTime>"
					+ DATE.qdtFormat(trans.getInvoiceReferencedIssueDate())
					+ "</ram:FormattedIssueDateTime>";
			}
			xml += "</ram:InvoiceReferencedDocument>";
		}

		xml += "</ram:ApplicableHeaderTradeSettlement>";
		// + "<IncludedSupplyChainTradeLineItem>\n"
		// + "<AssociatedDocumentLineDocument>\n"
		// + "<IncludedNote>\n"
		// + "<Content>Wir erlauben uns Ihnen folgende Positionen aus der Lieferung Nr.
		// 2013-51112 in Rechnung zu stellen:</Content>\n"
		// + "</IncludedNote>\n"
		// + "</AssociatedDocumentLineDocument>\n"
		// + "</IncludedSupplyChainTradeLineItem>\n";

		xml += "</rsm:SupplyChainTradeTransaction>"
			+ "</rsm:CrossIndustryInvoice>";

		final byte[] zugferdRaw;
		zugferdRaw = xml.getBytes(StandardCharsets.UTF_8);

		zugferdData = XMLTools.removeBOM(zugferdRaw);
	}

	protected String buildItemNotes(IZUGFeRDExportableItem currentItem) {
		if (currentItem.getNotes() == null) {
			return "";
		}
		return Arrays.stream(currentItem.getNotes())
				.map(IncludedNote::unspecifiedNote)
				.map(IncludedNote::toCiiXml)
				.collect(Collectors.joining());
	}

	protected String buildNotes(IExportableTransaction exportableTransaction) {
		final List<IncludedNote> includedNotes = new ArrayList<>();
		Optional.ofNullable(exportableTransaction.getNotesWithSubjectCode()).ifPresent(includedNotes::addAll);

		if (exportableTransaction.getNotes() != null) {
			for (final String currentNote : exportableTransaction.getNotes()) {
				includedNotes.add(IncludedNote.unspecifiedNote(currentNote));
			}
		}
		if (exportableTransaction.rebateAgreementExists()) {
			includedNotes.add(IncludedNote.discountBonusNote("Es bestehen Rabatt- und Bonusvereinbarungen."));
		}
		Optional.ofNullable(exportableTransaction.getOwnOrganisationFullPlaintextInfo())
				.ifPresent(info -> includedNotes.add(IncludedNote.regulatoryNote(info)));

		Optional.ofNullable(exportableTransaction.getSubjectNote())
				.ifPresent(note -> includedNotes.add(IncludedNote.unspecifiedNote(note)));

		return includedNotes.stream().map(IncludedNote::toCiiXml).collect(Collectors.joining(""));
	}

	@Override
	public void setProfile(Profile p) {
		profile = p;
	}

	private String buildPaymentTermsXml() {
		final IZUGFeRDPaymentTerms paymentTerms = trans.getPaymentTerms();
		if (paymentTerms == null) {
			return "";
		}
		String paymentTermsXml = "<ram:SpecifiedTradePaymentTerms>";

		final IZUGFeRDPaymentDiscountTerms discountTerms = paymentTerms.getDiscountTerms();
		final Date dueDate = paymentTerms.getDueDate();
		if (dueDate != null && discountTerms != null && discountTerms.getBaseDate() != null) {
			throw new IllegalStateException(
					"if paymentTerms.dueDate is specified, paymentTerms.discountTerms.baseDate has not to be specified");
		}
		paymentTermsXml += "<ram:Description>" + paymentTerms.getDescription() + "</ram:Description>";

		if (dueDate != null) {
			paymentTermsXml += "<ram:DueDateDateTime>";
			paymentTermsXml += DATE.udtFormat(dueDate);
			paymentTermsXml += "</ram:DueDateDateTime>";
		}

		if (trans.getTradeSettlement() != null) {
			for (final IZUGFeRDTradeSettlement payment : trans.getTradeSettlement()) {
				if ((payment != null) && (payment instanceof IZUGFeRDTradeSettlementDebit)) {
					paymentTermsXml += payment.getPaymentXML();
				}
			}
		}

		if (discountTerms != null) {
			paymentTermsXml += "<ram:ApplicableTradePaymentDiscountTerms>";
			final String currency = trans.getCurrency();
			final String basisAmount = currencyFormat(calc.getGrandTotal());
			paymentTermsXml += "<ram:BasisAmount currencyID=\"" + currency + "\">" + basisAmount + "</ram:BasisAmount>";
			paymentTermsXml += "<ram:CalculationPercent>" + discountTerms.getCalculationPercentage().toString()
					+ "</ram:CalculationPercent>";

			if (discountTerms.getBaseDate() != null) {
				final Date baseDate = discountTerms.getBaseDate();
				paymentTermsXml += "<ram:BasisDateTime>";
				paymentTermsXml += DATE.udtFormat(baseDate);
				paymentTermsXml += "</ram:BasisDateTime>";

				paymentTermsXml += "<ram:BasisPeriodMeasure unitCode=\"" + discountTerms.getBasePeriodUnitCode() + "\">"
						+ discountTerms.getBasePeriodMeasure() + "</ram:BasisPeriodMeasure>";
			}

			paymentTermsXml += "</ram:ApplicableTradePaymentDiscountTerms>";
		}

		paymentTermsXml += "</ram:SpecifiedTradePaymentTerms>";
		return paymentTermsXml;
	}

}
