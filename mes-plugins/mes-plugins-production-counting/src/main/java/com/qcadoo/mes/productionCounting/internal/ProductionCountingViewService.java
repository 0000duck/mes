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
package com.qcadoo.mes.productionCounting.internal;

import static com.qcadoo.mes.productionCounting.internal.constants.OrderFieldsPC.TYPE_OF_PRODUCTION_RECORDING;
import static com.qcadoo.mes.productionCounting.internal.constants.TypeOfProductionRecording.BASIC;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.orders.constants.OrdersConstants;
import com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants;
import com.qcadoo.mes.productionCounting.internal.print.utils.EntityProductionRecordComparator;
import com.qcadoo.mes.productionCounting.internal.states.ProductionCountingStates;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.GridComponent;

@Service
public class ProductionCountingViewService {

    private static final String L_PRODUCTION_RECORDS = "productionRecords";

    private static final String L_PRODUCT = "product";

    private static final String L_ORDER = "order";

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public void fillProductWhenOrderChanged(final ViewDefinitionState viewDefinitionState, final ComponentState state,
            final String[] args) {
        if (!(state instanceof FieldComponent)) {
            return;
        }
        FieldComponent orderLookup = (FieldComponent) viewDefinitionState.getComponentByReference(L_ORDER);
        if (orderLookup.getFieldValue() == null) {
            viewDefinitionState.getComponentByReference(L_PRODUCT).setFieldValue(null);
            viewDefinitionState.getComponentByReference(L_PRODUCTION_RECORDS).setVisible(false);
            return;
        }
        Entity order = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(
                (Long) orderLookup.getFieldValue());
        if (order == null) {
            viewDefinitionState.getComponentByReference(L_PRODUCT).setFieldValue(null);
            viewDefinitionState.getComponentByReference(L_PRODUCTION_RECORDS).setVisible(false);
            return;
        }
        if (order.getStringField(TYPE_OF_PRODUCTION_RECORDING) == null
                || BASIC.getStringValue().equals(order.getStringField(TYPE_OF_PRODUCTION_RECORDING))) {
            viewDefinitionState.getComponentByReference(L_PRODUCT).setFieldValue(null);
            viewDefinitionState.getComponentByReference(L_PRODUCTION_RECORDS).setVisible(false);
            ((FieldComponent) viewDefinitionState.getComponentByReference(L_ORDER)).addMessage(
                    "productionCounting.productionBalance.report.error.orderWithoutRecordingType",
                    ComponentState.MessageType.FAILURE);
            return;
        }

        setProductFieldValue(viewDefinitionState, order);
        setProductionRecordsGridContent(viewDefinitionState, order);
    }

    public void fillProductionRecordsGrid(final ViewDefinitionState viewDefinitionState) {
        FieldComponent orderLookup = (FieldComponent) viewDefinitionState.getComponentByReference(L_ORDER);
        if (orderLookup.getFieldValue() == null) {
            viewDefinitionState.getComponentByReference(L_PRODUCTION_RECORDS).setVisible(false);
            return;
        }
        Entity order = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(
                (Long) orderLookup.getFieldValue());
        if (order == null) {
            viewDefinitionState.getComponentByReference(L_PRODUCTION_RECORDS).setVisible(false);
            return;
        }
        if (order.getStringField(TYPE_OF_PRODUCTION_RECORDING) == null
                || BASIC.getStringValue().equals(order.getStringField(TYPE_OF_PRODUCTION_RECORDING))) {
            viewDefinitionState.getComponentByReference(L_PRODUCTION_RECORDS).setVisible(false);
            return;
        }

        setProductionRecordsGridContent(viewDefinitionState, order);
    }

    private void setProductFieldValue(final ViewDefinitionState viewDefinitionState, final Entity order) {
        FieldComponent productField = (FieldComponent) viewDefinitionState.getComponentByReference(L_PRODUCT);
        productField.setFieldValue(order.getBelongsToField(L_PRODUCT).getId());
    }

    private void setProductionRecordsGridContent(final ViewDefinitionState viewDefinitionState, final Entity order) {
        GridComponent productionRecords = (GridComponent) viewDefinitionState.getComponentByReference(L_PRODUCTION_RECORDS);
        List<Entity> productionRecordsList = dataDefinitionService
                .get(ProductionCountingConstants.PLUGIN_IDENTIFIER, ProductionCountingConstants.MODEL_PRODUCTION_RECORD).find()
                .add(SearchRestrictions.eq("state", ProductionCountingStates.ACCEPTED.getStringValue()))
                .add(SearchRestrictions.belongsTo(L_ORDER, order)).list().getEntities();
        Collections.sort(productionRecordsList, new EntityProductionRecordComparator());
        productionRecords.setEntities(productionRecordsList);
        productionRecords.setVisible(true);
    }

    public void disableFieldsWhenGenerated(final ViewDefinitionState view) {
        Boolean enabled = false;
        ComponentState generated = (ComponentState) view.getComponentByReference("generated");
        if (generated == null || generated.getFieldValue() == null || "0".equals(generated.getFieldValue())) {
            enabled = true;
        }
        for (String reference : Arrays.asList(L_ORDER, "name", "description")) {
            FieldComponent component = (FieldComponent) view.getComponentByReference(reference);
            component.setEnabled(enabled);
        }
    }
}
