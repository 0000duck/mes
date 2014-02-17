/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.2.0
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
package com.qcadoo.mes.workPlans.print;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.mes.basic.ParameterService;
import com.qcadoo.mes.columnExtension.constants.ColumnAlignment;
import com.qcadoo.mes.workPlans.constants.ColumnForProductsFields;
import com.qcadoo.mes.workPlans.constants.OrderSorting;
import com.qcadoo.mes.workPlans.constants.ParameterFieldsWP;
import com.qcadoo.mes.workPlans.constants.WorkPlanFields;
import com.qcadoo.mes.workPlans.constants.WorkPlanType;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchOrders;
import com.qcadoo.model.api.utils.EntityTreeUtilsService;
import com.qcadoo.report.api.FontUtils;
import com.qcadoo.report.api.PrioritizedString;
import com.qcadoo.report.api.pdf.HeaderAlignment;
import com.qcadoo.report.api.pdf.PdfDocumentService;
import com.qcadoo.report.api.pdf.PdfHelper;

@Service
public class WorkPlanPdfService extends PdfDocumentService {

    private static final String OPERATION_LITERAL = "operation";

    private static final String L_NAME = "name";

    private static final String NUMBER_LITERAL = "number";

    private static final String PRODUCT_LITERAL = "product";

    private static final String PLANED_QUANTITY = "plannedQuantity";

    @Autowired
    private EntityTreeUtilsService entityTreeUtilsService;

    @Autowired
    private ColumnFetcher columnFetcher;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private PdfHelper pdfHelper;

    @Autowired
    private ParameterService parameterService;

    enum ProductDirection {
        IN, OUT;
    }

    @Override
    public String getReportTitle(final Locale locale) {
        return translationService.translate("workPlans.workPlan.report.title", locale);
    }

    @Override
    public void buildPdfContent(final Document document, final Entity workPlan, final Locale locale) throws DocumentException {
        addMainHeader(document, workPlan, locale);
        if (!workPlan.getBooleanField("dontPrintOrdersInWorkPlans")) {
            addOrdersTable(document, workPlan, locale);
        }
        addOperations(document, workPlan, locale);
    }

    private void addOrdersTable(final Document document, final Entity workPlan, final Locale locale) throws DocumentException {
        List<Entity> columns = fetchOrderColumnDefinitions(workPlan);

        if (!columns.isEmpty()) {
            PdfPTable orderTable = pdfHelper.createTableWithHeader(columns.size(),
                    prepareOrdersTableHeader(document, columns, locale), false, prepareHeaderAlignment(columns, locale));

            List<Entity> orders = workPlan.getManyToManyField("orders");

            Map<Entity, Map<String, String>> columnValues = columnFetcher.getOrderColumnValues(orders);

            for (Entity order : orders) {
                for (Entity column : columns) {
                    String columnIdentifier = column.getStringField("identifier");
                    String value = columnValues.get(order).get(columnIdentifier);

                    alignColumn(orderTable.getDefaultCell(), ColumnAlignment.parseString(column.getStringField("alignment")));

                    orderTable.addCell(new Phrase(value, FontUtils.getDejavuRegular7Dark()));
                }
            }

            document.add(orderTable);
            document.add(Chunk.NEWLINE);

        }
    }

    private Map<String, HeaderAlignment> prepareHeaderAlignment(List<Entity> columns, Locale locale) {
        Map<String, HeaderAlignment> alignments = Maps.newHashMap();
        for (Entity column : columns) {
            String alignment = column.getStringField("alignment");
            HeaderAlignment headerAlignment = HeaderAlignment.RIGHT;
            if (ColumnAlignment.LEFT.equals(ColumnAlignment.parseString(alignment))) {
                headerAlignment = HeaderAlignment.LEFT;
            } else if (ColumnAlignment.RIGHT.equals(ColumnAlignment.parseString(alignment))) {
                headerAlignment = HeaderAlignment.RIGHT;
            }
            alignments.put(prepareHeaderTranslation(column.getStringField(L_NAME), locale), headerAlignment);
        }
        return alignments;
    }

    private String prepareHeaderTranslation(final String name, final Locale locale) {
        String translatedName = translationService.translate(name, locale);
        return translatedName;
    }

