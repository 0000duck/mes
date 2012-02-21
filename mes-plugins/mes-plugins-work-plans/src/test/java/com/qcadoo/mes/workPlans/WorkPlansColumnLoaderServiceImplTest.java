package com.qcadoo.mes.workPlans;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.qcadoo.mes.basic.constants.BasicConstants;
import com.qcadoo.mes.productionScheduling.constants.ProductionSchedulingConstants;
import com.qcadoo.mes.technologies.constants.TechnologiesConstants;
import com.qcadoo.mes.workPlans.constants.WorkPlansConstants;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchCriteriaBuilder;
import com.qcadoo.model.api.search.SearchResult;

public class WorkPlansColumnLoaderServiceImplTest {

    @Mock
    Entity parameter;

    @Mock
    Entity operation;

    @Mock
    Entity technologyOperationComponent;

    @Mock
    Entity orderOperationComponent;

    @Mock
    List<Entity> operations;

    @Mock
    List<Entity> technologyOperationComponents;

    @Mock
    List<Entity> orderOperationComponents;

    @Mock
    DataDefinition parameterDD;

    @Mock
    DataDefinition operationDD;

    @Mock
    DataDefinition technologyOperationComponentDD;

    @Mock
    DataDefinition orderOperationComponentDD;

    @Mock
    DataDefinition columnForInputProductsDD;

    @Mock
    DataDefinition columnForOutputProductsDD;

    @Mock
    SearchCriteriaBuilder searchCriteria;

    @Mock
    SearchResult searchResult;

    @Mock
    private DataDefinitionService dataDefinitionService;

    private WorkPlansColumnLoaderServiceImpl workPlansColumnLoaderServiceImpl;

    @Before
    public final void init() {
        MockitoAnnotations.initMocks(this);

        workPlansColumnLoaderServiceImpl = new WorkPlansColumnLoaderServiceImpl();

        ReflectionTestUtils.setField(workPlansColumnLoaderServiceImpl, "dataDefinitionService", dataDefinitionService);

    }

    @Test
    public void shouldSetParameterDefaultValuesIfParameterIsntNull() {
        // given
        when(dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, BasicConstants.MODEL_PARAMETER)).thenReturn(parameterDD);
        when(parameterDD.find()).thenReturn(searchCriteria);
        when(searchCriteria.uniqueResult()).thenReturn(parameter);

        when(parameter.isValid()).thenReturn(true);

        when(parameter.getDataDefinition()).thenReturn(parameterDD);
        when(parameterDD.save(parameter)).thenReturn(parameter);

        // when
        workPlansColumnLoaderServiceImpl.setParameterDefaultValues();

