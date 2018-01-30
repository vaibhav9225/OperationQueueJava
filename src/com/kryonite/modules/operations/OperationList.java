package com.kryonite.modules.operations;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

class OperationList {
    private Map<String, Operation> operationsMap = new HashMap<>();

    public void addOperation(Operation operation) {
        operationsMap.put(operation.getOperationId(), operation);
    }

    public void removeOperation(Operation operation) {
        operationsMap.remove(operation.getOperationId());
    }

    public Collection<Operation> getOperations() {
        return operationsMap.values();
    }

    public boolean isEmpty() {
        return operationsMap.isEmpty();
    }

    public boolean containsOperation(Operation operation) {
        return operationsMap.containsKey(operation.getOperationId());
    }

    public void clear() {
        operationsMap.clear();
    }
}
