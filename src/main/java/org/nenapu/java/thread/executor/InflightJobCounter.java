package org.nenapu.java.thread.executor;
/**
 * <p>
 * InflightJobCounter keeps the count of number of in-flight jobs
 * present at any given point in time for a particular reference.
 * One object will be created for a reference. 
 * </p>
 * This class keeps the vital information like,
 * <ul>
 * 	<li>Thread Name</li>
 *  <li>Reference</li>
 *  <li>Job count</li>
 * </ul>
 * @author Kirana NS
 */
public class InflightJobCounter {
	
	private int jobCount = 0;
	private String threadName = null;
	private String reference = "";
	
	public void setThreadName(String threadName){
		this.threadName = threadName;
	}
	
	public String getThreadName(){
		return this.threadName;
	}

	public int getJobCount(){
		return this.jobCount;
	}
	
	public void incrementJobCount(){
		this.jobCount++;
	}
	
	public void decrementJobCount(){
		this.jobCount--;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	@Override
	public String toString() {
		return "[jobCount=" + jobCount + ", threadName=" + threadName + ", reference=" + reference + "]";
	}
}
