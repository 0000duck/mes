package com.qcadoo.mes.techSubcontracting.hooks;

import org.springframework.stereotype.Service;

import com.qcadoo.mes.technologies.constants.TechnologyOperationComponentFields;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;

@Service
public class TechnologyOperationHooksTS {

    private static final String L_IS_SUBCONTRACTING = "isSubcontracting";

    public void copySubstractingFieldFromLowerInstance(final DataDefinition dataDefinition, final Entity entity) {
        Entity operation = entity.getBelongsToField(TechnologyOperationComponentFields.OPERATION);
        entity.setField(L_IS_SUBCONTRACTING, operation.getBooleanField(L_IS_SUBCONTRACTING));
    }
}
