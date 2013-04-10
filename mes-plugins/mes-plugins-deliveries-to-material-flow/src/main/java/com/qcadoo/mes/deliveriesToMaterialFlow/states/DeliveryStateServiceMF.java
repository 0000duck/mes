package com.qcadoo.mes.deliveriesToMaterialFlow.states;

import static com.qcadoo.mes.deliveriesToMaterialFlow.constants.TransferFieldsDTMF.FROM_DELIVERY;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.deliveries.constants.DeliveredProductFields;
import com.qcadoo.mes.deliveries.constants.DeliveryFields;
import com.qcadoo.mes.deliveriesToMaterialFlow.constants.DeliveryFieldsDTMF;
import com.qcadoo.mes.materialFlow.MaterialFlowService;
import com.qcadoo.mes.materialFlow.constants.LocationFields;
import com.qcadoo.mes.materialFlow.constants.MaterialFlowConstants;
import com.qcadoo.mes.materialFlow.constants.TransferFields;
import com.qcadoo.mes.materialFlow.constants.TransferType;
import com.qcadoo.mes.states.StateChangeContext;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;

@Service
public class DeliveryStateServiceMF {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private MaterialFlowService materialFlowService;

    public void createTransfersForTheReceivedProducts(final StateChangeContext stateChangeContext) {

        final Entity delivery = stateChangeContext.getOwner();

        if (delivery == null) {
            return;
        }

        Entity location = (Entity) delivery.getField(DeliveryFieldsDTMF.LOCATION);

        if (location == null) {
            return;
        }

        if (StringUtils.isEmpty(location.getStringField(LocationFields.EXTERNAL_NUMBER))) {

            List<Entity> deliveredProducts = delivery.getHasManyField(DeliveryFields.DELIVERED_PRODUCTS);

            DataDefinition transferDD = dataDefinitionService.get(MaterialFlowConstants.PLUGIN_IDENTIFIER,
                    MaterialFlowConstants.MODEL_TRANSFER);

            for (Entity product : deliveredProducts) {

                Entity transfer = transferDD.create();

                transfer.setField(TransferFields.NUMBER, materialFlowService.generateNumberFromProduct(
                        (Entity) product.getField(DeliveredProductFields.PRODUCT), MaterialFlowConstants.MODEL_TRANSFER));
                transfer.setField(TransferFields.TIME, delivery.getField(DeliveryFields.DELIVERY_DATE));
                transfer.setField(TransferFields.PRODUCT, product.getField(DeliveredProductFields.PRODUCT));
                transfer.setField(TransferFields.QUANTITY, product.getField(DeliveredProductFields.DELIVERED_QUANTITY));
                transfer.setField(TransferFields.LOCATION_TO, delivery.getField(DeliveryFieldsDTMF.LOCATION));
                transfer.setField(TransferFields.TYPE, TransferType.TRANSPORT.getStringValue());
                transfer.setField(FROM_DELIVERY, delivery);

                transfer.getDataDefinition().save(transfer);
            }

        }

    }
}
