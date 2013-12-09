package com.qcadoo.mes.masterOrders.listeners;

import static com.qcadoo.mes.orders.constants.OrdersConstants.BASIC_MODEL_PRODUCT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.masterOrders.hooks.MasterOrderDetailsHooks;
import com.qcadoo.mes.orders.TechnologyServiceO;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.ExpressionService;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.LookupComponent;

@Service
public class MasterOrderDetailsListeners {

    private static final String L_FORM = "form";

    @Autowired
    private MasterOrderDetailsHooks masterOrderDetailsHooks;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private TechnologyServiceO technologyServiceO;

    public void hideFieldDependOnMasterOrderType(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        masterOrderDetailsHooks.hideFieldDependOnMasterOrderType(view);
    }

    public void fillUnitField(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        masterOrderDetailsHooks.fillUnitField(view);
    }

    public void fillDefaultTechnology(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        LookupComponent productField = (LookupComponent) view.getComponentByReference(BASIC_MODEL_PRODUCT);
        FieldComponent defaultTechnology = (FieldComponent) view.getComponentByReference("defaultTechnology");
        FieldComponent technology = (FieldComponent) view.getComponentByReference("technology");

        Entity product = productField.getEntity();
        if (product == null || technologyServiceO.getDefaultTechnology(product) == null) {
            defaultTechnology.setFieldValue(null);
            technology.setFieldValue(null);
            defaultTechnology.requestComponentUpdateState();
            technology.requestComponentUpdateState();
            return;
        }
        Entity defaultTechnologyEntity = technologyServiceO.getDefaultTechnology(product);
        String defaultTechnologyValue = expressionService.getValue(defaultTechnologyEntity, "#number + ' - ' + #name",
                view.getLocale());
        defaultTechnology.setFieldValue(defaultTechnologyValue);
        technology.setFieldValue(defaultTechnologyEntity.getId());
        defaultTechnology.requestComponentUpdateState();
        technology.requestComponentUpdateState();
    }

    public void refreshView(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        FormComponent form = (FormComponent) view.getComponentByReference(L_FORM);
        form.performEvent(view, "refresh");
    }

    public void setUneditableWhenEntityHasUnsaveChanges(final ViewDefinitionState view, final ComponentState state,
            final String[] args) {
        masterOrderDetailsHooks.setUneditableWhenEntityHasUnsaveChanges(view);
    }

    public void createOrder(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        FormComponent masterOrderForm = (FormComponent) view.getComponentByReference(L_FORM);
        Entity masterOrder = masterOrderForm.getEntity();

        if (masterOrder.getId() == null) {
            return;
        }

        String url = "../page/orders/orderDetails.html?context={form.masterOrder: " + masterOrder.getId() + "}";
        view.redirectTo(url, false, true);
    }

}