    private Map<Long, Entity> buildEntityId2EntityMap(final Iterable<Entity> entities) {
        final Map<Long, Entity> entityId2EntityMap = Maps.newHashMap();
        for (final Entity entity : entities) {
            entityId2EntityMap.put(entity.getId(), entity);
        }
        return entityId2EntityMap;
    }

    private void addOperations(final Document document, final Entity workPlan, final Locale locale) throws DocumentException {
        final List<Entity> orders = workPlan.getManyToManyField("orders");
        final boolean haveManyOrders = orders.size() > 1;
        final Map<Long, Entity> orderId2Order = buildEntityId2EntityMap(orders);
        final Map<Long, Map<Entity, Map<String, String>>> orderId2columnValues = columnFetcher.getColumnValues(orders);

        for (final Entry<Long, Map<PrioritizedString, List<Entity>>> orderId2OpComponentsMapEntry : getOrderIdToOperationComponentsMap(
                workPlan, locale).entrySet()) {
            final Long orderId = orderId2OpComponentsMapEntry.getKey();
            final Entity order = orderId2Order.get(orderId);
            addOperationsForSpecifiedOrder(document, workPlan, orderId2OpComponentsMapEntry.getValue(),
                    orderId2columnValues.get(orderId), order, haveManyOrders, locale);
        }
    }

    private void addOperationsForSpecifiedOrder(final Document document, final Entity workPlan,
            final Map<PrioritizedString, List<Entity>> orderOpComponentsMap, final Map<Entity, Map<String, String>> columnValues,
            final Entity order, final boolean haveManyOrders, final Locale locale) throws DocumentException {
        Entity parameter = parameterService.getParameter();

        for (Entry<PrioritizedString, List<Entity>> entry : orderOpComponentsMap.entrySet()) {
            if (!parameter.getBooleanField(ParameterFieldsWP.PRINT_OPERATION_AT_FIRST_PAGE_IN_WORK_PLANS)) {

                document.newPage();
            }
            document.add(new Paragraph(entry.getKey().getString(), FontUtils.getDejavuBold11Dark()));
            int count = 0;
            for (Entity operationComponent : entry.getValue()) {
                count++;
                PdfPTable operationTable = pdfHelper.createPanelTable(3);
                addOperationInfoToTheOperationHeader(operationTable, operationComponent, locale);
                if (haveManyOrders && isOrderInfoEnabled(operationComponent)) {
                    addOrderInfoToTheOperationHeader(operationTable, order, locale);
                }

                if (isWorkstationInfoEnabled(operationComponent)) {
                    addWorkstationInfoToTheOperationHeader(operationTable, operationComponent, locale);
                }

                operationTable.setSpacingAfter(18);
                operationTable.setSpacingBefore(9);
                document.add(operationTable);

                if (isCommentEnabled(operationComponent)) {
                    addOperationComment(document, operationComponent, locale);
                }

                if (isOutputProductTableEnabled(operationComponent)) {
                    addOutProductsSeries(document, workPlan, columnValues, operationComponent, locale);
                }

                if (isInputProductTableEnabled(operationComponent)) {
                    addInProductsSeries(document, workPlan, columnValues, operationComponent, locale);
                }

                addAdditionalFields(document, operationComponent, locale);
                if (count != entry.getValue().size()) {

                    if (!parameter.getBooleanField(ParameterFieldsWP.PRINT_OPERATION_AT_FIRST_PAGE_IN_WORK_PLANS)) {

                        document.add(Chunk.NEXTPAGE);
                    }
                }
            }
        }
    }

    void addOperationComment(final Document document, final Entity operationComponent, final Locale locale)
            throws DocumentException {
        PdfPTable table = pdfHelper.createPanelTable(1);
        table.getDefaultCell().setBackgroundColor(null);

        String commentLabel = translationService.translate("workPlans.workPlan.report.operation.comment", locale);
        String commentContent = operationComponent.getStringField("comment");

        if (commentContent == null) {
            return;
        }

        pdfHelper.addTableCellAsOneColumnTable(table, commentLabel, commentContent);

        table.setSpacingAfter(18);
        table.setSpacingBefore(9);
        document.add(table);
    }

