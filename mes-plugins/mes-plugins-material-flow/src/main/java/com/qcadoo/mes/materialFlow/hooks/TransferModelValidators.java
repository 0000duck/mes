/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.7
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

import static com.qcadoo.mes.materialFlow.constants.TransferFields.LOCATION_FROM;
import static com.qcadoo.mes.materialFlow.constants.TransferFields.LOCATION_TO;
import static com.qcadoo.mes.materialFlow.constants.TransferFields.TIME;
import static com.qcadoo.mes.materialFlow.constants.TransferFields.TYPE;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;

@Service
public class TransferModelValidators {

    public boolean validateTransfer(final DataDefinition transferDD, final Entity transfer) {
        boolean validate = true;

        String type = transfer.getStringField(TYPE);
        Date time = (Date) transfer.getField(TIME);
        Entity locationFrom = transfer.getBelongsToField(LOCATION_FROM);
        Entity locationTo = transfer.getBelongsToField(LOCATION_TO);

        if (type == null) {
            transfer.addError(transferDD.getField(TYPE), "materialFlow.validate.global.error.fillType");

            validate = false;
        }

        if (time == null) {
            transfer.addError(transferDD.getField(TIME), "materialFlow.validate.global.error.fillDate");

            validate = false;
        }

        if (locationFrom == null && locationTo == null) {
            transfer.addError(transferDD.getField(LOCATION_FROM), "materialFlow.validate.global.error.fillAtLeastOneLocation");
            transfer.addError(transferDD.getField(LOCATION_TO), "materialFlow.validate.global.error.fillAtLeastOneLocation");

            validate = false;
        }

        return validate;
    }

}
