/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.2.0
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
package com.qcadoo.mes.assignmentToShift;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.plugin.api.Module;
import com.qcadoo.tenant.api.DefaultLocaleResolver;

@Component
public class OccupationTypeLoaderModule extends Module {

    private static final String L_QCADOO_MODEL = "qcadooModel";

    protected static final Logger LOG = LoggerFactory.getLogger(OccupationTypeLoaderModule.class);

    private static final String L_OCCUPATION_TYPE = "occupationType";

    private static final String L_DICTIONARY_ITEM = "dictionaryItem";

    private static final String L_DICTIONARY = "dictionary";

    private static final String L_TECHNICAL_CODE = "technicalCode";

    private static final String L_NAME = "name";

    @Autowired
    private DefaultLocaleResolver defaultLocaleResolver;

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Override
    @Transactional
    public final void multiTenantEnable() {
        if (databaseHasToBePrepared()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Occupation type table will be populated...");
            }
            readDataFromXML();
        }
    }

    private void readDataFromXML() {
        LOG.info("Loading test data from occupationType.xml ...");

        try {
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(getOccupationTypeXmlFile());
            Element rootNode = document.getRootElement();

            @SuppressWarnings("unchecked")
            List<Element> nodes = rootNode.getChildren("row");
            for (Element node : nodes) {
                parseAndAddDictionaries(node);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } catch (JDOMException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void parseAndAddDictionaries(final Element node) {
        @SuppressWarnings("unchecked")
        List<Attribute> attributes = node.getAttributes();
        Map<String, String> values = new HashMap<String, String>();
        for (Attribute attribute : attributes) {
            values.put(attribute.getName().toLowerCase(Locale.ENGLISH), attribute.getValue());
        }
        addDictionaryItem(values);
    }

    private void addDictionaryItem(final Map<String, String> values) {
        DataDefinition dictionaryItemDataDefinition = getDictionaryItemDataDefinition();
        Entity dictionaryItem = dictionaryItemDataDefinition.create();

        dictionaryItem.setField(L_TECHNICAL_CODE, values.get(L_TECHNICAL_CODE.toLowerCase(Locale.ENGLISH)));
        dictionaryItem.setField(L_NAME, values.get(L_NAME.toLowerCase(Locale.ENGLISH)));
        dictionaryItem.setField(L_DICTIONARY, getlDictionary());

        dictionaryItem = dictionaryItemDataDefinition.save(dictionaryItem);
        if (dictionaryItem.isValid() && LOG.isDebugEnabled()) {
            LOG.debug("Occupation type saved {currency=" + dictionaryItem.toString() + "}");
        } else {
            throw new IllegalStateException("Saved dictionaries entity have validation errors - " + values.get(L_NAME));
        }
    }

    private boolean databaseHasToBePrepared() {
        return getDictionaryItemDataDefinition()
                .find()
                .add(SearchRestrictions.or(SearchRestrictions.eq(L_TECHNICAL_CODE, "01workForLine"),
                        SearchRestrictions.eq(L_TECHNICAL_CODE, "04otherCase"))).list().getTotalNumberOfEntities() == 0;
    }

    private DataDefinition getDictionaryItemDataDefinition() {
        return dataDefinitionService.get(L_QCADOO_MODEL, L_DICTIONARY_ITEM);
    }

    private Entity getlDictionary() {
        return dataDefinitionService.get(L_QCADOO_MODEL, L_DICTIONARY).find()
                .add(SearchRestrictions.eq(L_NAME, L_OCCUPATION_TYPE)).uniqueResult();
    }

    private InputStream getOccupationTypeXmlFile() throws IOException {
        return OccupationTypeLoaderModule.class.getResourceAsStream("/assignmentToShift/model/data/occupationType" + "_"
                + defaultLocaleResolver.getDefaultLocale().getLanguage() + ".xml");
    }

}
