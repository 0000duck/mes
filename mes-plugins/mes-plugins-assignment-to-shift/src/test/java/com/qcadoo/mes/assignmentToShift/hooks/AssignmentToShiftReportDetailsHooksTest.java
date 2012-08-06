package com.qcadoo.mes.assignmentToShift.hooks;

import static com.qcadoo.mes.assignmentToShift.constants.AssignmentToShiftReportFields.DATE_FROM;
import static com.qcadoo.mes.assignmentToShift.constants.AssignmentToShiftReportFields.DATE_TO;
import static com.qcadoo.mes.assignmentToShift.constants.AssignmentToShiftReportFields.GENERATED;
import static com.qcadoo.mes.assignmentToShift.constants.AssignmentToShiftReportFields.NAME;
import static com.qcadoo.mes.assignmentToShift.constants.AssignmentToShiftReportFields.NUMBER;
import static com.qcadoo.mes.assignmentToShift.constants.AssignmentToShiftReportFields.SHIFT;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.qcadoo.mes.assignmentToShift.constants.AssignmentToShiftConstants;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.utils.NumberGeneratorService;

public class AssignmentToShiftReportDetailsHooksTest {

    private static final String L_FORM = "form";

    private AssignmentToShiftReportDetailsHooks hooks;

    @Mock
    private NumberGeneratorService numberGeneratorService;

    @Mock
    private DataDefinitionService dataDefinitionService;

    @Mock
    private ViewDefinitionState view;

    @Mock
    private FormComponent assignmentToShiftReportForm;

    @Mock
    private FieldComponent fieldComponent;

    @Mock
    private DataDefinition assignmentToShiftReportDD;

    @Mock
    private Entity assignmentToShiftReport;

    @Before
    public void init() {
        hooks = new AssignmentToShiftReportDetailsHooks();

        MockitoAnnotations.initMocks(this);

        ReflectionTestUtils.setField(hooks, "numberGeneratorService", numberGeneratorService);
        ReflectionTestUtils.setField(hooks, "dataDefinitionService", dataDefinitionService);
    }

    @Test
    public void shouldGenerateAssignmentToShiftReportNumber() {
        // given

        // when
        hooks.generateAssignmentToShiftReportNumber(view);

        // then
        verify(numberGeneratorService).generateAndInsertNumber(view, AssignmentToShiftConstants.PLUGIN_IDENTIFIER,
                AssignmentToShiftConstants.MODEL_ASSIGNMENT_TO_SHIFT_REPORT, L_FORM, NUMBER);
    }

    @Test
    public void shouldntDisableFieldsWhenEntityIsntSaved() {
        // given
        given(view.getComponentByReference(L_FORM)).willReturn(assignmentToShiftReportForm);

        given(view.getComponentByReference(NUMBER)).willReturn(fieldComponent);
        given(view.getComponentByReference(NAME)).willReturn(fieldComponent);
        given(view.getComponentByReference(SHIFT)).willReturn(fieldComponent);
        given(view.getComponentByReference(DATE_FROM)).willReturn(fieldComponent);
        given(view.getComponentByReference(DATE_TO)).willReturn(fieldComponent);

        given(assignmentToShiftReportForm.getEntityId()).willReturn(null);

        // when
        hooks.disableFields(view);

        // then
        verify(fieldComponent, times(5)).setEnabled(true);
    }

    @Test
    public void shouldntDisableFieldsWhenEntityIsSavedAndReportIsntGenerated() {
        // given
        given(view.getComponentByReference(L_FORM)).willReturn(assignmentToShiftReportForm);

        given(view.getComponentByReference(NUMBER)).willReturn(fieldComponent);
        given(view.getComponentByReference(NAME)).willReturn(fieldComponent);
        given(view.getComponentByReference(SHIFT)).willReturn(fieldComponent);
        given(view.getComponentByReference(DATE_FROM)).willReturn(fieldComponent);
        given(view.getComponentByReference(DATE_TO)).willReturn(fieldComponent);

        given(assignmentToShiftReportForm.getEntityId()).willReturn(1L);

        given(
                dataDefinitionService.get(AssignmentToShiftConstants.PLUGIN_IDENTIFIER,
                        AssignmentToShiftConstants.MODEL_ASSIGNMENT_TO_SHIFT_REPORT)).willReturn(assignmentToShiftReportDD);
        given(assignmentToShiftReportDD.get(1L)).willReturn(assignmentToShiftReport);

        given(assignmentToShiftReport.getBooleanField(GENERATED)).willReturn(false);

        // when
        hooks.disableFields(view);

        // then
        verify(fieldComponent, times(5)).setEnabled(true);
    }

    @Test
    public void shouldDisableFieldsWhenEntityIsSavedAndReportIsGenerated() {
        // given
        given(view.getComponentByReference(L_FORM)).willReturn(assignmentToShiftReportForm);

        given(view.getComponentByReference(NUMBER)).willReturn(fieldComponent);
        given(view.getComponentByReference(NAME)).willReturn(fieldComponent);
        given(view.getComponentByReference(SHIFT)).willReturn(fieldComponent);
        given(view.getComponentByReference(DATE_FROM)).willReturn(fieldComponent);
        given(view.getComponentByReference(DATE_TO)).willReturn(fieldComponent);

        given(assignmentToShiftReportForm.getEntityId()).willReturn(1L);

        given(
                dataDefinitionService.get(AssignmentToShiftConstants.PLUGIN_IDENTIFIER,
                        AssignmentToShiftConstants.MODEL_ASSIGNMENT_TO_SHIFT_REPORT)).willReturn(assignmentToShiftReportDD);
        given(assignmentToShiftReportDD.get(1L)).willReturn(assignmentToShiftReport);

        given(assignmentToShiftReport.getBooleanField(GENERATED)).willReturn(true);

        // when
        hooks.disableFields(view);

        // then
        verify(fieldComponent, times(5)).setEnabled(false);
    }

}
