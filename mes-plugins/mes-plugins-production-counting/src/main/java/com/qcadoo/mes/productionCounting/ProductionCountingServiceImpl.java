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
package com.qcadoo.mes.productionCounting;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.qcadoo.mes.basic.constants.ProductFields;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.orders.states.constants.OrderStateStringValues;
import com.qcadoo.mes.productionCounting.constants.CalculateOperationCostsMode;
import com.qcadoo.mes.productionCounting.constants.OrderFieldsPC;
import com.qcadoo.mes.productionCounting.constants.ProductionBalanceFields;
import com.qcadoo.mes.productionCounting.constants.ProductionCountingConstants;
import com.qcadoo.mes.productionCounting.constants.ProductionTrackingFields;
import com.qcadoo.mes.productionCounting.constants.ProductionTrackingReportFields;
import com.qcadoo.mes.productionCounting.constants.TypeOfProductionRecording;
import com.qcadoo.mes.productionCounting.print.utils.EntityProductionTrackingComparator;
import com.qcadoo.mes.productionCounting.states.constants.ProductionTrackingStateStringValues;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.GridComponent;
import com.qcadoo.view.api.components.LookupComponent;

@Service
public class ProductionCountingServiceImpl implements ProductionCountingService {

    private static final String L_FORM = "form";

    private static final String L_ORDER = "order";

    private static final String L_PRODUCT = "product";

    private static final String L_NUMBER = "number";

    private static final String L_NAME = "name";

    private static final String L_PLANNED_QUANTITY_UNIT = "plannedQuantityUNIT";

    private static final String L_USED_QUANTITY_UNIT = "usedQuantityUNIT";

    private static final String L_PRODUCTION_TRACKINGS = "productionTrackings";

    private static final List<String> L_TRACKING_OPERATION_PRODUCT_FIELD_NAMES = Lists.newArrayList(L_NUMBER, L_NAME,
            L_PLANNED_QUANTITY_UNIT, L_USED_QUANTITY_UNIT);

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Override
    public Entity getProductionTrackingReport(final Long productionTrackingReportId) {
        return getProductionTrackingReportDD().get(productionTrackingReportId);
    }

    @Override
    public Entity getProductionTracking(final Long productionTrackingId) {
        return getProductionTrackingDD().get(productionTrackingId);
    }

    @Override
    public Entity getProductionBalance(final Long productionBalanceId) {
        return getProductionBalanceDD().get(productionBalanceId);
    }

    @Override
    public Entity getTrackingOperationProductInComponent(final Long trackingOperationProductInComponentId) {
        return getTrackingOperationProductInComponentDD().get(trackingOperationProductInComponentId);
    }

    @Override
    public Entity getTrackingOperationProductOutComponent(final Long trackingOperationProductOutComponentId) {
        return getTrackingOperationProductOutComponentDD().get(trackingOperationProductOutComponentId);
    }

    @Override
    public DataDefinition getProductionTrackingReportDD() {
        return dataDefinitionService.get(ProductionCountingConstants.PLUGIN_IDENTIFIER,
                ProductionCountingConstants.MODEL_PRODUCTION_TRACKING_REPORT);
    }

    @Override
    public DataDefinition getProductionTrackingDD() {
        return dataDefinitionService.get(ProductionCountingConstants.PLUGIN_IDENTIFIER,
                ProductionCountingConstants.MODEL_PRODUCTION_TRACKING);
    }

    @Override
    public DataDefinition getProductionBalanceDD() {
        return dataDefinitionService.get(ProductionCountingConstants.PLUGIN_IDENTIFIER,
                ProductionCountingConstants.MODEL_PRODUCTION_BALANCE);
    }

    @Override
    public DataDefinition getTrackingOperationProductInComponentDD() {
        return dataDefinitionService.get(ProductionCountingConstants.PLUGIN_IDENTIFIER,
                ProductionCountingConstants.MODEL_TRACKING_OPERATION_PRODUCT_IN_COMPONENT);
    }

    @Override
    public DataDefinition getTrackingOperationProductOutComponentDD() {
        return dataDefinitionService.get(ProductionCountingConstants.PLUGIN_IDENTIFIER,
                ProductionCountingConstants.MODEL_TRACKING_OPERATION_PRODUCT_OUT_COMPONENT);
    }

    @Override
    public List<Entity> getProductionTrackingsForOrder(final Entity order) {
        return getProductionTrackingDD().find()
                .add(SearchRestrictions.eq(ProductionTrackingFields.STATE, ProductionTrackingStateStringValues.ACCEPTED))
                .add(SearchRestrictions.belongsTo(ProductionTrackingFields.ORDER, order)).list().getEntities();
    }

    @Override
    public boolean isTypeOfProductionRecordingBasic(final String typeOfProductionRecording) {
        return TypeOfProductionRecording.BASIC.getStringValue().equals(typeOfProductionRecording);
    }

