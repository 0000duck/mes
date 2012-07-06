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
package com.qcadoo.mes.samples.loader;

import static com.qcadoo.mes.samples.constants.SamplesConstants.L_DEFAULT_PRODUCTION_LINE;
import static com.qcadoo.mes.samples.constants.SamplesConstants.L_NAME;
import static com.qcadoo.mes.samples.constants.SamplesConstants.L_NUMBER;
import static com.qcadoo.mes.samples.constants.SamplesConstants.L_PRODUCTION_LINES;
import static com.qcadoo.mes.samples.constants.SamplesConstants.L_PRODUCTION_LINES_DICTIONARY;
import static com.qcadoo.mes.samples.constants.SamplesConstants.PRODUCTION_LINES_MODEL_PRODUCTION_LINE;
import static com.qcadoo.mes.samples.constants.SamplesConstants.PRODUCTION_LINES_PLUGIN_IDENTIFIER;

import java.util.Map;

import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.qcadoo.mes.basic.ParameterService;
import com.qcadoo.mes.samples.constants.SamplesConstants;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.security.api.SecurityRole;
import com.qcadoo.security.api.SecurityRolesService;

@Component
@Transactional
public class MinimalSamplesLoader extends AbstractXMLSamplesLoader {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private SecurityRolesService securityRolesService;

    @Autowired
    private ParameterService parameterService;

    @Override
    protected void loadData(final String locale) {
        final String dataset = "minimal";
        readDataFromXML(dataset, "dictionaries", locale);
        readDataFromXML(dataset, "defaultParameters", locale);
        readDataFromXML(dataset, "shifts", locale);
        readDataFromXML(dataset, "company", locale);

        if (isEnabledOrEnabling(PRODUCTION_LINES_PLUGIN_IDENTIFIER)) {
            readDataFromXML(dataset, L_PRODUCTION_LINES, locale);
            readDataFromXML(dataset, L_DEFAULT_PRODUCTION_LINE, locale);
        }
    }

    protected void readData(final Map<String, String> values, final String type, final Element node) {
        if ("dictionaries".equals(type)) {
            addDictionaryItems(values);
        } else if ("defaultParameters".equals(type)) {
            addParameters(values);
        } else if ("shifts".equals(type)) {
            addShifts(values);
        } else if ("company".equals(type)) {
            addCompany(values);
        } else if (L_PRODUCTION_LINES.equals(type)) {
            addProductionLines(values);
        } else if (L_PRODUCTION_LINES_DICTIONARY.equals(type)) {
            addDictionaryItems(values);
        } else if (L_DEFAULT_PRODUCTION_LINE.equals(type)) {
            addDefaultProductionLine(values);
        }
    }

    protected void addUser(final Map<String, String> values) {
        Entity user = dataDefinitionService.get("qcadooSecurity", "user").create();
        user.setField("userName", values.get("login"));
        user.setField(EMAIL, values.get(EMAIL));
        user.setField("firstName", values.get("firstname"));
        user.setField("lastName", values.get("lastname"));
        user.setField("password", "123");
        user.setField("passwordConfirmation", "123");
        user.setField("enabled", true);

        SecurityRole role = securityRolesService.getRoleByIdentifier(values.get("role"));
        user.setField("role", role.getName());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Add test user {login=" + user.getField("userName") + ", email=" + user.getField(EMAIL) + ", firstName="
                    + user.getField("firstName") + ", lastName=" + user.getField("lastName") + ", role=" + user.getField("role")
                    + "}");
        }

        user.getDataDefinition().save(user);
    }

    protected void addShifts(final Map<String, String> values) {
        Entity shift = dataDefinitionService.get(SamplesConstants.BASIC_PLUGIN_IDENTIFIER, SamplesConstants.BASIC_MODEL_SHIFT)
                .create();

        shift.setField(NAME, values.get(NAME));
        shift.setField("mondayWorking", values.get("mondayworking"));
        shift.setField("mondayHours", values.get("mondayhours"));
        shift.setField("tuesdayWorking", values.get("tuesdayworking"));
        shift.setField("tuesdayHours", values.get("tuesdayhours"));
        shift.setField("wensdayWorking", values.get("wensdayworking"));
        shift.setField("wensdayHours", values.get("wensdayhours"));
        shift.setField("thursdayWorking", values.get("thursdayworking"));
        shift.setField("thursdayHours", values.get("thursdayhours"));
        shift.setField("fridayWorking", values.get("fridayworking"));
        shift.setField("fridayHours", values.get("fridayhours"));
        shift.setField("saturdayWorking", values.get("saturdayworking"));
        shift.setField("saturdayHours", values.get("saturdayhours"));
        shift.setField("sundayWorking", values.get("sundayworking"));
        shift.setField("sundayHours", values.get("sundayhours"));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Add test shift item {shift=" + shift.getField(NAME) + "}");
        }

        shift.getDataDefinition().save(shift);
    }

    protected void addProductionLines(final Map<String, String> values) {
        Entity productionLine = dataDefinitionService.get(PRODUCTION_LINES_PLUGIN_IDENTIFIER,
                PRODUCTION_LINES_MODEL_PRODUCTION_LINE).create();
        productionLine.setField(L_NAME, values.get(L_NAME));
        productionLine.setField(L_NUMBER, values.get(L_NUMBER));
        productionLine.setField("supportsAllTechnologies", values.get("supportsalltechnologies"));
        productionLine.setField("supportsOtherTechnologiesWorkstationTypes",
                values.get("supportsothertechnologiesworkstationtypes"));
        productionLine.setField("quantityForOtherWorkstationTypes", values.get("quantityforotherworkstationtypes"));

        productionLine.getDataDefinition().save(productionLine);
    }

    protected void addDefaultProductionLine(final Map<String, String> values) {
        Entity parameter = parameterService.getParameter();
        parameter.setField(L_DEFAULT_PRODUCTION_LINE, getProductionLineByNumber(values.get("production_line_nr")));
        parameter.getDataDefinition().save(parameter);
    }

    protected Entity getProductionLineByNumber(final String number) {
        return dataDefinitionService.get(PRODUCTION_LINES_PLUGIN_IDENTIFIER, PRODUCTION_LINES_MODEL_PRODUCTION_LINE).find()
                .add(SearchRestrictions.eq(L_NUMBER, number)).setMaxResults(1).uniqueResult();
    }

}
