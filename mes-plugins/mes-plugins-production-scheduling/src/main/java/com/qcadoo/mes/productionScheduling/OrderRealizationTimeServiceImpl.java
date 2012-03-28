/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.4
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
package com.qcadoo.mes.productionScheduling;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.qcadoo.localization.api.utils.DateUtils;
import com.qcadoo.mes.basic.ShiftsServiceImpl;
import com.qcadoo.mes.technologies.ProductQuantitiesService;
import com.qcadoo.mes.technologies.TechnologyService;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityTreeNode;
import com.qcadoo.model.api.NumberService;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.utils.TimeConverterService;

@Service
public class OrderRealizationTimeServiceImpl implements OrderRealizationTimeService {

    private static final String OPERATION_NODE_ENTITY_TYPE = "operation";

    private static final String REFERENCE_TECHNOLOGY_ENTITY_TYPE = "referenceTechnology";

    private final Map<Entity, BigDecimal> operationRunsField = new HashMap<Entity, BigDecimal>();

    @Autowired
    private ShiftsServiceImpl shiftsService;

    @Autowired
    private ProductQuantitiesService productQuantitiesService;

    @Autowired
    private TechnologyService technologyService;

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private NumberService numberService;

    @Autowired
    private TimeConverterService timeConverterService;

    @Override
    public void changeDateFrom(final ViewDefinitionState viewDefinitionState, final ComponentState state, final String[] args) {
        if (!(state instanceof FieldComponent)) {
            return;
        }
        FieldComponent dateTo = (FieldComponent) state;
        FieldComponent dateFrom = (FieldComponent) viewDefinitionState.getComponentByReference("dateFrom");
        FieldComponent realizationTime = (FieldComponent) viewDefinitionState.getComponentByReference("realizationTime");
        if (StringUtils.hasText((String) dateTo.getFieldValue()) && !StringUtils.hasText((String) dateFrom.getFieldValue())) {
            Date date = shiftsService.findDateFromForOrder(timeConverterService.getDateFromField(dateTo.getFieldValue()),
                    Integer.valueOf((String) realizationTime.getFieldValue()));
            if (date != null) {
                dateFrom.setFieldValue(setDateToField(date));
            }
        }
    }

    @Override
    public void changeDateTo(final ViewDefinitionState viewDefinitionState, final ComponentState state, final String[] args) {
        if (!(state instanceof FieldComponent)) {
            return;
        }
        FieldComponent dateFrom = (FieldComponent) state;
        FieldComponent dateTo = (FieldComponent) viewDefinitionState.getComponentByReference("dateTo");
        FieldComponent realizationTime = (FieldComponent) viewDefinitionState.getComponentByReference("realizationTime");
        if (!StringUtils.hasText((String) dateTo.getFieldValue()) && StringUtils.hasText((String) dateFrom.getFieldValue())) {
            Date date = shiftsService.findDateToForOrder(timeConverterService.getDateFromField(dateFrom.getFieldValue()),
                    Integer.valueOf((String) realizationTime.getFieldValue()));
            if (date != null) {
                dateTo.setFieldValue(setDateToField(date));
            }
        }
    }

    @Override
    public Object setDateToField(final Date date) {
        return new SimpleDateFormat(DateUtils.L_DATE_TIME_FORMAT, Locale.getDefault()).format(date);
    }

    @Override
    @Transactional
    public int estimateRealizationTimeForOperation(final EntityTreeNode operationComponent, final BigDecimal plannedQuantity) {
        return estimateRealizationTimeForOperation(operationComponent, plannedQuantity, true, true);
    }

    @Override
    @Transactional
    public int estimateRealizationTimeForOperation(final EntityTreeNode operationComponent, final BigDecimal plannedQuantity,
            final boolean includeTpz, final boolean includeAdditionalTime) {
        Entity technology = operationComponent.getBelongsToField("technology");

        productQuantitiesService.getProductComponentQuantities(technology, plannedQuantity, operationRunsField);

        return evaluateOperationTime(operationComponent, includeTpz, includeAdditionalTime, operationRunsField);
    }

    @Override
    public Map<Entity, Integer> estimateRealizationTimes(final Entity entity, final BigDecimal plannedQuantity,
            final boolean includeTpz, final boolean includeAdditionalTime) {
        Map<Entity, Integer> operationDurations = new HashMap<Entity, Integer>();

        String entityType = entity.getDataDefinition().getName();
        Entity technology;
        List<Entity> operationComponents;

        if ("technology".equals(entityType)) {
            technology = entity;

            operationComponents = technology.getTreeField("operationComponents");
        } else if ("order".equals(entityType)) {
            technology = entity.getBelongsToField("technology");

            operationComponents = entity.getTreeField("orderOperationComponents");
        } else {
            throw new IllegalStateException("Entity has to be either order or technology");
        }

        productQuantitiesService.getProductComponentQuantities(technology, plannedQuantity, operationRunsField);

        for (Entity operationComponent : operationComponents) {
            if ("order".equals(entityType)) {
                operationComponent = operationComponent.getBelongsToField("technologyOperationComponent");
            }

            evaluateTimesConsideringOperationCanBeReferencedTechnology(operationDurations, operationComponent, includeTpz,
                    includeAdditionalTime, operationRunsField);
        }

        return operationDurations;
    }

