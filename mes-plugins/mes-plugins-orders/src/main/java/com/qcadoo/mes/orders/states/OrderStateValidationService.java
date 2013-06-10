/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.2.0
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
package com.qcadoo.mes.orders.states;

import static com.google.common.base.Preconditions.checkArgument;
import static com.qcadoo.mes.orders.constants.OrderFields.DATE_FROM;
import static com.qcadoo.mes.orders.constants.OrderFields.DATE_TO;
import static com.qcadoo.mes.orders.constants.OrderFields.DONE_QUANTITY;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.states.StateChangeContext;
import com.qcadoo.mes.states.service.client.StateChangeSamplesClient;
import com.qcadoo.mes.technologies.validators.TechnologyTreeValidators;
import com.qcadoo.model.api.Entity;

@Service
public class OrderStateValidationService {

    @Autowired
    private CopyOfTechnologyValidationService copyOfTechnologyValidationService;

    @Autowired
    private TechnologyTreeValidators technologyTreeValidators;

    @Autowired
    private StateChangeSamplesClient stateChangeSamplesClient;

    private static final String ENTITY_IS_NULL = "entity is null";

    public void validationOnAccepted(final StateChangeContext stateChangeContext) {
        final List<String> references = Arrays.asList(DATE_TO, DATE_FROM);
        checkRequired(references, stateChangeContext);

        validateTechnologyState(stateChangeContext);
    }

    public void validationOnInProgress(final StateChangeContext stateChangeContext) {
        final List<String> references = Arrays.asList(DATE_TO, DATE_FROM);
        checkRequired(references, stateChangeContext);

        validateTechnologyState(stateChangeContext);
    }

    public void validationOnCompleted(final StateChangeContext stateChangeContext) {
        final List<String> fieldNames = Arrays.asList(DATE_TO, DATE_FROM, DONE_QUANTITY);
        checkRequired(fieldNames, stateChangeContext);
    }

    private void checkRequired(final List<String> fieldNames, final StateChangeContext stateChangeContext) {
        checkArgument(stateChangeContext != null, ENTITY_IS_NULL);
        final Entity stateChangeEntity = stateChangeContext.getOwner();
        for (String fieldName : fieldNames) {
            if (stateChangeEntity.getField(fieldName) == null) {
                stateChangeContext.addFieldValidationError(fieldName, "orders.order.orderStates.fieldRequired");
            }
        }
    }

    private void validateTechnologyState(final StateChangeContext stateChangeContext) {
        checkArgument(stateChangeContext != null, ENTITY_IS_NULL);

        final Entity order = stateChangeContext.getOwner();
        final Entity technology = order.getBelongsToField(OrderFields.COPY_OF_TECHNOLOGY);
        if (technology == null) {
            return;
        }

        copyOfTechnologyValidationService.checkConsumingManyProductsFromOneSubOp(stateChangeContext, technology);
        technologyTreeValidators.checkConsumingTheSameProductFromManySubOperations(technology.getDataDefinition(), technology);
        copyOfTechnologyValidationService.checkIfTechnologyHasAtLeastOneComponent(stateChangeContext, technology);
        copyOfTechnologyValidationService.checkTopComponentsProducesProductForTechnology(stateChangeContext, technology);
        copyOfTechnologyValidationService.checkIfOperationsUsesSubOperationsProds(stateChangeContext, technology);

    }

}
