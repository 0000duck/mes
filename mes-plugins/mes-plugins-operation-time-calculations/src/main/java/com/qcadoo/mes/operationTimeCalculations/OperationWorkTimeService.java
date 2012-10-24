package com.qcadoo.mes.operationTimeCalculations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.qcadoo.model.api.Entity;

public interface OperationWorkTimeService {

    BigDecimal estimateAbstractOperationWorkTime(final Entity operationComponent, final BigDecimal neededNumberOfCycles,
            final boolean includeTpz, final boolean includeAdditionalTime, final Integer workstations);

    OperationWorkTime estimateOperationWorkTime(final Entity operationComponent, final BigDecimal neededNumberOfCycles,
            final boolean includeTpz, final boolean includeAdditionalTime, final Integer workstations, final boolean saved);

    Map<Entity, OperationWorkTime> estimateOperationsWorkTime(final List<Entity> operationComponents,
            Map<Entity, BigDecimal> operationRuns, final boolean includeTpz, final boolean includeAdditionalTime,
            final Map<Entity, Integer> workstations, final boolean saved);

    Map<Entity, OperationWorkTime> estimateOperationsWorkTime(final List<Entity> operationComponents,
            Map<Entity, BigDecimal> operationRuns, final boolean includeTpz, final boolean includeAdditionalTime,
            final Entity productionLine, final boolean saved);

    Map<Entity, OperationWorkTime> estimateOperationsWorkTimeForOrder(final Entity order, Map<Entity, BigDecimal> operationRuns,
            final boolean includeTpz, final boolean includeAdditionalTime, final Entity productionLine, final boolean saved);

    Map<Entity, OperationWorkTime> estimateOperationsWorkTimeForTechnology(final Entity technology,
            Map<Entity, BigDecimal> operationRuns, final boolean includeTpz, final boolean includeAdditionalTime,
            final Entity productionLine, final boolean saved);

    OperationWorkTime estimateTotalWorkTime(final List<Entity> operationComponents, final Map<Entity, BigDecimal> operationRuns,
            final boolean includeTpz, final boolean includeAdditionalTime, final Map<Entity, Integer> workstations,
            final boolean saved);

    OperationWorkTime estimateTotalWorkTime(final List<Entity> operationComponents, final Map<Entity, BigDecimal> operationRuns,
            final boolean includeTpz, final boolean includeAdditionalTime, final Entity productionLine, final boolean saved);

    OperationWorkTime estimateTotalWorkTimeForOrder(final Entity order, final Map<Entity, BigDecimal> operationRuns,
            final boolean includeTpz, final boolean includeAdditionalTime, final Entity productionLine, final boolean saved);

    OperationWorkTime estimateTotalWorkTimeForTechnology(final Entity technology, final Map<Entity, BigDecimal> operationRuns,
            final boolean includeTpz, final boolean includeAdditionalTime, final Entity productionLine, final boolean saved);

}
