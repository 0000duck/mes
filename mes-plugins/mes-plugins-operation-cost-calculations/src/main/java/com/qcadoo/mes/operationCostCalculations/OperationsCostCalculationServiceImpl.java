/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.7
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
package com.qcadoo.mes.operationCostCalculations;

import static com.google.common.base.Preconditions.checkArgument;
import static com.qcadoo.mes.costNormsForOperation.constants.CalculationOperationComponentFields.LABOR_HOURLY_COST;
import static com.qcadoo.mes.costNormsForOperation.constants.CalculationOperationComponentFields.MACHINE_HOURLY_COST;
import static com.qcadoo.mes.costNormsForOperation.constants.CostNormsForOperationConstants.MODEL_CALCULATION_OPERATION_COMPONENT;
import static com.qcadoo.mes.costNormsForOperation.constants.CostNormsForOperationConstants.PLUGIN_IDENTIFIER;
import static java.util.Arrays.asList;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qcadoo.mes.costNormsForOperation.constants.CalculateOperationCostMode;
import com.qcadoo.mes.operationTimeCalculations.OperationWorkTime;
import com.qcadoo.mes.operationTimeCalculations.OperationWorkTimeService;
import com.qcadoo.mes.productionLines.ProductionLinesService;
import com.qcadoo.mes.technologies.ProductQuantitiesService;
import com.qcadoo.mes.technologies.constants.TechnologiesConstants;
import com.qcadoo.mes.technologies.constants.TechnologyFields;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityTree;
import com.qcadoo.model.api.EntityTreeNode;
import com.qcadoo.model.api.NumberService;

@Service
public class OperationsCostCalculationServiceImpl implements OperationsCostCalculationService {

    private static final String L_PRODUCTION_LINE = "productionLine";

    private static final String L_QUANTITY = "quantity";

    private static final String L_TECHNOLOGY = "technology";

    private static final String L_ORDER = "order";

    private static final String L_OPERATION = "operation";

    private static final String L_ENTITY_TYPE = "entityType";

    private static final String L_PRODUCTION_IN_ONE_CYCLE = "productionInOneCycle";

    private static final String L_TECHNOLOGY_OPERATION_COMPONENT = "technologyOperationComponent";

    private static final String L_CALCULATION_OPERATION_COMPONENTS = "calculationOperationComponents";

    private static final String L_COST_CALCULATION = "costCalculation";

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private OperationWorkTimeService operationWorkTimeService;

    @Autowired
    private NumberService numberService;

    @Autowired
    private ProductionLinesService productionLinesService;

    @Autowired
    private ProductQuantitiesService productQuantitiesService;

    private Map<Entity, BigDecimal> operationsRuns = new HashMap<Entity, BigDecimal>();

    private static final Logger LOG = LoggerFactory.getLogger(OperationsCostCalculationServiceImpl.class);

    private static final Set<String> PATH_COST_KEYS = Sets.newHashSet(LABOR_HOURLY_COST, MACHINE_HOURLY_COST);

