# SequenceAwareExecutorService

<h2>Introduction</h2>
<p>
SequenceAwareExecutorService is a service to execute asynchronous tasks in parallel.
This executor service is sequence aware, i.e. If you have related tasks at any point 
in time, this service will make sure that they are processed by same thread in-order to 
maintain the sequence.
</p> 
<p>
Sequential processing of messages is very critical in businesses such as banking, stocks
etc.. where order of events matters. SequenceAwareExecutorService only sequences the 
related tasks. Unrelated tasks will be executed in parallel. Hence depending on the number
of related tasks at any given point in time the performance of this executor service will
vary between singlethread to pure multithreaded services like java.util.concurrent executor 
services.
</p>
<p>
To achieve this SequenceAwareExecutorService uses its own SequenceAwareExecutorJob which 
holds your actual task and a reference which will be used to identify the related tasks. There 
are two types SequenceAwareExecutorJob to cater Runnable tasks and Callable tasks.
Each job will have a unique reference and related reference. The SequenceAwareExecutorService
works on related reference.
</p>
<p>
There is a dedicated distributer thread which will distribute tasks based on the related references.
Every pool thread has their own job queue. Distributer thread scans the related reference against
the inflight reference list. If related reference is found then the related task will be queued
to the thread which is processing the related task. There is no sticky reference concept. Tasks are
only queued if they are currently in process or waiting to be processed in any of pool thread queue.   
</p>
<p>
Generally tasks are distributed in round robin manner to the pool threads by the distributer. If a pool
thread queue is full then task will be tried to put in the next thread (for unrelated task). If in 
case all the pool thread queues are full then distributer will sleep for 200 milliseconds and will
try again. In case of related task, if the desired pool thread queue is full then distributer will
wait for 200 milliseconds and will try again. This process is endless until distributer finds the space
in the pool thread queue.
</p>
<p>
You can keep unique related reference for every task to achieve full parallel processing and 
single related reference to all tasks to achieve full sequential processing (as if all messages 
processed by single thread).
</p>
<p>
Distributer thread bear the name of "SequenceAwareDistributerThread". And pool threads bear
the name of "SequenceAwareServiceThread-&lt;thread count&gt;".
</p>

<h2>JRE Required</h2>
Java 8

<h2>Usage</h2>
<h3>Initializing the executor service</h3>
Follwoing two constructor provided for easy construction of SequenceAwareExecutorService:<br/> 
1. 
```java
SequenceAwareExecutorService service = new SequenceAwareExecutorService(NUMBER_OF_THREADS, SERVICE_QUEUE_SIZE);
```
This constructor will create ```java.util.concurrent.LinkedBlockingQueue<SequenceAwareExecutorJob>``` by default with the size
passed.<br/>
2.
```java
SequenceAwareExecutorService service = new SequenceAwareExecutorService(NUMBER_OF_THREADS, new LinkedBlockingQueue<SequenceAwareExecutorJob>(1000000));
```
This constructor will take the implementation of ```java.util.concurrent.BlockingQueue<SequenceAwareExecutorJob>```
<br/>

<h3>Creating the Job</h3>
Based on your requirement, you can create either CallableJob or RunnableJob by providing following,<br/>
<ul>
<li>Unique Id: Unique id is a identifier which is unique. Just to identify the job. However SequenceAwareExecutorService
does not depend on this id for processing.</li>
<li>Related Reference: Related reference is the reference based on which related jobs are identified. This is a must. Based on 
this the behavior of the SequenceAwareExecutorService changes. If you provide same related reference to all jobs then SequenceAwareExecutorService will behave like a single thread. And if you provide unique reference of all jobs then SequenceAwareExecutorService will behave like a pure multithreaded executor service</li>
<li>Job: This is the actual Callable or Runnable job which you intended to perform</li>
</ul>
1. CallableJob
```java
CallableJob<String> job = new CallableJob<String>(UniqueId, Related_Reference, new MyCallableJob());
```
<br/>
2. RunnableJob
```java
RunnableJob job = new RunnableJob(UniqueId, Related_Reference, new MyCallableJob());
```
<br/>

<h3>Submitting the Job</h3>
There are two overloaded methods provided each for CallableJob and RunnableJob.<br/>
1. CallableJob
```java
Future<T> future = service.submit(job);
```
Submitting Callable job returns the future object associated with the job. You can use the future
object to get the result.
<br/>
2. RunnableJob
```java
service.submit(job);
```
Submitting Runnable job does not return anything.
<br/>

<h3>Stopping and Restarting the Service</h3>
You can stop and restart the SequenceAwareExecutorService.<br>
Stopping the Executor Service:
```java
service.stop();
```
This will stop all the PoolThreads, the Distributer Thread, and clears all queues and InflightReferenceRegistry.
<br/>
Restarting the Executor Service:
```java
service.restart();
```
This will recreate the PoolThreads, Distributer Thread, And starts the service.
<br/>

<b>Note:</b> There is no ``` service.start();``` method provided. Creating the service object itself will start the executor service.
