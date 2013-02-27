//Version 2
package nachos.threads;

import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.PriorityBlockingQueue;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TCB;

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

	/** 
	 * Custom self test made for this.
	 */
	public static int defaultNumToTest = 20;

	/**
	 * Testing variables
	 */
	static boolean kt2start, kt1start, testFinished = false;

	public static void selfTest() {
		selfTest(defaultNumToTest);
	}
	public static void selfTest(int numThreadsToTest) {
		int testNum = Math.max(numThreadsToTest, 1);
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
		System.out.println("\n --------- Test 5 Join test");
		System.out.println(" --------- Initializing test"); 
		pq = (PriorityQueue) ps.newThreadQueue(true);
		final KThread kt1 = new KThread(new Runnable() {
			public void run() {
				while (kt2start == false) {
					KThread.yield();
					System.out.println("Still in kt1 while loop");
				}
				testFinished = true;
			}
		});
		kt1.setName("kt1");

		final KThread kt2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Running kt2");
				kt2start = true;
				kt1.join();
				System.out.println("Finished joining kt2 on kt1");
			}
		});
		kt1.setName("kt2");

		System.out.println("Setting priorities for kt 1 -4"); 
		ps.setPriority(kt1, 1);
		ps.setPriority(kt2, 2);

		System.out.println("Forking...");
		kt1.fork();
		kt2.fork();
		while (!testFinished) {
			KThread.yield();
		}
		//		System.out.println("Joining...");
		//		kt1.join();
		//		kt2.join();

		Lib.assertTrue(testFinished);

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
		Lib.assertTrue( ((ThreadState) thread.schedulingState).parents != null );

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {

		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			int initialCapacity = 10;
			this.waitingThreads = new PriorityBlockingQueue<PriorityQueueEntry>(initialCapacity, new PriorityTimeComparator());
			this.currentThread = null;
		}

		/**
		 * Added method
		 * @return is the queue is empty 
		 */
		public boolean isEmpty() {
			return waitingThreads.isEmpty();
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			Lib.assertTrue(thread != null);
			Lib.assertTrue(getThreadState(thread) != null);

			//To see if this is an edge case
			if (currentThread == null) {
				acquire(thread);
			}
			//Should not be called if it can immediately obtain access
			//FIXME why can currentThread be null and still call waitForAccess?
			Lib.assertTrue(currentThread != null);

			PriorityQueueEntry pqe = new PriorityQueueEntry(getThreadState(thread), Machine.timer().getTime());
			waitingThreads.add(pqe);
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			Lib.assertTrue(currentThread == null);
			Lib.assertTrue(waitingThreads.isEmpty() == true);
			Lib.assertTrue(thread != null);
			Lib.assertTrue(getThreadState(thread) != null);

			currentThread = thread;
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());

			if (waitingThreads.isEmpty()) {
				if (currentThread != null) {
					getThreadState(currentThread).noLongerCurrentThread(this);
					currentThread = null;
				}
				return null;
			}
			// Process the currentThread's parents it also updates effective priority 
			KThread[] oldParents = getThreadState(currentThread).disownParents(this);

			// Set's up the new thread to be the currentThread
			// In waitForAccess we have already enforced that the ThreadState != null
			currentThread = waitingThreads.poll().thread();
			Lib.assertTrue(currentThread != null); //UNN sanity check
			getThreadState(currentThread).becomeCurrentThread(oldParents, this);
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
			if (waitingThreads.peek() != null) {
				return waitingThreads.peek().threadState();
			}
			return null;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}

		/**
		 * To update a PriorityQueueEntry to contain the latest priority
		 */
		protected void updateThreadPriority(ThreadState tState) {
			PriorityQueueEntry[] pqeEntries = waitingThreads.toArray(new PriorityQueueEntry[0]);
			PriorityQueueEntry pqe;
			for (int i = 0; i < pqeEntries.length; i++) {
				pqe = pqeEntries[i];
				if (tState == pqe.threadState()) {
					Lib.assertTrue(tState.thread == pqe.thread());
					Lib.assertTrue(tState.effectivePriority == pqe.threadState().getEffectivePriority());
					waitingThreads.remove(pqe);
					waitingThreads.add(pqe);
					//TODO add a break statement?
					break;
				}
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

			int initialCapacity = 10;
			// Some of these are for sanity checks, mostly unnecessary
			this.effectivePriority = priority;
			this.child = null;
			this.parents = new java.util.PriorityQueue<KThread>(initialCapacity,
					new PriorityComparator());
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
			//FIXME not complete and is a bad assumption perhaps. CHECK LOGIC.
			if (queueWaitingOn != null) {
				updateEffectivePriority(queueWaitingOn.transferPriority);
			} else {
				updateEffectivePriority(true);
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
			// Cannot wait on two items. Should have been reset back to null
			// if this thread got access to a resource and is waiting for another
			Lib.assertTrue(this.queueWaitingOn == null);
			Lib.assertTrue(waitQueue != null);


			this.queueWaitingOn = waitQueue;
			addChild(waitQueue.currentThread, waitQueue); //Auto sets childThreadState.parent
			getThreadState(child).updateEffectivePriority(waitQueue.transferPriority);
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
			queueWaitingOn = null;
			currentThreadQueue = waitQueue;
			Lib.assertTrue(child == null); //FIXME check logic
			if (child != null) {
				removeChild(waitQueue);
			}
		}	

		/**
		 * Added. To be always able to recalculate effective priorities down the chain.
		 * This is the mega function that ensure correctness down the parent (could be multiple) 
		 * -> child (only 1 child) tree.
		 * NOTE: Should be able to do this even if I don't have a child
		 */
		private void updateEffectivePriority(boolean transferPriority) {
			updateEffectivePriority(transferPriority, null);
		}

		protected void updateEffectivePriority(Boolean transferPriority, HashSet<KThread> updatedThreads) {
			if (updatedThreads == null) {
				//Start of the updating process
				updatedThreads = new HashSet<KThread>();
			}
			
			if (updatedThreads.contains(this.thread)) {
				return;
			}
					
			// Check if transferPriority is true or false, false is easy case
			if (transferPriority == false) {
				return;
			}
			
			// Also need to check if the thread this threadState is on, has a different transferPriority setting
			if (this.queueWaitingOn != null && this.queueWaitingOn.transferPriority == false) {
				return;
			}

			/*
			 *  Fist consider parent's donated priority to get the "donated" effective priority
			 */
			int oldEffectivePriority = this.effectivePriority;
			// For efficiency
			int highestPriority = this.priority;
			int parentPriority = highestPriority - 1;
			KThread parent;

			//FIXME why is parents null added a weird check?
			if (parents == null) {
				parents = new java.util.PriorityQueue<KThread>();
			}

			// Incorporates parents' priorities.
			if (parents.peek() != null) {
				Lib.assertTrue(getThreadState(getThreadState(parents.peek()).child) == this);
				highestPriority = Math.max(highestPriority, getThreadState(parents.peek()).getEffectivePriority());
			}

			// CASE: In case we are joined, also incorporated 
			if (joinedParentThread != null) {
				highestPriority = Math.max(highestPriority, getThreadState(joinedParentThread).getEffectivePriority());
			}

			this.effectivePriority = highestPriority;
			updatedThreads.add(this.thread); //Add ourselves as already updated
			/*
			 * Second, compare parent's highest "donated" priority with my own native priority
			 */

			// Must check even if we have less effective priority than before.
			if (effectivePriority != oldEffectivePriority && child != null) {
				getThreadState(child).updateEffectivePriority(transferPriority, updatedThreads);
			}

			/*
			 * Case: Check if I have parents but am still waiting for another resource
			 * basically I'm still in the waitingThreads queue and need to be pulled out for a 
			 * reordering of priority
			 */
			if (effectivePriority != oldEffectivePriority && queueWaitingOn != null) {
				queueWaitingOn.updateThreadPriority(this);
			}
		}
		/**
		 * Add's a child and automatically updates child/parent pointers
		 */
		protected void addChild(KThread c, PriorityQueue waitQueue) {
			Lib.assertTrue(child == null);
			Lib.assertTrue(c != null);
			this.child = c;
			getThreadState(child).parents.add(this.thread);
			//UNN Perhaps an unnecessary sanity check
			Lib.assertTrue(getThreadState(child).parents.contains(this.thread));

			getThreadState(child).updateEffectivePriority(waitQueue.transferPriority);
		}

		protected void removeChild(PriorityQueue waitQueue) {
			Lib.assertTrue(child != null);
			getThreadState(child).parents.remove(this.thread);
			getThreadState(child).updateEffectivePriority(waitQueue.transferPriority);
			child = null;
		}


		/**
		 * disownParents() removes the child's parents. SHOULD ONLY BE CALLED BY nextThread(); 
		 * @param currentQueue is the queue the thread is on, not necessarily waiting (not in local threads)
		 * Uses:
		 * 1) When it is removed from being the currentThread (another thread is replacing it)
		 * NOTE: If it ends up waiting for another resource, waitForAccess should take care of that
		 * 
		 */
		protected KThread[] disownParents(PriorityQueue currentQueue) {
			//TODO clean up code in this method
			//NOTE: This should only remove parents in currentQueue, as it may have other parents
			PriorityQueueEntry[] oldParentsCurrentQueuePQE = currentQueue.waitingThreads.toArray(new PriorityQueueEntry[0]);
			PriorityQueueEntry pqe;
			KThread[] oldParentsCurrentQueue = new KThread[oldParentsCurrentQueuePQE.length];
			KThread p;
			for (int i = 0; i < oldParentsCurrentQueuePQE.length; i++) {
				pqe = oldParentsCurrentQueuePQE[i];
				p = pqe.thread();
				Lib.assertTrue(getThreadState(p).child == this.thread);
				oldParentsCurrentQueue[i] = p;
				getThreadState(p).child = null;
			}
			parents.clear(); //Should have no more parents;
			Lib.assertTrue(parents != null);
			updateEffectivePriority(currentQueue.transferPriority);
			return oldParentsCurrentQueue;
		}

		/**
		 * Does bookkeeping to a ThreadState so that when it becomes a currentThread,
		 * all the entries are correct.
		 */
		protected void becomeCurrentThread(KThread[] oldParents, PriorityQueue waitQueue) {
			// No longer waiting for a resource. 
			queueWaitingOn = null;
			child = null;
			currentThreadQueue = waitQueue;

			//Process parents.
			//NOTE: you cannot just clear the current parents because you can
			//daisy chain priorityQueues. (and have further up ancestors)
			for (int i =0; i < oldParents.length; i++) {
				// It can't have itself be its own parent. Self-reproduction is prohibited.
				// Also do not want multiple of the same parents in the queue.
				if (!parents.contains(oldParents[i]) && oldParents[i] != this.thread) {
					getThreadState(oldParents[i]).addChild(this.thread, waitQueue);
				}
			}
			//EffectivePriority should not have changed
		}

		protected void gotJoined(KThread parThread) {
			/*
			 * Only one parent can be joined at any time since a second call of join on this thread 
			 * has undefined behavior. 
			 */
			this.joinedParentThread = parThread;
			if (queueWaitingOn != null) {
				updateEffectivePriority(queueWaitingOn.transferPriority);
			}
			else {
				updateEffectivePriority(true);
			}

		}

		protected void noLongerCurrentThread(PriorityQueue waitQueue) {
			Lib.assertTrue(thread == waitQueue.currentThread);
		}

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;

		/**
		 * Added Fields
		 */
		private int effectivePriority = -1;

		protected PriorityQueue currentThreadQueue = null; //set only if the current thread
		protected PriorityQueue queueWaitingOn = null;
		protected KThread child = null;
		protected java.util.PriorityQueue<KThread> parents = null;
		protected KThread joinedParentThread = null;
	}


	/**
	 * New class to be able to help keep track of what entries in localThreads
	 * @author Jonathan
	 *
	 */
	protected class PriorityQueueEntry {
		private ThreadState threadState = null;
		private long entryTime = 0;

		public PriorityQueueEntry(ThreadState tState, long entryTime) {
			threadState = tState;
			this.entryTime = entryTime;
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

}
