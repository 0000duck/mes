/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.4.10
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
package com.qcadoo.mes.costNormsForProduct;

import static com.google.common.base.Preconditions.checkArgument;
import static com.qcadoo.mes.costNormsForProduct.constants.ProductsCostCalculationConstants.AVERAGE;
import static com.qcadoo.mes.costNormsForProduct.constants.ProductsCostCalculationConstants.LASTPURCHASE;
import static com.qcadoo.mes.costNormsForProduct.constants.ProductsCostCalculationConstants.NOMINAL;
import static java.math.BigDecimal.ROUND_UP;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.costNormsForProduct.constants.ProductsCostCalculationConstants;
import com.qcadoo.mes.technologies.TechnologyService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityList;
import com.qcadoo.model.api.EntityTree;

@Service
public class ProductsCostCalculationServiceImpl implements ProductsCostCalculationService {

    @Autowired
    TechnologyService technologyService;

    public void calculateProductsCost(final Entity costCalculation) {
        checkArgument(costCalculation != null);
        BigDecimal quantity = getBigDecimal(costCalculation.getField("quantity"));
        BigDecimal result = BigDecimal.ZERO;
        EntityTree technologyOperationComponents = costCalculation.getBelongsToField("technology").getTreeField(
                "operationComponents");
        ProductsCostCalculationConstants mode = getProductModeFromField(costCalculation.getField("calculateMaterialCostsMode"));

        checkArgument(quantity != null && quantity != BigDecimal.ZERO, "quantity is  null");
        checkArgument(technologyOperationComponents != null, "operationComponents is null!");
        checkArgument(mode != null, "mode is null!");

        Entity technology = costCalculation.getBelongsToField("technology");

        for (Entity operationComponent : technologyOperationComponents) {
            EntityList inputProducts = operationComponent.getHasManyField("operationProductInComponents");
            for (Entity inputProduct : inputProducts) {
                BigDecimal quantityOfInputProducts = getBigDecimal(inputProduct.getField("quantity"));
                Entity product = inputProduct.getBelongsToField("product");
                if (!technologyService.getProductType(product, technology).equals(TechnologyService.COMPONENT)) {
                    continue;
                }
                BigDecimal cost = getBigDecimal(product.getField(mode.getStrValue()));
                BigDecimal costForNumber = getBigDecimal(product.getField("costForNumber"));
                BigDecimal costPerUnit = cost.divide(costForNumber, 3);

                result = result.add(costPerUnit.multiply(quantityOfInputProducts));
            }
        }
        result = result.multiply(quantity);
        costCalculation.setField("totalMaterialCosts", result.setScale(3, ROUND_UP));
    }

    private ProductsCostCalculationConstants getProductModeFromField(final Object value) {
        String strValue = value.toString();
        if ("01nominal".equals(strValue)) {
            return NOMINAL;
        }
        if ("02average".equals(strValue)) {
            return AVERAGE;
        }
        if ("03lastPurchase".equals(strValue)) {
            return LASTPURCHASE;
        }
        return ProductsCostCalculationConstants.valueOf(strValue);
    }

    private BigDecimal getBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString());
    }
}
