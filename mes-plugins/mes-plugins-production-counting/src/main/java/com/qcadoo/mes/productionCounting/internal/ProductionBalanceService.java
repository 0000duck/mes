/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.2
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
package com.qcadoo.mes.productionCounting.internal;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.lowagie.text.DocumentException;
import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.localization.api.utils.DateUtils;
import com.qcadoo.mes.basic.constants.BasicConstants;
import com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants;
import com.qcadoo.mes.productionCounting.internal.print.ProductionBalancePdfService;
import com.qcadoo.mes.productionCounting.internal.states.ProductionCountingStates;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.file.FileService;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.report.api.ReportService;
import com.qcadoo.security.api.SecurityService;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ComponentState.MessageType;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.WindowComponent;
import com.qcadoo.view.api.ribbon.RibbonActionItem;

@Service
public class ProductionBalanceService {

    private static final String WORKER_FIELD = "worker";

    private static final String FILE_NAME_FIELD = "fileName";

    private static final String GENERATED_FIELD = "generated";

    private static final String DATE_FIELD = "date";

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ProductionBalancePdfService productionBalancePdfService;

    @Autowired
    private FileService fileService;

    @Autowired
    private ReportService reportService;

    public boolean clearGeneratedOnCopy(final DataDefinition dataDefinition, final Entity entity) {
        entity.setField(FILE_NAME_FIELD, null);
        entity.setField(GENERATED_FIELD, false);
        entity.setField(DATE_FIELD, null);
        entity.setField(WORKER_FIELD, null);
        return true;
    }

    public boolean validateOrder(final DataDefinition dataDefinition, final Entity entity) {
        if (entity.getBelongsToField("order").getStringField("typeOfProductionRecording") == null
                || entity.getBelongsToField("order").getStringField("typeOfProductionRecording").equals("01none")) {
            entity.addError(dataDefinition.getField("order"),
                    "productionCounting.productionBalance.report.error.orderWithoutRecordingType");
            return false;
        }
        List<Entity> productionRecordList = dataDefinitionService
                .get(ProductionCountingConstants.PLUGIN_IDENTIFIER, ProductionCountingConstants.MODEL_PRODUCTION_RECORD).find()
                .add(SearchRestrictions.eq("state", ProductionCountingStates.ACCEPTED.getStringValue()))
                .add(SearchRestrictions.belongsTo("order", entity.getBelongsToField("order"))).list().getEntities();
        if (productionRecordList.size() == 0) {
            entity.addError(dataDefinition.getField("order"),
                    "productionCounting.productionBalance.report.error.orderWithoutProductionRecords");
            return false;
        }
        return true;
    }

    public void setGenerateButtonState(final ViewDefinitionState state) {
        setGenerateButtonState(state, state.getLocale(), ProductionCountingConstants.PLUGIN_IDENTIFIER,
                ProductionCountingConstants.MODEL_PRODUCTION_BALANCE);
    }

    public void setGenerateButtonState(final ViewDefinitionState state, final Locale locale, final String plugin,
            final String entityName) {
        WindowComponent window = (WindowComponent) state.getComponentByReference("window");
        FormComponent form = (FormComponent) state.getComponentByReference("form");
        RibbonActionItem generateButton = window.getRibbon().getGroupByName("generate").getItemByName("generate");
        RibbonActionItem deleteButton = window.getRibbon().getGroupByName("actions").getItemByName("delete");

        if (form.getEntityId() == null) {
            generateButton.setMessage("recordNotCreated");
            generateButton.setEnabled(false);
            deleteButton.setMessage(null);
            deleteButton.setEnabled(false);
        } else {
            Entity productionBalance = dataDefinitionService.get(plugin, entityName).get(form.getEntityId());

            if (productionBalance.getField(GENERATED_FIELD) == null) {
                productionBalance.setField(GENERATED_FIELD, "0");
            }

            if ("1".equals(productionBalance.getField(GENERATED_FIELD))) {
                generateButton.setMessage("orders.ribbon.message.recordAlreadyGenerated");
                generateButton.setEnabled(false);
                deleteButton.setMessage("orders.ribbon.message.recordAlreadyGenerated");
                deleteButton.setEnabled(false);
            } else {
                generateButton.setMessage(null);
                generateButton.setEnabled(true);
                deleteButton.setMessage(null);
                deleteButton.setEnabled(true);
            }
        }
        generateButton.requestUpdate(true);
        deleteButton.requestUpdate(true);
        window.requestRibbonRender();
    }

