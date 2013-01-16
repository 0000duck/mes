/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.2.0-SNAPSHOT
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
package com.qcadoo.mes.productionScheduling;

import static com.qcadoo.mes.orders.constants.OrderFields.DATE_FROM;
import static com.qcadoo.mes.orders.constants.OrderFields.DATE_TO;
import static com.qcadoo.mes.orders.constants.OrderFields.EFFECTIVE_DATE_FROM;
import static com.qcadoo.mes.orders.constants.OrderFields.EFFECTIVE_DATE_TO;
import static com.qcadoo.mes.orders.constants.OrderFields.START_DATE;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.qcadoo.mes.basic.ShiftsServiceImpl;
import com.qcadoo.mes.operationTimeCalculations.OperationWorkTime;
import com.qcadoo.mes.operationTimeCalculations.OperationWorkTimeService;
import com.qcadoo.mes.operationTimeCalculations.OrderRealizationTimeService;
import com.qcadoo.mes.orders.constants.OrdersConstants;
import com.qcadoo.mes.productionLines.constants.ProductionLinesConstants;
import com.qcadoo.mes.technologies.ProductQuantitiesService;
import com.qcadoo.mes.technologies.constants.TechnologiesConstants;
import com.qcadoo.mes.technologies.constants.TechnologyFields;
import com.qcadoo.mes.technologies.states.constants.TechnologyState;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ComponentState.MessageType;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.utils.TimeConverterService;

@Service
public class OrderTimePredictionService {

    private static final String PRODUCTION_SCHEDULING_ERROR_FIELD_REQUIRED = "productionScheduling.error.fieldRequired";

    private static final String TECHNOLOGY_COMPONENT = "technology";

    private static final String QUANTITY_COMPONENT = "quantity";

    @Autowired
    private OrderRealizationTimeService orderRealizationTimeService;

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private ShiftsServiceImpl shiftsService;

    @Autowired
    private ProductQuantitiesService productQuantitiesService;

    @Autowired
    private TimeConverterService timeConverterService;

    @Autowired
    private OperationWorkTimeService operationWorkTimeService;

    private void scheduleOrder(final Long orderId) {
        Entity order = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(orderId);

        if (order == null) {
            return;
        }
        DataDefinition dataDefinition = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                TechnologiesConstants.MODEL_TECHNOLOGY_INSTANCE_OPERATION_COMPONENT);

        List<Entity> operations = dataDefinition.find().add(SearchRestrictions.belongsTo(OrdersConstants.MODEL_ORDER, order))
                .list().getEntities();

