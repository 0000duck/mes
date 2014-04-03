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
package com.qcadoo.mes.productionCounting.states.listener;

import static com.qcadoo.mes.basicProductionCounting.constants.BasicProductionCountingFields.ORDER;
import static com.qcadoo.mes.orders.constants.OrderFields.STATE;
import static com.qcadoo.mes.orders.states.constants.OrderState.COMPLETED;
<<<<<<< HEAD:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionTrackingListenerService.java
=======
import static com.qcadoo.mes.productionCounting.internal.constants.OrderFieldsPC.TYPE_OF_PRODUCTION_RECORDING;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionRecordFields.*;
import static com.qcadoo.mes.productionCounting.internal.constants.TypeOfProductionRecording.FOR_EACH;
import static com.qcadoo.model.api.search.SearchRestrictions.*;
>>>>>>> master:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionRecordBasicListenerService.java

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.mes.basicProductionCounting.BasicProductionCountingService;
<<<<<<< HEAD:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionTrackingListenerService.java
import com.qcadoo.mes.basicProductionCounting.constants.BasicProductionCountingFields;
import com.qcadoo.mes.basicProductionCounting.constants.OrderFieldsBPC;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.orders.states.aop.OrderStateChangeAspect;
import com.qcadoo.mes.orders.states.constants.OrderState;
import com.qcadoo.mes.productionCounting.ProductionCountingService;
import com.qcadoo.mes.productionCounting.constants.OrderFieldsPC;
import com.qcadoo.mes.productionCounting.constants.ProductionTrackingFields;
import com.qcadoo.mes.productionCounting.constants.TrackingOperationProductInComponentFields;
import com.qcadoo.mes.productionCounting.constants.TrackingOperationProductOutComponentFields;
import com.qcadoo.mes.productionCounting.states.constants.ProductionTrackingStateStringValues;
=======
import com.qcadoo.mes.basicProductionCounting.constants.BasicProductionCountingConstants;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.orders.states.aop.OrderStateChangeAspect;
import com.qcadoo.mes.orders.states.constants.OrderState;
import com.qcadoo.mes.productionCounting.internal.constants.ProductionRecordFields;
import com.qcadoo.mes.productionCounting.states.constants.ProductionRecordState;
import com.qcadoo.mes.productionCounting.states.constants.ProductionRecordStateChangeFields;
>>>>>>> master:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionRecordBasicListenerService.java
import com.qcadoo.mes.productionCounting.utils.OrderClosingHelper;
import com.qcadoo.mes.states.StateChangeContext;
import com.qcadoo.mes.states.constants.StateChangeStatus;
import com.qcadoo.mes.states.messages.constants.StateMessageType;
import com.qcadoo.mes.states.service.StateChangeContextBuilder;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityList;
import com.qcadoo.model.api.NumberService;
import com.qcadoo.model.api.search.SearchCriteriaBuilder;
import com.qcadoo.model.api.search.SearchOrders;
import com.qcadoo.model.api.search.SearchProjections;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.model.api.validators.ErrorMessage;

@Service
public final class ProductionTrackingListenerService {

    private static final String L_USED_QUANTITY = "usedQuantity";

    private static final String L_PRODUCT = "product";

    @Autowired
    private NumberService numberService;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private ProductionCountingService productionCountingService;

    @Autowired
    private BasicProductionCountingService basicProductionCountingService;

    @Autowired
    private OrderStateChangeAspect orderStateChangeAspect;

    @Autowired
    private StateChangeContextBuilder stateChangeContextBuilder;

