package org.nenapu.java.thread.executor;


/**
 * Type of the {@link SequenceAwareExecutorJob}. This job is created using
 * {@link Runnable} task.
 * 
 * @author Kirana NS
 */
public final class RunnableJob implements SequenceAwareExecutorJob{

	private String reference = "";
	private long uniqueId = -1L;
	private Runnable job = null;
	
	public RunnableJob(long uniqueId, String reference, Runnable runnable){
		this.uniqueId = uniqueId;
		this.reference = reference;
		this.job = runnable;
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
		RunnableJob other = (RunnableJob) obj;
		if (reference == null) {
			if (other.reference != null)
				return false;
		} else if (!reference.equals(other.reference))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RunnableJob [reference=" + reference + ", uniqueId=" + uniqueId + "]";
	}
}
