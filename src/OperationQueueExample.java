import com.kryonite.modules.operations.*;

class OperationQueueExample {
    /**
     * In this example, op1, op2, begin execution in parallel, while op3, op4 wait as they depend on op1, op2.
     * Op3 then starts as soon as op1 finishes.
     * Op4 only begins once op2 (Longer Task), op3 are both finished.
     * Once all operations finish, then the final operation is executed.
    */
    public static void main(String[] args) throws Exception {
        OperationQueue queue1 = new OperationQueue();
        // Sets a maximum execution time of 400 milliseconds on all queue operations.
        queue1.setOperationTimeout(400);

        OperationQueue queue2 = new OperationQueue();

        Op op1 = new Op("op1", 10);

        Op op2 = new Op("op2", 100000);
        // Adds a timeout observer.
        // If two timeout observers are set on an operation/queue, the smaller one takes preference.
        // These times are sufficient based on the system you run them on.
        // On slower systems, 600ms might result in a timeout. In which case you should increase them.
        op2.addObserver(new TimeoutObserver(600));

        Op op3 = new Op("op3", 5);

        Op op4 = new Op("op4", 5);
        // Takes dependencies on op2, op3 and ignores op4.
        op4.addDependencies(op2, op3, op4);
        // Takes a condition. If returned true, condition is met and operation proceeds, else it aborts.
        op4.addCondition(new OperationCondition() {
            @Override
            protected boolean evaluate() {
                return true;
            }
        });

        // A final operation that listens to the completion of all operations.
        Operation finalOperation = new Operation() {
            @Override
            public void execute() {
                System.out.println("Total sum of all operations is : " + (op1.sum + op2.sum + op3.sum + op4.sum));
                finish();
            }
        };
        // This can be replaced by resolving dependency graph manually and taking dependency on just op4, but this is a safer bet.
        finalOperation.addDependencies(op1, op2, op3, op4);
        // By setting this to true, finish operation proceeds even if any of its dependencies are cancelled or aborted.
        finalOperation.canProceedWhenDependenciesAborted(true);
        // Sets the completion handler that listens to the completion of an operation.
        finalOperation.setCompletionHandler(new CompletionHandler() {
            @Override
            protected void onComplete() {
                System.out.println("All operations are now complete.");
            }
        });

        // Marking this to true, auto finished the operations on queue after completion.
        queue1.canAutoFinishOperationsOnCompletion(true);
        // Add operations to the queue.
        queue1.addOperations(op1, op2);

        // Operations can be added to a different queue that its dependencies.
        queue2.addOperations(finalOperation, op3, op4);
        queue2.canAutoFinishOperationsOnCompletion(true);
    }

    // SAMPLE OPERATION - That calculates the sum up to the provided number.
    static class Op extends Operation {
        private final String name;
        private int count;

        public int sum = 0;

        public Op(String name, int count) {
            this.name = name;
            this.count = count;
        }

        @Override
        public void execute() {
            for (int i=1; i<=count; i++) {
                sum += i;
            }
            System.out.println("Sum for " + name + " is = " + sum);
            finish();
        }
    }
}
