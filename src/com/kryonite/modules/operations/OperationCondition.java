package com.kryonite.modules.operations;

/**
 * Operation Condition represents a condition that must be met before operation begins execution.
 *
 * @author  Vaibhav Dwivedi
 * @version 1.0
 */
public abstract class OperationCondition {
    protected Operation operation = null;

    /**
     * Return true, if the condition is met.
     * @return
     */
    protected abstract boolean evaluate();

    void setOperation(Operation operation) {
        this.operation = operation;
    }
}
