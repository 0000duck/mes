package com.qcadoo.mes.costCalculation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.qcadoo.mes.basic.constants.BasicConstants.MODEL_PRODUCT;
import static com.qcadoo.mes.orders.constants.OrdersConstants.MODEL_ORDER;
import static com.qcadoo.mes.technologies.constants.TechnologiesConstants.MODEL_TECHNOLOGY;
import static com.qcadoo.view.api.ComponentState.MessageType.FAILURE;
import static com.qcadoo.view.api.ComponentState.MessageType.SUCCESS;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.mes.basic.constants.BasicConstants;
import com.qcadoo.mes.basic.util.CurrencyService;
import com.qcadoo.mes.costCalculation.constants.CostCalculateConstants;
import com.qcadoo.mes.costCalculation.print.CostCalculationReportService;
import com.qcadoo.mes.orders.constants.OrdersConstants;
import com.qcadoo.mes.technologies.constants.TechnologiesConstants;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.SearchCriteriaBuilder;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.utils.NumberGeneratorService;

@Service
public class CostCalculationViewService {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private CostCalculationService costCalculationService;

    @Autowired
    private CostCalculationReportService costCalculationReportService;

    @Autowired
    private NumberGeneratorService numberGeneratorService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private TranslationService translationService;

    private final static String EMPTY = "";

    public void showCostCalculateFromOrder(final ViewDefinitionState viewDefinitionState, final ComponentState state,
            final String[] args) {
        Long orderId = (Long) state.getFieldValue();

        if (orderId != null) {
            String url = "../page/costCalculation/costCalculationDetails.html?context={\"orderId\":\"" + orderId + "\"}";
            viewDefinitionState.redirectTo(url, false, true);
        }
    }

    public void showCostCalculateFromTechnology(final ViewDefinitionState viewDefinitionState, final ComponentState state,
            final String[] args) {
        Long technologyId = (Long) state.getFieldValue();

        if (technologyId != null) {
            String url = "../page/costCalculation/costCalculationDetails.html?context={\"technologyId\":\"" + technologyId
                    + "\"}";
            viewDefinitionState.redirectTo(url, false, true);
        }
    }

    public void copyFieldValues(final ViewDefinitionState viewDefinitionState, final ComponentState componentState,
            final String[] args) {
        if (args.length < 2) {
            return;
        }
        String sourceType = args[0];
        Long sourceId = Long.valueOf(args[1]);
        Boolean cameFromOrder = "order".equals(sourceType);
        Boolean cameFromTechnology = "technology".equals(sourceType);
        Entity technology;
        Entity order;

        if (!cameFromOrder && !cameFromTechnology) {
            return;
        }
        if (cameFromOrder) {
            order = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(sourceId);
            technology = order.getBelongsToField("technology");
        } else {
            order = null;
            technology = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                    TechnologiesConstants.MODEL_TECHNOLOGY).get(sourceId);
        }

