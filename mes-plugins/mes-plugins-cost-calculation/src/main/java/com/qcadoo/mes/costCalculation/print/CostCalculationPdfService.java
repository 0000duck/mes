/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.3
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.mes.costCalculation.print;

import static com.google.common.collect.Lists.newLinkedList;
import static com.qcadoo.mes.costCalculation.constants.CostCalculationFields.CALCULATE_OPERATION_COSTS_MODE;
import static com.qcadoo.mes.costCalculation.constants.CostCalculationFields.CALCULATION_OPERATION_COMPONENTS;
import static com.qcadoo.mes.costCalculation.constants.CostCalculationFields.NAME;
import static com.qcadoo.mes.costCalculation.constants.CostCalculationFields.NUMBER;
import static com.qcadoo.mes.costCalculation.constants.CostCalculationFields.ORDER;
import static com.qcadoo.mes.costCalculation.constants.CostCalculationFields.TECHNOLOGY;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.mes.basic.util.CurrencyService;
import com.qcadoo.mes.costCalculation.constants.CostCalculationConstants;
import com.qcadoo.mes.orders.util.EntityNumberComparator;
import com.qcadoo.mes.technologies.ProductQuantitiesService;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.NumberService;
import com.qcadoo.model.api.utils.TreeNumberingService;
import com.qcadoo.report.api.FontUtils;
import com.qcadoo.report.api.SortUtil;
import com.qcadoo.report.api.pdf.PdfDocumentService;
import com.qcadoo.report.api.pdf.PdfHelper;
import com.qcadoo.security.api.SecurityService;
import com.qcadoo.view.api.utils.TimeConverterService;

@Service
public class CostCalculationPdfService extends PdfDocumentService {

    private static final String COST_CALCULATION_COST_CALCULATION_DETAILS_REPORT_COLUMN_HEADER_NUMBER = "costCalculation.costCalculationDetails.report.columnHeader.number";

    private static final String COST_CALCULATION_COST_CALCULATION_DETAILS_REPORT_COLUMN_HEADER_NAME = "costCalculation.costCalculationDetails.report.columnHeader.name";

    @Autowired
    SecurityService securityService;

    @Autowired
    DataDefinitionService dataDefinitionService;

    @Autowired
    CurrencyService currencyService;

    @Autowired
    TreeNumberingService treeNumberingService;

    @Autowired
    private ProductQuantitiesService productQuantitiesService;

    @Autowired
    TimeConverterService timeConverterService;

    @Autowired
    private NumberService numberService;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private PdfHelper pdfHelper;

    private static final String tabInText = "\t \t \t";

    @Override
    protected void buildPdfContent(final Document document, final Entity entity, final Locale locale) throws DocumentException {
        String documentTitle = translationService.translate("costCalculation.costCalculationDetails.report.title", locale);
        String documentAuthor = translationService.translate("qcadooReport.commons.generatedBy.label", locale);
        pdfHelper
                .addDocumentHeader(document, "", documentTitle, documentAuthor, new Date(), securityService.getCurrentUserName());

        DataDefinition dataDefCostCalculation = dataDefinitionService.get(CostCalculationConstants.PLUGIN_IDENTIFIER,
                CostCalculationConstants.MODEL_COST_CALCULATION);
        Entity costCalculation = dataDefCostCalculation.find("where id = " + entity.getId().toString()).uniqueResult();

        PdfPTable leftPanelColumn = addLeftPanelToReport(costCalculation, locale);
        PdfPTable rightPanelColumn = addRightPanelToReport(costCalculation, locale);

        PdfPTable panelTable = pdfHelper.createPanelTable(2);
        panelTable.addCell(leftPanelColumn);
        panelTable.addCell(rightPanelColumn);
        panelTable.setSpacingAfter(20);
        panelTable.setSpacingBefore(20);
        document.add(panelTable);

        document.add(new Paragraph(translationService
                .translate("costCalculation.costCalculationDetails.report.paragraph", locale), FontUtils.getDejavuBold11Dark()));
        PdfPTable materialsTable = addMaterialsTable(costCalculation, locale);
        document.add(materialsTable);

        document.add(Chunk.NEWLINE);
        document.add(new Paragraph(translationService.translate("costCalculation.costCalculationDetails.report.paragraph2",
                locale), FontUtils.getDejavuBold11Dark()));

        if ("hourly".equals(costCalculation.getField(CALCULATE_OPERATION_COSTS_MODE))) {
            document.add(addTableAboutHourlyCost(costCalculation, locale));
        } else {
            document.add(addTableAboutPieceworkCost(costCalculation, locale));
        }

        if ((costCalculation.getBooleanField("printCostNormsOfMaterials") == true)) {

            document.add(Chunk.NEWLINE);
            document.add(new Paragraph(translationService.translate("costCalculation.costCalculationDetails.report.paragraph3",
                    locale), FontUtils.getDejavuBold11Dark()));
            PdfPTable optionTable = addOptionTablePrintCostNormsOfMaterials(costCalculation, locale);
            document.add(optionTable);
        }

        if ((costCalculation.getBooleanField("printOperationNorms") == true)) {

            if ("piecework".equals(costCalculation.getField(CALCULATE_OPERATION_COSTS_MODE))) {

                document.add(Chunk.NEWLINE);
                document.add(new Paragraph(translationService.translate(
                        "costCalculation.costCalculationDetails.report.paragraph4", locale), FontUtils.getDejavuBold11Dark()));
                PdfPTable optionTable2 = addOptionTablePrintOperationNormsPiecework(costCalculation, locale);
                document.add(optionTable2);
            } else if ("hourly".equals(costCalculation.getField(CALCULATE_OPERATION_COSTS_MODE))) {

                document.add(Chunk.NEWLINE);
                document.add(new Paragraph(translationService.translate(
                        "costCalculation.costCalculationDetails.report.paragraph4", locale), FontUtils.getDejavuBold11Dark()));
                addOptionTablePrintOperationNormsHourly(document, costCalculation, locale);
            }
        }
    }

