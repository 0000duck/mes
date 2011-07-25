/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.4.5
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
package com.qcadoo.mes.productionScheduling;

import org.springframework.stereotype.Service;

import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;

@Service
public class OperationService {

    public void changeCountRealizedOperation(final ViewDefinitionState viewDefinitionState, final ComponentState state,
            final String[] args) {
        FieldComponent countRealizedOperation = (FieldComponent) viewDefinitionState
                .getComponentByReference("countRealizedOperation");
        FieldComponent countMachineOperation = (FieldComponent) viewDefinitionState
                .getComponentByReference("countMachineOperation");

        if (countRealizedOperation.getFieldValue().equals("02specified")) {
            countMachineOperation.setVisible(true);
        } else {
            countMachineOperation.setVisible(false);
        }

    }

    public void updateCountMachineOperationFieldStateonWindowLoad(final ViewDefinitionState viewDefinitionState) {

        FieldComponent countRealizedOperation = (FieldComponent) viewDefinitionState
                .getComponentByReference("countRealizedOperation");
        FieldComponent countMachineOperation = (FieldComponent) viewDefinitionState
                .getComponentByReference("countMachineOperation");

        countRealizedOperation.setRequired(true);

        if (countRealizedOperation.getFieldValue().equals("02specified")) {
            countMachineOperation.setVisible(true);
            countMachineOperation.setEnabled(true);
        } else {
            countMachineOperation.setVisible(false);
        }

    }

    public void refereshGanttChart(final ViewDefinitionState viewDefinitionState, final ComponentState triggerState,
            final String[] args) {
        viewDefinitionState.getComponentByReference("gantt").performEvent(viewDefinitionState, "refresh");
    }

    public void disableFormWhenNoOrderSelected(final ViewDefinitionState viewDefinitionState) {
        if (viewDefinitionState.getComponentByReference("gantt").getFieldValue() == null) {
            viewDefinitionState.getComponentByReference("dateFrom").setEnabled(false);
            viewDefinitionState.getComponentByReference("dateTo").setEnabled(false);
        } else {
            viewDefinitionState.getComponentByReference("dateFrom").setEnabled(true);
            viewDefinitionState.getComponentByReference("dateTo").setEnabled(true);
        }
    }

    public void setCountRealizedOperationValue(final ViewDefinitionState viewDefinitionState) {
        FieldComponent countRealizedOperation = (FieldComponent) viewDefinitionState
                .getComponentByReference("countRealizedOperation");
        if (!"02specified".equals(countRealizedOperation.getFieldValue())) {
            countRealizedOperation.setFieldValue("01all");

        }
    }
}
