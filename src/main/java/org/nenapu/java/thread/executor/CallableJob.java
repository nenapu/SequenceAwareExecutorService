package org.nenapu.java.thread.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * Type of the {@link SequenceAwareExecutorJob}. This job is created using
 * {@link Callable} task.
 * 
 * @author Kirana NS
 *
 * @param <T>
 */
public final class CallableJob<T> implements SequenceAwareExecutorJob {

	private String reference = "";
	private long uniqueId = -1L;
	private RunnableFuture<T> job = null;
	
	public CallableJob(long uniqueId, String reference, Callable<T> callable){
		this.uniqueId = uniqueId;
		this.reference = reference;
		this.job = new FutureTask<T>(callable);
	}
	
	public String getRefernce(){
		return reference;
	}
	
	public long getUniqueId(){
		return uniqueId;
	}
	
	public Runnable getJob(){
		return job;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((reference == null) ? 0 : reference.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CallableJob other = (CallableJob) obj;
		if (reference == null) {
			if (other.reference != null)
				return false;
		} else if (!reference.equals(other.reference))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CallableJob [reference=" + reference + ", uniqueId=" + uniqueId + "]";
	}
}
