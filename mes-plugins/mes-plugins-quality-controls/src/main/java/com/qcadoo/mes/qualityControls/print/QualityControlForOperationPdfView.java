/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.3.0
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

package com.qcadoo.mes.qualityControls.print;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.qcadoo.mes.api.Entity;
import com.qcadoo.mes.api.SecurityService;
import com.qcadoo.mes.beans.users.UsersUser;
import com.qcadoo.mes.qualityControls.print.utils.EntityNumberComparator;
import com.qcadoo.mes.utils.SortUtil;
import com.qcadoo.mes.utils.pdf.PdfUtil;
import com.qcadoo.mes.utils.pdf.ReportPdfView;

public class QualityControlForOperationPdfView extends ReportPdfView {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private QualityControlsReportService qualityControlsReportService;

    @Override
    protected final String addContent(final Document document, final Map<String, Object> model, final Locale locale,
            final PdfWriter writer) throws DocumentException, IOException {
        String documentTitle = getTranslationService().translate("qualityControls.qualityControlForOperation.report.title",
                locale);
        String documentAuthor = getTranslationService().translate("qualityControls.qualityControl.report.author", locale);
        UsersUser user = securityService.getCurrentUser();
        PdfUtil.addDocumentHeader(document, "", documentTitle, documentAuthor, new Date(), user);
        qualityControlsReportService.addQualityControlReportHeader(document, model, locale);

        List<Entity> orders = qualityControlsReportService.getOrderSeries(model, "qualityControlsForOperation");
        Map<Entity, List<Entity>> operationOrders = qualityControlsReportService.getQualityOrdersForOperation(orders);
        Map<Entity, List<BigDecimal>> quantities = qualityControlsReportService
                .getQualityOrdersResultsQuantitiesForOperation(orders);

        quantities = SortUtil.sortMapUsingComparator(quantities, new EntityNumberComparator());

        addOrderSeries(document, quantities, locale);

        operationOrders = SortUtil.sortMapUsingComparator(operationOrders, new EntityNumberComparator());

        for (Entry<Entity, List<Entity>> entry : operationOrders.entrySet()) {
            document.add(Chunk.NEWLINE);
            addProductSeries(document, operationOrders, entry, locale);
        }

        String text = getTranslationService().translate("core.report.endOfReport", locale);
        PdfUtil.addEndOfDocument(document, writer, text);
        return getTranslationService().translate("qualityControls.qualityControlForOperation.report.fileName", locale);
    }

    @Override
    protected final void addTitle(final Document document, final Locale locale) {
        document.addTitle(getTranslationService().translate("qualityControls.qualityControlForOperation.report.title", locale));
    }

    private void addOrderSeries(final Document document, final Map<Entity, List<BigDecimal>> quantities, final Locale locale)
            throws DocumentException {
        List<String> qualityHeader = new ArrayList<String>();
        qualityHeader.add(getTranslationService().translate("qualityControls.qualityControl.report.operation.number", locale));
        qualityHeader.add(getTranslationService().translate("qualityControls.qualityControl.report.controlled.quantity", locale));
        qualityHeader.add(getTranslationService().translate("qualityControls.qualityControl.report.correct.quantity", locale));
        qualityHeader.add(getTranslationService().translate("qualityControls.qualityControl.report.incorrect.quantity", locale));
        qualityHeader.add(getTranslationService().translate("qualityControls.qualityControl.report.objective.quantity", locale));
        PdfPTable table = PdfUtil.createTableWithHeader(5, qualityHeader, false);
        for (Entry<Entity, List<BigDecimal>> entry : quantities.entrySet()) {
            table.addCell(new Phrase(entry.getKey() != null ? entry.getKey().getField("number").toString() : "", PdfUtil
                    .getArialRegular9Dark()));
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(new Phrase(getDecimalFormat().format(entry.getValue().get(0)), PdfUtil.getArialRegular9Dark()));
            table.addCell(new Phrase(getDecimalFormat().format(entry.getValue().get(1)), PdfUtil.getArialRegular9Dark()));
            table.addCell(new Phrase(getDecimalFormat().format(entry.getValue().get(2)), PdfUtil.getArialRegular9Dark()));
            table.addCell(new Phrase(getDecimalFormat().format(entry.getValue().get(3)), PdfUtil.getArialRegular9Dark()));
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
        }
        document.add(table);
    }

    private void addProductSeries(final Document document, final Map<Entity, List<Entity>> productOrders,
            final Entry<Entity, List<Entity>> entry, final Locale locale) throws DocumentException {

        document.add(qualityControlsReportService.prepareTitle(entry.getKey(), locale, "product"));

        List<String> productHeader = new ArrayList<String>();
        productHeader.add(getTranslationService().translate("qualityControls.qualityControl.report.control.number", locale));
        productHeader.add(getTranslationService().translate("qualityControls.qualityControl.report.controlled.quantity", locale));
        PdfPTable table = PdfUtil.createTableWithHeader(2, productHeader, false);
        List<Entity> sortedOrders = entry.getValue();
        Collections.sort(sortedOrders, new EntityNumberComparator());
        for (Entity entity : sortedOrders) {
            table.addCell(new Phrase(entity.getField("number").toString(), PdfUtil.getArialRegular9Dark()));
            table.addCell(new Phrase(
                    getTranslationService().translate(
                            "qualityControls.qualityForOrder.controlResult.value." + entity.getField("controlResult").toString(),
                            locale), PdfUtil.getArialRegular9Dark()));
        }
        document.add(table);

    }
}
