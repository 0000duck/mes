package com.qcadoo.mes.costNormsForOperation;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.qcadoo.mes.basic.util.CurrencyService;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;

public class CostNormsForOperationServiceTest {

    private CostNormsForOperationService costNormsForOperationService;

    @Mock
    private ViewDefinitionState view;

    @Mock
    private FieldComponent field1, field2, field3, field4;

    @Mock
    private ComponentState state;

    @Mock
    private FormComponent form;

    @Mock
    private DataDefinitionService dataDefinitionService;

    @Mock
    private DataDefinition operationDD, techInsOperCompDD;

    @Mock
    private Entity operationEntity, technologyInstanceOperCompEntity, techOperCompEntity;

    @Mock
    private Object obj1, obj2, obj3, obj4;

    @Mock
    private CurrencyService currencyService;

    @Before
    public void init() {
        costNormsForOperationService = new CostNormsForOperationService();
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(costNormsForOperationService, "dataDefinitionService", dataDefinitionService);
        ReflectionTestUtils.setField(costNormsForOperationService, "currencyService", currencyService);

        when(dataDefinitionService.get("technologies", "operation")).thenReturn(operationDD);

        when(view.getComponentByReference("operation")).thenReturn(state);
        when(view.getComponentByReference("form")).thenReturn(form);

        when(view.getComponentByReference("pieceworkCost")).thenReturn(field1);
        when(view.getComponentByReference("numberOfOperations")).thenReturn(field2);
        when(view.getComponentByReference("laborHourlyCost")).thenReturn(field3);
        when(view.getComponentByReference("machineHourlyCost")).thenReturn(field4);

    }

    @Test
    public void shouldReturnWhenOperationIsNull() throws Exception {
        // when
        costNormsForOperationService.copyCostValuesFromOperation(view, state, null);
    }

    @Test
    public void shouldApplyCostNormsForGivenSource() throws Exception {
        // given
        when(state.getFieldValue()).thenReturn(1L);
        Long operationId = 1L;
        when(operationDD.get(operationId)).thenReturn(operationEntity);
        when(operationEntity.getField("pieceworkCost")).thenReturn(obj1);
        when(operationEntity.getField("numberOfOperations")).thenReturn(obj2);
        when(operationEntity.getField("laborHourlyCost")).thenReturn(obj3);
        when(operationEntity.getField("machineHourlyCost")).thenReturn(obj4);
        // when
        costNormsForOperationService.copyCostValuesFromOperation(view, state, null);
        // then
        Mockito.verify(field1).setFieldValue(obj1);
        Mockito.verify(field2).setFieldValue(obj2);
        Mockito.verify(field3).setFieldValue(obj3);
        Mockito.verify(field4).setFieldValue(obj4);

    }

    @Test
    public void shouldCopyCostValuesFromTechnology() throws Exception {
        // given
        Long techInsOperCompId = 1L;
        when(view.getComponentByReference("form")).thenReturn(form);
        when(form.getEntity()).thenReturn(technologyInstanceOperCompEntity);
        when(technologyInstanceOperCompEntity.getId()).thenReturn(techInsOperCompId);
        when(technologyInstanceOperCompEntity.getDataDefinition()).thenReturn(techInsOperCompDD);
        when(techInsOperCompDD.get(techInsOperCompId)).thenReturn(technologyInstanceOperCompEntity);
        when(technologyInstanceOperCompEntity.getBelongsToField("technologyOperationComponent")).thenReturn(techOperCompEntity);

        when(techOperCompEntity.getField("pieceworkCost")).thenReturn(obj1);
        when(techOperCompEntity.getField("numberOfOperations")).thenReturn(obj2);
        when(techOperCompEntity.getField("laborHourlyCost")).thenReturn(obj3);
        when(techOperCompEntity.getField("machineHourlyCost")).thenReturn(obj4);
        // when
        costNormsForOperationService.copyCostValuesFromTechnology(view, state, null);

        Mockito.verify(field1).setFieldValue(obj1);
        Mockito.verify(field2).setFieldValue(obj2);
        Mockito.verify(field3).setFieldValue(obj3);
        Mockito.verify(field4).setFieldValue(obj4);
    }

    @Test
    public void shouldFillCurrencyFields() throws Exception {
        // given
        String currency = "PLN";
        when(currencyService.getCurrencyAlphabeticCode()).thenReturn(currency);
        when(view.getComponentByReference("pieceworkCostCURRENCY")).thenReturn(field1);
        when(view.getComponentByReference("laborHourlyCostCURRENCY")).thenReturn(field2);
        when(view.getComponentByReference("machineHourlyCostCURRENCY")).thenReturn(field3);

        // when
        costNormsForOperationService.fillCurrencyFields(view);
        // then

        Mockito.verify(field1).setFieldValue(currency);
        Mockito.verify(field2).setFieldValue(currency);
        Mockito.verify(field3).setFieldValue(currency);
    }

    @Test
    public void shouldCopyCostNormsToTechnologyInstanceOperationComponent() throws Exception {
        // given
        when(technologyInstanceOperCompEntity.getBelongsToField("technologyOperationComponent")).thenReturn(techOperCompEntity);
        // when
        costNormsForOperationService.copyCostNormsToTechnologyInstanceOperationComponent(techInsOperCompDD,
                technologyInstanceOperCompEntity);
        // then
    }
}
