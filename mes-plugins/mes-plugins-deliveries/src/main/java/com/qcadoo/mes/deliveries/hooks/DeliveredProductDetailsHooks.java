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
package com.qcadoo.mes.deliveries.hooks;

import static com.qcadoo.mes.deliveries.constants.DeliveredProductFields.DELIVERED_QUANTITY;
import static com.qcadoo.mes.deliveries.constants.DeliveredProductFields.DELIVERY;
import static com.qcadoo.mes.deliveries.constants.DeliveredProductFields.PRODUCT;
import static com.qcadoo.mes.deliveries.constants.OrderedProductFields.ORDERED_QUANTITY;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.qcadoo.mes.deliveries.DeliveriesService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.NumberService;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.LookupComponent;

@Service
public class DeliveredProductDetailsHooks {

    private static final String L_FORM = "form";

    @Autowired
    private NumberService numberService;

    @Autowired
    private DeliveriesService deliveriesService;

    public void fillOrderedQuantities(final ViewDefinitionState view) {
        FormComponent deliveredProductForm = (FormComponent) view.getComponentByReference(L_FORM);
        Entity deliveredProduct = deliveredProductForm.getEntity();

        LookupComponent productLookup = (LookupComponent) view.getComponentByReference(PRODUCT);
        Entity product = productLookup.getEntity();

        FieldComponent orderedQuantity = (FieldComponent) view.getComponentByReference(ORDERED_QUANTITY);

        if (product == null) {
            orderedQuantity.setFieldValue(null);
        } else {
            orderedQuantity.setFieldValue(numberService.format(getOrderedProductQuantity(deliveredProduct)));
        }

        orderedQuantity.requestComponentUpdateState();
    }

    private BigDecimal getOrderedProductQuantity(final Entity deliveredProduct) {
        Entity delivery = deliveredProduct.getBelongsToField(DELIVERY);
        Entity product = deliveredProduct.getBelongsToField(PRODUCT);

        BigDecimal orderedQuantity = null;

        Entity orderedProduct = deliveriesService.getOrderedProductDD().find()
                .add(SearchRestrictions.belongsTo(DELIVERY, delivery)).add(SearchRestrictions.belongsTo(PRODUCT, product))
                .setMaxResults(1).uniqueResult();

        if (orderedProduct != null) {
            orderedQuantity = orderedProduct.getDecimalField(ORDERED_QUANTITY);
        }

        return orderedQuantity;
    }

    public void fillUnitFields(final ViewDefinitionState view) {
        List<String> referenceNames = Lists.newArrayList("damagedQuantityUnit", "deliveredQuantityUnit", "orderedQuantityUnit");

        deliveriesService.fillUnitFields(view, PRODUCT, referenceNames);
    }

    public void fillCurrencyFields(final ViewDefinitionState view) {
        List<String> referenceNames = Lists.newArrayList("totalPriceCurrency", "pricePerUnitCurrency");

        deliveriesService.fillCurrencyFields(view, referenceNames);
    }

    public void setDeliveredQuantityFieldRequired(final ViewDefinitionState view) {
        FieldComponent delivedQuantity = (FieldComponent) view.getComponentByReference(DELIVERED_QUANTITY);
        delivedQuantity.setRequired(true);
        delivedQuantity.requestComponentUpdateState();
    }

}
