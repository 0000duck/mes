package com.qcadoo.mes.assignmentToShift.hooks;

import static com.qcadoo.mes.assignmentToShift.constants.OccupationTypeEnumStringValue.OTHER_CASE;
import static com.qcadoo.mes.assignmentToShift.constants.OccupationTypeEnumStringValue.WORK_ON_LINE;
import static com.qcadoo.mes.assignmentToShift.constants.StaffAssignmentToShiftFields.OCCUPATION_TYPE;
import static com.qcadoo.mes.assignmentToShift.constants.StaffAssignmentToShiftFields.OCCUPATION_TYPE_NAME;
import static com.qcadoo.mes.assignmentToShift.constants.StaffAssignmentToShiftFields.PRODUCTION_LINE;
import static com.qcadoo.model.constants.DictionaryItemFields.NAME;
import static com.qcadoo.model.constants.DictionaryItemFields.TECHNICAL_CODE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;

@Service
public class StaffAssignmentToShiftDetailsHooks {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public void setFieldsEnabledWhenTypeIsSpecific(final ViewDefinitionState view) {
        FieldComponent occupationType = (FieldComponent) view.getComponentByReference(OCCUPATION_TYPE);

        Entity dictionaryItem = findDictionaryItemByName(occupationType.getFieldValue().toString());

        if (dictionaryItem == null) {
            setFieldsEnabled(view, false, false);
        } else {
            String occupationTypeTechnicalCode = dictionaryItem.getStringField(TECHNICAL_CODE);

            if (occupationTypeTechnicalCode != null && WORK_ON_LINE.getStringValue().equals(occupationTypeTechnicalCode)) {
                setFieldsEnabled(view, true, false);
            } else if (occupationTypeTechnicalCode != null && OTHER_CASE.getStringValue().equals(occupationTypeTechnicalCode)) {
                setFieldsEnabled(view, false, true);
            } else {
                setFieldsEnabled(view, false, false);
            }
        }
    }

    private void setFieldsEnabled(final ViewDefinitionState view, final boolean enabledOrRequiredProductionLine,
            final boolean enabledOrRequiredOccupationTypeName) {
        FieldComponent productionLine = (FieldComponent) view.getComponentByReference(PRODUCTION_LINE);
        FieldComponent occupationTypeName = (FieldComponent) view.getComponentByReference(OCCUPATION_TYPE_NAME);

        productionLine.setEnabled(enabledOrRequiredProductionLine);
        productionLine.setRequired(enabledOrRequiredProductionLine);
        productionLine.requestComponentUpdateState();
        occupationTypeName.setEnabled(enabledOrRequiredOccupationTypeName);
        occupationTypeName.setRequired(enabledOrRequiredOccupationTypeName);
        occupationTypeName.requestComponentUpdateState();
    }

    public void setOccupationTypeToDefault(final ViewDefinitionState view) {
        FormComponent staffAssignmentToShiftForm = (FormComponent) view.getComponentByReference("form");
        FieldComponent occupationType = (FieldComponent) view.getComponentByReference(OCCUPATION_TYPE);

        if ((staffAssignmentToShiftForm.getEntityId() == null) && (occupationType.getFieldValue() == null)) {
            Entity dictionaryItem = findDictionaryItemByTechnicalCode(WORK_ON_LINE.getStringValue());

            if (dictionaryItem != null) {
                String occupationTypeName = dictionaryItem.getStringField(NAME);

                occupationType.setFieldValue(occupationTypeName);
                occupationType.requestComponentUpdateState();
            }
        }
    }

    protected Entity findDictionaryItemByName(final String name) {
        return dataDefinitionService.get("qcadooModel", "dictionaryItem").find().add(SearchRestrictions.eq(NAME, name))
                .uniqueResult();
    }

    protected Entity findDictionaryItemByTechnicalCode(final String technicalCode) {
        return dataDefinitionService.get("qcadooModel", "dictionaryItem").find()
                .add(SearchRestrictions.eq(TECHNICAL_CODE, technicalCode)).uniqueResult();
    }

}
