package com.qcadoo.mes.productionCounting.hooks;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.basic.constants.ProductFields;
import com.qcadoo.mes.basic.constants.UnitConversionItemFieldsB;
import com.qcadoo.mes.productionCounting.constants.AnomalyExplanationFields;
import com.qcadoo.mes.productionCounting.constants.AnomalyFields;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.NumberService;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.model.api.units.PossibleUnitConversions;
import com.qcadoo.model.api.units.UnitConversionService;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.CheckBoxComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.LookupComponent;

@Service
public class AnomalyExplanationDetailsHooks {

    @Autowired
    private UnitConversionService unitConversionService;

    @Autowired
    private NumberService numberService;

    public void onBeforeRender(final ViewDefinitionState view) {
        FormComponent form = (FormComponent) view.getComponentByReference("form");
        Entity entity = form.getEntity();

        if (view.isViewAfterRedirect()) {
            initializeFormValues(view, entity);
        }

        boolean useWaste = ((CheckBoxComponent) view.getComponentByReference("useWaste")).isChecked();
        LookupComponent productLookup = ((LookupComponent) view.getComponentByReference("product"));
        productLookup.setEnabled(!useWaste);
        view.getComponentByReference("location").setEnabled(!useWaste);

        ComponentState givenUnitComponent = view.getComponentByReference("givenUnit");

        String givenUnit = (String) givenUnitComponent.getFieldValue();
        Entity selectedProduct = productLookup.getEntity();

        boolean shouldAdditionalUnitBeEnabled = true;
        if (selectedProduct != null) {
            String selectedProductAdditionalUnit = selectedProduct.getStringField(ProductFields.ADDITIONAL_UNIT);

            if (isNotBlank(selectedProductAdditionalUnit) && isNotBlank(givenUnit)
                    && selectedProductAdditionalUnit.equals(givenUnit)) {
                shouldAdditionalUnitBeEnabled = false;
            }
        }
        givenUnitComponent.setEnabled(shouldAdditionalUnitBeEnabled);
    }

    private void initializeFormValues(ViewDefinitionState view, Entity entity) {

        LookupComponent productLookup = ((LookupComponent) view.getComponentByReference("product"));
        if (entity.getId() == null) {

            Entity anomaly = entity.getBelongsToField(AnomalyExplanationFields.ANOMALY);
            Entity anomalyProduct = anomaly.getBelongsToField(AnomalyFields.PRODUCT);

            productLookup.setFieldValue(anomalyProduct.getId());

            String selectedProductUnit = anomalyProduct.getStringField(ProductFields.UNIT);
            String additionalSelectedProductUnit = anomalyProduct.getStringField(ProductFields.ADDITIONAL_UNIT);
            ComponentState givenUnitComponent = view.getComponentByReference("givenUnit");
            if (isNotBlank(additionalSelectedProductUnit)) {
                givenUnitComponent.setFieldValue(additionalSelectedProductUnit);
            } else {
                givenUnitComponent.setFieldValue(selectedProductUnit);
            }

            BigDecimal anomalyUsedQuantity = anomaly.getDecimalField(AnomalyFields.USED_QUANTITY);

            view.getComponentByReference("usedQuantity")
                    .setFieldValue(numberService.formatWithMinimumFractionDigits(anomalyUsedQuantity, 0));

            String anomalyProductUnit = anomalyProduct.getStringField(ProductFields.UNIT);
            String additionalAnomalyProductUnit = anomalyProduct.getStringField(ProductFields.ADDITIONAL_UNIT);

            ComponentState givenQuantityComponent = view.getComponentByReference("givenQuantity");
            if (isNotBlank(additionalAnomalyProductUnit)) {
                PossibleUnitConversions unitConversions = unitConversionService.getPossibleConversions(anomalyProductUnit,
                        searchCriteriaBuilder -> searchCriteriaBuilder
                                .add(SearchRestrictions.belongsTo(UnitConversionItemFieldsB.PRODUCT, anomalyProduct)));
                if (unitConversions.isDefinedFor(additionalAnomalyProductUnit)) {
                    BigDecimal convertedQuantityNewValue = unitConversions.convertTo(anomalyUsedQuantity,
                            additionalAnomalyProductUnit);
                    givenQuantityComponent
                            .setFieldValue(numberService.formatWithMinimumFractionDigits(convertedQuantityNewValue, 0));
                }
            } else {
                givenQuantityComponent.setFieldValue(numberService.formatWithMinimumFractionDigits(anomalyUsedQuantity, 0));
            }

            Entity anomalyLocation = anomaly.getBelongsToField(AnomalyFields.LOCATION);
            view.getComponentByReference("location").setFieldValue(ofNullable(anomalyLocation).map(Entity::getId).orElse(null));
        }

        Entity selectedProduct = productLookup.getEntity();
        ComponentState productUnit = view.getComponentByReference("productUnit");
        ComponentState usedQuantity = view.getComponentByReference("usedQuantity");
        if (selectedProduct != null) {
            productUnit.setFieldValue(selectedProduct.getStringField(ProductFields.UNIT));
            usedQuantity.setEnabled(true);
        } else {
            if (((CheckBoxComponent) view.getComponentByReference("useWaste")).isChecked()) {
                productUnit.setFieldValue(entity.getStringField(AnomalyExplanationFields.GIVEN_UNIT));
            } else {
                productUnit.setFieldValue(null);
            }
            usedQuantity.setEnabled(false);
        }
    }

}