    @Override
    public void calculateOperationsCost(final Entity entity) {
        checkArgument(entity != null, "entity is null");
        String modelName = entity.getDataDefinition().getName();
        checkArgument(L_COST_CALCULATION.equals(modelName) || "productionBalance".equals(modelName), "unsupported entity type");

        DataDefinition costCalculationDD = entity.getDataDefinition();

        CalculateOperationCostMode mode = CalculateOperationCostMode.parseString(entity
                .getStringField("calculateOperationCostsMode"));
        BigDecimal quantity = getBigDecimal(entity.getField(L_QUANTITY));
        Boolean includeTPZ = entity.getBooleanField("includeTPZ");
        Boolean includeAdditionalTime = entity.getBooleanField("includeAdditionalTime");
        BigDecimal margin = getBigDecimal(entity.getField("productionCostMargin"));

        Entity costCalculation = copyTechnologyTree(entity);

        Entity yetAnotherCostCalculation = costCalculationDD.save(costCalculation);
        Entity newCostCalculation = costCalculationDD.get(yetAnotherCostCalculation.getId());
        EntityTree calculationOperationComponents = newCostCalculation.getTreeField(L_CALCULATION_OPERATION_COMPONENTS);

        checkArgument(calculationOperationComponents != null, "given operation components is null");

        Entity technology = entity.getBelongsToField(L_TECHNOLOGY);
        Entity order = entity.getBelongsToField(L_ORDER);
        if (order != null) {
            Entity technologyFromOrder = order.getBelongsToField(L_TECHNOLOGY);
            technology = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                    TechnologiesConstants.MODEL_TECHNOLOGY).get(technologyFromOrder.getId());
        }
        productQuantitiesService.getProductComponentQuantities(technology, quantity, operationsRuns);
        if (CalculateOperationCostMode.PIECEWORK.equals(mode)) {
            if (calculationOperationComponents.isEmpty()) {
                entity.addError(entity.getDataDefinition().getField(L_ORDER), "costCalculation.lackOfTreeComponents");
                entity.addError(entity.getDataDefinition().getField(L_TECHNOLOGY), "costCalculation.lackOfTreeComponents");
                return;
            }
            BigDecimal totalPieceworkCost = estimateCostCalculationForPieceWork(calculationOperationComponents.getRoot(),
                    operationsRuns, margin, quantity);
            entity.setField("totalPieceworkCosts", numberService.setScale(totalPieceworkCost));
        } else if (CalculateOperationCostMode.HOURLY.equals(mode)) {
            Entity productionLine = entity.getBelongsToField(L_PRODUCTION_LINE);
            Map<Entity, Integer> workstations = getWorkstationsMapsForOperationsComponent(costCalculation, productionLine);
            Map<Entity, OperationWorkTime> realizationTimes = operationWorkTimeService.estimateOperationsWorkTime(
                    calculationOperationComponents, operationsRuns, includeTPZ, includeAdditionalTime, workstations, true);

            Map<String, BigDecimal> hourlyResultsMap = estimateCostCalculationForHourly(calculationOperationComponents.getRoot(),
                    margin, quantity, realizationTimes);
            entity.setField("totalMachineHourlyCosts", numberService.setScale(hourlyResultsMap.get(MACHINE_HOURLY_COST)));
            entity.setField("totalLaborHourlyCosts", numberService.setScale(hourlyResultsMap.get(LABOR_HOURLY_COST)));
        } else {
            throw new IllegalStateException("Unsupported calculateOperationCostMode");
        }

