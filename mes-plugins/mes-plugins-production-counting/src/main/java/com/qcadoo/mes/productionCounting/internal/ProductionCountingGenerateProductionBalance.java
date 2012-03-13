package com.qcadoo.mes.productionCounting.internal;

import java.util.Observable;

import org.springframework.stereotype.Service;

import com.qcadoo.model.api.Entity;

@Service
public class ProductionCountingGenerateProductionBalance extends Observable {

    public void notifyObserversThatTheBalanceIsBeingGenerated(final Entity entity) {
        setChanged();
        notifyObservers(entity);
    }
}
