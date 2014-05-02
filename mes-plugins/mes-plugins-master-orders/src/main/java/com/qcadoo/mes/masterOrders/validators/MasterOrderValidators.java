package com.qcadoo.mes.masterOrders.validators;

import static com.qcadoo.model.api.search.SearchRestrictions.*;

import java.util.Collection;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.masterOrders.constants.MasterOrderFields;
import com.qcadoo.mes.masterOrders.constants.MasterOrderType;
import com.qcadoo.mes.masterOrders.util.MasterOrderOrdersDataProvider;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.orders.states.constants.OrderState;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.FieldDefinition;
import com.qcadoo.model.api.search.SearchCriterion;

@Service
public class MasterOrderValidators {

    @Autowired
    private MasterOrderOrdersDataProvider masterOrderOrdersDataProvider;

    public boolean checkIfCanChangeCompany(final DataDefinition masterOrderDD, final FieldDefinition fieldDefinition,
            final Entity masterOrder, final Object fieldOldValue, final Object fieldNewValue) {
        if (isNewlyCreated(masterOrder) || areSame((Entity) fieldOldValue, (Entity) fieldNewValue)
                || doesNotHaveAnyPendingOrder(masterOrder)) {
            return true;
        }

        masterOrder.addError(fieldDefinition, "masterOrders.masterOrder.company.orderAlreadyExists");
        return false;
    }

    public boolean checkIfCanChangeDeadline(final DataDefinition masterOrderDD, final FieldDefinition fieldDefinition,
            final Entity masterOrder, final Object fieldOldValue, final Object fieldNewValue) {
        if (isNewlyCreated(masterOrder) || areSame(fieldOldValue, fieldNewValue) || doesNotHaveAnyPendingOrder(masterOrder)) {
            return true;
        }
        masterOrder.addError(fieldDefinition, "masterOrders.masterOrder.deadline.orderAlreadyExists");
        return false;
    }

    public boolean checkIfCanChangeMasterOrderPrefixField(final DataDefinition masterOrderDD, final Entity masterOrder) {
        Boolean orderNumbersPrefixIsNotRequired = !masterOrder.getBooleanField(MasterOrderFields.ADD_MASTER_PREFIX_TO_NUMBER);
        return isNewlyCreated(masterOrder) || orderNumbersPrefixIsNotRequired
                || checkIfEachOrderHasNumberStartingWithMasterOrderNumber(masterOrder);
    }

    private boolean checkIfEachOrderHasNumberStartingWithMasterOrderNumber(final Entity masterOrder) {
        String newMasterOrderNumber = masterOrder.getStringField(MasterOrderFields.NUMBER);
        SearchCriterion criteria = not(like(OrderFields.NUMBER, newMasterOrderNumber + "%"));
        Collection<String> unsupportedOrderNumbers = masterOrderOrdersDataProvider.findBelongingOrderNumbers(masterOrder.getId(),
                criteria);

        if (unsupportedOrderNumbers.isEmpty()) {
            return true;
        }

        addUnsupportedOrdersError(masterOrder, MasterOrderFields.NUMBER,
                "masterOrders.order.number.alreadyExistsOrderWithWrongNumber", unsupportedOrderNumbers);
        return false;
    }

    private boolean doesNotHaveAnyPendingOrder(final Entity masterOrder) {
        return masterOrderOrdersDataProvider.countBelongingOrders(masterOrder.getId(),
                ne(OrderFields.STATE, OrderState.PENDING.getStringValue())) == 0;
    }

    public boolean checkIfCanChangeTechnology(final DataDefinition masterOrderDD, final FieldDefinition fieldDefinition,
            final Entity masterOrder, final Object fieldOldValue, final Object fieldNewValue) {
        return isNewlyCreated(masterOrder) || isNotOfOneProductType(masterOrder)
                || areSame((Entity) fieldOldValue, (Entity) fieldNewValue)
                || checkIfEachOrderSupportsNewTechnology(masterOrder, (Entity) fieldNewValue);
    }

