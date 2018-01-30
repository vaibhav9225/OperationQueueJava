package com.kryonite.modules.operations;

/**
 * Operation Observer is a callback listener for operation lifecycle events.
 * It can also take actions on the operation like cancel, abort or finish.
 *
 * @author  Vaibhav Dwivedi
 * @version 1.0
 */
public abstract class OperationObserver {
    protected Operation operation = null;

    /**
     * Callback method the notifies when an operation begins execution.
     */
    protected abstract void operationDidStart();

    /**
     * Callback method the notifies when an operation is aborted.
     */
    protected abstract void operationDidAbort();

    /**
     * Callback method the notifies when an operation has finished execution.
     */
    protected abstract void operationDidFinish();

    void setOperation(Operation operation) {
        this.operation = operation;
    }
}