    @Override
    public String getReportTitle(final Locale locale) {
        return translationService.translate("costCalculation.costCalculationDetails.report.title", locale);
    }

    public PdfPTable addLeftPanelToReport(final Entity costCalculation, final Locale locale) {
        PdfPTable leftPanelColumn = pdfHelper.createPanelTable(1);

        pdfHelper.addTableCellAsTwoColumnsTable(leftPanelColumn,
                translationService.translate("costCalculation.costCalculation.number.label", locale) + ":",
                costCalculation.getStringField(NUMBER));
        pdfHelper.addTableCellAsTwoColumnsTable(leftPanelColumn,
                translationService.translate("costCalculation.costCalculation.product.label", locale) + ":", costCalculation
                        .getBelongsToField("product").getStringField(NAME));
        pdfHelper.addTableCellAsTwoColumnsTable(leftPanelColumn,
                translationService.translate("costCalculation.costCalculation.defaultTechnology.label", locale) + ":",
                costCalculation.getBelongsToField("defaultTechnology").getStringField(NAME));
        pdfHelper.addTableCellAsTwoColumnsTable(leftPanelColumn,
                translationService.translate("costCalculation.costCalculation.technology.label", locale) + ":", costCalculation
                        .getBelongsToField(TECHNOLOGY).getStringField(NAME));
        pdfHelper.addTableCellAsTwoColumnsTable(leftPanelColumn,
                translationService.translate("costCalculation.costCalculation.quantity.label", locale) + ":",
                numberService.format(costCalculation.getField("quantity")));
        Entity order = costCalculation.getBelongsToField(ORDER);
        pdfHelper.addTableCellAsTwoColumnsTable(leftPanelColumn,
                translationService.translate("costCalculation.costCalculation.order.label", locale) + ":", order == null ? ""
                        : order.getStringField(NAME));

        leftPanelColumn.addCell(new Phrase(translationService.translate(
                "costCalculation.costCalculationDetails.window.mainTab.form.parameters", locale) + ":", FontUtils
                .getDejavuBold10Dark()));

        if (!costCalculation.getField(CALCULATE_OPERATION_COSTS_MODE).equals("piecework")) {

            pdfHelper.addTableCellAsTwoColumnsTable(
                    leftPanelColumn,
                    tabInText
                            + translationService.translate(
                                    "costCalculation.costCalculationDetails.report.columnHeader.includeAdditionalTime", locale)
                            + ":",
                    (Boolean) costCalculation.getField("includeAdditionalTime") ? translationService.translate("qcadooView.true",
                            locale) : translationService.translate("qcadooView.false", locale));

            pdfHelper.addTableCellAsTwoColumnsTable(leftPanelColumn,
                    tabInText + translationService.translate("costCalculation.costCalculation.includeTPZ.label", locale) + ":",
                    (Boolean) costCalculation.getField("includeTPZ") ? translationService.translate("qcadooView.true", locale)
                            : translationService.translate("qcadooView.false", locale));
        }

        Object reportData = costCalculation.getField("calculateMaterialCostsMode");
        pdfHelper.addTableCellAsTwoColumnsTable(
                leftPanelColumn,
                tabInText
                        + translationService
                                .translate("costCalculation.costCalculation.calculateMaterialCostsMode.label", locale),
                reportData == null ? translationService.translate("qcadooView.form.blankComboBoxValue", locale)
                        : translationService.translate("costCalculation.costCalculation.calculateMaterialCostsMode.value."
                                + reportData.toString(), locale));
        reportData = costCalculation.getField(CALCULATE_OPERATION_COSTS_MODE);
        pdfHelper.addTableCellAsTwoColumnsTable(
                leftPanelColumn,
                tabInText
                        + translationService.translate("costCalculation.costCalculation.calculateOperationCostsMode.label",
                                locale),
                reportData == null ? translationService.translate("qcadooView.form.blankComboBoxValue", locale)
                        : translationService.translate("costCalculation.costCalculation.calculateOperationCostsMode.value."
                                + reportData.toString(), locale));

        reportData = costCalculation.getField("description");
        pdfHelper.addTableCellAsTwoColumnsTable(leftPanelColumn,
                tabInText + translationService.translate("costCalculation.costCalculation.description.label", locale) + ":",
                (reportData == null ? "" : reportData));

        return leftPanelColumn;
    }

