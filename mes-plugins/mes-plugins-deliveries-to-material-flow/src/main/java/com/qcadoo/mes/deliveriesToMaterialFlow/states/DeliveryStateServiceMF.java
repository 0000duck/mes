/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.3
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
package com.qcadoo.mes.deliveriesToMaterialFlow.states;

import java.math.BigDecimal;
import java.util.List;

import com.qcadoo.mes.basic.ParameterService;
import com.qcadoo.mes.basic.constants.CurrencyFields;
import com.qcadoo.mes.basic.constants.ParameterFields;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.qcadoo.mes.deliveries.constants.DeliveredProductFields;
import com.qcadoo.mes.deliveries.constants.DeliveryFields;
import com.qcadoo.mes.deliveriesToMaterialFlow.constants.DocumentFieldsDTMF;
import com.qcadoo.mes.materialFlowResources.service.DocumentBuilder;
import com.qcadoo.mes.materialFlowResources.service.DocumentManagementService;
import com.qcadoo.mes.states.StateChangeContext;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.NumberService;

@Service
public class DeliveryStateServiceMF {

    @Autowired
    private DocumentManagementService documentManagementService;

    @Autowired
    private NumberService numberService;

    @Autowired
    private ParameterService parameterService;

    public void createDocumentsForTheReceivedProducts(final StateChangeContext stateChangeContext) {
        final Entity delivery = stateChangeContext.getOwner();

        Entity location = location(delivery);
        if (location == null)
            return;

        Entity currency = currency(delivery);

        List<Entity> deliveredProducts = delivery.getHasManyField(DeliveryFields.DELIVERED_PRODUCTS);

        DocumentBuilder documentBuilder = documentManagementService.getDocumentBuilder();
        documentBuilder.receipt(location);
        documentBuilder.setField(DocumentFieldsDTMF.DELIVERY, delivery);

        for (Entity deliveredProduct : deliveredProducts) {

            BigDecimal quantity = deliveredProduct.getDecimalField(DeliveredProductFields.DELIVERED_QUANTITY);
            Optional<BigDecimal> damagedQuantity = Optional.fromNullable(deliveredProduct
                    .getDecimalField(DeliveredProductFields.DAMAGED_QUANTITY));

            BigDecimal positionQuantity = quantity.subtract(damagedQuantity.or(BigDecimal.ZERO),
                    numberService.getMathContext());
            if (positionQuantity.compareTo(BigDecimal.ZERO) > 0) {
                documentBuilder.addPosition(
                        product(deliveredProduct),
                        positionQuantity,
                        price(deliveredProduct, currency),
                        null, null, null
                );
            }
        }
        documentBuilder.setAccepted().build();
    }

    private Entity currency(Entity delivery) {
        Entity currency = delivery.getBelongsToField(DeliveryFields.CURRENCY);
        return currency != null ? currency : currencyFromParameter();
    }

    private Entity location(Entity delivery) {
        return delivery.getBelongsToField(DeliveryFields.LOCATION);
    }

    private BigDecimal price(Entity deliveredProduct, Entity currency) {
        BigDecimal exRate = currency.getDecimalField(CurrencyFields.EXCHANGE_RATE);
        Optional<BigDecimal> pricePerUnit = Optional.fromNullable(deliveredProduct.getDecimalField(DeliveredProductFields.PRICE_PER_UNIT));
        if(!pricePerUnit.isPresent()){
            return null;
        }
        return exRateExists(exRate) ? pricePerUnit.get().multiply(exRate) : pricePerUnit.get();
    }

    private boolean exRateExists(BigDecimal exRate) {
        return exRate.compareTo(BigDecimal.ZERO) > 0;
    }

    private Entity product(Entity deliveredProduct) {
        return deliveredProduct.getBelongsToField(DeliveredProductFields.PRODUCT);
    }

    private Entity currencyFromParameter() {
        return parameterService.getParameter().getBelongsToField(ParameterFields.CURRENCY);
    }
}
