package com.qcadoo.mes.orders;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qcadoo.mes.basic.constants.ProductFields;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.orders.constants.OrderType;
import com.qcadoo.mes.orders.constants.OrdersConstants;
import com.qcadoo.mes.productionLines.ProductionLinesService;
import com.qcadoo.mes.productionLines.constants.ProductionLinesConstants;
import com.qcadoo.mes.productionLines.constants.TechOperCompWorkstationFields;
import com.qcadoo.mes.productionLines.constants.TechnologyOperationComponentFieldsPL;
import com.qcadoo.mes.technologies.constants.TechnologiesConstants;
import com.qcadoo.mes.technologies.constants.TechnologyFields;
import com.qcadoo.mes.technologies.constants.TechnologyType;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityTree;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.model.api.search.SearchResult;
import com.qcadoo.view.api.utils.NumberGeneratorService;

@Service
public class TechnologyServiceO {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private NumberGeneratorService numberGeneratorService;

    @Autowired
    private ProductionLinesService productionLinesService;

    public Entity getDefaultTechnology(final Entity product) {
        DataDefinition technologyDD = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                TechnologiesConstants.MODEL_TECHNOLOGY);

        SearchResult searchResult = technologyDD.find().setMaxResults(1).add(SearchRestrictions.eq("master", true))
                .add(SearchRestrictions.eq("active", true)).add(SearchRestrictions.belongsTo(TechnologyFields.PRODUCT, product))
                .list();

