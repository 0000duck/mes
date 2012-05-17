/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.6
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
package com.qcadoo.mes.timeNormsForOperations.listeners;

import static com.qcadoo.mes.technologies.constants.TechnologyInstanceOperCompFields.TECHNOLOGY_OPERATION_COMPONENT;
import static com.qcadoo.mes.timeNormsForOperations.constants.TimeNormsConstants.FIELDS_TECHNOLOGY;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FormComponent;

@Service
public class TechnologyInstanceOperCompDetailsListenersTNFO {

    @Autowired
    private TechnologyOperCompDetailsListenersTNFO technologyOperCompDetailsListenersTNFO;

    public void copyTimeNormsFromTechnology(final ViewDefinitionState view, final ComponentState componentState,
            final String[] args) {
        Entity technologyInstanceOperationComponent = ((FormComponent) view.getComponentByReference("form")).getEntity();

        // be sure that entity isn't in detached state
        technologyInstanceOperationComponent = technologyInstanceOperationComponent.getDataDefinition().get(
                technologyInstanceOperationComponent.getId());

        technologyOperCompDetailsListenersTNFO.applyTimeNormsFromGivenSource(view,
                technologyInstanceOperationComponent.getBelongsToField(TECHNOLOGY_OPERATION_COMPONENT), FIELDS_TECHNOLOGY);
    }
}
