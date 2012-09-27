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
package com.qcadoo.mes.productionPerShift.hooks;

import java.util.Date;

import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.productionPerShift.constants.ProgressForDayFields;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;

@Service
public class ProgressForDayHooks {

    public void saveDateOfDay(final DataDefinition dataDefinition, final Entity entity) {
        Entity order = entity.getBelongsToField(ProgressForDayFields.TECH_INST_OPER_COMP).getBelongsToField("order");
        Integer day = (Integer) entity.getField(ProgressForDayFields.DAY);
        DateTime orderStartDate = new DateTime();
        if (entity.getBooleanField(ProgressForDayFields.CORRECTED)) {
            orderStartDate = new DateTime((Date) order.getField(OrderFields.CORRECTED_DATE_FROM));
        } else {
            orderStartDate = new DateTime((Date) order.getField(OrderFields.DATE_FROM));
        }
        Date dayOfDay = orderStartDate.plusDays(day - 1).toDate();
        entity.setField(ProgressForDayFields.DATE_OF_DAY, dayOfDay);
    }
}
