/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.4.10
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
package com.qcadoo.mes.costCalculation.print.utils;

import java.io.Serializable;
import java.util.Comparator;

import com.qcadoo.model.api.Entity;

public class EntityOperationComponentComparator implements Comparator<Entity>, Serializable {

    private static final long serialVersionUID = 2360961924344935922L;

    @Override
    public final int compare(final Entity o1, final Entity o2) {
        int result = o1.getBelongsToField("operation").getStringField("number")
                .compareTo(o2.getBelongsToField("operation").getStringField("number"));
        if (result == 0) {
            result = o1.getBelongsToField("operation").getStringField("name")
                    .compareTo(o2.getBelongsToField("operation").getStringField("name"));
            if (result == 0) {
                return o1.getBelongsToField("operation").getId().toString()
                        .compareTo(o2.getBelongsToField("operation").getId().toString());
            } else {
                return result;
            }
        }
        return result;
    }

}
