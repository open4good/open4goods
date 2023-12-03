package org.open4goods.helper;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

public class BoundedExecutor {
	private final Executor exec;
	private final Semaphore semaphore;

	public BoundedExecutor(Executor exec, int bound) {
		this.exec = exec;
		semaphore = new Semaphore(bound);
	}

	public void submitTask(final Runnable command)
			throws InterruptedException, RejectedExecutionException {
		semaphore.acquire();
		try {
			exec.execute(() -> {
                try {
                    command.run();
                } finally {
                    semaphore.release();
                }
            });
		} catch (RejectedExecutionException e) {
			semaphore.release();
			throw e;
		}
	}
}