    public PdfPTable addRightPanelToReport(final Entity costCalculation, final Locale locale) {
        PdfPTable rightPanelColumn = pdfHelper.createPanelTable(1);
        rightPanelColumn.addCell(new Phrase(translationService.translate(
                "costCalculation.costCalculationDetails.window.mainTab.form.technicalProductionCost", locale) + ":", FontUtils
                .getDejavuBold10Dark()));

        Object reportData = costCalculation.getField("totalMaterialCosts");
        pdfHelper.addTableCellAsTwoColumnsTable(rightPanelColumn,
                tabInText + translationService.translate("costCalculation.costCalculation.totalMaterialCosts.label", locale)
                        + ":",
                (reportData == null ? "" : numberService.format(reportData)) + " " + currencyService.getCurrencyAlphabeticCode());
        reportData = costCalculation.getField("totalMachineHourlyCosts");
        pdfHelper.addTableCellAsTwoColumnsTable(rightPanelColumn,
                tabInText + translationService.translate("costCalculation.costCalculation.totalMachineHourlyCosts.label", locale)
                        + ":",
                (reportData == null ? "" : numberService.format(reportData)) + " " + currencyService.getCurrencyAlphabeticCode());
        reportData = costCalculation.getField("totalLaborHourlyCosts");
        pdfHelper.addTableCellAsTwoColumnsTable(rightPanelColumn,
                tabInText + translationService.translate("costCalculation.costCalculation.totalLaborHourlyCosts.label", locale)
                        + ":",
                (reportData == null ? "" : numberService.format(reportData)) + " " + currencyService.getCurrencyAlphabeticCode());
        reportData = costCalculation.getField("totalTechnicalProductionCosts");
        pdfHelper.addTableCellAsTwoColumnsTable(
                rightPanelColumn,
                tabInText
                        + translationService.translate("costCalculation.costCalculation.totalTechnicalProductionCosts.label",
                                locale) + ":", (reportData == null ? "" : numberService.format(reportData)) + " "
                        + currencyService.getCurrencyAlphabeticCode());

        rightPanelColumn.addCell(new Phrase(translationService.translate(
                "costCalculation.costCalculationDetails.window.mainTab.form.overheads", locale) + ":", FontUtils
                .getDejavuBold10Dark()));
        reportData = costCalculation.getField("productionCostMargin");
        Object reportData2 = costCalculation.getField("productionCostMarginValue");
        pdfHelper.addTableCellAsTwoColumnsTable(
                rightPanelColumn,
                tabInText + translationService.translate("costCalculation.costCalculation.productionCostMargin.label", locale)
                        + ":",
                (reportData == null ? "" : numberService.format(reportData)
                        + (reportData2 == null ? "" : " %\n (" + "= " + reportData2.toString() + " "
                                + currencyService.getCurrencyAlphabeticCode() + ")")));
        reportData = costCalculation.getField("materialCostMargin");
        reportData2 = costCalculation.getField("materialCostMarginValue");
        pdfHelper.addTableCellAsTwoColumnsTable(
                rightPanelColumn,
                tabInText + translationService.translate("costCalculation.costCalculation.materialCostMargin.label", locale)
                        + ":",
                (reportData == null ? "" : numberService.format(reportData)
                        + (reportData2 == null ? "" : " %\n (" + "= " + reportData2.toString() + " "
                                + currencyService.getCurrencyAlphabeticCode() + ")")));
        reportData = costCalculation.getField("additionalOverhead");
        pdfHelper.addTableCellAsTwoColumnsTable(rightPanelColumn,
                tabInText + translationService.translate("costCalculation.costCalculation.additionalOverhead.label", locale)
                        + ":",
                (reportData == null ? "" : numberService.format(reportData)) + " " + currencyService.getCurrencyAlphabeticCode());

        reportData = costCalculation.getField("totalCostsForQuantity");
        rightPanelColumn.addCell(new Phrase(translationService.translate(
                "costCalculation.costCalculationDetails.window.mainTab.form.totalCost", locale) + ":", FontUtils
                .getDejavuBold10Dark()));

        pdfHelper.addTableCellAsTable(rightPanelColumn,
                tabInText + translationService.translate("costCalculation.costCalculation.totalCostsForQuantity.label", locale)
                        + ":",
                (reportData == null ? "" : numberService.format(reportData)) + " " + currencyService.getCurrencyAlphabeticCode(),
                FontUtils.getDejavuBold10Dark(), FontUtils.getDejavuRegular10Dark(), 2);

        reportData = costCalculation.getField("totalCostPerUnit");
        pdfHelper.addTableCellAsTable(rightPanelColumn,
                tabInText + translationService.translate("costCalculation.costCalculation.totalCostPerUnit.label", locale) + ":",
                (reportData == null ? "" : numberService.format(reportData)) + " " + currencyService.getCurrencyAlphabeticCode(),
                FontUtils.getDejavuBold10Dark(), FontUtils.getDejavuRegular10Dark(), 2);
        return rightPanelColumn;
    }

