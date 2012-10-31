/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.2.0-SNAPSHOT
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
package com.qcadoo.mes.operationalTasksForOrders.hooks;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.qcadoo.mes.operationalTasks.constants.OperationalTasksConstants;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityList;
import com.qcadoo.model.api.search.SearchCriteriaBuilder;
import com.qcadoo.model.api.search.SearchCriterion;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.model.api.search.SearchResult;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SearchRestrictions.class)
public class OrderHooksOTFOTest {

    private OrderHooksOTFO hooksOTFO;

    @Mock
    private Entity entity, order, prodLine, orderProdLine, task1;

    @Mock
    private DataDefinition dataDefinition, operationalTasksDD;

    @Mock
    private DataDefinitionService dataDefinitionService;

    @Mock
    private SearchCriteriaBuilder builder;

    @Mock
    private SearchResult result;

    @Before
    public void init() {
        hooksOTFO = new OrderHooksOTFO();
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(SearchRestrictions.class);
        ReflectionTestUtils.setField(hooksOTFO, "dataDefinitionService", dataDefinitionService);

        when(
                dataDefinitionService.get(OperationalTasksConstants.PLUGIN_IDENTIFIER,
                        OperationalTasksConstants.MODEL_OPERATIONAL_TASK)).thenReturn(operationalTasksDD);
        when(operationalTasksDD.find()).thenReturn(builder);
        SearchCriterion criterion = SearchRestrictions.belongsTo("order", order);
        when(builder.add(criterion)).thenReturn(builder);
        when(builder.list()).thenReturn(result);
    }

    private EntityList mockEntityList(List<Entity> list) {
        EntityList entityList = mock(EntityList.class);
        when(entityList.iterator()).thenReturn(list.iterator());
        return entityList;
    }

    @Test
    public void shouldReturnWhenEntityIdIsNull() throws Exception {
        // given
        Mockito.when(order.getId()).thenReturn(null);

        // when
        hooksOTFO.changedProductionLine(dataDefinition, order);
        // then
    }

    @Test
    public void shouldReturnWhenProductionLineIsThisSame() throws Exception {
        // given
        Long orderId = 1L;
        when(entity.getId()).thenReturn(orderId);
        when(dataDefinition.get(orderId)).thenReturn(order);
        when(order.getBelongsToField(OrderFields.PRODUCTION_LINE)).thenReturn(orderProdLine);
        when(entity.getBelongsToField(OrderFields.PRODUCTION_LINE)).thenReturn(orderProdLine);

        // when
        hooksOTFO.changedProductionLine(dataDefinition, entity);
        // then
    }

    @Test
    public void shouldChangedProdLineWhenProdLineIsNullify() throws Exception {
        // given
        Long orderId = 1L;
        when(entity.getId()).thenReturn(orderId);
        when(dataDefinition.get(orderId)).thenReturn(order);
        when(order.getBelongsToField(OrderFields.PRODUCTION_LINE)).thenReturn(orderProdLine);
        when(entity.getBelongsToField(OrderFields.PRODUCTION_LINE)).thenReturn(null);
        EntityList tasks = mockEntityList(Lists.newArrayList(task1));

        when(result.getEntities()).thenReturn(tasks);
        // when
        hooksOTFO.changedProductionLine(dataDefinition, entity);
        // then

        Mockito.verify(task1).setField(OrderFields.PRODUCTION_LINE, null);
    }

    @Test
    public void shouldChangedProdLineWhenProdLineIsChanging() throws Exception {
        // given
        Long orderId = 1L;
        when(entity.getId()).thenReturn(orderId);
        when(dataDefinition.get(orderId)).thenReturn(order);
        when(order.getBelongsToField(OrderFields.PRODUCTION_LINE)).thenReturn(orderProdLine);
        when(entity.getBelongsToField(OrderFields.PRODUCTION_LINE)).thenReturn(prodLine);
        EntityList tasks = mockEntityList(Lists.newArrayList(task1));

        when(result.getEntities()).thenReturn(tasks);
        // when
        hooksOTFO.changedProductionLine(dataDefinition, entity);
        // then

        Mockito.verify(task1).setField(OrderFields.PRODUCTION_LINE, prodLine);
    }
}
