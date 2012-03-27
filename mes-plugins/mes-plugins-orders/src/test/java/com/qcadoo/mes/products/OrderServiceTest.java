/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.3
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
/**
 * ***************************************************************************
 * Project: Qcadoo MES
 * Version: 0.4.8
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
package com.qcadoo.mes.products;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.mes.basic.constants.BasicConstants;
import com.qcadoo.mes.orders.OrderService;
import com.qcadoo.mes.orders.constants.OrdersConstants;
import com.qcadoo.mes.technologies.constants.TechnologiesConstants;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.ExpressionService;
import com.qcadoo.model.api.FieldDefinition;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.model.api.search.SearchResult;
import com.qcadoo.model.internal.EntityListImpl;
import com.qcadoo.model.internal.EntityTreeImpl;
import com.qcadoo.model.internal.types.BooleanType;
import com.qcadoo.model.internal.types.StringType;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.utils.NumberGeneratorService;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ EntityTreeImpl.class, EntityListImpl.class })
public class OrderServiceTest {

    private OrderService orderService;

    private DataDefinitionService dataDefinitionService;

    private TranslationService translationService;

    private NumberGeneratorService numberGeneratorService;

    @Before
    public void init() {
        dataDefinitionService = mock(DataDefinitionService.class, RETURNS_DEEP_STUBS);
        translationService = mock(TranslationService.class);
        numberGeneratorService = mock(NumberGeneratorService.class);
        ExpressionService expressionService = mock(ExpressionService.class);
        orderService = new OrderService();
        setField(orderService, "dataDefinitionService", dataDefinitionService);
        setField(orderService, "translationService", translationService);
        setField(orderService, "numberGeneratorService", numberGeneratorService);
        setField(orderService, "expressionService", expressionService);
    }

    @Test
    public void shouldClearOrderFieldsOnCopy() throws Exception {
        // given
        Entity order = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);

        // when
        boolean result = orderService.clearOrderDatesOnCopy(dataDefinition, order);
        // then
        assertTrue(result);
        verify(order).setField("state", "01pending");
        verify(order).setField("effectiveDateTo", null);
        verify(order).setField("effectiveDateFrom", null);
        verify(order).setField("doneQuantity", null);
    }

    @Test
    public void shouldChangeOrderProductToNull() throws Exception {
        // given
        FieldComponent product = mock(FieldComponent.class);
        FieldComponent technology = mock(FieldComponent.class);
        FieldComponent defaultTechnology = mock(FieldComponent.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(product.getFieldValue()).willReturn(null);

        // when
        orderService.changeOrderProduct(viewDefinitionState, product, new String[0]);

        // then
        verify(defaultTechnology).setFieldValue("");
        verify(technology).setFieldValue(null);
    }

    @Test
    public void shouldChangeOrderProductWithoutDefaultTechnology() throws Exception {
        // given
        SearchResult searchResult = mock(SearchResult.class);
        FieldComponent product = mock(FieldComponent.class);
        FieldComponent technology = mock(FieldComponent.class);
        FieldComponent defaultTechnology = mock(FieldComponent.class);
        FieldDefinition masterField = mock(FieldDefinition.class);
        FieldDefinition productField = mock(FieldDefinition.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(product.getFieldValue()).willReturn(13L);
        given(dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY))
                .willReturn(dataDefinition);
        given(dataDefinition.find().setMaxResults(1).list()).willReturn(searchResult);
        given(dataDefinition.getField("master")).willReturn(masterField);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(masterField.getType()).willReturn(new BooleanType());
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(0);

        // when
        orderService.changeOrderProduct(viewDefinitionState, product, new String[0]);

        // then
        verify(defaultTechnology).setFieldValue("");
        verify(technology).setFieldValue(null);
    }

    @Test
    public void shouldChangeOrderProductWithDefaultTechnology() throws Exception {
        // given
        SearchResult searchResult = mock(SearchResult.class);
        Entity entity = mock(Entity.class);
        FieldComponent product = mock(FieldComponent.class);
        FieldComponent technology = mock(FieldComponent.class);
        FieldComponent defaultTechnology = mock(FieldComponent.class);
        FieldDefinition masterField = mock(FieldDefinition.class);
        FieldDefinition productField = mock(FieldDefinition.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(product.getFieldValue()).willReturn(13L);
        given(dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY))
                .willReturn(dataDefinition);
        given(
                dataDefinition.find().setMaxResults(1).add(SearchRestrictions.eq("master", any())).belongsTo(anyString(), any())
                        .list()).willReturn(searchResult);
        given(dataDefinition.getField("master")).willReturn(masterField);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(masterField.getType()).willReturn(new BooleanType());
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(1);
        given(searchResult.getEntities()).willReturn(Collections.singletonList(entity));
        given(entity.getId()).willReturn(117L);

        // when
        orderService.changeOrderProduct(viewDefinitionState, product, new String[0]);

        // then
        verify(defaultTechnology).setFieldValue("");
        verify(technology).setFieldValue(117L);
    }

    @Test
    public void shouldSetAndDisableState() throws Exception {
        // given
        FormComponent form = mock(FormComponent.class);
        FieldComponent orderState = mock(FieldComponent.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(form);
        given(viewDefinitionState.getComponentByReference("state")).willReturn(orderState);
        given(form.getEntityId()).willReturn(null);

        // when
        orderService.setAndDisableState(viewDefinitionState);

        // then
        verify(orderState).setEnabled(false);
        verify(orderState).setFieldValue("01pending");
    }

    @Test
    public void shouldDisableState() throws Exception {
        // given
        FormComponent form = mock(FormComponent.class);
        FieldComponent orderState = mock(FieldComponent.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(form);
        given(viewDefinitionState.getComponentByReference("state")).willReturn(orderState);
        given(form.getEntityId()).willReturn(1L);

        // when
        orderService.setAndDisableState(viewDefinitionState);

        // then
        verify(orderState).setEnabled(false);
        verify(orderState, never()).setFieldValue("01pending");
    }

    @Test
    public void shouldGenerateOrderNumber() throws Exception {
        // given
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);

        // when
        orderService.generateOrderNumber(viewDefinitionState);

        // then
        verify(numberGeneratorService).generateAndInsertNumber(viewDefinitionState, OrdersConstants.PLUGIN_IDENTIFIER,
                OrdersConstants.MODEL_ORDER, "form", "number");
    }

    @Test
    public void shouldNotFillDefaultTechnologyIfThereIsNoProduct() throws Exception {
        // given
        FieldComponent product = mock(FieldComponent.class);
        FieldComponent defaultTechnology = mock(FieldComponent.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("product")).willReturn(product);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(product.getFieldValue()).willReturn(null);

        // when
        orderService.fillDefaultTechnology(viewDefinitionState);

        // then
        verify(defaultTechnology, never()).setFieldValue(anyString());
    }

    @Test
    public void shouldNotFillDefaultTechnologyIfThereIsNoDefaultTechnology() throws Exception {
        // given
        FieldComponent product = mock(FieldComponent.class);
        FieldComponent defaultTechnology = mock(FieldComponent.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("product")).willReturn(product);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(product.getFieldValue()).willReturn(117L);

        FieldDefinition masterField = mock(FieldDefinition.class);
        FieldDefinition productField = mock(FieldDefinition.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        SearchResult searchResult = mock(SearchResult.class);
        given(dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY))
                .willReturn(dataDefinition);
        given(dataDefinition.find().setMaxResults(1).list()).willReturn(searchResult);
        given(dataDefinition.getField("master")).willReturn(masterField);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(masterField.getType()).willReturn(new BooleanType());
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(0);

        // when
        orderService.fillDefaultTechnology(viewDefinitionState);

        // then
        verify(defaultTechnology, never()).setFieldValue(anyString());
    }

    @Test
    public void shouldFillDefaultTechnology() throws Exception {
        // given
        FieldComponent product = mock(FieldComponent.class);
        FieldComponent defaultTechnology = mock(FieldComponent.class);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("product")).willReturn(product);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(product.getFieldValue()).willReturn(117L);

        Entity entity = mock(Entity.class);
        FieldDefinition masterField = mock(FieldDefinition.class);
        FieldDefinition productField = mock(FieldDefinition.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        SearchResult searchResult = mock(SearchResult.class);
        given(dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY))
                .willReturn(dataDefinition);
        given(
                dataDefinition.find().setMaxResults(1).add(SearchRestrictions.eq("master", any())).belongsTo(anyString(), any())
                        .list()).willReturn(searchResult);
        given(dataDefinition.getField("master")).willReturn(masterField);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(masterField.getType()).willReturn(new BooleanType());
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(1);
        given(searchResult.getEntities()).willReturn(Collections.singletonList(entity));

        // when
        orderService.fillDefaultTechnology(viewDefinitionState);

        // then
        verify(defaultTechnology).setFieldValue(anyString());
    }

    @Test
    public void shouldDisableTechnologyIfThereIsNoProduct() throws Exception {
        // given
        FieldComponent product = mock(FieldComponent.class);
        FieldComponent technology = mock(FieldComponent.class);
        FieldComponent defaultTechnology = mock(FieldComponent.class);
        FieldComponent plannedQuantity = mock(FieldComponent.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("product")).willReturn(product);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(viewDefinitionState.getComponentByReference("plannedQuantity")).willReturn(plannedQuantity);
        given(product.getFieldValue()).willReturn(null);

        // when
        orderService.disableTechnologiesIfProductDoesNotAny(viewDefinitionState);

        // then
        verify(defaultTechnology).setEnabled(false);
        verify(technology).setRequired(false);
        verify(plannedQuantity).setRequired(false);
    }

    @Test
    public void shouldDisableTechnologyIfProductHasNoTechnologies() throws Exception {
        // given
        FieldComponent product = mock(FieldComponent.class);
        FieldComponent technology = mock(FieldComponent.class);
        FieldComponent defaultTechnology = mock(FieldComponent.class);
        FieldComponent plannedQuantity = mock(FieldComponent.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("product")).willReturn(product);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(viewDefinitionState.getComponentByReference("plannedQuantity")).willReturn(plannedQuantity);
        given(product.getFieldValue()).willReturn(117L);

        FieldDefinition productField = mock(FieldDefinition.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        SearchResult searchResult = mock(SearchResult.class);
        given(dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY))
                .willReturn(dataDefinition);
        given(dataDefinition.find().setMaxResults(1).list()).willReturn(searchResult);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(0);

        // when
        orderService.disableTechnologiesIfProductDoesNotAny(viewDefinitionState);

        // then
        verify(defaultTechnology).setEnabled(false);
        verify(technology).setRequired(false);
        verify(plannedQuantity).setRequired(false);
    }

    @Test
    public void shouldSetTechnologyAndPlannedQuantityAsRequired() throws Exception {
        // given
        FieldComponent product = mock(FieldComponent.class);
        FieldComponent technology = mock(FieldComponent.class);
        FieldComponent defaultTechnology = mock(FieldComponent.class);
        FieldComponent plannedQuantity = mock(FieldComponent.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("product")).willReturn(product);
        given(viewDefinitionState.getComponentByReference("defaultTechnology")).willReturn(defaultTechnology);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(viewDefinitionState.getComponentByReference("plannedQuantity")).willReturn(plannedQuantity);
        given(product.getFieldValue()).willReturn(117L);

        FieldDefinition productField = mock(FieldDefinition.class);
        DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        SearchResult searchResult = mock(SearchResult.class);
        given(dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY))
                .willReturn(dataDefinition);
        given(dataDefinition.find().setMaxResults(1).belongsTo(anyString(), any()).list()).willReturn(searchResult);
        given(dataDefinition.getField("product")).willReturn(productField);
        given(productField.getType()).willReturn(new StringType());
        given(searchResult.getTotalNumberOfEntities()).willReturn(1);

        // when
        orderService.disableTechnologiesIfProductDoesNotAny(viewDefinitionState);

        // then
        verify(defaultTechnology).setEnabled(false);
        verify(technology).setRequired(true);
        verify(plannedQuantity).setRequired(true);
    }

    @Test
    public void shouldNotDisableFormIfThereIsNoIdentifier() throws Exception {
        // given
        FormComponent order = mock(FormComponent.class);
        FieldComponent technology = mock(FieldComponent.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(order);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(order.getFieldValue()).willReturn(null);

        // when
        orderService.disableFieldOrder(viewDefinitionState);

        // then
        verify(order).setFormEnabled(true);
        verify(technology).setEnabled(true);
    }

    @Test
    public void shouldNotDisableFormIfThereIsNoOrder() throws Exception {
        // given
        FormComponent order = mock(FormComponent.class);
        FieldComponent technology = mock(FieldComponent.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(order);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(order.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(117L)).willReturn(
                null);

        // when
        orderService.disableFieldOrder(viewDefinitionState);

        // then
        verify(order).setFormEnabled(true);
        verify(technology).setEnabled(true);
    }

    @Test
    public void shouldNotDisableFormIfOrderIsNotDone() throws Exception {
        // given
        FormComponent order = mock(FormComponent.class);
        FieldComponent technology = mock(FieldComponent.class);
        Entity entity = mock(Entity.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(order);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(order.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(117L)).willReturn(
                entity);
        given(entity.getStringField("state")).willReturn("01pending");
        given(order.isValid()).willReturn(true);

        // when
        orderService.disableFieldOrder(viewDefinitionState);

        // then
        verify(order).setFormEnabled(false);
        verify(technology).setEnabled(false);
    }

    @Test
    public void shouldNotDisableFormIfOrderIsNotValid() throws Exception {
        // given
        FormComponent order = mock(FormComponent.class);
        FieldComponent technology = mock(FieldComponent.class);
        Entity entity = mock(Entity.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(order);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(order.getFieldValue()).willReturn(117L);
        given(dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(117L)).willReturn(
                entity);
        given(entity.getStringField("state")).willReturn("04completed");
        given(order.isValid()).willReturn(false);

        // when
        orderService.disableFieldOrder(viewDefinitionState);

        // then
        verify(order).setFormEnabled(true);
        verify(technology).setEnabled(true);
    }

    @Test
    public void shouldNotDisableFormForDoneOrder() throws Exception {
        // given
        FormComponent order = mock(FormComponent.class);
        FieldComponent technology = mock(FieldComponent.class);
        Entity entity = mock(Entity.class);

        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(viewDefinitionState.getComponentByReference("form")).willReturn(order);
        given(viewDefinitionState.getComponentByReference("technology")).willReturn(technology);
        given(order.getEntityId()).willReturn(117L);
        given(dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(117L)).willReturn(
                entity);
        given(entity.getStringField("state")).willReturn("04completed");
        given(order.isValid()).willReturn(true);

        // when
        orderService.disableFieldOrder(viewDefinitionState);

        // then
        verify(order).setFormEnabled(false);
        verify(technology).setEnabled(false);
    }

    @Test
    public void shouldReturnTrueForValidOrderDates() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity entity = mock(Entity.class);
        given(entity.getField("dateFrom")).willReturn(new Date(System.currentTimeMillis() - 10000));
        given(entity.getField("dateTo")).willReturn(new Date());

        // when
        boolean results = orderService.checkOrderDates(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForNullFromDate() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity entity = mock(Entity.class);
        given(entity.getField("dateFrom")).willReturn(null);
        given(entity.getField("dateTo")).willReturn(new Date());

        // when
        boolean results = orderService.checkOrderDates(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForNullToDate() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity entity = mock(Entity.class);
        given(entity.getField("dateFrom")).willReturn(new Date());
        given(entity.getField("dateTo")).willReturn(null);

        // when
        boolean results = orderService.checkOrderDates(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnFalseForInvalidOrderDates() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        FieldDefinition dateToField = mock(FieldDefinition.class);
        Entity entity = mock(Entity.class);
        given(entity.getField("dateFrom")).willReturn(new Date());
        given(entity.getField("dateTo")).willReturn(new Date(System.currentTimeMillis() - 10000));
        given(dataDefinition.getField("dateTo")).willReturn(dateToField);

        // when
        boolean results = orderService.checkOrderDates(dataDefinition, entity);

        // then
        assertFalse(results);
        verify(entity).addError(dateToField, "orders.validate.global.error.datesOrder");
    }

    @Test
    public void shouldReturnTrueForPlannedQuantityValidationIfThereIsNoProduct() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity entity = mock(Entity.class);
        given(entity.getBelongsToField("product")).willReturn(null);

        // when
        boolean results = orderService.checkOrderPlannedQuantity(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForPlannedQuantityValidation() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity entity = mock(Entity.class);
        Entity product = mock(Entity.class);
        given(entity.getBelongsToField("product")).willReturn(product);
        given(entity.getField("plannedQuantity")).willReturn(BigDecimal.ONE);

        // when
        boolean results = orderService.checkOrderPlannedQuantity(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnFalseForPlannedQuantityValidation() throws Exception {
        // given
        DataDefinition dataDefinition = mock(DataDefinition.class);
        FieldDefinition plannedQuantityField = mock(FieldDefinition.class);
        Entity entity = mock(Entity.class);
        Entity product = mock(Entity.class);
        given(entity.getBelongsToField("product")).willReturn(product);
        given(entity.getField("plannedQuantity")).willReturn(null);
        given(dataDefinition.getField("plannedQuantity")).willReturn(plannedQuantityField);

        // when
        boolean results = orderService.checkOrderPlannedQuantity(dataDefinition, entity);

        // then
        assertFalse(results);
        verify(entity).addError(plannedQuantityField, "orders.validate.global.error.plannedQuantityError");
    }

    @Test
    public void shouldNotFillOrderDates() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);

        // when
        orderService.fillOrderDates(dataDefinition, entity);

        // then
        verify(entity, atLeastOnce()).getField("state");
        verifyNoMoreInteractions(entity);
    }

    @Test
    public void shouldFillInProgressOrderDates() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        given(entity.getField("state")).willReturn("03inProgress");

        // when
        orderService.fillOrderDates(dataDefinition, entity);

        // then
        verify(entity).setField(eq("effectiveDateFrom"), any(Date.class));
    }

    @Test
    public void shouldFillDoneOrderDates() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        given(entity.getField("state")).willReturn("04completed");

        // when
        orderService.fillOrderDates(dataDefinition, entity);

        // then
        verify(entity).setField(eq("effectiveDateFrom"), any(Date.class));
        verify(entity).setField(eq("effectiveDateTo"), any(Date.class));
    }

    @Test
    public void shouldNotFillExistingDates() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        given(entity.getField("state")).willReturn("04completed");
        given(entity.getField("effectiveDateFrom")).willReturn(new Date());
        given(entity.getField("effectiveDateTo")).willReturn(new Date());

        // when
        orderService.fillOrderDates(dataDefinition, entity);

        // then
        verify(entity, never()).setField(eq("effectiveDateFrom"), any(Date.class));
        verify(entity, never()).setField(eq("effectiveDateTo"), any(Date.class));
    }

    @Test
    public void shouldReturnTrueForOperationValidationIfThereIsNoOrder() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);

        // when
        boolean results = orderService.checkIfOrderTechnologyHasOperations(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForOperationValidationIfOrderDoesNotHaveTechnology() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        Entity order = mock(Entity.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        given(entity.getBelongsToField("order")).willReturn(order);

        // when
        boolean results = orderService.checkIfOrderTechnologyHasOperations(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForOperationValidationIfTechnologyHasOperations() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        Entity order = mock(Entity.class);
        Entity technology = mock(Entity.class);
        EntityTreeImpl operations = mock(EntityTreeImpl.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        given(entity.getBelongsToField("order")).willReturn(order);
        given(order.getField("technology")).willReturn(technology);
        given(order.getBelongsToField("technology")).willReturn(technology);
        given(technology.getTreeField("operationComponents")).willReturn(operations);
        given(operations.isEmpty()).willReturn(false);

        // when
        boolean results = orderService.checkIfOrderTechnologyHasOperations(dataDefinition, entity);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldReturnTrueForOperationValidationIfTechnologyDoesNotHaveOperations() throws Exception {
        // given
        Entity entity = mock(Entity.class);
        Entity order = mock(Entity.class);
        Entity technology = mock(Entity.class);
        EntityTreeImpl operations = mock(EntityTreeImpl.class);
        DataDefinition dataDefinition = mock(DataDefinition.class);
        given(entity.getBelongsToField("order")).willReturn(order);
        given(order.getField("technology")).willReturn(technology);
        given(order.getBelongsToField("technology")).willReturn(technology);
        given(technology.getTreeField("operationComponents")).willReturn(operations);
        given(operations.isEmpty()).willReturn(true);
        FieldDefinition orderField = mock(FieldDefinition.class);
        given(dataDefinition.getField("order")).willReturn(orderField);

        // when
        boolean results = orderService.checkIfOrderTechnologyHasOperations(dataDefinition, entity);

        // then
        assertFalse(results);
        verify(entity).addError(orderField, "orders.validate.global.error.orderTechnologyMustHaveOperation");
    }

    private void prepareCheckIfAllQualityControlsAreClosed(final Entity order, final boolean expected) {
        if (expected) {
            DataDefinition dataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
            given(order.getBelongsToField("technology").getField("qualityControlType").toString()).willReturn("01forBatch");
            given(dataDefinitionService.get("qualityControls", "qualityControl")).willReturn(dataDefinition);
            given(dataDefinition.find().list().getTotalNumberOfEntities()).willReturn(0);
        } else {
            given(order.getBelongsToField("technology").getField("qualityControlType")).willReturn(null);
        }
    }

    private void prepareIsQualityControlAutoCheckEnabled(final boolean expected) {
        if (expected) {
            Entity entity = mock(Entity.class);
            List<Entity> entities = new ArrayList<Entity>();
            entities.add(entity);
            given(entity.getField("checkDoneOrderForQuality")).willReturn(true);
            given(
                    dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, BasicConstants.MODEL_PARAMETER).find()
                            .setMaxResults(1).list().getEntities().size()).willReturn(1);
            given(
                    dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, BasicConstants.MODEL_PARAMETER).find()
                            .setMaxResults(1).list().getEntities()).willReturn(entities);
        } else {
            given(
                    dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, BasicConstants.MODEL_PARAMETER).find()
                            .setMaxResults(1).list().getEntities().size()).willReturn(0);
        }
    }

    private void prepareCheckRequiredBatch(final Entity order, final boolean expected) {
        if (expected) {
            given(order.getField("technology")).willReturn(null);
        } else {
            Entity technology = mock(Entity.class);
            given(order.getField("technology")).willReturn(technology);
            given(order.getHasManyField("genealogies").size()).willReturn(0);
            given(technology.getField("batchRequired")).willReturn(false);
            given(technology.getField("shiftFeatureRequired")).willReturn(true);
        }
    }

    @Test
    public void shouldFailCheckingRequiredBatchForBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getHasManyField("genealogies").isEmpty()).willReturn(true);
        given(order.getField("technology")).willReturn(technology);
        given(technology.getField("batchRequired")).willReturn(true);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    public void shouldFailCheckingRequiredBatchForPostBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").isEmpty()).willReturn(true);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(true);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    public void shouldFailCheckingRequiredBatchForOtherBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").isEmpty()).willReturn(true);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(true);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForOperationComponentBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity operationComponent = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity operationProductInComponents = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Iterator<Entity> iterator2 = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").isEmpty()).willReturn(true);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(false);
        given(technology.getTreeField("operationComponents").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(operationComponent);
        given(operationComponent.getHasManyField("operationProductInComponents").iterator()).willReturn(iterator2);
        given(iterator2.hasNext()).willReturn(true, false);
        given(iterator2.next()).willReturn(operationProductInComponents);
        given(operationProductInComponents.getField("batchRequired")).willReturn(true);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").isEmpty()).willReturn(false);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(true);
        given(genealogy.getField("batch")).willReturn(null);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyShiftBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").isEmpty()).willReturn(false);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(true);
        given(technology.getField("shiftFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("shiftFeatures").isEmpty()).willReturn(true);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyPostBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").isEmpty()).willReturn(false);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("shiftFeatures").isEmpty()).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("postFeatures").isEmpty()).willReturn(true);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyOtherBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(1);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("postFeatures").isEmpty()).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("otherFeatures").isEmpty()).willReturn(true);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyComponentsBatchRequired() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity productInComponent = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Iterator<Entity> iterator2 = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").isEmpty()).willReturn(false);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("otherFeatures").isEmpty()).willReturn(false);
        given(genealogy.getHasManyField("productInComponents").iterator()).willReturn(iterator2);
        given(iterator2.hasNext()).willReturn(true, false);
        given(iterator2.next()).willReturn(productInComponent);
        given(productInComponent.getBelongsToField("productInComponent").getField("batchRequired")).willReturn(true);
        given(productInComponent.getHasManyField("batch").isEmpty()).willReturn(true);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertFalse(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyComponentsBatchRequired2() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity productInComponent = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Iterator<Entity> iterator2 = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(1);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("otherFeatures").size()).willReturn(1);
        given(genealogy.getHasManyField("productInComponents").iterator()).willReturn(iterator2);
        given(iterator2.hasNext()).willReturn(true, false);
        given(iterator2.next()).willReturn(productInComponent);
        given(productInComponent.getBelongsToField("productInComponent").getField("batchRequired")).willReturn(true);
        given(productInComponent.getHasManyField("batch").size()).willReturn(1);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertTrue(results);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFailCheckingRequiredBatchForGenealogyComponentsBatchRequired3() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity productInComponent = mock(Entity.class, RETURNS_DEEP_STUBS);
        Iterator<Entity> iterator = mock(Iterator.class);
        Iterator<Entity> iterator2 = mock(Iterator.class);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity genealogy = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(1);
        given(order.getHasManyField("genealogies").iterator()).willReturn(iterator);
        given(iterator.hasNext()).willReturn(true, false);
        given(iterator.next()).willReturn(genealogy);
        given(technology.getField("batchRequired")).willReturn(false);
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(true);
        given(genealogy.getHasManyField("otherFeatures").size()).willReturn(1);
        given(genealogy.getHasManyField("productInComponents").iterator()).willReturn(iterator2);
        given(iterator2.hasNext()).willReturn(true, false);
        given(iterator2.next()).willReturn(productInComponent);
        given(productInComponent.getBelongsToField("productInComponent").getField("batchRequired")).willReturn(false);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldSuccessCheckingRequiredBatch() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        Entity technology = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(technology);
        given(order.getHasManyField("genealogies").size()).willReturn(1);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertTrue(results);
    }

    @Test
    public void shouldSuccessCheckingRequiredBatchIfThereIsNoTechnology() throws Exception {
        // given
        Entity order = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(order.getField("technology")).willReturn(null);

        // when
        boolean results = callCheckRequiredBatch(order);

        // then
        assertTrue(results);
    }

    private boolean callCheckRequiredBatch(final Entity order) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        Method method = OrderService.class.getDeclaredMethod("checkRequiredBatch", Entity.class);
        method.setAccessible(true);
        boolean results = (Boolean) method.invoke(orderService, order);
        return results;
    }

}
