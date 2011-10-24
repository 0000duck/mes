package com.qcadoo.mes.qualityControls;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.mes.basic.constants.BasicConstants;
import com.qcadoo.mes.orders.states.ChangeOrderStateMessage;
import com.qcadoo.mes.orders.states.OrderStateListener;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchResult;

@Service
public class QualityControlOrderStatesListener extends OrderStateListener {

    @Autowired
    TranslationService translationService;

    @Autowired
    DataDefinitionService dataDefinitionService;

    @Override
    public List<ChangeOrderStateMessage> onCompleted(final Entity newEntity) {

        checkArgument(newEntity != null, "entity is null");
        List<ChangeOrderStateMessage> listOfMessage = new ArrayList<ChangeOrderStateMessage>();
        if (isQualityControlAutoCheckEnabled() && !checkIfAllQualityControlsAreClosed(newEntity)) {
            listOfMessage.add(ChangeOrderStateMessage.error(translationService.translate(
                    "qualityControls.qualityControls.not.closed", LocaleContextHolder.getLocale())));
        }
        return listOfMessage;

    }

    private boolean isQualityControlAutoCheckEnabled() {
        SearchResult searchResult = dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, BasicConstants.MODEL_PARAMETER)
                .find().setMaxResults(1).list();

        Entity parameter = null;
        if (searchResult.getEntities().size() > 0) {
            parameter = searchResult.getEntities().get(0);
        }

        if (parameter != null) {
            return (Boolean) parameter.getField("checkDoneOrderForQuality");
        } else {
            return false;
        }
    }

    private boolean checkIfAllQualityControlsAreClosed(final Entity order) {
        if (order.getBelongsToField("technology") == null) {
            return true;
        }

        Object controlTypeField = order.getBelongsToField("technology").getField("qualityControlType");

        if (controlTypeField != null) {
            DataDefinition qualityControlDD = null;

            qualityControlDD = dataDefinitionService.get("qualityControls", "qualityControl");

            if (qualityControlDD != null) {
                SearchResult searchResult = qualityControlDD.find().belongsTo("order", order.getId()).isEq("closed", false)
                        .list();
                // qualityControlDD.find().add(SearchRestrictions.belongsTo("order", order)).add(SearchRestrictions.eq("closed",
                // false));
                return (searchResult.getTotalNumberOfEntities() <= 0);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