        // then
        verify(parameter).setField(WorkPlansConstants.HIDE_DESCRIPTION_IN_WORK_PLANS_FIELD, false);
        verify(parameter).setField(WorkPlansConstants.HIDE_DETAILS_IN_WORK_PLANS_FIELD, false);
        verify(parameter).setField(WorkPlansConstants.HIDE_TECHNOLOGY_AND_ORDER_IN_WORK_PLANS_FIELD, false);
        verify(parameter).setField(WorkPlansConstants.DONT_PRINT_INPUT_PRODUCTS_IN_WORK_PLANS_FIELD, false);
        verify(parameter).setField(WorkPlansConstants.DONT_PRINT_OUTPUT_PRODUCTS_IN_WORK_PLANS_FIELD, false);
    }

    @Test
    public void shouldntSetParameterDefaultValuesIfParameterIsNull() {
        // given
        when(dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, BasicConstants.MODEL_PARAMETER)).thenReturn(parameterDD);
        when(parameterDD.find()).thenReturn(searchCriteria);
        when(searchCriteria.uniqueResult()).thenReturn(null);

        // when
        workPlansColumnLoaderServiceImpl.setParameterDefaultValues();

        // then
        verify(parameter, never()).setField(WorkPlansConstants.HIDE_DESCRIPTION_IN_WORK_PLANS_FIELD, false);
        verify(parameter, never()).setField(WorkPlansConstants.HIDE_DETAILS_IN_WORK_PLANS_FIELD, false);
        verify(parameter, never()).setField(WorkPlansConstants.HIDE_TECHNOLOGY_AND_ORDER_IN_WORK_PLANS_FIELD, false);
        verify(parameter, never()).setField(WorkPlansConstants.DONT_PRINT_INPUT_PRODUCTS_IN_WORK_PLANS_FIELD, false);
        verify(parameter, never()).setField(WorkPlansConstants.DONT_PRINT_OUTPUT_PRODUCTS_IN_WORK_PLANS_FIELD, false);
    }

    @Test
    public void shouldSetOperationDefaultValuesIfOperationsIsntNull() {
        // given
        when(dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_OPERATION))
                .thenReturn(operationDD);
        when(operationDD.find()).thenReturn(searchCriteria);
        when(searchCriteria.list()).thenReturn(searchResult);
        when(searchResult.getEntities()).thenReturn(operations);

        Entity operation1 = mock(Entity.class);
        Entity operation2 = mock(Entity.class);
        Entity operation3 = mock(Entity.class);

        DataDefinition operation1DD = mock(DataDefinition.class);
        DataDefinition operation2DD = mock(DataDefinition.class);
        DataDefinition operation3DD = mock(DataDefinition.class);

        @SuppressWarnings("unchecked")
        Iterator<Entity> operationsIterator = mock(Iterator.class);
        when(operationsIterator.hasNext()).thenReturn(true, true, true, false);
        when(operationsIterator.next()).thenReturn(operation1, operation2, operation3);

        when(operations.iterator()).thenReturn(operationsIterator);

        when(operation1.isValid()).thenReturn(true);
        when(operation2.isValid()).thenReturn(true);
        when(operation3.isValid()).thenReturn(true);

        when(operation1.getDataDefinition()).thenReturn(operation1DD);
        when(operation2.getDataDefinition()).thenReturn(operation2DD);
        when(operation3.getDataDefinition()).thenReturn(operation3DD);

        // when
        workPlansColumnLoaderServiceImpl.setOperationDefaultValues();

        // then
        for (Entity operation : Arrays.asList(operation1, operation2, operation3)) {
            verify(operation).setField(WorkPlansConstants.HIDE_DESCRIPTION_IN_WORK_PLANS_FIELD, false);
            verify(operation).setField(WorkPlansConstants.HIDE_DETAILS_IN_WORK_PLANS_FIELD, false);
            verify(operation).setField(WorkPlansConstants.HIDE_TECHNOLOGY_AND_ORDER_IN_WORK_PLANS_FIELD, false);
            verify(operation).setField(WorkPlansConstants.DONT_PRINT_INPUT_PRODUCTS_IN_WORK_PLANS_FIELD, false);
            verify(operation).setField(WorkPlansConstants.DONT_PRINT_OUTPUT_PRODUCTS_IN_WORK_PLANS_FIELD, false);
        }
    }

    @Test
    public void shouldntSetOperationDefaultValuesIfOperationsIsNull() {
        // given
        when(dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_OPERATION))
                .thenReturn(operationDD);
        when(operationDD.find()).thenReturn(searchCriteria);
        when(searchCriteria.list()).thenReturn(searchResult);
        when(searchResult.getEntities()).thenReturn(null);

        // when
        workPlansColumnLoaderServiceImpl.setOperationDefaultValues();

        // then
        verify(operation, never()).setField(WorkPlansConstants.HIDE_DESCRIPTION_IN_WORK_PLANS_FIELD, false);
        verify(operation, never()).setField(WorkPlansConstants.HIDE_DETAILS_IN_WORK_PLANS_FIELD, false);
        verify(operation, never()).setField(WorkPlansConstants.HIDE_TECHNOLOGY_AND_ORDER_IN_WORK_PLANS_FIELD, false);
        verify(operation, never()).setField(WorkPlansConstants.DONT_PRINT_INPUT_PRODUCTS_IN_WORK_PLANS_FIELD, false);
        verify(operation, never()).setField(WorkPlansConstants.DONT_PRINT_OUTPUT_PRODUCTS_IN_WORK_PLANS_FIELD, false);
    }

    @Test
    public void shouldSetTechnologyOperationComponentDefaultValuesIfTechnologyOperationComponentsIsntNull() {
        // given
        when(
                dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                        TechnologiesConstants.MODEL_TECHNOLOGY_OPERATION_COMPONENT)).thenReturn(technologyOperationComponentDD);
        when(technologyOperationComponentDD.find()).thenReturn(searchCriteria);
        when(searchCriteria.list()).thenReturn(searchResult);
        when(searchResult.getEntities()).thenReturn(technologyOperationComponents);

        Entity technologyOperationComponent1 = mock(Entity.class);
        Entity technologyOperationComponent2 = mock(Entity.class);
        Entity technologyOperationComponent3 = mock(Entity.class);

        DataDefinition technologyOperationComponent1DD = mock(DataDefinition.class);
        DataDefinition technologyOperationComponent2DD = mock(DataDefinition.class);
        DataDefinition technologyOperationComponent3DD = mock(DataDefinition.class);

        @SuppressWarnings("unchecked")
        Iterator<Entity> operationsIterator = mock(Iterator.class);
        when(operationsIterator.hasNext()).thenReturn(true, true, true, false);
        when(operationsIterator.next()).thenReturn(technologyOperationComponent1, technologyOperationComponent2,
                technologyOperationComponent3);

        when(technologyOperationComponents.iterator()).thenReturn(operationsIterator);

        when(technologyOperationComponent1.isValid()).thenReturn(true);
        when(technologyOperationComponent2.isValid()).thenReturn(true);
        when(technologyOperationComponent3.isValid()).thenReturn(true);

        when(technologyOperationComponent1.getDataDefinition()).thenReturn(technologyOperationComponent1DD);
        when(technologyOperationComponent2.getDataDefinition()).thenReturn(technologyOperationComponent2DD);
        when(technologyOperationComponent3.getDataDefinition()).thenReturn(technologyOperationComponent3DD);

        // when
        workPlansColumnLoaderServiceImpl.setTechnologyOperationComponentDefaultValues();

        // then
        for (Entity technologyOperationCoponent : Arrays.asList(technologyOperationComponent1, technologyOperationComponent2,
                technologyOperationComponent3)) {
            verify(technologyOperationCoponent).setField(WorkPlansConstants.HIDE_DESCRIPTION_IN_WORK_PLANS_FIELD, false);
            verify(technologyOperationCoponent).setField(WorkPlansConstants.HIDE_DETAILS_IN_WORK_PLANS_FIELD, false);
            verify(technologyOperationCoponent).setField(WorkPlansConstants.HIDE_TECHNOLOGY_AND_ORDER_IN_WORK_PLANS_FIELD, false);
            verify(technologyOperationCoponent).setField(WorkPlansConstants.DONT_PRINT_INPUT_PRODUCTS_IN_WORK_PLANS_FIELD, false);
            verify(technologyOperationCoponent)
                    .setField(WorkPlansConstants.DONT_PRINT_OUTPUT_PRODUCTS_IN_WORK_PLANS_FIELD, false);
        }
    }

    @Test
    public void shouldntSetTechnologyOperationComponentDefaultValuesIfTechnologyOperationComponentsIsNull() {
        // given
        when(
                dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                        TechnologiesConstants.MODEL_TECHNOLOGY_OPERATION_COMPONENT)).thenReturn(technologyOperationComponentDD);
        when(technologyOperationComponentDD.find()).thenReturn(searchCriteria);
        when(searchCriteria.list()).thenReturn(searchResult);
        when(searchResult.getEntities()).thenReturn(null);

        // when
        workPlansColumnLoaderServiceImpl.setTechnologyOperationComponentDefaultValues();

        // then

        verify(technologyOperationComponent, never()).setField(WorkPlansConstants.HIDE_DESCRIPTION_IN_WORK_PLANS_FIELD, false);
        verify(technologyOperationComponent, never()).setField(WorkPlansConstants.HIDE_DETAILS_IN_WORK_PLANS_FIELD, false);
        verify(technologyOperationComponent, never()).setField(WorkPlansConstants.HIDE_TECHNOLOGY_AND_ORDER_IN_WORK_PLANS_FIELD,
                false);
        verify(technologyOperationComponent, never()).setField(WorkPlansConstants.DONT_PRINT_INPUT_PRODUCTS_IN_WORK_PLANS_FIELD,
                false);
        verify(technologyOperationComponent, never()).setField(WorkPlansConstants.DONT_PRINT_OUTPUT_PRODUCTS_IN_WORK_PLANS_FIELD,
                false);
    }

    @Test
    public void shouldSetOrderOperationComponentsDefaultValuesIfOrderOperationComponentsIsntNull() {
        // given
        when(
                dataDefinitionService.get(ProductionSchedulingConstants.PLUGIN_IDENTIFIER,
                        ProductionSchedulingConstants.MODEL_ORDER_OPERATION_COMPONENT)).thenReturn(orderOperationComponentDD);
        when(orderOperationComponentDD.find()).thenReturn(searchCriteria);
        when(searchCriteria.list()).thenReturn(searchResult);
        when(searchResult.getEntities()).thenReturn(orderOperationComponents);

        Entity orderOperationComponent1 = mock(Entity.class);
        Entity orderOperationComponent2 = mock(Entity.class);
        Entity orderOperationComponent3 = mock(Entity.class);

        DataDefinition orderOperationComponent1DD = mock(DataDefinition.class);
        DataDefinition orderOperationComponent2DD = mock(DataDefinition.class);
        DataDefinition orderOperationComponent3DD = mock(DataDefinition.class);

        @SuppressWarnings("unchecked")
        Iterator<Entity> operationsIterator = mock(Iterator.class);
        when(operationsIterator.hasNext()).thenReturn(true, true, true, false);
        when(operationsIterator.next()).thenReturn(orderOperationComponent1, orderOperationComponent2, orderOperationComponent3);

        when(orderOperationComponents.iterator()).thenReturn(operationsIterator);

        when(orderOperationComponent1.isValid()).thenReturn(true);
        when(orderOperationComponent2.isValid()).thenReturn(true);
        when(orderOperationComponent3.isValid()).thenReturn(true);

        when(orderOperationComponent1.getDataDefinition()).thenReturn(orderOperationComponent1DD);
        when(orderOperationComponent2.getDataDefinition()).thenReturn(orderOperationComponent2DD);
        when(orderOperationComponent3.getDataDefinition()).thenReturn(orderOperationComponent3DD);

        // when
        workPlansColumnLoaderServiceImpl.setOrderOperationComponentDefaultValues();

        // then
        for (Entity orderOperationCoponent : Arrays.asList(orderOperationComponent1, orderOperationComponent2,
                orderOperationComponent3)) {
            verify(orderOperationCoponent).setField(WorkPlansConstants.HIDE_DESCRIPTION_IN_WORK_PLANS_FIELD, false);
            verify(orderOperationCoponent).setField(WorkPlansConstants.HIDE_DETAILS_IN_WORK_PLANS_FIELD, false);
            verify(orderOperationCoponent).setField(WorkPlansConstants.HIDE_TECHNOLOGY_AND_ORDER_IN_WORK_PLANS_FIELD, false);
            verify(orderOperationCoponent).setField(WorkPlansConstants.DONT_PRINT_INPUT_PRODUCTS_IN_WORK_PLANS_FIELD, false);
            verify(orderOperationCoponent).setField(WorkPlansConstants.DONT_PRINT_OUTPUT_PRODUCTS_IN_WORK_PLANS_FIELD, false);
        }
    }

    @Test
    public void shouldntSetOrderOperationComponentsDefaultValuesIfOrderOperationComponentsIsNull() {
        // given
        when(
                dataDefinitionService.get(ProductionSchedulingConstants.PLUGIN_IDENTIFIER,
                        ProductionSchedulingConstants.MODEL_ORDER_OPERATION_COMPONENT)).thenReturn(orderOperationComponentDD);
        when(orderOperationComponentDD.find()).thenReturn(searchCriteria);
        when(searchCriteria.list()).thenReturn(searchResult);
        when(searchResult.getEntities()).thenReturn(null);

        // when
        workPlansColumnLoaderServiceImpl.setOrderOperationComponentDefaultValues();

        // then
        verify(orderOperationComponent, never()).setField(WorkPlansConstants.HIDE_DESCRIPTION_IN_WORK_PLANS_FIELD, false);
        verify(orderOperationComponent, never()).setField(WorkPlansConstants.HIDE_DETAILS_IN_WORK_PLANS_FIELD, false);
        verify(orderOperationComponent, never())
                .setField(WorkPlansConstants.HIDE_TECHNOLOGY_AND_ORDER_IN_WORK_PLANS_FIELD, false);
        verify(orderOperationComponent, never())
                .setField(WorkPlansConstants.DONT_PRINT_INPUT_PRODUCTS_IN_WORK_PLANS_FIELD, false);
        verify(orderOperationComponent, never()).setField(WorkPlansConstants.DONT_PRINT_OUTPUT_PRODUCTS_IN_WORK_PLANS_FIELD,
                false);
    }

    @Ignore
    @Test
    public void shouldFillColumnsForProducts() {
        // given
        when(dataDefinitionService.get(WorkPlansConstants.PLUGIN_IDENTIFIER, WorkPlansConstants.MODEL_COLUMN_FOR_INPUT_PRODUCTS))
                .thenReturn(columnForInputProductsDD);

        when(dataDefinitionService.get(WorkPlansConstants.PLUGIN_IDENTIFIER, WorkPlansConstants.MODEL_COLUMN_FOR_OUTPUT_PRODUCTS))
                .thenReturn(columnForOutputProductsDD);

        // when
        workPlansColumnLoaderServiceImpl.fillColumnsForProducts("plugin");

        // then
    }

    @Ignore
    @Test
    public void shouldClearColumnsForProducts() {
        // given
        when(dataDefinitionService.get(WorkPlansConstants.PLUGIN_IDENTIFIER, WorkPlansConstants.MODEL_COLUMN_FOR_INPUT_PRODUCTS))
                .thenReturn(columnForInputProductsDD);

        when(dataDefinitionService.get(WorkPlansConstants.PLUGIN_IDENTIFIER, WorkPlansConstants.MODEL_COLUMN_FOR_OUTPUT_PRODUCTS))
                .thenReturn(columnForOutputProductsDD);

        // when
        workPlansColumnLoaderServiceImpl.clearColumnsForProducts("plugin");

        // then
    }
}
