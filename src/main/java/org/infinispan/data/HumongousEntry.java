package org.infinispan.data;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.protostream.annotations.Proto;

@Proto
@Indexed(keyEntity = "test.PersonKey")
public record HumongousEntry(
      long dataDiCaricamento,

      @Basic(sortable = true, aggregable = true)
      long progressiveId,

      @Basic(sortable = true, aggregable = true)
      String fileName,
      String businessMessageIdentifier,
      String messageDefinitionIdentifier,
      String businessService,
      long creationDate,

      @Basic(sortable = true, aggregable = true)
      String technicalRecordIdentification,

      @Basic(sortable = true, aggregable = true)
      long reportingTimestamp,

      @Basic(sortable = true, aggregable = true)
      String reportSubmittingEntity,

      @Basic(sortable = true, aggregable = true)
      String reportingCounterParty,

      @Basic(sortable = true, aggregable = true)
      String natureOfTheReportingCounterParty,

      @Basic(sortable = true, aggregable = true)
      String sectorOfTheReportingCounterParty,

      String additionalSectorClassification,

      @Basic(sortable = true, aggregable = true)
      String branchOfTheReportingCounterParty,

      @Basic(sortable = true, aggregable = true)
      String branchOfTheOtherCounterParty,

      @Basic(sortable = true, aggregable = true)
      String counterPartySide,

      @Basic(sortable = true, aggregable = true)
      String entityResponsibleForTheReport,

      @Basic(sortable = true, aggregable = true)
      String otherCounterParty,

      @Basic(sortable = true, aggregable = true)
      String countryOfTheOtherCounterParty,

      @Basic(sortable = true, aggregable = true)
      String beneficiary,

      @Basic(sortable = true, aggregable = true)
      String tripartyAgent,

      @Basic(sortable = true, aggregable = true)
      String broker,

      @Basic(sortable = true, aggregable = true)
      String clearingMember,

      @Basic(sortable = true, aggregable = true)
      String centralSecuritiesDepositoryCSDParticipantOrIndirectParticipant,

      @Basic(sortable = true, aggregable = true)
      String agentLender,

      @Basic(sortable = true, aggregable = true)
      String uniqueTransactionIdentifierUTI,

      String reportTrackingNumber,

      @Basic(sortable = true, aggregable = true)
      long eventDate,

      @Basic(sortable = true, aggregable = true)
      String typeOfSFT,

      @Basic(sortable = true, aggregable = true)
      String cleared,

      long clearingTimestamp,

      @Basic(sortable = true, aggregable = true)
      String CCP,

      String tradingVenue,
      String materAgreementType,
      String otherMasterAgreementType,
      String masterAgreementVersion,
      long executionTimestamp,
      long valueDateStartDate,
      long maturityDateEndDate,
      long terminationDate,
      String minimumNoticePeriod,
      long earliestCallbackDate,
      String generalCollateralIndicator,
      String deliveryByValueIndicator,
      String methodUsedToProvideCollateral,

      @Basic(sortable = true, aggregable = true)
      String openTerm,

      String terminationOptionality,
      String fixedRate,
      String floatingRateReferencePeriodTimePeriod,
      String floatingRateReferencePeriodMultiplier,
      String floatingRatePaymentFrequencyTimePeriod,
      String floatingRatePaymentFrequencyMultiplier,
      String floatingRateResetFrequencyTimePeriod,
      String floatingRateResetFrequencyMultiplier,
      String spread,
      double marginLendingCurrencyAmount,
      String marginLendingCurrency,
      String adjustedRate,
      String rateDate,
      String principalAmountOnTheValueDate,
      String principalAmountOnTheMaturityDate,
      String principalAmountCurrency,

      @Basic(sortable = true, aggregable = true)
      String typeOfAsset,

      @Basic(sortable = true, aggregable = true)
      String securityIdentifier,

      @Basic(sortable = true, aggregable = true)
      String classificationOfASecurity,
      String baseProduct1,
      String subProduct1,
      String furtherSubProduct1,
      String quantityOrNominalAmount,
      String unitOfMeasure,
      String currencyOfNominalAmount,
      String securityOrCommodityPrice,
      String priceCurrency1,

      @Basic(sortable = true, aggregable = true)
      String securityQuality,

      String maturityOfTheSecurity,

      @Basic(sortable = true, aggregable = true)
      String jurisdictionOfTheIssuer1,

      @Basic(sortable = true, aggregable = true)
      String LEIOfTheIssuer1,

      @Basic(sortable = true, aggregable = true)
      String securityType,

      @Basic(sortable = true, aggregable = true)
      String loanValue,

      String marketValue,
      String fixedRebateRate,
      String floatingRebateRate,
      String floatingRebateRateReferencePeriodTimePeriod,
      String floatingRebateRateReferencePeriodMultiplier,
      String floatingRebateRatePaymentFrequencyTimePeriod,
      String floatingRebateRatePaymentFrequencyMultiplier,
      String floatingRebateRateResetFrequencyTimePeriod,
      String floatingRebateRateResetFrequencyMultiplier,
      String spreadOfTheRebateRate,

      @Basic(sortable = true, aggregable = true)
      String lendingFee,

      String exclusiveArrangements,
      String outstandingMarginLoan,
      String baseCurrencyOfOutstandingMarginLoan,
      String shortMarketValue,

      @Basic(sortable = true, aggregable = true)
      String uncollateralisedSecuritiesLendingFlag,

      @Basic(sortable = true, aggregable = true)
      String collateralisationOfNetExposure,

      long valueDateOfTheCollateral,

      @Basic(sortable = true, aggregable = true)
      String typeOfCollateralComponent,

      String cashCollateralAmount,
      String cashCollateralCurrency,

      @Basic(sortable = true, aggregable = true)
      String identificationOfASecurityUsedAsCollateral,

      @Basic(sortable = true, aggregable = true)
      String classificationOfASecurityUsedAsCollateral,

      String baseProduct2,
      String subProduct2,
      String furtherSubProduct2,
      String collateralQuantityOrNominalAmount,
      String collateralUnitOfMeasure,
      String currencyOfCollateralNominalAmount,
      String priceCurrency2,
      String pricePerUnit,
      String collateralMarketValue,
      String haircutOrMargin,

      @Basic(sortable = true, aggregable = true)
      String collateralQuality,

      String maturityDateOfTheSecurity,

      @Basic(sortable = true, aggregable = true)
      String jurisdictionOfTheIssuer2,

      @Basic(sortable = true, aggregable = true)
      String LEIOfTheIssuer2,

      @Basic(sortable = true, aggregable = true)
      String collateralType,

      @Basic(sortable = true, aggregable = true)
      String availabilityForCollateralReuse,

      @Basic(sortable = true, aggregable = true)
      String collateralBasketIdentifier,

      String portfolioCode,

      @Basic(sortable = true, aggregable = true)
      String actionType,

      @Basic(sortable = true, aggregable = true)
      String level,

      long dataDeRicezione,
      String tradeRepository,
      String flusso,

      @Basic(sortable = true, aggregable = true)
      long receivedReportDate
) {

   public static HumongousEntry createRandom() {
      return new HumongousEntry(
            randomTimestamp(),                             // dataDiCaricamento
            randomLong(),                                  // progressiveId
            randomString("fileName"),                      // fileName
            randomString("bizMsgId"),                      // businessMessageIdentifier
            randomString("msgDefId"),                      // messageDefinitionIdentifier
            randomString("bizService"),                    // businessService
            randomTimestamp(),                             // creationDate
            randomString("techRecId"),                     // technicalRecordIdentification
            randomTimestamp(),                             // reportingTimestamp
            randomString("repSubEnt"),                     // reportSubmittingEntity
            randomString("repCounterParty"),               // reportingCounterParty
            randomString("natureCounterParty"),            // natureOfTheReportingCounterParty
            randomString("sectorCounterParty"),            // sectorOfTheReportingCounterParty
            randomString("addSectorClass"),                // additionalSectorClassification
            randomString("branchRepCP"),                   // branchOfTheReportingCounterParty
            randomString("branchOtherCP"),                 // branchOfTheOtherCounterParty
            randomString("cpSide"),                        // counterPartySide
            randomString("entityResp"),                    // entityResponsibleForTheReport
            randomString("otherCP"),                       // otherCounterParty
            randomString("countryOtherCP"),                // countryOfTheOtherCounterParty
            randomString("beneficiary"),                   // beneficiary
            randomString("tripartyAgent"),                 // tripartyAgent
            randomString("broker"),                        // broker
            randomString("clearingMember"),                // clearingMember
            randomString("csdPart"),                       // centralSecuritiesDepositoryCSDParticipantOrIndirectParticipant
            randomString("agentLender"),                   // agentLender
            randomString("uti"),                           // uniqueTransactionIdentifierUTI
            randomString("trackingNum"),                   // reportTrackingNumber
            randomTimestamp(),                             // eventDate
            randomString("typeSFT"),                       // typeOfSFT
            randomString("cleared"),                       // cleared
            randomTimestamp(),                             // clearingTimestamp
            randomString("ccp"),                           // CCP
            randomString("tradingVenue"),                  // tradingVenue
            randomString("masterAgreeType"),               // materAgreementType
            randomString("otherMasterType"),               // otherMasterAgreementType
            randomString("masterVer"),                     // masterAgreementVersion
            randomTimestamp(),                             // executionTimestamp
            randomTimestamp(),                             // valueDateStartDate
            randomTimestamp(),                             // maturityDateEndDate
            randomTimestamp(),                             // terminationDate
            randomString("minNotice"),                     // minimumNoticePeriod
            randomTimestamp(),                             // earliestCallbackDate
            randomString("genColInd"),                     // generalCollateralIndicator
            randomString("delByValInd"),                   // deliveryByValueIndicator
            randomString("methodCol"),                     // methodUsedToProvideCollateral
            randomString("openTerm"),                      // openTerm
            randomString("termOpt"),                       // terminationOptionality
            randomString("fixedRate"),                     // fixedRate
            randomString("floatRefPer"),                   // floatingRateReferencePeriodTimePeriod
            randomString("floatRefMult"),                  // floatingRateReferencePeriodMultiplier
            randomString("floatPayFreq"),                  // floatingRatePaymentFrequencyTimePeriod
            randomString("floatPayMult"),                  // floatingRatePaymentFrequencyMultiplier
            randomString("floatResetFreq"),                // floatingRateResetFrequencyTimePeriod
            randomString("floatResetMult"),                // floatingRateResetFrequencyMultiplier
            randomString("spread"),                        // spread
            randomDouble(),                                // marginLendingCurrencyAmount (Double)
            randomString("marginCur"),                     // marginLendingCurrency
            randomString("adjRate"),                       // adjustedRate
            randomString("rateDate"),                      // rateDate
            randomString("princAmtVal"),                   // principalAmountOnTheValueDate
            randomString("princAmtMat"),                   // principalAmountOnTheMaturityDate
            randomString("princCur"),                      // principalAmountCurrency
            randomString("typeAsset"),                     // typeOfAsset
            randomString("secId"),                         // securityIdentifier
            randomString("classSec"),                      // classificationOfASecurity
            randomString("baseProd1"),                     // baseProduct1
            randomString("subProd1"),                      // subProduct1
            randomString("furSubProd1"),                   // furtherSubProduct1
            randomString("qtyNomAmt"),                     // quantityOrNominalAmount
            randomString("uom"),                           // unitOfMeasure
            randomString("curNomAmt"),                     // currencyOfNominalAmount
            randomString("secComPrice"),                   // securityOrCommodityPrice
            randomString("priceCur1"),                     // priceCurrency1
            randomString("secQual"),                       // securityQuality
            randomString("matSec"),                        // maturityOfTheSecurity
            randomString("jurisIss1"),                     // jurisdictionOfTheIssuer1
            randomString("leiIss1"),                       // LEIOfTheIssuer1
            randomString("secType"),                       // securityType
            randomString("loanVal"),                       // loanValue
            randomString("mktVal"),                        // marketValue
            randomString("fixRebate"),                     // fixedRebateRate
            randomString("floatRebate"),                   // floatingRebateRate
            randomString("floatRebRefPer"),                // floatingRebateRateReferencePeriodTimePeriod
            randomString("floatRebRefMult"),               // floatingRebateRateReferencePeriodMultiplier
            randomString("floatRebPayFreq"),               // floatingRebateRatePaymentFrequencyTimePeriod
            randomString("floatRebPayMult"),               // floatingRebateRatePaymentFrequencyMultiplier
            randomString("floatRebResetFreq"),             // floatingRebateRateResetFrequencyTimePeriod
            randomString("floatRebResetMult"),             // floatingRebateRateResetFrequencyMultiplier
            randomString("spreadRebate"),                  // spreadOfTheRebateRate
            randomString("lendFee"),                       // lendingFee
            randomString("exclArr"),                       // exclusiveArrangements
            randomString("outMarginLoan"),                 // outstandingMarginLoan
            randomString("baseCurOut"),                    // baseCurrencyOfOutstandingMarginLoan
            randomString("shortMktVal"),                   // shortMarketValue
            randomString("uncolSecFlag"),                  // uncollateralisedSecuritiesLendingFlag
            randomString("colNetExp"),                     // collateralisationOfNetExposure
            randomTimestamp(),                             // valueDateOfTheCollateral
            randomString("typeColComp"),                   // typeOfCollateralComponent
            randomString("cashColAmt"),                    // cashCollateralAmount
            randomString("cashColCur"),                    // cashCollateralCurrency
            randomString("idSecCol"),                      // identificationOfASecurityUsedAsCollateral
            randomString("classSecCol"),                   // classificationOfASecurityUsedAsCollateral
            randomString("baseProd2"),                     // baseProduct2
            randomString("subProd2"),                      // subProduct2
            randomString("furSubProd2"),                   // furtherSubProduct2
            randomString("colQtyNom"),                     // collateralQuantityOrNominalAmount
            randomString("colUom"),                        // collateralUnitOfMeasure
            randomString("curColNom"),                     // currencyOfCollateralNominalAmount
            randomString("priceCur2"),                     // priceCurrency2
            randomString("pricePerUnit"),                  // pricePerUnit
            randomString("colMktVal"),                     // collateralMarketValue
            randomString("haircut"),                       // haircutOrMargin
            randomString("colQual"),                       // collateralQuality
            randomString("matDateSec"),                    // maturityDateOfTheSecurity
            randomString("jurisIss2"),                     // jurisdictionOfTheIssuer2
            randomString("leiIss2"),                       // LEIOfTheIssuer2
            randomString("colType"),                       // collateralType
            randomString("availReuse"),                    // availabilityForCollateralReuse
            randomString("colBaskId"),                     // collateralBasketIdentifier
            randomString("portCode"),                      // portfolioCode
            randomString("actionType"),                    // actionType
            randomString("level"),                         // level
            randomTimestamp(),                             // dataDeRicezione
            randomString("tradeRepo"),                     // tradeRepository
            randomString("flusso"),                        // flusso
            randomTimestamp()                              // receivedReportDate
      );
   }

   // --- Helper Methods ---
   private static String randomString(String prefix) {
      // Appends a short random UUID substring to a readable prefix
      return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
   }

   private static long randomLong() {
      return ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
   }

   private static long randomTimestamp() {
      // Returns a timestamp +/- 30 days from now
      long now = System.currentTimeMillis();
      long thirtyDays = 30L * 24 * 60 * 60 * 1000;
      return ThreadLocalRandom.current().nextLong(now - thirtyDays, now + thirtyDays);
   }

   private static double randomDouble() {
      return ThreadLocalRandom.current().nextDouble(1.0, 1000000.0);
   }
}
