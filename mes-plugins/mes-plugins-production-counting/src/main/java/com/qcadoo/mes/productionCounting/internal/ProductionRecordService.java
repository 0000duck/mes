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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.qcadoo.mes.basic.constants.BasicConstants.MODEL_DIVISION;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants.MODEL_RECORD_OPERATION_PRODUCT_IN_COMPONENT;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants.MODEL_RECORD_OPERATION_PRODUCT_OUT_COMPONENT;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants.PARAM_RECORDING_TYPE_BASIC;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants.PARAM_RECORDING_TYPE_CUMULATED;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants.PARAM_RECORDING_TYPE_FOREACH;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants.PARAM_RECORDING_TYPE_NONE;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants.PARAM_REGISTER_IN_PRODUCTS;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants.PARAM_REGISTER_OUT_PRODUCTS;
import static java.math.BigDecimal.ROUND_UP;
import static java.util.Arrays.asList;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.basic.constants.BasicConstants;
import com.qcadoo.mes.orders.constants.OrdersConstants;
import com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants;
import com.qcadoo.mes.productionCounting.internal.states.ProductionCountingStates;
import com.qcadoo.mes.productionScheduling.OrderRealizationTimeService;
import com.qcadoo.mes.productionScheduling.constants.ProductionSchedulingConstants;
import com.qcadoo.mes.technologies.ProductQuantitiesService;
import com.qcadoo.mes.technologies.constants.TechnologiesConstants;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityTree;
import com.qcadoo.model.api.EntityTreeNode;
import com.qcadoo.model.api.NumberService;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.utils.NumberGeneratorService;

@Service
public class ProductionRecordService {

    private static final String L_ORDER_OPERATION_COMPONENTS = "orderOperationComponents";

    private static final String L_PLANNED_TIME = "plannedTime";

    private static final String L_PLANNED_LABOR_TIME = "plannedLaborTime";

    private static final String L_PLANNED_MACHINE_TIME = "plannedMachineTime";

    private static final String L_PLANNED_QUANTITY = "plannedQuantity";

    private static final String L_NUMBER = "number";

    private static final String L_TYPE_OF_PRODUCTION_RCORDING = "typeOfProductionRecording";

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private NumberGeneratorService numberGeneratorService;

    @Autowired
    private ProductQuantitiesService productQuantitiesService;

    @Autowired
    private NumberService numberService;

    @Autowired
    OrderRealizationTimeService orderRealizationTimeService;

    public void generateData(final DataDefinition productionRecordDD, final Entity productionRecord) {
        if (productionRecord.getField(L_NUMBER) == null) {
            productionRecord.setField(L_NUMBER, numberGeneratorService.generateNumber(
                    ProductionCountingConstants.PLUGIN_IDENTIFIER, productionRecord.getDataDefinition().getName()));
        }
    }

    public boolean checkTypeOfProductionRecording(final DataDefinition productionRecordDD, final Entity productionRecord) {
        final Entity order = productionRecord.getBelongsToField(OrdersConstants.MODEL_ORDER);
        final String typeOfProductionRecording = order.getStringField(L_TYPE_OF_PRODUCTION_RCORDING);
        return isValidTypeOfProductionRecording(productionRecord, typeOfProductionRecording, productionRecordDD);
    }

    public boolean isValidTypeOfProductionRecording(final Entity productionRecord, final String typeOfProductionRecording,
            final DataDefinition productionRecordDD) {
        boolean validTypeOfRecording = true;
        if (typeOfProductionRecording == null || PARAM_RECORDING_TYPE_NONE.equals(typeOfProductionRecording)) {
            productionRecord.addError(productionRecordDD.getField(OrdersConstants.MODEL_ORDER),
                    "productionCounting.validate.global.error.productionRecord.orderError");
            validTypeOfRecording = false;
        }
        if (PARAM_RECORDING_TYPE_BASIC.equals(typeOfProductionRecording)) {
            productionRecord.addError(productionRecordDD.getField(OrdersConstants.MODEL_ORDER),
                    "productionRecord.productionRecord.report.error.orderWithBasicProductionCounting");
            validTypeOfRecording = false;
        }
        return validTypeOfRecording;
    }

