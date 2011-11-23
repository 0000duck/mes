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
package com.qcadoo.mes.orders.states;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.mes.basic.ShiftsServiceImpl;
import com.qcadoo.mes.orders.constants.OrdersConstants;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.security.api.SecurityService;

@Service
public class OrderStateValidationService {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ShiftsServiceImpl shiftsServiceImpl;

    @Autowired
    TranslationService translationService;

    public void saveLogging(final Entity order, final String previousState, final String currentState) {
        Entity logging = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_LOGGING).create();

        logging.setField("order", order);
        logging.setField("previousState", previousState);
        logging.setField("currentState", currentState);
        Date dateTime = new Date();
        Entity shift = shiftsServiceImpl.getShiftFromDate(dateTime);
        if (shift != null) {
            logging.setField("shift", shift);
        } else {
            logging.setField("shift", null);
        }
        logging.setField("worker", securityService.getCurrentUserName());
        logging.setField("dateAndTime", dateTime);

        logging.getDataDefinition().save(logging);
    }

    public List<ChangeOrderStateMessage> validationAccepted(final Entity entity) {
        checkArgument(entity != null, "entity is null");
        List<String> references = Arrays.asList("dateTo", "dateFrom", "technology");
        return checkValidation(references, entity);
    }

    public List<ChangeOrderStateMessage> validationInProgress(final Entity entity) {
        checkArgument(entity != null, "entity is null");
        return validationAccepted(entity);
    }

    public List<ChangeOrderStateMessage> validationCompleted(final Entity entity) {
        checkArgument(entity != null, "entity is null");
        List<String> references = Arrays.asList("dateTo", "dateFrom", "technology", "doneQuantity");
        return checkValidation(references, entity);
    }

    private List<ChangeOrderStateMessage> checkValidation(final List<String> references, final Entity entity) {
        checkArgument(entity != null, "entity is null");
        List<ChangeOrderStateMessage> errors = new ArrayList<ChangeOrderStateMessage>();
        for (String reference : references) {
            if (entity.getField(reference) == null) {
                errors.add(ChangeOrderStateMessage.errorForComponent(
                        translationService.translate("orders.order.orderStates.fieldRequired", LocaleContextHolder.getLocale()),
                        reference));
            }
        }
        return errors;
    }
}
