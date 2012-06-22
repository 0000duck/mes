package com.qcadoo.mes.states.aop;

import static com.qcadoo.mes.states.constants.StateChangeStatus.SUCCESSFUL;
import static com.qcadoo.mes.states.messages.constants.StateMessageType.FAILURE;

import java.util.Date;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

import com.qcadoo.mes.states.StateChangeContext;
import com.qcadoo.mes.states.StateChangeEntityDescriber;
import com.qcadoo.mes.states.StateEnum;
import com.qcadoo.mes.states.constants.StateChangeStatus;
import com.qcadoo.mes.states.exception.StateChangeException;
import com.qcadoo.mes.states.exception.StateTransitionNotAlloweException;
import com.qcadoo.mes.states.messages.constants.StateMessageType;
import com.qcadoo.mes.states.service.StateChangePhaseUtil;
import com.qcadoo.mes.states.service.StateChangeService;
import com.qcadoo.model.api.Entity;

/**
 * Abstract service for changing entity state which provides default implementation.
 * 
 * @since 1.1.7
 */
@Aspect
@Configurable
@DeclarePrecedence("com.qcadoo.mes.states.aop.StateChangePhaseAspect, com.qcadoo.mes.states.aop.RunInPhaseAspect")
public abstract class AbstractStateChangeAspect implements StateChangeService {

    protected static final int DEFAULT_NUM_OF_PHASES = 2;

    @Override
    @Transactional
    public void changeState(final StateChangeContext stateChangeContext) {
        stateChangeContext.save();
        final StateChangeEntityDescriber describer = stateChangeContext.getDescriber();
        try {
            describer.checkFields();
            for (int phase = stateChangeContext.getPhase() + 1; phase <= getNumOfPhases(); phase++) {
                if (StateChangePhaseUtil.canRun(stateChangeContext)) {
                    stateChangeContext.setPhase(phase);
                    changeStatePhase(stateChangeContext, phase);
                }
            }
            final Entity owner = stateChangeContext.getOwner();
            stateChangeContext.setOwner(owner);
            performChangeEntityState(stateChangeContext);
        } catch (Exception e) {
            stateChangeContext.setStatus(StateChangeStatus.FAILURE);
            stateChangeContext.addMessage("states.messages.change.failure.internalServerError", StateMessageType.FAILURE);
            stateChangeContext.save();
            throw new StateChangeException(e);
        }
    }

    /**
     * Get number of state change phases. Default is 1.
     * 
     * @return number of phases
     */
    protected int getNumOfPhases() {
        return DEFAULT_NUM_OF_PHASES;
    }

    /**
     * Single state change phase join point.
     * 
     * @param stateChangeEntity
     * @param phaseNumber
     */
    protected abstract void changeStatePhase(final StateChangeContext stateChangeContext, final int phaseNumber);

    @Transactional
    protected void performChangeEntityState(final StateChangeContext stateChangeContext) {
        if (!StateChangePhaseUtil.canRun(stateChangeContext)) {
            return;
        }
        final StateChangeEntityDescriber describer = stateChangeContext.getDescriber();
        final Entity owner = stateChangeContext.getOwner();
        final StateEnum sourceState = stateChangeContext.getStateEnumValue(describer.getSourceStateFieldName());
        final StateEnum targetState = stateChangeContext.getStateEnumValue(describer.getTargetStateFieldName());

        if (sourceState != null && !sourceState.canChangeTo(targetState)) {
            throw new StateTransitionNotAlloweException(sourceState, targetState);
        }

        boolean ownerIsValid = owner.isValid();
        if (ownerIsValid) {
            owner.setField(describer.getOwnerStateFieldName(), targetState.getStringValue());
            ownerIsValid = owner.getDataDefinition().save(owner).isValid();
        }

        if (ownerIsValid) {
            markAsSuccessful(stateChangeContext);
        } else {
            markAsInvalid(stateChangeContext);
        }

        stateChangeContext.save();
    }

    private void markAsSuccessful(final StateChangeContext stateChangeContext) {
        final StateChangeEntityDescriber describer = stateChangeContext.getDescriber();
        stateChangeContext.setStatus(SUCCESSFUL);
        stateChangeContext.setField(describer.getDateTimeFieldName(), new Date());
    }

    private void markAsInvalid(final StateChangeContext stateChangeContext) {
        stateChangeContext.setStatus(StateChangeStatus.FAILURE);
        stateChangeContext.addMessage("states.messages.change.failure.invalidEntity.", FAILURE);
    }

}