    void addOperationInfoToTheOperationHeader(final PdfPTable operationTable, final Entity operationComponent, final Locale locale) {
        String operationLevel = operationComponent.getStringField("nodeNumber");
        pdfHelper.addTableCellAsOneColumnTable(operationTable,
                translationService.translate("workPlans.workPlan.report.operation.level", locale), operationLevel);

        String operationName = operationComponent.getBelongsToField(OPERATION_LITERAL).getStringField(L_NAME);
        pdfHelper.addTableCellAsOneColumnTable(operationTable,
                translationService.translate("workPlans.workPlan.report.operation.name", locale), operationName);

        String operationNumber = operationComponent.getBelongsToField(OPERATION_LITERAL).getStringField(NUMBER_LITERAL);
        pdfHelper.addTableCellAsOneColumnTable(operationTable,
                translationService.translate("workPlans.workPlan.report.operation.number", locale), operationNumber);
    }

    void addWorkstationInfoToTheOperationHeader(final PdfPTable operationTable, final Entity operationComponent,
            final Locale locale) {
        Entity workstationType = operationComponent.getBelongsToField(OPERATION_LITERAL).getBelongsToField("workstationType");
        String workstationTypeName = "";
        String divisionName = "";
        String supervisorName = "";
        String divisionLabel = "";
        String supervisorLabel = "";

        if (workstationType != null) {
            workstationTypeName = workstationType.getStringField(L_NAME);

            Entity division = workstationType.getBelongsToField("division");
            if (division != null) {
                divisionName = division.getStringField(L_NAME);
                divisionLabel = translationService.translate("workPlans.workPlan.report.operation.division", locale);
                Entity supervisor = division.getBelongsToField("supervisor");
                if (supervisor != null) {
                    supervisorName = supervisor.getStringField(L_NAME) + " " + supervisor.getStringField("surname");
                    supervisorLabel = translationService.translate("workPlans.workPlan.report.operation.supervisor", locale);
                }
            }

            pdfHelper.addTableCellAsOneColumnTable(operationTable,
                    translationService.translate("workPlans.workPlan.report.operation.workstationType", locale),
                    workstationTypeName);
            pdfHelper.addTableCellAsOneColumnTable(operationTable, divisionLabel, divisionName);
            pdfHelper.addTableCellAsOneColumnTable(operationTable, supervisorLabel, supervisorName);
        }
    }

    void addOrderInfoToTheOperationHeader(final PdfPTable operationTable, final Entity order, final Locale locale) {
        Entity technology = order.getBelongsToField("technology");
        String technologyString = null;
        if (technology != null) {
            technologyString = technology.getStringField(L_NAME);
        }
        pdfHelper.addTableCellAsOneColumnTable(operationTable,
                translationService.translate("workPlans.workPlan.report.operation.technology", locale), technologyString);

        String orderName = order.getStringField(L_NAME);
        pdfHelper.addTableCellAsOneColumnTable(operationTable,
                translationService.translate("workPlans.workPlan.report.operation.orderName", locale), orderName);

        String orderNumber = order.getStringField(NUMBER_LITERAL);
        pdfHelper.addTableCellAsOneColumnTable(operationTable,
                translationService.translate("workPlans.workPlan.report.operation.orderNumber", locale), orderNumber);
    }

    PrioritizedString generateOperationSectionTitle(final Entity workPlan, final Entity technology,
            final Entity operationComponent, final Locale locale) {
        String type = workPlan.getStringField("type");

        PrioritizedString title = null;

        if (WorkPlanType.NO_DISTINCTION.getStringValue().equals(type)) {
            title = new PrioritizedString(translationService.translate("workPlans.workPlan.report.title.noDistinction", locale));
        } else if (WorkPlanType.BY_END_PRODUCT.getStringValue().equals(type)) {
            Entity endProduct = technology.getBelongsToField(PRODUCT_LITERAL);

            String prefix = translationService.translate("workPlans.workPlan.report.title.byEndProduct", locale);
            String endProductName = endProduct.getStringField(L_NAME);
            title = new PrioritizedString(prefix + " " + endProductName);
        } else if (WorkPlanType.BY_WORKSTATION_TYPE.getStringValue().equals(type)) {
            Entity workstation = operationComponent.getBelongsToField(OPERATION_LITERAL).getBelongsToField("workstationType");

            if (workstation == null) {
                title = new PrioritizedString(translationService.translate("workPlans.workPlan.report.title.noWorkstationType",
                        locale), 1);
            } else {
                String suffix = translationService.translate("workPlans.workPlan.report.title.byWorkstationType", locale);
                String workstationName = workstation.getStringField(L_NAME);
                title = new PrioritizedString(suffix + " " + workstationName);
            }
        } else if (WorkPlanType.BY_DIVISION.getStringValue().equals(type)) {
            Entity workstation = operationComponent.getBelongsToField(OPERATION_LITERAL).getBelongsToField("workstationType");

            if (workstation == null) {
                title = new PrioritizedString(translationService.translate("workPlans.workPlan.report.title.noDivision", locale),
                        1);
            } else {
                Entity division = workstation.getBelongsToField("division");

                if (division == null) {
                    title = new PrioritizedString(translationService.translate("workPlans.workPlan.report.title.noDivision",
                            locale), 1);
                } else {
                    String suffix = translationService.translate("workPlans.workPlan.report.title.byDivision", locale);
                    String divisionName = division.getStringField(L_NAME);
                    title = new PrioritizedString(suffix + " " + divisionName);
                }
            }
        }

        return title;
    }