        if (searchResult.getTotalNumberOfEntities() == 1) {
            return searchResult.getEntities().get(0);
        } else {
            return null;
        }
    }

    @Transactional
    public void createOrUpdateTechnology(final DataDefinition dataDefinition, final Entity order) {
        if (OrderType.WITH_PATTERN_TECHNOLOGY.getStringValue().equals(order.getStringField(OrderFields.ORDER_TYPE))) {

            if (order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE) == null) {
                return;
            }
            DataDefinition technologyDD = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                    TechnologiesConstants.MODEL_TECHNOLOGY);

            DataDefinition orderDD = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER);

            if (isTechnologyCopied(order)) {
                if (isTypeOrderChnagedToPattern(order)) {
                    Entity technology = order.getBelongsToField(OrderFields.TECHNOLOGY);
                    Entity orderDB = orderDD.get(order.getId());
                    orderDB.setField(OrderFields.TECHNOLOGY, null);
                    orderDB.getDataDefinition().save(orderDB);

                    technologyDD.delete(technology.getId());

                    Entity newCopyOfTechnology = technologyDD.create();
                    newCopyOfTechnology = technologyDD.copy(order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE).getId())
                            .get(0);
                    newCopyOfTechnology.setField(TechnologyFields.NUMBER, numberGeneratorService.generateNumber(
                            TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY));
                    newCopyOfTechnology.setField(TechnologyFields.TECHNOLOGY_PROTOTYPE,
                            order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE));
                    newCopyOfTechnology.setField(TechnologyFields.TECHNOLOGY_TYPE,
                            TechnologyType.WITH_PATTERN_TECHNOLOGY.getStringValue());
                    newCopyOfTechnology = newCopyOfTechnology.getDataDefinition().save(newCopyOfTechnology);
                    order.setField(OrderFields.TECHNOLOGY, newCopyOfTechnology);
                } else if (technologyWasChanged(order)) {
                    Entity technology = order.getBelongsToField(OrderFields.TECHNOLOGY);
                    Entity orderDB = orderDD.get(order.getId());
                    orderDB.setField(OrderFields.TECHNOLOGY, null);
                    orderDB.getDataDefinition().save(orderDB);

                    technologyDD.delete(technology.getId());

                    Entity newCopyOfTechnology = technologyDD.create();
                    newCopyOfTechnology = technologyDD.copy(order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE).getId())
                            .get(0);
                    newCopyOfTechnology.setField(TechnologyFields.NUMBER, numberGeneratorService.generateNumber(
                            TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY));
                    newCopyOfTechnology.setField(TechnologyFields.TECHNOLOGY_PROTOTYPE,
                            order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE));
                    newCopyOfTechnology.setField(TechnologyFields.TECHNOLOGY_TYPE,
                            TechnologyType.WITH_PATTERN_TECHNOLOGY.getStringValue());
                    newCopyOfTechnology = newCopyOfTechnology.getDataDefinition().save(newCopyOfTechnology);
                    order.setField(OrderFields.TECHNOLOGY, newCopyOfTechnology);
                }

            } else {
                if (getExistingOrder(order) == null) {
                    Entity technology = technologyDD.create();
                    technology = technologyDD.copy(order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE).getId()).get(0);
                    technology.setField(TechnologyFields.NUMBER, numberGeneratorService.generateNumber(
                            TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY));
                    technology.setField(TechnologyFields.TECHNOLOGY_PROTOTYPE,
                            order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE));
                    technology
                            .setField(TechnologyFields.TECHNOLOGY_TYPE, TechnologyType.WITH_PATTERN_TECHNOLOGY.getStringValue());
                    technology = technology.getDataDefinition().save(technology);
                    order.setField(OrderFields.TECHNOLOGY, technology);
                } else if (!isTechnologySet(order)) {
                    Entity technology = technologyDD.create();
                    technology = technologyDD.copy(order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE).getId()).get(0);
                    technology.setField(TechnologyFields.NUMBER, numberGeneratorService.generateNumber(
                            TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY));
                    technology.setField(TechnologyFields.TECHNOLOGY_PROTOTYPE,
                            order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE));
                    technology
                            .setField(TechnologyFields.TECHNOLOGY_TYPE, TechnologyType.WITH_PATTERN_TECHNOLOGY.getStringValue());
                    technology = technology.getDataDefinition().save(technology);
                    order.setField(OrderFields.TECHNOLOGY, technology);
                }
            }
        } else if (OrderType.WITH_OWN_TECHNOLOGY.getStringValue().equals(order.getStringField(OrderFields.ORDER_TYPE))) {

            DataDefinition technologyDD = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                    TechnologiesConstants.MODEL_TECHNOLOGY);

            if (isTechnologyCopied(order)) {
                Entity prototypeTechnology = order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE);
                if (prototypeTechnology != null) {
                    order.setField(OrderFields.TECHNOLOGY_PROTOTYPE, null);
                    Entity technology = order.getBelongsToField(OrderFields.TECHNOLOGY);
                    technology.setField(TechnologyFields.TECHNOLOGY_PROTOTYPE, null);

                    technology.setField(TechnologyFields.TECHNOLOGY_TYPE, TechnologyType.WITH_OWN_TECHNOLOGY.getStringValue());
                    technology.setField(
                            TechnologyFields.NAME,
                            makeTechnologyName(technology.getStringField(TechnologyFields.NUMBER),
                                    order.getBelongsToField(OrderFields.PRODUCT)));

                    technology.getDataDefinition().save(technology);
                }

            } else {
                if (getExistingOrder(order) == null) {
                    Entity technology = technologyDD.create();
                    technology.setField(TechnologyFields.NUMBER, numberGeneratorService.generateNumber(
                            TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY));
                    technology.setField(
                            TechnologyFields.NAME,
                            makeTechnologyName(technology.getStringField(TechnologyFields.NUMBER),
                                    order.getBelongsToField(OrderFields.PRODUCT)));
                    technology.setField(TechnologyFields.PRODUCT, order.getBelongsToField(OrderFields.PRODUCT));
                    technology.setField(TechnologyFields.TECHNOLOGY_PROTOTYPE,
                            order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE));
                    technology.setField(TechnologyFields.TECHNOLOGY_TYPE, TechnologyType.WITH_OWN_TECHNOLOGY.getStringValue());
                    technology = technology.getDataDefinition().save(technology);

                    order.setField(OrderFields.TECHNOLOGY, technology);
                    Entity prototypeTechnology = order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE);
                    if (prototypeTechnology != null) {
                        order.setField(OrderFields.TECHNOLOGY_PROTOTYPE, null);
                    }
                } else if (getExistingOrder(order).getBelongsToField(OrderFields.TECHNOLOGY) == null) {
                    Entity technology = technologyDD.create();
                    technology.setField(TechnologyFields.NUMBER, numberGeneratorService.generateNumber(
                            TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY));
                    technology.setField(
                            TechnologyFields.NAME,
                            makeTechnologyName(technology.getStringField(TechnologyFields.NUMBER),
                                    order.getBelongsToField(OrderFields.PRODUCT)));
                    technology.setField(TechnologyFields.PRODUCT, order.getBelongsToField(OrderFields.PRODUCT));
                    technology.setField(TechnologyFields.TECHNOLOGY_PROTOTYPE,
                            order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE));
                    technology.setField(TechnologyFields.TECHNOLOGY_TYPE, TechnologyType.WITH_OWN_TECHNOLOGY.getStringValue());
                    technology = technology.getDataDefinition().save(technology);

                    order.setField(OrderFields.TECHNOLOGY, technology);
                    Entity prototypeTechnology = order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE);
                    if (prototypeTechnology != null) {
                        order.setField(OrderFields.TECHNOLOGY_PROTOTYPE, null);
                    }
                }
            }

        }

    }

    public void setQuantityOfWorkstationTypes(Entity order, final Entity technology) {
        DataDefinition technologyDD = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                TechnologiesConstants.MODEL_TECHNOLOGY);
        if (technology.getId() == null) {
            return;
        }
        Entity technologyDB = technologyDD.get(technology.getId());
        if (technologyDB == null) {
            return;
        }
        EntityTree techOperComponentTree = technologyDB.getTreeField(TechnologyFields.OPERATION_COMPONENTS);
        DataDefinition quantityOfWorkstationTypesDD = dataDefinitionService.get(ProductionLinesConstants.PLUGIN_IDENTIFIER,
                ProductionLinesConstants.MODEL_TECH_OPER_COMP_WORKSTATION);

        if (techOperComponentTree == null) {
            return;
        }

        Entity techOperCompWorkstation = quantityOfWorkstationTypesDD.create();
        for (Entity child : technologyDB.getHasManyField(TechnologyFields.OPERATION_COMPONENTS)) {
            techOperCompWorkstation = quantityOfWorkstationTypesDD.create();
            techOperCompWorkstation.setField(TechOperCompWorkstationFields.QUANTITY_OF_WORKSTATION_TYPES,
                    productionLinesService.getWorkstationTypesCount(child, order.getBelongsToField(OrderFields.PRODUCTION_LINE)));
            techOperCompWorkstation = quantityOfWorkstationTypesDD.save(techOperCompWorkstation);
            child.setField(TechnologyOperationComponentFieldsPL.TECH_OPER_COMP_WORKSTATION, techOperCompWorkstation);

            child = child.getDataDefinition().save(child);
        }
    }

    public void setQuantityOfWorkstationTypes(Entity order) {

        Entity technology = order.getBelongsToField(OrderFields.TECHNOLOGY);
        if (technology == null || technology.getId() == null) {
            return;
        }
        String sql = "select toc from #technologies_technologyOperationComponent as toc where technology.id = "
                + technology.getId();
        List<Entity> tocs = dataDefinitionService
                .get(TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY_OPERATION_COMPONENT)
                .find(sql).list().getEntities();

        DataDefinition quantityOfWorkstationTypesDD = dataDefinitionService.get(ProductionLinesConstants.PLUGIN_IDENTIFIER,
                ProductionLinesConstants.MODEL_TECH_OPER_COMP_WORKSTATION);

        Entity techOperCompWorkstation = quantityOfWorkstationTypesDD.create();
        for (Entity child : tocs) {
            if (child.getField(TechnologyOperationComponentFieldsPL.TECH_OPER_COMP_WORKSTATION) == null) {
                techOperCompWorkstation = quantityOfWorkstationTypesDD.create();
                techOperCompWorkstation.setField(
                        TechOperCompWorkstationFields.QUANTITY_OF_WORKSTATION_TYPES,
                        productionLinesService.getWorkstationTypesCount(child,
                                order.getBelongsToField(OrderFields.PRODUCTION_LINE)));
                techOperCompWorkstation = quantityOfWorkstationTypesDD.save(techOperCompWorkstation);
                child.setField(TechnologyOperationComponentFieldsPL.TECH_OPER_COMP_WORKSTATION, techOperCompWorkstation);

                child = child.getDataDefinition().save(child);
            }
        }
    }

    public String makeTechnologyName(final String technologyNumber, final Entity product) {
        return technologyNumber + " - " + product.getStringField(ProductFields.NUMBER);
    }

    private boolean isTypeOrderChnagedToPattern(final Entity order) {

        if (getExistingOrder(order) == null) {
            return false;
        }
        Entity existingOrder = getExistingOrder(order);
        String orderTypeDB = existingOrder.getStringField(OrderFields.ORDER_TYPE);
        if (orderTypeDB.equals(OrderType.WITH_PATTERN_TECHNOLOGY.getStringValue())) {
            return false;
        }
        return true;
    }

    private boolean isTechnologyCopied(final Entity order) {

        if (order.getField(OrderFields.TECHNOLOGY) == null) {
            return false;
        }

        return true;
    }

    private boolean isTechnologySet(final Entity order) {
        DataDefinition orderDD = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER);
        Entity orderDB = orderDD.get(order.getId());
        if (orderDB.getField(OrderFields.TECHNOLOGY) == null) {
            return false;
        }

        return true;
    }

    private boolean technologyWasChanged(final Entity order) {
        Entity existingOrder = getExistingOrder(order);
        if (existingOrder == null) {
            return false;
        }

        Entity technology = order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE);
        Entity existingOrderTechnology = existingOrder.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE);
        if (existingOrderTechnology == null) {
            return true;
        }

        if (!existingOrderTechnology.equals(technology)) {
            if (order.getBelongsToField(OrderFields.TECHNOLOGY) != null
                    && existingOrder.getBelongsToField(OrderFields.TECHNOLOGY) == null) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private Entity getExistingOrder(final Entity order) {
        if (order.getId() == null) {
            return null;
        }
        return order.getDataDefinition().get(order.getId());
    }

}
