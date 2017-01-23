package org.nenapu.java.thread.executor;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * PoolThread represents one thread of {@link SequenceAwareExecutorService}.
 * PoolThread is the actual thread which executes the job. Each PoolThread 
 * contains their own job queue. The jobs are distributed by the SequenceAwareDistributerThread.
 * </p>
 * 
 * @author Kirana NS
 */
public final class PoolThread extends Thread {

	private boolean stopped = false;
	
	private AtomicInteger numberOfjobs = new AtomicInteger(0);
	
	private Map<String, InflightJobCounter> inflightReferenceRegistry = null;
	private Queue<SequenceAwareExecutorJob> taskQueue = null;
	
	public PoolThread(String name, Map<String, InflightJobCounter> inflightReferenceRegistry){
		setName(name);
		this.taskQueue = new ConcurrentLinkedQueue<>();
		this.inflightReferenceRegistry = inflightReferenceRegistry;
	}

	/**
	 * Returns true if number of jobs in the PoolThread queue is equal to the maximum
	 * number of jobs allowed per thread at any given point in time.
	 * 
	 * @return boolean
	 */
	public boolean isFull(){
		if(this.numberOfjobs.get() == SequenceAwareExecutorService.MAX_JOBS_PER_POOLTHREAD){
			return true;
		}
		return false;
	}

	/**
	 * Submits {@link SequenceAwareExecutorJob} to the PoolThread queue.
	 * Returns true if successfully submits the job. returns false if there
	 * is a problem in submitting the job or queue is full. 
	 * 
	 * @param job
	 * @return boolean
	 */
	protected boolean submit(SequenceAwareExecutorJob job){
		if(this.numberOfjobs.get() == SequenceAwareExecutorService.MAX_JOBS_PER_POOLTHREAD){
			return false;
		}
		synchronized(taskQueue){
			this.taskQueue.add(job);
			this.numberOfjobs.incrementAndGet();
			this.taskQueue.notify();
		}
		return true;
	}

	/**
	 * Removes the first job in the queue.
	 */
	private void remove(){
		try{
			SequenceAwareExecutorJob job = null;
			synchronized(taskQueue){
				job = taskQueue.poll();
			}
			synchronized (SequenceAwareExecutorService.SEMAPHORE) {
				InflightJobCounter jobReference = inflightReferenceRegistry.get(job.getRefernce());
				jobReference.decrementJobCount();
				if(jobReference.getJobCount() <= 0){
					inflightReferenceRegistry.remove(job.getRefernce());
				}
				numberOfjobs.decrementAndGet();
			}
		}catch(Throwable e){
		}
	}

	public void run(){
		while(!isStopped()){
			try{
				synchronized(taskQueue){
					while(taskQueue.isEmpty() && !isStopped()){
						try{
							taskQueue.wait();
						}catch(InterruptedException e){
							break;
						}
					}
				}
				if(!isStopped()){
					SequenceAwareExecutorJob job = null;
					synchronized(taskQueue){
						job = taskQueue.peek();
					}
					//System.out.println("Executing job [" + job.getUniqueId() + "] with reference["+ job.getRefernce() +"] by thread:" + Thread.currentThread().getName());
					job.getJob().run();
				}
			} catch(Throwable e){
				e.printStackTrace();
			}finally{
				if(!isStopped()){
					remove();
				}
			}
		}
	}

	/**
	 * Clears the queue and corresponding {@link InflightJobCounter} from the 
	 * InflightReferenceRegistry.
	 */
	private void clear(){
		if(taskQueue.isEmpty()){
			return;
		}

		SequenceAwareExecutorJob job = taskQueue.poll();
		do{
			synchronized (SequenceAwareExecutorService.SEMAPHORE) {
				inflightReferenceRegistry.remove(job.getRefernce());
				numberOfjobs.decrementAndGet();
			}
			job = taskQueue.poll();
		}while (job != null);
	}

	/**
	 * Stops the PoolThread and clears queue and inflight references.
	 */
	public synchronized void doStop(){
		stopped = true;
		this.interrupt();
		clear();
	}
	
	/**
	 * Returns true if PoolThread is stopped.
	 * 
	 * @return boolean
	 */
	public synchronized boolean isStopped(){
		return stopped;
	}
}