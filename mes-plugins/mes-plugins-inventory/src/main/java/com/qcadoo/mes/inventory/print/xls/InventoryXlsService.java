/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.4.7
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
package com.qcadoo.mes.inventory.print.xls;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.qcadoo.model.api.Entity;
import com.qcadoo.report.api.xls.XlsDocumentService;
import com.qcadoo.report.api.xls.XlsUtil;

@Service
public final class InventoryXlsService extends XlsDocumentService {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryXlsService.class);

    public final void generateDocument(final Entity entity, final Map<Entity, BigDecimal> reportData, final Locale locale)
            throws IOException {
        setDecimalFormat((DecimalFormat) DecimalFormat.getInstance(locale));
        getDecimalFormat().setMaximumFractionDigits(3);
        getDecimalFormat().setMinimumFractionDigits(3);
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(getReportTitle(locale));
        addHeader(sheet, locale);
        addSeries(sheet, reportData);
        sheet.setZoom(4, 3);
        FileOutputStream outputStream = null;
        try {
            ensureReportDirectoryExist();
            outputStream = new FileOutputStream((String) entity.getField("fileName") + getSuffix() + XlsUtil.XLS_EXTENSION);
            workbook.write(outputStream);
        } catch (IOException e) {
            LOG.error("Problem with generating document - " + e.getMessage());
            if (outputStream != null) {
                outputStream.close();
            }
            throw e;
        }
        outputStream.close();
    }

    @Override
    protected void addHeader(final HSSFSheet sheet, final Locale locale) {
        HSSFRow header = sheet.createRow(0);
        HSSFCell cell0 = header.createCell(0);
        cell0.setCellValue(getTranslationService().translate("inventory.inventory.report.columnHeader.number", locale));
        cell0.setCellStyle(XlsUtil.getHeaderStyle(sheet.getWorkbook()));
        HSSFCell cell1 = header.createCell(1);
        cell1.setCellValue(getTranslationService().translate("inventory.inventory.report.columnHeader.name", locale));
        cell1.setCellStyle(XlsUtil.getHeaderStyle(sheet.getWorkbook()));
        HSSFCell cell2 = header.createCell(2);
        cell2.setCellValue(getTranslationService().translate("inventory.inventory.report.columnHeader.quantity", locale));
        cell2.setCellStyle(XlsUtil.getHeaderStyle(sheet.getWorkbook()));
        HSSFCell cell3 = header.createCell(3);
        cell3.setCellValue(getTranslationService().translate("inventory.inventory.report.columnHeader.unit", locale));
        cell3.setCellStyle(XlsUtil.getHeaderStyle(sheet.getWorkbook()));
    }

    protected void addSeries(final HSSFSheet sheet, Map<Entity, BigDecimal> reportData) {
        int rowNum = 1;
        for (Map.Entry<Entity, BigDecimal> data : reportData.entrySet()) {
            HSSFRow row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(data.getKey().getBelongsToField("product").getStringField("number"));
            row.createCell(1).setCellValue(data.getKey().getBelongsToField("product").getStringField("name"));
            row.createCell(2).setCellValue(this.getDecimalFormat().format(data.getValue()));
            row.createCell(3).setCellValue(data.getKey().getBelongsToField("product").getStringField("unit"));
        }
        sheet.autoSizeColumn((short) 0);
        sheet.autoSizeColumn((short) 1);
        sheet.autoSizeColumn((short) 2);
        sheet.autoSizeColumn((short) 3);
    }

    @Override
    protected String getReportTitle(final Locale locale) {
        return getTranslationService().translate("inventory.inventory.report.title", locale);
    }

    @Override
    protected String getSuffix() {
        return "";
    }

    @Override
    protected void addSeries(final HSSFSheet sheet, final Entity entity) {
    }
}
