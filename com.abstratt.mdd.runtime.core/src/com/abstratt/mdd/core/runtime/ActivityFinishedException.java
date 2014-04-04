package com.abstratt.mdd.core.runtime;

/** 
 * Not really an exception, used for halting execution of an
 * activity that has reached a final node. 
 */
public class ActivityFinishedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
}