    private Map<PrioritizedString, List<Entity>> fetchOperationComponentsFromTechnology(final Entity technology,
            final Entity workPlan, final Entity order, final Locale locale) {
        List<Entity> operationComponents = entityTreeUtilsService.getSortedEntities(technology
                .getTreeField("operationComponents"));
        final Map<PrioritizedString, List<Entity>> opComps = Maps.newTreeMap();
        for (Entity operationComponent : operationComponents) {
            if ("referenceTechnology".equals(operationComponent.getStringField("entityType"))) {
                Entity refTech = operationComponent.getBelongsToField("referenceTechnology");
                opComps.putAll(fetchOperationComponentsFromTechnology(refTech, workPlan, order, locale));
                continue;
            }

            PrioritizedString title = generateOperationSectionTitle(workPlan, technology, operationComponent, locale);

            if (title == null) {
                throw new IllegalStateException("undefined workplan type");
            }

            if (!opComps.containsKey(title)) {
                opComps.put(title, new ArrayList<Entity>());
            }

            opComps.get(title).add(operationComponent);
        }
        return opComps;
    }

    private Map<Long, Map<PrioritizedString, List<Entity>>> getOrderIdToOperationComponentsMap(final Entity workPlan,
            final Locale locale) {
        final Map<Long, Map<PrioritizedString, List<Entity>>> order2opComps = Maps.newTreeMap();

        List<Entity> orders = workPlan.getManyToManyField("orders");

        for (Entity order : orders) {
            Entity technology = order.getBelongsToField("technology");
            if (technology == null) {
                continue;
            }
            order2opComps.put(order.getId(), fetchOperationComponentsFromTechnology(technology, workPlan, order, locale));
        }

        return order2opComps;
    }

    void addProductsSeries(final List<Entity> productComponentsArg, final Document document, final Entity workPlan,
            final Map<Entity, Map<String, String>> columnValues, final Entity operationComponent,
            final ProductDirection direction, final Locale locale, final boolean sortColumnValues) throws DocumentException {
        if (productComponentsArg.isEmpty()) {
            return;
        }

        // TODO mici, I couldnt sort productComponents without making a new linkedList out of it
        List<Entity> productComponents = Lists.newLinkedList(productComponentsArg);
        Collections.sort(productComponents, new OperationProductComponentComparator());

        List<Entity> columns = fetchColumnDefinitions(direction, operationComponent);

        if (columns.isEmpty()) {
            return;
        }

        PdfPTable table = pdfHelper.createTableWithHeader(columns.size(),
                prepareProductsTableHeader(document, columns, direction, locale), false, prepareHeaderAlignment(columns, locale));
        if (sortColumnValues) {
            productComponents = getSortedProductByColumn(columns, columnValues, productComponents, workPlan);
        }
        for (Entity productComponent : productComponents) {
            for (Entity column : columns) {
                String columnIdentifier = column.getStringField("identifier");
                String value = columnValues.get(productComponent).get(columnIdentifier);

                alignColumn(table.getDefaultCell(), ColumnAlignment.parseString(column.getStringField("alignment")));
                table.addCell(new Phrase(value, FontUtils.getDejavuRegular7Dark()));
            }
        }

        table.setSpacingAfter(18);
        table.setSpacingBefore(9);

        document.add(table);
    }