    private boolean checkIfEachOrderSupportsNewTechnology(final Entity masterOrder, final Entity technology) {
        Collection<String> unsupportedOrderNumbers = masterOrderOrdersDataProvider.findBelongingOrderNumbers(masterOrder.getId(),
                not(belongsTo(OrderFields.TECHNOLOGY_PROTOTYPE, technology)));

        if (unsupportedOrderNumbers.isEmpty()) {
            return true;
        }

        addUnsupportedOrdersError(masterOrder, MasterOrderFields.TECHNOLOGY,
                "masterOrders.masterOrder.technology.wrongTechnology", unsupportedOrderNumbers);
        return false;
    }

    public boolean checkIfCanChangeProduct(final DataDefinition masterOrderDD, final FieldDefinition fieldDefinition,
            final Entity masterOrder, final Object fieldOldValue, final Object fieldNewValue) {
        return isNewlyCreated(masterOrder) || isNotOfOneProductType(masterOrder)
                || areSame((Entity) fieldOldValue, (Entity) fieldNewValue)
                || checkIfEachOrderSupportsNewProduct(masterOrder, (Entity) fieldNewValue);
    }

    private boolean checkIfEachOrderSupportsNewProduct(final Entity masterOrder, final Entity product) {
        Collection<String> unsupportedOrderNumbers = masterOrderOrdersDataProvider.findBelongingOrderNumbers(masterOrder.getId(),
                not(belongsTo(OrderFields.PRODUCT, product)));

        if (unsupportedOrderNumbers.isEmpty()) {
            return true;
        }

        addUnsupportedOrdersError(masterOrder, MasterOrderFields.PRODUCT, "masterOrders.masterOrder.product.wrongProduct",
                unsupportedOrderNumbers);
        return false;
    }

    public boolean checkIfProductIsSelected(final DataDefinition dataDefinition, final Entity masterOrder) {
        if (isNotOfOneProductType(masterOrder)) {
            return true;
        }
        if (masterOrder.getBelongsToField(MasterOrderFields.PRODUCT) == null) {
            masterOrder.addError(dataDefinition.getField(MasterOrderFields.PRODUCT),
                    "masterOrders.masterOrder.product.haveToBeSelected");
            return false;
        }
        return true;
    }

    public boolean checkIfCanChangeType(final DataDefinition masterOrderDD, final FieldDefinition fieldDefinition,
            final Entity masterOrder, final Object fieldOldValue, final Object fieldNewValue) {
        if (isNewlyCreated(masterOrder)) {
            return true;
        }

        MasterOrderType fromType = MasterOrderType.parseString((String) fieldOldValue);
        MasterOrderType toType = MasterOrderType.parseString((String) fieldNewValue);
        if (transitionRequireEmptyOrders(fromType, toType) && masterOrderHasAnyOrders(masterOrder)) {
            masterOrder.addError(fieldDefinition, "masterOrders.masterOrder.alreadyHaveOrder");
            return false;
        }
        return true;
    }

    private boolean transitionRequireEmptyOrders(final MasterOrderType fromType, final MasterOrderType toType) {
        return fromType == MasterOrderType.UNDEFINED && toType == MasterOrderType.MANY_PRODUCTS;
    }

    private boolean masterOrderHasAnyOrders(final Entity masterOrder) {
        return masterOrderOrdersDataProvider.countBelongingOrders(masterOrder.getId(), null) > 0;
    }

    private void addUnsupportedOrdersError(final Entity targetEntity, final String errorTargetFieldName,
            final String errorMessageKey, final Collection<String> unsupportedOrderNumbers) {
        FieldDefinition errorTargetFieldDef = targetEntity.getDataDefinition().getField(errorTargetFieldName);
        targetEntity.addError(errorTargetFieldDef, errorMessageKey, StringUtils.join(unsupportedOrderNumbers, ", "));
    }

    private boolean isNewlyCreated(final Entity masterOrder) {
        return masterOrder.getId() == null;
    }

    private boolean isNotOfOneProductType(Entity masterOrder) {
        return MasterOrderType.of(masterOrder) != MasterOrderType.ONE_PRODUCT;
    }

    private <T extends Object> boolean areSame(final T newValue, final T oldValue) {
        return (newValue == null && oldValue == null)
                || (newValue != null && oldValue != null && ObjectUtils.equals(newValue, oldValue));
    }

    private boolean areSame(final Entity newValue, final Entity oldValue) {
        return (newValue == null && oldValue == null)
                || (newValue != null && oldValue != null && ObjectUtils.equals(newValue.getId(), oldValue.getId()));
    }

}
