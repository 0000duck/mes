package com.qcadoo.mes.productionLines.constants;

import com.qcadoo.model.api.Entity;

public enum FactoryStructureElementType {
    COMPANY("company"), FACTORY("factory"), DIVISION("division"), PRODUCTION_LINE("productionLine"), WORKSTATION("workstation"), SUBASSEMBLY(
            "subassembly");

    private final String elementType;

    private FactoryStructureElementType(final String elementType) {
        this.elementType = elementType;
    }

    public String getStringValue() {
        return elementType;
    }

    public static FactoryStructureElementType of(final Entity factoryStructureElement) {
        return parseString(factoryStructureElement.getStringField(FactoryStructureElementFields.ENTITY_TYPE));
    }

    public static FactoryStructureElementType parseString(final String elementType) {
        if ("company".equals(elementType)) {
            return COMPANY;
        } else if ("factory".equals(elementType)) {
            return FACTORY;
        } else if ("division".equals(elementType)) {
            return DIVISION;
        } else if ("productionLine".equals(elementType)) {
            return PRODUCTION_LINE;
        } else if ("workstation".equals(elementType)) {
            return WORKSTATION;
        } else if ("subassembly".equals(elementType)) {
            return SUBASSEMBLY;
        }
        throw new IllegalStateException("Unsupported FactoryStructureElementType attribute: " + elementType);
    }
}
