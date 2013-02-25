package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
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

	/** 
	 * Custom self test made for this.
	 */
	public static void selfTest() {
		int testNum = 9000;
		boolean interrupt = Machine.interrupt().disable();

		System.out.println(" --------- Testing: " + testNum + " Threads."); 
		System.out.println(" --------- Initializing test"); 

		PriorityScheduler ps = new PriorityScheduler();
		PriorityQueue pq = (PriorityQueue) ps.newThreadQueue(true);
		KThread[] threads = new KThread[testNum];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new KThread( new Runnable() {
				String name = "Test1 Thread";
				public void run() {
					System.out.println(name + " is running!");
				}
			});
			ps.setPriority(threads[i], Math.min(priorityMaximum, Math.max(i % (priorityMaximum + 1), priorityMinimum)) );

			if (i == 0) {
				pq.acquire(threads[i]);
			} else {
				pq.waitForAccess(threads[i]);
			}
		}
		System.out.println("Done initializing");

		System.out.println("Running tests and popping off queue");
		KThread curThread = pq.currentThread;
		KThread prevThread = null;
		int curPriority, prevPriority;
		for (int i = 0; i < threads.length - 1; i++) { // length - 1 because 1 is currentThread, not on waitingThreads
			curThread = pq.nextThread();
			Lib.assertTrue(curThread != null);
			curPriority = ps.getThreadState(curThread).getEffectivePriority();
			if (prevThread != null && i > 1) {
				prevPriority = ps.getThreadState(prevThread).getEffectivePriority();
				if ( !(prevPriority >= curPriority) ) {
					System.out.println("Error when i = " + i);
				}
				Lib.assertTrue(prevPriority >=  curPriority);
			}
			prevThread = curThread;
		}
		System.out.println("---------PriorityScheduler test successful");
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
		//	TODO check if we can assume always ThreadState
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
			//TODO can threads rely on this method?
			return waitingThreads.isEmpty();
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			//Should not be called if it can immediately obtain access
			Lib.assertTrue(currentThread != null);
			Lib.assertTrue(thread != null);
			Lib.assertTrue(getThreadState(thread) != null);

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
				return null;
			}
			// Process the currentThread's parents it also updates effective priority 
			KThread[] oldParents = getThreadState(currentThread).disownParents(this);

			// Set's up the new thread to be the currentThread
			// In waitForAccess we have already enforced that the ThreadState != null
			currentThread = waitingThreads.poll().thread();
			Lib.assertTrue(currentThread != null); //UNN sanity check
			getThreadState(currentThread).becomeCurrentThread(oldParents);
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
			// implement me
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
			updateEffectivePriority();
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
			addChild(waitQueue.currentThread); //Auto sets childThreadState.parent
			getThreadState(child).updateEffectivePriority();
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
			if (child != null) {
				removeChild();
			}
		}	

		/**
		 * Added. To be always able to recalculate effective priorities down the chain.
		 * This is the mega function that ensure correctness down the parent (could be multiple) 
		 * -> child (only 1 child) tree.
		 * NOTE: Should be able to do this even if I don't have a child
		 */
		private void updateEffectivePriority() {
			/*
			 *  Fist consider parent's donated priority to get the "donated" effective priority
			 */
			//CHECK do not need to make priorityMinimum - 1. Ask yourself why?
			int highestPriority = priorityMinimum;
			int parentPriority = priorityMinimum - 1;
			KThread parent;
			//Don't worry about length 0, it works. Check api
			//FIXME why is parents null added a werid check?
			if (parents == null) {
				parents = new java.util.PriorityQueue<KThread>();
			}
			KThread[] parentThreads = parents.toArray(new KThread[0]); 
			for (int i = 0; i < parentThreads.length; i++) {
				parent = parentThreads[i];
				parentPriority = getThreadState(parent).getEffectivePriority();
				Lib.assertTrue(parent != null);
				if (parentPriority > highestPriority) {
					highestPriority = parentPriority;
				}
			}

			/*
			 * Second, compare parent's highest "donated" priority with my own native priority
			 */
			this.effectivePriority = Math.max(highestPriority, priority);

			/*
			 * Case: Check if I have parents but am still waiting for another resource
			 */
			if (queueWaitingOn != null) {
				queueWaitingOn.updateThreadPriority(this);
			}
		}

		/**
		 * Add's a child and automatically updates child/parent pointers
		 */
		protected void addChild(KThread c) {
			Lib.assertTrue(child == null);
			Lib.assertTrue(c != null);
			this.child = c;
			getThreadState(child).parents.add(this.thread);
			//UNN Perhaps an unnecessary sanity check
			Lib.assertTrue(getThreadState(child).parents.contains(this.thread));

			getThreadState(child).updateEffectivePriority();
		}

		protected void removeChild() {
			Lib.assertTrue(child != null);
			getThreadState(child).parents.remove(this.thread);
			getThreadState(child).updateEffectivePriority();
			child = null;
		}

		/**
		 * Adds a parent and automatically updates child/parent pointers
		 * @param p the parent KThread
		 */
		protected void addParent(KThread p) {
			Lib.assertTrue(p != null);
			parents.add(p);
			getThreadState(p).child = this.thread;
			//As the child I must now update my effective priority 
			updateEffectivePriority();
		}

		/**
		 * disownParents() removes the child's parents 
		 * @param currentQueue is the queue the thread is on, not necessarily waiting (not in local threads)
		 * Uses:
		 * 1) When it is removed from being the currentThread (another thread is replacing it)
		 * NOTE: If it ends up waiting for another resource, waitForAccess should take care of that
		 * 
		 */
		protected KThread[] disownParents(PriorityQueue currentQueue) {
			//TODO clean up code in this method
			PriorityQueueEntry[] oldParentsPQE = currentQueue.waitingThreads.toArray(new PriorityQueueEntry[0]);
			PriorityQueueEntry pqe;
			KThread[] oldParents = new KThread[oldParentsPQE.length];
			KThread p;
			for (int i = 0; i < oldParentsPQE.length; i++) {
				pqe = oldParentsPQE[i];
				p = pqe.thread();
				Lib.assertTrue(getThreadState(p).child == this.thread);
				oldParents[i] = p;
				getThreadState(p).child = null;
			}
			parents.clear();
			Lib.assertTrue(parents != null);
			updateEffectivePriority();
			return oldParents;
		}

		/**
		 * Does bookkeeping to a ThreadState so that when it becomes a currentThread,
		 * all the entries are correct.
		 */
		protected void becomeCurrentThread(KThread[] oldParents) {
			//CHECK logic of the multiple asserts but should be correct.

			// No longer waiting for a resource. 
			queueWaitingOn = null;
			child = null;

			//Process parents.
			//NOTE: you cannot just clear the current parents because you can
			//daisy chain priorityQueues. (and have further up ancestors)
			for (int i =0; i < oldParents.length; i++) {
				// It can't have itself be its own parent. Self-reproduction is prohibited.
				// Also do not want multiple of the same parents in the queue.
				if (!parents.contains(oldParents[i]) && oldParents[i] != this.thread) {
					getThreadState(oldParents[i]).addChild(this.thread);
				}
			}
			//EffectivePriority may have changed because of new parents
			//CHECK: Although it shouldn't (ask yourself why)!!
			updateEffectivePriority();
		}

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;

		/**
		 * Added Fields
		 */
		private int effectivePriority = -1;

		protected PriorityQueue queueWaitingOn = null;
		protected KThread child = null;
		protected java.util.PriorityQueue<KThread> parents = null;
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

}
