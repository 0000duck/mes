/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.4.9
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

import static com.qcadoo.mes.costNormsForProduct.constants.ProductsCostCalculationConstants.AVERAGE;
import static com.qcadoo.mes.costNormsForProduct.constants.ProductsCostCalculationConstants.LASTPURCHASE;
import static com.qcadoo.mes.costNormsForProduct.constants.ProductsCostCalculationConstants.NOMINAL;
import static java.math.BigDecimal.valueOf;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.qcadoo.mes.costNormsForProduct.constants.ProductsCostCalculationConstants;
import com.qcadoo.mes.technologies.TechnologyService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityList;
import com.qcadoo.model.api.EntityTree;

@RunWith(Parameterized.class)
public class ParameterizedProductsCostCalculationServiceTest {

    private ProductsCostCalculationService productCostCalc;

    private TechnologyService technologyService;

    private Entity costCalculation;

    private ProductsCostCalculationConstants calculationMode;

    private BigDecimal averageCost, lastPurchaseCost, nominalCost, inputQuantity, orderQuantity, expectedResult;

    private BigDecimal costForNumber;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // mode, average, lastPurchase, nominal, costForNumber, input qtty, order qtty, expectedResult
                { AVERAGE, valueOf(10), valueOf(5), valueOf(15), valueOf(1), valueOf(1), valueOf(1), valueOf(30) },
                { AVERAGE, valueOf(10), valueOf(5), valueOf(15), valueOf(1), valueOf(2), valueOf(1), valueOf(60) },
                { AVERAGE, valueOf(10), valueOf(5), valueOf(15), valueOf(1), valueOf(3), valueOf(1), valueOf(90) },
                { AVERAGE, valueOf(10), valueOf(5), valueOf(15), valueOf(1), valueOf(3), valueOf(2), valueOf(180) },
                { AVERAGE, valueOf(10), valueOf(5), valueOf(15), valueOf(1), valueOf(3), valueOf(3), valueOf(270) },
                { AVERAGE, valueOf(10), valueOf(5), valueOf(15), valueOf(1), valueOf(3), valueOf(4), valueOf(360) },
                { AVERAGE, valueOf(10), valueOf(5), valueOf(15), valueOf(2), valueOf(3), valueOf(2), valueOf(90) },
                { AVERAGE, valueOf(10), valueOf(5), valueOf(15), valueOf(2), valueOf(3), valueOf(3), valueOf(135) },
                { AVERAGE, valueOf(10), valueOf(5), valueOf(15), valueOf(2), valueOf(3), valueOf(4), valueOf(180) }, });
    }

    public ParameterizedProductsCostCalculationServiceTest(ProductsCostCalculationConstants mode, BigDecimal average,
            BigDecimal lastPurchase, BigDecimal nominal, BigDecimal costForNumber, BigDecimal inputQuantity,
            BigDecimal orderQuantity, BigDecimal expectedResult) {
        this.averageCost = average;
        this.expectedResult = expectedResult;
        this.inputQuantity = inputQuantity;
        this.lastPurchaseCost = lastPurchase;
        this.calculationMode = mode;
        this.nominalCost = nominal;
        this.orderQuantity = orderQuantity;
        this.costForNumber = costForNumber;
    }

    @Before
    public void init() {
        technologyService = mock(TechnologyService.class);

        costCalculation = mock(Entity.class);
        EntityTree operationComponents = mock(EntityTree.class);
        Entity operationComponent = mock(Entity.class);
        EntityList inputProducts = mock(EntityList.class);
        Entity inputProduct = mock(Entity.class);
        Entity product = mock(Entity.class);
        Entity technology = mock(Entity.class);

        productCostCalc = new ProductsCostCalculationServiceImpl();

        ReflectionTestUtils.setField(productCostCalc, "technologyService", technologyService);

        when(technologyService.getProductType(product, technology)).thenReturn(TechnologyService.COMPONENT);

        when(costCalculation.getField("quantity")).thenReturn(orderQuantity);
        when(costCalculation.getBelongsToField("technology")).thenReturn(technology);
        when(technology.getTreeField("operationComponents")).thenReturn(operationComponents);
        when(costCalculation.getField("calculateMaterialCostsMode")).thenReturn(calculationMode);

        @SuppressWarnings("unchecked")
        Iterator<Entity> operationComponentsIterator = mock(Iterator.class);
        when(operationComponentsIterator.hasNext()).thenReturn(true, false);
        when(operationComponentsIterator.next()).thenReturn(operationComponent);
        when(operationComponents.iterator()).thenReturn(operationComponentsIterator);

        @SuppressWarnings("unchecked")
        Iterator<Entity> inputProductsIterator = mock(Iterator.class);
        when(inputProductsIterator.hasNext()).thenReturn(true, true, true, false);
        when(inputProductsIterator.next()).thenReturn(inputProduct);
        when(inputProducts.iterator()).thenReturn(inputProductsIterator);

        when(operationComponent.getHasManyField("operationProductInComponents")).thenReturn(inputProducts);
        when(inputProduct.getField("quantity")).thenReturn(inputQuantity);
        when(inputProduct.getBelongsToField("product")).thenReturn(product);

        when(product.getField(AVERAGE.getStrValue())).thenReturn(averageCost);
        when(product.getField(LASTPURCHASE.getStrValue())).thenReturn(lastPurchaseCost);
        when(product.getField(NOMINAL.getStrValue())).thenReturn(nominalCost);
        when(product.getField("costForNumber")).thenReturn(costForNumber);
    }

    @Test
    public void shouldReturnCorrectCostValuesUsingTechnology() throws Exception {
        // when
        productCostCalc.calculateProductsCost(costCalculation);

        // then
        ArgumentCaptor<BigDecimal> argument = ArgumentCaptor.forClass(BigDecimal.class);
        Mockito.verify(costCalculation).setField(Mockito.eq("totalMaterialCosts"), argument.capture());
        assertEquals(expectedResult.setScale(3), argument.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldReturnExceptionWhenEntityIsNull() throws Exception {
        // when
        productCostCalc.calculateProductsCost(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldReturnExceptionWhenQuantityIsNull() throws Exception {
        // given
        when(costCalculation.getField("quantity")).thenReturn(null);

        // when
        productCostCalc.calculateProductsCost(costCalculation);
    }
}
