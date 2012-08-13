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
package com.qcadoo.mes.materialFlowResources;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.qcadoo.model.api.Entity;

public interface MaterialFlowResourcesService {

    boolean areResourcesSufficient(final Entity location, final Entity product, final BigDecimal quantity);

    void manageResources(final Entity transfer);

    List<Entity> getResourcesForLocationAndProduct(final Entity location, final Entity product);

    Map<Entity, BigDecimal> groupResourcesByProduct(final Entity location);

    void addResource(final Entity locationTo, final Entity product, final BigDecimal quantity, final Date time,
            final BigDecimal price, final String batch);

    void addResource(final Entity locationTo, final Entity product, final BigDecimal quantity, final Date time,
            final BigDecimal price);

    void updateResource(final Entity locationFrom, final Entity product, BigDecimal quantity);

    void moveResource(final Entity locationFrom, final Entity locationTo, final Entity product, BigDecimal quantity,
            final Date time, final BigDecimal price);

    BigDecimal calculatePrice(final Entity location, final Entity product);

}