    public boolean willOrderAcceptOneMore(final DataDefinition productionRecordDD, final Entity productionRecord) {
        final Entity order = productionRecord.getBelongsToField(OrdersConstants.MODEL_ORDER);
        final Entity operation = productionRecord
                .getBelongsToField(ProductionSchedulingConstants.MODEL_ORDER_OPERATION_COMPONENT);

        final List<Entity> productionCountings = productionRecordDD.find()
                .add(SearchRestrictions.belongsTo(OrdersConstants.MODEL_ORDER, order))
                .add(SearchRestrictions.belongsTo(ProductionSchedulingConstants.MODEL_ORDER_OPERATION_COMPONENT, operation))
                .list().getEntities();

        return willOrderAcceptOneMoreValidator(productionCountings, productionRecord, productionRecordDD);
    }

    boolean willOrderAcceptOneMoreValidator(final List<Entity> productionCountings, final Entity productionRecord,
            final DataDefinition dd) {
        for (Entity counting : productionCountings) {
            if (counting.getBooleanField("lastRecord")) {
                if (productionRecord.getBelongsToField(ProductionSchedulingConstants.MODEL_ORDER_OPERATION_COMPONENT) == null) {
                    productionRecord.addError(dd.getField(OrdersConstants.MODEL_ORDER),
                            "productionCounting.record.messages.error.final");
                } else {
                    productionRecord.addError(dd.getField(ProductionSchedulingConstants.MODEL_ORDER_OPERATION_COMPONENT),
                            "productionCounting.record.messages.error.operationFinal");
                }

                return false;
            }
        }

        return true;
    }

    public boolean checkIfOrderIsStarted(final DataDefinition dd, final Entity entity) {
        boolean isStarted = true;
        final String orderState = entity.getBelongsToField(OrdersConstants.MODEL_ORDER).getStringField("state");
        if (orderState == null || "01pending".equals(orderState) || "02accepted".equals(orderState)
                || "05declined".equals(orderState) || "07abandoned".equals(orderState)) {
            entity.addError(dd.getField(OrdersConstants.MODEL_ORDER),
                    "productionCounting.record.messages.error.orderIsNotStarted");
            isStarted = false;
        }
        return isStarted;
    }

    public void copyProductsFromOrderOperation(final DataDefinition productionRecordDD, final Entity productionRecord) {
        Entity order = productionRecord.getBelongsToField(OrdersConstants.MODEL_ORDER);
        if (order == null) {
            return;
        }
        String typeOfProductionRecording = order.getStringField(L_TYPE_OF_PRODUCTION_RCORDING);
        if (typeOfProductionRecording == null) {
            return;
        }
        List<Entity> operationComponents = null;

        Boolean registerInput = order.getBooleanField(PARAM_REGISTER_IN_PRODUCTS);
        Boolean registerOutput = order.getBooleanField(PARAM_REGISTER_OUT_PRODUCTS);

        if (!registerInput && !registerOutput) {
            return;
        }

        for (String fieldName : newArrayList("recordOperationProductInComponents", "recordOperationProductOutComponents")) {
            if (productionRecord.getField(fieldName) != null) {
                return;
            }
        }

        if (PARAM_RECORDING_TYPE_CUMULATED.equals(typeOfProductionRecording)) {
            operationComponents = order.getTreeField(L_ORDER_OPERATION_COMPONENTS);
        } else if (PARAM_RECORDING_TYPE_FOREACH.equals(typeOfProductionRecording)) {
            operationComponents = newArrayList(productionRecord
                    .getBelongsToField(ProductionSchedulingConstants.MODEL_ORDER_OPERATION_COMPONENT));
        }

        if (registerInput) {
            copyOperationProductComponents(operationComponents, productionRecord, MODEL_RECORD_OPERATION_PRODUCT_IN_COMPONENT);
        }
        if (registerOutput) {
            copyOperationProductComponents(operationComponents, productionRecord, MODEL_RECORD_OPERATION_PRODUCT_OUT_COMPONENT);
        }
    }

