package com.kryonite.modules.operations;

/**
 * Completion handler listens to the operation lifecycle and notifies the client when the operation completes, i.e., aborts or finishes.
 *
 * @author  Vaibhav Dwivedi
 * @version 1.0
 */
public abstract class CompletionHandler {
    protected abstract void onComplete();
}