    public PdfPTable addMaterialsTable(final Entity costCalculation, final Locale locale) {

        List<String> materialsTableHeader = new ArrayList<String>();
        for (String translate : Arrays.asList(COST_CALCULATION_COST_CALCULATION_DETAILS_REPORT_COLUMN_HEADER_NUMBER,
                "costCalculation.costCalculationDetails.report.columnHeader.quantity",
                "costCalculation.costCalculationDetails.report.columnHeader.unit",
                "costCalculation.costCalculationDetails.report.columnHeader.costs",
                "costCalculation.costCalculationDetails.report.columnHeader.margin",
                "costCalculation.costCalculationDetails.report.columnHeader.totalCostsForQuantity")) {

            materialsTableHeader.add(translationService.translate(translate, locale));
        }
        PdfPTable materialsTable = pdfHelper.createTableWithHeader(materialsTableHeader.size(), materialsTableHeader, false);
        Entity technology;
        if (costCalculation.getBelongsToField(ORDER) == null) {
            technology = costCalculation.getBelongsToField(TECHNOLOGY);
        } else {
            technology = costCalculation.getBelongsToField(ORDER).getBelongsToField(TECHNOLOGY);

        }

        BigDecimal givenQty = (BigDecimal) costCalculation.getField("quantity");

        Map<Entity, BigDecimal> products = productQuantitiesService.getNeededProductQuantities(technology, givenQty, true);

        products = SortUtil.sortMapUsingComparator(products, new EntityNumberComparator());

        for (Entry<Entity, BigDecimal> product : products.entrySet()) {
            materialsTable.addCell(new Phrase(product.getKey().getStringField(NUMBER), FontUtils.getDejavuRegular9Dark()));
            materialsTable.addCell(new Phrase(numberService.format(product.getValue()), FontUtils.getDejavuRegular9Dark()));
            materialsTable.addCell(new Phrase(product.getKey().getStringField("unit"), FontUtils.getDejavuRegular9Dark()));
            BigDecimal nominalCost = (BigDecimal) product.getKey().getField("nominalCost");
            BigDecimal costForNumber = (BigDecimal) product.getKey().getField("costForNumber");
            BigDecimal totalCostPerUnit = nominalCost.divide(costForNumber, numberService.getMathContext());
            BigDecimal costs = product.getValue().multiply(totalCostPerUnit, numberService.getMathContext());
            materialsTable.addCell(new Phrase(numberService.format(costs), FontUtils.getDejavuRegular9Dark()));
            BigDecimal margin = (BigDecimal) costCalculation.getField("materialCostMargin");
            if (margin == null) {
                materialsTable.addCell(new Phrase("", FontUtils.getDejavuRegular9Dark()));
                materialsTable.addCell(new Phrase("", FontUtils.getDejavuRegular9Dark()));
            } else {
                MathContext mc = numberService.getMathContext();
                BigDecimal toAdd = costs.multiply(margin.divide(new BigDecimal(100), mc), mc);
                materialsTable.addCell(new Phrase(numberService.format(toAdd), FontUtils.getDejavuRegular9Dark()));
                BigDecimal totalCostsForQuantity = costs.add(toAdd, mc);
                materialsTable
                        .addCell(new Phrase(numberService.format(totalCostsForQuantity), FontUtils.getDejavuRegular9Dark()));
            }
            materialsTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
        }
        return materialsTable;
    }

