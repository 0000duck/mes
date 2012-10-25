/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.7
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
package com.qcadoo.mes.costCalculation;

import static com.qcadoo.mes.costNormsForOperation.constants.CalculateOperationCostMode.HOURLY;
import static com.qcadoo.mes.costNormsForOperation.constants.CalculateOperationCostMode.PIECEWORK;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.costNormsForMaterials.ProductsCostCalculationService;
import com.qcadoo.mes.costNormsForOperation.constants.CalculateOperationCostMode;
import com.qcadoo.mes.operationCostCalculations.OperationsCostCalculationService;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.NumberService;

@Service
public class CostCalculationServiceImpl implements CostCalculationService {

    @Autowired
    private OperationsCostCalculationService operationsCostCalculationService;

    @Autowired
    private ProductsCostCalculationService productsCostCalculationService;

    @Autowired
    private NumberService numberService;

    @Override
    public Entity calculateTotalCost(final Entity entity) {
        final BigDecimal productionCosts;
        final BigDecimal materialCostMargin = getBigDecimal(entity.getField("materialCostMargin"));
        final BigDecimal productionCostMargin = getBigDecimal(entity.getField("productionCostMargin"));
        final BigDecimal additionalOverhead = getBigDecimal(entity.getField("additionalOverhead"));

        CalculateOperationCostMode operationMode = CalculateOperationCostMode.parseString(entity
                .getStringField("calculateOperationCostsMode"));

        entity.setField("date", new Date());

        operationsCostCalculationService.calculateOperationsCost(entity);

        String sourceOfMaterialCosts = entity.getStringField("sourceOfMaterialCosts");

        productsCostCalculationService.calculateTotalProductsCost(entity, sourceOfMaterialCosts);

        if (HOURLY.equals(operationMode)) {
            BigDecimal totalMachine = getBigDecimal(entity.getField("totalMachineHourlyCosts"));
            BigDecimal totalLabor = getBigDecimal(entity.getField("totalLaborHourlyCosts"));
            productionCosts = totalMachine.add(totalLabor, numberService.getMathContext());
        } else if (PIECEWORK.equals(operationMode)) {
            productionCosts = getBigDecimal(entity.getField("totalPieceworkCosts"));
        } else {
            throw new IllegalStateException("Unsupported calculateOperationCostsMode");
        }

        BigDecimal materialCosts = getBigDecimal(entity.getField("totalMaterialCosts"));

        BigDecimal productionCostMarginValue = productionCosts.multiply(productionCostMargin, numberService.getMathContext())
                .divide(BigDecimal.valueOf(100), numberService.getMathContext());
        BigDecimal materialCostMarginValue = materialCosts.multiply(materialCostMargin, numberService.getMathContext()).divide(
                BigDecimal.valueOf(100), numberService.getMathContext());

        // TODO mici, I think we should clamp it to get the consisted result, because DB clamps it to the setScale(3) anyway.
        productionCostMarginValue = numberService.setScale(productionCostMarginValue);
        materialCostMarginValue = numberService.setScale(materialCostMarginValue);

        BigDecimal totalTechnicalProductionCosts = productionCosts.add(materialCosts, numberService.getMathContext());
        BigDecimal totalOverhead = productionCostMarginValue.add(materialCostMarginValue, numberService.getMathContext()).add(
                additionalOverhead, numberService.getMathContext());
        BigDecimal totalCosts = totalOverhead.add(totalTechnicalProductionCosts, numberService.getMathContext());

        entity.setField("productionCostMarginValue", numberService.setScale(productionCostMarginValue));
        entity.setField("materialCostMarginValue", numberService.setScale(materialCostMarginValue));
        entity.setField("additionalOverheadValue", numberService.setScale(additionalOverhead));

        entity.setField("totalOverhead", numberService.setScale(totalOverhead));
        entity.setField("totalTechnicalProductionCosts", numberService.setScale(totalTechnicalProductionCosts));
        entity.setField("totalCosts", numberService.setScale(totalCosts));

        final BigDecimal doneQuantity = getDoneQuantity(entity);
        if (doneQuantity != null && BigDecimal.ZERO.compareTo(doneQuantity) != 0) {
            final BigDecimal totalCostsPerUnit = numberService.setScale(totalCosts.divide(doneQuantity,
                    numberService.getMathContext()));
            entity.setField("totalCostPerUnit", totalCostsPerUnit);
        }

        return entity.getDataDefinition().save(entity);
    }

    private BigDecimal getDoneQuantity(final Entity costCalculation) {
        final Entity order = costCalculation.getBelongsToField("order");
        if (order != null) {
            return order.getDecimalField(OrderFields.DONE_QUANTITY);
        }
        return null;
    }

    private BigDecimal getBigDecimal(final Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        // MAKU - using BigDecimal.valueOf(Double) instead of new BigDecimal(String) to prevent issue described at
        // https://forums.oracle.com/forums/thread.jspa?threadID=2251030
        return BigDecimal.valueOf(Double.valueOf(value.toString()));
    }
}