    private void alignColumn(final PdfPCell cell, final ColumnAlignment columnAlignment) {
        if (ColumnAlignment.LEFT.equals(columnAlignment)) {
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        } else if (ColumnAlignment.RIGHT.equals(columnAlignment)) {
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }
    }

    void addInProductsSeries(final Document document, final Entity workPlan, final Map<Entity, Map<String, String>> columnValues,
            final Entity operationComponent, final Locale locale) throws DocumentException {

        List<Entity> productComponents = operationComponent.getHasManyField("operationProductInComponents");

        addProductsSeries(productComponents, document, workPlan, columnValues, operationComponent, ProductDirection.IN, locale,
                true);
    }

    void addOutProductsSeries(final Document document, final Entity workPlan,
            final Map<Entity, Map<String, String>> columnValues, final Entity operationComponent, final Locale locale)
            throws DocumentException {

        List<Entity> productComponents = operationComponent.getHasManyField("operationProductOutComponents");

        addProductsSeries(productComponents, document, workPlan, columnValues, operationComponent, ProductDirection.OUT, locale,
                false);
    }

    void addAdditionalFields(final Document document, final Entity operationComponent, final Locale locale)
            throws DocumentException {
        String imagePath;
        try {
            imagePath = getImagePathFromDD(operationComponent);
        } catch (NoSuchElementException e) {
            return;
        }

        String titleString = translationService.translate("workPlans.workPlan.report.additionalFields", locale);
        document.add(new Paragraph(titleString, FontUtils.getDejavuBold10Dark()));

        pdfHelper.addImage(document, imagePath);

        document.add(Chunk.NEWLINE);
    }

    void addMainHeader(final Document document, final Entity entity, final Locale locale) throws DocumentException {
        String documenTitle = translationService.translate("workPlans.workPlan.report.title", locale);
        String documentAuthor = translationService.translate("qcadooReport.commons.generatedBy.label", locale);
        pdfHelper.addDocumentHeader(document, entity.getField(L_NAME).toString(), documenTitle, documentAuthor,
                (Date) entity.getField("date"));
    }

    private List<Entity> fetchOrderColumnDefinitions(final Entity workPlan) {
        List<Entity> columns = new LinkedList<Entity>();

        List<Entity> columnComponents = workPlan.getHasManyField("workPlanOrderColumns").find()
                .addOrder(SearchOrders.asc("succession")).list().getEntities();

        for (Entity columnComponent : columnComponents) {
            Entity columnDefinition = columnComponent.getBelongsToField("columnForOrders");

            columns.add(columnDefinition);
        }

        return columns;
    }

    private List<Entity> fetchColumnDefinitions(final ProductDirection direction, final Entity operationComponent) {
        List<Entity> columns = new LinkedList<Entity>();

        String columnDefinitionModel = null;

        List<Entity> columnComponents;
        if (ProductDirection.IN.equals(direction)) {
            columnComponents = operationComponent.getHasManyField("technologyOperationInputColumns").find()
                    .addOrder(SearchOrders.asc("succession")).list().getEntities();
            columnDefinitionModel = "columnForInputProducts";
        } else if (ProductDirection.OUT.equals(direction)) {
            columnComponents = operationComponent.getHasManyField("technologyOperationOutputColumns").find()
                    .addOrder(SearchOrders.asc("succession")).list().getEntities();
            columnDefinitionModel = "columnForOutputProducts";
        } else {
            throw new IllegalStateException("Wrong product direction");
        }

        for (Entity columnComponent : columnComponents) {
            Entity columnDefinition = columnComponent.getBelongsToField(columnDefinitionModel);

            columns.add(columnDefinition);
        }

        return columns;
    }

    List<String> prepareProductsTableHeader(final Document document, final List<Entity> columns,
            final ProductDirection direction, final Locale locale) throws DocumentException {
        String title;
        if (ProductDirection.IN.equals(direction)) {
            title = translationService.translate("workPlans.workPlan.report.productsInTable", locale);
        } else if (ProductDirection.OUT.equals(direction)) {
            title = translationService.translate("workPlans.workPlan.report.productsOutTable", locale);
        } else {
            throw new IllegalStateException("unknown product direction");
        }

        document.add(new Paragraph(title, FontUtils.getDejavuBold10Dark()));

        List<String> header = new ArrayList<String>();

        for (Entity column : columns) {
            String nameKey = column.getStringField(L_NAME);
            header.add(translationService.translate(nameKey, locale));
        }

        return header;
    }