    public PdfPTable addOptionTablePrintCostNormsOfMaterials(final Entity costCalculation, final Locale locale) {
        List<String> optionTableHeader = new ArrayList<String>();

        String localeType = null;
        String costMode = "";
        if ("01nominal".equals(costCalculation.getField("calculateMaterialCostsMode"))) {
            localeType = "costCalculation.costCalculationDetails.report.columnHeader.nominalCost";
            costMode = "nominalCost";
        } else if ("02average".equals(costCalculation.getField("calculateMaterialCostsMode"))) {
            localeType = "costCalculation.costCalculationDetails.report.columnHeader.averageCost";
            costMode = "averageCost";
        } else if ("03lastPurchase".equals(costCalculation.getField("calculateMaterialCostsMode"))) {
            localeType = "costCalculation.costCalculationDetails.report.columnHeader.lastPurchaseCost";
            costMode = "lastPurchaseCost";
        } else if ("04costForOrder".equals(costCalculation.getField("calculateMaterialCostsMode"))) {
            localeType = "costCalculation.costCalculationDetails.report.columnHeader.costForOrder";
            costMode = "costForOrder";
        }

        for (String translate : Arrays.asList(COST_CALCULATION_COST_CALCULATION_DETAILS_REPORT_COLUMN_HEADER_NUMBER,
                COST_CALCULATION_COST_CALCULATION_DETAILS_REPORT_COLUMN_HEADER_NAME, localeType

        )) {
            optionTableHeader.add(translationService.translate(translate, locale));
        }

        int[] columnWidth = { 20, 20, 20 };

        Entity technology;
        if (costCalculation.getBelongsToField(ORDER) == null) {
            technology = costCalculation.getBelongsToField(TECHNOLOGY);
        } else {
            technology = costCalculation.getBelongsToField(ORDER).getBelongsToField(TECHNOLOGY);

        }

        BigDecimal givenQty = (BigDecimal) costCalculation.getField("quantity");
        Map<Entity, BigDecimal> products = productQuantitiesService.getNeededProductQuantities(technology, givenQty, true);

        products = SortUtil.sortMapUsingComparator(products, new EntityNumberComparator());

        PdfPTable PrintCostNormsOfMaterialTable = pdfHelper.createTableWithHeader(optionTableHeader.size(), optionTableHeader,
                false, columnWidth);

        for (Entry<Entity, BigDecimal> product : products.entrySet()) {
            PrintCostNormsOfMaterialTable.addCell(new Phrase(product.getKey().getStringField(NUMBER), FontUtils
                    .getDejavuRegular9Dark()));
            PrintCostNormsOfMaterialTable.addCell(new Phrase(product.getKey().getStringField("name"), FontUtils
                    .getDejavuRegular9Dark()));

            BigDecimal toDisplay = (BigDecimal) product.getKey().getField(costMode);
            BigDecimal quantity = (BigDecimal) product.getValue();
            String unit = (String) product.getKey().getStringField("unit");

            PrintCostNormsOfMaterialTable.addCell(new Phrase(toDisplay + " / " + quantity + " " + unit, FontUtils
                    .getDejavuRegular9Dark()));

        }
        return PrintCostNormsOfMaterialTable;

    }

