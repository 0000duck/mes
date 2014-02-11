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
package com.qcadoo.mes.deliveries.listeners;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qcadoo.mes.deliveries.DeliveriesService;
import com.qcadoo.mes.deliveries.constants.DeliveredProductFields;
import com.qcadoo.mes.deliveries.constants.DeliveriesConstants;
import com.qcadoo.mes.deliveries.constants.DeliveryFields;
import com.qcadoo.mes.deliveries.constants.OrderedProductFields;
import com.qcadoo.mes.deliveries.hooks.DeliveryDetailsHooks;
import com.qcadoo.mes.deliveries.states.constants.DeliveryStateStringValues;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.NumberService;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ComponentState.MessageType;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.GridComponent;
import com.qcadoo.view.api.utils.NumberGeneratorService;

@Component
public class DeliveryDetailsListeners {

    private static final String L_FORM = "form";

    private static final String L_WINDOW_ACTIVE_MENU = "window.activeMenu";

    private static final String L_PRODUCT = "product";

    @Autowired
    private DeliveriesService deliveriesService;

    @Autowired
    private DeliveryDetailsHooks deliveryDetailsHooks;

    @Autowired
    private NumberService numberService;

    @Autowired
    private NumberGeneratorService numberGeneratorService;

    public void fillCompanyFieldsForSupplier(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        deliveryDetailsHooks.fillCompanyFieldsForSupplier(view);
    }

    public final void printDeliveryReport(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        if (state instanceof FormComponent) {
            state.performEvent(view, "save", args);

            if (!state.isHasError()) {
                view.redirectTo("/deliveries/deliveryReport." + args[0] + "?id=" + state.getFieldValue(), true, false);
            }
        } else {
            state.addMessage("deliveries.delivery.report.componentFormError", MessageType.FAILURE);
        }
    }

    public final void printOrderReport(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        if (state instanceof FormComponent) {
            state.performEvent(view, "save", args);

            if (!state.isHasError()) {
                view.redirectTo("/deliveries/orderReport." + args[0] + "?id=" + state.getFieldValue(), true, false);
            }
        } else {
            state.addMessage("deliveries.order.report.componentFormError", MessageType.FAILURE);
        }
    }

    public final void copyProductsWithoutQuantityAndPrice(final ViewDefinitionState view, final ComponentState state,
            final String[] args) {
        copyOrderedProductToDelivered(view, false);
    }

    public final void copyProductsWithQuantityAndPrice(final ViewDefinitionState view, final ComponentState state,
            final String[] args) {
        copyOrderedProductToDelivered(view, true);
    }

    private void copyOrderedProductToDelivered(final ViewDefinitionState view, boolean copyQuantityAndPrice) {
        FormComponent deliveryForm = (FormComponent) view.getComponentByReference(L_FORM);
        Long deliveryId = deliveryForm.getEntityId();

        if (deliveryId == null) {
            return;
        }

        Entity delivery = deliveriesService.getDelivery(deliveryId);

        copyOrderedProductToDelivered(delivery, copyQuantityAndPrice);
    }

    private void copyOrderedProductToDelivered(final Entity delivery, final boolean copyQuantityAndPrice) {
        // ALBR deliveredProduct has a validation so we have to delete all
        // entities before save HM field in delivery
        delivery.setField(DeliveryFields.DELIVERED_PRODUCTS, Lists.newArrayList());
        delivery.getDataDefinition().save(delivery);
        delivery.setField(DeliveryFields.DELIVERED_PRODUCTS,
                createDeliveredProducts(delivery.getHasManyField(DeliveryFields.ORDERED_PRODUCTS), copyQuantityAndPrice));

        delivery.getDataDefinition().save(delivery);
    }

    private List<Entity> createDeliveredProducts(final List<Entity> orderedProducts, final boolean copyQuantityAndPrice) {
        List<Entity> deliveredProducts = Lists.newArrayList();

        for (Entity orderedProduct : orderedProducts) {
            deliveredProducts.add(createDeliveredProduct(orderedProduct, copyQuantityAndPrice));
        }

        return deliveredProducts;
    }

