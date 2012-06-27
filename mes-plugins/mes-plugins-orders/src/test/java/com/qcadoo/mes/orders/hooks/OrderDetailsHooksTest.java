package com.qcadoo.mes.orders.hooks;

import static com.qcadoo.mes.orders.constants.OrderFields.COMMENT_REASON_TYPE_CORRECTION_DATE_FROM;
import static com.qcadoo.mes.orders.constants.OrderFields.COMMENT_REASON_TYPE_CORRECTION_DATE_TO;
import static com.qcadoo.mes.orders.constants.OrderFields.CORRECTED_DATE_FROM;
import static com.qcadoo.mes.orders.constants.OrderFields.CORRECTED_DATE_TO;
import static com.qcadoo.mes.orders.constants.OrderFields.REASON_TYPE_CORRECTION_DATE_FROM;
import static com.qcadoo.mes.orders.constants.OrderFields.REASON_TYPE_CORRECTION_DATE_TO;
import static com.qcadoo.mes.orders.constants.OrderFields.STATE;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.qcadoo.mes.orders.constants.OrdersConstants;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;

public class OrderDetailsHooksTest {

    private OrderDetailsHooks hooks;

    @Mock
    private ViewDefinitionState view;

    @Mock
    private FormComponent form;

    @Mock
    private DataDefinitionService dataDefinitionService;

    @Mock
    private DataDefinition dataDefinition;

    @Mock
    private Entity order;

    @Mock
    FieldComponent correctDateFromField, correctDateToField, commentDateFromField, commentDateToField, reasonDateFromField,
            dateFrom, dateTo, reasonDateToField;

    @Before
    public void init() {
        hooks = new OrderDetailsHooks();
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(hooks, "dataDefinitionService", dataDefinitionService);

        Long id = 1L;
        when(view.getComponentByReference("form")).thenReturn(form);
        when(form.getEntityId()).thenReturn(id);
        when(dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER))
                .thenReturn(dataDefinition);
        when(dataDefinition.get(id)).thenReturn(order);
        when(view.getComponentByReference(CORRECTED_DATE_FROM)).thenReturn(correctDateFromField);
        when(view.getComponentByReference(CORRECTED_DATE_TO)).thenReturn(correctDateToField);
        when(view.getComponentByReference(REASON_TYPE_CORRECTION_DATE_FROM)).thenReturn(reasonDateFromField);
        when(view.getComponentByReference(REASON_TYPE_CORRECTION_DATE_TO)).thenReturn(reasonDateToField);
        when(view.getComponentByReference(COMMENT_REASON_TYPE_CORRECTION_DATE_TO)).thenReturn(commentDateToField);
        when(view.getComponentByReference(COMMENT_REASON_TYPE_CORRECTION_DATE_FROM)).thenReturn(commentDateFromField);
    }

    @Test
    public void shouldCheckEnabledFieldWhenOrderStateIsAccepted() throws Exception {
        // given
        when(order.getStringField(STATE)).thenReturn("02accepted");
        // when
        hooks.changedEnabledFieldForSpecificOrderState(view);
        // then
        Mockito.verify(correctDateFromField).setEnabled(true);
        Mockito.verify(correctDateToField).setEnabled(true);
        Mockito.verify(commentDateFromField).setEnabled(true);
        Mockito.verify(commentDateToField).setEnabled(true);
        Mockito.verify(reasonDateFromField).setEnabled(true);
        Mockito.verify(reasonDateToField).setEnabled(true);
    }

    @Test
    public void shouldCheckEnabledFieldWhenOrderStateIsInProgress() throws Exception {
        // given
        when(order.getStringField(STATE)).thenReturn("03inProgress");
        // when
        hooks.changedEnabledFieldForSpecificOrderState(view);
        // then
        Mockito.verify(correctDateToField).setEnabled(true);
        Mockito.verify(commentDateToField).setEnabled(true);
        Mockito.verify(reasonDateToField).setEnabled(true);
    }

}
