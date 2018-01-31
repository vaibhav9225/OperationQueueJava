package com.kryonite.modules.operations;

import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * Operation Queue is a wrapper around executor service that executes the operations, once all its dependencies are met.
 *
 * @author  Vaibhav Dwivedi
 * @version 1.0
 */
public class OperationQueue {
    private long maxExecutionTime = -1;
    private int maxConcurrentOperations = 0;
    private ExecutorService executor = null;
    private boolean autoFinishOperationsOnCompletion = false;
    private OperationList operationList = new OperationList();

    private static final int DEFAULT_MAX_CONCURRENT_OPERATIONS = 5;

    /**
     * Initializes operation queue.
     */
    public OperationQueue() {
        this(DEFAULT_MAX_CONCURRENT_OPERATIONS);
    }

    /**
     * Initializes operation queue with a limit on maximum concurrent operations.
     * @param maxConcurrentOperations
     */
    public OperationQueue(int maxConcurrentOperations) {
        setMaxConcurrentOperationsCount(maxConcurrentOperations);
    }

    /**
     * Adds the specified operation to the queue and begins it's execution provided all its dependencies are satisfied.
     * @param operation
     */
    public void addOperation(Operation operation) {
        operation.setOperationQueue(this);

        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            initExecutor();
        }

        trackOperation(operation);
        if (!operation.hasUnfinishedDependencies()) {
            executeOperation(operation);
        }
    }

    /**
     * Adds the specified operations to the queue and begins each operations execution provided all its dependencies are satisfied.
     * @param operations
     */
    public void addOperations(Operation... operations) {
        for (Operation operation : operations) {
            addOperation(operation);
        }
    }

    /**
     * Sets a timeout observer on the queue operations with the provided maximum execution time.
     * @param maxExecutionTime - Maximum execution time in milliseconds.
     */
    public void setOperationTimeout(long maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime;
    }

    /**
     * Disables the timeout observer on the queue operations.
     */
    public void disableOperationTimeout() {
        this.maxExecutionTime = -1;
    }

    /**
     * Sets a maximum limit on the execution of concurrent operations.
     * @param maxConcurrentOperations - Maximum number of allowed concurrent operations.
     */
    public void setMaxConcurrentOperationsCount(int maxConcurrentOperations) {
        this.maxConcurrentOperations = maxConcurrentOperations;
    }

    /**
     * Returns the value of the flag that tells if an operation should be auto finished upon completion.
     * @return
     */
    public boolean shouldAutoFinishOperationsOnCompletion() {
        return autoFinishOperationsOnCompletion;
    }

    /**
     * If set to true, the operation auto finishes upon completion.
     * @param autoFinishOperationsOnCompletion
     */
    public void canAutoFinishOperationsOnCompletion(boolean autoFinishOperationsOnCompletion) {
        this.autoFinishOperationsOnCompletion = autoFinishOperationsOnCompletion;
    }

    void executeOperation(Operation operation) {
        if (operation.isAborted() || operation.isFinished()) {
            notifyOperationComplete(operation);
            return;
        }

        if (operation.isExecuting()) {
            return;
        }

        if (maxExecutionTime != -1) {
            operation.addObserver(new TimeoutObserver(maxExecutionTime));
        }

        Future operationFuture = executor.submit(operation);
        operation.setOperationFuture(operationFuture);
    }

    synchronized void notifyOperationComplete(Operation operation) {
        operationList.removeOperation(operation);

        if (operationList.isEmpty()) {
            executor.shutdownNow();
        }
    }

    private void trackOperation(Operation operation) {
        if (operationList.containsOperation(operation)) {
            return;
        }
        operationList.addOperation(operation);
    }

    private synchronized void initExecutor() {
        this.executor = Executors.newFixedThreadPool(maxConcurrentOperations);
    }
}