    private void evaluateTimesConsideringOperationCanBeReferencedTechnology(final Map<Entity, Integer> operationDurations,
            Entity operationComponent, final boolean includeTpz, final boolean includeAdditionalTime,
            Map<Entity, BigDecimal> operationRuns) {
        if (REFERENCE_TECHNOLOGY_ENTITY_TYPE.equals(operationComponent.getStringField("entityType"))) {
            for (Entity operComp : operationComponent.getBelongsToField("referenceTechnology")
                    .getTreeField("operationComponents")) {
                evaluateTimesConsideringOperationCanBeReferencedTechnology(operationDurations, operComp, includeTpz,
                        includeAdditionalTime, operationRuns);
            }
        } else {
            int duration = evaluateSingleOperationTime(operationComponent, includeTpz, includeAdditionalTime, operationRunsField);
            operationDurations.put(operationComponent, duration);
        }
    }

    private int evaluateOperationTime(final Entity operationComponent, final boolean includeTpz,
            final boolean includeAdditionalTime, final Map<Entity, BigDecimal> operationRuns) {
        String entityType = operationComponent.getStringField("entityType");

        if (REFERENCE_TECHNOLOGY_ENTITY_TYPE.equals(entityType)) {
            EntityTreeNode actualOperationComponent = operationComponent.getBelongsToField("referenceTechnology")
                    .getTreeField("operationComponents").getRoot();

            return evaluateOperationTime(actualOperationComponent, includeTpz, includeAdditionalTime, operationRuns);
        } else if (OPERATION_NODE_ENTITY_TYPE.equals(entityType)) {
            int operationTime = evaluateSingleOperationTime(operationComponent, includeTpz, includeAdditionalTime, operationRuns);
            int pathTime = 0;

            for (Entity child : operationComponent.getHasManyField("children")) {
                int tmpPathTime = evaluateOperationTime(child, includeTpz, includeAdditionalTime, operationRuns);
                if (tmpPathTime > pathTime) {
                    pathTime = tmpPathTime;
                }
            }

            if ("orderOperationComponent".equals(operationComponent.getDataDefinition().getName())) {
                operationComponent.setField("effectiveOperationRealizationTime", operationTime);
                operationComponent.setField("operationOffSet", pathTime);
                operationComponent.getDataDefinition().save(operationComponent);
            }

            return pathTime + operationTime;
        }

        throw new IllegalStateException("entityType has to be either operation or referenceTechnology");
    }

    private int evaluateSingleOperationTime(Entity operationComponent, final boolean includeTpz,
            final boolean includeAdditionalTime, final Map<Entity, BigDecimal> operationRuns) {
        int operationTime = 0;

        operationComponent = operationComponent.getDataDefinition().get(operationComponent.getId());

        BigDecimal productionInOneCycle = (BigDecimal) operationComponent.getField("productionInOneCycle");

        BigDecimal producedInOneRun = technologyService.getProductCountForOperationComponent(operationComponent);

        BigDecimal roundUp = producedInOneRun.divide(productionInOneCycle, numberService.getMathContext()).multiply(
                operationRuns.get(operationComponent), numberService.getMathContext());

        if ("01all".equals(operationComponent.getField("countRealized"))
                || operationComponent.getBelongsToField("parent") == null) {
            operationTime = roundUp.multiply(BigDecimal.valueOf(getIntegerValue(operationComponent.getField("tj"))),
                    numberService.getMathContext()).intValue();
        } else {
            operationTime = (operationComponent.getField("countMachine") == null ? BigDecimal.ZERO
                    : (BigDecimal) operationComponent.getField("countMachine")).multiply(
                    BigDecimal.valueOf(getIntegerValue(operationComponent.getField("tj"))), numberService.getMathContext())
                    .intValue();
        }

        if (includeTpz) {
            operationTime += getIntegerValue(operationComponent.getField("tpz"));
        }

        if (includeAdditionalTime) {
            operationTime += getIntegerValue(operationComponent.getField("timeNextOperation"));
        }

        return operationTime;
    }

    private Integer getIntegerValue(final Object value) {
        return value == null ? Integer.valueOf(0) : (Integer) value;
    }

    @Override
    public BigDecimal getBigDecimalFromField(final Object value, final Locale locale) {
        try {
            DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(locale);
            format.setParseBigDecimal(true);
            return new BigDecimal(format.parse(value.toString()).doubleValue());
        } catch (ParseException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
