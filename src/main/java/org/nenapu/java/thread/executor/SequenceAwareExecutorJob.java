package org.nenapu.java.thread.executor;

/**
 * Type of the job supported by SequenceAwareExecutorService
 * 
 * @author Kirana NS
 *
 */
public interface SequenceAwareExecutorJob {
	
	String getRefernce();
	
	long getUniqueId();
	
	Runnable getJob();
}