    @Override
    public boolean isTypeOfProductionRecordingForEach(final String typeOfProductionRecording) {
        return TypeOfProductionRecording.FOR_EACH.getStringValue().equals(typeOfProductionRecording);
    }

    @Override
    public boolean isTypeOfProductionRecordingCumulated(final String typeOfProductionRecording) {
        return TypeOfProductionRecording.CUMULATED.getStringValue().equals(typeOfProductionRecording);
    }

    @Override
    public boolean checkIfTypeOfProductionRecordingIsEmptyOrBasic(final String typeOfProductionRecording) {
        return StringUtils.isEmpty(typeOfProductionRecording) || isTypeOfProductionRecordingBasic(typeOfProductionRecording);
    }

    @Override
    public boolean isCalculateOperationCostModeHourly(final String calculateOperationCostMode) {
        return CalculateOperationCostsMode.HOURLY.getStringValue().equals(calculateOperationCostMode);
    }

    @Override
    public boolean isCalculateOperationCostModePiecework(final String calculateOperationCostMode) {
        return CalculateOperationCostsMode.PIECEWORK.getStringValue().equals(calculateOperationCostMode);
    }

    @Override
    public boolean validateOrder(final DataDefinition productionTrackingReportOrBalanceDD,
            final Entity productionTrackingReportOrBalance) {
        Entity order = productionTrackingReportOrBalance.getBelongsToField(L_ORDER);

        if ((order == null) || isTypeOfProductionRecordingBasic(order.getStringField(OrderFieldsPC.TYPE_OF_PRODUCTION_RECORDING))) {
            productionTrackingReportOrBalance.addError(productionTrackingReportOrBalanceDD.getField(L_ORDER),
                    "productionCounting.productionBalance.report.error.orderWithoutRecordingType");

            return false;
        }

        String calculateOperationCostMode = productionTrackingReportOrBalance
                .getStringField(ProductionBalanceFields.CALCULATE_OPERATION_COST_MODE);

        if (!order.getBooleanField(OrderFieldsPC.REGISTER_PRODUCTION_TIME)
                && isCalculateOperationCostModeHourly(calculateOperationCostMode)) {
            productionTrackingReportOrBalance.addError(productionTrackingReportOrBalanceDD.getField(L_ORDER),
                    "productionCounting.productionBalance.report.error.orderWithoutRegisterProductionTime");

            return false;
        } else if (!order.getBooleanField(OrderFieldsPC.REGISTER_PIECEWORK)
                && isCalculateOperationCostModePiecework(calculateOperationCostMode)) {
            productionTrackingReportOrBalance.addError(productionTrackingReportOrBalanceDD.getField(L_ORDER),
                    "productionCounting.productionBalance.report.error.orderWithoutRegisterPiecework");

            return false;
        }

        List<Entity> productionTrackings = getProductionTrackingsForOrder(order);

        if (productionTrackings.isEmpty()) {
            productionTrackingReportOrBalance.addError(productionTrackingReportOrBalanceDD.getField(L_ORDER),
                    "productionCounting.productionBalance.report.error.orderWithoutProductionTrackings");

            return false;
        }

        return true;
    }

    @Override
    public void setComponentsState(final ViewDefinitionState view, final List<String> componentReferenceNames,
            final boolean isEnabled, final boolean requestComponentUpdateState) {
        for (String componentReference : componentReferenceNames) {
            view.getComponentByReference(componentReference).setEnabled(isEnabled);

            if (requestComponentUpdateState) {
                ((FieldComponent) view.getComponentByReference(componentReference)).requestComponentUpdateState();
            }
        }

    }

    @Override
    public void setComponentsVisibility(final ViewDefinitionState view, final List<String> componentReferenceNames,
            final boolean isVisible, final boolean requestComponentUpdateState) {
        for (String componentReference : componentReferenceNames) {
            view.getComponentByReference(componentReference).setVisible(isVisible);

            if (requestComponentUpdateState) {
                ((FieldComponent) view.getComponentByReference(componentReference)).requestComponentUpdateState();
            }
        }
    }

