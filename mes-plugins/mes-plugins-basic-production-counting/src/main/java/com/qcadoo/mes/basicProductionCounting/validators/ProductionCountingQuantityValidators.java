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
package com.qcadoo.mes.basicProductionCounting.validators;

import org.springframework.stereotype.Service;

import com.qcadoo.mes.basicProductionCounting.constants.OrderFieldsBPC;
import com.qcadoo.mes.basicProductionCounting.constants.ProductionCountingQuantityFields;
import com.qcadoo.mes.basicProductionCounting.constants.ProductionCountingQuantityTypeOfMaterial;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchRestrictions;

@Service
public class ProductionCountingQuantityValidators {

    public boolean validatesWith(final DataDefinition productionCountingQuantityDD, final Entity productionCountingQuantity) {
        return checkTypeOfMaterial(productionCountingQuantityDD, productionCountingQuantity);
    }

    private boolean checkTypeOfMaterial(final DataDefinition productionCountingQuantityDD, final Entity productionCountingQuantity) {
        String typeOfMaterial = productionCountingQuantity.getStringField(ProductionCountingQuantityFields.TYPE_OF_MATERIAL);

        if (ProductionCountingQuantityTypeOfMaterial.FINAL_PRODUCT.getStringValue().equals(typeOfMaterial)) {
            Entity order = productionCountingQuantity.getBelongsToField(ProductionCountingQuantityFields.ORDER);

            if (checkIfAnotherFinalProductExists(order)) {
                productionCountingQuantity.addError(
                        productionCountingQuantityDD.getField(ProductionCountingQuantityFields.TYPE_OF_MATERIAL),
                        "basicProductionCounting.productionCountingQuantity.typeOfMaterial.error.anotherFinalProductExists");

                return false;
            }
        } else if (ProductionCountingQuantityTypeOfMaterial.INTERMEDIATE.getStringValue().equals(typeOfMaterial)) {
            Entity technologyOperationComponent = productionCountingQuantity
                    .getBelongsToField(ProductionCountingQuantityFields.TECHNOLOGY_OPERATION_COMPONENT);

            if (technologyOperationComponent == null) {
                productionCountingQuantity
                        .addError(productionCountingQuantityDD
                                .getField(ProductionCountingQuantityFields.TECHNOLOGY_OPERATION_COMPONENT),
                                "basicProductionCounting.productionCountingQuantity.typeOfMaterial.error.technologyOperationComponentRequired");

                return false;
            }
        }

        return true;
    }

    private boolean checkIfAnotherFinalProductExists(final Entity order) {
        return (order
                .getHasManyField(OrderFieldsBPC.PRODUCTION_COUNTING_QUANTITIES)
                .find()
                .add(SearchRestrictions.eq(ProductionCountingQuantityFields.TYPE_OF_MATERIAL,
                        ProductionCountingQuantityTypeOfMaterial.FINAL_PRODUCT.getStringValue())).list()
                .getTotalNumberOfEntities() == 1);
    }

}
