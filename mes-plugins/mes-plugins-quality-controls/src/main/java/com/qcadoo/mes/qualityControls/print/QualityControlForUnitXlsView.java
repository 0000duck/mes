package com.qcadoo.mes.qualityControls.print;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;

import com.qcadoo.mes.api.Entity;
import com.qcadoo.mes.utils.xls.ReportXlsView;
import com.qcadoo.mes.utils.xls.XlsUtil;

public class QualityControlForUnitXlsView extends ReportXlsView {

    @Autowired
    private QualityControlsReportService qualityControlsReportService;

    @Override
    protected String addContent(final Map<String, Object> model, final HSSFWorkbook workbook, final Locale locale) {
        HSSFSheet sheet = workbook.createSheet(getTranslationService().translate(
                "qualityControls.qualityControlForUnit.report.title", locale));
        sheet.setZoom(4, 3);
        addOrderHeader(sheet, locale);
        addOrderSeries(model, sheet);
        return getTranslationService().translate("qualityControls.qualityControlForUnit.report.fileName", locale);
    }

    private void addOrderHeader(final HSSFSheet sheet, final Locale locale) {
        HSSFRow header = sheet.createRow(0);
        HSSFCell cell0 = header.createCell(0);
        cell0.setCellValue(getTranslationService().translate("qualityControls.qualityControl.report.product.number", locale));
        cell0.setCellStyle(XlsUtil.getHeaderStyle(sheet.getWorkbook()));
        HSSFCell cell1 = header.createCell(1);
        cell1.setCellValue(getTranslationService().translate(
                "qualityControls.qualityControlForUnit.window.qualityControlForUnit.number.label", locale));
        cell1.setCellStyle(XlsUtil.getHeaderStyle(sheet.getWorkbook()));
        HSSFCell cell2 = header.createCell(2);
        cell2.setCellValue(getTranslationService().translate(
                "qualityControls.qualityControlForUnit.window.qualityControlForUnit.controlledQuantity.label", locale));
        cell2.setCellStyle(XlsUtil.getHeaderStyle(sheet.getWorkbook()));
        HSSFCell cell3 = header.createCell(3);
        cell3.setCellValue(getTranslationService().translate(
                "qualityControls.qualityControlForUnit.window.qualityControlForUnit.rejectedQuantity.label", locale));
        cell3.setCellStyle(XlsUtil.getHeaderStyle(sheet.getWorkbook()));
        HSSFCell cell4 = header.createCell(4);
        cell4.setCellValue(getTranslationService().translate(
                "qualityControls.qualityControlForUnit.window.qualityControlForUnit.acceptedDefectsQuantity.label", locale));
        cell4.setCellStyle(XlsUtil.getHeaderStyle(sheet.getWorkbook()));
    }

    private void addOrderSeries(final Map<String, Object> model, final HSSFSheet sheet) {
        int rowNum = 1;
        Map<Entity, List<Entity>> productOrders = new HashMap<Entity, List<Entity>>();
        qualityControlsReportService.aggregateOrdersDataForProduct(productOrders, new HashMap<Entity, List<BigDecimal>>(),
                qualityControlsReportService.getOrderSeries(model.get("dateFrom").toString(), model.get("dateTo").toString(),
                        "qualityControlsForUnit"), false);
        for (Entry<Entity, List<Entity>> entry : productOrders.entrySet()) {
            for (Entity order : entry.getValue()) {
                HSSFRow row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey() == null ? "" : entry.getKey().getField("number").toString());
                row.createCell(1).setCellValue(order.getField("number").toString());
                row.createCell(2).setCellValue(
                        ((BigDecimal) order.getField("controlledQuantity")).setScale(3, RoundingMode.HALF_EVEN).doubleValue());
                row.createCell(3).setCellValue(
                        ((BigDecimal) order.getField("rejectedQuantity")).setScale(3, RoundingMode.HALF_EVEN).doubleValue());
                row.createCell(4).setCellValue(
                        ((BigDecimal) order.getField("acceptedDefectsQuantity")).setScale(3, RoundingMode.HALF_EVEN)
                                .doubleValue());
            }
        }
        sheet.autoSizeColumn((short) 0);
        sheet.autoSizeColumn((short) 1);
        sheet.autoSizeColumn((short) 2);
        sheet.autoSizeColumn((short) 3);
        sheet.autoSizeColumn((short) 4);
    }

}
