package com.qcadoo.mes.technologies.grouping;

import java.math.BigDecimal;
import java.util.List;

import com.qcadoo.model.api.Entity;

public interface OperationMergeService {

    void mergeProductIn(Entity existingOperationComponent, Entity operationProductIn, BigDecimal quantity);

    void storeProductIn(Entity operationComponent, Entity mergeOperationComponent, Entity operationProductIn, BigDecimal quantity);

    public void mergeProductOut(Entity existingOperationComponent, Entity operationProductOut, BigDecimal quantity);

    void storeProductOut(Entity operationComponent, Entity mergeOperationComponent, Entity operationProductIn, BigDecimal quantity);

    List<Long> findMergedToOperationComponentIds();

    Entity findMergedByOperationComponent(Entity operationComponent);

    Entity findMergedFromOperationInByOperationComponentId(Long operationComponentId);

    Entity findMergedFromOperationOutByOperationComponentId(Long operationComponentId);

    List<Entity> findMergedToByOperationComponentId(Long operationComponentId);

    List<Entity> findMergedProductInComponentsByOperationComponent(Entity operationComponent);

    List<Entity> findMergedToProductOutComponentsByOperationComponent(Entity operationComponent);

    List<Entity> findMergedEntitiesByOperationComponent(Entity operationComponent);

    List<Entity> findMergedEntitiesByOperationComponentId(Long operationComponentId);

    void adjustOperationProductComponentsDueMerge(Entity operationComponent);
}