    @Autowired
    private OrderClosingHelper orderClosingHelper;

<<<<<<< HEAD:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionTrackingListenerService.java
    public void onChangeFromDraftToAny(final StateChangeContext stateChangeContext) {
        Entity productionTracking = stateChangeContext.getOwner();
        productionTracking.setField(ProductionTrackingFields.LAST_STATE_CHANGE_FAILS, false);
        productionTracking.setField(ProductionTrackingFields.LAST_STATE_CHANGE_FAIL_CAUSE, null);
        stateChangeContext.setOwner(productionTracking);
=======
    @Autowired
    private BasicProductionCountingService basicProductionCountingService;

    public void onLeavingDraft(final StateChangeContext stateChangeContext) {
        Entity productionRecord = stateChangeContext.getOwner();
        productionRecord.setField(ProductionRecordFields.LAST_STATE_CHANGE_FAILS, false);
        productionRecord.setField(ProductionRecordFields.LAST_STATE_CHANGE_FAIL_CAUSE, null);
        stateChangeContext.setOwner(productionRecord);
>>>>>>> master:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionRecordBasicListenerService.java
    }

    public void validationOnAccept(final StateChangeContext stateChangeContext) {
        checkIfRecordOperationProductComponentsWereFilled(stateChangeContext);
        checkIfExistsFinalRecord(stateChangeContext);
    }

    public void onAccept(final StateChangeContext stateChangeContext) {
        final Entity productionTracking = stateChangeContext.getOwner();
        updateBasicProductionCounting(productionTracking, new Addition());
        setOrderDoneQuantity(productionTracking);
        closeOrder(stateChangeContext);
    }

    public void onChangeFromAcceptedToDeclined(final StateChangeContext stateChangeContext) {
<<<<<<< HEAD:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionTrackingListenerService.java
        final Entity productionTracking = stateChangeContext.getOwner();
        updateBasicProductionCounting(productionTracking, new Substraction());
        setOrderDoneQuantity(productionTracking);
=======
        final Entity productionRecord = stateChangeContext.getOwner();
        updateBasicProductionCounting(productionRecord, new Subtraction());
        setOrderDoneQuantity(productionRecord);
>>>>>>> master:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionRecordBasicListenerService.java
    }

    private void checkIfRecordOperationProductComponentsWereFilled(final StateChangeContext stateChangeContext) {
        final Entity productionTracking = stateChangeContext.getOwner();

        if (!checkIfUsedQuantitiesWereFilled(productionTracking,
                ProductionTrackingFields.TRACKING_OPERATION_PRODUCT_IN_COMPONENTS)
                && !checkIfUsedQuantitiesWereFilled(productionTracking,
                        ProductionTrackingFields.TRACKING_OPERATION_PRODUCT_OUT_COMPONENTS)) {
            stateChangeContext
                    .addValidationError("productionCounting.productionTracking.messages.error.recordOperationProductComponentsNotFilled");
        }
    }

<<<<<<< HEAD:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionTrackingListenerService.java
    private boolean checkIfUsedQuantitiesWereFilled(final Entity productionTracking, final String modelName) {
        final SearchCriteriaBuilder searchBuilder = productionTracking.getHasManyField(modelName).find()
                .add(SearchRestrictions.isNotNull(L_USED_QUANTITY));

        return (searchBuilder.list().getTotalNumberOfEntities() != 0);
    }

    private void checkIfTimesWereFilled(final StateChangeContext stateChangeContext) {
        final Entity productionTracking = stateChangeContext.getOwner();
        Integer machineTime = productionTracking.getIntegerField(ProductionTrackingFields.MACHINE_TIME);
        Integer laborTime = productionTracking.getIntegerField(ProductionTrackingFields.LABOR_TIME);

        if ((machineTime == null) || (laborTime == null)) {
            stateChangeContext.addValidationError("productionCounting.productionTracking.messages.error.timesNotFilled");
=======
    private boolean checkIfUsedQuantitiesWereFilled(final Entity productionRecord, final String productComponentsFieldName) {
        EntityList productComponents = productionRecord.getHasManyField(productComponentsFieldName);
        if (productComponents.isEmpty()) {
            return true;
>>>>>>> master:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionRecordBasicListenerService.java
        }
        final SearchCriteriaBuilder searchBuilder = productComponents.find().add(SearchRestrictions.isNotNull(L_USED_QUANTITY));
        return (searchBuilder.list().getTotalNumberOfEntities() != 0);
    }

<<<<<<< HEAD:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionTrackingListenerService.java
    public void checkIfExistsFinalRecord(final StateChangeContext stateChangeContext) {
        final Entity productionTracking = stateChangeContext.getOwner();
        final Entity order = productionTracking.getBelongsToField(ProductionTrackingFields.ORDER);
        final String typeOfProductionRecording = order.getStringField(OrderFieldsPC.TYPE_OF_PRODUCTION_RECORDING);

        final SearchCriteriaBuilder searchBuilder = productionTracking.getDataDefinition().find();
        searchBuilder.add(SearchRestrictions.eq(ProductionTrackingFields.STATE, ProductionTrackingStateStringValues.ACCEPTED));
        searchBuilder.add(SearchRestrictions.belongsTo(ProductionTrackingFields.ORDER, order));
        searchBuilder.add(SearchRestrictions.eq(ProductionTrackingFields.LAST_TRACKING, true));

        if (productionCountingService.isTypeOfProductionRecordingForEach(typeOfProductionRecording)) {
            searchBuilder.add(SearchRestrictions.belongsTo(ProductionTrackingFields.TECHNOLOGY_OPERATION_COMPONENT,
                    productionTracking.getBelongsToField(ProductionTrackingFields.TECHNOLOGY_OPERATION_COMPONENT)));
        }

        if (searchBuilder.list().getTotalNumberOfEntities() != 0) {
            stateChangeContext.addValidationError("productionCounting.productionTracking.messages.error.finalExists");
=======
    private void checkIfExistsFinalRecord(final StateChangeContext stateChangeContext) {
        final Entity productionRecord = stateChangeContext.getOwner();
        final Entity order = productionRecord.getBelongsToField(ORDER);
        final SearchCriteriaBuilder searchBuilder = productionRecord.getDataDefinition().find();

        // check if record is accepted or during acceptation
        searchBuilder.createCriteria(ProductionRecordFields.STATE_CHANGES, "sc")
                .add(eq(ProductionRecordStateChangeFields.TARGET_STATE, ProductionRecordState.ACCEPTED.getStringValue()))
                .add(ne(ProductionRecordStateChangeFields.STATUS, StateChangeStatus.FAILURE.getStringValue()))
                .add(ne(ProductionRecordStateChangeFields.STATUS, StateChangeStatus.CANCELED.getStringValue()));

        // omit current production record
        searchBuilder.add(idNe(productionRecord.getId()));
        searchBuilder.add(SearchRestrictions.belongsTo(ORDER, order));
        searchBuilder.add(eq(LAST_RECORD, true));

        final String typeOfProductionRecording = order.getStringField(TYPE_OF_PRODUCTION_RECORDING);
        if (FOR_EACH.getStringValue().equals(typeOfProductionRecording)) {
            searchBuilder.add(SearchRestrictions.belongsTo(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT,
                    productionRecord.getBelongsToField(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT)));
        }

        searchBuilder.setProjection(SearchProjections.alias(SearchProjections.count("id"), "count"));
        // Fix for (P)SQLException on missing id column.
        searchBuilder.addOrder(SearchOrders.desc("count"));
        Entity projectionResults = searchBuilder.setMaxResults(1).uniqueResult();
        Long countValue = (Long) projectionResults.getField("count");
        if (countValue > 0) {
            stateChangeContext.addValidationError("productionCounting.productionRecord.messages.error.finalExists");
>>>>>>> master:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionRecordBasicListenerService.java
        }
    }

    public void closeOrder(final StateChangeContext stateChangeContext) {
        final Entity productionTracking = stateChangeContext.getOwner();
        final Entity order = productionTracking.getBelongsToField(ORDER);

        if (!orderClosingHelper.orderShouldBeClosed(productionTracking)) {
            return;
        }
        if (order.getStringField(STATE).equals(COMPLETED.getStringValue())) {
            stateChangeContext.addMessage("productionCounting.order.orderIsAlreadyClosed", StateMessageType.INFO, false);
            return;
        }
        final StateChangeContext orderStateChangeContext = stateChangeContextBuilder.build(
                orderStateChangeAspect.getChangeEntityDescriber(), order, OrderState.COMPLETED.getStringValue());
        orderStateChangeAspect.changeState(orderStateChangeContext);
        Entity orderFromDB = order.getDataDefinition().get(orderStateChangeContext.getOwner().getId());
        if (orderFromDB.getStringField(STATE).equals(COMPLETED.getStringValue())) {
            stateChangeContext.addMessage("productionCounting.order.orderClosed", StateMessageType.INFO, false);
        } else if (StateChangeStatus.PAUSED.equals(orderStateChangeContext.getStatus())) {
            stateChangeContext.addMessage("productionCounting.order.orderWillBeClosedAfterExtSync", StateMessageType.INFO, false);
        } else {
            stateChangeContext.addMessage("productionCounting.order.orderCannotBeClosed", StateMessageType.FAILURE, false);

            List<ErrorMessage> errors = Lists.newArrayList();

            if (!orderFromDB.getErrors().isEmpty()) {
                errors.addAll(order.getErrors().values());
            }
            if (!orderFromDB.getGlobalErrors().isEmpty()) {
                errors.addAll(order.getGlobalErrors());
            }

            if (!errors.isEmpty()) {
                StringBuilder errorMessages = new StringBuilder();

                for (ErrorMessage errorMessage : errors) {
                    String translatedErrorMessage = translationService.translate(errorMessage.getMessage(), Locale.getDefault(),
                            errorMessage.getVars());
                    errorMessages.append(translatedErrorMessage);
                    errorMessages.append(", ");
                }

                stateChangeContext.addValidationError("orders.order.orderStates.error", errorMessages.toString());
            }
        }
    }

<<<<<<< HEAD:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionTrackingListenerService.java
    private void setOrderDoneQuantity(final Entity productionTracking) {
        Entity order = productionTracking.getBelongsToField(ProductionTrackingFields.ORDER);

        order.setField(OrderFields.DONE_QUANTITY,
                basicProductionCountingService.getProducedQuantityFromBasicProductionCountings(order));

        order.getDataDefinition().save(order);
    }

    private void updateBasicProductionCounting(final Entity productionTracking, final Operation operation) {
        final Entity order = productionTracking.getBelongsToField(ProductionTrackingFields.ORDER);

        final List<Entity> basicProductionCountings = order.getHasManyField(OrderFieldsBPC.BASIC_PRODUCTION_COUNTINGS);

        final List<Entity> trackingOperationProductInComponents = productionTracking
                .getHasManyField(ProductionTrackingFields.TRACKING_OPERATION_PRODUCT_IN_COMPONENTS);
        final List<Entity> trackingOperationProductOutComponents = productionTracking
                .getHasManyField(ProductionTrackingFields.TRACKING_OPERATION_PRODUCT_OUT_COMPONENTS);

        for (Entity trackingOperationProductInComponent : trackingOperationProductInComponents) {
            Entity basicProductionCounting;

            try {
                basicProductionCounting = getBasicProductionCounting(trackingOperationProductInComponent,
                        basicProductionCountings);
            } catch (IllegalStateException e) {
                continue;
            }

            final BigDecimal usedQuantity = basicProductionCounting.getDecimalField(BasicProductionCountingFields.USED_QUANTITY);
            final BigDecimal productQuantity = trackingOperationProductInComponent
                    .getDecimalField(TrackingOperationProductInComponentFields.USED_QUANTITY);
            final BigDecimal result = operation.perform(usedQuantity, productQuantity);

            basicProductionCounting.setField(BasicProductionCountingFields.USED_QUANTITY, result);
            basicProductionCounting = basicProductionCounting.getDataDefinition().save(basicProductionCounting);
        }

        for (Entity trackingOperationProductOutComponent : trackingOperationProductOutComponents) {
            Entity productionCounting;

            try {
                productionCounting = getBasicProductionCounting(trackingOperationProductOutComponent, basicProductionCountings);
            } catch (IllegalStateException e) {
                continue;
            }

            final BigDecimal usedQuantity = productionCounting.getDecimalField(BasicProductionCountingFields.PRODUCED_QUANTITY);
            final BigDecimal productQuantity = trackingOperationProductOutComponent
                    .getDecimalField(TrackingOperationProductOutComponentFields.USED_QUANTITY);
            final BigDecimal result = operation.perform(usedQuantity, productQuantity);

            productionCounting.setField(BasicProductionCountingFields.PRODUCED_QUANTITY, result);
            productionCounting = productionCounting.getDataDefinition().save(productionCounting);
        }
    }

    private Entity getBasicProductionCounting(final Entity trackingOperationProductComponent,
            final List<Entity> basicProductionCountings) {
        Entity product = trackingOperationProductComponent.getBelongsToField(L_PRODUCT);

        for (Entity basicProductionCounting : basicProductionCountings) {
            if (basicProductionCounting.getBelongsToField(BasicProductionCountingFields.PRODUCT).getId().equals(product.getId())) {
                return basicProductionCounting;
=======
    private void setOrderDoneQuantity(final Entity productionRecord) {
        Entity order = productionRecord.getBelongsToField(ProductionRecordFields.ORDER);

        order.setField(OrderFields.DONE_QUANTITY,
                basicProductionCountingService.getProducedQuantityFromBasicProductionCountings(order));

        order.getDataDefinition().save(order);
    }

    // FIXME MAKU It could be replaced with just a single db query..
    private Entity getBasicProductionCounting(final Entity productIn, final List<Entity> productionCountings) {
        Entity product = productIn.getBelongsToField(PRODUCT);
        for (Entity productionCounting : productionCountings) {
            if (productionCounting.getBelongsToField(PRODUCT).getId().equals(product.getId())) {
                return productionCounting;
>>>>>>> master:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionRecordBasicListenerService.java
            }
        }
        return null;
    }

    private interface Operation {

        BigDecimal perform(BigDecimal argument1, BigDecimal argument2);
    }

    private class Addition implements Operation {

        @Override
        public BigDecimal perform(final BigDecimal orginalValue, final BigDecimal addition) {
            BigDecimal value;
            BigDecimal add;
            if (orginalValue == null) {
                value = BigDecimal.ZERO;
            } else {
                value = orginalValue;
            }
            if (addition == null) {
                add = BigDecimal.ZERO;
            } else {
                add = addition;
            }

            return value.add(add, numberService.getMathContext());
        }

    }

    private class Subtraction implements Operation {

        @Override
        public BigDecimal perform(final BigDecimal originalValue, final BigDecimal subtrahend) {
            BigDecimal value;
            BigDecimal sub;
            if (originalValue == null) {
                value = BigDecimal.ZERO;
            } else {
                value = originalValue;
            }

            if (subtrahend == null) {
                sub = BigDecimal.ZERO;
            } else {
                sub = subtrahend;
            }
            return value.subtract(sub, numberService.getMathContext());
        }

    }

<<<<<<< HEAD:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionTrackingListenerService.java
}
=======
    // FIXME MAKU this method isn't well efficient..
    private void updateBasicProductionCounting(final Entity productionRecord, final Operation operation) {
        final Entity order = productionRecord.getBelongsToField(ORDER);

        final List<Entity> basicProductionCountings = dataDefinitionService
                .get(BasicProductionCountingConstants.PLUGIN_IDENTIFIER,
                        BasicProductionCountingConstants.MODEL_BASIC_PRODUCTION_COUNTING).find()
                .add(SearchRestrictions.belongsTo(ORDER, order)).list().getEntities();

        final List<Entity> productsIn = productionRecord.getHasManyField(RECORD_OPERATION_PRODUCT_IN_COMPONENTS);
        final List<Entity> productsOut = productionRecord.getHasManyField(RECORD_OPERATION_PRODUCT_OUT_COMPONENTS);

        for (Entity productIn : productsIn) {
            Entity basicProductionCounting = getBasicProductionCounting(productIn, basicProductionCountings);
            if (basicProductionCounting == null) {
                continue;
            }

            final BigDecimal usedQuantity = basicProductionCounting.getDecimalField(L_USED_QUANTITY);
            final BigDecimal productQuantity = productIn.getDecimalField(L_USED_QUANTITY);
            final BigDecimal result = operation.perform(usedQuantity, productQuantity);
            basicProductionCounting.setField(L_USED_QUANTITY, result);
            basicProductionCounting.getDataDefinition().save(basicProductionCounting);
        }

        for (Entity productOut : productsOut) {
            Entity productionCounting = getBasicProductionCounting(productOut, basicProductionCountings);
            if (productionCounting == null) {
                continue;
            }

            final BigDecimal usedQuantity = (BigDecimal) productionCounting.getField(L_PRODUCED_QUANTITY);
            final BigDecimal productQuantity = (BigDecimal) productOut.getField(L_USED_QUANTITY);
            final BigDecimal result = operation.perform(usedQuantity, productQuantity);
            productionCounting.setField(L_PRODUCED_QUANTITY, result);
            productionCounting.getDataDefinition().save(productionCounting);
        }
    }

}
>>>>>>> master:mes-plugins/mes-plugins-production-counting/src/main/java/com/qcadoo/mes/productionCounting/states/listener/ProductionRecordBasicListenerService.java
