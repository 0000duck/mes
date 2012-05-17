/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.6
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
package com.qcadoo.mes.costNormsForMaterials.listeners;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.qcadoo.mes.costNormsForProduct.CostNormsForProductService;
import com.qcadoo.mes.costNormsForProduct.constants.CostNormsForProductConstants;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;

@Service
public class OrderDetailsListenersCNFM {

    private static final String L_COST_FOR_NUMBER_UNIT = "costForNumberUnit";

    @Autowired
    private CostNormsForProductService costNormsForProductService;

    public void fillUnitFieldInOrder(final ViewDefinitionState viewDefinitionState) {
        costNormsForProductService.fillUnitField(viewDefinitionState, L_COST_FOR_NUMBER_UNIT, false);
    }

    public void fillCurrencyFieldsInOrder(final ViewDefinitionState viewDefinitionState) {
        costNormsForProductService.fillCurrencyFields(viewDefinitionState, CostNormsForProductConstants.CURRENCY_FIELDS_ORDER);
    }

    public final void showInputProductsCostInOrder(final ViewDefinitionState viewState, final ComponentState componentState,
            final String[] args) {
        Long orderId = (Long) componentState.getFieldValue();

        if (orderId == null) {
            return;
        }

        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("order.id", orderId);

        String url = "../page/costNormsForMaterials/costNormsForMaterialsInOrderList.html";
        viewState.redirectTo(url, false, true, parameters);
    }
}