    public void addOptionTablePrintOperationNormsHourly(final Document document, final Entity costCalculation, final Locale locale)
            throws DocumentException {
        List<String> optionTableHeader = new ArrayList<String>();
        for (String translate : Arrays.asList(COST_CALCULATION_COST_CALCULATION_DETAILS_REPORT_COLUMN_HEADER_NUMBER,
                COST_CALCULATION_COST_CALCULATION_DETAILS_REPORT_COLUMN_HEADER_NAME)) {
            optionTableHeader.add(translationService.translate(translate, locale));
        }

        List<Entity> calculationOperationComponents = newLinkedList(costCalculation
                .getTreeField(CALCULATION_OPERATION_COMPONENTS));

        Collections.sort(calculationOperationComponents, treeNumberingService.getTreeNodesNumberComparator());

        for (Entity calculationOperationComponent : calculationOperationComponents) {
            PdfPTable panelTableHeader = pdfHelper.createPanelTable(2);
            PdfPTable panelTableContent = pdfHelper.createPanelTable(2);
            panelTableHeader.setSpacingBefore(20);
            panelTableContent.getDefaultCell().setBackgroundColor(null);
            panelTableHeader.addCell(new Phrase(translationService.translate(
                    COST_CALCULATION_COST_CALCULATION_DETAILS_REPORT_COLUMN_HEADER_NUMBER, locale)
                    + ": "
                    + calculationOperationComponent.getBelongsToField("operation").getStringField(NUMBER), FontUtils
                    .getDejavuRegular9Dark()));

            panelTableHeader.addCell(new Phrase(translationService.translate(
                    COST_CALCULATION_COST_CALCULATION_DETAILS_REPORT_COLUMN_HEADER_NAME, locale)
                    + ": "
                    + calculationOperationComponent.getBelongsToField("operation").getStringField("name"), FontUtils
                    .getDejavuRegular9Dark()));

            pdfHelper
                    .addTableCellAsTwoColumnsTable(
                            panelTableContent,
                            translationService.translate(
                                    "costCalculation.costCalculationDetails.report.columnHeader.productionSetUpTime.label",
                                    locale)
                                    + ":", calculationOperationComponent.getField("tpz"));

            pdfHelper.addTableCellAsTwoColumnsTable(
                    panelTableContent,
                    translationService.translate(
                            "costCalculation.costCalculationDetails.report.columnHeader.machineUtilization.label", locale) + ":",
                    calculationOperationComponent.getField("machineUtilization"));

            pdfHelper.addTableCellAsTwoColumnsTable(
                    panelTableContent,
                    translationService.translate(
                            "costCalculation.costCalculationDetails.report.columnHeader.productionTimeForOneCycle.label", locale)
                            + ":", calculationOperationComponent.getField("tj"));

            pdfHelper.addTableCellAsTwoColumnsTable(
                    panelTableContent,
                    translationService.translate(
                            "costCalculation.costCalculationDetails.report.columnHeader.laborUtilization.label", locale) + ":",
                    calculationOperationComponent.getField("laborUtilization"));

            pdfHelper.addTableCellAsTwoColumnsTable(
                    panelTableContent,
                    translationService.translate(
                            "costCalculation.costCalculationDetails.report.columnHeader.additionalTime.label", locale) + ":",
                    calculationOperationComponent.getField("timeNextOperation"));

            pdfHelper.addTableCellAsTwoColumnsTable(
                    panelTableContent,
                    translationService.translate(
                            "costCalculation.costCalculationDetails.report.columnHeader.machineHourlyCost.label", locale) + ":",
                    calculationOperationComponent.getField("machineHourlyCost"));

            pdfHelper.addTableCellAsTwoColumnsTable(panelTableContent, null, null);

            pdfHelper.addTableCellAsTwoColumnsTable(
                    panelTableContent,
                    translationService.translate(
                            "costCalculation.costCalculationDetails.report.columnHeader.laborHourlyCost.label", locale) + ":",
                    calculationOperationComponent.getField("laborHourlyCost"));

            document.add(panelTableHeader);
            document.add(panelTableContent);

        }

    }

