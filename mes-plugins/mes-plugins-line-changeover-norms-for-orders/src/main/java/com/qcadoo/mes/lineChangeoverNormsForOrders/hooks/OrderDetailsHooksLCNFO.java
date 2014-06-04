package com.qcadoo.mes.lineChangeoverNormsForOrders.hooks;

import org.springframework.stereotype.Service;

import com.qcadoo.mes.orders.constants.OrderType;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.LookupComponent;
import com.qcadoo.view.api.components.WindowComponent;
import com.qcadoo.view.api.ribbon.Ribbon;
import com.qcadoo.view.api.ribbon.RibbonActionItem;
import com.qcadoo.view.api.ribbon.RibbonGroup;

@Service
public class OrderDetailsHooksLCNFO {

    public void onBeforeRender(final ViewDefinitionState view) {
        enableOrDisableChangeoverButton(view);
    }

    private void enableOrDisableChangeoverButton(final ViewDefinitionState view) {
        boolean hasPatternTechnology = hasDefinedPatternTechnology(view);
        setChangeoverButtonEnabled(view, hasPatternTechnology);
    }

    private boolean hasDefinedPatternTechnology(ViewDefinitionState view) {
        ComponentState orderTypeSelect = view.getComponentByReference("orderType");
        LookupComponent technologyPrototypeLookup = (LookupComponent) view.getComponentByReference("technologyPrototype");
        if (technologyPrototypeLookup == null || orderTypeSelect == null || orderTypeSelect.getFieldValue() == null) {
            return false;
        }

        OrderType type = OrderType.parseString((String) orderTypeSelect.getFieldValue());
        return type == OrderType.WITH_PATTERN_TECHNOLOGY && !technologyPrototypeLookup.isEmpty();
    }

    private void setChangeoverButtonEnabled(final ViewDefinitionState view, final boolean enabled) {
        WindowComponent window = (WindowComponent) view.getComponentByReference("window");
        if (window == null) {
            return;
        }
        Ribbon ribbon = window.getRibbon();
        RibbonGroup changeoverGroup = ribbon.getGroupByName("changeover");
        if (changeoverGroup == null) {
            return;
        }
        RibbonActionItem showChangeoverButton = changeoverGroup.getItemByName("showChangeover");
        if (showChangeoverButton == null) {
            return;
        }

        showChangeoverButton.setEnabled(enabled);
        showChangeoverButton.requestUpdate(true);
        window.requestRibbonRender();
    }

}
