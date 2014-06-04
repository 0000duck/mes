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
package com.qcadoo.mes.costNormsForMaterials.hooks;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.qcadoo.mes.costNormsForMaterials.CostNormsForMaterialsService;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;

public class OrderHooksCNFMTest {

    private OrderHooksCNFM orderHooksCNFM;

    @Mock
    private DataDefinition orderDD, techInsOperCompProdInDD;

    @Mock
    private CostNormsForMaterialsService costNormsForMaterialsService;

    @Mock
    private DataDefinitionService dataDefinitionService;

    @Mock
    private Entity order, orderFromDB, technology, prod1, prod2, techInsOperCompProdIn1, techInsOperCompProdIn2;

    @Mock
    private Map<Long, BigDecimal> productQuantities;

    @Mock
    private Set<Entry<Long, BigDecimal>> entrySet;

    @Mock
    private Entry<Long, BigDecimal> entry1, entry2;

    @Mock
    private Iterator<Entry<Long, BigDecimal>> iterator;

    @Before
    public void init() {
        orderHooksCNFM = new OrderHooksCNFM();

        MockitoAnnotations.initMocks(this);

        ReflectionTestUtils.setField(orderHooksCNFM, "costNormsForMaterialsService", costNormsForMaterialsService);
        ReflectionTestUtils.setField(orderHooksCNFM, "dataDefinitionService", dataDefinitionService);

        when(order.getDataDefinition()).thenReturn(orderDD);
        when(dataDefinitionService.get("costNormsForMaterials", "technologyInstOperProductInComp")).thenReturn(
                techInsOperCompProdInDD);

    }

    @Test
    public void shouldReturnWhenTechnologyIsNull() throws Exception {
        // when
        orderHooksCNFM.fillOrderOperationProductsInComponents(orderDD, order);
    }

    @Test
    public void shouldReturnWhenProductQuantitiesFromTechnologyIsnotEmpty() throws Exception {
        // given
        Long orderId = 1L;
        Long technologyId = 2L;
        when(order.getBelongsToField("technology")).thenReturn(technology);
        when(order.getId()).thenReturn(orderId);
        when(orderDD.get(orderId)).thenReturn(order);
        when(technology.getId()).thenReturn(technologyId);
        // when
        orderHooksCNFM.fillOrderOperationProductsInComponents(orderDD, order);
    }

    // TODO LUPO fix problem with test
    @Ignore
    @Test
    public void shouldCreatetTechnologyInstOperProductInComp() throws Exception {
        // given
        Long orderId = 1L;
        Long technologyId = 2L;
        BigDecimal nominalCost1 = BigDecimal.TEN;
        BigDecimal nominalCost2 = BigDecimal.TEN;

        when(productQuantities.entrySet()).thenReturn(entrySet);
        when(entrySet.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true, true, false);
        when(iterator.next()).thenReturn(entry1, entry2);

        when(entry1.getKey()).thenReturn(prod1.getId());
        when(entry2.getKey()).thenReturn(prod2.getId());

        when(prod1.getDecimalField("nominalCost")).thenReturn(nominalCost1);
        when(prod2.getDecimalField("nominalCost")).thenReturn(nominalCost2);

        when(order.getBelongsToField("technology")).thenReturn(technology);
        when(order.getId()).thenReturn(orderId);
        when(orderDD.get(orderId)).thenReturn(orderFromDB);
        when(technology.getId()).thenReturn(technologyId);

        when(costNormsForMaterialsService.getProductQuantitiesFromTechnology(technologyId)).thenReturn(productQuantities);
        when(techInsOperCompProdInDD.create()).thenReturn(techInsOperCompProdIn1, techInsOperCompProdIn2);
        when(techInsOperCompProdIn1.getDataDefinition()).thenReturn(techInsOperCompProdInDD);
        when(techInsOperCompProdIn2.getDataDefinition()).thenReturn(techInsOperCompProdInDD);
        when(techInsOperCompProdInDD.save(techInsOperCompProdIn1)).thenReturn(techInsOperCompProdIn1, techInsOperCompProdIn2);
        // when
        orderHooksCNFM.fillOrderOperationProductsInComponents(orderDD, order);

        // then
        Mockito.verify(techInsOperCompProdIn1).setField("order", order);
        Mockito.verify(techInsOperCompProdIn1).setField("product", prod1);
        Mockito.verify(techInsOperCompProdIn1).setField("nominalCost", nominalCost1);
        Mockito.verify(techInsOperCompProdIn2).setField("order", order);
        Mockito.verify(techInsOperCompProdIn2).setField("product", prod2);
        Mockito.verify(techInsOperCompProdIn2).setField("nominalCost", nominalCost2);

    }
}
