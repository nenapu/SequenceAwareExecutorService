package org.nenapu.java.thread.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>
 * SequenceAwareExecutorService is a service to execute asynchronous tasks in parallel.
 * This executor service is sequence aware, i.e. If you have related tasks at any point 
 * in time, this service will make sure that they are processed by same thread in-order to 
 * maintain the sequence.
 * </p> 
 * <p>
 * Sequential processing of messages is very critical in businesses such as banking, stocks
 * etc.. where order of events matters. SequenceAwareExecutorService only sequences the 
 * related tasks. Unrelated tasks will be executed in parallel. Hence depending on the number
 * of related tasks at any given point in time the performance of this executor service will
 * vary between singlethread to pure multithreaded services like java.util.concurrent executor 
 * services.
 * </p>
 * <p>
 * To achieve this SequenceAwareExecutorService uses its own {@link SequenceAwareExecutorJob} which 
 * holds your actual task and a reference which will be used to identify the related tasks. There 
 * are two types SequentialExecutorJob to cater {@link Runnable} tasks and {@link Callable} tasks.
 * Each job will have a unique reference and related reference. The SequenceAwareExecutorService
 * works on related reference.
 * </p>
 * <p>
 * There is a dedicated distributer thread which will distribute tasks based on the related references.
 * Every pool thread has their own job queue. Distributer thread scans the related reference against
 * the inflight reference list. If related reference is found then the related task will be queued
 * to the thread which is processing the related task. There is no sticky reference concept. Tasks are
 * only queued if they are currently in process or waiting to be processed in any of pool thread queue.   
 * </p>
 * <p>
 * Generally tasks are distributed in round robin manner to the pool threads by the distributer. If a pool
 * thread queue is full then task will be tried to put in the next thread (for unrelated task). If in 
 * case all the pool thread queues are full then distributer will sleep for 200 milliseconds and will
 * try again. In case of related task, if the desired pool thread queue is full then distributer will
 * wait for 200 milliseconds and will try again. This process is endless until distributer finds the space
 * in the pool thread queue.
 * </p>
 * <p>
 * You can keep unique related reference for every task to achieve full parallel processing and 
 * single related reference to all tasks to achieve full sequential processing (as if all messages 
 * processed by single thread).
 * </p>
 * <p>
 * Distributer thread bear the name of "SequenceAwareDistributerThread". And pool threads bear
 * the name of "SequenceAwareServiceThread-&lt;thread count&gt;".
 * </p>
 * 
 * @author Kirana NS
 *
 */
public final class SequenceAwareExecutorService {

	/**
	 * Semaphore to coordinate inflight references
	 */
	public static final Object SEMAPHORE = new Object();
	
	public static final int MAX_JOBS_PER_POOLTHREAD = 10;
	public static final int WAIT_TIME_FOR_DISTRIBUTER_TO_DISTRIBUTE = 200;

	private boolean stopped = false;
	private int corepoolsize = 0;

	private List<PoolThread> activethreads = null;
	private Map<String, PoolThread> threadInstances = null;
	private Map<String, InflightJobCounter> inflightReferenceRegistry = null;

	private BlockingQueue<SequenceAwareExecutorJob> taskQueue = null;
	private Thread distributer = null;

	public SequenceAwareExecutorService(int corepoolsize, int queueSize){
		this.corepoolsize = corepoolsize;
		this.threadInstances = new ConcurrentHashMap<>(corepoolsize);
		this.activethreads = new ArrayList<>(corepoolsize);
		this.inflightReferenceRegistry = new HashMap<>(MAX_JOBS_PER_POOLTHREAD * corepoolsize);
		this.taskQueue = new LinkedBlockingQueue<>(queueSize);

		init();
	}
	
	public SequenceAwareExecutorService(int corepoolsize, BlockingQueue<SequenceAwareExecutorJob> taskQueue){
		this.corepoolsize = corepoolsize;
		this.threadInstances = new ConcurrentHashMap<>(corepoolsize);
		this.activethreads = new ArrayList<>(corepoolsize);
		this.inflightReferenceRegistry = new HashMap<>(MAX_JOBS_PER_POOLTHREAD * corepoolsize);
		this.taskQueue = taskQueue;

		init();
	}