    @Override
    public void changeDoneQuantityAndAmountOfProducedQuantityFieldState(final ViewDefinitionState view) {
        FormComponent orderForm = (FormComponent) view.getComponentByReference(L_FORM);
        FieldComponent typeOfProductionRecordingField = (FieldComponent) view
                .getComponentByReference(OrderFieldsPC.TYPE_OF_PRODUCTION_RECORDING);

        FieldComponent doneQuantityField = (FieldComponent) view.getComponentByReference(OrderFields.DONE_QUANTITY);
        FieldComponent amountOfProductProducedField = (FieldComponent) view
                .getComponentByReference(OrderFields.AMOUNT_OF_PRODUCT_PRODUCED);

        Long orderId = orderForm.getEntityId();

        if (orderId == null) {
            doneQuantityField.setEnabled(false);
            amountOfProductProducedField.setEnabled(false);

            return;
        }

        Entity order = orderForm.getEntity();

        String state = order.getStringField(OrderFields.STATE);

        if (OrderStateStringValues.PENDING.equals(state) || OrderStateStringValues.ACCEPTED.equals(state)) {
            doneQuantityField.setEnabled(false);
            amountOfProductProducedField.setEnabled(false);
        } else {
            String typeOfProductionRecording = (String) typeOfProductionRecordingField.getFieldValue();

            if (checkIfTypeOfProductionRecordingIsEmptyOrBasic(typeOfProductionRecording)) {
                doneQuantityField.setEnabled(true);
                amountOfProductProducedField.setEnabled(true);
            } else {
                doneQuantityField.setEnabled(false);
                amountOfProductProducedField.setEnabled(false);
            }
        }
    }

    @Override
    public void fillFieldsFromProduct(final ViewDefinitionState view, final DataDefinition trackingOperationProductComponentDD) {
        FormComponent trackingOperationProductComponentForm = (FormComponent) view.getComponentByReference(L_FORM);

        Long trackingOperationProductComponentId = trackingOperationProductComponentForm.getEntityId();

        if (trackingOperationProductComponentId == null) {
            return;
        }

        Entity trackingOperationProductComponent = trackingOperationProductComponentDD.get(trackingOperationProductComponentId);

        if (trackingOperationProductComponent == null) {
            return;
        }

        Entity product = trackingOperationProductComponent.getBelongsToField(L_PRODUCT);

        if (product == null) {
            return;
        }

        view.getComponentByReference(L_NUMBER).setFieldValue(product.getField(ProductFields.NUMBER));
        view.getComponentByReference(L_NAME).setFieldValue(product.getField(ProductFields.NAME));

        view.getComponentByReference(L_USED_QUANTITY_UNIT).setFieldValue(product.getStringField(ProductFields.UNIT));
        view.getComponentByReference(L_PLANNED_QUANTITY_UNIT).setFieldValue(product.getStringField(ProductFields.UNIT));

        for (String fieldComponentNames : L_TRACKING_OPERATION_PRODUCT_FIELD_NAMES) {
            ((FieldComponent) view.getComponentByReference(fieldComponentNames)).requestComponentUpdateState();
        }
    }

    @Override
    public void fillProductField(final ViewDefinitionState view) {
        LookupComponent orderLookup = (LookupComponent) view.getComponentByReference(ProductionTrackingReportFields.ORDER);
        LookupComponent productLookup = (LookupComponent) view.getComponentByReference(ProductionTrackingReportFields.PRODUCT);

        Entity order = orderLookup.getEntity();

        if (order == null) {
            productLookup.setFieldValue(null);

            return;
        }

        String typeOfProductionRecording = order.getStringField(OrderFieldsPC.TYPE_OF_PRODUCTION_RECORDING);

        if (checkIfTypeOfProductionRecordingIsEmptyOrBasic(typeOfProductionRecording)) {
            productLookup.setFieldValue(null);

            return;
        }

        setProductFieldValue(view, order);
    }

    private void setProductFieldValue(final ViewDefinitionState view, final Entity order) {
        LookupComponent productLookup = (LookupComponent) view.getComponentByReference(ProductionTrackingReportFields.PRODUCT);

        Entity product = order.getBelongsToField(OrderFields.PRODUCT);

        if (product != null) {
            productLookup.setFieldValue(product.getId());
            productLookup.requestComponentUpdateState();
        }
    }

    @Override
    public void fillProductionTrackingsGrid(final ViewDefinitionState view) {
        LookupComponent orderLookup = (LookupComponent) view.getComponentByReference(ProductionTrackingReportFields.ORDER);
        GridComponent productionTrackingsGrid = (GridComponent) view.getComponentByReference(L_PRODUCTION_TRACKINGS);

        Entity order = orderLookup.getEntity();

        if (order == null) {
            productionTrackingsGrid.setVisible(false);

            return;
        }

        String typeOfProductionRecording = order.getStringField(OrderFieldsPC.TYPE_OF_PRODUCTION_RECORDING);

        if (checkIfTypeOfProductionRecordingIsEmptyOrBasic(typeOfProductionRecording)) {
            productionTrackingsGrid.setVisible(false);

            return;
        }

        productionTrackingsGrid.setVisible(true);

        setProductionTrackingsGridContent(view, order);
    }

    private void setProductionTrackingsGridContent(final ViewDefinitionState view, final Entity order) {
        GridComponent productionTrackingsGrid = (GridComponent) view.getComponentByReference(L_PRODUCTION_TRACKINGS);

        List<Entity> productionTrackings = getProductionTrackingsForOrder(order);

        Collections.sort(productionTrackings, new EntityProductionTrackingComparator());

        productionTrackingsGrid.setEntities(productionTrackings);
    }

}
