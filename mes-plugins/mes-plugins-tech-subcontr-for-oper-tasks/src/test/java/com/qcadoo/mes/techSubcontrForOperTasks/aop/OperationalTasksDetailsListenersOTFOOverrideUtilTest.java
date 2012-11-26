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
package com.qcadoo.mes.techSubcontrForOperTasks.aop;

import static com.qcadoo.mes.operationalTasks.constants.OperationalTasksFields.DESCRIPTION;
import static com.qcadoo.mes.operationalTasks.constants.OperationalTasksFields.NAME;
import static com.qcadoo.mes.operationalTasks.constants.OperationalTasksFields.PRODUCTION_LINE;
import static com.qcadoo.mes.operationalTasksForOrders.constants.OperationalTasksOTFOFields.ORDER;
import static com.qcadoo.mes.operationalTasksForOrders.constants.OperationalTasksOTFOFields.TECHNOLOGY_INSTANCE_OPERATION_COMPONENT;
import static com.qcadoo.mes.techSubcontracting.constants.TechnologyInstanceOperCompFieldsFieldsTS.IS_SUBCONTRACTING;
import static com.qcadoo.mes.technologies.constants.TechnologyInstanceOperCompFields.COMMENT;
import static com.qcadoo.mes.technologies.constants.TechnologyInstanceOperCompFields.OPERATION;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.LookupComponent;

public class OperationalTasksDetailsListenersOTFOOverrideUtilTest {

    private OperationalTasksDetailsListenersOTFOOverrideUtil util;

    @Mock
    private ViewDefinitionState viewDefinitionState;

    @Mock
    private LookupComponent orderLookup, techInstOperCompLookup;

    @Mock
    private FieldComponent productionLineLookup, nameField, descriptionField;

    @Mock
    private Entity order, techInstOperComp, productionLine, operation;

    @Before
    public void init() {
        util = new OperationalTasksDetailsListenersOTFOOverrideUtil();

        MockitoAnnotations.initMocks(this);

        when(viewDefinitionState.getComponentByReference(ORDER)).thenReturn(orderLookup);
        when(viewDefinitionState.getComponentByReference(TECHNOLOGY_INSTANCE_OPERATION_COMPONENT)).thenReturn(
                techInstOperCompLookup);
        when(viewDefinitionState.getComponentByReference(PRODUCTION_LINE)).thenReturn(productionLineLookup);
        when(viewDefinitionState.getComponentByReference(NAME)).thenReturn(nameField);
        when(viewDefinitionState.getComponentByReference(DESCRIPTION)).thenReturn(descriptionField);

    }

    @Test
    public void shouldClearOperationFieldWhenOrderIsChanged() throws Exception {

        // when
        util.checkIfOperationIsSubcontracted(viewDefinitionState);
        // then
        Mockito.verify(techInstOperCompLookup).setFieldValue(null);
    }

    @Test
    public void shouldClearFieldWhenOrderIsNull() throws Exception {
        when(orderLookup.getEntity()).thenReturn(null);

        util.checkIfOperationIsSubcontracted(viewDefinitionState);
        // then
        Mockito.verify(techInstOperCompLookup).setFieldValue(null);
    }

    @Test
    public void shouldReturnWhenTechInstOperCompIsNull() throws Exception {
        // given
        Long productionLineId = 1L;
        when(orderLookup.getEntity()).thenReturn(order);
        when(techInstOperCompLookup.getEntity()).thenReturn(null);
        when(order.getBelongsToField(PRODUCTION_LINE)).thenReturn(productionLine);
        when(productionLine.getId()).thenReturn(productionLineId);
        util.checkIfOperationIsSubcontracted(viewDefinitionState);
        // then
        Mockito.verify(techInstOperCompLookup).setFieldValue(null);
    }

    @Test
    public void shouldSetNullInProductionLineLookupWhenOperationIsNotSubcontrAndOrderProductionLineIsNull() throws Exception {
        // given
        String name = "name";
        when(orderLookup.getEntity()).thenReturn(order);
        when(order.getBelongsToField(PRODUCTION_LINE)).thenReturn(null);
        when(techInstOperCompLookup.getEntity()).thenReturn(techInstOperComp);
        when(techInstOperComp.getBooleanField(IS_SUBCONTRACTING)).thenReturn(true);
        when(techInstOperComp.getBelongsToField(OPERATION)).thenReturn(operation);
        when(operation.getStringField(NAME)).thenReturn(name);

        util.checkIfOperationIsSubcontracted(viewDefinitionState);
        // then
        Mockito.verify(techInstOperCompLookup).setFieldValue(null);
        Mockito.verify(productionLineLookup).setFieldValue(null);
    }

    @Test
    public void shouldSetProductionLineInLookupWhenOperationIsNotSubcontr() throws Exception {
        // given
        Long productionLineId = 1L;

        when(orderLookup.getEntity()).thenReturn(order);
        when(order.getBelongsToField(PRODUCTION_LINE)).thenReturn(productionLine);
        when(techInstOperCompLookup.getEntity()).thenReturn(techInstOperComp);
        when(techInstOperComp.getBooleanField(IS_SUBCONTRACTING)).thenReturn(false);
        when(productionLine.getId()).thenReturn(productionLineId);
        util.checkIfOperationIsSubcontracted(viewDefinitionState);
        // then
        Mockito.verify(techInstOperCompLookup).setFieldValue(null);
        Mockito.verify(productionLineLookup).setFieldValue(productionLineId);
    }

    @Test
    public void shouldSetOperationCommentAndName() throws Exception {
        // given
        String comment = "Comment";
        String name = "name";
        when(techInstOperCompLookup.getEntity()).thenReturn(techInstOperComp);
        when(techInstOperComp.getBooleanField(IS_SUBCONTRACTING)).thenReturn(true);
        when(orderLookup.getEntity()).thenReturn(order);
        when(order.getBelongsToField(PRODUCTION_LINE)).thenReturn(null);
        when(techInstOperComp.getStringField(COMMENT)).thenReturn(comment);
        when(techInstOperComp.getBelongsToField(OPERATION)).thenReturn(operation);
        when(operation.getStringField(NAME)).thenReturn(name);
        // when
        util.setOperationalNameAndDescriptionForSubcontractedOperation(viewDefinitionState);
        // then
        Mockito.verify(descriptionField).setFieldValue(comment);
        Mockito.verify(nameField).setFieldValue(name);
    }

    @Test
    public void shouldSetNullToFieldWhenTechInstOpCompIsNull() throws Exception {
        // given
        when(techInstOperCompLookup.getEntity()).thenReturn(null);
        // when
        util.setOperationalNameAndDescriptionForSubcontractedOperation(viewDefinitionState);
        // then
        Mockito.verify(descriptionField).setFieldValue(null);
        Mockito.verify(nameField).setFieldValue(null);
    }

}