        Date orderStartDate = (Date) order.getField(START_DATE);
        for (Entity operation : operations) {
            Integer offset = (Integer) operation.getField("operationOffSet");
            Integer duration = (Integer) operation.getField("effectiveOperationRealizationTime");

            operation.setField(EFFECTIVE_DATE_FROM, null);
            operation.setField(EFFECTIVE_DATE_TO, null);

            if (offset == null || duration == null || duration.equals(0)) {
                continue;
            }
            Date dateFrom = shiftsService.findDateToForOrder(orderStartDate, offset + 1);
            if (dateFrom == null) {
                continue;
            }
            Date dateTo = shiftsService.findDateToForOrder(orderStartDate, offset + duration);
            if (dateTo == null) {
                continue;
            }
            operation.setField(EFFECTIVE_DATE_FROM, dateFrom);
            operation.setField(EFFECTIVE_DATE_TO, dateTo);
        }
        // TODO ALBR
        for (Entity operation : operations) {
            dataDefinition.save(operation);
        }
    }

    public void copyRealizationTime(final ViewDefinitionState viewDefinitionState, final ComponentState state, final String[] args) {
        FieldComponent generatedEndDate = (FieldComponent) viewDefinitionState.getComponentByReference("generatedEndDate");
        FieldComponent endDate = (FieldComponent) viewDefinitionState.getComponentByReference("stopTime");
        endDate.setFieldValue(generatedEndDate.getFieldValue());

        state.performEvent(viewDefinitionState, "save", new String[0]);
    }

    @Transactional
    public void generateRealizationTime(final ViewDefinitionState viewDefinitionState, final ComponentState state,
            final String[] args) {
        FormComponent form = (FormComponent) viewDefinitionState.getComponentByReference("form");
        FieldComponent startTimeField = (FieldComponent) viewDefinitionState.getComponentByReference("startTime");
        if (!StringUtils.hasText((String) startTimeField.getFieldValue())) {
            startTimeField.addMessage("productionScheduling.error.fieldRequired", MessageType.FAILURE);
            return;
        }
        FieldComponent plannedQuantity = (FieldComponent) viewDefinitionState.getComponentByReference("plannedQuantity");
        FieldComponent productionLineLookup = (FieldComponent) viewDefinitionState.getComponentByReference("productionLine");
        FieldComponent generatedEndDate = (FieldComponent) viewDefinitionState.getComponentByReference("generatedEndDate");
        FieldComponent dateTo = (FieldComponent) viewDefinitionState.getComponentByReference("stopTime");
        Entity productionLine = dataDefinitionService.get(ProductionLinesConstants.PLUGIN_IDENTIFIER,
                ProductionLinesConstants.MODEL_PRODUCTION_LINE).get((Long) productionLineLookup.getFieldValue());

        Entity order = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(
                form.getEntity().getId());
        Entity technology = order.getBelongsToField("technology");
        Validate.notNull(technology, "technology is null");
        BigDecimal quantity = orderRealizationTimeService.getBigDecimalFromField(plannedQuantity.getFieldValue(),
                viewDefinitionState.getLocale());

        Boolean includeTpz = "1".equals(viewDefinitionState.getComponentByReference("includeTpz").getFieldValue());
        Boolean includeAdditionalTime = "1".equals(viewDefinitionState.getComponentByReference("includeAdditionalTime")
                .getFieldValue());

        Map<Entity, BigDecimal> operationRuns = new HashMap<Entity, BigDecimal>();
        productQuantitiesService.getProductComponentQuantities(technology, quantity, operationRuns);

        OperationWorkTime workTime = operationWorkTimeService.estimateTotalWorkTimeForOrder(order, operationRuns, includeTpz,
                includeAdditionalTime, productionLine, true);
        fillWorkTimeFields(viewDefinitionState, workTime);

        order = getActualOrderWithChanges(order);
        int maxPathTime = orderRealizationTimeService.estimateMaxOperationTimeConsumptionForWorkstation(
                order.getTreeField("technologyInstanceOperationComponents").getRoot(), quantity, includeTpz,
                includeAdditionalTime, productionLine);

        if (maxPathTime > OrderRealizationTimeService.MAX_REALIZATION_TIME) {
            state.addMessage("orders.validate.global.error.RealizationTimeIsToLong", MessageType.FAILURE);
            generatedEndDate.setFieldValue(null);
            generatedEndDate.requestComponentUpdateState();
        } else {
            order.setField("realizationTime", maxPathTime);
            Date startTime = (Date) order.getField(DATE_FROM);
            Date stopTime = (Date) order.getField(DATE_TO);
            if (startTime == null) {
                startTimeField.addMessage("orders.validate.global.error.dateFromIsNull", MessageType.FAILURE);
            } else {
                Date generatedStopTime = shiftsService.findDateToForOrder(startTime, maxPathTime);
                if (generatedStopTime == null) {
                    form.addMessage("productionScheduling.timenorms.isZero", MessageType.FAILURE, false);
                } else {
                    if (stopTime == null) {
                        generatedEndDate.setFieldValue(orderRealizationTimeService.setDateToField(generatedStopTime));
                    }
                    order.setField("generatedEndDate", orderRealizationTimeService.setDateToField(generatedStopTime));
                    scheduleOrder(order.getId());
                }
                generatedEndDate.requestComponentUpdateState();
            }
            order.getDataDefinition().save(order);
        }
    }

    private void fillWorkTimeFields(final ViewDefinitionState view, final OperationWorkTime workTime) {
        FieldComponent laborWorkTime = (FieldComponent) view.getComponentByReference("laborWorkTime");
        FieldComponent machineWorkTime = (FieldComponent) view.getComponentByReference("machineWorkTime");
        laborWorkTime.setFieldValue(workTime.getLaborWorkTime());
        machineWorkTime.setFieldValue(workTime.getMachineWorkTime());
        laborWorkTime.requestComponentUpdateState();
        machineWorkTime.requestComponentUpdateState();
    }

    private Entity getActualOrderWithChanges(final Entity entity) {
        return dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(entity.getId());
    }

    public Date getDateFromOrdersFromOperation(final List<Entity> operations) {
        Date beforeOperation = null;
        for (Entity operation : operations) {
            Date operationDateFrom = (Date) operation.getField(EFFECTIVE_DATE_FROM);
            if (operationDateFrom != null) {
                if (beforeOperation == null) {
                    beforeOperation = operationDateFrom;
                }
                if (operationDateFrom.compareTo(beforeOperation) == -1) {
                    beforeOperation = operationDateFrom;
                }
            }
        }
        return beforeOperation;
    }

    public Date getDateToOrdersFromOperation(final List<Entity> operations) {
        Date laterOperation = null;
        for (Entity operation : operations) {
            Date operationDateTo = (Date) operation.getField(EFFECTIVE_DATE_TO);
            if (operationDateTo != null) {
                if (laterOperation == null) {
                    laterOperation = operationDateTo;
                }
                if (operationDateTo.compareTo(laterOperation) == 1) {
                    laterOperation = operationDateTo;
                }
            }
        }
        return laterOperation;
    }

    @Transactional
    public void changeRealizationTime(final ViewDefinitionState viewDefinitionState, final ComponentState state,
            final String[] args) {

        FieldComponent technologyLookup = (FieldComponent) viewDefinitionState.getComponentByReference(TECHNOLOGY_COMPONENT);
        FieldComponent plannedQuantity = (FieldComponent) viewDefinitionState.getComponentByReference(QUANTITY_COMPONENT);
        FieldComponent dateFrom = (FieldComponent) viewDefinitionState.getComponentByReference(DATE_FROM);
        FieldComponent dateTo = (FieldComponent) viewDefinitionState.getComponentByReference(DATE_TO);
        FieldComponent productionLineLookup = (FieldComponent) viewDefinitionState.getComponentByReference("productionLine");

        if (technologyLookup.getFieldValue() == null) {
            technologyLookup.addMessage(PRODUCTION_SCHEDULING_ERROR_FIELD_REQUIRED, MessageType.FAILURE);
            return;
        }
        if (!StringUtils.hasText((String) dateFrom.getFieldValue())) {
            dateFrom.addMessage(PRODUCTION_SCHEDULING_ERROR_FIELD_REQUIRED, MessageType.FAILURE);
            return;
        }
        if (!StringUtils.hasText((String) plannedQuantity.getFieldValue())) {
            plannedQuantity.addMessage(PRODUCTION_SCHEDULING_ERROR_FIELD_REQUIRED, MessageType.FAILURE);
            return;
        }
        if (productionLineLookup.getFieldValue() == null) {
            productionLineLookup.addMessage(PRODUCTION_SCHEDULING_ERROR_FIELD_REQUIRED, MessageType.FAILURE);
            return;
        }
        BigDecimal quantity = orderRealizationTimeService.getBigDecimalFromField(plannedQuantity.getFieldValue(),
                viewDefinitionState.getLocale());

        if (quantity.intValue() < 0) {
            plannedQuantity.addMessage(PRODUCTION_SCHEDULING_ERROR_FIELD_REQUIRED, MessageType.FAILURE);
            return;
        }

        int maxPathTime = 0;

        Entity technology = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                TechnologiesConstants.MODEL_TECHNOLOGY).get((Long) technologyLookup.getFieldValue());
        Validate.notNull(technology, "technology is null");
        if (technology.getStringField(TechnologyFields.STATE).equals(TechnologyState.DRAFT.getStringValue())
                || technology.getStringField(TechnologyFields.STATE).equals(TechnologyState.OUTDATED.getStringValue())) {
            technologyLookup.addMessage("productionScheduling.technology.incorrectState", MessageType.FAILURE);
            return;
        }
        FieldComponent laborWorkTime = (FieldComponent) viewDefinitionState.getComponentByReference("laborWorkTime");
        FieldComponent machineWorkTime = (FieldComponent) viewDefinitionState.getComponentByReference("machineWorkTime");

        Boolean includeTpz = "1".equals(viewDefinitionState.getComponentByReference("includeTpz").getFieldValue());
        Boolean includeAdditionalTime = "1".equals(viewDefinitionState.getComponentByReference("includeAdditionalTime")
                .getFieldValue());

        Entity productionLine = dataDefinitionService.get(ProductionLinesConstants.PLUGIN_IDENTIFIER,
                ProductionLinesConstants.MODEL_PRODUCTION_LINE).get((Long) productionLineLookup.getFieldValue());
        Map<Entity, BigDecimal> operationRuns = new HashMap<Entity, BigDecimal>();
        productQuantitiesService.getProductComponentQuantities(technology, quantity, operationRuns);

        boolean saved = true;
        OperationWorkTime workTime = operationWorkTimeService.estimateTotalWorkTimeForTechnology(technology, operationRuns,
                includeTpz, includeAdditionalTime, productionLine, saved);

        laborWorkTime.setFieldValue(workTime.getLaborWorkTime());
        machineWorkTime.setFieldValue(workTime.getMachineWorkTime());

        maxPathTime = orderRealizationTimeService.estimateOperationTimeConsumption(technology.getTreeField("operationComponents")
                .getRoot(), quantity, productionLine);

        if (maxPathTime > OrderRealizationTimeService.MAX_REALIZATION_TIME) {
            state.addMessage("orders.validate.global.error.RealizationTimeIsToLong", MessageType.FAILURE);
            dateTo.setFieldValue(null);
        } else {
            Date startTime = timeConverterService.getDateTimeFromField(dateFrom.getFieldValue());
            Date stopTime = shiftsService.findDateToForOrder(startTime, maxPathTime);

            if (stopTime != null) {
                startTime = shiftsService.findDateFromForOrder(stopTime, maxPathTime);
            }
            if (startTime == null) {
                dateFrom.setFieldValue(null);
            } else {
                dateFrom.setFieldValue(orderRealizationTimeService.setDateToField(startTime));
            }
            if (stopTime == null) {
                dateTo.setFieldValue(null);
            } else {
                dateTo.setFieldValue(orderRealizationTimeService.setDateToField(stopTime));
            }
        }
        laborWorkTime.requestComponentUpdateState();
        machineWorkTime.requestComponentUpdateState();
        dateFrom.requestComponentUpdateState();
        dateTo.requestComponentUpdateState();
    }

    public void fillUnitField(final ViewDefinitionState viewDefinitionState) {
        FormComponent form = (FormComponent) viewDefinitionState.getComponentByReference("form");
        FieldComponent unitField = (FieldComponent) viewDefinitionState.getComponentByReference("operationDurationQuantityUNIT");

        Entity order = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(
                form.getEntity().getId());
        Entity product = order.getBelongsToField("product");

        unitField.setFieldValue(product.getField("unit"));
    }

    public void fillUnitField(final ViewDefinitionState viewDefinitionState, final ComponentState state, final String[] args) {
        FieldComponent unitField = (FieldComponent) viewDefinitionState.getComponentByReference("operationDurationQuantityUNIT");

        FieldComponent technologyLookup = (FieldComponent) viewDefinitionState.getComponentByReference("technology");

        if (technologyLookup.getFieldValue() != null) {
            Entity technology = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                    TechnologiesConstants.MODEL_TECHNOLOGY).get((Long) technologyLookup.getFieldValue());

            Entity product = technology.getBelongsToField("product");

            unitField.setFieldValue(product.getField("unit"));
        }
    }

    public void clearFieldValue(final ViewDefinitionState viewDefinitionState, final ComponentState state, final String[] args) {
        state.setFieldValue(null);
    }

}
