package com.qcadoo.mes.orders.util;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.orders.states.constants.OrderState;
import com.qcadoo.mes.technologies.states.constants.TechnologyState;
import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.WindowComponent;
import com.qcadoo.view.api.ribbon.Ribbon;
import com.qcadoo.view.api.ribbon.RibbonActionItem;
import com.qcadoo.view.api.ribbon.RibbonGroup;

@Service
public class OrderDetailsRibbonHelper {

    private static final Set<TechnologyState> SUPPORTED_TECHNOLOGY_STATES = ImmutableSet.of(TechnologyState.ACCEPTED,
            TechnologyState.CHECKED);

    public static final Predicate<Entity> HAS_CHECKED_OR_ACCEPTED_TECHNOLOGY = new Predicate<Entity>() {

        @Override
        public boolean apply(final Entity order) {
            if (order == null) {
                return false;
            }
            Entity orderTechnology = order.getBelongsToField(OrderFields.TECHNOLOGY);
            return orderTechnology != null && SUPPORTED_TECHNOLOGY_STATES.contains(TechnologyState.of(orderTechnology));
        }
    };

    public void setButtonEnabledForPendingOrder(final ViewDefinitionState view, final String ribbonGroupName,
            final String ribbonItemName, final Predicate<Entity> predicate) {
        RibbonActionItem ribbonItem = getRibbonItem(view, ribbonGroupName, ribbonItemName);
        Entity order = getOrderEntity(view);
        if (ribbonItem == null || order == null || OrderState.of(order) != OrderState.PENDING) {
            return;
        }
        ribbonItem.setEnabled(predicate.apply(order));
        ribbonItem.requestUpdate(true);
    }

    private Entity getOrderEntity(final ViewDefinitionState view) {
        FormComponent form = (FormComponent) view.getComponentByReference("form");
        if (form == null) {
            return null;
        }
        return form.getPersistedEntityWithIncludedFormValues();
    }

    private RibbonActionItem getRibbonItem(final ViewDefinitionState view, final String ribbonGroupName,
            final String ribbonItemName) {
        WindowComponent window = (WindowComponent) view.getComponentByReference("window");
        Ribbon ribbon = window.getRibbon();
        RibbonGroup ribbonGroup = ribbon.getGroupByName(ribbonGroupName);
        if (ribbonGroup == null) {
            return null;
        }
        return ribbonGroup.getItemByName(ribbonItemName);
    }

}
