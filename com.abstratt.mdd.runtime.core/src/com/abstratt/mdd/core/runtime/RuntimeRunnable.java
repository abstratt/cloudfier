package com.abstratt.mdd.core.runtime;

/**
 * It might look silly we have our own runnable, but chances are we will need
 * to be able to throw exceptions/return values in the future, and that we can't
 * do with java.lang.Runnable.
 */
public interface RuntimeRunnable extends Runnable {
	public void run();
}
