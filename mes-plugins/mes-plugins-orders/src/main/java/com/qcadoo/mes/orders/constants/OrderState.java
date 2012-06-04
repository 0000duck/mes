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
package com.qcadoo.mes.orders.constants;

public enum OrderState {

    PENDING("01pending"), ACCEPTED("02accepted"), IN_PROGRESS("03inProgress"), COMPLETED("04completed"), DECLINED("05declined"), INTERRUPTED(
            "06interrupted"), ABANDONED("07abandoned");

    private final String state;

    private OrderState(final String state) {
        this.state = state;
    }

    public String getStringValue() {
        return state;
    }

    public static OrderState parseString(final String string) {
        if ("01pending".equalsIgnoreCase(string)) {
            return PENDING;
        } else if ("02accepted".equalsIgnoreCase(string)) {
            return ACCEPTED;
        } else if ("03inProgress".equalsIgnoreCase(string)) {
            return IN_PROGRESS;
        } else if ("04completed".equalsIgnoreCase(string)) {
            return COMPLETED;
        } else if ("05declined".equalsIgnoreCase(string)) {
            return DECLINED;
        } else if ("06interrupted".equalsIgnoreCase(string)) {
            return INTERRUPTED;
        } else if ("07abandoned".equalsIgnoreCase(string)) {
            return ABANDONED;
        } else {
            throw new IllegalArgumentException("Couldn't parse OrderState from string '" + string + "'");
        }
    }

}
