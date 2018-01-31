package com.kryonite.modules.operations;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Operation represents a single executable unit.
 *
 * @author  Vaibhav Dwivedi
 * @version 1.0
 */
public abstract class Operation implements Runnable {
    private AtomicBoolean isExecuting = new AtomicBoolean(false);
    private AtomicBoolean isFinished = new AtomicBoolean(false);
    private AtomicBoolean isCancelled = new AtomicBoolean(false);
    private AtomicBoolean isAborted = new AtomicBoolean(false);
    private AtomicBoolean canProceedWhenDependenciesAborted = new AtomicBoolean(false);
    private OperationQueue operationQueue;
    private Future operationFuture = null;
    private OperationList dependencyList = new OperationList();
    private OperationList subscriberList = new OperationList();
    private List<OperationCondition> conditionList = new ArrayList<>();
    private List<OperationObserver> observerList = new ArrayList<>();

    private final String operationId = UUID.randomUUID().toString();

    protected abstract void execute();

    /**
     * Do not call this method outside the scope of the package.
     * This is an internal method that is used for the execution of the operation by the executor.
     */
    @Override
    @Deprecated
    public void run() {
        notifyObservers(OperationState.STARTED);

        if (isCancelled.get() || !meetsAllConditions()) {
            cancel();
            return;
        }

        isExecuting.set(true);
        execute();
        isExecuting.set(false);

        if (operationQueue != null && operationQueue.shouldAutoFinishOperationsOnCompletion() && !isFinished()) {
            finish();
        }
    }

    /**
     * Takes a dependency on the specified operation.
     * @param operation
     */
    public void addDependency(Operation operation) {
        if (operation == this) {
            return;
        }

        dependencyList.addOperation(operation);
        operation.addSubscriber(this);
    }

    /**
     * Takes dependencies on the specified operations.
     * @param operations
     */
    public void addDependencies(Operation... operations) {
        for (Operation operation : operations) {
            addDependency(operation);
        }
    }

    /**
     * Removes the dependency on the specified operation.
     * @param operation
     */
    public void removeDependency(Operation operation) {
        if (operation == this) {
            return;
        }

        dependencyList.removeOperation(operation);
        operation.removeSubscriber(operation);
    }

    /**
     * Removes the dependencies on the specified operations.
     * @param operations
     */
    public void removeDependencies(Operation... operations) {
        for (Operation operation : operations) {
            removeDependency(operation);
        }
    }

    /**
     * Returns true, if the operation is still under execution.
     * @return
     */
    public boolean isExecuting() {
        return isExecuting.get();
    }

    /**
     * Returns true, if the operation has finished execution.
     * @return
     */
    public boolean isFinished() {
        return isFinished.get();
    }

    /**
     * Returns true if the operation is cancelled or aborted.
     * @return
     */
    public boolean isAborted() {
        return isAborted.get() || isCancelled.get();
    }

    /**
     * Returns the unique ID for the operation.
     * @return
     */
    public String getOperationId() {
        return operationId;
    }

    /**
     * If set to true, the operations proceeds with execution even if it's dependencies are cancelled or aborted.
     * @param canProceedWhenDependenciesAborted
     */
    public void canProceedWhenDependenciesAborted(boolean canProceedWhenDependenciesAborted) {
        this.canProceedWhenDependenciesAborted.set(canProceedWhenDependenciesAborted);
    }

    /**
     * Cancels the execution of the operation.
     * NOTE: If the operation has begun execution, it might take a while to completely stop execution.
     */
    public synchronized void cancel() {
        if (operationFuture != null) {
            operationFuture.cancel(true);
        }

        if (!isAborted()) {
            abort();
        }

        isCancelled.set(true);
    }

    /**
     * Finishes the operation and removes it from the queue and notifies other operations that depend on it.
     */
    public synchronized void finish() {
        if (isAborted() || isFinished()) {
            return;
        }

        isFinished.set(true);
        if (operationQueue != null) {
            operationQueue.notifyOperationComplete(this);
        }
        notifySubscribers();
        notifyObservers(OperationState.FINISHED);
    }

    /**
     * Aborts the operation and removes it from the queue and notifies other operations that depend on it.
     */
    public synchronized void abort() {
        if (isFinished() || isAborted()) {
            return;
        }

        isAborted.set(true);
        if (operationQueue != null) {
            operationQueue.notifyOperationComplete(this);
        }
        notifySubscribers();
        notifyObservers(OperationState.ABORTED);
    }

    /**
     * Takes a dependency on the condition, what needs to be met before operation begins execution.
     * @param operationCondition
     */
    public void addCondition(OperationCondition operationCondition) {
        conditionList.add(operationCondition);
        operationCondition.setOperation(this);
    }

    /**
     * Takes dependencies on the conditions, what needs to be met before operation begins execution.
     * @param operationConditions
     */
    public void addConditions(OperationCondition... operationConditions) {
        for (OperationCondition operationCondition : operationConditions) {
            addCondition(operationCondition);
        }
    }

    /**
     * Adds an observer to the operation which listens to the lifecycle events of the operation.
     * @param operationObserver
     */
    public void addObserver(OperationObserver operationObserver) {
        observerList.add(operationObserver);
        operationObserver.setOperation(this);
    }

    /**
     * Adds observers to the operation which listen to the lifecycle events of the operation.
     * @param operationObservers
     */
    public void addObservers(OperationObserver... operationObservers) {
        for (OperationObserver operationObserver : operationObservers) {
            addObserver(operationObserver);
        }
    }

    void setOperationFuture(Future operationFuture) {
        this.operationFuture = operationFuture;
    }

    void setOperationQueue(OperationQueue operationQueue) {
        this.operationQueue = operationQueue;
    }

    synchronized boolean hasUnfinishedDependencies() {
        return !dependencyList.isEmpty();
    }

    void addSubscriber(Operation operation) {
        subscriberList.addOperation(operation);
    }

    void removeSubscriber(Operation operation) {
        subscriberList.removeOperation(operation);
    }

    synchronized void notifyDependencyComplete(Operation operation) {
        if (operation.isAborted() && !canProceedWhenDependenciesAborted.get()) {
            cancel();
            return;
        }

        dependencyList.removeOperation(operation);

        if (dependencyList.isEmpty() && operationQueue != null) {
            operationQueue.executeOperation(this);
        }
    }

    private boolean meetsAllConditions() {
        for (OperationCondition condition : conditionList) {
            if (condition.evaluate() == false) {
                return false;
            }
        }

        return true;
    }

    private synchronized void notifyObservers(OperationState operationState) {
        for (OperationObserver operationObserver : observerList) {
            switch (operationState) {
                case STARTED:
                    operationObserver.operationDidStart();
                    break;
                case ABORTED:
                    operationObserver.operationDidAbort();
                    break;
                case FINISHED:
                    operationObserver.operationDidFinish();
                    break;
            }
        }
    }

    private synchronized void notifySubscribers() {
        for (Operation operation : subscriberList.getOperations()) {
            operation.notifyDependencyComplete(this);
        }
        subscriberList.clear();
    }

    private enum OperationState {
        STARTED,
        ABORTED,
        FINISHED
    }
}
