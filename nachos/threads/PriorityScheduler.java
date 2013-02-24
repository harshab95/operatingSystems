package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

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
			int initialCapacity = 10;
			this.localThreads = new java.util.PriorityQueue<PriorityQueueEntry>(initialCapacity,
					new PriorityTimeComparator());
			this.currentThread = null;
		}

		/**
		 * Added method
		 * @return is the queue is empty 
		 */
		public boolean isEmpty() {
			//TODO can threads rely on this method?
			return localThreads.isEmpty();
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			//Should not be called if it can immediately obtain access
			Lib.assertTrue(currentThread != null);
			
			PriorityQueueEntry pqe = new PriorityQueueEntry(getThreadState(thread), Machine.timer().getTime());
			localThreads.add(pqe);
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			Lib.assertTrue(currentThread == null);
			Lib.assertTrue(localThreads.isEmpty() == true);
			
			currentThread = thread;
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			return null;
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
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		
		/**
		 * Added fields
		 */
		java.util.PriorityQueue<PriorityQueueEntry> localThreads = null;
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
			// implement me
			return priority;
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

			// implement me
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
			this.queueWaitingOn = waitQueue;
			child = waitQueue.currentThread;
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
			// implement me
		}	
		
		/**
		 * Added. To be always able to recalculate effective priorities down the chain
		 */
		protected void updateEffectivePriority() {
			//TODO implement me
		}

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		
		/**
		 * Added Fields
		 */
		private int effectivePriority = -1;
		
		protected KThread child = null;
		protected java.util.PriorityQueue<KThread> parents = null;
		protected PriorityQueue queueWaitingOn = null;
		
		
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

		public ThreadState identity() {
			return threadState;
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
