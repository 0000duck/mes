/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.2.0-SNAPSHOT
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
package com.qcadoo.mes.materialFlowResources;

import static com.qcadoo.mes.basic.constants.ProductFields.NAME;
import static com.qcadoo.mes.materialFlow.constants.LocationFields.TYPE;
import static com.qcadoo.mes.materialFlow.constants.TransferFields.LOCATION_FROM;
import static com.qcadoo.mes.materialFlow.constants.TransferFields.LOCATION_TO;
import static com.qcadoo.mes.materialFlow.constants.TransferFields.TIME;
import static com.qcadoo.mes.materialFlowResources.constants.LocationTypeMFR.WAREHOUSE;
import static com.qcadoo.mes.materialFlowResources.constants.ResourceFields.BATCH;
import static com.qcadoo.mes.materialFlowResources.constants.ResourceFields.LOCATION;
import static com.qcadoo.mes.materialFlowResources.constants.ResourceFields.PRODUCT;
import static com.qcadoo.mes.materialFlowResources.constants.ResourceFields.QUANTITY;
import static com.qcadoo.mes.materialFlowResources.constants.TransferFieldsMFR.PRICE;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qcadoo.mes.basic.ParameterService;
import com.qcadoo.mes.materialFlowResources.constants.MaterialFlowResourcesConstants;
import com.qcadoo.mes.materialFlowResources.constants.ParameterFieldsMFR;
import com.qcadoo.mes.materialFlowResources.constants.ResourceFields;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.NumberService;
import com.qcadoo.model.api.search.SearchOrders;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.LookupComponent;

@Service
public class MaterialFlowResourcesServiceImpl implements MaterialFlowResourcesService {

    private static final String L_FORM = "form";

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private NumberService numberService;

    @Autowired
    private ParameterService parameterService;

    @Override
    public boolean areResourcesSufficient(final Entity location, final Entity product, final BigDecimal quantity) {
        List<Entity> resources = getResourcesForLocationAndProduct(location, product);

        String type = location.getStringField(TYPE);

        if (isTypeWarehouse(type)) {
            if (resources == null) {
                return false;
            } else {
                BigDecimal resourcesQuantity = BigDecimal.ZERO;

                for (Entity resource : resources) {
                    resourcesQuantity = resourcesQuantity.add(resource.getDecimalField(QUANTITY), numberService.getMathContext());
                }

                return (resourcesQuantity.compareTo(quantity) >= 0);
            }
        } else {
            return true;
        }
    }

    @Transactional
    @Override
    public void manageResources(final Entity transfer) {
        if (transfer == null) {
            return;
        }

        Entity locationFrom = transfer.getBelongsToField(LOCATION_FROM);
        Entity locationTo = transfer.getBelongsToField(LOCATION_TO);
        Entity product = transfer.getBelongsToField(PRODUCT);
        BigDecimal quantity = transfer.getDecimalField(QUANTITY);
        Date time = (Date) transfer.getField(TIME);
        BigDecimal price = transfer.getDecimalField(PRICE);

        if ((locationFrom != null) && isTypeWarehouse(locationFrom.getStringField(TYPE)) && (locationTo != null)
                && isTypeWarehouse(locationTo.getStringField(TYPE))) {
            moveResource(locationFrom, locationTo, product, quantity, time, price);
        } else if ((locationFrom != null) && isTypeWarehouse(locationFrom.getStringField(TYPE))) {
            updateResource(locationFrom, product, quantity);
        } else if ((locationTo != null) && isTypeWarehouse(locationTo.getStringField(TYPE))) {
            addResource(locationTo, product, quantity, time, price);
        }
    }

    private boolean isTypeWarehouse(final String type) {
        return ((type != null) && WAREHOUSE.getStringValue().equals(type));
    }

    @Override
    public void addResource(final Entity locationTo, final Entity product, final BigDecimal quantity, final Date time,
            final BigDecimal price) {
        addResource(locationTo, product, quantity, time, price, null);
    }

    @Override
    public void addResource(final Entity locationTo, final Entity product, final BigDecimal quantity, final Date time,
            final BigDecimal price, final String batch) {
        Entity resource = dataDefinitionService.get(MaterialFlowResourcesConstants.PLUGIN_IDENTIFIER,
                MaterialFlowResourcesConstants.MODEL_RESOURCE).create();

        resource.setField(LOCATION, locationTo);
        resource.setField(PRODUCT, product);
        resource.setField(QUANTITY, quantity);
        resource.setField(TIME, time);
        resource.setField(BATCH, batch);
        resource.setField(PRICE, price);

        resource.getDataDefinition().save(resource);
    }

    @Override
    public void updateResource(final Entity locationFrom, final Entity product, BigDecimal quantity) {
        List<Entity> resources = getResourcesForLocationAndProduct(locationFrom, product);

        if (resources != null) {
            for (Entity resource : resources) {
                BigDecimal resourceQuantity = resource.getDecimalField(QUANTITY);

                if (quantity.compareTo(resourceQuantity) >= 0) {
                    quantity = quantity.subtract(resourceQuantity, numberService.getMathContext());

                    resource.getDataDefinition().delete(resource.getId());

                    if (BigDecimal.ZERO.equals(quantity)) {
                        return;
                    }
                } else {
                    resourceQuantity = resourceQuantity.subtract(quantity, numberService.getMathContext());

                    resource.setField(QUANTITY, resourceQuantity);

                    resource.getDataDefinition().save(resource);

                    return;
                }
            }
        }
    }