    List<String> prepareOrdersTableHeader(final Document document, final List<Entity> columns, final Locale locale)
            throws DocumentException {
        document.add(new Paragraph(translationService.translate("workPlans.workPlan.report.ordersTable", locale), FontUtils
                .getDejavuBold11Dark()));

        List<String> orderHeader = new ArrayList<String>();

        for (Entity column : columns) {
            String nameKey = column.getStringField(L_NAME);
            orderHeader.add(translationService.translate(nameKey, locale));
        }

        return orderHeader;
    }

    String getImagePathFromDD(final Entity operationComponent) {
        String imagePath = operationComponent.getStringField("imageUrlInWorkPlan");

        if (imagePath == null) {
            throw new NoSuchElementException("no image");
        } else {
            return imagePath;
        }
    }

    public boolean isCommentEnabled(final Entity operationComponent) {
        return !operationComponent.getBooleanField("hideDescriptionInWorkPlans");
    }

    public boolean isOrderInfoEnabled(final Entity operationComponent) {
        return !operationComponent.getBooleanField("hideTechnologyAndOrderInWorkPlans");
    }

    public boolean isWorkstationInfoEnabled(final Entity operationComponent) {
        return !operationComponent.getBooleanField("hideDetailsInWorkPlans");
    }

    public boolean isInputProductTableEnabled(final Entity operationComponent) {
        return !operationComponent.getBooleanField("dontPrintInputProductsInWorkPlans");
    }

    public boolean isOutputProductTableEnabled(final Entity operationComponent) {
        return !operationComponent.getBooleanField("dontPrintOutputProductsInWorkPlans");
    }

    private static final class OperationProductComponentComparator implements Comparator<Entity>, Serializable {

        private static final long serialVersionUID = 2985797934972953807L;

        @Override
        public int compare(final Entity o0, final Entity o1) {
            Entity prod0 = o0.getBelongsToField(PRODUCT_LITERAL);
            Entity prod1 = o1.getBelongsToField(PRODUCT_LITERAL);
            return prod0.getStringField(NUMBER_LITERAL).compareTo(prod1.getStringField(NUMBER_LITERAL));
        }

    }

    private List<Entity> getSortedProductByColumn(final List<Entity> columns,
            final Map<Entity, Map<String, String>> columnValues, final List<Entity> productComponents, final Entity workPlan) {
        final Entity columnIdentifier = workPlan.getBelongsToField(WorkPlanFields.INPUT_PRODUCT_COLUMN_TO_SORT_BY);

        if (columnIdentifier == null || StringUtils.isEmpty(workPlan.getStringField(WorkPlanFields.ORDER_SORTING))
                || !columns.contains(columnIdentifier)) {
            return productComponents;
        }
        final String identifier = columnIdentifier.getStringField(ColumnForProductsFields.IDENTIFIER);

        Map<Entity, String> tempMap = Maps.newHashMap();
        for (Entity productComponent : productComponents) {
            tempMap.put(productComponent, columnValues.get(productComponent).get(identifier));
        }

        List<Entity> sortedProductComponents = Lists.newLinkedList();

        List<Map.Entry<Entity, String>> entries = Lists.newLinkedList(tempMap.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<Entity, String>>() {

            @Override
            public int compare(Entry<Entity, String> o1, Entry<Entity, String> o2) {
                if (o1.getValue() == null && o2.getValue() == null) {
                    return 0;
                }
                if (o1.getValue() == null) {
                    return -1;
                }
                if (PLANED_QUANTITY.equals(identifier)) {
                    return o1.getKey().getDecimalField("quantity").compareTo(o2.getKey().getDecimalField("quantity"));
                }
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        for (Entry<Entity, String> entry : entries) {
            sortedProductComponents.add(entry.getKey());
        }

        if (OrderSorting.ASC.getStringValue().equals(workPlan.getStringField(WorkPlanFields.ORDER_SORTING))) {
            return sortedProductComponents;
        }
        Collections.reverse(sortedProductComponents);

        return sortedProductComponents;
    }
}
