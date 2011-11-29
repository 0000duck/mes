/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.0
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qcadoo.mes.basic.constants.BasicConstants;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchResult;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FormComponent;

@Service
public class CompanyService {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Transactional
    public Long getParameterId() {

        DataDefinition dataDefinition = dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, BasicConstants.MODEL_COMPANY);
        SearchResult searchResult = dataDefinition.find().setMaxResults(1).list();

        if (searchResult.getEntities().size() > 0) {
            return searchResult.getEntities().get(0).getId();
        } else {
            Entity newCompany = dataDefinition.create();
            Entity savedCompany = dataDefinition.save(newCompany);
            return savedCompany.getId();
        }

    }

    public void disableCompanyFormForOwner(final ViewDefinitionState state) {
        FormComponent form = (FormComponent) state.getComponentByReference("form");

        if (form.getEntityId() == null) {
            return;
        }

        Entity entity = dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, BasicConstants.MODEL_COMPANY).get(
                form.getEntityId());

        if (entity == null) {
            return;
        }

        Object owner = entity.getField("owner");

        if (owner != null && owner.equals(true)) {
            form.setFormEnabled(false);
        }
    }
}
