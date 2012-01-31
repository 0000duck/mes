/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.1.2
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
import com.qcadoo.mes.technologies.constants.TechnologiesConstants;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityTreeNode;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;

@Service
public class OrderRealizationTimeServiceImpl implements OrderRealizationTimeService {

    private static final String OPERATION_NODE_ENTITY_TYPE = "operation";

    private static final String REFERENCE_TECHNOLOGY_ENTITY_TYPE = "referenceTechnology";

    private Map<Entity, BigDecimal> operationRunsField = new HashMap<Entity, BigDecimal>();

    @Autowired
    private ShiftsServiceImpl shiftsService;

    @Autowired
    private ProductQuantitiesService productQuantitiesService;

    @Autowired
    private TechnologyService technologyService;

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Override
    public void changeDateFrom(final ViewDefinitionState viewDefinitionState, final ComponentState state, final String[] args) {
        if (!(state instanceof FieldComponent)) {
            return;
        }
        FieldComponent dateTo = (FieldComponent) state;
        FieldComponent dateFrom = (FieldComponent) viewDefinitionState.getComponentByReference("dateFrom");
        FieldComponent realizationTime = (FieldComponent) viewDefinitionState.getComponentByReference("realizationTime");
        if (StringUtils.hasText((String) dateTo.getFieldValue()) && !StringUtils.hasText((String) dateFrom.getFieldValue())) {
            Date date = shiftsService.findDateFromForOrder(getDateFromField(dateTo.getFieldValue()),
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
            Date date = shiftsService.findDateToForOrder(getDateFromField(dateFrom.getFieldValue()),
                    Integer.valueOf((String) realizationTime.getFieldValue()));
            if (date != null) {
                dateTo.setFieldValue(setDateToField(date));
            }
        }
    }

    @Override
    public Object setDateToField(final Date date) {
        return new SimpleDateFormat(DateUtils.DATE_TIME_FORMAT, Locale.getDefault()).format(date);
    }

    @Override
    public Date getDateFromField(final Object value) {
        try {
            return new SimpleDateFormat(DateUtils.DATE_TIME_FORMAT, Locale.getDefault()).parse((String) value);
        } catch (ParseException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int estimateRealizationTimeForOperation(final EntityTreeNode operationComponent, final BigDecimal plannedQuantity) {
        return estimateRealizationTimeForOperation(operationComponent, plannedQuantity, true);
    }

    /**
     * 
     * @param operationComponent
     *            operationComponent of an operation we want to estimate. Can be either technologyOperationComponent or
     *            orderOperationComponent
     * @param plannedQuantity
     *            How many products we want this operation to produce
     * @param includeTpz
     *            Flag indicating if we want to include Tpz
     * @return Duration of an operation in seconds.
     */
    @Override
    @Transactional
    public int estimateRealizationTimeForOperation(final EntityTreeNode operationComponent, final BigDecimal plannedQuantity,
            final Boolean includeTpz) {
        Entity technology = operationComponent.getBelongsToField("technology");

        productQuantitiesService.getProductComponentQuantities(technology, plannedQuantity, operationRunsField);

        return evaluateOperationTime(operationComponent, includeTpz, operationRunsField);
    }

    /**
     * 
     * @param entity
     *            An order or a technology for which we want to estimate operation times.
     * @param plannedQuantity
     *            How many products we want this order/technology to produce
     * @param includeTpz
     *            Flag indicating if we want to include Tpz
     * @return Map where keys are operationComponents and values are corresponding operation durations
     */
    public Map<Entity, Integer> estimateRealizationTimes(Entity entity, BigDecimal plannedQuantity, boolean includeTpz) {
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
            int duration = evaluateOperationTime(operationComponent, includeTpz, operationRunsField);
            operationDurations.put(operationComponent, duration);
        }

        return operationDurations;
    }

    private int evaluateOperationTime(final Entity operationComponent, final Boolean includeTpz,
            final Map<Entity, BigDecimal> operationRuns) {
        String entityType = operationComponent.getStringField("entityType");

        if (REFERENCE_TECHNOLOGY_ENTITY_TYPE.equals(entityType)) {
            EntityTreeNode actualOperationComponent = operationComponent.getBelongsToField("referenceTechnology")
                    .getTreeField("operationComponents").getRoot();

            return evaluateOperationTime(actualOperationComponent, includeTpz, operationRuns);
        } else if (OPERATION_NODE_ENTITY_TYPE.equals(entityType)) {
            Entity technologyOperationComponent = operationComponent;

            if ("orderOperationComponent".equals(operationComponent.getDataDefinition().getName())) {
                long techOperationId = operationComponent.getBelongsToField("technologyOperationComponent").getId();
                technologyOperationComponent = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                        TechnologiesConstants.MODEL_TECHNOLOGY_OPERATION_COMPONENT).get(techOperationId);
            }

            int operationTime = 0;
            int pathTime = 0;

            for (Entity child : operationComponent.getHasManyField("children")) {
                int tmpPathTime = evaluateOperationTime(child, includeTpz, operationRuns);
                if (tmpPathTime > pathTime) {
                    pathTime = tmpPathTime;
                }
            }

            BigDecimal productionInOneCycle = (BigDecimal) operationComponent.getField("productionInOneCycle");

            BigDecimal producedInOneRun = technologyService.getProductCountForOperationComponent(technologyOperationComponent);

            BigDecimal roundUp = producedInOneRun.divide(productionInOneCycle, BigDecimal.ROUND_UP).multiply(
                    operationRuns.get(technologyOperationComponent));

            if ("01all".equals(operationComponent.getField("countRealized"))
                    || operationComponent.getBelongsToField("parent") == null) {
                operationTime = (roundUp.multiply(BigDecimal.valueOf(getIntegerValue(operationComponent.getField("tj")))))
                        .intValue();
            } else {
                operationTime = ((operationComponent.getField("countMachine") == null ? BigDecimal.ZERO
                        : (BigDecimal) operationComponent.getField("countMachine")).multiply(BigDecimal
                        .valueOf(getIntegerValue(operationComponent.getField("tj"))))).intValue();
            }

            if (includeTpz) {
                operationTime += getIntegerValue(operationComponent.getField("tpz"));
            }

            if ("orderOperationComponent".equals(operationComponent.getDataDefinition().getName())) {
                operationComponent.setField("effectiveOperationRealizationTime", operationTime);
                operationComponent.setField("operationOffSet", pathTime);
                operationComponent.getDataDefinition().save(operationComponent);
            }

            pathTime += operationTime + getIntegerValue(operationComponent.getField("timeNextOperation"));
            return pathTime;
        }

        throw new IllegalStateException("entityType has to be either operation or referenceTechnology");
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
