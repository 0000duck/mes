package com.qcadoo.mes.genealogies;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.basic.BasicConstants;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.search.Restrictions;
import com.qcadoo.model.api.search.SearchCriteriaBuilder;
import com.qcadoo.model.api.search.SearchResult;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;

@Service
public class GenealogyTechnologyService {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public void checkBatchNrReq(final ViewDefinitionState viewDefinitionState, final ComponentState state, final String[] args) {
        if (!(state instanceof FieldComponent)) {
            throw new IllegalStateException("component is not lookup");
        }

        FieldComponent product = (FieldComponent) state;

        FieldComponent batchReq = (FieldComponent) viewDefinitionState.getComponentByReference("batchRequired");

        if (product.getFieldValue() != null) {
            if (batchRequired((Long) product.getFieldValue())) {
                batchReq.setFieldValue("1");
            } else {
                batchReq.setFieldValue("0");
            }
        }
    }

    public void checkAttributesReq(final ViewDefinitionState viewDefinitionState) {

        FormComponent form = (FormComponent) viewDefinitionState.getComponentByReference("form");

        if (form.getEntityId() != null) {
            // form is already saved
            return;
        }

        SearchResult searchResult = dataDefinitionService
                .get(GenealogiesConstants.PLUGIN_IDENTIFIER, GenealogiesConstants.MODEL_CURRENT_ATTRIBUTE).find()
                .setMaxResults(1).list();
        Entity currentAttribute = null;

        if (searchResult.getEntities().size() > 0) {
            currentAttribute = searchResult.getEntities().get(0);
        }

        if (currentAttribute != null) {

            Boolean shiftReq = (Boolean) currentAttribute.getField("shiftReq");
            if (shiftReq != null && shiftReq) {
                FieldComponent req = (FieldComponent) viewDefinitionState.getComponentByReference("shiftFeatureRequired");
                req.setFieldValue("1");
            }

            Boolean postReq = (Boolean) currentAttribute.getField("postReq");
            if (postReq != null && postReq) {
                FieldComponent req = (FieldComponent) viewDefinitionState.getComponentByReference("postFeatureRequired");
                req.setFieldValue("1");
            }

            Boolean otherReq = (Boolean) currentAttribute.getField("otherReq");
            if (otherReq != null && otherReq) {
                FieldComponent req = (FieldComponent) viewDefinitionState.getComponentByReference("otherFeatureRequired");
                req.setFieldValue("1");
            }
        }

    }

    private boolean batchRequired(final Long selectedProductId) {
        Entity product = getProductById(selectedProductId);
        if (product != null) {
            return product.getField("genealogyBatchReq") != null ? (Boolean) product.getField("genealogyBatchReq") : false;
        } else {
            return false;
        }
    }

    private Entity getProductById(final Long productId) {
        DataDefinition instructionDD = dataDefinitionService.get(BasicConstants.PLUGIN_IDENTIFIER, BasicConstants.MODEL_PRODUCT);

        SearchCriteriaBuilder searchCriteria = instructionDD.find().setMaxResults(1).addRestriction(Restrictions.idEq(productId));

        SearchResult searchResult = searchCriteria.list();
        if (searchResult.getTotalNumberOfEntities() == 1) {
            return searchResult.getEntities().get(0);
        }
        return null;
    }
}
