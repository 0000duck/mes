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
package com.qcadoo.mes.basic;

import static com.qcadoo.mes.basic.constants.BasicConstants.MODEL_COMPANY;
import static com.qcadoo.mes.basic.constants.BasicConstants.PLUGIN_IDENTIFIER;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;

@Service
public class StaffService {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public void setOwnerCompany(final ViewDefinitionState view) {
        FieldComponent lookup = (FieldComponent) view.getComponentByReference("workFor");
        Entity ownerCompany = dataDefinitionService.get(PLUGIN_IDENTIFIER, MODEL_COMPANY).find()
                .add(SearchRestrictions.eq("owner", true)).uniqueResult();
        FormComponent form = (FormComponent) view.getComponentByReference("form");
        if (form.getEntityId() != null || lookup.getFieldValue() != null || ownerCompany == null) {
            return;
        }
        lookup.setFieldValue(ownerCompany.getId());
        lookup.requestComponentUpdateState();
    }
}