	/**
	 * Creates and initializes all the pool threads and work distributer thread
	 */
	private void init() {
		this.stopped = false;
		for(int i=0; i < corepoolsize; i++){
			PoolThread thread =  new PoolThread("SequenceAwareServiceThread-"+i, inflightReferenceRegistry);
			this.threadInstances.put("SequenceAwareServiceThread-"+i, thread);
			this.activethreads.add(thread);
		}
		for(PoolThread thread : threadInstances.values()){
			thread.start();
		}
		this.distributer = new Thread(new DistributerJob(), "SequenceAwareDistributerThread");
		this.distributer.start();
	}

	/**
	 * Stops the executor service pool threads and distributer thread.
	 * Clears are the internal data structures. Unfinished jobs will be
	 * cleared from job queues.
	 */
	public synchronized void stop(){
		this.stopped = true;
		this.distributer.interrupt();
		for(PoolThread thread : threadInstances.values()){
			thread.doStop();
		}
		this.taskQueue.clear();
		this.threadInstances.clear();
		this.activethreads.clear();
	}

	/**
	 * Restarts the executor service.
	 */
	public synchronized void restart(){
		init();
	}
	
	/**
	 * Submits the {@link RunnableJob} which is an implementation of {@link SequenceAwareExecutorJob}
	 * into the distributer queue. This method does not return anything.
	 * 
	 * @param task
	 * @throws Exception
	 */
	public synchronized void submit(RunnableJob task) throws Exception{
		if(isStopped()){ 
			throw new IllegalStateException("SequenceAwareExecutorService is stopped");
		}
		this.taskQueue.put(task);
	}

	/**
	 * Submits the {@link CallableJob} which is an implementation of {@link SequenceAwareExecutorJob}
	 * into the distributer queue. This method returns the future object associated with the given
	 * task.
	 * 
	 * @param task
	 * @throws Exception
	 */
	public synchronized <T> Future<T> submit(CallableJob<T> task) throws Exception{
		if(isStopped()){ 
			throw new IllegalStateException("SequenceAwareExecutorService is stopped");
		}
		this.taskQueue.put(task);
		return (Future)task.getJob();
	}

	/**
	 * <p>
	 * DistributerJob is a runnable job executed by the SequenceAwareDistributerThread.
	 * The class holds the logic for distributing the jobs to respective pool threads.
	 * </p>
	 * There is two parts to the distribution logic,
	 * <ul>
	 * 	<li>If reference is found in inflight reference registry then the job will be submitted 
	 * to the respective pool thread queue</li>
	 * <li>If reference is not found in the inflight reference registry then job will be
	 * submitted to pool threads in round-robin fashion</li>
	 * </ul>
	 * 
	 * @author Kirana NS
	 */
	private class DistributerJob implements Runnable {
		@Override
		public void run() {
			int currentthread = 0;

			while(!isStopped()){
				try{
					SequenceAwareExecutorJob job = taskQueue.take();
					boolean submitted = false;
					InflightJobCounter inflightJobCounter = null;
					while(!submitted){
						synchronized (SEMAPHORE) {
							inflightJobCounter = inflightReferenceRegistry.get(job.getRefernce());
							if(inflightJobCounter == null){
								break;
							}
							PoolThread thread = threadInstances.get(inflightJobCounter.getThreadName());
							if(thread!=null) {
								submitted = thread.submit(job);
								if(submitted){
									break;
								}
							}
						}
						sleep();
					}
					if(submitted){
						inflightJobCounter.incrementJobCount();
						continue;
					}

					while(!submitted){
						PoolThread thread = activethreads.get(currentthread);
						synchronized (SEMAPHORE) {
							inflightJobCounter = new InflightJobCounter();
							inflightJobCounter.setThreadName(thread.getName());
							inflightJobCounter.setReference(job.getRefernce());
							inflightJobCounter.incrementJobCount();
							inflightReferenceRegistry.putIfAbsent(job.getRefernce(), inflightJobCounter);
						}
						submitted = thread.submit(job);
						if(currentthread+1 == corepoolsize){
							currentthread=0;
						}else{
							currentthread++;
						}
						if (!submitted && currentthread==0){
							sleep();
						}
					}
				} catch(InterruptedException e){
				}catch(Throwable e){
					e.printStackTrace();
				}
			}
		}
		
		/**
		 * Distributer thread will slepp for a specified number of milliseconds for pool
		 * thread queue to get clear for new jobs.
		 */
		private void sleep() {
			try{
				Thread.sleep(WAIT_TIME_FOR_DISTRIBUTER_TO_DISTRIBUTE); 
			}catch(Exception e){};
		}
	}

	/**
	 * Returns true if SequentialAwareExecutorService is stopped.
	 * 
	 * @return boolean
	 */
	public synchronized boolean isStopped(){
		return stopped;
	}
}
