/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.4
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
package com.qcadoo.mes.basic;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.utils.NumberGeneratorService;

public class ProductServiceTest {

    private ProductService productService;

    @Mock
    private ViewDefinitionState view;

    @Mock
    private DataDefinitionService dataDefinitionService;

    @Mock
    private NumberGeneratorService numberGeneratorService;

    @Mock
    private DataDefinition dataDefinition;

    @Mock
    private Entity product, entity, substitute;

    @Before
    public final void init() {
        productService = new ProductService();

        MockitoAnnotations.initMocks(this);

        ReflectionTestUtils.setField(productService, "numberGeneratorService", numberGeneratorService);
        ReflectionTestUtils.setField(productService, "dataDefinitionService", dataDefinitionService);
    }

    @Test
    public void shouldReturnTrueWhenProductIsNotRemoved() throws Exception {
        // when
        boolean result = productService.checkIfProductIsNotRemoved(dataDefinition, entity);

        // then
        assertTrue(result);
    }

    @Test
    public void shouldReturnFalseWhenEntityDoesnotExists() throws Exception {
        // given
        DataDefinition productDD = Mockito.mock(DataDefinition.class);
        Long productId = 1L;
        when(entity.getBelongsToField("product")).thenReturn(product);
        when(dataDefinitionService.get("basic", "product")).thenReturn(productDD);
        when(product.getId()).thenReturn(productId);
        when(productDD.get(productId)).thenReturn(null);
        // when
        boolean result = productService.checkIfProductIsNotRemoved(dataDefinition, entity);
        // then
        assertFalse(result);
    }

    @Test
    public void shouldReturnTrueWhenProductExists() throws Exception {
        // given
        DataDefinition productDD = Mockito.mock(DataDefinition.class);
        Long productId = 1L;
        when(entity.getBelongsToField("product")).thenReturn(product);
        when(dataDefinitionService.get("basic", "product")).thenReturn(productDD);
        when(product.getId()).thenReturn(productId);
        when(productDD.get(productId)).thenReturn(product);
        // when
        boolean result = productService.checkIfProductIsNotRemoved(dataDefinition, entity);

        // then
        assertTrue(result);
    }

    @Test
    public void shouldClearExternalIdOnCopy() throws Exception {
        // given

        // when
        boolean result = productService.clearExternalIdOnCopy(dataDefinition, entity);
        // then
        assertTrue(result);

        Mockito.verify(entity).setField("externalNumber", null);
    }

    @Test
    public void shouldReturnFalseWhenSubstituteDoesnotExists() throws Exception {
        // given
        DataDefinition substituteDD = Mockito.mock(DataDefinition.class);
        Long substituteId = 1L;
        when(entity.getBelongsToField("substitute")).thenReturn(substitute);
        when(dataDefinitionService.get("basic", "substitute")).thenReturn(substituteDD);
        when(substitute.getId()).thenReturn(substituteId);
        when(substituteDD.get(substituteId)).thenReturn(null);
        // when
        boolean result = productService.checkIfSubstituteIsNotRemoved(dataDefinition, entity);
        // then
        assertFalse(result);
    }

    @Test
    public void shouldReturnTrueWhenEntityDoesnotHaveBTSubstitute() throws Exception {
        // when
        boolean result = productService.checkIfSubstituteIsNotRemoved(dataDefinition, entity);

        // then
        assertTrue(result);
    }

    @Test
    public void shouldReturnTrueWhenSubstitueExists() throws Exception {
        // given
        DataDefinition substituteDD = Mockito.mock(DataDefinition.class);
        Long substituteId = 1L;
        when(entity.getBelongsToField("substitute")).thenReturn(substitute);
        when(dataDefinitionService.get("basic", "substitute")).thenReturn(substituteDD);
        when(substitute.getId()).thenReturn(substituteId);
        when(substituteDD.get(substituteId)).thenReturn(substitute);
        // when
        boolean result = productService.checkIfSubstituteIsNotRemoved(dataDefinition, entity);
        // then
        assertTrue(result);
    }

    @Test
    public void shouldDisabledFormWhenExternalNumberIsnotNull() throws Exception {
        // given
        FormComponent form = mock(FormComponent.class);
        Long productId = 1L;
        String externalNumber = "0001";
        DataDefinition productDD = mock(DataDefinition.class);
        when(view.getComponentByReference("form")).thenReturn(form);
        when(form.getEntityId()).thenReturn(productId);
        when(dataDefinitionService.get("basic", "product")).thenReturn(productDD);
        when(productDD.get(productId)).thenReturn(product);
        when(product.getStringField("externalNumber")).thenReturn(externalNumber);
        // when
        productService.disableProductFormForExternalItems(view);
        // then
        verify(form).setFormEnabled(false);

    }

}