    public PdfPTable addOptionTablePrintOperationNormsPiecework(final Entity costCalculation, final Locale locale) {
        List<String> optionTableHeader = new ArrayList<String>();

        for (String translate : Arrays.asList(COST_CALCULATION_COST_CALCULATION_DETAILS_REPORT_COLUMN_HEADER_NUMBER,
                COST_CALCULATION_COST_CALCULATION_DETAILS_REPORT_COLUMN_HEADER_NAME,
                "costCalculation.costCalculationDetails.report.columnHeader.pieceworkCost",
                "costCalculation.costCalculationDetails.report.columnHeader.forNumberOfOperations"

        )) {
            optionTableHeader.add(translationService.translate(translate, locale));
        }

        int[] columnWidth = { 20, 20, 20 };

        List<Entity> calculationOperationComponents = newLinkedList(costCalculation
                .getTreeField(CALCULATION_OPERATION_COMPONENTS));

        Collections.sort(calculationOperationComponents, treeNumberingService.getTreeNodesNumberComparator());

        PdfPTable printCostNormsOfMaterialTable2 = pdfHelper.createTableWithHeader(optionTableHeader.size(), optionTableHeader,
                false, columnWidth);

        for (Entity calculationOperationComponent : calculationOperationComponents) {

            printCostNormsOfMaterialTable2.addCell(new Phrase(calculationOperationComponent.getBelongsToField("operation")
                    .getStringField(NUMBER), FontUtils.getDejavuRegular9Dark()));

            printCostNormsOfMaterialTable2.addCell(new Phrase(calculationOperationComponent.getBelongsToField("operation")
                    .getStringField("name"), FontUtils.getDejavuRegular9Dark()));

            printCostNormsOfMaterialTable2.addCell(new Phrase(calculationOperationComponent.getField("pieceworkCost").toString(),
                    FontUtils.getDejavuRegular9Dark()));

            printCostNormsOfMaterialTable2.addCell(new Phrase(calculationOperationComponent.getField("numberOfOperations")
                    .toString(), FontUtils.getDejavuRegular9Dark()));
        }

        return printCostNormsOfMaterialTable2;

    }

    private PdfPTable addTableAboutHourlyCost(final Entity costCalculation, final Locale locale) {
        List<String> operationsTableHeader = new ArrayList<String>();

        for (String translate : Arrays.asList("costCalculation.costCalculationDetails.report.columnHeader.level",
                COST_CALCULATION_COST_CALCULATION_DETAILS_REPORT_COLUMN_HEADER_NUMBER,
                "costCalculation.costCalculationDetails.report.columnHeader.duration",
                "costCalculation.costCalculationDetails.report.columnHeader.machCosts",
                "costCalculation.costCalculationDetails.report.columnHeader.labCosts",
                "costCalculation.costCalculationDetails.report.columnHeader.operationCost",
                "costCalculation.costCalculationDetails.report.columnHeader.margin",
                "costCalculation.costCalculationDetails.report.columnHeader.totalCostsForQuantity")) {
            operationsTableHeader.add(translationService.translate(translate, locale));
        }

        int[] columnWitdh = { 20, 20, 20, 20, 20, 20, 20, 20, 20 };
        List<Entity> calculationOperationComponents = newLinkedList(costCalculation
                .getTreeField(CALCULATION_OPERATION_COMPONENTS));

        Collections.sort(calculationOperationComponents, treeNumberingService.getTreeNodesNumberComparator());

        PdfPTable operationsTable = pdfHelper.createTableWithHeader(operationsTableHeader.size(), operationsTableHeader, false,
                columnWitdh);

        if (calculationOperationComponents != null && !calculationOperationComponents.isEmpty()) {
            for (Entity calculationOperationComponent : calculationOperationComponents) {
                operationsTable.addCell(new Phrase(calculationOperationComponent.getField("nodeNumber").toString(), FontUtils
                        .getDejavuRegular9Dark()));

                operationsTable.addCell(new Phrase(calculationOperationComponent.getBelongsToField("operation").getStringField(
                        NUMBER), FontUtils.getDejavuRegular9Dark()));

                String duration = timeConverterService.convertTimeToString((Integer) calculationOperationComponent
                        .getField("duration"));

                Integer durationMs = (Integer) calculationOperationComponent.getField("duration");
                BigDecimal milisecondsInHour = BigDecimal.valueOf(3600);
                BigDecimal durationHours = BigDecimal.valueOf(durationMs).divide(milisecondsInHour,
                        numberService.getMathContext());
                BigDecimal machineUtilization = (BigDecimal) calculationOperationComponent.getField("machineUtilization");
                BigDecimal machineHourlyCost = (BigDecimal) calculationOperationComponent.getField("machineHourlyCost");
                BigDecimal durationOfMachine = (BigDecimal) machineUtilization.multiply(machineHourlyCost)
                        .multiply(durationHours);
                String durationOfMachineToString = numberService.format(durationOfMachine);

                BigDecimal laborUtilization = (BigDecimal) calculationOperationComponent.getField("laborUtilization");
                BigDecimal laborHourlyCost = (BigDecimal) calculationOperationComponent.getField("laborHourlyCost");
                BigDecimal durationOfLabor = (BigDecimal) laborUtilization.multiply(laborHourlyCost).multiply(durationHours);
                String durationOfLaborToString = numberService.format(durationOfLabor);

                operationsTable.addCell(new Phrase(duration, FontUtils.getDejavuRegular9Dark()));
                operationsTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
                operationsTable.addCell(new Phrase(durationOfMachineToString, FontUtils.getDejavuRegular9Dark()));

                operationsTable.addCell(new Phrase(durationOfLaborToString, FontUtils.getDejavuRegular9Dark()));
                operationsTable.addCell(new Phrase(numberService.format(calculationOperationComponent.getField("operationCost")),
                        FontUtils.getDejavuRegular9Dark()));
                operationsTable.addCell(new Phrase(numberService.format(calculationOperationComponent
                        .getField("operationMarginCost")), FontUtils.getDejavuRegular9Dark()));
                operationsTable.addCell(new Phrase(numberService.format(calculationOperationComponent
                        .getField("totalOperationCost")), FontUtils.getDejavuRegular9Dark()));
                operationsTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            }
        }
        return operationsTable;
    }