    @Override
    public void moveResource(final Entity locationFrom, final Entity locationTo, final Entity product, BigDecimal quantity,
            final Date time, final BigDecimal price) {
        List<Entity> resources = getResourcesForLocationAndProduct(locationFrom, product);

        if (resources != null) {
            for (Entity resource : resources) {
                BigDecimal resourceQuantity = resource.getDecimalField(QUANTITY);
                BigDecimal resourcePrice = (price == null) ? resource.getDecimalField(PRICE) : price;
                String resourceBatch = (price == null) ? resource.getStringField(BATCH) : null;

                if (quantity.compareTo(resourceQuantity) >= 0) {
                    quantity = quantity.subtract(resourceQuantity, numberService.getMathContext());

                    resource.getDataDefinition().delete(resource.getId());

                    addResource(locationTo, product, resourceQuantity, time, resourcePrice, resourceBatch);

                    if (BigDecimal.ZERO.compareTo(quantity) == 0) {
                        return;
                    }
                } else {
                    resourceQuantity = resourceQuantity.subtract(quantity, numberService.getMathContext());

                    resource.setField(QUANTITY, resourceQuantity);

                    resource.getDataDefinition().save(resource);

                    addResource(locationTo, product, quantity, time, resourcePrice, resourceBatch);

                    return;
                }
            }
        }
    }

    public List<Entity> getResourcesForLocationAndProduct(final Entity location, final Entity product) {
        List<Entity> resources = dataDefinitionService
                .get(MaterialFlowResourcesConstants.PLUGIN_IDENTIFIER, MaterialFlowResourcesConstants.MODEL_RESOURCE).find()
                .add(SearchRestrictions.belongsTo(LOCATION, location)).add(SearchRestrictions.belongsTo(PRODUCT, product))
                .addOrder(SearchOrders.asc(TIME)).list().getEntities();

        return resources;
    }

    @Override
    public Map<Entity, BigDecimal> groupResourcesByProduct(final Entity location) {
        Map<Entity, BigDecimal> productsAndQuantities = new LinkedHashMap<Entity, BigDecimal>();

        List<Entity> resources = getResourcesForLocation(location);

        if (resources != null) {
            for (Entity resource : resources) {
                Entity product = resource.getBelongsToField(PRODUCT);
                BigDecimal quantity = resource.getDecimalField(QUANTITY);

                if (productsAndQuantities.containsKey(product)) {
                    productsAndQuantities.put(product,
                            productsAndQuantities.get(product).add(quantity, numberService.getMathContext()));
                } else {
                    productsAndQuantities.put(product, quantity);
                }
            }
        }

        return productsAndQuantities;
    }

    private List<Entity> getResourcesForLocation(final Entity location) {
        List<Entity> resources = dataDefinitionService
                .get(MaterialFlowResourcesConstants.PLUGIN_IDENTIFIER, MaterialFlowResourcesConstants.MODEL_RESOURCE).find()
                .createAlias(PRODUCT, PRODUCT).add(SearchRestrictions.belongsTo(LOCATION, location))
                .addOrder(SearchOrders.asc(PRODUCT + "." + NAME)).list().getEntities();

        return resources;
    }

    @Override
    public BigDecimal calculatePrice(final Entity location, final Entity product) {
        if ((location != null) && (product != null)) {
            List<Entity> resources = getResourcesForLocationAndProduct(location, product);

            if (resources != null) {
                BigDecimal avgPrice = BigDecimal.ZERO;
                BigDecimal avgQuantity = BigDecimal.ZERO;

                for (Entity resource : resources) {
                    BigDecimal quantity = resource.getDecimalField(ResourceFields.QUANTITY);
                    BigDecimal price = resource.getDecimalField(ResourceFields.PRICE);

                    if (price != null) {
                        avgPrice = avgPrice.add(quantity.multiply(price, numberService.getMathContext()),
                                numberService.getMathContext());
                        avgQuantity = avgQuantity.add(quantity, numberService.getMathContext());
                    }
                }

                if (!BigDecimal.ZERO.equals(avgPrice) && !BigDecimal.ZERO.equals(avgQuantity)) {
                    avgPrice = avgPrice.divide(avgQuantity, numberService.getMathContext());

                    return avgPrice;
                }
            }
        }

        return null;
    }

    public boolean canChangeDateWhenTransferToWarehouse() {
        return parameterService.getParameter().getBooleanField(ParameterFieldsMFR.CAN_CHANGE_DATE_WHEN_TRANSFER_TO_WAREHOUSE);
    }

    public void disableDateField(final ViewDefinitionState view) {
        FormComponent form = (FormComponent) view.getComponentByReference(L_FORM);

        FieldComponent dateField = (FieldComponent) view.getComponentByReference(TIME);
        LookupComponent locationFromField = (LookupComponent) view.getComponentByReference(LOCATION_FROM);
        LookupComponent locationToField = (LookupComponent) view.getComponentByReference(LOCATION_TO);

        Entity locationFrom = locationFromField.getEntity();
        Entity locationTo = locationToField.getEntity();

        if (form.getEntityId() == null) {
            if (isTransferBetweenWarehouses(locationFrom, locationTo) && !canChangeDateWhenTransferToWarehouse()) {
                String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                dateField.setFieldValue(currentDate);
                dateField.setEnabled(false);
            } else {
                dateField.setEnabled(true);
            }
        }
    }

    private boolean isTransferBetweenWarehouses(final Entity locationFrom, final Entity locationTo) {
        return ((locationFrom != null) && WAREHOUSE.getStringValue().equals(locationFrom.getStringField(TYPE)))
                || ((locationTo != null) && WAREHOUSE.getStringValue().equals(locationTo.getStringField(TYPE)));
    }

}
