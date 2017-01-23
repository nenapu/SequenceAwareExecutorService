package org.nenapu.java.thread.executor;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Test class to test the {@link SequenceAwareExecutorService}
 * @author Kirana NS
 */
public class TestSequenceAwareExecutorService {
	
	private static final int NUMBER_OF_THREADS = 50;
	private static final int PROCESSING_TIME_MILLISEC = 10;
	
	private static final int NUMBER_OF_MESSAGS = 10000;
	private static final int SERVICE_QUEUE_SIZE = 1000000;

	private static final int UPPER_BOUND_RANDOM_REFERENCE = 10000;
	private static final int LOWER_BOUND_RANDOM_REFERENCE = 1;

	@SuppressWarnings("unused")
	private class MyRunnableJob implements Runnable {

		@Override
		public void run() {
			try{
				Thread.sleep(PROCESSING_TIME_MILLISEC);
			}catch(Exception e){
			}
		}
	}
	
	private class MyCallableJob implements Callable<String> {

		@Override
		public String call() {
			try{
				Thread.sleep(PROCESSING_TIME_MILLISEC);
			}catch(Exception e){
			}
			return "";
		}
	}

	public static void runSequentialExecutorService(){

		long sequentialStart = System.nanoTime();
		SequenceAwareExecutorService service = new SequenceAwareExecutorService(NUMBER_OF_THREADS, SERVICE_QUEUE_SIZE);
		List<Future<String>> futureList = new ArrayList<>();
		
		long start = System.nanoTime();
		for (int i = 1; i <= NUMBER_OF_MESSAGS; i++) {
			int jobId = ThreadLocalRandom.current().nextInt(LOWER_BOUND_RANDOM_REFERENCE,UPPER_BOUND_RANDOM_REFERENCE);
			CallableJob<String> job = new CallableJob<String>(i, String.valueOf(jobId), new TestSequenceAwareExecutorService(). new MyCallableJob());
			try {
				futureList.add(service.submit(job));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		long end = System.nanoTime();
		System.out.println("Time taken to submit jobs to Sequential distributer queue:" + new DecimalFormat("#.##########").format(((double)(end-start)/1000000000)) + " sec");
		
		int count = 0;
		while(!futureList.isEmpty()){
			try{
				futureList.remove(0).get();
			}catch(Exception e){};
		}
		service.stop();
		long sequentialEnd = System.nanoTime();
		System.out.println("Time taken to process all jobs by SequentialExecutorService:" + new DecimalFormat("#.##########").format(((double)(sequentialEnd-sequentialStart)/1000000000)) + " sec");

	}
	
	public static void runMultiThreadExecutorService(){
		long multiStart = System.nanoTime();
		ExecutorService service = new ThreadPoolExecutor(NUMBER_OF_THREADS, NUMBER_OF_THREADS, 3600L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(SERVICE_QUEUE_SIZE));
		List<Future<String>> futureList = new ArrayList<>();
		
		long start = System.nanoTime();
		for (int i = 1; i <= NUMBER_OF_MESSAGS; i++) {
			futureList.add(service.submit(new TestSequenceAwareExecutorService().new MyCallableJob()));
		}
		long end = System.nanoTime();
		System.out.println("Time taken to submit jobs to MultiThread distributer queue:" + new DecimalFormat("#.##########").format(((double)(end-start)/1000000000)) + " sec");
		
		while(!futureList.isEmpty()){
			try{
				futureList.remove(0).get();
			}catch(Exception e){};
		}
		service.shutdown();
		long multiEnd = System.nanoTime();
		System.out.println("Time taken to process all jobs by MultiThreadExecutorService:" + new DecimalFormat("#.##########").format(((double)(multiEnd-multiStart)/1000000000)) + " sec");

	}
	
	public static void main(String[] args) {
		runSequentialExecutorService();
		runMultiThreadExecutorService();
	}
}