    private PdfPTable addTableAboutPieceworkCost(final Entity costCalculation, final Locale locale) {
        List<String> operationsTableHeader = new ArrayList<String>();

        for (String translate : Arrays.asList("costCalculation.costCalculationDetails.report.columnHeader.level",
                COST_CALCULATION_COST_CALCULATION_DETAILS_REPORT_COLUMN_HEADER_NUMBER,
                "costCalculation.costCalculationDetails.report.columnHeader.pieces",
                "costCalculation.costCalculationDetails.report.columnHeader.operationCost",
                "costCalculation.costCalculationDetails.report.columnHeader.margin",
                "costCalculation.costCalculationDetails.report.columnHeader.totalCostsForQuantity")) {
            operationsTableHeader.add(translationService.translate(translate, locale));
        }
        List<Entity> calculationOperationComponents = costCalculation.getTreeField(CALCULATION_OPERATION_COMPONENTS);

        PdfPTable operationsTable = pdfHelper.createTableWithHeader(operationsTableHeader.size(), operationsTableHeader, false);

        if (!calculationOperationComponents.isEmpty()) {
            for (Entity calculationOperationComponent : calculationOperationComponents) {
                operationsTable.addCell(new Phrase(calculationOperationComponent.getField("nodeNumber").toString(), FontUtils
                        .getDejavuRegular9Dark()));
                operationsTable.addCell(new Phrase(calculationOperationComponent.getBelongsToField("operation").getStringField(
                        NUMBER), FontUtils.getDejavuRegular9Dark()));
                operationsTable.addCell(new Phrase(numberService.format(calculationOperationComponent.getField("pieces")),
                        FontUtils.getDejavuRegular9Dark()));
                operationsTable.addCell(new Phrase(numberService.format(calculationOperationComponent.getField("operationCost")),
                        FontUtils.getDejavuRegular9Dark()));
                operationsTable.addCell(new Phrase(numberService.format(calculationOperationComponent
                        .getField("operationMarginCost")), FontUtils.getDejavuRegular9Dark()));
                operationsTable.addCell(new Phrase(numberService.format(calculationOperationComponent
                        .getField("totalOperationCost")), FontUtils.getDejavuRegular9Dark()));
                operationsTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            }
        }
        return operationsTable;
    }
}
