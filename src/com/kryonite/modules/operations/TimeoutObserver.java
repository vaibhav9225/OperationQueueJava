package com.kryonite.modules.operations;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A convenience observer class that times out the operations if it executes more than the allotted time.
 *
 * @author  Vaibhav Dwivedi
 * @version 1.0
 */
public class TimeoutObserver extends OperationObserver {
    private boolean isOperationComplete = false;

    private final long delay;
    private final Timer timer;

    /**
     * Creates a timeout observer with the specified delay.
     * @param delay
     */
    public TimeoutObserver(long delay) {
        this.delay = delay;
        this.timer = new Timer();
    }

    @Override
    public void operationDidStart() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (operation != null && !isOperationComplete) {
                    operation.cancel();
                }
                operation = null;
            }
        }, delay);
    }

    @Override
    public void operationDidAbort() {
        isOperationComplete = true;
        cancelTimer();
    }

    @Override
    public void operationDidFinish() {
        isOperationComplete = true;
        cancelTimer();
    }

    private void cancelTimer() {
        timer.cancel();
        timer.purge();
    }
}
