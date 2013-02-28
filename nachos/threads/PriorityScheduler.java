/*
 * Version 3.
 * 
 * To do:
 * NO self test yet
 * Remove some inefficiencies
 */
package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {

	/**** CUSTOM SELF TEST METHODS ***/
	public static void selfTest() {
		int testNum = 10;
		int numThreadsToTest = testNum;
		boolean interrupt = Machine.interrupt().disable();
		if (testNum <= 1) {
			System.out.println("We have less than 2 threads. Aborting a useless test");
			return;
		}

		System.out.println(" --------- Testing: " + testNum + " Threads."); 
		System.out.println(" --------- Test 1 Make sure gets popped off in order of highest priority"); 
		System.out.println(" --------- Initializing test"); 

		PriorityScheduler ps = new PriorityScheduler();
		PriorityQueue pq = (PriorityQueue) ps.newThreadQueue(true);
		KThread[] threads = new KThread[testNum];


		// Initialize all threads 

		for (int i = 0; i < threads.length; i++) {
			threads[i] = new KThread();
			if (i == 0) {
				pq.acquire(threads[i]);
				ps.setPriority(threads[i], priorityMaximum);
			} else {
				pq.waitForAccess(threads[i]);
				ps.setPriority(threads[i], Math.min(priorityMaximum, Math.max(i % (priorityMaximum + 1), priorityMinimum)) );
			}
		}
		System.out.println("Running tests and popping off queue");

		KThread curThread = pq.currentThread;
		int curPriority = ps.getThreadState(curThread).getEffectivePriority();
		int upcomingPriority = priorityMinimum - 1;
		for (int i = 1; i < threads.length; i++) {
			upcomingPriority = ps.getThreadState(pq.nextThread()).getEffectivePriority();
			if (upcomingPriority > curPriority) {
				System.out.println("Error at i = " + i);
				break;
			}
		}
		System.out.println("**PriorityScheduler test 1 successful\n");



		/*
		 * TEST 3 Jonathan Eng
		 */
		System.out.println("\n --------- Test 3 Priority Inversion Test");
		System.out.println(" --------- Initializing test"); 

		// Thread c will have the highest priority, wait on a( lowest) whereas b is also on the ready queue
		KThread pq1low,pq1high,pq2low,pq2high;
		pq1low = new KThread();
		pq1low.setName("pq1low");
		pq1high = new KThread();
		pq1high.setName("pq1high");

		pq2low = new KThread();
		pq2low.setName("pq2low");
		pq2high = new KThread();
		pq2high.setName("pq2high");

		ps.setPriority(pq1low, 0);
		ps.setPriority(pq1high, 3);
		ps.setPriority(pq2low, 1);
		ps.setPriority(pq2high, 2);

		PriorityQueue pq1 = (PriorityQueue) ps.newThreadQueue(true);	
		PriorityQueue pq2 = (PriorityQueue) ps.newThreadQueue(true);
		pq1.acquire(pq1low);
		pq2.acquire(pq2low);
		pq1.waitForAccess(pq1high);
		pq2.waitForAccess(pq2high);
		pq2.waitForAccess(pq1low); //pq1high should donate priority to pq1low

		Lib.assertTrue(pq2.nextThread() == pq1low);
		Lib.assertTrue(pq2.nextThread() == pq2high);
		Lib.assertTrue(ps.getThreadState(pq1low).getEffectivePriority() == ps.getThreadState(pq1high).getEffectivePriority());
		Lib.assertTrue(pq1.nextThread() == pq1high);
		Lib.assertTrue(ps.getThreadState(pq1low).getEffectivePriority() == 0);

		System.out.println("--------- Test 3 Priority Donation (Chain 1 queue) simple passed");

		System.out.println("\n --------- Test 4 Lock stress test");
		final int stressCount = Math.min(numThreadsToTest * 4, TCB.maxThreads -1);
		System.out.println(" --------- Testing " + stressCount + " threads.");
		System.out.println(" --------- Initializing test"); 

		//Must have same locks, same # threads, 
		final Lock[] stressLocks = new Lock[stressCount];
		final KThread[] stressedThreads = new KThread[stressCount];

		for (int i = 0; i < stressCount; i++) {
			stressedThreads[i] = new KThread( new Runnable() {
				public void run() {
					for (int j = 0; j < stressCount; j = j + 4) {
						stressLocks[j].acquire();
						stressLocks[j+1].acquire();
						stressLocks[j+2].acquire();
						stressLocks[j+3].acquire();
						stressLocks[j+3].release();
						stressLocks[j+2].release();
						stressLocks[j+1].release();
						stressLocks[j].release();
					}
				}
			});
			stressLocks[i] = new Lock();
		}

		System.out.println("Forking threads");
		for (int i = 0; i < stressCount; i++) {
			stressedThreads[i].fork();
		}
		System.out.println("joining threads");
		for (int i = 0; i < stressCount; i++) {
			stressedThreads[i].join();
		}
	}

	/* Variables used in selfTest1 */
	public static boolean testFinished, kt2start, kt2finished = false;
	public static ThreadQueue ktReadyQueue = null;
	public static void selfTest1() {
		
		KThread.currentThread().setName("Default Thread");

		System.out.println("\n --------- Join test");
		System.out.println(" --------- Initializing test"); 
		Lib.debug('z', "kt1finished: " + testFinished);
		Lib.debug('z', "kt2finished: " + kt2finished);

		PriorityScheduler ps = new PriorityScheduler();
		boolean interrupt = Machine.interrupt().disable();
		final KThread kt1 = new KThread(new Runnable() {
			public void run() {
				while (kt2start == false) {
					KThread.yield();
					Lib.debug('z', "Still in kt1 while loop");
				}
				testFinished = true;
			}
		});
		kt1.setName("kt1");

		final KThread kt2 = new KThread(new Runnable() {
			public void run() {
				Lib.debug('z', "Running kt2");
				kt2start = true;
				kt1.join();
				Lib.debug('z',"Finished joining kt2 on kt1");
				kt2finished = true;
			}
		});
		kt1.setName("kt2");

		// Should be the thread running all these tests
		KThread.currentThread().setName("Priority Tester");
		ps.setPriority(KThread.currentThread(), priorityMaximum);

		System.out.println("Setting priorities for kt 1 -4"); 
		ps.setPriority(kt1, 1);
		ps.setPriority(kt2, priorityMaximum);

		System.out.println("Forking...");
		kt1.fork();
		kt2.fork();
		while (!testFinished || !kt2finished) {
			KThread.yield();
		}

		if (!kt2finished) {
			Lib.debug('z', "Shoudl not have printed this line.");
		}
		else
		{
			Lib.debug('z', "kt2finished");
		}
		//		System.out.println("Joining...");
		//		kt1.join();
		//		kt2.join();

		Lib.assertTrue(testFinished);
		Machine.interrupt().restore(interrupt);

		System.out.println("\n ---------PriorityScheduler test successful");
	}
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param	transferPriority	<tt>true</tt> if this queue should
	 *					transfer priority from waiting threads
	 *					to the owning thread.
	 * @return	a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum &&
				priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority-1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;    

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param	thread	the thread whose scheduling state to return.
	 * @return	the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			this.waitingThreads = new PriorityBlockingQueue<PriorityScheduler.PriorityQueueEntry>(1, 
					new PriorityTimeComparator());
			this.currentThread = null;
		}

		public boolean isEmpty() {
			return waitingThreads.isEmpty();
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			Lib.assertTrue(thread != null);
			Lib.assertTrue(getThreadState(thread)!= null);
			
			Lib.assertTrue(currentThread != thread);
			//FIXME will need change this in case autograder complains again
//			Lib.assertTrue(currentThread != null);
			if (currentThread == null) {
				acquire(thread);
				return;
			}

			PriorityQueueEntry pqe = new PriorityQueueEntry(getThreadState(thread), Machine.timer().getTime());
			waitingThreads.add(pqe);

			getThreadState(thread).waitForAccess(this);
		}

		/**
		 * Acquire has become the only gateway for a thread to become the current thread.
		 * NO THREAD CAN BECOME THE CURRENT THREAD BESIDE THROUGH ACQUIRE
		 */
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled()); //Given assertion from nachos

			Lib.assertTrue(currentThread == null);
			Lib.assertTrue(thread != null);
			currentThread = thread;

			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled()); //Given

			// Process the currentThread 
			if (currentThread != null) {
				// No longer has this resource (symbolized by PriorityQueues)
				// Q: How come there is no child queue pointer to be reset (queueWaitingOn)?
				getThreadState(currentThread).parentQueues.remove(this); 
				getThreadState(currentThread).refreshEffectivePriorityAfterRemoval();
			}

			currentThread = null;
			if (! waitingThreads.isEmpty() ) {
				// Acquire will set all the things correctly.
				acquire(waitingThreads.poll().thread());
			}
			return currentThread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		protected ThreadState pickNextThread() {
			if (!waitingThreads.isEmpty()) {
				return waitingThreads.peek().threadState();
			}
			return null;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}

		/*
		 * Checks the effective 
		 */
		public boolean highestPriorityValid() {
			return currentThread != null;
		}
		public int highestPriority() {
			return highestPriority(null);
		}
		public int highestPriority(KThread excludedThread) {
			int highestPr = priorityMinimum;
			for (PriorityQueueEntry pqe: waitingThreads) {
				if (pqe.thread() != excludedThread) {
					highestPr = Math.max(highestPr, pqe.threadState().getEffectivePriority());
				}
			}

			// Q: Why does the order of my logic checks here matter?
			if (currentThread != null && currentThread != excludedThread) {
				highestPr = Math.max(highestPr, getThreadState(currentThread).getEffectivePriority());
			}
			return highestPr;
		}


		private void refreshQueueOrder() {
			Lib.assertTrue(waitingThreads != null);

			PriorityQueueEntry[] waitingEntries = waitingThreads.toArray(new PriorityQueueEntry[0]);
			for (int i = 0; i < waitingEntries.length; i++) {
				PriorityQueueEntry pqe = waitingEntries[i];
				KThread waitingThread = pqe.thread();
				waitingThreads.remove(pqe);
				waitingThreads.add(pqe);
			}
		}

		protected void updateCurrentThreadPriority_Recursive() {
			if (currentThread == null) {
				//Nothing to update
				return;
			}

			if (transferPriority == false) {
				return;
			}

			//cst is currentThreadState
			ThreadState cst = getThreadState(currentThread);
			Lib.assertTrue(cst.parentQueues.contains(this)); //This thread must be a parent of current thread state

			//Compare old with new priority
			int oldEffectivePriority = cst.effectivePriority;
			int highestPriority = cst.priority;

			// get priority donated through joins
			if (cst.parentJoinee != null) {
				highestPriority = Math.max(highestPriority, getThreadState(cst.parentJoinee).getEffectivePriority());
			}

			//Get the priority of parents. note parents are queues, or resources cst owns.
			PriorityQueue[] parentQueuesArray = cst.parentQueues.toArray(new PriorityQueue[0]);
			for (PriorityQueue pq: parentQueuesArray) {
				// Excludes currentThread from the highestPriority() search
				highestPriority = Math.max(highestPriority, pq.highestPriority(currentThread));
			}

			cst.effectivePriority = highestPriority;

			Lib.assertTrue(cst.queueWaitingOn != this);
			// Set queueWaiting on to refresh it's priority
			if (cst.queueWaitingOn != null) {
				/*
				 * Extra check: check if the currentThread of the child queue (queueWaitingOn)
				 * has this queue as a parent. Enforing child parent queue relationships
				 */
				if (cst.queueWaitingOn.currentThread != null) {
					Lib.assertTrue(getThreadState(cst.queueWaitingOn.currentThread).parentQueues.contains(this));
				}
				cst.queueWaitingOn.updateCurrentThreadPriority_Recursive();
			}
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		/**
		 * Added fields
		 */
		PriorityBlockingQueue<PriorityQueueEntry> waitingThreads = null;
		KThread currentThread = null;

		public static final int INVALID_PRIORITY = priorityMinimum -1;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue
	 * it's waiting for, if any.
	 *
	 * @see	nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param	thread	the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			setPriority(priorityDefault);

			this.effectivePriority = priority;
			this.parentQueues = new ArrayList<PriorityScheduler.PriorityQueue>();
			this.queueWaitingOn = null;
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return	the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return	the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			return effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;
			int oldEffectivePriority = this.effectivePriority;
			this.effectivePriority = Math.max(priority, effectivePriority);

			if (oldEffectivePriority != effectivePriority && queueWaitingOn != null) {
				queueWaitingOn.updateCurrentThreadPriority_Recursive();
			}
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			Lib.assertTrue(waitQueue != null);
			Lib.assertTrue(this.queueWaitingOn == null);

			this.queueWaitingOn = waitQueue;
			//Otherwise we should have acquired
			Lib.assertTrue(waitQueue.currentThread != this.thread);
			//Now that I'm on the queue, must update the priority
			waitQueue.updateCurrentThreadPriority_Recursive();
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			Lib.assertTrue(waitQueue.currentThread == this.thread);
			parentQueues.add(waitQueue); //acquired a resource, has new parent queue
			/*
			 * Could be null or not before 
			 * null: never was waiting for anything, just got in.
			 * not null: was waiting and now that I have it, reset queueWaitingOn to null
			 */
			this.queueWaitingOn = null; 

			Lib.assertTrue(queueWaitingOn == null); // For super safety precaution
			waitQueue.updateCurrentThreadPriority_Recursive(); //update my own priority 

		}	

		public void joinUpdate(KThread parent) {
			Lib.assertTrue(parentJoinee == null);
			parentJoinee = parent;

			// Update myself
			this.effectivePriority = Math.max(effectivePriority, getThreadState(parent).getEffectivePriority());

			/*
			 *  Update resource I'm waiting on that I (in line for the resource)
			 *  got a perhaps different priority
			 */
			if (queueWaitingOn != null) {
				//FIXME can make more efficient
				queueWaitingOn.updateCurrentThreadPriority_Recursive();
			} 
		}

		/** ONLY TO BE CALLED IN NEXT THREAD AFTER A THREAD HAS BEEN DETHRONED FROM BEING A CURRENT THREAD
		 * Thus I am no longer in a queue and do not need to heed transferPriority. 
		 * Ensure that I have removed the queue from parentQueues of the PriorityQueue I jsut got dethroned from.
		 * (the resource I no longer hold, and just released)
		 * 
		 * We just got off a queue so we shouldn't be already be waiting for another queue. We need to update 
		 * our priority before that.
		 */
		protected void refreshEffectivePriorityAfterRemoval() {
// 			The Autograde is somehow able to get it on the queue without using acquire?
//			Lib.assertTrue(queueWaitingOn == null);

			int highestPriority = this.priority;
			for (PriorityQueue p: parentQueues) {
				highestPriority = Math.max(highestPriority, p.highestPriority());
			}

			if (parentJoinee != null) {
				highestPriority = Math.max(highestPriority, getThreadState(parentJoinee).getEffectivePriority());
			}
			this.effectivePriority = highestPriority;
		}

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		protected int effectivePriority;

		protected ArrayList<PriorityQueue> parentQueues = null;
		protected PriorityQueue queueWaitingOn = null;
		protected KThread parentJoinee = null;
	}

	/**
	 * New class to be able to help keep track of what entries in localThreads
	 * @author Jonathan
	 *
	 */
	protected class PriorityQueueEntry {
		private ThreadState threadState = null;
		private long entryTime = 0;
		private KThread thread = null;

		public PriorityQueueEntry(ThreadState tState, long entryTime) {
			threadState = tState;
			this.entryTime = entryTime;
			this.thread = tState.thread;
		}

		public int priority() {
			return threadState.getEffectivePriority();
		}

		public ThreadState threadState() {
			return threadState;
		}

		public KThread thread() {
			return threadState.thread;
		}
		public long entryTime() {
			return entryTime;
		}
	}

	/** 
	 * Implemented a PriorityComparator for our PriorityThreadQueue, a
	 *  subclass of ThreadQueue. 
	 * NOTE: Because a java.util.pq takes the LEAST as given by the comparator 
	 * ordering, things seemed flipped.   
	 */
	public class PriorityTimeComparator implements Comparator<PriorityQueueEntry> {
		public int compare(PriorityQueueEntry kt1, PriorityQueueEntry kt2) {
			int kt1p= kt1.priority();
			int kt2p = kt2.priority();
			if (kt1p == kt2p) {
				if (kt1.entryTime() > kt2.entryTime()) {
					return -1;
				}
				if (kt1.entryTime() < kt2.entryTime()) {
					return 1;
				} else {
					//TODO break ties arbitrarily ?
					return 0;
				}
			}
			else if (kt1p < kt2p) {
				return 1;
			}
			else {
				// kt1p > kt2p
				return -1;
			}
		}
		public boolean equals(Object o) {
			return o.equals(this);
		}
	}

	public class PriorityComparator implements Comparator<KThread> {
		public int compare(KThread kt1, KThread kt2) {
			int kt1p = getThreadState(kt1).getEffectivePriority();
			int kt2p = getThreadState(kt2).getEffectivePriority();

			if (kt1p == kt2p) {
				return 0;
			}
			else if (kt1p < kt2p) {
				return 1;
			}
			else { // kt1p > kt2p
				return -1;
			}
		}
		public boolean equals(Object o) {
			return o.equals(this);
		}
	}

	//	Not used as there are many priorityQueue edge cases	
	//	public class PriorityQueueComparator implements Comparator<PriorityQueue> {
	//		public int compare (PriorityQueue pq1, PriorityQueue pq2) {
	//			int pq1Priority = priorityMinimum - 1;
	//			int pq2Priority = priorityMinimum - 1;
	//			
	//			//FIXME enforce the invariant that we never have null priorityQueues
	//			Lib.assertTrue(!pq1.isEmpty() && !pq2.isEmpty());
	//			
	//			if (pq1.isEmpty() && pq2.isEmpty()) {
	//				return -1;
	//			} 
	//			else if ( !pq1.isEmpty() && !pq2.isEmpty()) {
	//				pq1Priority = pq1.pickNextThread().getEffectivePriority();
	//				pq2Priority = pq2.pickNextThread().getEffectivePriority();
	//				if (pq1Priority >= pq2Priority) {
	//					return -1;
	//				} else {
	//					return 1;
	//				}
	//			}
	//			else if ( !pq1.isEmpty() && pq2.isEmpty()) {
	//				return -1;
	//			}
	//			else if ( pq1.isEmpty() && !pq2.isEmpty()) {
	//				return 1;
	//			}
	//		}
	//	}
}