        applyValuesToFields(viewDefinitionState, technology, order);
    }

    private void applyValuesToFields(final ViewDefinitionState viewDefinitionState, final Entity technology, final Entity order) {
        Boolean cameFromOrder = false;
        Boolean cameFromTechnology = false;
        Set<String> referenceNames = new HashSet<String>(Arrays.asList("defaultTechnology", "product", "order", "quantity",
                "technology"));
        Map<String, FieldComponent> componentsMap = new HashMap<String, FieldComponent>();
        for (String referenceName : referenceNames) {
            FieldComponent fieldComponent = (FieldComponent) viewDefinitionState.getComponentByReference(referenceName);
            componentsMap.put(referenceName, fieldComponent);
        }

        if (order != null) {
            cameFromOrder = true;
        } else {
            cameFromTechnology = true;
        }

        if (cameFromOrder) {
            componentsMap.get("order").setFieldValue(order.getId());
            componentsMap.get("defaultTechnology").setEnabled(false);
            componentsMap.get("quantity").setFieldValue(order.getField("plannedQuantity"));
        } else {
            componentsMap.get("order").setFieldValue(EMPTY);
            componentsMap.get("defaultTechnology").setEnabled(false);
            componentsMap.get("quantity").setFieldValue(technology.getField("minimalQuantity"));
        }
        componentsMap.get("order").setEnabled(cameFromOrder);
        componentsMap.get("technology").setFieldValue(technology.getId());
        componentsMap.get("technology").setEnabled(cameFromTechnology);
        componentsMap.get("defaultTechnology").setFieldValue(technology.getId());

        componentsMap.get("quantity").setEnabled(!cameFromOrder);
        componentsMap.get("product").setFieldValue(technology.getBelongsToField("product").getId());
        componentsMap.get("product").setEnabled(false);
    }

    private void generateNumber(final ViewDefinitionState viewDefinitionState) {
        numberGeneratorService.generateAndInsertNumber(viewDefinitionState, CostCalculateConstants.PLUGIN_IDENTIFIER,
                CostCalculateConstants.MODEL_COST_CALCULATION, "form", "number");
    }

    public void generateDateOfCalculation(final DataDefinition dataDefinition, final Entity entity) {
        entity.setField("dateOfCalculation", new Date());
    }

    public void fillCurrencyFields(final ViewDefinitionState viewDefinitionState) {
        final String currencyAlphabeticCode = currencyService.getCurrencyAlphabeticCode();
        generateNumber(viewDefinitionState);
        Set<String> fields = Sets.newHashSet("totalCostsCurrency", "totalOverheadCurrency", "additionalOverheadValueCurrency",
                "materialCostMarginValueCurrency", "productionCostMarginValueCurrency", "totalTechnicalProductionCostsCurrency",
                "totalPieceworkCostsCurrency", "totalLaborHourlyCostsCurrency", "totalMachineHourlyCostsCurrency",
                "totalMaterialCostsCurrency", "additionalOverheadCurrency");

        for (String componentReference : fields) {
            FieldComponent field = (FieldComponent) viewDefinitionState.getComponentByReference(componentReference);
            field.setEnabled(true);
            field.setFieldValue(currencyAlphabeticCode);
            field.setEnabled(false);
            field.requestComponentUpdateState();
        }

        FieldComponent productionCostMarginProc = (FieldComponent) viewDefinitionState
                .getComponentByReference("productionCostMarginProc");
        productionCostMarginProc.setFieldValue("%");
        FieldComponent materialCostMarginProc = (FieldComponent) viewDefinitionState
                .getComponentByReference("materialCostMarginProc");
        materialCostMarginProc.setFieldValue("%");

        fillCostPerUnitUnitField(viewDefinitionState, null, null);
    }

    public void fillCostPerUnitUnitField(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        final String currencyAlphabeticCode = currencyService.getCurrencyAlphabeticCode();
        if (view.getComponentByReference("product").getFieldValue() == null) {
            return;
        }

        Long productId = (Long) view.getComponentByReference("product").getFieldValue();
        Entity product = dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, MODEL_PRODUCT).get(productId);

        FieldComponent totalCostsPerUnitUNIT = (FieldComponent) view.getComponentByReference("totalCostsPerUnitUNIT");
        totalCostsPerUnitUNIT.setEnabled(true);
        totalCostsPerUnitUNIT.setFieldValue(currencyAlphabeticCode + " / " + product.getStringField("unit"));
        totalCostsPerUnitUNIT.setEnabled(false);
        totalCostsPerUnitUNIT.requestComponentUpdateState();
    }

    public void fillFieldWhenTechnologyChanged(final ViewDefinitionState viewDefinitionState, final ComponentState state,
            final String[] args) {
        if (!(state instanceof FieldComponent)) {
            return;
        }
        FieldComponent technologyLookup = (FieldComponent) viewDefinitionState.getComponentByReference("technology");
        if (technologyLookup.getFieldValue() == null) {
            return;
        }

        Entity technology = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                TechnologiesConstants.MODEL_TECHNOLOGY).get((Long) technologyLookup.getFieldValue());
        if (technology != null) {
            applyValuesToFields(viewDefinitionState, technology, null);
        }

        // fillCostPerUnitUnitField(viewDefinitionState);
    }

    public void fillFieldWhenOrderChanged(final ViewDefinitionState viewDefinitionState, final ComponentState state,
            final String[] args) {
        if (!(state instanceof FieldComponent)) {
            return;
        }
        FieldComponent orderLookup = (FieldComponent) viewDefinitionState.getComponentByReference("order");
        if (orderLookup.getFieldValue() == null) {
            return;
        }
        Entity order = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_ORDER).get(
                (Long) orderLookup.getFieldValue());

        if (order == null) {
            return;
        }
        Entity technology = order.getBelongsToField("technology");
        applyValuesToFields(viewDefinitionState, technology, order);
    }

    public void setFieldEnable(final ViewDefinitionState viewDefinitionState, final ComponentState state, final String[] args) {
        FieldComponent technologyLookup = (FieldComponent) viewDefinitionState.getComponentByReference("technology");
        FieldComponent orderLookup = (FieldComponent) viewDefinitionState.getComponentByReference("order");

        FieldComponent product = (FieldComponent) viewDefinitionState.getComponentByReference("product");
        FieldComponent quantity = (FieldComponent) viewDefinitionState.getComponentByReference("quantity");

        if (technologyLookup.getFieldValue() == null) {
            product.setEnabled(true);
            quantity.setEnabled(true);
            orderLookup.setEnabled(true);
        } else {
            if (orderLookup.getFieldValue() == null) {
                technologyLookup.setEnabled(true);
                product.setEnabled(true);
                quantity.setEnabled(true);
            }
        }
    }

    /* FUNCTIONS FOR FIRE CALCULATION AND HANDLING RESULTS BELOW */

    /* Event handler, fire total calculation */
    public void generateCostCalculation(ViewDefinitionState view, ComponentState componentState, String[] args) {
        Entity costCalculation = getEntityFromForm(view);
        if (costCalculation.getId() == null) {
            view.getComponentByReference("form")
                    .addMessage(
                            translationService.translate("costCalculation.messages.failure.calculationOnUnsavedEntity",
                                    view.getLocale()), FAILURE);
            return;
        }
        attachBelongsToFields(costCalculation);
        // Fire cost calculation algorithm
        costCalculation = costCalculationService.calculateTotalCost(costCalculation);
        fillFields(view, costCalculation);
        costCalculationReportService.generateCostCalculationReport(view, componentState, args);
        view.getComponentByReference("form").addMessage(
                translationService.translate("costCalculation.messages.success.calculationComplete", view.getLocale()), SUCCESS);
    }

    private void attachBelongsToFields(final Entity costCalculation) {
        final Map<String, DataDefinition> belongsToFieldDDs = Maps.newHashMap();
        belongsToFieldDDs.put("order", dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, MODEL_ORDER));
        belongsToFieldDDs.put("technology", dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER, MODEL_TECHNOLOGY));
        belongsToFieldDDs.put("defaultTechnology",
                dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER, MODEL_TECHNOLOGY));
        belongsToFieldDDs.put("product", dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, MODEL_PRODUCT));

        Entity fieldEntity;
        Object fieldValue;

        for (Map.Entry<String, DataDefinition> belongsToFieldDD : belongsToFieldDDs.entrySet()) {
            fieldValue = costCalculation.getField(belongsToFieldDD.getKey());
            if (fieldValue == null || !(fieldValue instanceof Long)) {
                continue;
            }
            fieldEntity = belongsToFieldDD.getValue().get((Long) fieldValue);
            costCalculation.setField(belongsToFieldDD.getKey(), fieldEntity);
        }
    }

    // get values from form fields
    private Entity getEntityFromForm(final ViewDefinitionState view) {
        FormComponent form = (FormComponent) view.getComponentByReference("form");
        checkArgument(form != null, "form is null");
        checkArgument(form.isValid(), "invalid form");
        return form.getEntity();
    }

    private String getStringValueFromBigDecimal(final Object value) {
        // FIXME MAKU check locales (dots or commas?)
        if (value == null) {
            return "0.000";
        }
        return getBigDecimal(value).setScale(3, BigDecimal.ROUND_UP).toString();
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

    // put result values into proper form fields
    private void fillFields(final ViewDefinitionState view, final Entity costCalculation) {
        final Set<String> outputDecimalFields = Sets.newHashSet("productionCostMarginValue", "materialCostMarginValue",
                "totalOverhead", "totalMaterialCosts", "totalMachineHourlyCosts", "totalLaborHourlyCosts", "totalPieceworkCosts",
                "totalTechnicalProductionCosts", "totalCosts", "totalCostsPerUnit");

        for (String referenceName : outputDecimalFields) {
            FieldComponent fieldComponent = (FieldComponent) view.getComponentByReference(referenceName);
            fieldComponent.setFieldValue(getStringValueFromBigDecimal(costCalculation.getField(referenceName)));
        }
    }

    public void changeOrderProduct(final ViewDefinitionState viewDefinitionState, final ComponentState state, final String[] args) {
        if (!(state instanceof FieldComponent)) {
            return;
        }
        FieldComponent product = (FieldComponent) state;
        FieldComponent technology = (FieldComponent) viewDefinitionState.getComponentByReference("technology");
        FieldComponent defaultTechnology = (FieldComponent) viewDefinitionState.getComponentByReference("defaultTechnology");

        if (viewDefinitionState.getComponentByReference("product").getFieldValue() != null) {
            fillCostPerUnitUnitField(viewDefinitionState, state, args);
        }
        defaultTechnology.setFieldValue("");
        technology.setFieldValue(null);

        if (product.getFieldValue() != null) {
            Entity defaultTechnologyEntity = getDefaultTechnology((Long) product.getFieldValue());

            if (defaultTechnologyEntity != null) {
                technology.setFieldValue(defaultTechnologyEntity.getId());
                defaultTechnology.setFieldValue(defaultTechnologyEntity.getId());
            }
        }
    }

    private Entity getDefaultTechnology(final Long selectedProductId) {
        DataDefinition technologyDD = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER, MODEL_TECHNOLOGY);

        SearchCriteriaBuilder searchCriteria = technologyDD.find().add(SearchRestrictions.eq("master", true))
                .add(SearchRestrictions.belongsTo("product", BasicConstants.PLUGIN_IDENTIFIER, MODEL_PRODUCT, selectedProductId));

        return searchCriteria.uniqueResult();
    }
}
