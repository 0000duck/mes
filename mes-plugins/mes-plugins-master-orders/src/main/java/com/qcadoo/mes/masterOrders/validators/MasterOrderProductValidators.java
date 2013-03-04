package com.qcadoo.mes.masterOrders.validators;

import static com.qcadoo.mes.masterOrders.constants.MasterOrderProductFields.MASTER_ORDER;
import static com.qcadoo.mes.masterOrders.constants.MasterOrderProductFields.PRODUCT;

import java.util.List;

import org.springframework.stereotype.Service;

import com.qcadoo.mes.masterOrders.constants.MasterOrderFields;
import com.qcadoo.mes.masterOrders.constants.MasterOrderProductFields;
import com.qcadoo.mes.masterOrders.constants.MasterOrderType;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchRestrictions;

@Service
public class MasterOrderProductValidators {

    public boolean checkIfEntityAlreadyExistsForProductAndMasterOrder(final DataDefinition masterOrderProductDD,
            final Entity masterOrderProduct) {
        List<Entity> masterOrderProductList = masterOrderProductDD.find()
                .add(SearchRestrictions.ne("id", masterOrderProduct.getId()))
                .add(SearchRestrictions.belongsTo(MASTER_ORDER, masterOrderProduct.getBelongsToField(MASTER_ORDER)))
                .add(SearchRestrictions.belongsTo(PRODUCT, masterOrderProduct.getBelongsToField(PRODUCT))).list().getEntities();

        if (masterOrderProductList.isEmpty()) {
            return true;
        } else {
            masterOrderProduct.addError(masterOrderProductDD.getField(PRODUCT),
                    "masterOrders.masterOrderProduct.alreadyExistsForProductAndMasterOrder");

            return false;
        }
    }

    public boolean checkIfCanChangedTechnology(final DataDefinition masterProductOrderDD, final Entity masterProductOrder) {
        if (masterProductOrder.getId() == null) {
            return true;
        }
        Entity masterProductOrderDB = masterProductOrderDD.get(masterProductOrder.getId());
        Entity masterOrder = masterProductOrder.getBelongsToField(MasterOrderProductFields.MASTER_ORDER);
        Entity technologyFromDB = masterProductOrderDB.getBelongsToField(MasterOrderFields.TECHNOLOGY);
        Entity productFromDB = masterProductOrder.getBelongsToField(MasterOrderFields.PRODUCT);
        Entity technology = masterProductOrder.getBelongsToField(MasterOrderFields.TECHNOLOGY);
        if (technology == null
                || (technology != null && technologyFromDB != null && technology.getId().equals(technologyFromDB.getId()))) {
            return true;
        }
        if (!masterOrder.getStringField(MasterOrderFields.MASTER_ORDER_TYPE).equals(
                MasterOrderType.MANY_PRODUCTS.getStringValue())) {
            return true;
        }

        List<Entity> orders = masterOrder.getHasManyField(MasterOrderFields.ORDERS).find()
                .add(SearchRestrictions.belongsTo(MasterOrderProductFields.TECHNOLOGY, technology))
                .add(SearchRestrictions.belongsTo(MasterOrderProductFields.PRODUCT, productFromDB)).list().getEntities();
        boolean isValid = true;
        StringBuilder orderNumberListWitkWrongNumer = new StringBuilder();
        for (Entity order : orders) {
            isValid = false;
            orderNumberListWitkWrongNumer.append(order.getStringField(OrderFields.NUMBER));
            orderNumberListWitkWrongNumer.append(", ");
        }
        if (!isValid) {
            masterProductOrder.addError(masterProductOrderDD.getField(MasterOrderFields.TECHNOLOGY),
                    "masterOrders.masterOrder.technology.wrongTechnology", orderNumberListWitkWrongNumer.toString());
        }
        return isValid;
    }

    public boolean checkIfCanChangedProduct(final DataDefinition masterOrderDD, final Entity masterOrder) {
        if (masterOrder.getId() == null) {
            return true;
        }
        Entity masterOrderFromDB = masterOrderDD.get(masterOrder.getId());
        Entity productFromDB = masterOrderFromDB.getBelongsToField(MasterOrderFields.PRODUCT);
        Entity product = masterOrder.getBelongsToField(MasterOrderFields.PRODUCT);
        if (product == null || (product != null && productFromDB != null && product.getId().equals(productFromDB.getId()))) {
            return true;
        }
        if (!masterOrder.getStringField(MasterOrderFields.MASTER_ORDER_TYPE).equals(
                MasterOrderType.MANY_PRODUCTS.getStringValue())) {
            return true;
        }
        List<Entity> orders = masterOrderFromDB.getHasManyField(MasterOrderFields.ORDERS).find()
                .add(SearchRestrictions.belongsTo(MasterOrderProductFields.PRODUCT, productFromDB)).list().getEntities();
        boolean isValid = true;
        StringBuilder orderNumberListWitkWrongNumer = new StringBuilder();
        for (Entity order : orders) {
            isValid = false;
            orderNumberListWitkWrongNumer.append(order.getStringField(OrderFields.NUMBER));
            orderNumberListWitkWrongNumer.append(", ");
        }
        if (!isValid) {
            masterOrder.addError(masterOrderDD.getField(MasterOrderFields.PRODUCT),
                    "masterOrders.masterOrder.product.wrongProduct", orderNumberListWitkWrongNumer.toString());
        }
        return isValid;
    }

}
