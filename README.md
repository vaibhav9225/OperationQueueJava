# OperationQueueJava
A java based implementation of NS Operation Queue.

The following library provides the following features:
- Creating async operations which can have dependencies on each other.
- Adding a condition to the operator that needs to be met for its successful execution.
- Adding an observer to the operation that listens to its lifecycle events.
- Adding a completion listener to the operation that signal the completion of operation.

Utils:
- Timeout observer - This can be added to an operation to limit its execution time.

New features over NS Operation Queue:
- Ability to add timeout for the entire queue.
- Ability to proceed an operation if any of its dependencies fail or gets aborted.

Missing features from NS Operation Queue:
- Synchronous operations. (Only async operations supported.)

Take a look at https://github.com/vaibhav9225/OperationQueueJava/blob/master/src/OperationQueueExample.java for more information.
