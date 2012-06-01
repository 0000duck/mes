package com.qcadoo.mes.productionPerShift.hooks;

import static com.qcadoo.mes.orders.constants.OrdersConstants.PLUGIN_IDENTIFIER;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.orders.constants.OrderStates;
import com.qcadoo.mes.orders.constants.OrdersConstants;
import com.qcadoo.mes.productionPerShift.constants.PlannedProgressType;
import com.qcadoo.mes.technologies.TechnologyService;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityTree;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.utils.TimeConverterService;

@Service
public class ProductionPerShiftDetailsHooks {

    private static final String PRODUCTION_PER_SHIFT_OPERATION = "productionPerShiftOperation";

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private TechnologyService technologyService;

    @Autowired
    private TimeConverterService timeConverterService;

    public void addRootForOperation(final ViewDefinitionState view) {
        FormComponent form = (FormComponent) view.getComponentByReference("form");
        if (form.getEntityId() == null) {
            return;
        }
        Entity order = dataDefinitionService.get(PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(form.getEntityId());
        EntityTree techInstOperComps = order.getTreeField(OrderFields.TECHNOLOGY_INSTANCE_OPERATION_COMPONENTS);
        if (techInstOperComps.isEmpty()) {
            return;
        }
        Entity root = techInstOperComps.getRoot();
        FieldComponent operation = (FieldComponent) view.getComponentByReference(PRODUCTION_PER_SHIFT_OPERATION);
        if (operation.getFieldValue() == null) {
            operation.setFieldValue(root.getId());
        }

    }

    public void disablePlannedProgressTypeForPendingOrder(final ViewDefinitionState view) {
        Entity order = getOrderFromLookup(view);
        FieldComponent plannedProgressType = (FieldComponent) view.getComponentByReference("plannedProgressType");
        if (order.getStringField(OrderFields.STATE).equals(OrderStates.PENDING.getStringValue())) {
            plannedProgressType.setFieldValue(PlannedProgressType.PLANNED.getStringValue());
            plannedProgressType.setEnabled(false);
        } else {
            plannedProgressType.setEnabled(true);
        }
    }

    public void fillProducedField(final ViewDefinitionState viewState) {
        Entity tioc = getTiocFromOperationLookup(viewState);
        String producedProduct = null;

        if (tioc != null) {
            Entity toc = tioc.getBelongsToField("technologyOperationComponent");

            Entity prodComp = technologyService.getMainOutputProductComponent(toc);
            Entity prod = prodComp.getBelongsToField("product");
            producedProduct = prod.getStringField("name");
        }

        ComponentState producesInput = viewState.getComponentByReference("produces");
        producesInput.setFieldValue(producedProduct);
    }

    public Entity getTiocFromOperationLookup(final ViewDefinitionState viewState) {
        ComponentState operationLookup = viewState.getComponentByReference("productionPerShiftOperation");
        Long id = (Long) operationLookup.getFieldValue();
        Entity tioc = null;
        if (id != null) {
            tioc = dataDefinitionService.get("technologies", "technologyInstanceOperationComponent").get(id);
        }
        return tioc;
    }

    public void setOrderStartDate(final ViewDefinitionState view) {
        Entity order = getOrderFromLookup(view);
        FieldComponent orderPlannedStartDate = (FieldComponent) view.getComponentByReference("orderPlannedStartDate");
        FieldComponent orderCorrectedStartDate = (FieldComponent) view.getComponentByReference("orderCorrectedStartDate");
        if (order.getField(OrderFields.DATE_FROM) != null) {
            orderPlannedStartDate
                    .setFieldValue(timeConverterService.setDateToField((Date) order.getField(OrderFields.DATE_FROM)));
        }
        if (order.getField(OrderFields.CORRECTED_DATE_FROM) != null) {
            orderCorrectedStartDate.setFieldValue(timeConverterService.setDateToField((Date) order
                    .getField(OrderFields.CORRECTED_DATE_FROM)));
        }
        orderPlannedStartDate.requestComponentUpdateState();
        orderCorrectedStartDate.requestComponentUpdateState();
    }

    public void disableReasonOfCorrection(final ViewDefinitionState view) {
        FieldComponent plannedProgressType = (FieldComponent) view.getComponentByReference("plannedProgressType");
        FieldComponent plannedProgressCorrectionType = (FieldComponent) view
                .getComponentByReference("plannedProgressCorrectionType");
        FieldComponent plannedProgressCorrectionComment = (FieldComponent) view
                .getComponentByReference("plannedProgressCorrectionComment");
        if (plannedProgressType.getFieldValue().equals(PlannedProgressType.PLANNED.getStringValue())) {
            plannedProgressCorrectionType.setEnabled(false);
            plannedProgressCorrectionComment.setEnabled(false);
        } else {
            plannedProgressCorrectionType.setEnabled(true);
            plannedProgressCorrectionComment.setEnabled(true);
        }
    }

    public Entity getOrderFromLookup(final ViewDefinitionState view) {
        ComponentState lookup = view.getComponentByReference("order");
        if (!(lookup.getFieldValue() instanceof Long)) {
            return null;
        }
        return dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(
                (Long) lookup.getFieldValue());
    }
}
