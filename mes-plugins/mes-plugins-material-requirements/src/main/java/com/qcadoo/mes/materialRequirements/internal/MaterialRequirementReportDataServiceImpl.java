/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.4.9
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
package com.qcadoo.mes.materialRequirements.internal;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.materialRequirements.api.MaterialRequirementReportDataService;
import com.qcadoo.mes.technologies.print.ReportDataService;
import com.qcadoo.model.api.Entity;

@Service
public class MaterialRequirementReportDataServiceImpl implements MaterialRequirementReportDataService {

    @Autowired
    private ReportDataService reportDataService;

    @Override
    public final Map<Entity, BigDecimal> getQuantitiesForOrdersTechnologyProducts(final List<Entity> orders,
            final Boolean onlyComponents) {
        Map<Entity, BigDecimal> products = new HashMap<Entity, BigDecimal>();
        for (Entity order : orders) {
            Entity technology = (Entity) order.getField("technology");
            BigDecimal plannedQuantity = (BigDecimal) order.getField("plannedQuantity");
            if (technology != null && plannedQuantity != null && plannedQuantity.compareTo(BigDecimal.ZERO) > 0) {
                reportDataService.countQuantityForProductsIn(products, technology, plannedQuantity, onlyComponents);
            }
        }
        return products;
    }

}
