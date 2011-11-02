/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.4.9
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
package com.qcadoo.mes.technologies.print;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.qcadoo.mes.technologies.constants.TechnologiesConstants.MODEL_TECHNOLOGY;
import static com.qcadoo.mes.technologies.constants.TechnologiesConstants.PLUGIN_IDENTIFIER;
import static com.qcadoo.model.api.types.TreeType.NODE_NUMBER_FIELD;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;

import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.utils.TreeNumberingService;
import com.qcadoo.report.api.xls.ReportXlsView;
import com.qcadoo.report.api.xls.XlsUtil;

public class TechnologiesTechnologyDetailsXlsView extends ReportXlsView {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private TreeNumberingService treeNumberingService;

    @Override
    protected String addContent(final Map<String, Object> model, final HSSFWorkbook workbook, final Locale locale) {
        HSSFSheet sheet = workbook.createSheet(getTranslationService().translate(
                "technologies.technologiesTechnologyDetails.report.title", locale));
        addOrderHeader(sheet, locale);
        addOrderSeries(model, sheet, locale);
        return getTranslationService().translate("technologies.technologiesTechnologyDetails.report.fileName", locale);
    }

    private void addOrderHeader(final HSSFSheet sheet, final Locale locale) {
        HSSFRow header = sheet.createRow(0);
        int columnCounter = 0;
        for (String headerText : newArrayList("level", "name", "direction", "product", "quantity", "unit")) {
            HSSFCell cell = header.createCell(columnCounter);
            cell.setCellValue(getTranslationService().translate(
                    "technologies.technologiesTechnologyDetails.report.columnHeader." + headerText, locale));
            cell.setCellStyle(XlsUtil.getHeaderStyle(sheet.getWorkbook()));
            columnCounter++;
        }
    }

    private void addOrderSeries(final Map<String, Object> model, final HSSFSheet sheet, final Locale locale) {
        DataDefinition technologyDD = dataDefinitionService.get(PLUGIN_IDENTIFIER, MODEL_TECHNOLOGY);
        Entity technology = technologyDD.get(new Long(model.get("id").toString()));
        List<Entity> technologyOperations = newLinkedList(technology.getTreeField("operationComponents"));
        Collections.sort(technologyOperations, treeNumberingService.getTreeNodesNumberComparator());

        int rowNum = 1;
        for (Entity technologyOperation : technologyOperations) {
            String nodeNumber = technologyOperation.getStringField(NODE_NUMBER_FIELD);
            String operationName = technologyOperation.getBelongsToField("operation").getStringField("name");
            List<Entity> technologyOperationProducts = newArrayList();
            technologyOperationProducts.addAll(technologyOperation.getHasManyField("operationProductInComponents"));
            technologyOperationProducts.addAll(technologyOperation.getHasManyField("operationProductOutComponents"));

            for (Entity product : technologyOperationProducts) {
                HSSFRow row = sheet.createRow(rowNum++);
                String productType = "technologies.technologiesTechnologyDetails.report.direction.out";
                if (product.getDataDefinition().getName().toString().equals("operationProductInComponent")) {
                    productType = "technologies.technologiesTechnologyDetails.report.direction.in";
                }
                row.createCell(0).setCellValue(nodeNumber);
                row.createCell(1).setCellValue(operationName);
                row.createCell(2).setCellValue(getTranslationService().translate(productType, locale));
                row.createCell(3).setCellValue(product.getBelongsToField("product").getStringField("name"));
                row.createCell(4).setCellValue(product.getField("quantity").toString());
                row.createCell(5).setCellValue(product.getBelongsToField("product").getStringField("unit"));
            }

        }

        sheet.autoSizeColumn((short) 0);
        sheet.autoSizeColumn((short) 1);
        sheet.autoSizeColumn((short) 2);
        sheet.autoSizeColumn((short) 3);
        sheet.autoSizeColumn((short) 4);
        sheet.autoSizeColumn((short) 5);
    }
}