    @Transactional
    public void generateProductionBalance(final ViewDefinitionState viewDefinitionState, final ComponentState state,
            final String[] args) {
        if (state instanceof FormComponent) {
            ComponentState generated = viewDefinitionState.getComponentByReference(GENERATED_FIELD);
            ComponentState date = viewDefinitionState.getComponentByReference(DATE_FIELD);
            ComponentState worker = viewDefinitionState.getComponentByReference(WORKER_FIELD);
            FieldComponent name = (FieldComponent) viewDefinitionState.getComponentByReference("name");
            FieldComponent description = (FieldComponent) viewDefinitionState.getComponentByReference("description");
            FieldComponent order = (FieldComponent) viewDefinitionState.getComponentByReference("order");
            Entity productionBalance = dataDefinitionService.get(ProductionCountingConstants.PLUGIN_IDENTIFIER,
                    ProductionCountingConstants.MODEL_PRODUCTION_BALANCE).get((Long) state.getFieldValue());

            if (productionBalance == null) {
                String message = translationService.translate("qcadooView.message.entityNotFound", state.getLocale());
                state.addMessage(message, MessageType.FAILURE);
                return;
            } else if (StringUtils.hasText(productionBalance.getStringField(FILE_NAME_FIELD))) {
                String message = translationService.translate(
                        "productionCounting.productionBalance.report.error.documentsWasGenerated", state.getLocale());
                state.addMessage(message, MessageType.FAILURE);
                return;
            }

            if ("0".equals(generated.getFieldValue())) {
                worker.setFieldValue(securityService.getCurrentUserName());
                generated.setFieldValue("1");
                name.setEnabled(false);
                order.setEnabled(false);
                description.setEnabled(false);
                date.setFieldValue(new SimpleDateFormat(DateUtils.DATE_TIME_FORMAT).format(new Date()));
                requestComponentUpdateState(viewDefinitionState);
            }

            state.performEvent(viewDefinitionState, "save", new String[0]);

            if (state.getFieldValue() == null || !((FormComponent) state).isValid()) {
                worker.setFieldValue(null);
                generated.setFieldValue("0");
                date.setFieldValue(null);
                return;
            }

            productionBalance = dataDefinitionService.get(ProductionCountingConstants.PLUGIN_IDENTIFIER,
                    ProductionCountingConstants.MODEL_PRODUCTION_BALANCE).get((Long) state.getFieldValue());

            try {
                generateProductionBalanceDocuments(state, productionBalance);
                state.performEvent(viewDefinitionState, "reset", new String[0]);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            } catch (DocumentException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    private void requestComponentUpdateState(final ViewDefinitionState view) {
        for (String reference : Arrays.asList("name", "description", "order", WORKER_FIELD, GENERATED_FIELD, DATE_FIELD)) {
            FieldComponent component = (FieldComponent) view.getComponentByReference(reference);
            component.requestComponentUpdateState();
        }
    }

    public void printProductionBalance(final ViewDefinitionState viewDefinitionState, final ComponentState state,
            final String[] args) {
        args[1] = ProductionCountingConstants.PLUGIN_IDENTIFIER;
        args[2] = ProductionCountingConstants.MODEL_PRODUCTION_BALANCE;
        reportService.printGeneratedReport(viewDefinitionState, state, args);
    }

    private void generateProductionBalanceDocuments(final ComponentState state, final Entity productionBalance)
            throws IOException, DocumentException {
        Entity productionBalanceWithFileName = fileService.updateReportFileName(
                productionBalance,
                "date",
                translationService.translate("productionCounting.productionBalance.report.fileName",
                        LocaleContextHolder.getLocale()));
        Entity company = dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, BasicConstants.MODEL_COMPANY).find()
                .add(SearchRestrictions.eq("owner", true)).setMaxResults(1).uniqueResult();
        productionBalancePdfService.generateDocument(productionBalanceWithFileName, company, state.getLocale());
    }

}