    private Entity createDeliveredProduct(final Entity orderedProduct, final boolean copyQuantityAndPrice) {
        Entity deliveredProduct = deliveriesService.getDeliveredProductDD().create();

        deliveredProduct.setField(DeliveredProductFields.PRODUCT, orderedProduct.getBelongsToField(OrderedProductFields.PRODUCT));

        if (copyQuantityAndPrice) {
            deliveredProduct.setField(DeliveredProductFields.DELIVERED_QUANTITY,
                    numberService.setScale(orderedProduct.getDecimalField(OrderedProductFields.ORDERED_QUANTITY)));
            deliveredProduct.setField(DeliveredProductFields.PRICE_PER_UNIT,
                    numberService.setScale(orderedProduct.getDecimalField(OrderedProductFields.PRICE_PER_UNIT)));
            deliveredProduct.setField(DeliveredProductFields.TOTAL_PRICE,
                    numberService.setScale(orderedProduct.getDecimalField(OrderedProductFields.TOTAL_PRICE)));
        }

        return deliveredProduct;
    }

    public final void createRelatedDelivery(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        FormComponent deliveryForm = (FormComponent) view.getComponentByReference(L_FORM);
        Long deliveryId = deliveryForm.getEntityId();

        if (deliveryId == null) {
            return;
        }

        Entity delivery = deliveriesService.getDelivery(deliveryId);

        if (DeliveryStateStringValues.RECEIVED.equals(delivery.getStringField(DeliveryFields.STATE))
                || DeliveryStateStringValues.RECEIVE_CONFIRM_WAITING.equals(delivery.getStringField(DeliveryFields.STATE))) {
            Entity relatedDelivery = createRelatedDelivery(delivery);

            if (relatedDelivery == null) {
                deliveryForm.addMessage("deliveries.delivery.relatedDelivery.thereAreNoLacksToCover", MessageType.INFO);

                return;
            }

            Long relatedDeliveryId = relatedDelivery.getId();

            Map<String, Object> parameters = Maps.newHashMap();
            parameters.put("form.id", relatedDeliveryId);

            parameters.put(L_WINDOW_ACTIVE_MENU, "deliveries.deliveryDetails");

            String url = "../page/deliveries/deliveryDetails.html";
            view.redirectTo(url, false, true, parameters);
        }
    }

    private Entity createRelatedDelivery(final Entity delivery) {
        Entity relatedDelivery = null;

        List<Entity> orderedProducts = createOrderedProducts(delivery);

        if (!orderedProducts.isEmpty()) {
            relatedDelivery = deliveriesService.getDeliveryDD().create();

            relatedDelivery.setField(DeliveryFields.NUMBER, numberGeneratorService.generateNumber(
                    DeliveriesConstants.PLUGIN_IDENTIFIER, DeliveriesConstants.MODEL_DELIVERY));
            relatedDelivery.setField(DeliveryFields.SUPPLIER, delivery.getBelongsToField(DeliveryFields.SUPPLIER));
            relatedDelivery.setField(DeliveryFields.DELIVERY_DATE, new Date());
            relatedDelivery.setField(DeliveryFields.RELATED_DELIVERY, delivery);
            relatedDelivery.setField(DeliveryFields.ORDERED_PRODUCTS, orderedProducts);
            relatedDelivery.setField(DeliveryFields.EXTERNAL_SYNCHRONIZED, true);

            relatedDelivery = relatedDelivery.getDataDefinition().save(relatedDelivery);
        }

        return relatedDelivery;
    }

    private List<Entity> createOrderedProducts(final Entity delivery) {
        List<Entity> newOrderedProducts = Lists.newArrayList();

        List<Entity> orderedProducts = delivery.getHasManyField(DeliveryFields.ORDERED_PRODUCTS);
        List<Entity> deliveredProducts = delivery.getHasManyField(DeliveryFields.DELIVERED_PRODUCTS);

        for (Entity orderedProduct : orderedProducts) {
            Entity deliveredProduct = getDeliveredProduct(deliveredProducts, orderedProduct);

            if (deliveredProduct == null) {
                BigDecimal orderedQuantity = orderedProduct.getDecimalField(OrderedProductFields.ORDERED_QUANTITY);

                newOrderedProducts.add(createOrderedProduct(orderedProduct, orderedQuantity));
            } else {
                BigDecimal orderedQuantity = getLackQuantity(orderedProduct, deliveredProduct);

                if (BigDecimal.ZERO.compareTo(orderedQuantity) < 0) {
                    newOrderedProducts.add(createOrderedProduct(orderedProduct, orderedQuantity));
                }
            }
        }

        return newOrderedProducts;
    }

