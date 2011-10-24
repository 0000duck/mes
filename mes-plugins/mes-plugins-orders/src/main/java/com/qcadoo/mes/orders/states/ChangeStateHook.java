package com.qcadoo.mes.orders.states;

import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.context.i18n.LocaleContextHolder.getLocale;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.mes.orders.constants.OrderStates;
import com.qcadoo.mes.orders.constants.OrdersConstants;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ComponentState.MessageType;

@Service
public class ChangeStateHook {

    @Autowired
    private OrderStatesChangingService orderStatesChangingService;

    @Autowired
    private OrderStateValidationService orderStateValidationService;

    @Autowired
    DataDefinitionService dataDefinitionService;

    @Autowired
    private TranslationService translationService;

    public void changedState(final DataDefinition dataDefinition, final Entity newEntity) {
        checkArgument(newEntity != null, "entity is null");
        if (newEntity.getId() == null) {
            return;
        }
        Entity oldEntity = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(
                newEntity.getId());

        if (oldEntity == null) {
            return;
        }
        if (oldEntity.getStringField("state").equals(newEntity.getStringField("state"))) {
            String state = oldEntity.getStringField("state");
            List<ChangeOrderStateMessage> errors = null;
            if (state.equals(OrderStates.ACCEPTED.getStringValue())) {
                errors = orderStateValidationService.validationAccepted(newEntity);
            } else if (state.equals(OrderStates.IN_PROGRESS.getStringValue())
                    || state.equals(OrderStates.INTERRUPTED.getStringValue())) {
                errors = orderStateValidationService.validationInProgress(newEntity);
            } else if (state.equals(OrderStates.COMPLETED.getStringValue())) {
                errors = orderStateValidationService.validationCompleted(newEntity);
            }
            if (errors != null && errors.size() > 0) {
                for (ChangeOrderStateMessage error : errors) {
                    if (error.getReferenceToField() != null) {
                        newEntity
                                .addError(dataDefinition.getField(error.getReferenceToField()), translationService.translate(
                                        error.getMessage() + "." + error.getReferenceToField(), getLocale()));
                    } else {
                        newEntity.addGlobalError(translationService.translate(error.getMessage(), getLocale()));
                    }
                }
            }
            return;
        }
        List<ChangeOrderStateMessage> errors = orderStatesChangingService.performChangeState(newEntity, oldEntity);
        if (errors != null && errors.size() > 0) {
            if (errors.size() == 1 && errors.get(0).getType().equals(MessageType.INFO)) {
                return;
            }
            newEntity.setField("state", oldEntity.getStringField("state"));
            for (ChangeOrderStateMessage error : errors) {
                if (error.getReferenceToField() != null) {
                    newEntity.addError(dataDefinition.getField(error.getReferenceToField()),
                            translationService.translate(error.getMessage() + "." + error.getReferenceToField(), getLocale()));
                } else {
                    newEntity.addGlobalError(translationService.translate(error.getMessage(), getLocale()));
                }
            }
            return;
        }

        orderStateValidationService.saveLogging(newEntity, oldEntity.getStringField("state"), newEntity.getStringField("state"));
    }
}
