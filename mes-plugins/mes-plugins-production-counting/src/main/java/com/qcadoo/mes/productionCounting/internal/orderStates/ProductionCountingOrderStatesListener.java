package com.qcadoo.mes.productionCounting.internal.orderStates;

import static com.google.common.base.Preconditions.checkArgument;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants.MODEL_PRODUCTION_RECORD;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants.PARAM_RECORDING_TYPE_CUMULATED;
import static com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants.PARAM_RECORDING_TYPE_FOREACH;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.mes.orders.states.ChangeOrderStateMessage;
import com.qcadoo.mes.orders.states.OrderStateListener;
import com.qcadoo.mes.productionCounting.internal.constants.ProductionCountingConstants;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchRestrictions;

@Service
public class ProductionCountingOrderStatesListener extends OrderStateListener {

    @Autowired
    DataDefinitionService dataDefinitionService;

    @Autowired
    TranslationService translationService;

    @Override
    public List<ChangeOrderStateMessage> onCompleted(final Entity newEntity) {
        checkArgument(newEntity != null, "entity is null");
        List<ChangeOrderStateMessage> listOfMessage = new ArrayList<ChangeOrderStateMessage>();

        Entity order = newEntity.getDataDefinition().get(newEntity.getId());
        ChangeOrderStateMessage message = null;
        String typeOfProductionRecording = order.getStringField("typeOfProductionRecording");
        if (PARAM_RECORDING_TYPE_CUMULATED.equals(typeOfProductionRecording)) {
            message = checkFinalProductionCountingForOrderCumulated(order);
        } else if (PARAM_RECORDING_TYPE_FOREACH.equals(typeOfProductionRecording)) {
            message = checkFinalProductionCountingForOrderForEach(order);
        }
        if (message != null) {
            listOfMessage.add(message);
        }
        return listOfMessage;
    }

    private ChangeOrderStateMessage checkFinalProductionCountingForOrderCumulated(final Entity order) {
        Boolean allowToClose = (Boolean) order.getField("allowToClose");
        ChangeOrderStateMessage message = null;
        List<Entity> productionRecordings = dataDefinitionService
                .get(ProductionCountingConstants.PLUGIN_IDENTIFIER, MODEL_PRODUCTION_RECORD).find()
                .add(SearchRestrictions.belongsTo("order", order)).add(SearchRestrictions.eq("lastRecord", true)).list()
                .getEntities();

        if (allowToClose != null && allowToClose && productionRecordings.size() == 0) {
            message = ChangeOrderStateMessage.error(translationService.translate("orders.order.state.allowToClose.failure",
                    LocaleContextHolder.getLocale()));
        }
        return message;
    }

    private ChangeOrderStateMessage checkFinalProductionCountingForOrderForEach(final Entity order) {
        Boolean allowToClose = (Boolean) order.getField("allowToClose");
        ChangeOrderStateMessage message = null;
        List<Entity> operations = order.getTreeField("orderOperationComponents");
        Integer numberOfRecord = Integer.valueOf(0);
        for (Entity operation : operations) {
            List<Entity> productionRecordings = dataDefinitionService
                    .get(ProductionCountingConstants.PLUGIN_IDENTIFIER, MODEL_PRODUCTION_RECORD).find()
                    .add(SearchRestrictions.belongsTo("order", order))
                    .add(SearchRestrictions.belongsTo("orderOperationComponent", operation))
                    .add(SearchRestrictions.eq("lastRecord", true)).list().getEntities();
            if (productionRecordings.size() != 0) {
                numberOfRecord++;
            }
        }
        if (allowToClose != null && allowToClose && operations.size() != numberOfRecord) {
            message = ChangeOrderStateMessage.error(translationService.translate("orders.order.state.allowToClose.failure",
                    LocaleContextHolder.getLocale()));
        }
        return message;
    }

}
