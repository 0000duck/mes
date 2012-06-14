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
package com.qcadoo.mes.materialFlow.hooks;

import static com.qcadoo.mes.materialFlow.constants.TransferFields.STAFF;
import static com.qcadoo.mes.materialFlow.constants.TransferFields.STOCK_AREAS_FROM;
import static com.qcadoo.mes.materialFlow.constants.TransferFields.STOCK_AREAS_TO;
import static com.qcadoo.mes.materialFlow.constants.TransferFields.TIME;
import static com.qcadoo.mes.materialFlow.constants.TransferFields.TRANSFORMATIONS_CONSUMPTION;
import static com.qcadoo.mes.materialFlow.constants.TransferFields.TRANSFORMATIONS_PRODUCTION;
import static com.qcadoo.mes.materialFlow.constants.TransferFields.TYPE;
import static com.qcadoo.mes.materialFlow.constants.TransferType.CONSUMPTION;
import static com.qcadoo.mes.materialFlow.constants.TransferType.PRODUCTION;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.materialFlow.MaterialFlowResourceService;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;

@Service
public class TransferModelHooks {

    @Autowired
    private MaterialFlowResourceService matersialFlowResourceService;

    public void copyProductionOrConsumptionDataFromBelongingTransformation(final DataDefinition dd, final Entity transfer) {
        Entity transformation = transfer.getBelongsToField(TRANSFORMATIONS_PRODUCTION);

        if (transformation == null) {
            transformation = transfer.getBelongsToField(TRANSFORMATIONS_CONSUMPTION);

            if (transformation == null) {
                // came here from plain transfer detail view
                return;
            } else {
                transfer.setField(TYPE, CONSUMPTION.getStringValue());
                transfer.setField(STOCK_AREAS_FROM, transformation.getBelongsToField(STOCK_AREAS_FROM));
            }
        } else {
            transfer.setField(TYPE, PRODUCTION.getStringValue());
            transfer.setField(STOCK_AREAS_TO, transformation.getBelongsToField(STOCK_AREAS_TO));
        }

        transfer.setField(TIME, transformation.getField(TIME));
        transfer.setField(STAFF, transformation.getBelongsToField(STAFF));
    }

    public void manageResources(final DataDefinition transferDD, final Entity transfer) {
        matersialFlowResourceService.manageResources(transfer);
    }
}