        entity.setField(L_CALCULATION_OPERATION_COMPONENTS, calculationOperationComponents);
    }

    private Map<String, BigDecimal> estimateCostCalculationForHourly(final EntityTreeNode operationComponent,
            final BigDecimal margin, final BigDecimal plannedQuantity, final Map<Entity, OperationWorkTime> realizationTimes) {
        return estimateCostCalculationForHourly(operationComponent, margin, plannedQuantity, realizationTimes, true);
    }

    public Map<String, BigDecimal> estimateCostCalculationForHourlyWitoutSaving(final EntityTreeNode operationComponent,
            final BigDecimal margin, final BigDecimal plannedQuantity, final Map<Entity, OperationWorkTime> realizationTimes) {
        return estimateCostCalculationForHourly(operationComponent, margin, plannedQuantity, realizationTimes, false);
    }

    private Map<String, BigDecimal> estimateCostCalculationForHourly(final EntityTreeNode calcOperComp, final BigDecimal margin,
            final BigDecimal plannedQuantity, final Map<Entity, OperationWorkTime> realizationTimes, final boolean saveValues) {
        checkArgument(calcOperComp != null, "given operationComponent is empty");
        Map<String, BigDecimal> resultsMap = Maps.newHashMapWithExpectedSize(PATH_COST_KEYS.size());
        MathContext mc = numberService.getMathContext();

        for (String key : PATH_COST_KEYS) {
            resultsMap.put(key, BigDecimal.ZERO);
        }

        for (EntityTreeNode child : calcOperComp.getChildren()) {
            Map<String, BigDecimal> unitResultsMap = estimateCostCalculationForHourly(child, margin, plannedQuantity,
                    realizationTimes, saveValues);
            for (String key : PATH_COST_KEYS) {
                BigDecimal unitOperationCost = resultsMap.get(key).add(unitResultsMap.get(key), numberService.getMathContext());
                resultsMap.put(key, unitOperationCost);
            }
        }
        OperationWorkTime dur = realizationTimes.get(calcOperComp);
        Map<String, BigDecimal> costs = estimateHourlyCostCalculationSingleOperations(calcOperComp, dur, margin);
        savedGeneratedValues(costs, calcOperComp, true, dur, null);

        resultsMap.put(MACHINE_HOURLY_COST, resultsMap.get(MACHINE_HOURLY_COST).add(costs.get("operationMachineCost"), mc));
        resultsMap.put(LABOR_HOURLY_COST, resultsMap.get(LABOR_HOURLY_COST).add(costs.get("operationLaborCost"), mc));
        return resultsMap;
    }

    private Map<String, BigDecimal> estimateHourlyCostCalculationSingleOperations(final Entity calcOperComp,
            final OperationWorkTime dur, final BigDecimal margin) {

        Map<String, BigDecimal> results = new HashMap<String, BigDecimal>();
        BigDecimal hourlyMachineCost = getBigDecimal(calcOperComp.getField(MACHINE_HOURLY_COST));
        BigDecimal hourlyLaborCost = getBigDecimal(calcOperComp.getField(LABOR_HOURLY_COST));

        BigDecimal durationMachine = BigDecimal.valueOf(dur.getMachineWorkTime());
        BigDecimal durationLabor = BigDecimal.valueOf(dur.getLaborWorkTime());

        BigDecimal durationMachineInHours = durationMachine.divide(BigDecimal.valueOf(3600), numberService.getMathContext());
        BigDecimal durationLaborInHours = durationLabor.divide(BigDecimal.valueOf(3600), numberService.getMathContext());

        BigDecimal operationMachineCost = durationMachineInHours.multiply(hourlyMachineCost, numberService.getMathContext());
        BigDecimal operationLaborCost = durationLaborInHours.multiply(hourlyLaborCost, numberService.getMathContext());

        BigDecimal operationCost = operationMachineCost.add(operationLaborCost, numberService.getMathContext());
        BigDecimal operationMarginCost = operationCost.multiply(
                margin.divide(BigDecimal.valueOf(100), numberService.getMathContext()), numberService.getMathContext());

        results.put("operationCost", operationCost);
        results.put("operationMarginCost", operationMarginCost);
        results.put("operationMachineCost", operationMachineCost);
        results.put("operationLaborCost", operationLaborCost);
        return results;

    }

    private void savedGeneratedValues(final Map<String, BigDecimal> costs, final Entity calcOperComp, boolean hourlyCosts,
            final OperationWorkTime dur, final BigDecimal operationRuns) {
        if (hourlyCosts) {
            calcOperComp.setField("duration", new BigDecimal(dur.getDuration(), numberService.getMathContext()));
        } else {
            calcOperComp.setField("pieces", numberService.setScale(operationRuns));
        }
        BigDecimal operationCost = costs.get("operationCost");
        BigDecimal operationMarginCost = costs.get("operationMarginCost");
        calcOperComp.setField("operationCost", numberService.setScale(operationCost));
        calcOperComp.setField("operationMarginCost", numberService.setScale(operationMarginCost));
        calcOperComp.setField("totalOperationCost",
                numberService.setScale(operationCost.add(operationMarginCost, numberService.getMathContext())));

        checkArgument(calcOperComp.getDataDefinition().save(calcOperComp).isValid(), "invalid operationComponent");
    }

    private BigDecimal estimateCostCalculationForPieceWork(final EntityTreeNode operationComponent,
            final Map<Entity, BigDecimal> productComponentQuantities, final BigDecimal margin, final BigDecimal plannedQuantity) {
        return estimateCostCalculationForPieceWork(operationComponent, productComponentQuantities, margin, plannedQuantity, true);
    }

    public BigDecimal estimateCostCalculationForPieceWorkWithoutSaving(final EntityTreeNode operationComponent,
            final Map<Entity, BigDecimal> productComponentQuantities, final BigDecimal margin, final BigDecimal plannedQuantity) {
        return estimateCostCalculationForPieceWork(operationComponent, productComponentQuantities, margin, plannedQuantity, false);
    }

    private BigDecimal estimateCostCalculationForPieceWork(final EntityTreeNode calcOperComp,
            final Map<Entity, BigDecimal> productComponentQuantities, final BigDecimal margin, final BigDecimal plannedQuantity,
            final boolean saveValues) {

        BigDecimal totalPieceworkCost = BigDecimal.ZERO;

        for (EntityTreeNode child : calcOperComp.getChildren()) {
            totalPieceworkCost = totalPieceworkCost.add(
                    estimateCostCalculationForPieceWork(child, productComponentQuantities, margin, plannedQuantity),
                    numberService.getMathContext());
        }
        Entity techOperComp = calcOperComp.getBelongsToField("technologyOperationComponent");
        // TODO mici, proxy entity thing. I think we should tweak hashCode too.
        Long techOperCompId = techOperComp.getId();
        techOperComp = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                TechnologiesConstants.MODEL_TECHNOLOGY_OPERATION_COMPONENT).get(techOperCompId);

        BigDecimal operationRuns = productComponentQuantities.get(techOperComp);
        Map<String, BigDecimal> costs = estimatePieceworkCostCalculationSingleOperations(calcOperComp, operationRuns, margin);
        totalPieceworkCost = totalPieceworkCost.add(costs.get("totalOperationCost"));
        savedGeneratedValues(costs, calcOperComp, false, null, operationRuns);
        return totalPieceworkCost;
    }

    private Map<String, BigDecimal> estimatePieceworkCostCalculationSingleOperations(final EntityTreeNode calcOperComp,
            final BigDecimal operationRuns, final BigDecimal margin) {
        Map<String, BigDecimal> results = new HashMap<String, BigDecimal>();

        BigDecimal pieceworkCost = getBigDecimal(calcOperComp.getField("pieceworkCost"));
        BigDecimal numberOfOperations = getBigDecimal(calcOperComp.getField("numberOfOperations"));

        BigDecimal pieceworkCostPerOperation = pieceworkCost.divide(numberOfOperations, numberService.getMathContext());
        BigDecimal operationCost = operationRuns.multiply(pieceworkCostPerOperation, numberService.getMathContext());
        BigDecimal operationMarginCost = operationCost.multiply(margin.divide(BigDecimal.valueOf(100),
                numberService.getMathContext()));
        BigDecimal totalOperationCost = numberService.setScale(operationCost.add(operationMarginCost,
                numberService.getMathContext()));

        results.put("operationCost", numberService.setScale(operationCost));
        results.put("operationMarginCost", numberService.setScale(operationMarginCost));
        results.put("pieces", numberService.setScale(operationRuns));
        results.put("totalOperationCost", totalOperationCost);

        return results;
    }

    @Transactional
    private Entity copyTechnologyTree(final Entity costCalculation) {
        EntityTree sourceOperationComponents;

        deleteOperationsTreeIfExists(costCalculation);

        if (costCalculation.getBelongsToField(L_ORDER) == null) {
            sourceOperationComponents = costCalculation.getBelongsToField(L_TECHNOLOGY).getTreeField("operationComponents");
        } else {
            sourceOperationComponents = costCalculation.getBelongsToField(L_ORDER).getTreeField(
                    "technologyInstanceOperationComponents");
        }
        return createTechnologyInstanceForCalculation(sourceOperationComponents, costCalculation);
    }

    private Entity createTechnologyInstanceForCalculation(final EntityTree sourceTree, final Entity parentEntity) {
        checkArgument(sourceTree != null, "source is null");
        DataDefinition calculationOperationComponentDD = dataDefinitionService.get(PLUGIN_IDENTIFIER,
                MODEL_CALCULATION_OPERATION_COMPONENT);

        // drop old operation components tree
        EntityTree oldCalculationOperationComponents = parentEntity.getTreeField(L_CALCULATION_OPERATION_COMPONENTS);
        if (oldCalculationOperationComponents != null && oldCalculationOperationComponents.getRoot() != null) {
            calculationOperationComponentDD.delete(oldCalculationOperationComponents.getRoot().getId());
        }

        Entity tree = createCalculationOperationComponent(sourceTree.getRoot(), null, calculationOperationComponentDD,
                parentEntity);

        parentEntity.setField(L_CALCULATION_OPERATION_COMPONENTS, asList(tree));
        return parentEntity;
    }

    private Entity createCalculationOperationComponent(final EntityTreeNode sourceTreeNode, final Entity parent,
            final DataDefinition calculationOperationComponentDD, final Entity parentEntity) {
        Entity calculationOperationComponent = calculationOperationComponentDD.create();

        calculationOperationComponent.setField("parent", parent);
        calculationOperationComponent.setField(parentEntity.getDataDefinition().getName(), parentEntity);

        if (L_OPERATION.equals(sourceTreeNode.getField(L_ENTITY_TYPE))) {
            createOrCopyCalculationOperationComponent(sourceTreeNode, calculationOperationComponentDD,
                    calculationOperationComponent, parentEntity);
        } else {
            Entity referenceTechnology = sourceTreeNode.getBelongsToField("referenceTechnology");
            createOrCopyCalculationOperationComponent(referenceTechnology.getTreeField("operationComponents").getRoot(),
                    calculationOperationComponentDD, calculationOperationComponent, parentEntity);
        }

        return calculationOperationComponent;
    }

    private void createOrCopyCalculationOperationComponent(final EntityTreeNode operationComponent,
            final DataDefinition calculationOperationComponentDD, final Entity calculationOperationComponent,
            final Entity costCalculation) {
        DataDefinition sourceDD = operationComponent.getDataDefinition();

        for (String fieldName : Arrays.asList("priority", "nodeNumber", "tpz", "tj", L_PRODUCTION_IN_ONE_CYCLE,
                "nextOperationAfterProducedQuantity", "timeNextOperation", "operationOffSet",
                "effectiveOperationRealizationTime", "effectiveDateFrom", "effectiveDateTo", "pieceworkCost", "laborHourlyCost",
                "machineHourlyCost", "numberOfOperations", "totalOperationCost", "laborUtilization", "machineUtilization")) {
            calculationOperationComponent.setField(fieldName, operationComponent.getField(fieldName));
        }

        calculationOperationComponent.setField(L_OPERATION, operationComponent.getBelongsToField(L_OPERATION));
        calculationOperationComponent.setField(
                "nextOperationAfterProducedType",
                operationComponent.getField("nextOperationAfterProducedType") == null ? "01all" : operationComponent
                        .getField("nextOperationAfterProducedType"));

        if (TechnologiesConstants.MODEL_TECHNOLOGY_OPERATION_COMPONENT.equals(sourceDD.getName())) {
            calculationOperationComponent.setField(L_TECHNOLOGY_OPERATION_COMPONENT, operationComponent);
        } else if (TechnologiesConstants.MODEL_TECHNOLOGY_INSTANCE_OPERATION_COMPONENT.equals(sourceDD.getName())) {
            calculationOperationComponent.setField(L_TECHNOLOGY_OPERATION_COMPONENT,
                    operationComponent.getBelongsToField(L_TECHNOLOGY_OPERATION_COMPONENT));
        }

        calculationOperationComponent.setField(L_ENTITY_TYPE, L_OPERATION);
        List<Entity> newTechnologyInstanceOperationComponents = new ArrayList<Entity>();

        for (EntityTreeNode child : operationComponent.getChildren()) {
            newTechnologyInstanceOperationComponents.add(createCalculationOperationComponent(child,
                    calculationOperationComponent, calculationOperationComponentDD, costCalculation));
        }

        calculationOperationComponent.setField("children", newTechnologyInstanceOperationComponents);
    }

    private void deleteOperationsTreeIfExists(final Entity costCalculation) {
        Entity yetAnotherCostCalculation = costCalculation.getDataDefinition().get(costCalculation.getId());
        EntityTree existingOperationsTree = yetAnotherCostCalculation.getTreeField(L_CALCULATION_OPERATION_COMPONENTS);

        if (existingOperationsTree == null || existingOperationsTree.getRoot() == null) {
            return;
        }

        debug("existing calculation operation components tree will be removed..");
        EntityTreeNode existingOperationsTreeRoot = existingOperationsTree.getRoot();
        existingOperationsTreeRoot.getDataDefinition().delete(existingOperationsTreeRoot.getId());
    }

    private Map<Entity, Integer> getWorkstationsMapsForOperationsComponent(final Entity costCalculation,
            final Entity productionLine) {
        Entity order = costCalculation.getBelongsToField(L_ORDER);
        if (order == null) {
            return getWorkstationsFromTechnology(costCalculation.getBelongsToField("technology"), productionLine);
        } else {
            return getWorkstationsFromOrder(order, productionLine);
        }
    }

    private Map<Entity, Integer> getWorkstationsFromTechnology(final Entity technology, final Entity productionLine) {
        Map<Entity, Integer> workstations = new HashMap<Entity, Integer>();
        for (Entity operComp : technology.getHasManyField(TechnologyFields.OPERATION_COMPONENTS)) {
            workstations.put(operComp, productionLinesService.getWorkstationTypesCount(operComp, productionLine));
        }
        return workstations;
    }

    private Map<Entity, Integer> getWorkstationsFromOrder(final Entity order, final Entity productionLine) {
        Map<Entity, Integer> workstations = new HashMap<Entity, Integer>();
        for (Entity operComp : order.getHasManyField("technologyInstanceOperationComponents")) {
            workstations.put(operComp.getBelongsToField("technologyOperationComponent"),
                    (Integer) operComp.getField("quantityOfWorkstationTypes"));
        }
        return workstations;
    }

    private BigDecimal getBigDecimal(final Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        // MAKU - using BigDecimal.valueOf(Double) instead of new BigDecimal(String) to prevent issue described at
        // https://forums.oracle.com/forums/thread.jspa?threadID=2251030
        return BigDecimal.valueOf(Double.valueOf(value.toString()));
    }

    private void debug(final String message) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(message);
        }
    }

}
