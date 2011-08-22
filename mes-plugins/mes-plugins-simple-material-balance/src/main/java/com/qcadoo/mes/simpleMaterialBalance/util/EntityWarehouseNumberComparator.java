package com.qcadoo.mes.simpleMaterialBalance.util;

import java.io.Serializable;
import java.util.Comparator;

import com.qcadoo.model.api.Entity;

public class EntityWarehouseNumberComparator implements Comparator<Entity>, Serializable {

    private static final long serialVersionUID = 1784214465897023057L;

    @Override
    public final int compare(final Entity o1, final Entity o2) {
        return o1.getBelongsToField("warehouse").getStringField("number")
                .compareTo(o2.getBelongsToField("warehouse").getStringField("number"));
    }
}