    private Entity getDeliveredProduct(final List<Entity> deliveredProducts, final Entity orderedProduct) {
        for (Entity deliveredProduct : deliveredProducts) {
            if (checkIfProductsAreSame(orderedProduct, deliveredProduct)) {
                return deliveredProduct;
            }
        }

        return null;
    }

    private boolean checkIfProductsAreSame(final Entity orderedProduct, final Entity deliveredProduct) {
        return (orderedProduct.getBelongsToField(OrderedProductFields.PRODUCT).getId().equals(deliveredProduct.getBelongsToField(
                DeliveredProductFields.PRODUCT).getId()));
    }

    private Entity createOrderedProduct(final Entity orderedProduct, final BigDecimal orderedQuantity) {
        Entity newOrderedProduct = deliveriesService.getOrderedProductDD().create();

        newOrderedProduct.setField(OrderedProductFields.PRODUCT, orderedProduct.getBelongsToField(OrderedProductFields.PRODUCT));
        newOrderedProduct.setField(OrderedProductFields.ORDERED_QUANTITY, numberService.setScale(orderedQuantity));
        newOrderedProduct.setField(OrderedProductFields.TOTAL_PRICE,
                orderedProduct.getDecimalField(OrderedProductFields.TOTAL_PRICE));
        newOrderedProduct.setField(OrderedProductFields.PRICE_PER_UNIT,
                orderedProduct.getDecimalField(OrderedProductFields.PRICE_PER_UNIT));
        return newOrderedProduct;
    }

    private BigDecimal getLackQuantity(final Entity orderedProduct, final Entity deliveredProduct) {
        BigDecimal orderedQuantity = orderedProduct.getDecimalField(OrderedProductFields.ORDERED_QUANTITY);
        BigDecimal deliveredQuantity = deliveredProduct.getDecimalField(DeliveredProductFields.DELIVERED_QUANTITY);
        BigDecimal damagedQuantity = deliveredProduct.getDecimalField(DeliveredProductFields.DAMAGED_QUANTITY);

        if (damagedQuantity == null) {
            return orderedQuantity.subtract(deliveredQuantity, numberService.getMathContext());
        } else {
            return orderedQuantity.subtract(deliveredQuantity, numberService.getMathContext()).add(damagedQuantity,
                    numberService.getMathContext());
        }
    }

    public final void showRelatedDeliveries(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        FormComponent deliveryForm = (FormComponent) view.getComponentByReference(L_FORM);
        Long deliveryId = deliveryForm.getEntityId();

        if (deliveryId == null) {
            return;
        }

        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("delivery.id", deliveryId);

        parameters.put(L_WINDOW_ACTIVE_MENU, "deliveries.deliveriesList");

        String url = "../page/deliveries/deliveriesList.html";
        view.redirectTo(url, false, true, parameters);
    }

    public final void showProduct(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        GridComponent orderedProductGrid = (GridComponent) view.getComponentByReference(DeliveryFields.ORDERED_PRODUCTS);
        GridComponent deliveredProductsGrid = (GridComponent) view.getComponentByReference(DeliveryFields.DELIVERED_PRODUCTS);

        List<Entity> selectedEntities = orderedProductGrid.getSelectedEntities();
        if (selectedEntities.isEmpty()) {
            selectedEntities = deliveredProductsGrid.getSelectedEntities();
        }

        Entity selectedEntity = selectedEntities.iterator().next();
        Entity product = selectedEntity.getBelongsToField(L_PRODUCT);

        Map<String, Object> parameters = Maps.newHashMap();

        parameters.put("form.id", product.getId());

        parameters.put(L_WINDOW_ACTIVE_MENU, "basic.productDetails");

        String url = "../page/basic/productDetails.html";
        view.redirectTo(url, false, true, parameters);
    }

    public void disableShowProductButton(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        deliveriesService.disableShowProductButton(view);
    }

}