    private void copyOperationProductComponents(final List<Entity> orderOperations, final Entity productionRecord,
            final String modelName) {
        if (checkIfOperationListIsEmpty(orderOperations)) {
            return;
        }

        DataDefinition recordProductDD = dataDefinitionService.get(ProductionCountingConstants.PLUGIN_IDENTIFIER, modelName);
        Map<Long, Entity> recordProductsMap = newHashMap();
        String productModel = "operationProductOutComponent";
        String recordProductFieldName = "recordOperationProductOutComponents";

        if (MODEL_RECORD_OPERATION_PRODUCT_IN_COMPONENT.equals(modelName)) {
            productModel = "operationProductInComponent";
            recordProductFieldName = "recordOperationProductInComponents";
        }

        for (Entity orderOperation : orderOperations) {
            Entity order = orderOperation.getBelongsToField(OrdersConstants.MODEL_ORDER);

            Map<Entity, BigDecimal> productQuantities = new HashMap<Entity, BigDecimal>();

            Map<Entity, BigDecimal> productComponentQuantities = productQuantitiesService
                    .getProductComponentQuantities(asList(order));

            Entity orderOperationComponent = productionRecord
                    .getBelongsToField(ProductionSchedulingConstants.MODEL_ORDER_OPERATION_COMPONENT);

            for (Entry<Entity, BigDecimal> prodCompQty : productComponentQuantities.entrySet()) {
                Entity product = prodCompQty.getKey().getBelongsToField(BasicConstants.MODEL_PRODUCT);

                if (orderOperationComponent != null) {
                    Entity operation = orderOperationComponent.getBelongsToField(TechnologiesConstants.MODEL_OPERATION);
                    Entity currentOperation = prodCompQty.getKey().getBelongsToField("operationComponent")
                            .getBelongsToField(TechnologiesConstants.MODEL_OPERATION);
                    if (!operation.getId().equals(currentOperation.getId())) {
                        continue;
                    }
                }

                if (productModel.equals(prodCompQty.getKey().getDataDefinition().getName())) {
                    BigDecimal qty = prodCompQty.getValue();
                    if (productQuantities.get(product) != null) {
                        qty = qty.add(productQuantities.get(product), numberService.getMathContext());
                    }
                    productQuantities.put(product, qty);
                }
            }

            for (Entry<Entity, BigDecimal> productQuantity : productQuantities.entrySet()) {
                Entity recordProduct = recordProductDD.create();
                recordProduct.setField(BasicConstants.MODEL_PRODUCT, productQuantity.getKey());
                recordProduct.setField(L_PLANNED_QUANTITY, productQuantity.getValue());
                recordProductsMap.put(productQuantity.getKey().getId(), recordProduct);
            }
        }

        productionRecord.setField(recordProductFieldName, newArrayList(recordProductsMap.values()));
    }

    public boolean checkIfOperationIsSet(final DataDefinition productionRecordDD, final Entity productionRecord) {
        String recordingMode = productionRecord.getBelongsToField(OrdersConstants.MODEL_ORDER).getStringField(
                L_TYPE_OF_PRODUCTION_RCORDING);
        Object orderOperation = productionRecord.getField(ProductionSchedulingConstants.MODEL_ORDER_OPERATION_COMPONENT);

        if (PARAM_RECORDING_TYPE_FOREACH.equals(recordingMode) && orderOperation == null) {
            productionRecord.addError(productionRecordDD.getField(ProductionSchedulingConstants.MODEL_ORDER_OPERATION_COMPONENT),
                    "productionCounting.record.messages.error.operationIsNotSet");
            return false;
        }
        return true;
    }

    public void countPlannedTimeAndBalance(final DataDefinition productionRecordDD, final Entity productionRecord) {
        Entity order = productionRecord.getBelongsToField(OrdersConstants.MODEL_ORDER);
        if (order == null) {
            return;
        }

        if (!order.getBooleanField("registerProductionTime")) {
            return;
        }
        String typeOfProductionRecording = order.getStringField(L_TYPE_OF_PRODUCTION_RCORDING);
        EntityTreeNode operationComponents = order.getTreeField(L_ORDER_OPERATION_COMPONENTS).getRoot();
        if (PARAM_RECORDING_TYPE_CUMULATED.equals(typeOfProductionRecording)) {
            countPlannedTime(productionRecord, operationComponents, null);
        } else if (PARAM_RECORDING_TYPE_FOREACH.equals(typeOfProductionRecording)) {
            countPlannedTime(productionRecord, operationComponents,
                    productionRecord.getBelongsToField(ProductionSchedulingConstants.MODEL_ORDER_OPERATION_COMPONENT));
        }
        countTimeBalance(productionRecord);
    }

    public final void changeStateToDefault(final DataDefinition productionRecordDD, final Entity productionRecord) {
        productionRecord.setField("state", ProductionCountingStates.DRAFT.getStringValue());
    }

    public final void fillShiftAndDivisionField(final ViewDefinitionState view, final ComponentState component,
            final String[] args) {
        fillShiftAndDivisionField(view);
    }

    public final void fillShiftAndDivisionField(final ViewDefinitionState view) {
        FieldComponent staffLookup = getFieldComponent(view, BasicConstants.MODEL_STAFF);
        FieldComponent shiftLookup = getFieldComponent(view, BasicConstants.MODEL_SHIFT);
        FieldComponent divisionLookup = getFieldComponent(view, MODEL_DIVISION);

        if (staffLookup.getFieldValue() == null) {
            shiftLookup.setFieldValue(null);
            return;
        }

        Long staffId = Long.valueOf(staffLookup.getFieldValue().toString());
        Entity staff = dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, BasicConstants.MODEL_STAFF).get(staffId);

        if (staff == null) {
            return;
        }

        Entity shift = staff.getBelongsToField(BasicConstants.MODEL_SHIFT);

        if (shift == null) {
            shiftLookup.setFieldValue(null);
        } else {
            shiftLookup.setFieldValue(shift.getId());
        }

        Entity division = staff.getBelongsToField(MODEL_DIVISION);

        if (division == null) {
            divisionLookup.setFieldValue(null);
        } else {
            divisionLookup.setFieldValue(division.getId());
        }
    }

    public final void fillDivisionField(final ViewDefinitionState view, final ComponentState component, final String[] args) {
        fillDivisionField(view);
    }

    public final void fillDivisionField(final ViewDefinitionState view) {
        FieldComponent workstationTypeLookup = getFieldComponent(view, "workstationType");
        FieldComponent divisionLookup = getFieldComponent(view, MODEL_DIVISION);

        if (workstationTypeLookup.getFieldValue() == null) {
            divisionLookup.setFieldValue(null);
            return;
        }

        Long workstationTypeId = Long.valueOf(workstationTypeLookup.getFieldValue().toString());
        Entity workstationType = dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER,
                BasicConstants.MODEL_WORKSTATION_TYPE).get(workstationTypeId);

        if (workstationType == null) {
            return;
        }

        Entity division = workstationType.getBelongsToField(MODEL_DIVISION);

        if (division == null) {
            divisionLookup.setFieldValue(null);
        } else {
            divisionLookup.setFieldValue(division.getId());
        }
    }

    private void countPlannedTime(final Entity productionRecord, final EntityTreeNode operationComponents,
            final Entity orderOperComp) {
        Map<String, BigDecimal> plannedTimeValues = new HashMap<String, BigDecimal>();
        plannedTimeValues.put(L_PLANNED_TIME, BigDecimal.ZERO);
        plannedTimeValues.put(L_PLANNED_MACHINE_TIME, BigDecimal.ZERO);
        plannedTimeValues.put(L_PLANNED_LABOR_TIME, BigDecimal.ZERO);
        Entity order = productionRecord.getBelongsToField(OrdersConstants.MODEL_ORDER);
        BigDecimal plannedQuantity = (BigDecimal) order.getField(L_PLANNED_QUANTITY);
        Map<Entity, Integer> durationOperation = orderRealizationTimeService.estimateRealizationTimes(order, plannedQuantity,
                true);
        if (orderOperComp == null) {
            EntityTree orderOperationComponentsTree = order.getTreeField(L_ORDER_OPERATION_COMPONENTS);
            for (Entity orderOperationComponent : orderOperationComponentsTree) {
                countTimeOperation(orderOperationComponent, plannedTimeValues, durationOperation.get(orderOperationComponent
                        .getBelongsToField(TechnologiesConstants.MODEL_TECHNOLOGY_OPERATION_COMPONENT)));
            }
        } else {
            countTimeOperation(orderOperComp, plannedTimeValues, durationOperation.get(orderOperComp
                    .getBelongsToField(TechnologiesConstants.MODEL_TECHNOLOGY_OPERATION_COMPONENT)));
        }
        productionRecord.setField(L_PLANNED_TIME, plannedTimeValues.get(L_PLANNED_TIME).setScale(0, ROUND_UP).intValue());
        productionRecord.setField(L_PLANNED_MACHINE_TIME, plannedTimeValues.get(L_PLANNED_MACHINE_TIME).setScale(0, ROUND_UP)
                .intValue());
        productionRecord.setField(L_PLANNED_LABOR_TIME, plannedTimeValues.get(L_PLANNED_LABOR_TIME).setScale(0, ROUND_UP)
                .intValue());
    }

    private void countTimeOperation(final Entity orderOperationComponent, Map<String, BigDecimal> plannedTimeValues,
            final Integer durationOfOperation) {
        BigDecimal durationOfOperationComponent = new BigDecimal(durationOfOperation);
        BigDecimal plannedTime = plannedTimeValues.get(L_PLANNED_TIME).add(durationOfOperationComponent);
        plannedTimeValues.put(L_PLANNED_TIME, plannedTime);

        BigDecimal plannedMachineTime = plannedTimeValues.get(L_PLANNED_MACHINE_TIME).add(
                durationOfOperationComponent.multiply(getBigDecimal(orderOperationComponent.getField("machineUtilization")),
                        numberService.getMathContext()), numberService.getMathContext());
        plannedTimeValues.put(L_PLANNED_MACHINE_TIME, plannedMachineTime);
        BigDecimal plannedLaborTime = plannedTimeValues.get(L_PLANNED_LABOR_TIME).add(
                durationOfOperationComponent.multiply(getBigDecimal(orderOperationComponent.getField("laborUtilization")),
                        numberService.getMathContext()), numberService.getMathContext());
        plannedTimeValues.put(L_PLANNED_LABOR_TIME, plannedLaborTime);
    }

    private void countTimeBalance(final Entity productionRecord) {
        BigDecimal plannedMachineTime = getBigDecimal(productionRecord.getField(L_PLANNED_MACHINE_TIME));
        BigDecimal plannedLaborTime = getBigDecimal(productionRecord.getField(L_PLANNED_LABOR_TIME));
        BigDecimal machineTime = getBigDecimal(productionRecord.getField("machineTime"));
        BigDecimal laborTime = getBigDecimal(productionRecord.getField("laborTime"));

        productionRecord.setField("machineTimeBalance", machineTime.subtract(plannedMachineTime, numberService.getMathContext())
                .setScale(0, ROUND_UP).intValue());
        productionRecord.setField("laborTimeBalance", laborTime.subtract(plannedLaborTime, numberService.getMathContext())
                .setScale(0, ROUND_UP).intValue());
    }

    private static boolean checkIfOperationListIsEmpty(final List<Entity> orderOperations) {
        return orderOperations == null || orderOperations.isEmpty() || orderOperations.get(0) == null;
    }

    private FieldComponent getFieldComponent(final ViewDefinitionState view, final String name) {
        return (FieldComponent) view.getComponentByReference(name);
    }

    public static BigDecimal getBigDecimal(final Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return BigDecimal.valueOf(Double.valueOf(value.toString()));
    }

    public static Integer getInteger(final Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return Integer.valueOf(value.toString());
    }

    public static Boolean getBooleanValue(final Object fieldValue) {
        return fieldValue instanceof Boolean && (Boolean) fieldValue;
    }
}
